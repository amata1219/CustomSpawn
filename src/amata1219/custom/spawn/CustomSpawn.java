package amata1219.custom.spawn;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class CustomSpawn extends JavaPlugin implements Listener {

	private final static Field console;
	private final static Method getPlayerList, sendMessage, getHandle, getCombatTracker, getDeathMessage;
	private Object server;

	static{
		Class<?> CraftServer = Reflection.getOBCClass("CraftServer");
		console = Reflection.getField(CraftServer, "console");

		Class<?> MinecraftServer = Reflection.getNMSClass("MinecraftServer");
		getPlayerList = Reflection.getMethod(MinecraftServer, "getPlayerList");

		Class<?> PlayerList = Reflection.getNMSClass("PlayerList");
		Class<?> IChatBaseComponent = Reflection.getNMSClass("IChatBaseComponent");
		sendMessage = Reflection.getMethod(PlayerList, "sendMessage", IChatBaseComponent);

		Class<?> CraftPlayer = Reflection.getOBCClass("entity.CraftPlayer");
		getHandle = Reflection.getMethod(CraftPlayer, "getHandle");

		Class<?> EntityLiving = Reflection.getNMSClass("EntityLiving");
		getCombatTracker = Reflection.getMethod(EntityLiving, "getCombatTracker");

		Class<?> CombatTracker = Reflection.getNMSClass("CombatTracker");
		getDeathMessage = Reflection.getMethod(CombatTracker, "getDeathMessage");
	}

	private Yaml config, spawn, database;
	private long waitTime;
	private TextComponent messageOfSetSpawnPoint, messageOfRespawn, messageOfSameSpawnPoint, messageOfCannotMove;

	private final HashMap<String, Location> namesToLocationsMap = new HashMap<>();
	private final HashMap<UUID, String> points = new HashMap<>();

	private final HashMap<UUID, Long> deads = new HashMap<>();

	@Override
	public void onEnable(){
		server = Reflection.getFieldValue(console, getServer());

		config = new Yaml(this, "config.yml");

		spawn = new Yaml(this, "spawn.yml");

		reload();

		database = new Yaml(this, "database.yml");

		//各UUIDとスポーン地点をセットする
		for(String uuid : database.getKeys(false))
			points.put(UUID.fromString(uuid), database.getString(uuid));

		getCommand("customspawn").setExecutor(this);

		for(Player player : getServer().getOnlinePlayers())
			onJoin(new PlayerJoinEvent(player, ""));

		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable(){
		HandlerList.unregisterAll((JavaPlugin) this);

		for(Entry<String, Location> entry : namesToLocationsMap.entrySet())
			spawn.set(entry.getKey(), locationToString(entry.getValue()));

		spawn.save();

		for(Entry<UUID, String> entry : points.entrySet())
			database.set(entry.getKey().toString(), entry.getValue());

		database.save();

		for(Player player : getServer().getOnlinePlayers())
			onQuit(new PlayerQuitEvent(player, ""));
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if(!(sender instanceof Player)){
			sender.sendMessage(ChatColor.RED + "ゲーム内から実行して下さい。");
			return true;
		}

		Player player = (Player) sender;

		if(args.length == 0){
			sender.sendMessage("§b§lCustomSpawn(Spigot 1.12.2) §r§7@ §fdeveloped by amata1219(twitter@amata1219)");
			sender.sendMessage("§7: §b/customspawn list §7@ §f全スポーン地点を表示します。");
			sender.sendMessage("§7: §b/customspawn bind [npc_name] §7@ §fNPCと現在地をバインドします。");
			sender.sendMessage("§7: §b/customspawn unbind [npc_name] §7@ §fNPCとスポーン地点をアンバインドします。");
			sender.sendMessage("§7: §b/customspawn reload §7@ §fconfig.ymlとspawn.ymlを再読み込みします。");
			return true;
		}else if(args[0].equalsIgnoreCase("list")){
			sender.sendMessage(ChatColor.AQUA + ": Information > 全スポーン地点");

			boolean flag = true;

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
		}else if(args[0].equalsIgnoreCase("unbind")){
			if(args.length == 1){
				sender.sendMessage(ChatColor.RED + ": Syntax error > /customspawn unbind [npc_name]");
				return true;
			}

			String npcName = args[1];

			if(!namesToLocationsMap.containsKey(npcName)){
				sender.sendMessage(ChatColor.RED + ": Value error > NPC[" + npcName + "]とバインドされたスポーン地点はありません。");
				return true;
			}

			namesToLocationsMap.remove(npcName);

			sender.sendMessage(ChatColor.AQUA + ": Success > NPC[" + npcName + "]とスポーン地点をアンバインドしました。");
			return true;
		}else if(args[0].equalsIgnoreCase("reload")){
			reload();
			sender.sendMessage(ChatColor.AQUA + ": Success > config.yml, spawn.yml を再読み込みしました。");
			return true;

		}

		return true;
	}

	private void reload(){
		config.reload();

		waitTime = config.getLong("Wait time");

		ConfigurationSection messages = config.getConfigurationSection("Messages");

		messageOfSetSpawnPoint = new TextComponent(color(messages.getString("Set spawn point")));
		messageOfRespawn = new TextComponent(color(messages.getString("Respawn")));
		messageOfSameSpawnPoint = new TextComponent(color(messages.getString("Same spawn point")));
		messageOfCannotMove = new TextComponent(color(messages.getString("Cannot move")));

		spawn.reload();

		namesToLocationsMap.clear();

		//各NPC名と対応座標をセットする
		for(String name : spawn.getKeys(false))
			namesToLocationsMap.put(name, textToLocation(spawn.getString(name)));
	}

	private String color(String text){
		return ChatColor.translateAlternateColorCodes('&', text);
	}

	@EventHandler
	public void onClick(NPCRightClickEvent event){
		String npcName = event.getNPC().getFullName();

		//スポーン地点と結び付けられていなければ戻る
		if(!namesToLocationsMap.containsKey(npcName)) return;

		Player clicker = event.getClicker();
		UUID uuid = clicker.getUniqueId();

		if(points.containsKey(uuid) && points.get(uuid).equals(npcName)){
			clicker.spigot().sendMessage(ChatMessageType.ACTION_BAR, messageOfSameSpawnPoint);
			return;
		}

		points.put(uuid, npcName);

		clicker.spigot().sendMessage(ChatMessageType.ACTION_BAR, messageOfSetSpawnPoint);
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onDead(EntityDamageEvent event){
		Entity victim = event.getEntity();

		if(!(victim instanceof Player)) return;

		Player player = (Player) victim;

		//致死量のダメージでなければ戻る
		if(player.getHealth() > event.getDamage()) return;

		event.setDamage(0D);
		player.setHealth(player.getMaxHealth());


		die(player);

		//死亡ログの表示
		new BukkitRunnable(){

			@Override
			public void run() {
				Object entityPlayer = Reflection.invokeMethod(getHandle, player);
				Object combatTracker = Reflection.invokeMethod(getCombatTracker, entityPlayer);
				Object deathMessage = Reflection.invokeMethod(getDeathMessage, combatTracker);
				Object playerList = Reflection.invokeMethod(getPlayerList, server);
				Reflection.invokeMethod(sendMessage, playerList, deathMessage);
			}

		}.runTaskLater(this, 2);
	}

	@EventHandler
	public void onMove(PlayerMoveEvent event){
		Player player = event.getPlayer();
		if(player.getGameMode() != GameMode.SPECTATOR)
			return;

		if(!deads.containsKey(player.getUniqueId())) return;

		Location from = event.getFrom();
		Location to = event.getTo();

		//移動していればキャンセルする
		if(from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()){
			event.setCancelled(true);
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, messageOfCannotMove);
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event){
		Player player = event.getPlayer();

		//鯖落ち対策としてログイン時にスペクテイター状態を消す
		if(player.getGameMode() == GameMode.SPECTATOR)
			player.setGameMode(GameMode.ADVENTURE);

		if(deads.containsKey(player.getUniqueId()))
			die(player);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event){
		Player player = event.getPlayer();

		if(player.getGameMode() == GameMode.SPECTATOR)
			player.setGameMode(GameMode.ADVENTURE);

		UUID uuid = player.getUniqueId();
		if(deads.containsKey(uuid))
			deads.put(uuid, System.currentTimeMillis());
	}

	public void die(Player player){
		UUID uuid = player.getUniqueId();

		long currentTime = System.currentTimeMillis();

		//残りの待ち時間
		long remainingTime = Math.max((currentTime - deads.getOrDefault(uuid, currentTime)) / 50 + waitTime, 0);

		deads.put(uuid, System.currentTimeMillis());

		player.setGameMode(GameMode.SPECTATOR);

		new BukkitRunnable(){

			@Override
			public void run() {
				//オフラインであれば戻る
				if(!player.isOnline()) return;

				player.setGameMode(GameMode.ADVENTURE);

				Location location;
				if(points.containsKey(uuid) && (location = namesToLocationsMap.get(points.get(uuid))) != null)
					player.teleport(location);
				else
					player.teleport(player.getWorld().getSpawnLocation());

				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, messageOfRespawn);

				deads.remove(uuid);
			}

		}.runTaskLater(this, remainingTime);
	}

	public Location textToLocation(String text){
		String[] args = text.split(",");
		return new Location(Bukkit.getWorld(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]), Float.parseFloat(args[4]), Float.parseFloat(args[5]));
	}

	public String locationToString(Location location){
		return location.getWorld().getName() + ", " + location.getX() + ", " + location.getY() + ", " + location.getZ() + ", " + location.getYaw() + ", " + location.getPitch();
	}

}
