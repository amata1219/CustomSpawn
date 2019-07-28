package amata1219.custom.spawn;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.citizensnpcs.api.event.NPCRightClickEvent;

public class CustomSpawn extends JavaPlugin implements Listener {

	private Yaml config, spawn, database;

	private final HashMap<String, Location> namesToLocationsMap = new HashMap<>();
	private final HashMap<UUID, String> points = new HashMap<>();

	@Override
	public void onEnable(){
		config = new Yaml(this, "config.yml");

		spawn = new Yaml(this, "spawn.yml");

		//各NPC名と対応座標をセットする
		for(String name : spawn.getKeys(false))
			namesToLocationsMap.put(name, textToLocation(spawn.getString(name)));

		database = new Yaml(this, "database.yml");

		//各UUIDとスポーン地点をセットする
		for(String uuid : spawn.getKeys(false))
			points.put(UUID.fromString(uuid), spawn.getString(uuid));

		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable(){
		HandlerList.unregisterAll((JavaPlugin) this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(!(sender instanceof Player)){
			sender.sendMessage(ChatColor.RED + "ゲーム内から実行して下さい。");
			return true;
		}

		Player player = (Player) sender;

		if(args.length == 0){

		}else if(args[0].equalsIgnoreCase("list")){
			sender.sendMessage(ChatColor.AQUA + ": Information > 全スポーン地点");

			boolean flag = false;

			for(Entry<String, Location> entry : namesToLocationsMap.entrySet())
				sender.sendMessage(((flag = !flag) ? ChatColor.AQUA : ChatColor.WHITE) + ": " + entry.getKey() + " @ " + locationToString(entry.getValue()));

			return true;
		}else if(args[0].equalsIgnoreCase("bind")){
			if(args.length == 1){
				sender.sendMessage(ChatColor.RED + ": Syntax error > /customspawn bind [npc_name]");
				return true;
			}

			String npcName = args[1];
			Location location = player.getLocation();

			namesToLocationsMap.put(npcName, location);

			sender.sendMessage(ChatColor.AQUA + ": Success > NPC[" + npcName + "]を現在地点[" + locationToString(location) + "]とバインドしました。");
			return true;
		}else if(args[0].equalsIgnoreCase("remove")){
			if(args.length == 1){
				sender.sendMessage(ChatColor.RED + ": Syntax error > /customspawn remove [npc_name]");
				return true;
			}

			String npcName = args[1];

			if(!namesToLocationsMap.containsKey(npcName)){
				sender.sendMessage(ChatColor.RED + ": Value error > NPC[" + npcName + "]とバインドされたスポーン地点はありません。");
				return true;
			}

			namesToLocationsMap.remove(npcName);

			sender.sendMessage(ChatColor.AQUA + ": Success > NPC[" + npcName + "]とスポーン地点のバインドを解消しました。");
			return true;
		}
	}

	@EventHandler
	public void onClick(NPCRightClickEvent event){
		String npcName = event.getNPC().getFullName();

		//スポーン地点と結び付けられていなければ戻る
		if(!namesToLocationsMap.containsKey(npcName))
			return;

		Player clicker = event.getClicker();
		UUID uuid = clicker.getUniqueId();

		if(points.get(uuid).equals(npcName))
			return;

		points.put(uuid, npcName);

		clicker.sendMessage(ChatColor.AQUA + "スポーン地点を設定しました。");
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent event){
	}

	public Location textToLocation(String text){
		String[] args = text.split(",");
		return new Location(Bukkit.getWorld(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]), Float.parseFloat(args[4]), Float.parseFloat(args[5]));
	}

	public String locationToString(Location location){
		return location.getWorld().getName() + ", " + location.getX() + ", " + location.getY() + ", " + location.getZ() + ", " + location.getYaw() + ", " + location.getPitch();
	}

}
