package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.data.recipe.GTIngredientTypes;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Matches items whose {@code minecraft:custom_data} contains the given tag: every key it lists, with the same
 * value, and anything else besides. This is what Forge's {@code PartialNBTIngredient} did on 1.20, and what
 * NeoForge's component ingredients cannot do, since they compare whole components. Things that keep a captured
 * entity in custom data, such as Productive Bees' bee cages, need it.
 */
public class PartialCustomDataIngredient implements ICustomIngredient {

    // spotless:off
    public static final MapCodec<PartialCustomDataIngredient> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
            HolderSetCodec.create(Registries.ITEM, BuiltInRegistries.ITEM.holderByNameCodec(), false).fieldOf("items").forGetter(PartialCustomDataIngredient::items),
            CustomData.CODEC.fieldOf("data").forGetter(PartialCustomDataIngredient::data)
    ).apply(builder, PartialCustomDataIngredient::new));
    // spotless:on

    private final HolderSet<Item> items;
    private final CustomData data;

    public PartialCustomDataIngredient(HolderSet<Item> items, CustomData data) {
        this.items = items;
        this.data = data;
    }

    public HolderSet<Item> items() {
        return items;
    }

    public CustomData data() {
        return data;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (stack == null || !items.contains(stack.getItemHolder())) return false;
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(data.getUnsafe());
    }

    @Override
    public @NotNull Stream<ItemStack> getItems() {
        return items.stream().map(item -> {
            ItemStack stack = new ItemStack(item);
            stack.set(DataComponents.CUSTOM_DATA, data);
            return stack;
        });
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public @NotNull IngredientType<?> getType() {
        return GTIngredientTypes.PARTIAL_CUSTOM_DATA_INGREDIENT.get();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PartialCustomDataIngredient other)) return false;
        return items.equals(other.items) && data.equals(other.data);
    }

    @Override
    public int hashCode() {
        return 31 * items.hashCode() + data.hashCode();
    }

    /**
     * Creates an ingredient matching any of the items whose custom data contains {@code data}
     */
    public static @NotNull Ingredient of(CompoundTag data, ItemLike... items) {
        HolderSet<Item> set = HolderSet.direct(Arrays.stream(items)
                .map(item -> (Holder<Item>) item.asItem().builtInRegistryHolder())
                .toList());
        return new PartialCustomDataIngredient(set, CustomData.of(data)).toVanilla();
    }

    /**
     * Creates an ingredient matching the item when its custom data contains the SNBT compound {@code snbt}. Named
     * apart from {@link #of} so that KubeJS scripts can call it without picking an overload.
     */
    public static @NotNull Ingredient ofSnbt(String snbt, ItemLike item) {
        try {
            return of(TagParser.parseTag(snbt), item);
        } catch (CommandSyntaxException e) {
            throw new IllegalArgumentException("Invalid SNBT for a partial custom data ingredient: " + snbt, e);
        }
    }
}
