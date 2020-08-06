package com.samaritans.enigmaticamod;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TieredItem;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;

public class ShapelessToolRecipe extends ShapelessRecipe {

    public ShapelessToolRecipe(ResourceLocation idIn, String groupIn, ItemStack recipeOutputIn,
							   NonNullList<Ingredient> recipeItemsIn) {
        super(idIn, groupIn, recipeOutputIn, recipeItemsIn);
    }

    private ItemStack damageTool(final ItemStack stack) {
		final PlayerEntity craftingPlayer = ForgeHooks.getCraftingPlayer();
		if (stack.attemptDamageItem(1, craftingPlayer.getEntityWorld().rand, craftingPlayer instanceof ServerPlayerEntity ? (ServerPlayerEntity) craftingPlayer : null)) {
			ForgeEventFactory.onPlayerDestroyItem(craftingPlayer, stack, null);
			return ItemStack.EMPTY;
		}

		return stack;
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(final CraftingInventory inv) {
		final NonNullList<ItemStack> remainingItems = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

		for (int i = 0; i < remainingItems.size(); ++i) {
			final ItemStack itemstack = inv.getStackInSlot(i);

			if (!itemstack.isEmpty() && itemstack.getItem() instanceof TieredItem) {
				remainingItems.set(i, damageTool(itemstack.copy()));
			} else {
				remainingItems.set(i, ForgeHooks.getContainerItem(itemstack));
			}
		}

		return remainingItems;
	}

	public static class Serializer extends net.minecraftforge.registries.ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<ShapelessToolRecipe> {
		public ShapelessToolRecipe read(ResourceLocation recipeId, JsonObject json) {
			String s = JSONUtils.getString(json, "group", "");
			NonNullList<Ingredient> nonnulllist = readIngredients(JSONUtils.getJsonArray(json, "ingredients"));
			if (nonnulllist.isEmpty()) {
				throw new JsonParseException("No ingredients for shapeless recipe");
			} else if (nonnulllist.size() > 9) {
				throw new JsonParseException("Too many ingredients for shapeless recipe the max is " + 9);
			} else {
				ItemStack itemstack = ShapedRecipe.deserializeItem(JSONUtils.getJsonObject(json, "result"));
				return new ShapelessToolRecipe(recipeId, s, itemstack, nonnulllist);
			}
		}

		private static NonNullList<Ingredient> readIngredients(JsonArray p_199568_0_) {
			NonNullList<Ingredient> nonnulllist = NonNullList.create();
			for(int i = 0; i < p_199568_0_.size(); ++i) {
				Ingredient ingredient = Ingredient.deserialize(p_199568_0_.get(i));
				if (!ingredient.hasNoMatchingItems()) {
					nonnulllist.add(ingredient);
				}
			}
			return nonnulllist;
		}

		public ShapelessToolRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {
			String s = buffer.readString(32767);
			int i = buffer.readVarInt();
			NonNullList<Ingredient> nonnulllist = NonNullList.withSize(i, Ingredient.EMPTY);

			for(int j = 0; j < nonnulllist.size(); ++j) {
				nonnulllist.set(j, Ingredient.read(buffer));
			}

			ItemStack itemstack = buffer.readItemStack();
			return new ShapelessToolRecipe(recipeId, s, itemstack, nonnulllist);
		}

		@Override
		public void write(PacketBuffer buffer, ShapelessToolRecipe recipe) {
			buffer.writeString(recipe.getGroup());
			buffer.writeVarInt(recipe.getIngredients().size());

			for(Ingredient ingredient : recipe.getIngredients()) {
				ingredient.write(buffer);
			}

			buffer.writeItemStack(recipe.getRecipeOutput());
		}
	}
}
