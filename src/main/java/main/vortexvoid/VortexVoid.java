package main.vortexvoid;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
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
    private Location globalSpawnLocation;
    private double voidYLevel = -1; // Default Y level
    private boolean voidProtectionEnabled = true; // Toggle void protection

    private WorldGuardPlugin worldGuard;

    @Override
    public void onEnable() {
        // Register WorldGuard plugin
        worldGuard = (WorldGuardPlugin) getServer().getPluginManager().getPlugin("WorldGuard");
        if (worldGuard == null) {
            getLogger().warning("WorldGuard plugin not found! VortexVoid may not work properly.");
        }

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
                    globalSpawnLocation = player.getLocation();
                    saveConfigData();
                    player.sendMessage(ChatColor.GREEN + "Global spawn location set successfully.");
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

            case "list":
                if (player.hasPermission("vortexvoid.admin")) {
                    player.sendMessage(ChatColor.YELLOW + "Regions:");
                    regions.keySet().forEach(region -> player.sendMessage(ChatColor.GREEN + "- " + region));
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
        if (to == null || to.getY() >= voidYLevel) return;

        // Check if the player is inside a WorldGuard region
        if (worldGuard != null && worldGuard.getRegionManager(player.getWorld()).getRegion(player.getLocation()) != null) {
            if (globalSpawnLocation != null) {
                player.teleport(globalSpawnLocation);
                player.sendTitle(ChatColor.RED + "Oops!", "You fell into the void!", 10, 70, 20);
                Bukkit.broadcastMessage(ChatColor.GOLD + "VortexVoid>> " + ChatColor.RED + player.getName() + " fell into the void and was protected by VortexVoid!");
            } else {
                player.sendMessage(ChatColor.RED + "Global spawn location is not set. Contact an admin.");
            }
        }
    }

    private void loadConfigData() {
        FileConfiguration config = getConfig();

        if (config.contains("globalSpawn")) {
            globalSpawnLocation = config.getLocation("globalSpawn");
        }

        voidYLevel = config.getDouble("voidYLevel", -1);
        voidProtectionEnabled = config.getBoolean("voidProtectionEnabled", true);
    }

    private void saveConfigData() {
        FileConfiguration config = getConfig();

        if (globalSpawnLocation != null) {
            config.set("globalSpawn", globalSpawnLocation);
        }

        config.set("voidYLevel", voidYLevel);
        config.set("voidProtectionEnabled", voidProtectionEnabled);

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
            double x1 = Math.min(pos1.getX(), pos2.getX());
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
