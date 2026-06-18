package com.nnpg.meteorplusplus.mixins;

import meteordevelopment.meteorclient.gui.utils.SettingsWidgetFactory;
import meteordevelopment.meteorclient.settings.Setting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = SettingsWidgetFactory.class, remap = false)
public interface DefaultSettingsWidgetFactoryAccessor {
    @Accessor(value = "factories", remap = false)
    Map<Class<?>, SettingsWidgetFactory.Factory> getFactories();
}
