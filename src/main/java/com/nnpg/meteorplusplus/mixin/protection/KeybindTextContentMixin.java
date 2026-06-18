package com.nnpg.meteorplusplus.mixin.protection;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nnpg.meteorplusplus.protection.PacketContext;
import com.nnpg.meteorplusplus.protection.TranslationProtectionHandler;
import com.nnpg.meteorplusplus.protection.TranslationProtectionHandler.InterceptionType;
import com.nnpg.meteorplusplus.protection.KeybindDefaults;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.KeybindTextContent;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(KeybindTextContent.class)
public class KeybindTextContentMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("MeteorPlusPlus-Protection");

    @Shadow @Final
    private String key;

    @Unique
    private boolean meteorplusplus$fromPacket = false;

    @Inject(method = "<init>(Ljava/lang/String;)V", at = @At("TAIL"))
    private void meteorplusplus$tagFromPacket(String key, CallbackInfo ci) {
        this.meteorplusplus$fromPacket = PacketContext.isProcessingPacket();
    }

    @WrapOperation(
        method = "getTranslated",
        at = @At(value = "INVOKE", target = "Ljava/util/function/Supplier;get()Ljava/lang/Object;"),
        require = 0
    )
    private Object meteorplusplus$interceptKeybind(Supplier<?> supplier, Operation<Object> original) {
        try {
            if (!this.meteorplusplus$fromPacket || meteorplusplus$isIntegratedServerRunning()) {
                return original.call(supplier);
            }
        } catch (Throwable t) {

            return original.call(supplier);
        }

        if (KeybindDefaults.hasDefault(key)) {
            String spoofedValue = KeybindDefaults.getDefault(key);
            meteorplusplus$logBlocked(key, spoofedValue);
            return Text.literal(spoofedValue);
        }

        meteorplusplus$logBlocked(key, key);
        return Text.translatable(key);
    }

    @Unique
    private static boolean meteorplusplus$isIntegratedServerRunning() {
        try {

            return net.minecraft.client.MinecraftClient.getInstance().isIntegratedServerRunning();
        } catch (Exception e) {
            return false;
        }
    }

    @Unique
    private void meteorplusplus$logBlocked(String keybindName, String spoofedValue) {
        String realValue = meteorplusplus$readKeybindDisplay();

        TranslationProtectionHandler.logDetection(InterceptionType.KEYBIND, keybindName, realValue, spoofedValue);
    }

    @Unique
    private String meteorplusplus$readKeybindDisplay() {
        try {
            Text display = KeyBinding.getLocalizedName(key).get();
            if (display != null) {
                return display.getString();
            }
        } catch (Exception e) {
            LOGGER.info("[MeteorPlusPlus Protection] Failed to read keybind '{}': {}", key, e.getMessage());
        }
        return key;
    }
}
