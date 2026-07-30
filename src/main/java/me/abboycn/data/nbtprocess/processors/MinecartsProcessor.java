package me.abboycn.data.nbtprocess.processors;

import me.abboycn.data.nbtprocess.AbstractEntityProcessor;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Map;

public final class MinecartsProcessor extends AbstractEntityProcessor {
    private static final TagKey<EntityType<?>> MINECARTS = TagKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of("c", "minecarts")
    );

    @Override
    public boolean process(NbtCompound entityNbt, Map<Item, Integer> itemCountMap) {
        String entityId = entityNbt.getString("id");
        if (entityId == null || entityId.isEmpty()) {
            return false;
        }

        Identifier id = Identifier.tryParse(entityId);
        if (id == null) {
            return false;
        }

        EntityType<?> entityType = Registries.ENTITY_TYPE.get(id);

        if (!entityType.isIn(MINECARTS)) {
            return false;
        }

        Item minecartItem = Registries.ITEM.get(id);
        if (minecartItem != Items.AIR) {
            addItem(itemCountMap, minecartItem, 1);
        }
        return true;
    }

    @Override
    public boolean supports(String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            return false;
        }

        Identifier id = Identifier.tryParse(entityId);
        if (id == null) {
            return false;
        }

        return Registries.ENTITY_TYPE.get(id).isIn(MINECARTS);
    }
}
