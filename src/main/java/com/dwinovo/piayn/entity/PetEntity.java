package com.dwinovo.piayn.entity;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.dwinovo.piayn.client.resource.PIAYNLoader;
import com.mojang.logging.LogUtils;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PetEntity extends TamableAnimal implements GeoEntity{
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final EntityType<PetEntity> TYPE = EntityType.Builder.<PetEntity>of(PetEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.5f).build("pet");

    protected PetEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        // 只在服务端生成随机模型ID
        if (!level.isClientSide()) {
            // 在服务端生成随机模型ID，会通过网络同步到客户端
            this.entityData.set(MODEL_ID, PIAYNLoader.getRandomModelId());
        }
    }

    public static final EntityDataAccessor<String> MODEL_ID = SynchedEntityData.defineId(PetEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);

    @Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<PetEntity> main = new AnimationController<>(this, "main", 5, state -> {
            RawAnimation builder = RawAnimation.begin();
            builder.thenLoop("idle");
            state.setAndContinue(builder);
            return PlayState.CONTINUE;
        });

        AnimationController<PetEntity> sub = new AnimationController<>(this, "sub", 1, state -> {
            return PlayState.CONTINUE;
        });

        controllers.add(main, sub);
	}
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODEL_ID, "");
    }
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("model_id", getModelID());
        LOGGER.debug("PetEntity addAdditionalSaveData model_id: {}", getModelID());
    }
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("model_id", Tag.TAG_STRING)) {
            setModelID(compound.getString("model_id"));
            LOGGER.debug("PetEntity readAdditionalSaveData model_id: {}", getModelID());
        }
    }

    @Override
    public void tick() {
        super.tick();
    }
    

    @Override
    public boolean isFood(@NotNull ItemStack stack) {
        
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob otherParent) {
        return null;
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }
    
    /**
     * 返回模型ID
     * @return 模型ID
     */
    public String getModelID() {
        return this.entityData.get(MODEL_ID);
    }
    
    /**
     * 设置模型ID
     * @param modelID 模型ID
     */
    public void setModelID(String modelID) {
        this.entityData.set(MODEL_ID, modelID);
    }
    /**
     * 返回模型缓存
     * @return 模型缓存
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
}
