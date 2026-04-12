package ru.hiflif.luckyexp.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketBuffer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.function.Supplier;

public class SortPacket {
    public SortPacket() {}

    public static void encode(SortPacket msg, PacketBuffer buf) {}
    public static SortPacket decode(PacketBuffer buf) { return new SortPacket(); }

    public static void handle(SortPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                // СООБЩЕНИЕ НА СЕРВЕРЕ
                player.sendMessage(new StringTextComponent("§b[Сервер] Пакет получен! Начинаю поиск пузырьков..."), player.getUUID());
                processInventory(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void processInventory(ServerPlayerEntity player) {
        PlayerInventory inv = player.inventory;
        int totalBottles = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == Items.GLASS_BOTTLE) {
                totalBottles += stack.getCount();
                inv.setItem(i, ItemStack.EMPTY);
            }
        }

        player.sendMessage(new StringTextComponent("§b[Сервер] Найдено пузырьков: " + totalBottles), player.getUUID());

        if (totalBottles <= 0) {
            player.sendMessage(new StringTextComponent("§c[Ошибка] В инвентаре нет ПУСТЫХ пузырьков!"), player.getUUID());
            return;
        }

        for (int i = 0; i <= 7 && totalBottles > 0; i++) {
            inv.setItem(i, new ItemStack(Items.GLASS_BOTTLE, 1));
            totalBottles--;
        }

        if (totalBottles > 0) {
            for (int i = 9; i < inv.getContainerSize() && totalBottles > 0; i++) {
                ItemStack currentStack = inv.getItem(i);
                if (currentStack.isEmpty()) {
                    int amountToAdd = Math.min(totalBottles, Items.GLASS_BOTTLE.getMaxStackSize());
                    inv.setItem(i, new ItemStack(Items.GLASS_BOTTLE, amountToAdd));
                    totalBottles -= amountToAdd;
                }
            }
        }

        if (totalBottles > 0) {
            player.drop(new ItemStack(Items.GLASS_BOTTLE, totalBottles), false, false);
            player.sendMessage(new StringTextComponent("§6[Сервер] Часть пузырьков не влезла и была выброшена."), player.getUUID());
        }

        player.sendMessage(new StringTextComponent("§a[Сервер] Готово! Слоты 0-7 заполнены."), player.getUUID());
        player.containerMenu.broadcastChanges();
    }
}