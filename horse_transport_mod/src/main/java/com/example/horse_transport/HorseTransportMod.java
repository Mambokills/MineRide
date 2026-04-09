package com.example.horse_transport;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.resources.ResourceLocation;

@Mod(HorseTransportMod.MOD_ID)
public class HorseTransportMod {
    public static final String MOD_ID = "horse_transport";

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<EntityType<HorseTransportEntity>> HORSE_TRANSPORT = 
        ENTITY_TYPES.register("horse_transport", () -> 
            EntityType.Builder.<HorseTransportEntity>of(HorseTransportEntity::new, MobCategory.MISC)
                .sized(2.5f, 3.0f)
                .clientTrackingRange(10)
                .build(new ResourceLocation(MOD_ID, "horse_transport").toString()));

    public HorseTransportMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITY_TYPES.register(modEventBus);
    }
}
