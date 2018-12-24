package net.h31ix.travelpad;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.buildatnight.unity.Unity;
import jdk.nashorn.internal.codegen.types.Type;
import net.h31ix.travelpad.api.Configuration;
import net.h31ix.travelpad.api.Pad;
import net.h31ix.travelpad.api.TravelPadManager;
import net.h31ix.travelpad.api.UnnamedPad;
import net.h31ix.travelpad.tasks.SyncMeta;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class Travelpad extends JavaPlugin {
    private Configuration config;
    private TravelPadManager manager;
    private LangManager l;
    private SyncMeta syncMeta;
    private BukkitTask syncMetaTask;

    private Economy economy;

    public static final String PLUGIN_PREFIX_COLOR = ChatColor.DARK_GRAY + "[" + ChatColor.BLUE + "TravelPads" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
    public static final UUID ADMIN_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final String DELIMINATOR = "/";

    @Override
    public void onDisable() {

    }

    @Override
    public void onEnable() {
        if (!new File("plugins/TravelPad/config.yml").exists()) {
            saveDefaultConfig();
        }
        if (!new File("plugins/TravelPad/pads.yml").exists()) {
            try {
                new File("plugins/TravelPad/pads.yml").createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(Travelpad.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (!new File("plugins/TravelPad/lang.yml").exists()) {
            saveResource("lang.yml", false);
        }
        //TODO: Remove this for production, change to auto update method
        //Force lang update flag for development
        saveResource("lang.yml", true);

        config = new Configuration(this);
        manager = new TravelPadManager(this);
        l = new LangManager();
        /* Test for lang
        for(Method m:l.getClass().getMethods()){
            if(m.getGenericReturnType()==String.class) {
                try {
                    log(m.getName()+"="+m.invoke(l));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException ex) {
                    ex.printStackTrace();
                }
            }
        }
        */
        if (config.economyEnabled) {
            setupEconomy();
        }
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new TravelPadBlockListener(this), this);
        pm.registerEvents(new TravelPadListener(this), this);
        getCommand("travelpad").setExecutor(new TravelPadCommandExecutor(this));
        syncMeta = new SyncMeta(this);
        syncMetaTask = getServer().getScheduler().runTaskAsynchronously(this, syncMeta);
    }

    public boolean namePad(Player player, String name) {
        Object[] pads = manager.getUnnamedPadsFrom(player.getUniqueId()).toArray();
        if (pads.length == 0) {
            return false;
        } else {
            UnnamedPad pad = ((UnnamedPad) pads[0]);
            manager.switchPad(pad, name);
            return true;
        }
    }

    public boolean hasPad(Player player) {
        List<Pad> pads = manager.getPadsFrom(player.getUniqueId());
        if (pads.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    //TODO: simplify
    public double getRandom() {
        int x = (int) (2 * Math.random()) + 1;
        double e = (4 * Math.random()) + 1;
        if (x == 2) {
            e = 0 - e;
        }
        return e;
    }

    public void delete(Pad pad) {
        double returnValue = config.deleteAmount;
        if (returnValue != 0) {
            refund(getServer().getPlayer(pad.ownerUUID()));
        }
        manager.deletePad(pad);
    }

    public void create(Location location, Player player) {
        double createValue = config.createAmount;
        if (createValue != 0) {
            chargeCreate(player);
        }
        manager.createPad(location, player.getUniqueId());
        message(player, l.create_approve_1());
        message(player, l.create_approve_2());
    }

    public void teleport(Player player, Location loc) {
        boolean tp = true;
        boolean take = false;
        boolean found = false;
        ItemStack s = null;
        loc.setY(loc.getY() + 1);
        if (!Manager().isSafe(loc, player)) {
            //player.sendMessage("X:"+loc.getX()+" Y:"+loc.getY()+" Z:"+loc.getZ());
            player.sendMessage(ChatColor.RED + l.travel_unsafe());
            tp = false;
        }
        if (config.requireItem) {
            s = new ItemStack(config.itemType, 1);
            for (int i = 0; i < player.getInventory().getContents().length; i++) {
                if (player.getInventory().getContents()[i] != null) {
                    if (player.getInventory().getContents()[i].getType().name().equals(s.getType().name())) {
                        if (config.takeItem) {
                            take = true;
                        }
                        found = true;
                    }
                }
            }
            if (!found) {
                errorMessage(player, l.travel_deny_item() + " " + s.getType().name().toLowerCase().replaceAll("_", ""));
                tp = false;
            }
        }
        if (config.chargeTeleport && tp) {
            if (canAffordTeleport(player)) {
                chargeTP(player);
            } else {
                errorMessage(player, l.travel_deny_money());
                tp = false;
            }
        }
        if (take && tp) {
            player.getInventory().removeItem(s);
            player.sendMessage(ChatColor.GOLD + s.getType().name().toLowerCase().replaceAll("_", "") + " " + l.travel_approve_item());
        }
        if (tp) {
            for (int i = 0; i != 32; i++) {
                player.getWorld().playEffect(player.getLocation().add(getRandom(), getRandom(), getRandom()), Effect.SMOKE, 3);
            }
            loc.getChunk().load();
            //Delay teleportation to allow the server time to load the chunk.
            getServer().getScheduler().runTaskLater(this, new Runnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.teleport(loc);
                        player.sendMessage(ChatColor.GREEN + l.travel_message());
                        for (int i = 0; i != 32; i++) {
                            player.getWorld().playEffect(loc.add(getRandom(), getRandom(), getRandom()), Effect.SMOKE, 3);
                        }
                    }
                }
            }, 4);

        }
    }

    public boolean charge(Player player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean refund(Player player, double amount) {
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public void chargeCreate(Player player) {
        if (!player.hasPermission("travelpad.nopay")) {
            economy.withdrawPlayer(player, config.createAmount);
            player.sendMessage(ChatColor.GOLD + l.charge_message() + " " + config.createAmount);
        }
    }

    public void chargeTP(Player player) {
        if (!player.hasPermission("travelpad.nopay")) {
            economy.withdrawPlayer(player, config.teleportAmount);
            player.sendMessage(ChatColor.GOLD + l.charge_message() + " " + config.teleportAmount);
        }
    }

    public void refund(Player player) {
        if (!player.hasPermission("travelpad.nopay")) {
            economy.depositPlayer(player, config.deleteAmount);
            player.sendMessage(ChatColor.GOLD + l.refund_message() + " " + config.deleteAmount);
        }
    }

    public void refundNoCreate(Player player) {
        if (!player.hasPermission("travelpad.nopay")) {
            economy.depositPlayer(player, config.createAmount);
            player.sendMessage(ChatColor.GOLD + l.refund_message() + " " + config.deleteAmount);
        }
    }

    public boolean canAffordTeleport(Player player) {
        if (player.hasPermission("travelpad.nopay")) {
            return true;
        } else if (!config.economyEnabled) {
            return true;
        }
        double balance = economy.getBalance(player);
        if (balance >= config.teleportAmount) {
            return true;
        } else {
            return false;
        }
    }

    public int getAllowedPads(Player player) {
        if (player.hasPermission("travelpad.infinite")) {
            return -1;
        } else {
            int allowed = 1;
            for (int i = 0; i <= 100; i++) {
                if (player.hasPermission("travelpad.max." + i)) {
                    allowed = i;
                }
            }
            return allowed;
        }
    }

    public boolean canAfford(Player player, double amount) {
        return economy.has(player, amount);
    }

    private Boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }


    public int getPads(Player player) {
        List<Pad> pads = manager.getPadsFrom(player.getUniqueId());
        int has = 0;
        if (pads != null) {
            has = pads.size();
        }
        return has;
    }

    public boolean canCreate(Player player) {
        if (player.hasPermission("travelpad.create") || player.hasPermission("travelpad.admin") || player.isOp()) {
            List<UnnamedPad> upads = manager.getUnnamedPadsFrom(player.getUniqueId());
            if (!upads.isEmpty()) {
                errorMessage(player, l.create_deny_waiting());
                return false;
            }
            if (config.economyEnabled) {
                if (!(economy.getBalance(player) >= config.createAmount)) {
                    errorMessage(player, l.create_deny_money());
                    return false;
                }
            }
            int allow = getAllowedPads(player);
            List<Pad> pads = manager.getPadsFrom(player.getUniqueId());
            int has = 0;
            if (pads != null) {
                has = pads.size();
            }
            if (allow < 0 || allow > has) {
                return true;
            } else {
                errorMessage(player, l.create_deny_max());
                return false;
            }
        } else {
            errorMessage(player, l.command_deny_permission());
            return false;
        }
    }

    public TravelPadManager Manager() {
        return manager;
    }

    public LangManager Lang() {
        return l;
    }

    public Configuration Config() {
        return config;
    }

    public SyncMeta Meta() {
        return syncMeta;
    }

    public static void log(String str) {
        Bukkit.getLogger().info(PLUGIN_PREFIX_COLOR + " " + str);
    }

    public static void error(String str) {
        Bukkit.getLogger().severe(PLUGIN_PREFIX_COLOR + " " + str);
    }

    public static String formatLocation(Location loc) {
        if (loc != null) {
            if (loc.getWorld() != null)
                return loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " " + loc.getWorld().getName();
            else
                return loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " null world.";
        } else {
            return "Location is null, File corruption may ensue";
        }
    }

    public void errorMessage(CommandSender sender, String message) {
        sender.sendMessage(PLUGIN_PREFIX_COLOR + ChatColor.RED + message);
    }

    public void message(CommandSender sender, String message) {
        sender.sendMessage(PLUGIN_PREFIX_COLOR + ChatColor.GREEN + message);
    }

    public String getPlayerName(UUID playersUUID) {
        OfflinePlayer oPlayer = getServer().getOfflinePlayer(playersUUID);
        if (oPlayer != null) {
            return oPlayer.getName();
        }
        //TODO: Fallback to unity next (allow it to MjAPI?)
        return null;
    }

    public UUID getPlayerUUIDbyName(String playerName) {
        if (playerName.equalsIgnoreCase("Admin")) {
            return ADMIN_UUID;
        } else {
            OfflinePlayer pl = Bukkit.getPlayer(playerName);
            if (pl == null) {
                pl = Bukkit.getOfflinePlayer(Unity.getUnityHandle().getCache().fetchUUID(playerName));
            }
            if (pl != null) {
                return pl.getUniqueId();
            }
        }
        return null;
    }
}

