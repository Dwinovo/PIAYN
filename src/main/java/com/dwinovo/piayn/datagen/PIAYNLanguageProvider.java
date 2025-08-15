package com.dwinovo.piayn.datagen;

import com.dwinovo.piayn.PIAYN;
import com.dwinovo.piayn.init.InitEntity;
import com.dwinovo.piayn.init.InitItem;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * PIAYN模组语言文件数据生成器
 * <p>用于自动生成英文翻译文件</p>
 * 
 * @author PIAYN Team
 */
public class PIAYNLanguageProvider extends LanguageProvider {
    
    public PIAYNLanguageProvider(PackOutput output) {
        super(output, PIAYN.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // === 配置翻译（与现有结构保持一致） ===
        this.add("piayn.configuration.title", "Pet is All You Need Configs");
        this.add("piayn.configuration.section.piayn.common.toml", "Pet is All You Need Configs");
        this.add("piayn.configuration.section.piayn.common.toml.title", "Pet is All You Need Configs");
        this.add("piayn.configuration.items", "Item List");
        this.add("piayn.configuration.logDirtBlock", "Log Dirt Block");
        this.add("piayn.configuration.magicNumberIntroduction", "Magic Number Text");
        this.add("piayn.configuration.magicNumber", "Magic Number");
        
        // === Jade集成翻译 ===
        this.add("config.jade.plugin_piayn.pet_model_name", "Pet Model Name");
        
        // === 实体翻译 ===
        this.addEntityType(InitEntity.PET, "Pet");
        
        // === 物品翻译 ===
        this.addItem(InitItem.PET_SPAWN_EGG, "Pet Spawn Egg");
        this.addItem(InitItem.SCHEMATIC_PEN, "Blueprint Pen");
        

        
        // === GUI翻译 ===
        // 模型选择界面
        this.add("gui.piayn.entity_model_select", "Select Pet Model");
        
        // 模型切换按钮无障碍功能
        this.add("gui.piayn.model_switch_button.narration", "%s model %s");
        
        // === 状态翻译 ===
        this.add("status.piayn.selected", "selected");
        this.add("status.piayn.available", "available");
    }
}
