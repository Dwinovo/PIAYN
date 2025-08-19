package com.dwinovo.piayn.client.resource.schematic;

import com.dwinovo.piayn.client.renderer.schematic.SchematicPreviewRenderer;
import com.dwinovo.piayn.world.schematic.level.SchematicLevel;
import com.dwinovo.piayn.client.resource.schematic.cache.SchematicTemplateCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 
 */
public class ClientSchematicManager {
    private static final ClientSchematicManager INSTANCE = new ClientSchematicManager();
    
    private SchematicLevel schematicLevel;
    private final SchematicPreviewRenderer renderer = new SchematicPreviewRenderer();
    
    private ClientSchematicManager() {}
    
    public static ClientSchematicManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 面向事件/UI的一站式显示接口：传入文件名，必要时自动IO加载、创建并设置当前 SchematicLevel。
     * 若文件名与当前一致且已有有效的 currentLevel，则不做任何事（避免重复工作）。
     */
    public void display(Level level, String fileName) {

        StructureTemplate template = SchematicTemplateCache.getInstance().getOrLoadTemplate(level, fileName);
        if (template == null)
            return;
        this.schematicLevel = buildLevelByTemplate(template, level);
    }
    /**
     * 事件侧的一站式渲染入口：内部获取当前 SchematicLevel 并委托给渲染器。
     * 若当前没有已设置的 SchematicLevel，则直接返回不做任何渲染。
     */
    public void renderPreview(RenderLevelStageEvent event, BlockPos anchor) {
        SchematicLevel level = this.schematicLevel;
        if (level == null)
            return;
        this.renderer.renderFromEvent(event, level, anchor);
    }

    private SchematicLevel buildLevelByTemplate(StructureTemplate template, Level level) {
        SchematicLevel schematicLevel = new SchematicLevel(level);
        // 使用StructureTemplate的place方法将结构放置到SchematicLevel中
        template.placeInWorld(schematicLevel, BlockPos.ZERO, BlockPos.ZERO, 
            new StructurePlaceSettings(), 
            schematicLevel.getRandom(), 2);
        return schematicLevel;
    }
    
    /** 清除当前 SchematicLevel（例如关闭预览或重新选择前重置）。 */
    public void clearCurrent() {
        this.schematicLevel = null;
    }

    
}
