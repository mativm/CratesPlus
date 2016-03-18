package plus.crates.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import plus.crates.Crate;
import plus.crates.CratesPlus;
import plus.crates.Handlers.CrateHandler;
import plus.crates.Handlers.MessageHandler;
import plus.crates.Utils.Hologram;
import plus.crates.Utils.ReflectionUtil;
import plus.crates.Utils.SignInputHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class CrateCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String string, String[] args) {

		if (sender instanceof Player && !sender.hasPermission("cratesplus.admin")) {
			//if (args.length == 0 || (args.length > 0 && args[0].equalsIgnoreCase("claim"))) {
			//	// Assume player and show "claim" GUI
			//	doClaim((Player) sender);
			//	return true;
			//}
			sender.sendMessage(CratesPlus.getPluginPrefix() + MessageHandler.getMessage(CratesPlus.getPlugin(), "Command No Permission", (Player) sender, null, null));
			return false;
		}

		//if (args.length >= 1 && args[0].equalsIgnoreCase("claim")) {
		//	if (!(sender instanceof Player)) {
		//		sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + "This command must be ran as a player");
		//		return false;
		//	}
		//	doClaim((Player) sender);
		//	return true;
		//}

		if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
			CratesPlus.reloadPlugin();
			sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.GREEN + "CratesPlus configuration was reloaded - This feature is not fully tested and may not work correctly");
			return true;
		}

		if (args.length >= 1 && args[0].equalsIgnoreCase("settings")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + "This command must be ran as a player");
				return false;
			}
			CratesPlus.getSettingsHandler().openSettings((Player) sender);
			return true;
		}

		if (args.length >= 1 && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("createbeta"))) {
			if (args[0].equalsIgnoreCase("createbeta") && false) { // TODO Bring back in 4.1/5.0
				// Lets try and open a sign to do the name! :D
				Player player = (Player) sender;
				try {
					Constructor signConstructor = getNMSClass("PacketPlayOutOpenSignEditor").getConstructor(getNMSClass("BlockPosition"));
					Object packet = signConstructor.newInstance(getBlockPosition(player));
					SignInputHandler.injectNetty(player);
					sendPacket(player, packet);
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}

				return true;
			}
			if (args.length < 2) {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + "Correct Usage: /crate create <name>");
				return false;
			}

			String name = args[1];
			FileConfiguration config = CratesPlus.getPlugin().getConfig();
			if (config.isSet("Crates." + name)) {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + name + " crate already exists");
				return false;
			}

			// Setup example item
			config.set("Crates." + name + ".Winnings.1.Type", "ITEM");
			config.set("Crates." + name + ".Winnings.1.Item Type", "IRON_SWORD");
			config.set("Crates." + name + ".Winnings.1.Item Data", 0);
			config.set("Crates." + name + ".Winnings.1.Percentage", 0);
			config.set("Crates." + name + ".Winnings.1.Name", "&6&lExample Sword");
			config.set("Crates." + name + ".Winnings.1.Amount", 1);

			// Setup key with defaults
			config.set("Crates." + name + ".Key.Item", "TRIPWIRE_HOOK");
			config.set("Crates." + name + ".Key.Name", "%type% Crate Key");
			config.set("Crates." + name + ".Key.Enchanted", true);

			config.set("Crates." + name + ".Knockback", 0.0);
			config.set("Crates." + name + ".Broadcast", false);
			config.set("Crates." + name + ".Firework", false);
			config.set("Crates." + name + ".Preview", true);
			config.set("Crates." + name + ".Block", "CHEST");
			config.set("Crates." + name + ".Color", "WHITE");
			CratesPlus.getPlugin().saveConfig();
			CratesPlus.getPlugin().reloadConfig();

			CratesPlus.getConfigHandler().getCrates().put(name.toLowerCase(), new Crate(name));
			CratesPlus.getSettingsHandler().setupCratesInventory();

			sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.GREEN + name + " crate has been created");
			return true;
		}

		if (args.length >= 1 && args[0].equalsIgnoreCase("h")) {
			Hologram hologram = new Hologram(new Location(Bukkit.getWorld("world"), Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3])), "Test #1", "Test #2", "Test #3");
			hologram.displayAll();
		} else if (args.length >= 1 && args[0].equalsIgnoreCase("rename")) {
			if (args.length < 3) {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + "Correct Usage: /crate rename <old name> <new name>");
				return false;
			}

			String oldName = args[1];
			String newName = args[2];

			if (!CratesPlus.getConfigHandler().getCrates().containsKey(oldName.toLowerCase())) {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + oldName + " crate was not found");
				return false;
			}
			Crate crate = CratesPlus.getConfigHandler().getCrates().get(oldName.toLowerCase());

			FileConfiguration config = CratesPlus.getPlugin().getConfig();
			if (config.isSet("Crates." + newName)) {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + newName + " crate already exists");
				return false;
			}

			for (String id : CratesPlus.getPlugin().getConfig().getConfigurationSection("Crates." + crate.getName(false) + ".Winnings").getKeys(false)) {
				String path = "Crates." + crate.getName(false) + ".Winnings." + id;
				String newPath = "Crates." + newName + ".Winnings." + id;

				if (config.isSet(path + ".Type"))
					config.set(newPath + ".Type", config.getString(path + ".Type"));
				if (config.isSet(path + ".Item Type"))
					config.set(newPath + ".Item Type", config.getString(path + ".Item Type"));
				if (config.isSet(path + ".Item Data"))
					config.set(newPath + ".Item Data", config.getInt(path + ".Item Data"));
				if (config.isSet(path + ".Percentage"))
					config.set(newPath + ".Percentage", config.getDouble(path + ".Percentage"));
				if (config.isSet(path + ".Name"))
					config.set(newPath + ".Name", config.getString(path + ".Name"));
				if (config.isSet(path + ".Amount"))
					config.set(newPath + ".Amount", config.getInt(path + ".Amount"));
				if (config.isSet(path + ".Enchantments"))
					config.set(newPath + ".Enchantments", config.getList(path + ".Enchantments"));
				if (config.isSet(path + ".Commands"))
					config.set(newPath + ".Commands", config.getList(path + ".Commands"));
			}

			config.set("Crates." + newName + ".Knockback", config.getDouble("Crates." + crate.getName(false) + ".Knockback"));
			if (config.isSet("Crates." + crate.getName(false) + ".Block"))
				config.set("Crates." + newName + ".Block", config.getString("Crates." + crate.getName(false) + ".Block"));
			if (config.isSet("Crates." + crate.getName(false) + ".Color"))
				config.set("Crates." + newName + ".Color", config.getString("Crates." + crate.getName(false) + ".Color"));
			if (config.isSet("Crates." + crate.getName(false) + ".Knockback"))
				config.set("Crates." + newName + ".Knockback", config.getDouble("Crates." + crate.getName(false) + ".Knockback"));
			if (config.isSet("Crates." + crate.getName(false) + ".Broadcast"))
				config.set("Crates." + newName + ".Broadcast", config.getBoolean("Crates." + crate.getName(false) + ".Broadcast"));
			if (config.isSet("Crates." + crate.getName(false) + ".Firework"))
				config.set("Crates." + newName + ".Firework", config.getBoolean("Crates." + crate.getName(false) + ".Firework"));
			if (config.isSet("Crates." + crate.getName(false) + ".Preview"))
				config.set("Crates." + newName + ".Preview", config.getBoolean("Crates." + crate.getName(false) + ".Preview"));

			config.set("Crates." + crate.getName(false), null);
			CratesPlus.getPlugin().saveConfig();
			CratesPlus.getPlugin().reloadConfig();

			CratesPlus.getConfigHandler().getCrates().remove(oldName.toLowerCase());
			CratesPlus.getConfigHandler().getCrates().put(newName.toLowerCase(), new Crate(newName));
			CratesPlus.getSettingsHandler().setupCratesInventory();

			sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.GREEN + oldName + " has been renamed to " + newName);
			return true;
		}

		if (args.length >= 1 && args[0].equalsIgnoreCase("delete")) {
			if (args.length < 2) {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + "Correct Usage: /crate delete <name>");
				return false;
			}

			String name = args[1];
			FileConfiguration config = CratesPlus.getPlugin().getConfig();
			if (!config.isSet("Crates." + name)) {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + name + " crate doesn't exist");
				return false;
			}

			config.set("Crates." + name, null);
			CratesPlus.getPlugin().saveConfig();
			CratesPlus.getPlugin().reloadConfig();
			CratesPlus.getConfigHandler().getCrates().remove(name.toLowerCase());
			CratesPlus.getSettingsHandler().setupCratesInventory();

			sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.GREEN + name + " crate has been deleted");
			return true;
		}

		if (args.length >= 1 && args[0].equalsIgnoreCase("key")) {
			if (args.length < 2) {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + "Correct Usage: /crate key <player/all> [type] [amount]");
				return false;
			}

			Integer amount = 1;
			if (args.length > 3) {
				try {
					amount = Integer.parseInt(args[3]);
				} catch (Exception ignored) {
					sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + "Invalid amount");
					return false;
				}
			}

			Player player = null;
			if (!args[1].equalsIgnoreCase("all")) {
				player = Bukkit.getPlayer(args[1]);
				if (player == null) {
					sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + "The player " + args[1] + " was not found");
					return false;
				}
			}

			String crateType = null;
			if (args.length >= 3) {
				crateType = args[2];
			}

			if (crateType != null) {

				if (CratesPlus.getConfigHandler().getCrates().get(crateType.toLowerCase()) == null) {
					sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + "Crate not found");
					return false;
				}

				if (player == null) {
					for (Player p : Bukkit.getOnlinePlayers()) {
						CrateHandler.giveCrateKey(p, crateType, amount);
					}
				} else {
					CrateHandler.giveCrateKey(player, crateType, amount);
				}
			} else {
				if (player == null) {
					for (Player p : Bukkit.getOnlinePlayers()) {
						CrateHandler.giveCrateKey(p);
					}
				} else {
					CrateHandler.giveCrateKey(player);
				}
			}

			if (player == null) {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.GREEN + "Given all players a crate key");
			} else {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.GREEN + "Given " + player.getDisplayName() + ChatColor.RESET + ChatColor.GREEN + " a crate key");
			}
			return true;
		}

		if (args.length >= 1 && args[0].equalsIgnoreCase("crate")) {
			Player player;
			String crateType;
			if (args.length == 1) {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + "Correct Usage: /crate crate <type> [player]");
				return false;
			}

			if (args.length == 3) {
				player = Bukkit.getPlayer(args[2]);
			} else if (sender instanceof Player) {
				player = (Player) sender;
			} else {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + "Correct Usage: /crate crate <type> [player]");
				return false;
			}

			if (player == null) {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + "The player " + args[2] + " was not found");
				return false;
			}

			try {
				crateType = args[1];
			} catch (IllegalArgumentException e) {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + "Please specify a valid crate type");
				return false;
			}

			if (CratesPlus.getConfigHandler().getCrates().get(crateType.toLowerCase()) == null) {
				sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + "Crate not found");
				return false;
			}

			CrateHandler.giveCrate(player, crateType);

			sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.GREEN + "Given " + player.getDisplayName() + ChatColor.RESET + ChatColor.GREEN + " a crate");
			return true;
		}

		if (args.length > 0 && !args[0].equalsIgnoreCase("help")) {
			sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.RED + "Unknown args");
			return false;
		}

		// Help Messages
		sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.AQUA + "----- CratePlus v" + CratesPlus.getPlugin().getDescription().getVersion() + " Help -----");
		sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate reload " + ChatColor.YELLOW + "- Reload configuration for CratesPlus (Experimental)");
		sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate settings " + ChatColor.YELLOW + "- Edit settings of CratesPlus and crate winnings");
		sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate create <name> " + ChatColor.YELLOW + "- Create a new crate");
		sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate rename <old name> <new name> " + ChatColor.YELLOW + "- Rename a new crate");
		sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate delete <name> " + ChatColor.YELLOW + "- Delete a crate");
		sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate key <player/all> [type] [amount] " + ChatColor.YELLOW + "- Give player a random crate key");
		sender.sendMessage(CratesPlus.getPluginPrefix() + ChatColor.AQUA + "/crate crate <type> [player] " + ChatColor.YELLOW + "- Give player a crate to be placed");

		return true;
	}

	private void doClaim(Player player) {
		if (!CrateHandler.hasPendingKeys(player.getUniqueId())) {
			// TODO Send message
			return;
		}
		Integer size = CrateHandler.getPendingKey(player.getUniqueId()).size();
		if (size < 9)
			size = 9;
		else if (size <= 18)
			size = 18;
		else if (size <= 27)
			size = 27;
		else if (size <= 36)
			size = 36;
		else if (size <= 45)
			size = 45;
		else
			size = 54;
		Inventory inventory = Bukkit.createInventory(null, size, "Claim Crate Keys");
		Integer i = 0;
		for (Map.Entry<String, Integer> map : CrateHandler.getPendingKey(player.getUniqueId()).entrySet()) {
			String crateName = map.getKey();
			Integer amount = map.getValue();
			Crate crate = CratesPlus.getConfigHandler().getCrates().get(crateName.toLowerCase());
			if (crate == null)
				return; // Crate must have been removed?
			ItemStack keyItem = crate.getKey().getKeyItem(amount);
			inventory.setItem(i, keyItem);
			i++;
		}
		player.openInventory(inventory);
	}

	@Deprecated
	public Class<?> getNMSClass(String name) {
		return ReflectionUtil.getNMSClass(name);
	}

	public Object getBlockPosition(Player player) {
		try {
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Constructor constructor = getNMSClass("BlockPosition").getConstructor(getNMSClass("Entity"));
			return constructor.newInstance(handle);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void sendPacket(Player player, Object packet) {
		try {
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
			playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
