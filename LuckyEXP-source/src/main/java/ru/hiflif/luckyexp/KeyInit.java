package ru.hiflif.luckyexp;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

public class KeyInit {
    public static KeyBinding sortKey;

    public static void register() {
        sortKey = new KeyBinding("key.luckyexp.sort", GLFW.GLFW_KEY_M, "key.categories.luckyexp");
        ClientRegistry.registerKeyBinding(sortKey);
    }
}