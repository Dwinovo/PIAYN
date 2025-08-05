package com.dwinovo.piayn.init;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

import com.dwinovo.piayn.client.gui.container.PetContainerMenu;

/**
 * 菜单类型初始化类
 * 注册所有的GUI菜单类型
 */
public class InitMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = 
        DeferredRegister.create(BuiltInRegistries.MENU, "piayn");

    /**
     * 宠物GUI菜单类型
     */
    public static final Supplier<MenuType<PetContainerMenu>> PET_MENU = MENU_TYPES.register("pet_menu",
        () -> IMenuTypeExtension.create((windowId, inv, data) -> {
            // 从网络数据中读取宠物实体ID
            int petEntityId = data.readInt();
            if (inv.player.level().getEntity(petEntityId) instanceof com.dwinovo.piayn.entity.PetEntity petEntity) {
                return new PetContainerMenu(windowId, inv, petEntity);
            }
            return null;
        }));
}
