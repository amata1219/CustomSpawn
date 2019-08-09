package amata1219.custom.spawn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Yaml extends YamlConfiguration {

	public final JavaPlugin plugin;
	public final File file;

	public Yaml(JavaPlugin plugin, String fileName){
		this(plugin, new File(plugin.getDataFolder(), fileName));
	}

	public Yaml(JavaPlugin plugin, File file){
		this.plugin = plugin;
		this.file = file;

		String fileName = file.getName();

		if(!file.exists()) plugin.saveResource(fileName, false);

		reload();
	}

	public void save(){
		try {
			save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void reload(){
		super.map.clear();

		try {
			load(file);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}

		InputStream input = plugin.getResource(file.getName());
		if(input != null)
			setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8)));
	}

	public void update(){
		save();
		reload();
	}

}
