package com.dwinovo.piayn.item;

import javax.annotation.Nonnull;

import com.dwinovo.piayn.init.InitComponent;
import com.dwinovo.piayn.client.gui.screen.schematic.SchematicSelectScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SchematicPaperItem extends Item {

    public SchematicPaperItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand usedHand) {
        // 右键打开蓝图选择 GUI
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            Minecraft.getInstance().setScreen(new SchematicSelectScreen(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }


}
