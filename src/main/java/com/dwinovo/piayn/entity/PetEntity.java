package com.dwinovo.piayn.entity;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import com.dwinovo.piayn.client.resource.ClientModelDataManager;
import com.dwinovo.piayn.entity.container.impl.PetContainerHandler;
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
            .sized(0.6f, 0.6f).build("pet");

    protected PetEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        // 初始化宠物容器处理器
        this.containerHandler = new PetContainerHandler();
        // 只在服务端生成随机模型ID
        if (!level.isClientSide()) {
            // 在服务端生成随机模型ID，会通过网络同步到客户端
            this.entityData.set(MODEL_ID, ClientModelDataManager.getRandomModelId());
        }
    }

    public static final EntityDataAccessor<String> MODEL_ID = SynchedEntityData.defineId(PetEntity.class, EntityDataSerializers.STRING);

    // 宠物容器处理器
    private final PetContainerHandler containerHandler;
    
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
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            if (this.level() instanceof ServerLevel) {
                player.openMenu(this.createMenuProvider(), buf -> buf.writeInt(this.getId()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        
        super.tick();
        LOGGER.debug("mainHandItem: {}", this.getMainHandItem());
    }

    /**
     * 创建菜单提供者
     * @return 菜单提供者
     */
    public MenuProvider createMenuProvider() {
        return new SimpleMenuProvider(
            (containerId, playerInventory, player) -> 
                new com.dwinovo.piayn.client.gui.screen.container.PetContainerMenu(containerId, playerInventory, this),
            Component.literal(ClientModelDataManager.getModelNameById(this.getModelID()))
        );
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
        // 保存宠物容器数据
        containerHandler.serializeToNBT(compound, this.registryAccess());
    }
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("model_id", Tag.TAG_STRING)) {
            setModelID(compound.getString("model_id"));
        }
        // 读取宠物容器数据
        containerHandler.deserializeFromNBT(compound, this.registryAccess());
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
     * 获取宠物的存储容器
     * @return 宠物存储容器
     */
    public SimpleContainer getContainer() {
        return containerHandler.getContainer();
    }
    
    /**
     * 获取容器处理器
     * @return 容器处理器实例
     */
    public PetContainerHandler getContainerHandler() {
        return containerHandler;
    }
    
    /**
     * 获取主手物品
     * @return 主手物品
     */
    public ItemStack getMainHandItem() {
        return this.getItemBySlot(EquipmentSlot.MAINHAND);
    }
    
    /**
     * 设置主手物品
     * @param itemStack 要设置的物品
     */
    public void setMainHandItem(ItemStack itemStack) {
        this.setItemSlot(EquipmentSlot.MAINHAND, itemStack);
        // 同步到容器第0号槽
        if (this.containerHandler != null && this.containerHandler.getContainer() != null) {
            this.containerHandler.getContainer().setItem(0, itemStack.copy());
        }
    }
    
    
    

    /**
     * 返回模型缓存
     * @return 模型缓存
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
    /**
     * 不可繁殖
     */
    @Override
    public boolean isFood(@NotNull ItemStack stack) {
        return false;
    }

    /**
     * 没有成长
     */
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob otherParent) {
        return null;
    }
}
