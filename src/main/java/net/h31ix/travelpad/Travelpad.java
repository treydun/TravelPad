package net.h31ix.travelpad;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.buildatnight.pagination.BukkitPaginator;
import com.buildatnight.pagination.Pagination;
import com.buildatnight.pagination.util.Format;
import com.buildatnight.travelpad.Statistics;
import com.buildatnight.unity.Unity;
import net.h31ix.travelpad.api.Configuration;
import net.h31ix.travelpad.api.Pad;
import net.h31ix.travelpad.api.TravelPadManager;
import net.h31ix.travelpad.api.UnnamedPad;
import net.h31ix.travelpad.tasks.SyncMeta;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Travelpad extends JavaPlugin {
    private Configuration config;
    private TravelPadManager manager;
    private LangManager l;
    private SyncMeta syncMeta;
    private int syncMetaTaskID;
    //private HashMap<UUID, String> idToName;

    private Economy economy;
    private Permission permission;

    public static final String PLUGIN_PREFIX_COLOR = ChatColor.DARK_GRAY + "[" + ChatColor.BLUE + "TravelPads" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
    public static final TextComponent PLUGIN_PREFIX_COMPONENT = new TextComponent(TextComponent.fromLegacyText(PLUGIN_PREFIX_COLOR));
    public static final TextComponent NEWLINE = new TextComponent("\n"); //Should be careful where this is used. I suspect it was 'inheriting' a color when i was appending it with a builder
    public static final TextComponent NEWLINE_INDENT;
    public static final UUID ADMIN_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final String DELIMINATOR = "/";
    public static final Pattern isInteger = Pattern.compile("-?\\d+");

    static {
        NEWLINE_INDENT = new TextComponent("\n > ");
        NEWLINE_INDENT.setColor(ChatColor.DARK_GRAY);
    }

    private BukkitPaginator paginator;
    private TextComponent pluginHeader;

    @Override
    public void onDisable() {
        Statistics.getStats();
        getServer().getScheduler().cancelTask(syncMetaTaskID);
        syncMeta.forceSave();
    }

    @Override
    public void onEnable() {
        load();
    }

    private void load(){
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

        paginator = (BukkitPaginator) getServer().getPluginManager().getPlugin("BukkitPaginator");

        if (paginator == null) {
            getLogger().severe("Failed to find BukkitPaginator, DISABLING");
            getServer().getPluginManager().disablePlugin(this);
        }

        /*
        idToName = new HashMap<>(Bukkit.getOfflinePlayers().length);
        //Propogate fast nameMap (Should be unity still :S)
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer != null) {
                UUID uuid = offlinePlayer.getUniqueId();
                String name = offlinePlayer.getName();
                if (uuid == null || name == null) {
                    continue;
                } else {
                    idToName.put(uuid, name);
                }
            }
        }
        */

        //TODO: Remove this for production, change to auto update method
        // Force lang update flag for development
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
        TravelPadCommandExecutor commandExecutor = new TravelPadCommandExecutor(this);
        getCommand("t").setExecutor(commandExecutor);
        getCommand("t").setTabCompleter(commandExecutor);
        syncMeta = new SyncMeta(this);
        syncMetaTaskID = getServer().getScheduler().scheduleSyncRepeatingTask(this, syncMeta, 299L, 500L);
        String[] padding = Format.getPaddingNeeded(" [Travelpads] ", 55, ' ');
        pluginHeader = new TextComponent(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', padding[0] + " &8[&9Travelpads&8] ")));
        //ComponentTests.runTests();
    }

    public void reload(){
        getServer().getScheduler().cancelTasks(this);
        syncMeta.forceSave();
        load();
        Config().reload();
        Manager().reload();
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
            refund(pad.ownerUUID());
        }
        manager.deletePad(pad);
    }

    public void create(Location location, Player player) {
        int padsHas = Manager().getPadsFrom(player.getUniqueId()).size();
        double createValue = config.createAmount * padsHas;
        if (createValue != 0) {
            if (!player.hasPermission("travelpad.create.free")) {
                economy.withdrawPlayer(player, createValue);
                player.sendMessage(ChatColor.GOLD + l.charge_message() + " " + createValue);
            }
        }
        manager.createPad(location, player.getUniqueId());
        message(player, l.create_approve_1());
        message(player, l.create_approve_2());
    }

    public boolean charge(Player player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean refund(OfflinePlayer player, double amount) {
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public void chargeTP(Player player) {
        if (!player.hasPermission("travelpad.teleport.free")) {
            economy.withdrawPlayer(player, config.teleportAmount);
            player.sendMessage(ChatColor.GOLD + l.charge_message() + " " + config.teleportAmount);
        }
    }

    public void refund(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);

        if (player !=null) {
            if (player.isOnline()) {
                if (!player.getPlayer().hasPermission("travelpad.create.free")) {
                    int padsHas = Manager().getPadsFrom(player.getUniqueId()).size();
                    double refund = config.deleteAmount * (padsHas-1);
                    refund(player, refund);
                    player.getPlayer().sendMessage(ChatColor.GOLD + l.refund_message() + " " + refund);
                }
            }
        } else {

            OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(playerUUID);
            if (!permission.playerHas(getServer().getWorlds().get(0).getName(), oPlayer, "travelpad.create.free")) {
                int padsHas = Manager().getPadsFrom(oPlayer.getUniqueId()).size();
                double refund = config.deleteAmount * (padsHas-1);
                //DEPOSIT OFFLINEPLAYER IS THROWING A NPE VIA ESSENTIALS "Add Balance to player name... wtf even is that"
                economy.depositPlayer(oPlayer, refund);
                if (oPlayer.isOnline()) {
                    ((Player) oPlayer).sendMessage(ChatColor.GOLD + l.refund_message() + " " + refund);
                }
            }

        }
    }

    public void refundNoCreate(Player player) {
        if (!player.hasPermission("travelpad.create.free")) {
            int padsHas = Manager().getPadsFrom(player.getUniqueId()).size();
            economy.depositPlayer(player, config.createAmount*padsHas);
            player.sendMessage(ChatColor.GOLD + l.refund_message() + " " + config.deleteAmount);
        }
    }

    public boolean canAffordTeleport(Player player) {
        if (player.hasPermission("travelpad.teleport.free")) {
            return true;
        } else if (!config.economyEnabled) {
            return true;
        } else if (player.getInventory().contains(config.itemType, 1)) {
            return true;
        }
        double balance = economy.getBalance(player);
        return balance >= config.teleportAmount;
    }

    public int getAllowedPads(Player player) {
        if (player.hasPermission("travelpad.create.infinite")) {
            return -1;
        } else {
            int allowed = 1;
            for (int i = 0; i <= 100; i++) {
                if (player.hasPermission("travelpad.create.max." + i)) {
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
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (economy != null && permission != null);
    }

    public Permission getPermission() {
        return permission;
    }


    public int padsPlayerHas(Player player) {
        List<Pad> pads = manager.getPadsFrom(player.getUniqueId());
        int has = 0;
        if (pads != null) {
            has = pads.size();
        }
        return has;
    }

    public boolean canCreate(Player player) {
        if (player.hasPermission("travelpad.create")) {
            List<UnnamedPad> upads = manager.getUnnamedPadsFrom(player.getUniqueId());
            if (!upads.isEmpty()) {
                errorMessage(player, l.create_deny_waiting());
                return false;
            }
            List<Pad> pads = manager.getPadsFrom(player.getUniqueId());
            int has = 0;
            if (pads != null) {
                has = pads.size();
            }
            if (config.economyEnabled) {
                // Was going to be has+1 but i like first ones free better todo: make sure you update the refund >.>
                double cost = config.createAmount * (has);
                if (!(canAfford(player, cost))) {
                    errorMessage(player, l.create_deny_money() + cost);
                    return false;
                }
            }
            int allow = getAllowedPads(player);

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

    public void errorMessage(CommandSender sender, String message) {
        message(sender, ChatColor.RED + message);
    }

    public void message(CommandSender sender, String message) {
        sender.sendMessage(PLUGIN_PREFIX_COLOR + message);
    }

    public void message(CommandSender sender, BaseComponent component) {
        if (sender instanceof Player) {
            message((Player) sender, component);
        } else {
            message((ConsoleCommandSender) sender, component);
        }
    }

    public void message(ConsoleCommandSender sender, BaseComponent component) {
        sender.sendMessage(component.toLegacyText());
    }

    public void message(Player sender, BaseComponent component) {
        sender.spigot().sendMessage(component);
    }

    public String getPlayerName(UUID playersUUID) {
        OfflinePlayer oPlayer = getServer().getOfflinePlayer(playersUUID);
        if (oPlayer == null) {
            //String name = idToName.get(playersUUID);
            String name = Unity.getUnityHandle().getCache().fetchCurrentName(playersUUID);
            if (name != null) {
                return name;
            }
        } else {
            return oPlayer.getName();
        }
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

    public static long getLastSeen(UUID playersUUID) {
        if (playersUUID.equals(ADMIN_UUID)) {
            //Admins /seen time is fixed to 7 days ago.
            return 1000 * 60 * 60 * 24 * 7;
        } else {
            OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(playersUUID);
            if (oPlayer != null) {
                log(System.currentTimeMillis() + " " + oPlayer.getLastPlayed());
                return System.currentTimeMillis() - oPlayer.getLastPlayed();
            }
        }
        return -1;
    }

    public static BaseComponent clickablePad(Pad pad) {
        return clickablePad(pad, true);
    }

    /**
     * THIS METHOD HAD TO BE REWRITTEN TO HAVE AN EXTRA COMPONENT JOIN THE TEXT/COLOR BECAUSE DOING IT WITH ONE COMPONENT
     * RESULTED IN A DANGLING JSON 'GREEN' TAG WITH NO CONTENT (Appeared to be attached to the hover event somehow...)
     *
     * @param pad
     * @param tooltip
     * @return
     */
    public static BaseComponent clickablePad(Pad pad, boolean tooltip) {
        TextComponent combined = new TextComponent("");
        TextComponent component = new TextComponent(pad.getName());
        component.setColor(ChatColor.GREEN);
        combined.addExtra(component);
        combined.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/t tp " + pad.getName()));
        if (tooltip)
            combined.setHoverEvent(padTooltip(pad));
        return combined;
    }

    public static HoverEvent padTooltip(Pad pad) {
        TextComponent name = new TextComponent("Name: ");
        TextComponent padName = new TextComponent(pad.getName());
        padName.setColor(ChatColor.GREEN);
        name.addExtra(padName);
        ComponentBuilder builder = new ComponentBuilder(name);

        TextComponent owner = new TextComponent("\nOwner: ");
        TextComponent ownerName = new TextComponent(pad.ownerName());
        ownerName.setColor(ChatColor.GREEN);
        owner.addExtra(ownerName);
        builder.append(owner);

        TextComponent location = new TextComponent("\nLocation: ");
        TextComponent locationName = new TextComponent(locationString(pad.getLocation()));
        locationName.setColor(ChatColor.GREEN);
        location.addExtra(locationName);
        builder.append(location);

        if (!pad.getDescription().isEmpty()) {
            TextComponent description = new TextComponent("\nDescription: ");
            TextComponent descriptionText = new TextComponent(pad.getDescription());
            descriptionText.setColor(ChatColor.GRAY);
            description.addExtra(descriptionText);
            builder.append(description);
        }
        if (pad.isPublic()) {
            BaseComponent line = NEWLINE_INDENT.duplicate();
            TextComponent isPublic = new TextComponent("Public!");
            isPublic.setColor(ChatColor.GREEN);
            line.addExtra(isPublic);
            builder.append(line);
        }
        if (pad.prepaidsLeft() > 0) {
            BaseComponent line = NEWLINE_INDENT.duplicate();
            TextComponent isPrepaid = new TextComponent("Prepaid!");
            isPrepaid.setColor(ChatColor.GREEN);
            line.addExtra(isPrepaid);
            builder.append(line);
        }
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, builder.create());
    }

    public static String locationString(Location loc) {
        if (loc != null) {
            return loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " " + loc.getWorld().getName();
        }
        return "";
    }

    public static String formatLocation(Location loc) {
        if (loc != null) {
            if (loc.getWorld() != null)
                return loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " " + loc.getWorld().getName() + " pitch:" + loc.getPitch();
            else
                return loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " null world.";
        } else {
            return "Location is null, File corruption may ensue";
        }
    }

    public static TextComponent fancyLocation(Location loc) {
        TextComponent component = new TextComponent(String.valueOf(loc.getBlockX()));
        component.addExtra(" ");
        component.addExtra(String.valueOf(loc.getBlockY()));
        component.addExtra(" ");
        component.addExtra(String.valueOf(loc.getBlockZ()));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("World: ")
                .append(loc.getWorld().getName())
                .append(NEWLINE)
                .append("Pitch: ")
                .append(String.valueOf(loc.getPitch()))
                .append(NEWLINE)
                .append("Yaw: ")
                .append(String.valueOf(loc.getYaw())).create()));
        return component;
    }

    public void sendPagination(CommandSender sender, BaseComponent[] lines, String title) {
        Pagination pagination = new Pagination(lines);
        pagination.setTitle(title);
        paginator.sendPagination(sender, pagination);
    }

    public static boolean isAdminPad(Pad pad) {
        return pad.ownerUUID().equals(ADMIN_UUID);
    }

    public TextComponent getHeader() {
        return pluginHeader;
    }
}

