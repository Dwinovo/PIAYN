package com.dwinovo.piayn.entity;


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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PetEntity extends TamableAnimal implements GeoEntity{

    public static final EntityType<PetEntity> TYPE = EntityType.Builder.<PetEntity>of(PetEntity::new, MobCategory.CREATURE)
            .sized(0.6f, 1.5f).build("pet");

    protected PetEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
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
    
}
