package com.github.maxopoly.WurstCivTools.effect;

import com.github.maxopoly.WurstCivTools.WurstCivTools;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.model.Reinforcement;
import vg.civcraft.mc.civmodcore.api.BlockAPI;

public class VeinMiner extends WurstEffect{

    private int limit;
    private String cannotBypassMessage;
    private double durabilityLossChance;
    private Random rnd;
    private int counter;

    public VeinMiner(int limit, String cannotBypassMessage, double durabilityLossChance) {
        super();
        this.limit = limit;
        this.cannotBypassMessage = cannotBypassMessage;
        this.durabilityLossChance = durabilityLossChance;
        this.rnd = new Random();
        this.counter = 0;
    }

    @Override
    public void handleBreak(Player p, BlockBreakEvent e) {
        counter = 0;

        if (e.isCancelled()) {
            return;
        }

        if (p == null) {
            return;
        }

        if (!isOre(e.getBlock())) {
            return;
        }

        Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(e.getBlock());
        if (rein != null) {
            return;
        }

        e.setCancelled(true);
        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(e.getBlock().getType()));
        e.getBlock().setType(Material.AIR);

        final ItemStack handItem = p.getInventory().getItemInMainHand();

        //Following manipulations are needed to synchronize tool durability with client after cancel block break
        handItem.setDurability((short)(handItem.getDurability() + 1));

        final Player player = p;
        final Block firstOre = e.getBlock();

        Bukkit.getScheduler().runTask(WurstCivTools.getPlugin(), () -> {
            handItem.setDurability((short)(handItem.getDurability() - 1));

            damageItem(handItem, player);

            veinMine(firstOre);
        });
    }

    private boolean isOre(Block block) {
        Material material = block.getType();
        switch (material) {
            case COAL_ORE:
            case IRON_ORE:
            case GOLD_ORE:
            case NETHER_GOLD_ORE:
            case DIAMOND_ORE:
            case ANCIENT_DEBRIS:
            case NETHER_QUARTZ_ORE:
                return true;
            default:
                return false;
        }
    }

    private int getDamage() {
        return this.rnd.nextDouble() < this.durabilityLossChance ? 1: 0;
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

    private void veinMine(Block ore) {
        List<Block> touchingOres = BlockAPI.getAllSides(ore);
        for (Block b : touchingOres) {
            if (!isOre(b)) {
                continue;
            }
            Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(b);
            if (rein != null) {
                return;
            }
            if (counter >= limit) {
                return;
            }
            b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(b.getType()));
            b.setType(Material.AIR);
            counter++;
            veinMine(b);
        }
    }
}
