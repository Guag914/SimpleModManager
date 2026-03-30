package net.guag.simplemodmanager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
//import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screens.Screen;
//import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.components.Button;
//import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.components.EditBox;
//import net.minecraft.text.Text;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModManagerScreen extends Screen{
    private final Minecraft client;
    private final List<ModToggle> modToggles;
    private final List<ModToggle> resourceToggles;
    private final List<ModToggle> shaderToggles;

    DrawingUtils drawUtil = new DrawingUtils();

    // Scrolling state
    private double scrollAmount = 0;
    private double maxScroll = 0;
    private final double scrollStep = 15;

    private EditBox searchBox;
    private String searchQuery = "";

    //Button Info
    int btnHeight = 20;

    private final List<Button> modToggleButtons = new ArrayList<>();
    private final List<Button> shaderButtons = new ArrayList<>();
    private final List<Button> resourceButtons = new ArrayList<>();
    private final List<Button> modResetButtons = new ArrayList<>();
    private final List<Button> modMetadataButtons = new ArrayList<>();
    private final Map<Button, String> tooltipMap = new HashMap<>();
    private final List<Button> reloadButtons = new ArrayList<>();

    private final List<Button> resourceMetadataButtons = new ArrayList<>();
    private final List<Button> shaderMetadataButtons = new ArrayList<>();
    private final List<Button> resourceToggleButtons = new ArrayList<>();
    private final List<Button> shaderToggleButtons = new ArrayList<>();
    private final List<Button> resourceResetButtons = new ArrayList<>();
    private final List<Button> shaderResetButtons = new ArrayList<>();

    private final List<Button> headerButtons = new ArrayList<>();


    public String getModId(ModToggle mod) {
        File modFile = mod.getFile();

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
            e.printStackTrace();
        }
        return null;
    }

    public ModManagerScreen(Minecraft client, List<ModToggle> modToggles, List<ModToggle> resourceToggles, List<ModToggle> shaderToggles) {
        super(Component.literal("Simple Mod Manager"));
        this.client = client;
        this.modToggles = modToggles;
        this.resourceToggles = resourceToggles;
        this.shaderToggles = shaderToggles;
    }


    public String getMetadataSummaryForMod(ModToggle mod) {
        File modFile = mod.getFile();

        String modId = getModId(mod);
        Optional<ModContainer> containerOpt = FabricLoader.getInstance().getModContainer(modId);

        String modName;
        boolean jarName;
        if (containerOpt.isPresent()) {
            modName = containerOpt.get().getMetadata().getName();
            jarName = false;
        } else {
            modName = mod.getJarName(); // fallback to jar name if metadata is missing
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
                } else if (!jarName) { summary.append(modName + ":");

                    if (version != null) {
                        summary.append(" v").append(version);
                    }
                }
                return summary.toString().isEmpty() ? "No metadata" : summary.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error reading metadata";
        }
    }

    public String getExtraInfo(ModToggle mod) {
        File modFile = mod.getFile();
        String modName;
        modName = mod.getJarName(); // fallback to jar name (this is because tooltip needs full file)

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = jar.getJarEntry("fabric.mod.json");
            if (entry == null) return "No metadata";

            try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonElement je = JsonParser.parseReader(reader);
                if (!je.isJsonObject()) return "Invalid metadata";

                JsonObject root = je.getAsJsonObject();

                // Extract fields
                String version = root.has("version") ? root.get("version").getAsString() : null;
                // Authors can be array or string, handle both
                String authors = null;
                if (root.has("authors")) {
                    if (root.get("authors").isJsonArray()) {
                        authors = root.get("authors").getAsJsonArray().toString();
                    } else {
                        authors = root.get("authors").getAsString();
                    }
                }

                // Compose a summary string (truncate description for brevity)
                StringBuilder extraSummary = new StringBuilder();

                extraSummary.append(modName + ":");

                if (version != null) {
                    extraSummary.append(" v").append(version);
                }
                if (authors != null) {
                    extraSummary.append(" by ").append(authors);
                }

                return extraSummary.toString().isEmpty() ? "No metadata" : extraSummary.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error reading metadata";
        }
    }

    protected  void fillScreen(GuiGraphicsExtractor context){ context.fill(0, 0, this.width, this.height, 0xFF202020); }

    @Override
    protected void init() {
        ResourceUtils resourceUtil = new ResourceUtils(client);
        this.modToggleButtons.clear();
        this.shaderButtons.clear();
        this.resourceButtons.clear();

        this.modResetButtons.clear();
        this.shaderResetButtons.clear();
        this.resourceResetButtons.clear();
        this.headerButtons.clear();

        this.clearWidgets();

        int centerX = this.width / 2;
        int buttonWidth = 40;
        int buttonHeight = 20;
        int spacing = 25;
        int y = 10;

        // --- Mods ---


        this.searchBox = new EditBox(this.font, 0, 0, this.width, 20, Component.literal("Search"));
        this.searchBox.setResponder(query -> {
            this.searchQuery = query.toLowerCase();
            // Don't call updateVisibleButtons here, let render() handle it
        });
        this.searchBox.setMaxLength(100);
        this.searchBox.setEditable(true);
        this.addWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        ModToggle.initializeDefaultDisabledMods();

        int index = 0;

        Button modsHeader = Button.builder(Component.literal("Mods"),
                button -> {} // Do Nothing
        ).bounds(centerX, this.height - 50, 240, 20).build();

        modsHeader.active = false;  // Disable interaction
        headerButtons.add(modsHeader);
        addWidget(modsHeader);

        for (ModToggle toggle : modToggles) {
            String name = cleanName(toggle.getFile().getName());

            Button toggleFunc = Button.builder(Component.literal(toggle.getButtonText().getString()), button -> {
                toggle.toggle();
                button.setMessage(Component.literal(toggle.getButtonText().getString()));
            }).bounds(centerX + 10, y, buttonWidth+20, buttonHeight).build();
            addWidget(toggleFunc);
            modToggleButtons.add(toggleFunc);

            Button resetFunc = Button.builder(Component.literal("Reset"), button -> {
                toggle.resetToDefault();
                // Optionally update toggle button text here if needed
            }).bounds(centerX + 110, y, 60, buttonHeight).build();

            addWidget(resetFunc);
            modResetButtons.add(resetFunc);

            // Metadata button (left column)
            Button metadataFunc = Button.builder(
                    Component.literal(getMetadataSummaryForMod(modToggles.get(index))),
                    button -> {} // no action on click
            ).bounds(centerX, y, 180, buttonHeight).build();

            metadataFunc.active = false;  // disable interaction
            addWidget(metadataFunc); // add to screen
            modMetadataButtons.add(metadataFunc); // keep track of it

            // keep track of it



            String extraInfo = getExtraInfo(modToggles.get(index));

            // Store the tooltip string for later
            tooltipMap.put(metadataFunc, extraInfo);// tooltipMap is a Map<ButtonWidget, String>

            y += spacing;
            index += 1;
        }

        index = 0;
        //Index 3 = mods, index 4  = resource packs, index 5 = shader packs

        Button resourceHeader = Button.builder(Component.literal("Resource Packs"),
                button -> {} // Do Nothing
        ).bounds(centerX, this.height - 50, 240, 20).build();

        resourceHeader.active = false;  // Disable interaction
        headerButtons.add(resourceHeader);
        addWidget(resourceHeader);

        for (ModToggle toggle : resourceToggles) {
            Button resourceMetadataFunc = Button.builder(
                    Component.literal(resourceToggles.get(index).getFile().getName()),
                    button -> {
                    } // no action on click
            ).bounds(centerX, y, 180, buttonHeight).build();

            resourceMetadataFunc.active = false;  // disable interaction
            addWidget(resourceMetadataFunc); // add to screen
            resourceMetadataButtons.add(resourceMetadataFunc); // keep track of it

            Button resourceToggleFunc = Button.builder(Component.literal(toggle.getButtonText().getString()), button -> {
                toggle.toggle();
                resourceUtil.toggleResourcePack(toggle.getFile().getName(), toggle.isEnabled());
                button.setMessage(Component.literal(toggle.getButtonText().getString()));
            }).bounds(centerX + 10, y, buttonWidth + 20, buttonHeight).build();
            addWidget(resourceToggleFunc);
            resourceToggleButtons.add(resourceToggleFunc);


            y += spacing;
            index += 1;
        }

        index = 0;

        Button shaderHeader = Button.builder(Component.literal("Shader Packs"),
                button -> {} // Do Nothing
        ).bounds(centerX, this.height - 50, 240, 20).build();

        shaderHeader.active = false;  // Disable interaction
        headerButtons.add(shaderHeader);
        addWidget(shaderHeader);

        for (ModToggle toggle : shaderToggles) {
            Button shaderMetadataFunc = Button.builder(
                    Component.literal(shaderToggles.get(index).getFile().getName()),
                    button -> {
                    } // no action on click
            ).bounds(centerX, y, 180, buttonHeight).build();

            shaderMetadataFunc.active = false;  // disable interaction
            addWidget(shaderMetadataFunc); // add to screen
            shaderMetadataButtons.add(shaderMetadataFunc); // keep track of it

            Button shaderToggleFunc = Button.builder(Component.literal(toggle.getButtonText().getString()), button -> {
                toggle.toggle();
                resourceUtil.toggleShaderPack(toggle.getFile().getName(), toggle.isEnabled());
                button.setMessage(Component.literal(toggle.getButtonText().getString()));
            }).bounds(centerX + 10, y, buttonWidth + 20, buttonHeight).build();
            addWidget(shaderToggleFunc);
            shaderToggleButtons.add(shaderToggleFunc);


            y += spacing;
            index += 1;
        }

        Button resourceFunc = Button.builder(Component.literal("Refresh Resources"), b -> {
            Minecraft.getInstance().reloadResourcePacks();
            client.setScreen(null);
        }).bounds(centerX, y, 240, btnHeight).build();
        reloadButtons.add(resourceFunc);
        addWidget(resourceFunc);

        int contentHeight = y + 20;
        int buttonY = this.height - 50;

        Button applyFunc = Button.builder(Component.literal("Apply Changes"), button -> {
            for (ModToggle toggle : modToggles) toggle.applyChange();
            Minecraft.getInstance().reloadResourcePacks();
        }).bounds(centerX - 130, 10, 120, 20).build();
        headerButtons.add(applyFunc);
        addWidget(applyFunc);

        Button cancelFunc = Button.builder(Component.literal("Cancel"), button -> {
            client.setScreen(null);
        }).bounds(centerX + 10, 10, 120, 20).build();
        headerButtons.add(cancelFunc);
        addWidget(cancelFunc);

        tooltipMap.put(applyFunc, "Restart the game to apply changes to mod settings.");
        maxScroll = Math.max(0, contentHeight+200 /** change content height to scroll less/more on screen**/ - (this.height - 80));

    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        //Index 3 = mods, index 4  = resource packs, index 5 = shader packs (headers)
        context.fill(0, 0, this.width, this.height, 0xFF202020);

        // First, hide all buttons that don't match search
        updateVisibleButtonsBasedOnSearch();

        // Then render tooltips for visible buttons
        for (Map.Entry<Button, String> entry : tooltipMap.entrySet()) {
            Button button = entry.getKey();
            if (button.isHovered() && button.visible) {
                context.setComponentTooltipForNextFrame(
                        Minecraft.getInstance().font,
                        Collections.singletonList(Component.literal(entry.getValue())),
                        mouseX,
                        mouseY
                );
                break; // Show only one tooltip at a time
            }
        }

        int y = -(int)scrollAmount;
        int centerX = this.width / 2;
        int spacing = 25;


        int offset = 40;
        int col1X = centerX - 200 + offset;  // mod name + metadata
        int col2X = centerX + offset;  // toggle button only
        int col3X = centerX + 70 + offset;  // reset button

        y += 50;

        // Only render if there are visible mods or if search is empty
        boolean hasVisibleMods = hasVisibleItemsInCategory("mods");
        if (hasVisibleMods || searchQuery.isEmpty()) {
            Button modsHeader = headerButtons.getFirst();
            modsHeader.setX(centerX-120);
            modsHeader.setY(y);
            if (hasVisibleMods || searchQuery.isEmpty()) {
                modsHeader.extractRenderState(context, mouseX, mouseY, delta);
            }
            y += 25;
        }

        for (int i = 0; i < modToggles.size(); i++) {
            ModToggle toggle = modToggles.get(i);
            if (!toggle.getDisplayName().toLowerCase().contains(searchQuery)) continue;

            drawUtil.renderModIcon(modToggles.get(i), context, col1X - offset, y, 20);

            // Position and render toggle button
            Button toggleBtn = modToggleButtons.get(i);
            toggleBtn.setX(col2X);
            toggleBtn.setY(y);
            toggleBtn.extractRenderState(context, mouseX, mouseY, delta);

            // Position and render reset button
            Button resetBtn = modResetButtons.get(i);
            resetBtn.setX(col3X);
            resetBtn.setY(y);
            resetBtn.extractRenderState(context, mouseX, mouseY, delta);

            Button metadataBtn = modMetadataButtons.get(i);
            metadataBtn.setX(col1X);
            metadataBtn.setY(y);
            metadataBtn.extractRenderState(context, mouseX, mouseY, delta);

            y += 25; // spacing between rows
        }

        // Only render if there are visible resource packs or if search is empty
        boolean hasVisibleResources = hasVisibleItemsInCategory("resourcepacks");
        if (hasVisibleResources || searchQuery.isEmpty()) {
            y += 25;
            Button resourceHeader = headerButtons.get(1);
            resourceHeader.setX(centerX-120);
            resourceHeader.setY(y);
            resourceHeader.extractRenderState(context, mouseX, mouseY, delta);
            y += 25;
        }

        for (int i = 0; i < resourceToggles.size(); i++) {
            ModToggle toggle = resourceToggles.get(i);
            if (!toggle.getDisplayName().toLowerCase().contains(searchQuery)) continue;

            drawUtil.renderModIcon(resourceToggles.get(i), context, col1X - offset, y, 20);

            //Position and render toggle button
            Button resourceToggleBtn = resourceToggleButtons.get(i);
            resourceToggleBtn.setX(col2X+30);
            resourceToggleBtn.setY(y);
            resourceToggleBtn.extractRenderState(context, mouseX, mouseY, delta);

            Button resourceMetadataBtn = resourceMetadataButtons.get(i);
            resourceMetadataBtn.setX(col1X+30);
            resourceMetadataBtn.setY(y);
            resourceMetadataBtn.extractRenderState(context, mouseX, mouseY, delta);

            y += 25; // spacing between rows
        }

        // Only render if there are visible shader packs or if search is empty
        boolean hasVisibleShaders = hasVisibleItemsInCategory("shaderpacks");
        if (hasVisibleShaders || searchQuery.isEmpty()) {
            y += 25;
            Button shaderHeader = headerButtons.get(2);
            shaderHeader.setX(centerX-120);
            shaderHeader.setY(y);
            shaderHeader.extractRenderState(context, mouseX, mouseY, delta);
            y += 25;
        }

        for (int i = 0; i < shaderToggles.size(); i++) {
            ModToggle toggle = shaderToggles.get(i);
            if (!toggle.getDisplayName().toLowerCase().contains(searchQuery)) continue;

            drawUtil.renderModIcon(shaderToggles.get(i), context, col1X - offset, y, 20);

            // Position and render toggle button
            Button shaderToggleBtn = shaderToggleButtons.get(i);
            shaderToggleBtn.setX(col2X+30);
            shaderToggleBtn.setY(y);
            shaderToggleBtn.extractRenderState(context, mouseX, mouseY, delta);

            Button shaderMetadataBtn = shaderMetadataButtons.get(i);
            shaderMetadataBtn.setX(col1X+30);
            shaderMetadataBtn.setY(y);
            shaderMetadataBtn.extractRenderState(context, mouseX, mouseY, delta);

            y += 25; // spacing between rows
        }

        y += 25;

        Button resourceButton = reloadButtons.getFirst();
        resourceButton.setX(centerX-120);
        resourceButton.setY(y);
        resourceButton.extractRenderState(context, mouseX, mouseY, delta);

        y +=25;

        Button applyBtn = headerButtons.get(3);
        applyBtn.setX(centerX - 130);
        applyBtn.setY(y);
        applyBtn.extractRenderState(context, mouseX, mouseY, delta);

        Button cancelBtn = headerButtons.get(4);
        cancelBtn.setX(centerX + 10);
        cancelBtn.setY(y);
        cancelBtn.extractRenderState(context, mouseX, mouseY, delta);

        y += btnHeight + spacing;// move y down for any following content

        super.extractRenderState(context, mouseX, mouseY, delta);

        context.fillGradient(0, 20, this.width, 30, 0xC0000000, 0x00000000);
        context.fillGradient(0, this.height-10, this.width, this.height, 0x00000000, 0xC0000000);
        this.searchBox.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollAmount -= verticalAmount * scrollStep;
        scrollAmount = Math.max(0, Math.min(scrollAmount, maxScroll));
        return true;
    }

    private String cleanName(String filename) {
        return filename.replaceAll("\\.(jar|zip|json)$", "");
    }

    private boolean hasVisibleItemsInCategory(String category) {
        switch (category) {
            case "mods":
                for (ModToggle toggle : modToggles) {
                    if (toggle.getDisplayName().toLowerCase().contains(searchQuery)) {
                        return true;
                    }
                }
                return false;
            case "resourcepacks":
                for (ModToggle toggle : resourceToggles) {
                    if (toggle.getDisplayName().toLowerCase().contains(searchQuery)) {
                        return true;
                    }
                }
                return false;
            case "shaderpacks":
                for (ModToggle toggle : shaderToggles) {
                    if (toggle.getDisplayName().toLowerCase().contains(searchQuery)) {
                        return true;
                    }
                }
                return false;
            default:
                return false;
        }
    }

    private void updateVisibleButtonsBasedOnSearch() {
        // Update mod buttons visibility
        for (int i = 0; i < modToggles.size(); i++) {
            ModToggle toggle = modToggles.get(i);
            boolean shouldShow = toggle.getDisplayName().toLowerCase().contains(searchQuery);

            modToggleButtons.get(i).visible = shouldShow;
            modResetButtons.get(i).visible = shouldShow;
            modMetadataButtons.get(i).visible = shouldShow;
        }

        // Update resource pack buttons visibility
        for (int i = 0; i < resourceToggles.size(); i++) {
            ModToggle toggle = resourceToggles.get(i);
            boolean shouldShow = toggle.getDisplayName().toLowerCase().contains(searchQuery);

            resourceToggleButtons.get(i).visible = shouldShow;
            resourceMetadataButtons.get(i).visible = shouldShow;
        }

        // Update shader pack buttons visibility
        for (int i = 0; i < shaderToggles.size(); i++) {
            ModToggle toggle = shaderToggles.get(i);
            boolean shouldShow = toggle.getDisplayName().toLowerCase().contains(searchQuery);

            shaderToggleButtons.get(i).visible = shouldShow;
            shaderMetadataButtons.get(i).visible = shouldShow;
        }

        headerButtons.get(0).visible = hasVisibleItemsInCategory("mods") || searchQuery.isEmpty(); // Mods header
        headerButtons.get(1).visible = hasVisibleItemsInCategory("resourcepacks") || searchQuery.isEmpty(); // Resource packs header
        headerButtons.get(2).visible = hasVisibleItemsInCategory("shaderpacks") || searchQuery.isEmpty(); // Shader packs header
    }

    public void updateVisibleButtons(String type){
        if (type.equals("mods")){
            for (int i = 0; i < modToggles.size(); i++) {
                ModToggle toggle = modToggles.get(i);
                Button btn = modToggleButtons.get(i);
                btn.setMessage(toggle.getButtonText());
                btn.active = true;

            }
        } else if (type.equals("resourcepacks")){
            for (int i = 0; i < resourceToggles.size(); i++) {
                ModToggle toggle = resourceToggles.get(i);
                Button btn = resourceToggleButtons.get(i);
                btn.setMessage(toggle.getButtonText());
                btn.active = true;

            }
        } else if (type.equals("shaderpacks")){
            for (int i = 0; i < shaderToggles.size(); i++) {
                ModToggle toggle = shaderToggles.get(i);
                Button btn = shaderToggleButtons.get(i);
                btn.setMessage(toggle.getButtonText());
                btn.active = true;
            }
        } else if (type.equals("headers")){
            for (int i = 0; i < headerButtons.size(); i++) {
                Button btn = headerButtons.get(i);
                if (!(btn.active == true)){btn.active = false;}
                else if (btn.active == true){btn.active = true;}

            }
        }

        if (type.equals("all")){
            updateVisibleButtons("mods");
            updateVisibleButtons("resourcepacks");
            updateVisibleButtons("shaderpacks");
            updateVisibleButtons("headers");
        }
    }

}