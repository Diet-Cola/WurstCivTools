package com.github.maxopoly.WurstCivTools.listener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import com.github.maxopoly.WurstCivTools.WurstCivTools;
import com.github.maxopoly.WurstCivTools.effect.PlayerHook;
import com.github.maxopoly.WurstCivTools.misc.HookData;
import com.github.maxopoly.WurstCivTools.tags.Tag;

public class PlayerHookListener implements Listener{
	private final PlayerHook effect;
	private final Tag tag;
	private final HashMap<UUID, HookData> hooks = new HashMap<UUID, HookData>();
	private final UUID emptyid = UUID.fromString("00000000-0000-0000-0000-000000000000");
	private final HashSet<UUID> held = new HashSet<UUID>();
	

	public PlayerHookListener(PlayerHook playerHook) {
		this.effect = playerHook;
		this.tag = WurstCivTools.getManager().getEffectTag(this.effect);
	}
	
	public HashMap<UUID,HookData> getHooks(){
		return hooks;
	}
	
	@EventHandler(ignoreCancelled = true)
	public void fishEvent(PlayerFishEvent e){
		ItemStack is = e.getPlayer().getItemInHand();
		if (is == null)
			return;
		if (!tag.appliedOn(is))
			return;
		
		UUID hook = e.getHook().getUniqueId();
		switch (e.getState()){
		case FISHING:
			addhook(hook, e.getPlayer().getUniqueId());
			break;
		
		case CAUGHT_ENTITY:
			removehook(hook);
			break;
			
		default:
			removehook(hook);
			break;
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void pearlThrow(ProjectileLaunchEvent e){
		if (!e.getEntityType().equals(EntityType.ENDER_PEARL))
			return;
		if (!(e.getEntity().getShooter() instanceof Player))
			return;
		
		Player p = (Player) e.getEntity().getShooter();
		UUID uuid = p.getUniqueId();
		for(HookData hook : hooks.values()){
			if (hook.getTarget().equals(uuid)){
				e.setCancelled(true);
				p.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 1));
				break;
			}
		}
	}
	
	
	@EventHandler(ignoreCancelled = true)
	public void hookGrab(ProjectileHitEvent e){
		Entity entity = e.getEntity();
		if (!e.getEntityType().equals(EntityType.FISHING_HOOK))
			return;
		HookData hook = hooks.get(entity.getUniqueId());
		if (hook == null || hook.getTarget().equals(emptyid) == false)
			return;
		List<Entity> nearby = entity.getNearbyEntities(0, 0, 0);
		for(Entity ent : nearby){
			if (!(ent instanceof Player) || ent.getUniqueId().equals(hook.getSource()))
				continue;
			applyhook((Player)ent, hook);
			return;
		}
		
		// Delayed detection of entity hooked. Provides better accuracy, as ProjectileHitEvent
		//  doesn't provide any instance of block or entity hit, only projectile entity.
		// Will only happen if initial detection does not succeed
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(WurstCivTools.getPlugin(), new Runnable(){
			@Override
			public void run() {
				List<Entity> nearby = entity.getNearbyEntities(0, 1, 0);
				for(Entity ent : nearby){
					if (!(ent instanceof Player) || ent.getUniqueId().equals(hook.getSource()))
						continue;
					applyhook((Player)ent, hook);
					break;
				}
			}
			
		}, 1);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void playerMove(PlayerMoveEvent e){
		if (held.isEmpty() || held.contains(e.getPlayer().getUniqueId()) == false)
			return;
		
		Location from = e.getFrom(); Location to = e.getTo();
		if (from.getX()!=to.getX() || from.getY()!=to.getY() || from.getZ()!=to.getZ()){
			e.setCancelled(true);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void playerLogout(PlayerQuitEvent e){
		UUID uuid = e.getPlayer().getUniqueId();
		for (Entry<UUID, HookData> hook : hooks.entrySet()){
			HookData dat = hook.getValue();
			if (dat.getSource().equals(uuid) || dat.getTarget().equals(uuid)){
				removehook(hook.getKey());
			}
		}
	}
	
	public void removehook(UUID hookid){
		HookData dat = hooks.get(hookid); if (dat==null)return;
		if (!dat.getTarget().equals(emptyid)){
			int count = 0;
			for(HookData hook : hooks.values()){
				if (dat.getTarget().equals(hook.getTarget()))
					count++;
			}
			count--;
			Player p = Bukkit.getPlayer(dat.getTarget());
			if (p != null){
				setspeed(p,count);
			}
		}
		hooks.remove(hookid);
	}
	
	private void addhook(UUID hook, UUID source){
		hooks.put(hook, new HookData(source, emptyid));
	}
	
	private void applyhook(Player player, HookData hook){
		UUID uuid = player.getUniqueId();
		hook.setTarget(uuid);
		int count = 0;
		for(Entry<UUID,HookData> hooks : getHooks().entrySet()){
			if (hooks.getValue().getTarget().equals(uuid)){
				count++;
			}
		}
		setspeed(player, count);
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, .3F);
		player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation().add(0, 1, 0), 1);
		player.getWorld().spawnParticle(Particle.SPELL_WITCH, player.getLocation().add(0, 1, 0), 20);
	}
	
	private void setspeed(Player p, int count) {
		if (count <=0){
			p.setWalkSpeed(0.2F);
			held.remove(p.getUniqueId());
		} else if (count >= effect.stopcount){
			p.setWalkSpeed(0.0F);
			held.add(p.getUniqueId());
		} else {
			double slowby = (effect.speed_change*count);
			if (slowby < -0.2D)
				slowby = -0.2D;
			else if (slowby >= 0D)
				slowby = 0D;
			
			if (slowby != 0D){
				p.setWalkSpeed((float)(0.2D+slowby));
			}
			held.remove(p.getUniqueId());
		}
	}

}
