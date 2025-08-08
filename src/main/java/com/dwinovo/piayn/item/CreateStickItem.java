package com.dwinovo.piayn.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import com.dwinovo.piayn.schem.SchemSerializer;
import com.dwinovo.piayn.schem.pojo.StructureData;
import com.mojang.logging.LogUtils;


public class CreateStickItem extends Item {
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String POS1_KEY = "pos1";
    private static final String POS2_KEY = "pos2";
    private static final String HAS_POS1_KEY = "hasPos1";
    private static final String HAS_POS2_KEY = "hasPos2";
    private static final String SELECTED_SCHEM_KEY = "selectedSchem";
    private static final String MODE_KEY = "mode";
    
    // 模式常量
    private static final int MODE_SELECT = 0; // 圈地模式
    private static final int MODE_PASTE = 1;  // 粘贴模式
    
   

    public CreateStickItem(Properties properties) {
        super(properties);
       
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        // player 已经通过 @Nonnull 注解保证非空，移除死代码检查

        CustomData customData = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        
        // 获取当前模式，默认为圈地模式
        int currentMode = tag.getInt(MODE_KEY);
        
        // 对着空气右键切换模式
        int newMode = (currentMode == MODE_SELECT) ? MODE_PASTE : MODE_SELECT;
        tag.putInt(MODE_KEY, newMode);
        
        String modeName = (newMode == MODE_SELECT) ? "§e圈地模式" : "§b粘贴模式";
        player.sendSystemMessage(Component.literal("§a已切换到: " + modeName));
        
        // 如果切换到粘贴模式，自动选择test.schem
        if (newMode == MODE_PASTE) {
            tag.putString(SELECTED_SCHEM_KEY, "test.schem");
            player.sendSystemMessage(Component.literal("§7已自动选择test.schem文件"));
        }
        
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
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
        
        // 获取当前模式，默认为圈地模式
        int currentMode = tag.getInt(MODE_KEY);
        
        // 根据模式执行不同操作
        if (currentMode == MODE_PASTE) {
            // 粘贴模式：右键直接粘贴
            return handlePasteMode(context, tag);
        } else {
            // 圈地模式：原有的圈地逻辑
            return handleSelectMode(context, tag);
        }
    }
    
    /**
     * 处理圈地模式
     */
    private InteractionResult handleSelectMode(@Nonnull UseOnContext context, @Nonnull CompoundTag tag) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();
        ItemStack itemStack = context.getItemInHand();
        
        if (player.isShiftKeyDown()) {
            // Shift+右键设置第二个点
            setPosition(tag, POS2_KEY, HAS_POS2_KEY, clickedPos);
            player.sendSystemMessage(Component.literal("§e[圈地模式] 第二个点已设置: " + clickedPos.getX() + ", " + clickedPos.getY() + ", " + clickedPos.getZ()));
            
            // 如果两个点都设置了，尝试保存schematic
            if (tag.getBoolean(HAS_POS1_KEY) && tag.getBoolean(HAS_POS2_KEY)) {
                BlockPos pos1 = getPosition(tag, POS1_KEY);
                BlockPos pos2 = getPosition(tag, POS2_KEY);
                
                if (level instanceof ServerLevel serverLevel) {
                     
                    
                    CompoundTag schemTag = SchemSerializer.serialize(new StructureData(serverLevel, pos1, pos2, "test.schem", "dwin"));
                    LOGGER.info("schemTag: {}", schemTag);
                    SchemSerializer.writeSchem(schemTag, "test.schem");
                    
                }
            }
        } else {
            // 普通右键设置第一个点
            setPosition(tag, POS1_KEY, HAS_POS1_KEY, clickedPos);
            player.sendSystemMessage(Component.literal("§e[圈地模式] 第一个点已设置: " + clickedPos.getX() + ", " + clickedPos.getY() + ", " + clickedPos.getZ()));
        }
        
        // 保存数据组件
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return InteractionResult.SUCCESS;
    }
    
    /**
     * 处理粘贴模式
     */
    private InteractionResult handlePasteMode(@Nonnull UseOnContext context, @Nonnull CompoundTag tag) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        ItemStack itemStack = context.getItemInHand();
        
        // 计算粘贴位置：在点击面的相邻位置
        BlockPos pastePos = clickedPos.relative(clickedFace);
        
        if(level instanceof ServerLevel serverLevel){
            CompoundTag schemTag = SchemSerializer.readSchem("test.schem");
            if (schemTag != null) {
                SchemSerializer.deserialize(schemTag, serverLevel, pastePos);
                player.sendSystemMessage(Component.literal("§a[粘贴模式] 已在位置粘贴: " + pastePos.getX() + ", " + pastePos.getY() + ", " + pastePos.getZ()));
            } else {
                player.sendSystemMessage(Component.literal("§c[粘贴模式] 无法读取schematic文件"));
            }
        }
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
