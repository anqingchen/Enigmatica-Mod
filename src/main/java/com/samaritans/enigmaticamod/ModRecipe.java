package com.samaritans.enigmaticamod;

import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ObjectHolder;

@Mod.EventBusSubscriber(modid = Enigmatica.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
@ObjectHolder(Enigmatica.MODID)
public class ModRecipe {

    public static final IRecipeSerializer<ShapelessToolRecipe> STRIPPING_SHAPELESS = null;

    @SubscribeEvent
    public static void onRegisterRecipeSerializers(RegistryEvent.Register<IRecipeSerializer<?>> event) {
        event.getRegistry().registerAll(
                Util.setup(new ShapelessToolRecipe.Serializer(), "tool_shapeless")
        );
    }
}