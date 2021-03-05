package com.github.maxopoly.WurstCivTools.anvil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import vg.civcraft.mc.civmodcore.itemHandling.ItemMap;

public class AnvilHandler {

	private Map<ItemMap, Double> values;
	private Map<Enchantment, Double> enchantCosts;
	private Map<String, Double> loreCosts;
	private static Set<Material> tools;
	private boolean scaleWithMissingDurability;
	private List<String> blacklistedLore;
	Map<Material, Double> materialValues;

	public AnvilHandler(Map<ItemMap, Double> values, Map<Enchantment, Double> enchantCosts, Map<String, Double> loreCosts,
			Map<Material, Double> materialValues, boolean scaleWithMissingDura, List<String> blacklistedLore) {
		this.values = values;
		this.enchantCosts = enchantCosts;
		this.scaleWithMissingDurability = scaleWithMissingDura;
		this.blacklistedLore = blacklistedLore;
		this.materialValues = materialValues;
		this.loreCosts = loreCosts;
		initRepairableSet();
	}

	/**
	 * How much value is required total to execute the changes currently staged
	 * in the given anvil inventory
	 * 
	 * @param i
	 *            Anvil inventory to analyze
	 * @return Value needed in the second slot
	 */
	public double getTotalAnvilActionCost(AnvilInventory i) {
		if (i.getItem(0) == null) {
			return 0.0;
		}
		double requiredAmount = calculateRepairCost(i.getItem(0));
		return requiredAmount;
	}

	/**
	 * Calculates how much value is required to fully repair the given ItemStack
	 * 
	 * @param is
	 *            ItemStack to repair
	 * @return Value needed to repair the item fully
	 */
	public double calculateRepairCost(ItemStack is) {
		if (!isTool(is.getType()) || is.getType() == Material.ENCHANTED_BOOK) {
			return 0;
		}
		double duraMultiplier = 1.0;
		if (scaleWithMissingDurability) {
			duraMultiplier = ((double) is.getDurability()) / ((double) is.getType().getMaxDurability());
		}
		ItemMeta im = is.getItemMeta();
		double enchantMultiplier = 0.0;
		if (im.hasEnchants()) {
			for (Entry<Enchantment, Integer> enchant : im.getEnchants().entrySet()) {
				Double multi = enchantCosts.get(enchant.getKey());
				if (multi != null) {
					enchantMultiplier += multi * enchant.getValue();
				}
			}
		}
		double loreMultiplier = 0;
		if (im.hasLore()) {
			for(String lore : im.getLore()) {
				Double d = loreCosts.get(lore);
				if (d != null) {
					loreMultiplier += d;
				}
			}
		}
		double materialAddition = 0.0;
		Double retrieveMatAddition = materialValues.get(is.getType());
		if (retrieveMatAddition != null) {
			materialAddition = retrieveMatAddition;
		}
		return (enchantMultiplier + materialAddition + loreMultiplier) * duraMultiplier;
	}

	/**
	 * Calculates how much value the given ItemStack has, items with value are
	 * used in the second anvil slot to repair repairables
	 * 
	 * @param is
	 *            ItemStack to analyze
	 * @param respectAmount
	 *            Whether the amount of the given ItemStack should be taken into
	 *            account. Set to false to just get information on the given
	 *            ItemStack type
	 * @return Value of the given ItemStack
	 */
	public double getValue(ItemStack is, boolean respectAmount) {
		if (is == null || is.getType() == Material.AIR) {
			return 0;
		}
		Double matValue = null;
		for (Entry<ItemMap, Double> entry : values.entrySet()) {
			if (entry.getKey().getAmount(is) != 0) {
				matValue = entry.getValue();
			}
		}
		if (matValue != null) {
			if (respectAmount) {
				return matValue * is.getAmount();
			}
			return matValue;
		}
		return 0;
	}

	/**
	 * Checks whether the given Material is a tool, ergo whether players should
	 * be allowed to repair it
	 * 
	 * @param m
	 *            Material to check
	 * @return Whether the given Material is a tool
	 */
	public static boolean isTool(Material m) {
		return tools.contains(m);
	}

	public boolean consumeRequiredMaterials(AnvilInventory i) {
		if (isCombiningRepairables(i)) {
			i.setItem(0, null);
			i.setItem(1, null);
			return true;
		}
		double requiredAmount = getTotalAnvilActionCost(i);
		double availableAmount = getValue(i.getItem(1), true);
		if (i.getItem(1) == null) {
			i.setItem(0, null);
			return true;
		}
		if (getValue(i.getItem(1), true) == 0) {
			//dont consume worthless items, in this case no repair will be done either
			i.setItem(0, null);
			return true;
		}
		if (i.getItem(1).getType() == Material.ENCHANTED_BOOK) {
			i.setItem(0, null);
			i.setItem(1, null);
			return true;
		}
		if (availableAmount >= requiredAmount) {
			int amountToConsume = (int) Math.ceil(requiredAmount / getValue(i.getItem(1), false));
			i.setItem(0, null);
			if (i.getItem(1).getAmount() == amountToConsume) {
				i.setItem(1, null);
			} else {
				i.getItem(1).setAmount(i.getItem(1).getAmount() - amountToConsume);
			}
			return true;
		}
		else {
			i.setItem(0, null);
			i.setItem(1, null);
			return true;
		}
	}

	public ItemStack getAdjustedOutput(AnvilInventory i, ItemStack vanillaResult) {
		ItemStack firstItem = i.getItem(0);
		ItemStack secondItem = i.getItem(1);
		if (firstItem == null) {
			return null;
		}
		String newName = null;
		if (vanillaResult != null && vanillaResult.getItemMeta() != null
				&& vanillaResult.getItemMeta().hasDisplayName()) {
			newName = vanillaResult.getItemMeta().getDisplayName();
		}
		if (isCombiningRepairables(i) || (secondItem != null && secondItem.getType() == Material.ENCHANTED_BOOK)) {
			if (firstItem.getType() != secondItem.getType() && secondItem.getType() != Material.ENCHANTED_BOOK) {
				return null;
			}
			Map<Enchantment, Integer> enchants = new HashMap<Enchantment, Integer>();
			List<String> lore = new LinkedList<String>();
			List<ItemStack> items = new LinkedList<ItemStack>();
			items.add(firstItem);
			items.add(secondItem);
			for (ItemStack is : items) {
				ItemMeta im = is.getItemMeta();
				if (im.hasEnchants()) {
					for (Entry<Enchantment, Integer> entry : im.getEnchants().entrySet()) {
						Integer existingVal = enchants.get(entry.getKey());
						if (existingVal == null) {
							enchants.put(entry.getKey(), entry.getValue());
						} else {
							enchants.put(entry.getKey(), Math.max(entry.getValue(), existingVal));
						}
					}
				}
				if (im.hasLore()) {
					for (String exisLore : im.getLore()) {
						if (!lore.contains(exisLore) && !blacklistedLore.contains(exisLore)) {
							lore.add(exisLore);
						}
					}
				}
			}
			ItemStack result = new ItemStack(firstItem.getType(), 1, firstItem.getDurability());
			ItemMeta im = result.getItemMeta();
			if (lore.size() != 0) {
				im.setLore(lore);
			}
			for (Entry<Enchantment, Integer> entry : enchants.entrySet()) {
				im.addEnchant(entry.getKey(), entry.getValue(), true);
			}
			if (secondItem.getType() == Material.ENCHANTED_BOOK) {
				for (Entry<Enchantment, Integer> entry : ((EnchantmentStorageMeta) secondItem.getItemMeta())
						.getStoredEnchants().entrySet()) {
					im.addEnchant(entry.getKey(), Math.max(entry.getValue(), im.getEnchantLevel(entry.getKey())), true);
				}
			}
			if (newName != null) {
				im.setDisplayName(newName);
			}
			result.setItemMeta(im);
			return result;
		} else {
			//repairing or second slot is empty
			if (secondItem == null) {
				return null;
			}
			double value = getValue(secondItem, true);
			double repairCost = calculateRepairCost(firstItem);
			ItemStack repl = firstItem.clone();
			int missingDura = repl.getDurability();
			int newDura;
			if (scaleWithMissingDurability) {
				double available = Math.min(value/repairCost, 1.0);
				newDura = (int) (missingDura - (missingDura * available));
			}
			else {
				if (value >= repairCost) {
					newDura = 0;
				}
				else {
					newDura = missingDura;
				}
			}
			if (newName != null) {
				ItemMeta im = repl.getItemMeta();
				im.setDisplayName(newName);
				repl.setItemMeta(im);
			}
			repl.setDurability((short) newDura);
			return repl;
		}
	}

	public static boolean isCombiningRepairables(AnvilInventory i) {
		return i.getItem(0) != null && i.getItem(1) != null && isTool(i.getItem(0).getType())
				&& isTool(i.getItem(1).getType());
	}

	private void initRepairableSet() {
		tools = new HashSet<Material>();
		tools.add(Material.WOODEN_AXE);
		tools.add(Material.WOODEN_HOE);
		tools.add(Material.WOODEN_PICKAXE);
		tools.add(Material.WOODEN_SHOVEL);
		tools.add(Material.WOODEN_SWORD);
		tools.add(Material.IRON_AXE);
		tools.add(Material.IRON_HOE);
		tools.add(Material.IRON_PICKAXE);
		tools.add(Material.IRON_SHOVEL);
		tools.add(Material.IRON_SWORD);
		tools.add(Material.GOLDEN_AXE);
		tools.add(Material.GOLDEN_HOE);
		tools.add(Material.GOLDEN_PICKAXE);
		tools.add(Material.GOLDEN_SHOVEL);
		tools.add(Material.GOLDEN_SWORD);
		tools.add(Material.STONE_AXE);
		tools.add(Material.STONE_PICKAXE);
		tools.add(Material.STONE_SWORD);
		tools.add(Material.STONE_SHOVEL);
		tools.add(Material.STONE_HOE);
		tools.add(Material.DIAMOND_AXE);
		tools.add(Material.DIAMOND_HOE);
		tools.add(Material.DIAMOND_PICKAXE);
		tools.add(Material.DIAMOND_SHOVEL);
		tools.add(Material.DIAMOND_SWORD);
		tools.add(Material.LEATHER_BOOTS);
		tools.add(Material.LEATHER_CHESTPLATE);
		tools.add(Material.LEATHER_HELMET);
		tools.add(Material.LEATHER_LEGGINGS);
		tools.add(Material.IRON_BOOTS);
		tools.add(Material.IRON_CHESTPLATE);
		tools.add(Material.IRON_HELMET);
		tools.add(Material.IRON_LEGGINGS);
		tools.add(Material.GOLDEN_BOOTS);
		tools.add(Material.GOLDEN_CHESTPLATE);
		tools.add(Material.GOLDEN_HELMET);
		tools.add(Material.GOLDEN_LEGGINGS);
		tools.add(Material.DIAMOND_BOOTS);
		tools.add(Material.DIAMOND_CHESTPLATE);
		tools.add(Material.DIAMOND_HELMET);
		tools.add(Material.DIAMOND_LEGGINGS);
		tools.add(Material.CHAINMAIL_BOOTS);
		tools.add(Material.CHAINMAIL_CHESTPLATE);
		tools.add(Material.CHAINMAIL_HELMET);
		tools.add(Material.CHAINMAIL_LEGGINGS);
		tools.add(Material.SHEARS);
		tools.add(Material.SHIELD);
		tools.add(Material.BOW);
		tools.add(Material.FLINT_AND_STEEL);
		tools.add(Material.FISHING_ROD);
	}
}
