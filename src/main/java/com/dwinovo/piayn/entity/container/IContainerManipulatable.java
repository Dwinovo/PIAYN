package com.dwinovo.piayn.entity.container;

import net.minecraft.world.item.ItemStack;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * 容器可操作接口
 * 定义容器的高级操作和查询能力
 */
public interface IContainerManipulatable extends IContainerProvider {
    
    /**
     * 查找第一个满足条件的槽位
     * @param predicate 槽位物品匹配条件
     * @return 槽位索引，未找到返回-1
     */
    default int findFirstSlotWhere(Predicate<ItemStack> predicate) {
        return IntStream.range(0, getContainerCapacity())
            .filter(i -> predicate.test(getContainer().getItem(i)))
            .findFirst()
            .orElse(-1);
    }
    
    /**
     * 查找第一个空槽位
     * @return 空槽位索引，未找到返回-1
     */
    default int findFirstEmptySlot() {
        return findFirstSlotWhere(ItemStack::isEmpty);
    }
    
    /**
     * 统计满足条件的物品总数量
     * @param predicate 物品匹配条件
     * @return 物品总数量
     */
    default int countItemsWhere(Predicate<ItemStack> predicate) {
        return IntStream.range(0, getContainerCapacity())
            .mapToObj(i -> getContainer().getItem(i))
            .mapToInt(stack -> predicate.test(stack) ? stack.getCount() : 0)
            .sum();
    }
    
    /**
     * 尝试向容器添加物品
     * @param itemToAdd 要添加的物品堆
     * @return 无法添加的剩余物品
     */
    default ItemStack tryAddItem(ItemStack itemToAdd) {
        if (itemToAdd.isEmpty()) return ItemStack.EMPTY;
        
        ItemStack remaining = itemToAdd.copy();
        
        // 第一阶段：尝试合并到现有相同物品堆
        for (int slot = 0; slot < getContainerCapacity() && !remaining.isEmpty(); slot++) {
            ItemStack slotStack = getContainer().getItem(slot);
            if (canMergeItems(slotStack, remaining)) {
                int merged = tryMergeIntoSlot(slotStack, remaining);
                remaining.shrink(merged);
            }
        }
        
        // 第二阶段：尝试放入空槽位
        for (int slot = 0; slot < getContainerCapacity() && !remaining.isEmpty(); slot++) {
            if (getContainer().getItem(slot).isEmpty() && canPlaceItemInSlot(remaining, slot)) {
                int placed = tryPlaceInEmptySlot(slot, remaining);
                remaining.shrink(placed);
            }
        }
        
        return remaining;
    }
    
    /**
     * 移除指定数量的满足条件的物品
     * @param predicate 物品匹配条件
     * @param maxCount 最大移除数量
     * @return 实际移除的数量
     */
    default int removeItemsWhere(Predicate<ItemStack> predicate, int maxCount) {
        int totalRemoved = 0;
        
        for (int slot = 0; slot < getContainerCapacity() && totalRemoved < maxCount; slot++) {
            ItemStack slotStack = getContainer().getItem(slot);
            if (predicate.test(slotStack)) {
                int toRemove = Math.min(maxCount - totalRemoved, slotStack.getCount());
                slotStack.shrink(toRemove);
                
                if (slotStack.isEmpty()) {
                    getContainer().setItem(slot, ItemStack.EMPTY);
                }
                
                totalRemoved += toRemove;
            }
        }
        
        return totalRemoved;
    }
    
    /**
     * 检查两个物品堆是否可以合并
     */
    private boolean canMergeItems(ItemStack existing, ItemStack toAdd) {
        return !existing.isEmpty() && 
               !toAdd.isEmpty() && 
               ItemStack.isSameItemSameComponents(existing, toAdd);
    }
    
    /**
     * 尝试将物品合并到指定槽位
     */
    private int tryMergeIntoSlot(ItemStack slotStack, ItemStack toMerge) {
        int maxStackSize = Math.min(slotStack.getMaxStackSize(), getContainer().getMaxStackSize());
        int canAdd = maxStackSize - slotStack.getCount();
        int willAdd = Math.min(canAdd, toMerge.getCount());
        
        if (willAdd > 0) {
            slotStack.grow(willAdd);
        }
        
        return willAdd;
    }
    
    /**
     * 尝试将物品放入空槽位
     */
    private int tryPlaceInEmptySlot(int slot, ItemStack toPlace) {
        int maxStackSize = Math.min(toPlace.getMaxStackSize(), getContainer().getMaxStackSize());
        int willPlace = Math.min(maxStackSize, toPlace.getCount());
        
        if (willPlace > 0) {
            ItemStack newStack = toPlace.copy();
            newStack.setCount(willPlace);
            getContainer().setItem(slot, newStack);
        }
        
        return willPlace;
    }
    
    /**
     * 检查容器是否为空
     * @return 是否为空
     */
    default boolean isEmpty() {
        return countItemsWhere(stack -> !stack.isEmpty()) == 0;
    }
    
    /**
     * 检查容器是否已满
     * @return 是否已满
     */
    default boolean isFull() {
        return findFirstEmptySlot() == -1;
    }
    
    /**
     * 获取容器中非空槽位数量
     * @return 非空槽位数量
     */
    default int getOccupiedSlotCount() {
        return (int) IntStream.range(0, getContainerCapacity())
            .mapToObj(i -> getContainer().getItem(i))
            .filter(stack -> !stack.isEmpty())
            .count();
    }
}
