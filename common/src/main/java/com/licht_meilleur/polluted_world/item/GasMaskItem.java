package com.licht_meilleur.polluted_world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.Equippable;

public class GasMaskItem extends Item {

    public GasMaskItem(Properties properties) {
        super(properties.component(
                DataComponents.EQUIPPABLE,
                Equippable.builder(EquipmentSlot.HEAD).build()
        ));
    }
}