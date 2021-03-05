package com.github.maxopoly.WurstCivTools.effect;

import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;

import com.github.maxopoly.WurstCivTools.listener.PlayerHookListener;
import com.github.maxopoly.WurstCivTools.misc.HookData;

public class PlayerHook extends WurstEffect {
	private PlayerHookListener listener;
	public final boolean swords;
	public final boolean axes;
	public final boolean bows;
	public final int stopcount;
	public final double speed_change;
	

	public PlayerHook(boolean prevent_swords, boolean prevent_axes, 
			boolean prevent_bows, int stop_count, double speed_change) {
		this.swords = prevent_swords;
		this.axes = prevent_axes;
		this.bows = prevent_bows;
		this.stopcount = stop_count;
		this.speed_change = speed_change;
	}
	
	@Override
	public Listener getListener(){
		this.listener = new PlayerHookListener(this);
		return this.listener;
	}
	
	@Override 
	public void handleItemSelect(Player p, PlayerItemHeldEvent e){
		if (canSelect(p) == false)
			e.setCancelled(true);
	}

	@Override
	public void handleItemDeselect(Player p, PlayerItemHeldEvent e) {
		clearHooks(p.getUniqueId());
	}
	
	@Override
	public void handleSwapToMainHand(Player p, PlayerSwapHandItemsEvent e) {
		if (canSelect(p) == false)
			e.setCancelled(true);
	}

	@Override
	public void handleSwapToOffHand(Player p, PlayerSwapHandItemsEvent e) {
		clearHooks(p.getUniqueId());
	}

	private boolean canSelect(Player p){
		if (swords){
			Inventory inv = p.getInventory();
			if (inv.contains(Material.DIAMOND_SWORD)){
				p.sendMessage("You cannot use this item with a Sword in your inventory.");
				return false;
			} else if (inv.contains(Material.IRON_SWORD)){
				p.sendMessage("You cannot use this item with a Sword in your inventory.");
				return false;
			} else if (inv.contains(Material.STONE_SWORD)){
				p.sendMessage("You cannot use this item with a Sword in your inventory.");
				return false;
			} else if (inv.contains(Material.WOODEN_SWORD)){
				p.sendMessage("You cannot use this item with a Sword in your inventory.");
				return false;
			} else if (inv.contains(Material.GOLDEN_SWORD)){
				p.sendMessage("You cannot use this item with a Sword in your inventory.");
				return false;
			}
		} 
		if (axes){
			Inventory inv = p.getInventory();
			if (inv.contains(Material.GOLDEN_AXE)){
				p.sendMessage("You cannot use this item with an Axe in your inventory.");
				return false;
			} else if (inv.contains(Material.DIAMOND_AXE)){
				p.sendMessage("You cannot use this item with an Axe in your inventory.");
				return false;
			} else if (inv.contains(Material.IRON_AXE)){
				p.sendMessage("You cannot use this item with an Axe in your inventory.");
				return false;
			} else if (inv.contains(Material.STONE_AXE)){
				p.sendMessage("You cannot use this item with an Axe in your inventory.");
				return false;
			} else if (inv.contains(Material.WOODEN_AXE)){
				p.sendMessage("You cannot use this item with an Axe in your inventory.");
				return false;
			}
		} 
		if (bows){
			if (p.getInventory().contains(Material.BOW)){
				p.sendMessage("You cannot use this item with a Bow in your inventory.");
				return false;
			}
		}
		return true;
	}
	
	private void clearHooks(UUID uuid){
		for(Entry<UUID, HookData> hook : listener.getHooks().entrySet()){
			if (uuid.equals(hook.getValue().getSource())){
				listener.removehook(hook.getKey());
				break;
			}
		}
	}
	
}
