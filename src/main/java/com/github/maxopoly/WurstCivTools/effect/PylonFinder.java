package com.github.maxopoly.WurstCivTools.effect;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import vg.civcraft.mc.civmodcore.util.cooldowns.ICoolDownHandler;
import vg.civcraft.mc.civmodcore.util.cooldowns.TickCoolDownHandler;

import com.github.igotyou.FactoryMod.FactoryMod;
import com.github.igotyou.FactoryMod.eggs.FurnCraftChestEgg;
import com.github.igotyou.FactoryMod.factories.Factory;
import com.github.igotyou.FactoryMod.factories.FurnCraftChestFactory;
import com.github.igotyou.FactoryMod.recipes.IRecipe;
import com.github.igotyou.FactoryMod.recipes.PylonRecipe;
import com.github.igotyou.FactoryMod.recipes.Upgraderecipe;
import com.github.maxopoly.WurstCivTools.WurstCivTools;

public class PylonFinder extends WurstEffect {

	private boolean showNonRunning;
	private boolean showUpgrading;
	private ICoolDownHandler <UUID> cdHandler;

	public PylonFinder(boolean showNonRunning, boolean showUpgrading,
			long updateCooldown) {
		super();
		this.showNonRunning = showNonRunning;
		this.showUpgrading = showUpgrading;
		this.cdHandler = new TickCoolDownHandler<UUID>(WurstCivTools.getPlugin(), updateCooldown);
	}

	public void setCompassLocation(Player p) {
		long coolDown = cdHandler.getRemainingCoolDown(p.getUniqueId());
		if (coolDown != 0) {
			p.sendMessage(ChatColor.RED + "You have to wait another "
					+ coolDown / 1000 + "." + coolDown % 1000 + " seconds before using this again");
			return;
		}
		cdHandler.putOnCoolDown(p.getUniqueId());
		HashSet<FurnCraftChestFactory> pylons = FurnCraftChestFactory
				.getPylonFactories();
		FurnCraftChestFactory closest = null;
		double distance = 0;
		if (!showUpgrading) {
			for (FurnCraftChestFactory pylon : pylons) {
				if (!showNonRunning) {
					if (!pylon.isActive()
							|| !(pylon.getCurrentRecipe() instanceof PylonRecipe)) {
						continue;
					}
				}
				if (closest == null) {
					closest = pylon;
					distance = pylon.getMultiBlockStructure().getCenter()
							.distance(p.getLocation());
					continue;
				}
				double compDistance = pylon.getMultiBlockStructure()
						.getCenter().distance(p.getLocation());
				if (compDistance < distance) {
					distance = compDistance;
					closest = pylon;
				}
			}
		} else {
			// show factories that are currently upgrading to be a pylon
			for (Factory f : FactoryMod.getInstance().getManager().getAllFactories()) {
				if (!f.isActive() && !pylons.contains(f)) {
					continue;
				}
				if (!(f instanceof FurnCraftChestFactory)) {
					continue;
				}
				FurnCraftChestFactory fac = (FurnCraftChestFactory) f;
				if (!pylons.contains(fac)) {
					if (!(fac.getCurrentRecipe() instanceof Upgraderecipe)) {
						continue;
					} else {
						// checks whether factory is upgrading to a pylon
						List<IRecipe> upgradedRecipes = ((FurnCraftChestEgg) ((Upgraderecipe) fac
								.getCurrentRecipe()).getEgg()).getRecipes();
						boolean found = false;
						for (IRecipe rec : upgradedRecipes) {
							if (rec instanceof PylonRecipe) {
								found = true;
								break;
							}
						}
						if (!found) {
							continue;
						}
					}
				}
				if (closest == null) {
					closest = fac;
					distance = fac.getMultiBlockStructure().getCenter()
							.distance(p.getLocation());
					continue;
				}
				double compDistance = fac.getMultiBlockStructure().getCenter()
						.distance(p.getLocation());
				if (compDistance < distance) {
					distance = compDistance;
					closest = fac;
				}
			}

		}
		if (closest != null) {
			if (!p.getCompassTarget().equals(closest.getMultiBlockStructure().getCenter())) {
				p.sendMessage(ChatColor.GREEN + "Found new target");
				p.setCompassTarget(closest.getMultiBlockStructure().getCenter());
			}
		}
		else {
			p.sendMessage(ChatColor.RED + "No pylons found");
		}

	}

	@Override
	public void handleInteract(Player p, PlayerInteractEvent e) {
		setCompassLocation(p);
	}

}
