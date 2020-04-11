package com.samaritans.enigmaticamod;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.merchant.villager.VillagerTrades;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MerchantOffer;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Mod.EventBusSubscriber(modid = Enigmatica.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VillagerTradesSerializer {
    public static final Map<VillagerProfession, Map<Integer, List<VillagerTrades.ITrade>>> VILLAGER_TRADES = new HashMap<>();
    public static final Map<VillagerProfession, Boolean> VILLAGER_TRADES_REPLACE = new HashMap<>();

    public static void init() throws Exception {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("enigmatica");
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }
        Files.walk(dir).filter(Files::isRegularFile).map(Path::toFile).forEach(VillagerTradesSerializer::readFromFile);
    }

    public static void readFromFile(File file) {
        Gson gson = new Gson();
        try {
            JsonObject obj = gson.fromJson(new FileReader(file), JsonObject.class);
            String professionStr = JSONUtils.getString(obj, "profession");
            VillagerProfession profession = ForgeRegistries.PROFESSIONS.getValue(new ResourceLocation(professionStr));
            boolean replace = JSONUtils.getBoolean(obj, "replace");
            VILLAGER_TRADES_REPLACE.put(profession, replace);
            JsonArray trades = JSONUtils.getJsonArray(obj, "trades");
            Map<Integer, List<VillagerTrades.ITrade>> tradeLvlMap = new HashMap<>();
            for (JsonElement element : trades) {
                JsonObject trade = element.getAsJsonObject();
                int level = JSONUtils.getInt(trade, "level");
                JsonObject iTrade = JSONUtils.getJsonObject(trade, "trade");
                VillagerTrades.ITrade iTradeObj = getTradeFromJson(iTrade);
                if (iTradeObj != null) {
                    if (tradeLvlMap.containsKey(level)) {
                        tradeLvlMap.get(level).add(iTradeObj);
                    } else {
                        tradeLvlMap.put(level, new ArrayList<>(Collections.singletonList(iTradeObj)));
                    }
                }
            }
            VILLAGER_TRADES.put(profession, tradeLvlMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static VillagerTrades.ITrade getTradeFromJson(JsonObject json) {
        String type = json.get("type").getAsString();
        switch (type) {
            case "minecraft:emerald_for_items": {
                Item item = JSONUtils.getItem(json, "item");
                int count = JSONUtils.getInt(json, "count");
                int maxUses = JSONUtils.getInt(json, "max_uses", 8);
                int xpValue = JSONUtils.getInt(json, "xp_value", 12);
                return new VillagerTrades.EmeraldForItemsTrade(item, count, maxUses, xpValue);
            }
            case "minecraft:items_for_emeralds": {
                ItemStack itemStack = ShapedRecipe.deserializeItem(json);
                int emeraldCount = JSONUtils.getInt(json, "emerald_count");
                int maxUses = JSONUtils.getInt(json, "max_uses", 8);
                int xpValue = JSONUtils.getInt(json, "xp_value", 12);
                return new ItemsForEmeraldsTrade(itemStack, emeraldCount, maxUses, xpValue);
            }
            case "minecraft:enchanted_item_for_emeralds": {
                Item item = JSONUtils.getItem(json, "item");
                int emeraldCount = JSONUtils.getInt(json, "emerald_count");
                int maxUses = JSONUtils.getInt(json, "max_uses", 8);
                int xpValue = JSONUtils.getInt(json, "xp_value", 12);
                return new VillagerTrades.EnchantedItemForEmeraldsTrade(item, emeraldCount, maxUses, xpValue);
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onRegisterVillagerTrades(VillagerTradesEvent event) {
        VillagerProfession profession = event.getType();
        if (VILLAGER_TRADES.containsKey(profession)) {
            if (VILLAGER_TRADES_REPLACE.get(profession)) {
                event.getTrades().clear();
            }
            for (Map.Entry<Integer, List<VillagerTrades.ITrade>> entry : VILLAGER_TRADES.get(profession).entrySet()) {
                if (event.getTrades().containsKey((int) entry.getKey())) {
                    event.getTrades().get((int) entry.getKey()).addAll(entry.getValue());
                } else {
                    event.getTrades().put((int) entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public static class ItemsForEmeraldsTrade implements VillagerTrades.ITrade {
        private final ItemStack field_221208_a;
        private final int emeraldCount;
        private final int field_221211_d;
        private final int field_221212_e;
        private final float field_221213_f;

        public ItemsForEmeraldsTrade(ItemStack p_i50531_1_, int p_i50531_2_, int p_i50531_4_, int p_i50531_5_) {
            this(p_i50531_1_, p_i50531_2_, p_i50531_4_, p_i50531_5_, 0.05F);
        }

        public ItemsForEmeraldsTrade(ItemStack p_i50532_1_, int p_i50532_2_, int p_i50532_4_, int p_i50532_5_, float p_i50532_6_) {
            this.field_221208_a = p_i50532_1_;
            this.emeraldCount = p_i50532_2_;
            this.field_221211_d = p_i50532_4_;
            this.field_221212_e = p_i50532_5_;
            this.field_221213_f = p_i50532_6_;
        }

        public MerchantOffer getOffer(Entity p_221182_1_, Random p_221182_2_) {
            ItemStack is = this.field_221208_a.copy();
            return new MerchantOffer(new ItemStack(Items.EMERALD, this.emeraldCount), is, this.field_221211_d, this.field_221212_e, this.field_221213_f);
        }
    }
}