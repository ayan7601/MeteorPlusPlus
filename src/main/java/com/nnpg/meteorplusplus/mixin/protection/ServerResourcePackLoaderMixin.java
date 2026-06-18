package com.nnpg.meteorplusplus.mixin.protection;

import net.minecraft.client.resource.server.ServerResourcePackLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URL;
import java.nio.file.Path;
import java.util.UUID;

@Mixin(ServerResourcePackLoader.class)
public class ServerResourcePackLoaderMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("MeteorPlusPlus-Protection");

    @Inject(
        method = "addResourcePack(Ljava/util/UUID;Ljava/net/URL;Ljava/lang/String;)V",
        at = @At("HEAD"),
        require = 0
    )
    private void meteorplusplus$onServerPackUrlLoad(UUID id, URL url, String hash, CallbackInfo ci) {
        try {
            LOGGER.info("[MeteorPlusPlus Protection] Server resource pack loading: {}", id);
        } catch (Throwable t) {
            LOGGER.error("[MeteorPlusPlus Protection] Error in server pack load", t);
        }
    }

    @Inject(
        method = "addResourcePack(Ljava/util/UUID;Ljava/net/URL;Ljava/lang/String;)V",
        at = @At("RETURN"),
        require = 0
    )
    private void meteorplusplus$afterServerPackUrlLoad(UUID id, URL url, String hash, CallbackInfo ci) {
        try {
            LOGGER.info("[MeteorPlusPlus Protection] Server resource pack load complete");
        } catch (Throwable t) {
            LOGGER.error("[MeteorPlusPlus Protection] Error after server pack load", t);
        }
    }

    @Inject(
        method = "addResourcePack(Ljava/util/UUID;Ljava/nio/file/Path;)V",
        at = @At("HEAD"),
        require = 0
    )
    private void meteorplusplus$onServerPackFileLoad(UUID id, Path path, CallbackInfo ci) {
        try {
            LOGGER.info("[MeteorPlusPlus Protection] Server resource pack loading: {}", id);
        } catch (Throwable t) {
            LOGGER.error("[MeteorPlusPlus Protection] Error in server pack load", t);
        }
    }

    @Inject(
        method = "addResourcePack(Ljava/util/UUID;Ljava/nio/file/Path;)V",
        at = @At("RETURN"),
        require = 0
    )
    private void meteorplusplus$afterServerPackFileLoad(UUID id, Path path, CallbackInfo ci) {
        try {
            LOGGER.info("[MeteorPlusPlus Protection] Server resource pack load complete");
        } catch (Throwable t) {
            LOGGER.error("[MeteorPlusPlus Protection] Error after server pack load", t);
        }
    }
}
