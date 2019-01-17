package net.h31ix.travelpad;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.buildatnight.pagination.BukkitPaginator;
import com.buildatnight.pagination.Pagination;
import com.buildatnight.travelpad.ComponentTests;
import net.h31ix.travelpad.api.Configuration;
import net.h31ix.travelpad.api.Pad;
import net.h31ix.travelpad.api.TravelPadManager;
import net.h31ix.travelpad.api.UnnamedPad;
import net.h31ix.travelpad.tasks.SyncMeta;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.chat.ComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
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
    private HashMap<UUID, String> idToName;

    private Economy economy;

    public static final String PLUGIN_PREFIX_COLOR = ChatColor.DARK_GRAY + "[" + ChatColor.BLUE + "TravelPads" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;
    public static final TextComponent PLUGIN_PREFIX_COMPONENT = new TextComponent(TextComponent.fromLegacyText(PLUGIN_PREFIX_COLOR));
    public static final String PLUGIN_CHAT_HEADER = ChatColor.DARK_GRAY + "        - - - - - - - " + PLUGIN_PREFIX_COLOR + ChatColor.DARK_GRAY + "- - - - - - -";
    public static final TextComponent PLUGIN_HEADER_COMPONENT = new TextComponent(TextComponent.fromLegacyText(PLUGIN_CHAT_HEADER));
    public static final TextComponent NEWLINE = new TextComponent("\n");
    public static final UUID ADMIN_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final String DELIMINATOR = "/";
    public static final Pattern isInteger = Pattern.compile("-?\\d+");

    private BukkitPaginator paginator;

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTask(syncMetaTaskID);
        syncMeta.forceSave();
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

        paginator = (BukkitPaginator) getServer().getPluginManager().getPlugin("BukkitPaginator");
        if (paginator == null) {
            getLogger().severe("Failed to find BukkitPaginator, DISABLING");
            getServer().getPluginManager().disablePlugin(this);
        }

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
        TravelPadCommandExecutor commandExecutor = new TravelPadCommandExecutor(this);
        getCommand("t").setExecutor(commandExecutor);
        getCommand("t").setTabCompleter(commandExecutor);
        syncMeta = new SyncMeta(this);
        syncMetaTaskID = getServer().getScheduler().scheduleSyncRepeatingTask(this, syncMeta, 299L, 500L);
        ComponentTests.runTests();
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
        if (!Manager().isSafe(loc, player)) {
            errorMessage(player, l.travel_unsafe());
            return;
        }

        if (player.hasMetadata("prepaid")) {
            player.removeMetadata("prepaid", this);
            //TODO: STUB - finish this
            player.sendMessage("TP'd for free!");
        } else if (!player.hasPermission("travelpad.teleport.free")) {
            //If RequireItem is false but takeitem is true its considered the optional charge item
            if (config.requireItem || config.takeItem) {
                ItemStack itemToTake = new ItemStack(config.itemType, 1);
                //If item is required, legacy setting compatibility.
                if (config.requireItem && player.getInventory().contains(Config().itemType, 1)) {
                    if (config.takeItem) {
                        player.getInventory().removeItem(itemToTake);
                        message(player, l.travel_approve_item().replace("%item%", ChatColor.GREEN + itemToTake.getType().name().replaceAll("_", " ") + ChatColor.GRAY));
                        //player.sendMessage(ChatColor.GOLD + itemToTake.getType().name().toLowerCase().replaceAll("_", " ") + " " + l.travel_approve_item());
                    } else {
                        message(player, "You used %item% to teleport");
                    }
                } else if (config.requireItem) {
                    errorMessage(player, l.travel_deny_item() + " " + itemToTake.getType().name().toLowerCase().replaceAll("_", " "));
                    return;
                }

                //If item is not required but can be taken due to setting (Our new hybrid mode)
                if (!config.requireItem && (config.chargeTeleport || config.takeItem)) {
                    if (player.getInventory().contains(Config().itemType, 1)) {
                        player.getInventory().removeItem(itemToTake);
                        //TODO: fix this ugly message a bit more
                        message(player, l.travel_approve_item().replace("%item%", ChatColor.GREEN + "(1) " + itemToTake.getType().name().toLowerCase().replaceAll("_", " ") + ChatColor.GRAY));
                    } else {
                        //chargeTP(player);
                        if (config.chargeTeleport) {
                            if (!charge(player, config.teleportAmount)) {
                                errorMessage(player, l.travel_deny_money());
                                return;
                            } else {
                                player.sendMessage(ChatColor.GOLD + l.charge_message() + " " + config.teleportAmount);
                            }
                        }
                    }
                }
            }
            //TODO: Update
            for (int i = 0; i != 32; i++) {
                player.getWorld().playEffect(player.getLocation().add(getRandom(), getRandom(), getRandom()), Effect.SMOKE, 3);
            }
            loc.getChunk().load();
            //Delay teleportation to allow the server time to load the chunk.
            getServer().getScheduler().runTaskLater(this, new Runnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
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
        if (!player.hasPermission("travelpad.create.free")) {
            economy.withdrawPlayer(player, config.createAmount);
            player.sendMessage(ChatColor.GOLD + l.charge_message() + " " + config.createAmount);
        }
    }

    public void chargeTP(Player player) {
        if (!player.hasPermission("travelpad.teleport.free")) {
            economy.withdrawPlayer(player, config.teleportAmount);
            player.sendMessage(ChatColor.GOLD + l.charge_message() + " " + config.teleportAmount);
        }
    }

    public void refund(Player player) {
        if (!player.hasPermission("travelpad.create.free")) {
            int padsHas = Manager().getPadsFrom(player.getUniqueId()).size();
            double refund = config.deleteAmount * (padsHas + 1);
            economy.depositPlayer(player, refund);
            player.sendMessage(ChatColor.GOLD + l.refund_message() + " " + config.deleteAmount);
        }
    }

    public void refundNoCreate(Player player) {
        if (!player.hasPermission("travelpad.create.free")) {
            economy.depositPlayer(player, config.createAmount);
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
        return (economy != null);
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
                double cost = config.createAmount * (has + 1);
                if (!(canAfford(player, cost))) {
                    errorMessage(player, l.create_deny_money());
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
        sender.sendMessage(PLUGIN_PREFIX_COLOR + ChatColor.RED + message);
    }

    public void message(CommandSender sender, String message) {
        sender.sendMessage(PLUGIN_PREFIX_COLOR + message);
    }

    public void message(CommandSender sender, BaseComponent component) {
        if (ComponentSerializer.toString(component).length() > 32000) {
            error("MESSAGE OVERFLOW! This method MUST be updated now!");
            return;
        }
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

    public void sendLine(CommandSender sender, String message) {
        sender.sendMessage(message);
    }

    public String getPlayerName(UUID playersUUID) {
        OfflinePlayer oPlayer = getServer().getOfflinePlayer(playersUUID);
        if (oPlayer == null) {
            String name = idToName.get(playersUUID);
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
                error("Failed to match " + playerName + " to a UUID via Bukkits internal method. Failover to unity?");
                //pl = Bukkit.getOfflinePlayer(Unity.getUnityHandle().getCache().fetchUUID(playerName));
            }
            if (pl != null) {
                return pl.getUniqueId();
            }
        }
        return null;
    }

    public static long getLastSeen(UUID playersUUID) {
        if (playersUUID.equals(ADMIN_UUID)) {
            //Admins /seen time is fixed to 24 hours ago.
            return 1000 * 60 * 60 * 24;
        } else {
            OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(playersUUID);
            if (oPlayer != null) {
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
        TextComponent padName = new TextComponent(pad.getName()+'\n');
        padName.setColor(ChatColor.GREEN);
        name.addExtra(padName);
        ComponentBuilder builder = new ComponentBuilder(name);
        TextComponent owner = new TextComponent("Owner: ");
        TextComponent ownerName = new TextComponent(pad.ownerName()+'\n');
        ownerName.setColor(ChatColor.GREEN);
        owner.addExtra(ownerName);
        builder.append(owner);
        TextComponent location = new TextComponent("Location: ");
        TextComponent locationName = new TextComponent(locationString(pad.getLocation())+'\n');
        locationName.setColor(ChatColor.GREEN);
        location.addExtra(locationName);
        builder.append(location);
        if(pad.isPublic()){
            TextComponent isPublic = new TextComponent("Public"+'\n');
            builder.append(isPublic);
        }
        if(pad.prepaidsLeft() > 0) {
            TextComponent isPrepaid = new TextComponent(" ~ Prepaid ~");
            builder.append(isPrepaid);
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
}

