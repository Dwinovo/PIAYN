package com.dwinovo.piayn.schem.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import com.dwinovo.piayn.schem.SchemSerializer;

/**
 * 实体处理工具类
 * 处理实体的构建、解析和粘贴操作
 */
public class EntityUtil {
    
    /**
     * 构建指定区域内的实体列表
     */
    @Nonnull
    public static ListTag buildEntitiesList(@Nonnull Level level, 
                                           @Nonnull BlockPos originPos, 
                                           int width, int height, int length) {
        
        ListTag entitiesList = new ListTag();
        
        // 定义搜索区域
        AABB searchArea = new AABB(
            originPos.getX(), originPos.getY(), originPos.getZ(),
            originPos.getX() + width, originPos.getY() + height, originPos.getZ() + length
        );
        
        // 获取区域内的所有实体
        for (Entity entity : level.getEntitiesOfClass(Entity.class, searchArea)) {
            CompoundTag entityTag = createEntityTag(entity, originPos);
            if (entityTag != null) {
                entitiesList.add(entityTag);
            }
        }
        
        return entitiesList;
    }
    
    /**
     * 创建实体NBT标签
     */
    @Nullable
    public static CompoundTag createEntityTag(@Nonnull Entity entity, @Nonnull BlockPos originPos) {
        try {
            CompoundTag entityTag = new CompoundTag();
            
            // 获取实体类型ID
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            
            entityTag.putString("Id", entityId.toString());
            
            // 计算相对位置
            double relativeX = entity.getX() - originPos.getX();
            double relativeY = entity.getY() - originPos.getY();
            double relativeZ = entity.getZ() - originPos.getZ();
            
            ListTag positionList = new ListTag();
            positionList.add(DoubleTag.valueOf(relativeX));
            positionList.add(DoubleTag.valueOf(relativeY));
            positionList.add(DoubleTag.valueOf(relativeZ));
            entityTag.put("Pos", positionList);
            
            // 保存实体数据
            CompoundTag entityData = new CompoundTag();
            entity.save(entityData);
            
            // 移除位置信息，因为我们使用相对位置
            entityData.remove("Pos");
            entityData.remove("UUID"); // 移除UUID，避免冲突
            
            if (!entityData.isEmpty()) {
                entityTag.put("Data", entityData);
            }
            
            return entityTag;
            
        } catch (Exception e) {
            System.err.println("Failed to create entity tag: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 粘贴实体到世界中
     */
    public static int pasteEntities(@Nonnull ServerLevel serverLevel,
                                   @Nonnull CompoundTag schematicTag,
                                   @Nonnull BlockPos targetPos) {
        
        if (!schematicTag.contains(SchemSerializer.ENTITIES_KEY)) {
            return 0;
        }
        
        ListTag entitiesList = schematicTag.getList(SchemSerializer.ENTITIES_KEY, Tag.TAG_COMPOUND);
        int entitiesPlaced = 0;
        
        for (Tag entityTag : entitiesList) {
            if (entityTag instanceof CompoundTag entityCompound) {
                try {
                    if (pasteEntity(serverLevel, entityCompound, targetPos)) {
                        entitiesPlaced++;
                    }
                } catch (Exception e) {
                    System.err.println("Failed to paste entity: " + e.getMessage());
                }
            }
        }
        
        return entitiesPlaced;
    }
    
    /**
     * 粘贴单个实体
     */
    public static boolean pasteEntity(@Nonnull ServerLevel serverLevel,
                                     @Nonnull CompoundTag entityCompound,
                                     @Nonnull BlockPos targetPos) throws Exception {
        
        if (!entityCompound.contains("Pos") || !entityCompound.contains("Id")) {
            return false;
        }
        
        // 获取实体类型
        String entityId = entityCompound.getString("Id");
        ResourceLocation entityResourceLocation = ResourceLocation.parse(entityId);
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityResourceLocation);
        
        // 获取相对位置
        ListTag positionList = entityCompound.getList("Pos", Tag.TAG_DOUBLE);
        if (positionList.size() != 3) {
            return false;
        }
        
        double relativeX = positionList.getDouble(0);
        double relativeY = positionList.getDouble(1);
        double relativeZ = positionList.getDouble(2);
        
        double absoluteX = targetPos.getX() + relativeX;
        double absoluteY = targetPos.getY() + relativeY;
        double absoluteZ = targetPos.getZ() + relativeZ;
        
        // 创建实体
        Entity entity = entityType.create(serverLevel);
        if (entity == null) {
            return false;
        }
        
        // 设置位置
        entity.setPos(absoluteX, absoluteY, absoluteZ);
        
        // 加载实体数据
        if (entityCompound.contains("Data")) {
            CompoundTag entityData = entityCompound.getCompound("Data");
            entity.load(entityData);
        }
        
        // 添加到世界
        return serverLevel.addFreshEntity(entity);
    }
    
    private EntityUtil() {
        // 工具类，禁止实例化
    }
}
