package com.dwinovo.piayn.client.gui.screen.container.slot;

import com.dwinovo.piayn.entity.PetEntity;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 宠物主手物品槽
 * 将容器第0号槽与宠物的主手物品进行双向同步
 */
public class PetMainHandSlot extends Slot {
    private final PetEntity petEntity;

    public PetMainHandSlot(Container container, int slot, int x, int y, PetEntity petEntity) {
        super(container, slot, x, y);
        this.petEntity = petEntity;
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        super.set(stack);
        // 当设置物品时，同步到宠物的主手
        if (petEntity != null) {
            petEntity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, stack.copy());
        }
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        // 可以放置任何物品到主手槽
        return true;
    }
}
