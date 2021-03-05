package com.github.maxopoly.WurstCivTools;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import vg.civcraft.mc.civmodcore.ACivMod;

import com.github.maxopoly.WurstCivTools.anvil.AnvilHandler;
import com.github.maxopoly.WurstCivTools.listener.AnvilListener;
import com.github.maxopoly.WurstCivTools.listener.ToolListener;
import com.github.maxopoly.WurstCivTools.nms.INmsManager;
import com.github.maxopoly.WurstCivTools.tags.Tag;

public class WurstCivTools extends ACivMod {
	
	private static WurstCivTools instance;
	private static WurstManager manager;
	private static AnvilHandler anvilHandler;
	
	private static INmsManager nmsManager;
	public static INmsManager getNmsManager() {
		return nmsManager;
	}
	
	public String getPluginName() {
		return "WurstCivTools";
	}
	
	public void onEnable() {
		instance = this;
		ConfigParser cp = new ConfigParser();
		manager = cp.parse();
		anvilHandler = cp.getAnvilHandler();
		nmsManager = new com.github.maxopoly.WurstCivTools.nms.v1_16_R3.NmsManager();
		registerListeners();
	}
	
	public void onDisable() {
		
	}
	
	public static WurstCivTools getPlugin() {
		return instance;
	}
	
	public static WurstManager getManager() {
		return manager;
	}
	
	public static AnvilHandler getAnvilHandler() {
		return anvilHandler;
	}
	
	public void registerListeners() {
		Bukkit.getPluginManager().registerEvents(new ToolListener(), this);
		if (anvilHandler != null) {
			Bukkit.getPluginManager().registerEvents(new AnvilListener(anvilHandler), this);
		}
		for(List<Tag> tags : manager.getAllTags()){
			for (Tag tag : tags){
				Listener l = tag.getEffect().getListener();
				if (l != null){
					Bukkit.getPluginManager().registerEvents(l, this);
				}
			}
		}
	}
}
