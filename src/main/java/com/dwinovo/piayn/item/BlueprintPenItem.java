package com.dwinovo.piayn.item;


import com.mojang.logging.LogUtils;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class BlueprintPenItem extends Item{
    public static final Logger LOGGER = LogUtils.getLogger();

    public BlueprintPenItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        // 改为完全由客户端监听器处理，物品侧放行
        return InteractionResult.PASS;
    }
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand usedHand) {
        // 改为完全由客户端监听器处理，物品侧放行
        return InteractionResultHolder.pass(player.getItemInHand(usedHand));
    }



}
