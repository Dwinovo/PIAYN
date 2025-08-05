package com.dwinovo.piayn.entity.container;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup;

/**
 * 容器提供者接口
 * 定义实体容器的基本访问和持久化能力
 */
public interface IContainerProvider {
    
    /**
     * 获取容器实例
     * @return 容器实例
     */
    SimpleContainer getContainer();
    
    /**
     * 获取容器容量
     * @return 容器槽位数量
     */
    int getContainerCapacity();
    
    /**
     * 将容器数据序列化到NBT
     * @param compound NBT复合标签
     * @param registryAccess 注册表访问器
     */
    void serializeToNBT(CompoundTag compound, HolderLookup.Provider registryAccess);
    
    /**
     * 从NBT反序列化容器数据
     * @param compound NBT复合标签
     * @param registryAccess 注册表访问器
     */
    void deserializeFromNBT(CompoundTag compound, HolderLookup.Provider registryAccess);
    
    /**
     * 检查物品是否可放置在指定槽位
     * @param stack 物品堆
     * @param slot 槽位索引
     * @return 是否可放置
     */
    default boolean canPlaceItemInSlot(ItemStack stack, int slot) {
        return slot >= 0 && slot < getContainerCapacity() && !stack.isEmpty();
    }
    
    /**
     * 清空容器所有内容
     */
    default void clearAllContents() {
        getContainer().clearContent();
    }
}
