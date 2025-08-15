package com.dwinovo.piayn.client.resource.schematic;

import com.dwinovo.piayn.client.renderer.schematic.SchematicPreviewRenderer;
import com.dwinovo.piayn.world.schematic.level.SchematicLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import javax.annotation.Nullable;

/**
 * 客户端蓝图管理器（简化版）。
 *
 * 职责：
 * - 维护“当前激活”的 {@link SchematicLevel} 与单例 {@link SchematicPreviewRenderer}；
 * - 不做文件名/模板缓存与监听，避免状态复杂化；
 * - 与 UI/事件协作：
 *   - 由 {@code SchematicSelectScreen} 负责从磁盘加载 {@link StructureTemplate}，并调用
 *     {@link #createSchematicLevel(StructureTemplate, Level)} 与 {@link #setCurrentLevel(SchematicLevel)}。
 *   - 由 {@code SchematicPaperClientEvent} 在渲染阶段通过 {@link #getCurrentLevel()} 与 {@link #getRenderer()} 获取数据并绘制。
 */
public class ClientSchematicManager {
    private static final ClientSchematicManager INSTANCE = new ClientSchematicManager();
    
    private SchematicLevel currentLevel;
    private final SchematicPreviewRenderer renderer = new SchematicPreviewRenderer();
    
    private ClientSchematicManager() {}
    
    public static ClientSchematicManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 创建并填充一个 {@link SchematicLevel} 实例。
     *
     * 关键点：
     * - {@link SchematicLevel} 仅保存相对(0,0,0)的局部坐标数据；
     * - 通过 {@link StructureTemplate#placeInWorld} 以 {@link BlockPos#ZERO} 为起点写入；
     * - 锚点（anchor）不在此处管理，交由 {@link SchematicPreviewRenderer} 在渲染时处理。
     */
    public SchematicLevel createSchematicLevel(StructureTemplate template, Level level) {
        SchematicLevel schematicLevel = new SchematicLevel(level);
        // 使用StructureTemplate的place方法将结构放置到SchematicLevel中
        template.placeInWorld(schematicLevel, BlockPos.ZERO, BlockPos.ZERO, 
            new StructurePlaceSettings(), 
            schematicLevel.getRandom(), 2);
        return schematicLevel;
    }
    
    /** 设置当前活跃的 SchematicLevel（供渲染事件读取）。 */
    public void setCurrentLevel(SchematicLevel schematicLevel) {
        this.currentLevel = schematicLevel;
    }
    
    /** 获取当前 SchematicLevel；可能为 null（尚未选择或已清空）。 */
    @Nullable
    public SchematicLevel getCurrentLevel() {
        return currentLevel;
    }
    
    /** 获取渲染器实例（单例，供事件端重复复用以获得平滑追踪状态）。 */
    public SchematicPreviewRenderer getRenderer() {
        return renderer;
    }
    
    /** 清除当前 SchematicLevel（例如关闭预览或重新选择前重置）。 */
    public void clearCurrent() {
        this.currentLevel = null;
    }
}
