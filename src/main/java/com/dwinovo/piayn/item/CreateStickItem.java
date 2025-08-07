package com.dwinovo.piayn.item;

import com.dwinovo.piayn.blueprint.IBlueprintSerializer;
import com.dwinovo.piayn.blueprint.impl.SchematicSerialize;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import javax.annotation.Nonnull;

public class CreateStickItem extends Item {
    private static final String POS1_KEY = "pos1";
    private static final String POS2_KEY = "pos2";
    private static final String HAS_POS1_KEY = "hasPos1";
    private static final String HAS_POS2_KEY = "hasPos2";
    
    // 蓝图处理器，使用多态
    private final IBlueprintSerializer blueprintHandler;

    public CreateStickItem(Properties properties) {
        super(properties);
        // 默认使用Schematic格式，未来可以通过配置或其他方式选择不同格式
        this.blueprintHandler = new SchematicSerialize();
    }

    @Override
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();
        ItemStack itemStack = context.getItemInHand();
        
        if (player == null) {
            return InteractionResult.FAIL;
        }

        CustomData customData = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        
        if (player.isShiftKeyDown()) {
            // Shift+右键设置第二个点
            setPosition(tag, POS2_KEY, HAS_POS2_KEY, clickedPos);
            player.sendSystemMessage(Component.literal("第二个点已设置: " + clickedPos.getX() + ", " + clickedPos.getY() + ", " + clickedPos.getZ()));
            
            // 如果两个点都设置了，尝试保存schematic
            if (tag.getBoolean(HAS_POS1_KEY) && tag.getBoolean(HAS_POS2_KEY)) {
                BlockPos pos1 = getPosition(tag, POS1_KEY);
                BlockPos pos2 = getPosition(tag, POS2_KEY);
                
                if (level instanceof ServerLevel serverLevel) {
                    
                    CompoundTag blueprintData = this.blueprintHandler.getTagData(serverLevel, pos1, pos2, player);
                    this.blueprintHandler.exportTagToFile(blueprintData);
                    
                }
            }
        } else {
            // 普通右键设置第一个点
            setPosition(tag, POS1_KEY, HAS_POS1_KEY, clickedPos);
            player.sendSystemMessage(Component.literal("第一个点已设置: " + clickedPos.getX() + ", " + clickedPos.getY() + ", " + clickedPos.getZ()));
        }
        
        // 保存数据组件
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        
        return InteractionResult.SUCCESS;
    }
    
    private void setPosition(CompoundTag tag, String posKey, String hasKey, BlockPos pos) {
        CompoundTag posTag = new CompoundTag();
        posTag.putInt("x", pos.getX());
        posTag.putInt("y", pos.getY());
        posTag.putInt("z", pos.getZ());
        tag.put(posKey, posTag);
        tag.putBoolean(hasKey, true);
    }
    
    private BlockPos getPosition(CompoundTag tag, String posKey) {
        CompoundTag posTag = tag.getCompound(posKey);
        return new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
    }
    
}
