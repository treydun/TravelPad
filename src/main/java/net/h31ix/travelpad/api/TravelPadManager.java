package net.h31ix.travelpad.api;

import net.h31ix.travelpad.Travelpad;
import net.h31ix.travelpad.event.TravelPadExpireEvent;

import java.util.*;

import net.h31ix.travelpad.event.TravelPadCreateEvent;
import net.h31ix.travelpad.event.TravelPadDeleteEvent;
import net.h31ix.travelpad.event.TravelPadNameEvent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TravelPadManager {

    final private Travelpad plugin;

    private HashMap<String, Pad> padsByLocation = new HashMap<>();
    private HashMap<String, Pad> padsByName = new HashMap<>();
    private HashMap<UUID, List<Pad>> padsByUUID = new HashMap<>();

    private List<UnnamedPad> unvList = new ArrayList<>();
    private List<Pad> publicPads = new ArrayList<>();


    public TravelPadManager(Travelpad plugin) {
        this.plugin = plugin;
        update();
    }

    /**
     * Update the list of pads from the Config() datastore
     * Does NOT trigger a disk read any longer
     */
    public void update() {
        //Import padlist from config
        List<String> serializedPads = plugin.Config().getPads();
        if (serializedPads != null && !serializedPads.isEmpty()) {
            //Reinitialize HashMaps to be presized to the amount of pads needed
            padsByLocation = new HashMap<>(serializedPads.size());
            padsByName = new HashMap<>(serializedPads.size());
            padsByUUID = new HashMap<>(serializedPads.size());
            //Propogate map data
            for (String serializedPad : serializedPads) {
                Pad pad = Pad.deserialize(serializedPad);
                //Pads should just be cached here, no need to trigger a save...
                if (pad != null) {
                    setOwnerName(pad);
                    if (plugin.Config().hasMeta(pad.getName())) {
                        pad.importMeta(plugin.Config().getPadMeta(pad.getName()));
                    }
                    cachePad(pad);
                } else {
                    plugin.getLogger().warning("Unable to load pad " + serializedPad + " is the world loaded?");
                }
            }
        }
        //Attempt to load any unnamed pads from a server restart or crash
        //TODO: This really needs to trigger some sort of pad delete task on these...
        //What good is remembering its unnamed if the task to cancel its creation is missing?
        List<String> serializedUnnamedPads = plugin.Config().getUnvPads();
        if (serializedUnnamedPads != null && !serializedUnnamedPads.isEmpty()) {
            for (String serializedUnnamedPad : serializedUnnamedPads) {
                UnnamedPad unnamedPad = UnnamedPad.deserialize(serializedUnnamedPad);
                //Unnamed pads should just be cached here, no need to trigger a save...
                if (unnamedPad != null) {
                    cachePad(unnamedPad);
                } else {
                    plugin.getLogger().warning("Unable to load UnnamedPad " + serializedUnnamedPad + " is the world loaded?");
                }
            }
        }
    }

    public void cachePad(Pad pad) {
        Map<String, Object> padMeta = plugin.Config().getPadMeta(pad.getName());
        if (padMeta != null) {
            Travelpad.log("Found meta for " + pad.getName() + ", importing");
            pad.importMeta(padMeta);
        }

        padsByLocation.put(locToString(pad.getLocation()), pad);
        padsByName.put(pad.getName().toLowerCase(), pad);
        List<Pad> pads = padsByUUID.getOrDefault(pad.ownerUUID(), new ArrayList<>());
        pads.add(pad);
        padsByUUID.put(pad.ownerUUID(), pads);
        if (pad.isPublic()) {
            publicPads.add(pad);
        }
    }

    public void addPad(Pad pad) {
        plugin.Config().addPad(Pad.serialize(pad), true);
        cachePad(pad);
    }

    public void cachePad(UnnamedPad pad) {
        unvList.add(pad);
    }

    public void addPad(UnnamedPad pad) {
        plugin.Config().addUnnamedPad(pad.serialize(), true);
        cachePad(pad);
    }

    /**
     * Drops Pad from cache maps, does not remove it from Config() or disk
     *
     * @param pad pad to flush
     */
    public void flushPad(Pad pad) {
        padsByLocation.remove(locToString(pad.getLocation()));
        padsByName.remove(pad.getName().toLowerCase());
        List<Pad> pads = padsByUUID.get(pad.ownerUUID());
        pads.remove(pad);
        if (pads.isEmpty()) {
            padsByUUID.remove(pad.ownerUUID());
        } else {
            padsByUUID.put(pad.ownerUUID(), pads);
        }
    }

    /**
     * Removes pad totally from datastore and memory, runs async save after
     * which then clears it from disk data store
     *
     * @param pad pad to remove
     */
    public void removePad(Pad pad) {
        plugin.Config().removePad(Pad.serialize(pad), true);
        flushPad(pad);
    }

    /**
     * Removes Unnamed pad from unnamed pad cache, does not remove it from Config() or disk
     *
     * @param pad unnamed pad to remove
     */
    public void flushPad(UnnamedPad pad) {
        Travelpad.log("Attempting to flush "+pad.toString());
        unvList.remove(pad);
    }

    /**
     * Removes an Unnamed pad from the cache as well as Config() and disk data store
     *
     * @param pad
     */
    public void removePad(UnnamedPad pad) {
        plugin.Config().removeUnnamedPad(pad.serialize(), true);
        flushPad(pad);
    }

    public String locToString(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    /**
     * Create a new, unnamed pad
     *
     * @param location  Location of the center of the pad
     * @param ownerUUID UUID who should own this pad
     */
    public void createPad(final Location location, final UUID ownerUUID) {
        final UnnamedPad pad = new UnnamedPad(location, ownerUUID);

        TravelPadCreateEvent e = new TravelPadCreateEvent(pad);
        plugin.getServer().getPluginManager().callEvent(e);

        if (!e.isCancelled()) {
            //Store pad in unv cache and unv datastore
            addPad(pad); //Triggers async save

            //Schedule expiration
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    if (isStillUnnamed(pad)) {
                        TravelPadExpireEvent e = new TravelPadExpireEvent(pad);
                        plugin.getServer().getPluginManager().callEvent(e);
                        if (!e.isCancelled()) {
                            //Remove expired pad from unnamed list and datastore
                            removePad(pad);//Triggers async save
                            if (!ownerUUID.equals(Travelpad.ADMIN_UUID)) {
                                final Player owner = Bukkit.getPlayer(pad.OwnerUUID());
                                if (owner != null && owner.isOnline()) {
                                    //Send owner message that they didnt name fast enough
                                    plugin.errorMessage(owner, plugin.Lang().pad_expire());
                                }
                            }
                            //Cleanup pad structure
                            deleteBlocks(location);
                        }
                    }
                }
            }, 600L);

            //Set surrounding blocks and do emit water routine
            final Block block = location.getBlock();
            block.getRelative(BlockFace.EAST).setType(Material.STONE_SLAB);
            block.getRelative(BlockFace.WEST).setType(Material.STONE_SLAB);
            block.getRelative(BlockFace.NORTH).setType(Material.STONE_SLAB);
            block.getRelative(BlockFace.SOUTH).setType(Material.STONE_SLAB);
            if (plugin.Config().emitsWater()) {
                block.getRelative(BlockFace.UP).setType(Material.WATER);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        block.getRelative(BlockFace.UP).setType(Material.AIR);
                    }
                }, 5L);
            }


        }
    }

    public void setOwnerName(Pad pad) {
        if (!pad.ownerUUID().equals(Travelpad.ADMIN_UUID)) {
            pad.setOwnerName(plugin.getPlayerName(pad.ownerUUID()));
        } else {
            pad.setOwnerName("Admin");
        }
    }

    public void setPublic(Pad pad) {
        pad.setPublic(true);
        plugin.Meta().saveMeta(pad.getName());
    }

    public void setPrivate(Pad pad) {
        pad.setPublic(false);
    }

    public void setMeta(Pad pad, String description) {
        pad.setDescription(description);
        plugin.Meta().saveMeta(pad.getName());
    }

    public void setMeta(Pad pad, int direction) {

    }

    public boolean isStillUnnamed(UnnamedPad pad) {
        Travelpad.log("unvList.contains:"+pad.toString()+" "+unvList.contains(pad));
        return unvList.contains(pad);
    }

    /**
     * Remove an Unnamed Pad
     *
     * @param pad UnnamedPad to be deleted
     */
    public void deleteUnnamedPadLegacy(UnnamedPad pad) {
        //Renamed to Legacy to prevent APIs from using it elsewhere...
        TravelPadExpireEvent e = new TravelPadExpireEvent(pad);
        plugin.getServer().getPluginManager().callEvent(e);
        if (!e.isCancelled()) {
            update();
            Location location = pad.getLocation();
            //config.removePad(pad);
            //Bukkit.getPlayer(pad.OwnerUUID()).sendMessage(ChatColor.RED + l.pad_expire());
            deleteBlocks(location);
            update();
        }
    }

    /**
     * Clean up all the blocks around a pad after it has been deleted
     *
     * @param location Location of pad to be destroyed
     */
    public void deleteBlocks(Location location) {
        World world = location.getWorld();
        Block block = world.getBlockAt(location);
        block.setType(Material.AIR);
        block.getRelative(BlockFace.EAST).setType(Material.AIR);
        block.getRelative(BlockFace.SOUTH).setType(Material.AIR);
        block.getRelative(BlockFace.NORTH).setType(Material.AIR);
        block.getRelative(BlockFace.WEST).setType(Material.AIR);
        ItemStack i = new ItemStack(plugin.Config().center, 1);
        ItemStack e = new ItemStack(plugin.Config().outline, 4);
        world.dropItemNaturally(block.getLocation(), i);
        world.dropItemNaturally(block.getLocation(), e);
    }

    /**
     * Change an unnamed pad into a named, operational pad
     *
     * @param pad  Unnamed pad to be changed
     * @param name The name of the pad
     */
    public void switchPad(UnnamedPad pad, String name) {
        TravelPadNameEvent e = new TravelPadNameEvent(pad, name);
        plugin.getServer().getPluginManager().callEvent(e);
        if (!e.isCancelled()) {
            flushPad(pad);
            plugin.Config().removeUnnamedPad(pad.serialize());
            Pad newPad = new Pad(pad.getLocation(), pad.OwnerUUID(), e.getName());
            addPad(newPad);//Triggers async save...
        }
    }

    /**
     * Remove a Pad
     *
     * @param pad Pad to be deleted
     */
    public void deletePad(Pad pad) {
        TravelPadDeleteEvent d = new TravelPadDeleteEvent(pad);
        plugin.getServer().getPluginManager().callEvent(d);
        if (!d.isCancelled()) {
            removePad(pad); //Triggers Async save
            Player player = Bukkit.getPlayer(pad.ownerUUID());
            if (player != null) {
                plugin.errorMessage(player, plugin.Lang().delete_approve() + " " + ChatColor.WHITE + pad.getName());
            }
            deleteBlocks(pad.getLocation());
        }
    }

    /**
     * Check if a name is already in use
     *
     * @param name Name to be checked
     */
    public boolean padExists(String name) {
        Travelpad.log("padsByName.contains "+name.toLowerCase()+padsByName.containsKey(name.toLowerCase()));
        return padsByName.containsKey(name.toLowerCase());
    }

    /**
     * Get a pad by it's name
     *
     * @param name Pad's name to be found
     * @return Pad if found, null if no pad by that name
     */
    public Pad getPad(String name) {
        return padsByName.get(name.toLowerCase());
    }

    /**
     * Get a pad by it's location
     *
     * @param location Pad's location to be found
     * @return Pad if found, null if no pad at that location
     */
    public Pad getPadAt(Location location) {
        return padsByLocation.get(locToString(location));
    }

    public Pad getPadNear(Location location) {
        //Quick cutaway in case by some miracle we are at the 'right' location. Odds are slim
        Pad quickPad = getPadAt(location);
        if (quickPad != null) {
            return quickPad;
        }
        List<Pad> list = getPads();
        for (Pad pad : list) {
            int padX = (int) pad.getLocation().getX();
            int padY = (int) pad.getLocation().getY();
            int padZ = (int) pad.getLocation().getZ();
            String padWorld = pad.getLocation().getWorld().getName();
            int locX = (int) location.getX();
            int locY = (int) location.getY();
            int locZ = (int) location.getZ();
            String locWorld = location.getWorld().getName();
            if (padX <= locX + 2 && padX >= locX - 2 && padY <= locY + 2 && padY >= locY - 2 && padZ <= locZ + 2 && padZ >= locZ - 2 && padWorld.equals(locWorld)) {
                return pad;
            }
        }
        return null;
    }

    /**
     * Get an Unnamed Pad by it's location
     *
     * @param location Unnamed Pad's location to be found
     * @return Unnamed Pad if found, null if no pad by that name
     */
    public UnnamedPad getUnnamedPadAt(Location location) {
        //Unnamed list should always be pretty darn short
        //Only used for block place and break afaict
        for (UnnamedPad pad : unvList) {
            int x = (int) pad.getLocation().getX();
            int y = (int) pad.getLocation().getY();
            int z = (int) pad.getLocation().getZ();
            int xx = (int) location.getX();
            int yy = (int) location.getY();
            int zz = (int) location.getZ();
            if (x == xx && y == yy && z == zz) {
                return pad;
            }
        }
        return null;
    }

    /* doesnt appear to ever be needed, naming uses UUID of command sender */
    public UnnamedPad getUnnamedPadNear(Location location) {
        for (UnnamedPad pad : unvList) {
            int x = (int) pad.getLocation().getX();
            int y = (int) pad.getLocation().getY();
            int z = (int) pad.getLocation().getZ();
            int xx = (int) location.getX();
            int yy = (int) location.getY();
            int zz = (int) location.getZ();
            if (x <= xx + 2 && x >= xx - 2 && y <= yy + 2 && y >= yy - 2 && z <= zz + 2 && z >= zz - 2) {
                return pad;
            }
        }
        return null;
    }

    /**
     * Get all the pads that a player owns
     *
     * @param owner UUID to search for
     * @return Set of pads that the player owns, null if they have none.
     */
    public List<Pad> getPadsFrom(UUID owner) {
        return padsByUUID.get(owner);
    }

    /**
     * Get all the unnamed pads that a player owns
     *
     * @param owner UUID to search for
     * @return Set of unnamed pads that the player owns, null if they have none.
     */
    public List<UnnamedPad> getUnnamedPadsFrom(UUID owner) {
        List<UnnamedPad> list = new ArrayList<>();
        for (UnnamedPad pad : unvList) {
            if (pad.OwnerUUID() == owner) {
                list.add(pad);
            }
        }
        return list;
    }

    /**
     * Get all registered pads
     *
     * @return Set of pads that exists
     */
    public List<Pad> getPads() {
        Travelpad.log("GETALLPADS BEING CALLED...");
        //Could use padsByName.values() as well but its a collection
        //This really needs to be switched to a primitive array...
        List<Pad> allPads = new ArrayList<>();
        for (String key : padsByName.keySet()) {
            allPads.add(padsByName.get(key));
        }
        return allPads;
    }

    /**
     * Get all unregistered pads
     *
     * @return Set of pads that are awaiting naming
     */
    public List<UnnamedPad> getUnnamedPads() {
        return unvList;
    }

    public boolean isSafe(Location loc, Player player) {
        World world = loc.getWorld();
        Block block = world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 2, loc.getBlockZ());
        Block block1 = world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
        Block block2 = world.getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (!(block1.getType() == Material.AIR || block2.getType() == Material.AIR)) {
            plugin.errorMessage(player, "Suffocated");
            return false;//not safe, suffocated
        } else if (block.getRelative(BlockFace.DOWN).getType() != Material.OBSIDIAN) {
            plugin.errorMessage(player, "Not a valid tpad?" + block.getRelative(BlockFace.DOWN).getType().toString());
            player.sendMessage("X:" + block.getX() + " Y:" + block.getY() + " Z:" + block.getZ());
            return false;
        }
        return true; //assume passed
    }
}
