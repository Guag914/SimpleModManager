package net.guag.simplemodmanager.mixin;

import net.guag.simplemodmanager.screen.ModManagerScreen;
import net.guag.simplemodmanager.util.ModUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class LoginWidget extends Screen {

    protected LoginWidget(Component title) {
        super(title);
    }


//    @Inject(method = "init", at = @At("RETURN"))
//    private void addOpenGUIButton(int y, int spacingY, CallbackInfo ci) {
//        // 'this' here refers to the TitleScreen instance at runtime
//        TitleScreen self = (TitleScreen)(Object)this;
//        addWidget(
//                Button.builder(
//                                Component.translatable("button.SimpleModManager.opengui"),
//                                button -> Minecraft.getInstance().setScreen(
//                                        new ModManagerScreen(Minecraft.getInstance(),
//                                                ModUtils.getModToggles(),
//                                                ModUtils.getResourceToggles(),
//                                                ModUtils.getShaderToggles())))
//                        .bounds(self.width / 2 - 100 + 205, y, 20, 20)
//                        .build()
//        );
//    }
}
