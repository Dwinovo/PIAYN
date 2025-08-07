package com.dwinovo.piayn.entity.container.impl;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.core.HolderLookup;
import org.slf4j.Logger;

import com.dwinovo.piayn.entity.container.IContainerManipulatable;
import com.mojang.logging.LogUtils;

/**
 * 宠物容器处理器
 * 负责管理宠物实体的存储容器，提供完整的容器操作能力
 */
public class PetContainerHandler implements IContainerManipulatable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_CONTAINER_SIZE = 16;
    private static final String NBT_CONTAINER_KEY = "PetContainer";
    
    private final SimpleContainer container;
    private final int containerCapacity;
    
    /**
     * 构造指定容量的宠物容器处理器
     * @param capacity 容器容量
     */
    public PetContainerHandler(int capacity) {
        this.containerCapacity = capacity;
        this.container = new SimpleContainer(capacity);
    }
    
    /**
     * 构造默认容量的宠物容器处理器
     */
    public PetContainerHandler() {
        this(DEFAULT_CONTAINER_SIZE);
    }
    
    @Override
    public SimpleContainer getContainer() {
        return container;
    }
    
    @Override
    public int getContainerCapacity() {
        return containerCapacity;
    }
    
    @Override
    public void serializeToNBT(CompoundTag compound, HolderLookup.Provider registryAccess) {
        try {
            NonNullList<ItemStack> containerItems = createContainerSnapshot();
            CompoundTag containerTag = new CompoundTag();
            ContainerHelper.saveAllItems(containerTag, containerItems, registryAccess);
            compound.put(NBT_CONTAINER_KEY, containerTag);
        
        } catch (Exception e) {
            LOGGER.error("Failed to serialize pet container to NBT", e);
        }
    }
    
    @Override
    public void deserializeFromNBT(CompoundTag compound, HolderLookup.Provider registryAccess) {
        try {
            if (compound.contains(NBT_CONTAINER_KEY)) {
                CompoundTag containerTag = compound.getCompound(NBT_CONTAINER_KEY);
                NonNullList<ItemStack> containerItems = NonNullList.withSize(containerCapacity, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(containerTag, containerItems, registryAccess);
                restoreContainerFromSnapshot(containerItems);
                
            }
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize pet container from NBT", e);
        }
    }
    
    @Override
    public boolean canPlaceItemInSlot(ItemStack stack, int slot) {
        // 可以在这里添加宠物特定的物品放置规则
        if (slot < 0 || slot >= containerCapacity || stack.isEmpty()) {
            return false;
        }
        
        // 示例：某些特殊物品可能有放置限制
        // if (stack.is(Items.NETHERITE_INGOT) && slot < 4) {
        //     return false; // 贵重物品只能放在后面的槽位
        // }
        
        return true;
    }
    
    /**
     * 创建当前容器的快照
     * @return 容器物品列表
     */
    private NonNullList<ItemStack> createContainerSnapshot() {
        NonNullList<ItemStack> snapshot = NonNullList.withSize(containerCapacity, ItemStack.EMPTY);
        for (int i = 0; i < containerCapacity; i++) {
            snapshot.set(i, container.getItem(i).copy());
        }
        return snapshot;
    }
    
    /**
     * 从快照恢复容器
     * @param snapshot 容器快照
     */
    private void restoreContainerFromSnapshot(NonNullList<ItemStack> snapshot) {
        for (int i = 0; i < Math.min(snapshot.size(), containerCapacity); i++) {
            container.setItem(i, snapshot.get(i));
        }
    }
    
    /**
     * 获取容器利用率
     * @return 利用率百分比 (0.0 - 1.0)
     */
    public double getContainerUtilization() {
        return (double) getOccupiedSlotCount() / containerCapacity;
    }
    
    /**
     * 检查是否有足够空间放置物品
     * @param stack 要检查的物品
     * @return 是否有足够空间
     */
    public boolean hasSpaceFor(ItemStack stack) {
        return !tryAddItem(stack.copy()).equals(stack);
    }
    
    /**
     * 获取指定物品的总数量
     * @param stack 要查找的物品
     * @return 总数量
     */
    public int getTotalCountOf(ItemStack stack) {
        return countItemsWhere(containerStack -> 
            ItemStack.isSameItemSameComponents(containerStack, stack));
    }
    
    /**
     * 压缩容器（整理相同物品）
     * @return 是否进行了压缩
     */
    public boolean compactContainer() {
        boolean changed = false;
        
        for (int i = 0; i < containerCapacity - 1; i++) {
            ItemStack currentStack = container.getItem(i);
            if (currentStack.isEmpty()) continue;
            
            for (int j = i + 1; j < containerCapacity; j++) {
                ItemStack otherStack = container.getItem(j);
                if (ItemStack.isSameItemSameComponents(currentStack, otherStack)) {
                    int maxStackSize = Math.min(currentStack.getMaxStackSize(), container.getMaxStackSize());
                    int canMerge = maxStackSize - currentStack.getCount();
                    int willMerge = Math.min(canMerge, otherStack.getCount());
                    
                    if (willMerge > 0) {
                        currentStack.grow(willMerge);
                        otherStack.shrink(willMerge);
                        
                        if (otherStack.isEmpty()) {
                            container.setItem(j, ItemStack.EMPTY);
                        }
                        
                        changed = true;
                    }
                }
            }
        }
        
        if (changed) {
            LOGGER.debug("Container compacted successfully");
        }
        
        return changed;
    }
}
