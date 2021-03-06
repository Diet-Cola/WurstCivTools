package com.github.maxopoly.WurstCivTools.effect;

import com.github.maxopoly.WurstCivTools.WurstCivTools;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.model.Reinforcement;

public class AOEMiner extends WurstEffect {

    private int radius;
    private String cannotBypassMessage;
    private double durabilityLossChance;
    private Random rnd;

    public AOEMiner(int radius, String cannotBypassMessage, double durabilityLossChance) {
        super();
        this.rnd = new Random();
        this.radius = radius;
        this.cannotBypassMessage = cannotBypassMessage;
        this.durabilityLossChance = durabilityLossChance;
    }

    @Override
    public void handleBreak(Player p, BlockBreakEvent e) {
        if (e.isCancelled()) {
            return;
        }

        if (p == null) {
            return;
        }

        Reinforcement rein = Citadel.getInstance().getReinforcementManager().getReinforcement(e.getBlock());
        if (rein != null) {
            return;
        }

        e.setCancelled(true);

        final ItemStack handItem = p.getInventory().getItemInMainHand();

        //Following manipulations are needed to synchronize tool durability with client after cancel block break
        handItem.setDurability((short) (handItem.getDurability() + 1));

        final Player player = p;
        final Block center = e.getBlock();

        Bukkit.getScheduler().runTask(WurstCivTools.getPlugin(), () -> {
            handItem.setDurability((short) (handItem.getDurability() - 1));

            damageItem(handItem, player);

            mineAOE(center, getBlockFace(player));
        });
    }

    private int getDamage() {
        return this.rnd.nextDouble() < this.durabilityLossChance ? 1 : 0;
    }

    private void damageItem(ItemStack item, Player player) {
        int damage = getDamage();

        if (damage == 0) {
            return;
        }

        if (WurstCivTools.getNmsManager().damageItem(item, damage, player)) {
            player.getInventory().remove(player.getInventory().getItemInMainHand());
        }
    }

    private void mineAOE(Block center, BlockFace face) {
        List<Block> blocksToMine = getDirectionalAOE(face, center, radius);
        for (Block b : blocksToMine) {
            if (b.getType().isAir()) {
                continue;
            }
            b.getWorld().dropItemNaturally(b.getLocation(), new ItemStack(b.getType()));
            b.setType(Material.AIR);
        }
    }

    private List<Block> getDirectionalAOE(BlockFace face, Block block, int radius) {
        List<Block> blocks = new ArrayList<>();
        for (int xMod = -radius; xMod <= radius; xMod++) {
            for (int yMod = -radius; yMod <= radius; yMod++) {
                for (int zMod = -radius; zMod <= radius; zMod++) {
                    Block currentBlock;
                    switch (face) {
                        case NORTH:
                        case SOUTH:
                            currentBlock = block.getRelative(xMod, yMod, 0);
                            break;
                        case EAST:
                        case WEST:
                            currentBlock = block.getRelative(0, yMod, zMod);
                            break;
                        case UP:
                        case DOWN:
                            currentBlock = block.getRelative(xMod, 0, zMod);
                            break;
                        default:
                            return blocks;
                    }

                    if (currentBlock.getType() == Material.BEDROCK) {
                        continue;
                    }

                    Reinforcement reinforcement =
                            Citadel.getInstance().getReinforcementManager().getReinforcement(currentBlock);
                    if (reinforcement != null) {
                        //TODO: Send player msg saying some block were reinforced
                        continue;
                    }

                    blocks.add(currentBlock);
                }
            }
        }
        return blocks;
    }

    private BlockFace getBlockFace(Player player) {
        float pitch = player.getEyeLocation().getPitch();
        float yaw = player.getEyeLocation().getYaw();
        if (pitch < -45) {
            return BlockFace.UP;
        } else if (pitch > 45) {
            return BlockFace.DOWN;
        } else if (yaw > 135 || yaw < -135) {
            return BlockFace.NORTH;
        } else if (yaw < -45) {
            return BlockFace.EAST;
        } else if (yaw > 45) {
            return BlockFace.WEST;
        } else {
            return BlockFace.SOUTH;
        }
    }
}
