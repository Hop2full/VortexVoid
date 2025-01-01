package main.vortexvoid;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class VortexVoid extends JavaPlugin implements Listener, TabExecutor {

    private final Map<String, Region> regions = new HashMap<>();
    private Location spawnLocation;
    private double voidYLevel = -1; // Default Y level
    private boolean voidProtectionEnabled = true; // Toggle void protection

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("vv").setExecutor(this);
        getCommand("vv").setTabCompleter(this);
        loadConfigData();
    }

    @Override
    public void onDisable() {
        saveConfigData();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Invalid usage. Use /vv <subcommand>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setspawn":
                if (player.hasPermission("vortexvoid.admin")) {
                    spawnLocation = player.getLocation();
                    saveConfigData();
                    player.sendMessage(ChatColor.GREEN + "Spawn location set successfully.");
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                }
                break;

            case "sety":
                if (player.hasPermission("vortexvoid.admin")) {
                    if (args.length != 2) {
                        player.sendMessage(ChatColor.RED + "Usage: /vv sety <y-level>");
                        return true;
                    }
                    try {
                        voidYLevel = Double.parseDouble(args[1]);
                        saveConfigData();
                        player.sendMessage(ChatColor.GREEN + "Void Y-level set to " + voidYLevel);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid Y-level. Please enter a number.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                }
                break;

            case "pos1":
            case "pos2":
                if (player.hasPermission("vortexvoid.admin")) {
                    String regionName = args.length > 1 ? args[1] : "default";
                    regions.putIfAbsent(regionName, new Region());
                    Region region = regions.get(regionName);

                    if (args[0].equalsIgnoreCase("pos1")) {
                        region.setPos1(player.getLocation());
                        saveConfigData();
                        player.sendMessage(ChatColor.GREEN + "Position 1 set for region: " + regionName);
                    } else {
                        region.setPos2(player.getLocation());
                        saveConfigData();
                        player.sendMessage(ChatColor.GREEN + "Position 2 set for region: " + regionName);
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                }
                break;

            case "create":
                if (player.hasPermission("vortexvoid.admin")) {
                    if (args.length != 2) {
                        player.sendMessage(ChatColor.RED + "Usage: /vv create <region-name>");
                        return true;
                    }
                    String regionName = args[1];
                    if (regions.containsKey(regionName)) {
                        player.sendMessage(ChatColor.RED + "Region with this name already exists.");
                    } else {
                        regions.put(regionName, new Region());
                        saveConfigData();
                        player.sendMessage(ChatColor.GREEN + "Region " + regionName + " created successfully.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                }
                break;

            case "delete":
                if (player.hasPermission("vortexvoid.admin")) {
                    if (args.length != 2) {
                        player.sendMessage(ChatColor.RED + "Usage: /vv delete <region-name>");
                        return true;
                    }
                    String regionName = args[1];
                    if (regions.remove(regionName) != null) {
                        saveConfigData();
                        player.sendMessage(ChatColor.GREEN + "Region " + regionName + " deleted successfully.");
                    } else {
                        player.sendMessage(ChatColor.RED + "Region not found.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                }
                break;

            case "list":
                if (player.hasPermission("vortexvoid.admin")) {
                    player.sendMessage(ChatColor.YELLOW + "Regions:");
                    regions.keySet().forEach(region -> player.sendMessage(ChatColor.GREEN + "- " + region));
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                }
                break;

            case "spawn":
                if (spawnLocation != null) {
                    player.teleport(spawnLocation);
                    player.sendMessage(ChatColor.GREEN + "Teleported to spawn.");
                } else {
                    player.sendMessage(ChatColor.RED + "Spawn location is not set.");
                }
                break;

            case "toggle":
                if (player.hasPermission("vortexvoid.admin")) {
                    voidProtectionEnabled = !voidProtectionEnabled;
                    saveConfigData();
                    player.sendMessage(ChatColor.GREEN + "Void protection " + (voidProtectionEnabled ? "enabled" : "disabled") + ".");
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                }
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /vv <subcommand>");
                break;
        }
        return true;
    }

    @EventHandler
    public void onPlayerFall(PlayerMoveEvent event) {
        if (!voidProtectionEnabled) return;

        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to != null && to.getY() < voidYLevel) {
            if (isInRegion(to)) {
                if (spawnLocation != null) {
                    player.teleport(spawnLocation);
                    player.sendTitle(ChatColor.RED + "Oops!", "", 10, 70, 20);
                    Bukkit.broadcastMessage(ChatColor.GOLD + "VortexVoid>> " + ChatColor.RED + player.getName() + " fell into the void and was protected by VortexVoid, silly player!");
                } else {
                    player.sendMessage(ChatColor.RED + "Spawn location is not set. Contact an admin.");
                }
            }
        }
    }

    private boolean isInRegion(Location location) {
        for (Region region : regions.values()) {
            if (region.isInRegion(location)) {
                return true;
            }
        }
        return false;
    }

    private void loadConfigData() {
        FileConfiguration config = getConfig();

        // Load spawn location
        if (config.contains("spawn")) {
            spawnLocation = config.getLocation("spawn");
        }

        // Load void Y-level and toggle
        voidYLevel = config.getDouble("voidYLevel", -1);
        voidProtectionEnabled = config.getBoolean("voidProtectionEnabled", true);

        // Load regions
        ConfigurationSection regionsSection = config.getConfigurationSection("regions");
        if (regionsSection != null) {
            for (String name : regionsSection.getKeys(false)) {
                ConfigurationSection regionSection = regionsSection.getConfigurationSection(name);
                if (regionSection != null) {
                    Location pos1 = regionSection.getLocation("pos1");
                    Location pos2 = regionSection.getLocation("pos2");
                    Region region = new Region();
                    region.setPos1(pos1);
                    region.setPos2(pos2);
                    regions.put(name, region);
                }
            }
        }
    }

    private void saveConfigData() {
        FileConfiguration config = getConfig();

        // Save spawn location
        if (spawnLocation != null) {
            config.set("spawn", spawnLocation);
        }

        // Save void Y-level and toggle
        config.set("voidYLevel", voidYLevel);
        config.set("voidProtectionEnabled", voidProtectionEnabled);

        // Save regions
        ConfigurationSection regionsSection = config.createSection("regions");
        for (Map.Entry<String, Region> entry : regions.entrySet()) {
            String name = entry.getKey();
            Region region = entry.getValue();
            ConfigurationSection regionSection = regionsSection.createSection(name);
            regionSection.set("pos1", region.pos1);
            regionSection.set("pos2", region.pos2);
        }

        saveConfig();
    }

    private static class Region {
        private Location pos1;
        private Location pos2;

        public void setPos1(Location pos1) {
            this.pos1 = pos1;
        }

        public void setPos2(Location pos2) {
            this.pos2 = pos2;
        }

        public boolean isInRegion(Location location) {
            if (pos1 == null || pos2 == null) return false;
            double x1 = Math.min             (pos1.getX(), pos2.getX());
            double x2 = Math.max(pos1.getX(), pos2.getX());
            double y1 = Math.min(pos1.getY(), pos2.getY());
            double y2 = Math.max(pos1.getY(), pos2.getY());
            double z1 = Math.min(pos1.getZ(), pos2.getZ());
            double z2 = Math.max(pos1.getZ(), pos2.getZ());

            return location.getX() >= x1 && location.getX() <= x2 &&
                    location.getY() >= y1 && location.getY() <= y2 &&
                    location.getZ() >= z1 && location.getZ() <= z2;
        }
    }
}

