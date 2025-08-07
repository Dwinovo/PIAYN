package com.dwinovo.piayn.datagen;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.init.InitEntity;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * PIAYN模组中文语言文件数据生成器
 * <p>用于自动生成中文翻译文件</p>
 * 
 * @author PIAYN Team
 */
public class PIAYNChineseLanguageProvider extends LanguageProvider {
    
    public PIAYNChineseLanguageProvider(PackOutput output) {
        super(output, PIAYN.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        // === 配置翻译（与现有结构保持一致） ===
        this.add("piayn.configuration.title", "宠物就是你的全部 配置");
        this.add("piayn.configuration.section.piayn.common.toml", "宠物就是你的全部 配置");
        this.add("piayn.configuration.section.piayn.common.toml.title", "宠物就是你的全部 配置");
        this.add("piayn.configuration.items", "物品列表");
        this.add("piayn.configuration.logDirtBlock", "记录泥土方块");
        this.add("piayn.configuration.magicNumberIntroduction", "魔法数字文本");
        this.add("piayn.configuration.magicNumber", "魔法数字");
        
        // === Jade集成翻译 ===
        this.add("config.jade.plugin_piayn.pet_model_name", "宠物模型名称");
        
        // === 实体翻译 ===
        this.addEntityType(InitEntity.PET, "宠物");
        
        // === GUI翻译 ===
        // 模型选择界面
        this.add("gui.piayn.entity_model_select", "选择宠物模型");
        
        // 模型切换按钮无障碍功能
        this.add("gui.piayn.model_switch_button.narration", "%s模型%s");
        
        // === 状态翻译 ===
        this.add("status.piayn.selected", "已选择");
        this.add("status.piayn.available", "可用");
    }
}
