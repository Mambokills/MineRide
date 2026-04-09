package com.example.horse_transport;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class HorseTransportEntity extends Entity {
    private static final EntityDataAccessor<Boolean> HAS_ANIMAL = 
        SynchedEntityData.defineId(HorseTransportEntity.class, EntityDataSerializers.BOOLEAN);
    
    private static final EntityDataAccessor<Boolean> IS_DRIVING = 
        SynchedEntityData.defineId(HorseTransportEntity.class, EntityDataSerializers.BOOLEAN);

    // Store animal data when captured
    private CompoundTag animalData = null;
    private String animalType = "";
    private double animalRelX = 0;
    private double animalRelY = 0;
    private double animalRelZ = 0;

    public HorseTransportEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(HAS_ANIMAL, false);
        this.entityData.define(IS_DRIVING, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("AnimalData", 10)) {
            animalData = tag.getCompound("AnimalData");
        }
        if (tag.contains("AnimalType")) {
            animalType = tag.getString("AnimalType");
        }
        if (tag.contains("HasAnimal")) {
            entityData.set(HAS_ANIMAL, tag.getBoolean("HasAnimal"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (animalData != null) {
            tag.put("AnimalData", animalData);
        }
        tag.putString("AnimalType", animalType);
        tag.putBoolean("HasAnimal", entityData.get(HAS_ANIMAL));
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            boolean hasAnimal = entityData.get(HAS_ANIMAL);
            
            if (!hasAnimal) {
                // Try to pick up an animal from nearby
                Entity nearbyAnimal = findNearbyAnimal();
                if (nearbyAnimal != null) {
                    captureAnimal(nearbyAnimal);
                    return InteractionResult.SUCCESS;
                } else {
                    // Enter as driver
                    if (!player.isSecondaryUseActive()) {
                        player.startRiding(this);
                        entityData.set(IS_DRIVING, true);
                        return InteractionResult.SUCCESS;
                    }
                }
            } else {
                // Release the animal
                releaseAnimal();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private Entity findNearbyAnimal() {
        double range = 5.0;
        for (Entity entity : level().getEntities(this, getBoundingBox().inflate(range))) {
            if (entity instanceof Mob mob && !(entity instanceof HorseTransportEntity)) {
                if (!mob.isPassenger() && !mob.isVehicle()) {
                    return entity;
                }
            }
        }
        return null;
    }

    private void captureAnimal(Entity animal) {
        // Save animal data
        animalData = new CompoundTag();
        animal.saveWithoutId(animalData);
        
        // Get entity type ID
        animalType = EntityType.getKey(animal.getType()).toString();
        
        // Store relative position (inside the transport)
        animalRelX = 0.0;
        animalRelY = 0.5;  // Slightly above floor
        animalRelZ = -0.5; // Center of transport
        
        entityData.set(HAS_ANIMAL, true);
        
        // Remove animal from world
        animal.discard();
    }

    private void releaseAnimal() {
        if (animalType.isEmpty() || animalData == null) {
            entityData.set(HAS_ANIMAL, false);
            return;
        }
        
        // Spawn the animal back
        BlockPos spawnPos = blockPosition().below();
        Vec3 spawnVec = new Vec3(
            getX() + animalRelX,
            getY() + animalRelY,
            getZ() + animalRelZ
        );
        
        // Create entity from saved type
        EntityType.byString(animalType).ifPresent(type -> {
            Entity newAnimal = type.create(level());
            if (newAnimal != null) {
                newAnimal.moveTo(spawnVec.x, spawnVec.y, spawnVec.z, getYRot(), 0);
                
                // Load saved data
                if (animalData != null) {
                    newAnimal.load(animalData);
                }
                
                level().addFreshEntity(newAnimal);
            }
        });
        
        // Clear stored data
        animalData = null;
        animalType = "";
        entityData.set(HAS_ANIMAL, false);
    }

    @Override
    public void tick() {
        super.tick();
        
        if (!level().isClientSide) {
            // Handle driving controls
            if (entityData.get(IS_DRIVING) && getFirstPassenger() instanceof Player player) {
                float forward = 0.0f;
                float sideways = 0.0f;
                
                if (player.input.up()) forward += 0.6f;
                if (player.input.down()) forward -= 0.3f;
                if (player.input.left()) sideways += 0.5f;
                if (player.input.right()) sideways -= 0.5f;
                
                // Apply movement
                Vec3 motion = getDeltaMovement();
                Vec3 forwardVec = getForward();
                Vec3 rightVec = getRight();
                
                double newX = motion.x + (rightVec.x * sideways + forwardVec.x * forward) * 0.1;
                double newZ = motion.z + (rightVec.z * sideways + forwardVec.z * forward) * 0.1;
                
                setDeltaMovement(newX, motion.y, newZ);
                move(MoverType.SELF, getDeltaMovement());
                
                // Apply friction
                setDeltaMovement(getDeltaMovement().multiply(0.9, 1.0, 0.9));
                
                // Update rotation based on movement
                if (forward != 0 || sideways != 0) {
                    float targetYaw = getYRot() + (sideways * 3.0f);
                    setYRot(targetYaw);
                    setYBodyRot(targetYaw);
                }
            }
        }
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        // Only allow one passenger (the driver)
        return getPassengers().isEmpty();
    }

    @Override
    protected boolean canRide(Entity entity) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
