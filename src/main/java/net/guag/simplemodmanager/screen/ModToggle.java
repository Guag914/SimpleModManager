package net.guag.simplemodmanager.screen;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.guag.simplemodmanager.util.ModUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModToggle {
    private File file;
    private final String jarName;
    private boolean enabled;

    private static final File STATE_FILE = new File(Minecraft.getInstance().gameDirectory, "config/simplemodmanager_state.json");
    private static boolean initialized = false;

    public static final Logger LOGGER = LoggerFactory.getLogger("simplemodmanager");

    private static final Set<String> defaultDisabledMods = new HashSet<>();

    public ModToggle(File file, boolean enabled) {
        this.file = file;
        this.enabled = enabled;
        this.jarName = file.getName();

    }


    public void applyChange() {
        try {
            if (enabled) {
                this.file = ModUtils.moveModToEnabled(file);
            } else {
                this.file = ModUtils.moveModToDisabled(file);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to {} mod file: {}", enabled ? "enable" : "disable", file.getName(), e);
        }
    }


    public void resetToDefault() {
        boolean shouldBeEnabled = !isDisabledByDefault(file.getName());
        if (this.isEnabled() != shouldBeEnabled) {
            this.setEnabled(shouldBeEnabled);
        }
    }


    public static void initializeDefaultDisabledMods() {
        if (initialized) return;

        if (STATE_FILE.exists()) {
            loadState();
        } else {
            scanDisabledModsFolder();
            saveState();
        }
        initialized = true;
    }

    private static void scanDisabledModsFolder() {
        File disabledDir = new File(Minecraft.getInstance().gameDirectory, "disabled-mods");
        if (disabledDir.exists() && disabledDir.isDirectory()) {
            File[] files = disabledDir.listFiles((_, name) -> name.toLowerCase().endsWith(".jar"));
            if (files != null) {
                for (File file : files) {
                    defaultDisabledMods.add(file.getName().toLowerCase());
                }
            }
        }
    }

    private static void loadState() {
        try (BufferedReader reader = new BufferedReader(new FileReader(STATE_FILE))) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            if (obj.has("defaultDisabledMods")) {
                JsonArray arr = obj.getAsJsonArray("defaultDisabledMods");
                for (int i = 0; i < arr.size(); i++) {
                    defaultDisabledMods.add(arr.get(i).getAsString().toLowerCase());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse state file {}. Falling back to folder scan.", STATE_FILE.getName(), e);
            scanDisabledModsFolder();
        }
    }

    public static void saveState() {
        try {
            JsonObject obj = new JsonObject();
            JsonArray arr = new JsonArray();
            for (String mod : defaultDisabledMods) {
                arr.add(mod);
            }
            obj.add("defaultDisabledMods", arr);

            STATE_FILE.getParentFile().mkdirs(); // Make sure config dir exists
            try (FileWriter writer = new FileWriter(STATE_FILE)) {
                new Gson().toJson(obj, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save mod state configuration to {}", STATE_FILE.getName(), e);
        }
    }

    //Metadata getters
    public String getIconPath(){
        File modFile = this.getFile();

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) return "Error reading metadata";

            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonElement je = JsonParser.parseReader(reader);
                if (!je.isJsonObject()) return "Invalid metadata";

                return  je.getAsJsonObject().has("icon") ?  je.getAsJsonObject().get("icon").getAsString() : null;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to get mod icon path: {}", this, e);
            return "Error reading metadata";
        }
    }

    public String getModId() {
        File modFile = this.getFile();

        if (!modFile.exists()) {
            // Try the other folder — e.g., if enabled, check disabled, or vice versa
            File altFile = new File("run/disabled-mods", modFile.getName());
            if (altFile.exists()) modFile = altFile;
        }

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) return null;

            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonElement je = JsonParser.parseReader(reader);
                if (!je.isJsonObject()) return null;

                JsonObject root = je.getAsJsonObject();

                if (root.has("id")) {
                    return root.get("id").getAsString();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to get modID: {}", this, e);
        }
        return null;
    }

    public String getMetadataSummaryForMod() {
        File modFile = this.getFile();

        String modId = this.getModId();
        Optional<ModContainer> containerOpt = FabricLoader.getInstance().getModContainer(modId);

        String modName;
        boolean jarName;
        if (containerOpt.isPresent()) {
            modName = containerOpt.get().getMetadata().getName();
            jarName = false;
        } else {
            modName = this.getJarName(); // fallback to jar name if metadata is missing
            jarName = true;
        }

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) return "No metadata";

            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonElement je = JsonParser.parseReader(reader);
                if (!je.isJsonObject()) return "Invalid metadata";

                JsonObject root = je.getAsJsonObject();

                // Extract fields
                String version = root.has("version") ? root.get("version").getAsString() : null;

                // Compose a summary string (truncate description for brevity)
                StringBuilder summary = new StringBuilder();

                if (jarName){
                    summary.append(modName);
                } else  { summary.append(modName).append(":");

                    if (version != null) {
                        summary.append(" v").append(version);
                    }
                }
                return summary.toString().isEmpty() ? "No metadata" : summary.toString();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to get mod metadata summary: {}", this, e);
            return "Error reading metadata";
        }
    }

    public String getDescription(){
        File modFile = this.getFile();

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) return "No metadata";

            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonElement je = JsonParser.parseReader(reader);
                if (!je.isJsonObject()) return "Invalid metadata";

                return  je.getAsJsonObject().has("description") ?  je.getAsJsonObject().get("description").getAsString() : null;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to get mod description: {}", this, e);
            return "Error reading metadata";
        }
    }

    //Getters
    public String getJarName() {return this.jarName;}
    public String getDisplayName() {return jarName.replaceAll("\\.(jar|zip)$", "");}
    public void toggled() {
        this.enabled = !this.enabled;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public File getFile() {return file;}
    public Component getButtonText() {return Component.literal(enabled ? "§aEnabled " : "§7Disabled ");}
    public static boolean isDisabledByDefault(String modFileName) {return defaultDisabledMods.contains(modFileName.toLowerCase());}

    //Setters
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
