package ru.hiflif.luckyexp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CChatMessagePacket;
import net.minecraft.util.Hand;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = LuckyEXP.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    private static final List<Task> taskQueue = new ArrayList<>();
    private static int delayTimer = 0;
    private static int cycleStep = 0;
    private static boolean isRunning = false;

    private static final int COMMAND_DELAY = 5;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (KeyInit.sortKey != null && KeyInit.sortKey.consumeClick()) {
            isRunning = !isRunning;
            taskQueue.clear();
            cycleStep = 0;
            mc.player.sendMessage(new StringTextComponent(isRunning ? "§a[LuckyEXP] СТАРТ" : "§c[LuckyEXP] СТОП"), mc.player.getUUID());
        }

        if (!isRunning) return;

        if (!taskQueue.isEmpty()) {
            if (delayTimer > 0) {
                delayTimer--;
            } else {
                Task task = taskQueue.remove(0);
                executeTask(mc, task);
                delayTimer = task.fast ? 1 : COMMAND_DELAY;
            }
            return;
        }

        runAutomationLogic(mc);
    }

    private static void runAutomationLogic(Minecraft mc) {
        switch (cycleStep) {
            case 0: // ПРОВЕРКА И СОРТИРОВКА
                if (countBottles(mc) < 9) {
                    stopBot(mc, "§cМало пустых бутылок (нужно минимум 9)!");
                    return;
                }
                if (!(mc.screen instanceof InventoryScreen)) {
                    mc.setScreen(new InventoryScreen(mc.player));
                } else {
                    planFastBottleSorting(mc);
                    cycleStep = 1;
                }
                break;

            case 1: // ПОИСК И БРОСОК ОПЫТА 200 УР
                planStrictExpFromInventory(mc);
                cycleStep = 2;
                break;

            case 2: // КОМАНДЫ ДЛЯ СЛОТОВ 0, 1, 2 (25) И 3 (200 + РАЗБИТЬ)
                planSafeCommandChain(mc, new int[]{0, 1, 2}, 25, false);
                planSafeCommandChain(mc, new int[]{3}, 200, true);
                cycleStep = 3;
                break;

            case 3: // КОМАНДЫ ДЛЯ СЛОТОВ 4, 5, 6 (25) И 7 (200 + РАЗБИТЬ)
                planSafeCommandChain(mc, new int[]{4, 5, 6}, 25, false);
                planSafeCommandChain(mc, new int[]{7}, 200, true);
                cycleStep = 4;
                break;

            case 4:
                if (!(mc.screen instanceof InventoryScreen)) {
                    mc.setScreen(new InventoryScreen(mc.player));
                } else {
                    for (int i = 0; i <= 8; i++) {
                        taskQueue.add(new Task(i + 36, 0, ClickType.QUICK_MOVE).setFast());
                    }
                    taskQueue.add(new Task(true).setFast());
                    cycleStep = 0;
                }
                break;
        }
    }

    private static void planSafeCommandChain(Minecraft mc, int[] slots, int power, boolean andBreak) {
        for (int slot : slots) {
            taskQueue.add(new Task(true).setFast());
            taskQueue.add(new Task(slot, null, false).setFast());
            taskQueue.add(new Task(slot, "/bottleexp " + power));

            if (andBreak) {
                taskQueue.add(new Task(slot, null, false).setFast());
                taskQueue.add(new Task(slot, null, true));
            }
        }
    }

    private static void planFastBottleSorting(Minecraft mc) {
        for (int t = 0; t <= 7; t++) {
            ItemStack inHotbar = mc.player.inventory.getItem(t);
            if (isAnyBottle(inHotbar) && inHotbar.getCount() == 1) continue;

            for (int s = 9; s <= 35; s++) {
                ItemStack inInv = mc.player.inventoryMenu.getSlot(s).getItem();
                if (isAnyBottle(inInv)) {
                    taskQueue.add(new Task(s, 0, ClickType.PICKUP).setFast());
                    taskQueue.add(new Task(t + 36, 1, ClickType.PICKUP).setFast());
                    taskQueue.add(new Task(s, 0, ClickType.PICKUP).setFast());
                    break;
                }
            }
        }
    }

    private static boolean planStrictExpFromInventory(Minecraft mc) {
        for (int i = 0; i < 45; i++) {
            ItemStack stack = (i < 9) ? mc.player.inventory.getItem(i) : mc.player.inventoryMenu.getSlot(i).getItem();
            if (!stack.isEmpty() && stack.getItem() == Items.EXPERIENCE_BOTTLE && stack.getHoverName().getString().contains("200 ур.")) {
                int winSlot = (i < 9) ? i + 36 : i;
                taskQueue.add(new Task(winSlot, 8, ClickType.SWAP).setFast());
                taskQueue.add(new Task(true).setFast());
                taskQueue.add(new Task(8, null, true));
                return true;
            }
        }
        return false;
    }

    private static void executeTask(Minecraft mc, Task task) {
        if (task.isClose) {
            mc.player.closeContainer();
        } else if (task.command != null) {
            mc.player.inventory.selected = task.slot;
            mc.getConnection().send(new CChatMessagePacket(task.command));
        } else if (task.isUse) {
            mc.player.inventory.selected = task.slot;
            mc.gameMode.useItem(mc.player, mc.level, Hand.MAIN_HAND);
        } else if (task.slot != -1) {
            mc.player.inventory.selected = task.slot;
            if (task.type != null) {
                mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, task.slot, task.button, task.type, mc.player);
            }
        }
    }

    private static void stopBot(Minecraft mc, String message) {
        isRunning = false;
        taskQueue.clear();
        mc.player.sendMessage(new StringTextComponent(message), mc.player.getUUID());
    }

    private static int countBottles(Minecraft mc) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            if (isAnyBottle(mc.player.inventory.getItem(i))) count += mc.player.inventory.getItem(i).getCount();
        }
        return count;
    }

    private static boolean isAnyBottle(ItemStack s) {
        if (s.isEmpty()) return false;
        return s.getItem() == Items.GLASS_BOTTLE || s.getHoverName().getString().contains("Бутылочка");
    }

    private static class Task {
        int slot = -1, button; ClickType type; String command; boolean isClose, isUse, fast = false;
        Task(int s, int b, ClickType t) { slot = s; button = b; type = t; }
        Task(boolean c) { isClose = c; }
        Task(int s, String cmd) { slot = s; command = cmd; }
        Task(int s, String cmd, boolean u) { slot = s; isUse = u; }
        Task setFast() { this.fast = true; return this; }
    }
}