package com.github.eterdelta.broomsmod;

import com.github.eterdelta.broomsmod.client.renderer.WoodenBroomRenderer;
import com.github.eterdelta.broomsmod.registry.BroomsEnchantments;
import com.github.eterdelta.broomsmod.registry.BroomsEntities;
import com.github.eterdelta.broomsmod.registry.BroomsItems;
import com.github.eterdelta.broomsmod.registry.BroomsSounds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BroomsMod.MODID)
public class BroomsMod {
    public static final String MODID = "broomsmod";

    public BroomsMod() {
        final IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        eventBus.addListener(this::setupClient);

        BroomsEnchantments.ENCHANTMENTS.register(eventBus);
        BroomsEntities.ENTITIES.register(eventBus);
        BroomsItems.ITEMS.register(eventBus);
        BroomsSounds.SOUND_EVENTS.register(eventBus);
    }

    public void setupClient(final FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(BroomsEntities.WOODEN_BROOM.get(), WoodenBroomRenderer::new);
    }
}
