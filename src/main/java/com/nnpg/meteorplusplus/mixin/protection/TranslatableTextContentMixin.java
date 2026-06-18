package com.nnpg.meteorplusplus.mixin.protection;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nnpg.meteorplusplus.protection.PacketContext;
import com.nnpg.meteorplusplus.protection.TranslationProtectionHandler;
import com.nnpg.meteorplusplus.protection.TranslationProtectionHandler.InterceptionType;
import com.nnpg.meteorplusplus.protection.ModRegistry;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(value = TranslatableTextContent.class, priority = 1500)
public abstract class TranslatableTextContentMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("MeteorPlusPlus-Protection");

    @Shadow @Final private String key;
    @Shadow @Final private String fallback;

    @Unique
    private boolean meteorplusplus$fromPacket = false;

    @Inject(method = "<init>(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V", at = @At("TAIL"), require = 0)
    private void meteorplusplus$tagFromPacket(String key, String fallback, Object[] args, CallbackInfo ci) {
        try {
            this.meteorplusplus$fromPacket = PacketContext.isProcessingPacket();
            if (this.meteorplusplus$fromPacket) {
                LOGGER.info("[MeteorPlusPlus-Debug] TranslatableTextContent created from packet: {} | key='{}' fallback='{}'",
                    PacketContext.getPacketName(), key, fallback);
            }
        } catch (Throwable t) {

            this.meteorplusplus$fromPacket = false;
        }
    }

    @Unique
    private static final String GLAZED_ALLOW_ORIGINAL = "\0__meteorplusplus_allow__";

    @WrapOperation(
        method = "updateTranslations()V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Language;get(Ljava/lang/String;)Ljava/lang/String;"),
        require = 0
    )
    private String meteorplusplus$wrapGetSingle(Language instance, String keyArg, Operation<String> original) {

        if (!this.meteorplusplus$fromPacket) {
            return original.call(instance, keyArg);
        }

        String result = meteorplusplus$handleTranslationLookup(keyArg, keyArg);
        if (result == GLAZED_ALLOW_ORIGINAL) {
            return original.call(instance, keyArg);
        }
        return result;
    }

    @WrapOperation(
        method = "updateTranslations()V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Language;get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"),
        require = 0
    )
    private String meteorplusplus$wrapGet(Language instance, String keyArg, String fallbackArg, Operation<String> original) {

        if (!this.meteorplusplus$fromPacket) {
            return original.call(instance, keyArg, fallbackArg);
        }

        String result = meteorplusplus$handleTranslationLookup(keyArg, fallbackArg);
        if (result == GLAZED_ALLOW_ORIGINAL) {
            return original.call(instance, keyArg, fallbackArg);
        }
        return result;
    }

    @Unique
    private String meteorplusplus$handleTranslationLookup(String translationKey, String defaultValue) {

        try {

            if (!this.meteorplusplus$fromPacket || meteorplusplus$isIntegratedServerRunning()) {
                return GLAZED_ALLOW_ORIGINAL;
            }
        } catch (Throwable t) {

            return GLAZED_ALLOW_ORIGINAL;
        }

        if (ModRegistry.isVanillaTranslationKey(translationKey)) {
            return GLAZED_ALLOW_ORIGINAL;
        }

        if (ModRegistry.isServerPackTranslationKey(translationKey)) {
            return GLAZED_ALLOW_ORIGINAL;
        }

        String blockedValue = defaultValue;
        meteorplusplus$logBlocked(translationKey, blockedValue);
        return blockedValue;
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
    private void meteorplusplus$logBlocked(String translationKey, String defaultValue) {
        String originalValue = meteorplusplus$getRealTranslation(translationKey, defaultValue);

        TranslationProtectionHandler.logDetection(InterceptionType.TRANSLATION, translationKey, originalValue, defaultValue);
    }

    @Unique
    private String meteorplusplus$getRealTranslation(String translationKey, String defaultValue) {
        try {
            Language lang = Language.getInstance();
            if (lang instanceof TranslationStorageAccessor accessor) {
                Map<String, String> translations = accessor.meteorplusplus$getTranslations();
                String value = translations.get(translationKey);
                return value != null ? value : defaultValue;
            }
        } catch (Exception e) {

        }
        return defaultValue;
    }
}
