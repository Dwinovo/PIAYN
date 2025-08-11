package com.dwinovo.piayn.data.datagen;

import com.dwinovo.piayn.PIAYN;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * PIAYN物品模型数据生成器
 * <p>负责生成物品的模型文件</p>
 * 
 * @author PIAYN Team
 */
public class PIAYNItemModelProvider extends ItemModelProvider {

    public PIAYNItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, PIAYN.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // 生成宠物生成蛋的模型
        // 使用minecraft:item/template_spawn_egg作为父模型
        withExistingParent("pet_spawn_egg", mcLoc("item/template_spawn_egg"));
        

        // 生成蓝图笔的模型
        singleTexture("schematic_pen", mcLoc("item/handheld"), "layer0", modLoc("item/schematic_pen"));
        
        // 生成蓝图的模型
        singleTexture("schematic_paper", mcLoc("item/generated"), "layer0", modLoc("item/schematic_paper"));
    }
}
