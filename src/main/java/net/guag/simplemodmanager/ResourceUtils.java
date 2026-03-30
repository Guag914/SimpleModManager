package net.guag.simplemodmanager;

import net.minecraft.client.Minecraft;

import java.io.IOException;

public class ResourceUtils {
    private final Minecraft client;

    public ResourceUtils(Minecraft client) {
        this.client = client;
    }

    public void toggleResourcePack(String packName, boolean enable) {
        try {
            ModUtils.moveResourcePack(packName, enable);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void toggleShaderPack(String packName, boolean enable) {
        try {
            ModUtils.moveShaderPack(packName, enable);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
