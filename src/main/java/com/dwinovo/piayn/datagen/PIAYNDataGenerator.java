package com.dwinovo.piayn.datagen;

import com.dwinovo.piayn.PIAYN;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * PIAYN模组数据生成事件处理器
 * <p>负责注册所有数据生成提供者</p>
 * 
 * @author PIAYN Team
 */
@EventBusSubscriber(modid = PIAYN.MOD_ID)
public class PIAYNDataGenerator {
    
    /**
     * 数据生成事件处理
     * <p>注册语言文件和其他数据生成提供者</p>
     * 
     * @param event 数据收集事件
     */
    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var packOutput = generator.getPackOutput();
        var existingFileHelper = event.getExistingFileHelper();
        var lookupProvider = event.getLookupProvider();
        
        // 注册客户端数据生成器
        if (event.includeClient()) {
            // 英文语言文件
            generator.addProvider(true, new PIAYNLanguageProvider(packOutput));
            
            // 中文语言文件
            generator.addProvider(true, new PIAYNChineseLanguageProvider(packOutput));
            
            // 物品模型
            generator.addProvider(true, new PIAYNItemModelProvider(packOutput, existingFileHelper));
            
        }
        
        // 注册服务端数据生成器
        if (event.includeServer()) {
            
            
           
        }
    }
}
