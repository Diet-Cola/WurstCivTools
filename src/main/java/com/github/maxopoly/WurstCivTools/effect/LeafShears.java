/**
 * @author Aleksey Terzi
 *
 */

package com.github.maxopoly.WurstCivTools.effect;

import com.github.maxopoly.WurstCivTools.WurstCivTools;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.civmodcore.particles.ParticleEffect;

public class LeafShears extends WurstEffect {
	private int clearCubeSize;
    private String cannotBypassMessage;
    private double durabilityLossChance;
    private Random rnd;

	public LeafShears(int clearCubeSize, String cannotBypassMessage, double durabilityLossChance) {
		super();
		
		this.clearCubeSize = clearCubeSize;
	    this.cannotBypassMessage = ChatColor.translateAlternateColorCodes('&', cannotBypassMessage);
	    this.durabilityLossChance = durabilityLossChance;
	    this.rnd = new Random();
	}

	@Override
	public void handleBreak(Player p, BlockBreakEvent e) {
		if(e.isCancelled()) {
			return;
		}
		
		if(p == null) {
			return;
		}
		
		if(!isLeaf(e.getBlock())) {
			return;
		}
		
		e.setCancelled(true);
		e.getBlock().setType(Material.AIR);
		
		final ItemStack handItem = p.getInventory().getItemInMainHand();
		
		//Following manipulations are needed to synchronize tool durability with client after cancel block break
		handItem.setDurability((short)(handItem.getDurability() + 1));
		
		final Player player = p;
		final Location center = e.getBlock().getLocation();
		
		Bukkit.getScheduler().runTask(WurstCivTools.getPlugin(), new Runnable() {
            public void run() {
           		handItem.setDurability((short)(handItem.getDurability() - 1));
            	
            	damageItem(handItem, player);
            	
           		clearCube(player, center);
            }
        });
	}
	
	private void damageItem(ItemStack item, Player player) {
		int damage = getDamage();		
		
		if(damage == 0) {
			return;
		}
		
		if(WurstCivTools.getNmsManager().damageItem(item, damage, player)) {
			player.getInventory().remove(player.getInventory().getItemInMainHand());
		}
	}
	
	private int getDamage() {
		return this.rnd.nextDouble() < this.durabilityLossChance ? 1: 0;
	}
	
	private void clearCube(Player player, Location center) {
		if(this.clearCubeSize <= 1) {
			return;
		}
		
		World world = center.getWorld();
		int radius = (this.clearCubeSize - 1) / 2;
		int startX = center.getBlockX() - radius;
		int startY = center.getBlockY() - radius;
		int startZ = center.getBlockZ() - radius;
		boolean cannotBypass = false;
		
		for(int x = startX; x < startX + this.clearCubeSize; x++) {
			for(int y = startY; y < startY + this.clearCubeSize; y++) {
				for(int z = startZ; z < startZ + this.clearCubeSize; z++) {
					
					if(x == center.getBlockX() && y == center.getBlockY() && z == center.getBlockZ()) {
						continue;
					}
					
					Block block = world.getBlockAt(x, y, z);
					
					if(!isLeaf(block)) {
						continue;
					}
					
					if(Citadel.getInstance().getReinforcementManager().getReinforcement(block.getLocation()) != null) {
						cannotBypass = true;
						continue;
					}
					
					block.setType(Material.AIR);

					ParticleEffect effect = new ParticleEffect(Particle.SMOKE_NORMAL, 0.0f, 0.0f, 0.0f, 3, 32);
					effect.playEffect(block.getLocation());
				}
			}
		}
		
		if(cannotBypass) {
			player.sendMessage(this.cannotBypassMessage);
		}
	}
	
	private static boolean isLeaf(Block block) {
		Material blockMaterial = block.getType();
		
		return blockMaterial.equals(Material.OAK_LEAVES) || blockMaterial.equals(Material.BIRCH_LEAVES) || blockMaterial.equals(Material.SPRUCE_LEAVES)
				|| blockMaterial.equals(Material.JUNGLE_LEAVES) || blockMaterial.equals(Material.ACACIA_LEAVES) || blockMaterial.equals(Material.DARK_OAK_LEAVES);
	}
}
