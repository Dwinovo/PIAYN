package com.dwinovo.piayn.event;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.init.InitItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * 创造模式标签页事件处理器
 * <p>负责将模组物品添加到相应的创造模式标签页中</p>
 * 
 * @author PIAYN Team
 */
@EventBusSubscriber(modid = PIAYN.MOD_ID)
public class CreativeTabEvent {
    
    /**
     * 构建创造模式标签页内容事件处理
     * <p>将宠物生成蛋添加到刷怪蛋标签页中</p>
     * 
     * @param event 构建创造模式标签页内容事件
     */
    @SubscribeEvent
    public static void buildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        // 将宠物生成蛋添加到刷怪蛋标签页
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(InitItem.PET_SPAWN_EGG.get());
        }
    }
}
