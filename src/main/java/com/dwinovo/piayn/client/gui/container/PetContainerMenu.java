package com.dwinovo.piayn.client.gui.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import com.dwinovo.piayn.entity.PetEntity;
import com.dwinovo.piayn.init.InitMenuTypes;

/**
 * 宠物GUI菜单类
 * 处理宠物GUI的逻辑和数据交互
 */
public class PetContainerMenu extends AbstractContainerMenu {
    private final PetEntity petEntity;
    private final Player player;

    public PetContainerMenu(int containerId, Inventory playerInventory, PetEntity petEntity) {
        super(InitMenuTypes.PET_MENU.get(), containerId);
        this.petEntity = petEntity;
        this.player = playerInventory.player;
        this.addPlayerSlots(playerInventory);
        this.addPetSlots(petEntity.getContainer());
    }

    /**
     * 获取宠物实体
     * @return 宠物实体
     */
    public PetEntity getPetEntity() {
        return petEntity;
    }


    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        // 暂时不支持快速移动物品
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        // 检查玩家是否仍然可以访问此菜单
        return this.petEntity.isAlive() && 
               this.petEntity.distanceToSqr(player) <= 64.0D;
    }
    public void addPetSlots(Container container){
        // 添加自定义生物物品槽 (物品槽坐标为 x=8, y=18)
        this.addSlot(new Slot(container, 0, 8, 18)); 
        // 添加自定义生物物品槽 (物品槽坐标为 x=80, y=20)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 5; ++col) {
                int x = 80 + col * 18;  // 每个物品槽宽度为18
                int y = 18 + row * 18;  // 每个物品槽高度为18
                this.addSlot(new Slot(container, col + row * 5+1, x, y));
            }
        }
    }
    /**
     * 添加玩家物品栏
     * @param playerInventory
     */
    private void addPlayerSlots(Inventory playerInventory)
    {
        // 图片偏移
        int x_offset = 8; 
        // 图片偏移
        int y_offset = 84; 
        // 添加玩家背包槽位
        for (int row = 0; row < 3; ++row) {
            // 添加玩家背包槽位
            for (int col = 0; col < 9; ++col) {
                // 每个物品槽宽度为18
                int x = x_offset + col * 18;  
                // 每个物品槽高度为18
                int y = y_offset + row * 18;  
                // 添加玩家背包槽位
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, x, y));
            }
        }
        // 快捷栏位于玩家物品栏的下方
        for (int col = 0; col < 9; ++col) {
            // 快捷栏的 X 坐标
            int x = x_offset + col * 18;  
            // 快捷栏距离物品栏的 Y 偏移通常是58
            int y = y_offset + 58;  
            // 添加快捷栏槽位
            this.addSlot(new Slot(playerInventory, col, x, y));
        }
    }

}
