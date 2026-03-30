package net.guag.simplemodmanager;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

import net.fabricmc.api.ModInitializer;


public class SimpleModManager implements ModInitializer, ClientModInitializer {
	private static KeyMapping openUIBinding;


	@Override
	public void onInitialize() {
		onInitializeClient();
		System.out.println("Simple Mod Manager initialized!");
	}

	@Override
	public void onInitializeClient() {

		// Register a keybind (F8) to open the mod manager GUI.
		openUIBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.SimpleModManager.openui",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_F8,
				KeyMapping.Category.register(Identifier.bySeparator("simplemodmanager:keybinds", ':'))
		));

		// On each client tick, if F8 is pressed and no screen is open, open the GUI.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openUIBinding.consumeClick()) {
				Minecraft mc = Minecraft.getInstance();
				if (mc.screen == null) {
					// Pass the client and the hardcoded mod toggles (loaded from file folders)
					mc.setScreen(new ModManagerScreen(mc, ModUtils.getModToggles(), ModUtils.getResourceToggles(), ModUtils.getShaderToggles()));
				}
			}
		});
	}

}
