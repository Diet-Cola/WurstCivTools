package com.github.maxopoly.WurstCivTools.nms.v1_10_R1;

import com.github.maxopoly.WurstCivTools.misc.ReflectionHelper;
import com.github.maxopoly.WurstCivTools.nms.INmsManager;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.ItemStack;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;

public class NmsManager implements INmsManager {
	public boolean damageItem(org.bukkit.inventory.ItemStack item, int damage, Player player) {
		EntityPlayer nmsPlayer = ((CraftPlayer)player).getHandle();
		ItemStack nmsItem = (ItemStack)ReflectionHelper.getFieldValue((CraftItemStack)item, "handle");

		nmsItem.damage(damage, nmsPlayer, null);

		return nmsItem.getCount() == 0;
	}
}
