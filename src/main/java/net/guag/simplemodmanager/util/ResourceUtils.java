package net.guag.simplemodmanager.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ResourceUtils {
    public static final Logger LOGGER = LoggerFactory.getLogger("simplemodmanager");

    public ResourceUtils() {}

    public void toggleResourcePack(String packName, boolean enable) {
        try {
            ModUtils.moveResourcePack(packName, enable);
        } catch (IOException e) {
            LOGGER.error("Failed to {} resource pack: {}", enable ? "enable" : "disable", packName, e);
        }
    }

    public void toggleShaderPack(String packName, boolean enable) {
        try {
            ModUtils.moveShaderPack(packName, enable);
        } catch (IOException e) {
            LOGGER.error("Failed to {} shader pack: {}", enable ? "enable" : "disable", packName, e);
        }
    }
}
