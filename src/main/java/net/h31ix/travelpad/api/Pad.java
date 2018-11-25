package net.h31ix.travelpad.api;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import net.h31ix.travelpad.Travelpad;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 * Defines a new TravelPad on the map, this is only used after a pad has a name.
 */

//TODO: STOP FORGETTING! Location STAYSSS here. No pad that doesnt load a world should ever be added to this list/created/instantized
    //Their data will be stored in the config data structure only.
    //New pads will be added to that data structure and a write will be performed asyncronously
    //deleted pads will be removed from that data structure.
    //Its just a list, of strings... easy to add or remove... time to simplify a lot of this... no need to create a special location object container or anything
    //world existance is not an issue if i just do it this way...

    //waitwait... stats.... engine... would be an excessive amount of writes/updates if stats ARE stored in the same data structure?
    // unless you went yml again and just use the data structure in yml...... :S


//How many times has it been hit recently
//how many hits per week? reset it each sort? true organic popularity
//world name? or uuid? i added world name to new pad because when its missing the pads would refuse to load

public class Pad {

    private Location location;
    private String name;
    private UUID ownerUUID;

    private String ownerName="";
    private boolean publicPad = false;
    private String description = "";
    private long lastUsed = 0L;
    private int prepaidTeleports = 0;


    public Pad(Location location, UUID ownerUUID, String name) {
        this.location = location;
        this.ownerUUID = ownerUUID;
        this.name = name;
    }

    /**
     * Get the location of the pad
     *
     * @return location  Location of the obsidian center of the pad
     */
    public Location getLocation() { return location; }

    /**
     * Get the location of the pad that is safe for a player to teleport to
     *
     * @return location  Safe teleport location
     */
    public Location getTeleportLocation() {
        return new Location(location.getWorld(), location.getX(), location.getY() + 2, location.getZ());
    }

    /**
     * Get the owner of the pad
     *
     * @return owner Player who owns the pad's name
     */
    //public String getOwner() { return owner; }

    public UUID ownerUUID() { return ownerUUID; }

    public String ownerName() { return ownerName; }

    public void setOwnerName(String ownerName) { this.ownerName=ownerName; }

    /**
     * Get the name of the pad
     *
     * @return name  Name of the pad
     */
    public String getName() { return name; }

    /**
     * (Re)Name the pad
     *
     * @param name Name of the pad
     */
    public void setName(String name) { this.name = name; }

    public String toString() {
        return name + " " + Travelpad.formatLocation(location) + " " + ownerUUID;
    }

    public static String serialize(Pad pad) {
        StringBuilder padString = new StringBuilder(pad.getName());
        padString.append(Travelpad.DELIMINATOR);
        padString.append(pad.getLocation().getWorld().getName());
        padString.append(Travelpad.DELIMINATOR);
        padString.append(pad.getLocation().getX());
        padString.append(Travelpad.DELIMINATOR);
        padString.append(pad.getLocation().getY());
        padString.append(Travelpad.DELIMINATOR);
        padString.append(pad.getLocation().getZ());
        padString.append(Travelpad.DELIMINATOR);
        padString.append(pad.ownerUUID.toString());
        return padString.toString();
    }

    public static Pad deserialize(String serialized) {
        String[] padData = serialized.split("/");
        Pad pad=null;

        if(padData.length==6) {
            //Tpads 3.0 Name/World/X/Y/Z/OwnerUUID
           World world = Bukkit.getWorld(padData[1]);
           if(world!=null) {
               int x = Integer.parseInt(padData[2]);
               int y = Integer.parseInt(padData[3]);
               int z = Integer.parseInt(padData[4]);
               Location location = new Location(world, x, y, z);
               UUID ownerID = UUID.fromString(padData[5]);
               pad = new Pad(location, ownerID, padData[0]);
           }
        } else if(padData.length==7) {
            //Tpads 2.0 Name/X/Y/Z/World/OwnerName/OwnerUUID
            World world = Bukkit.getWorld(padData[4]);
            if(world!=null) {
                int x = Integer.parseInt(padData[1]);
                int y = Integer.parseInt(padData[2]);
                int z = Integer.parseInt(padData[3]);
                Location location = new Location(world, x, y, z);
                UUID ownerID = UUID.fromString(padData[6]);
                pad = new Pad(location, ownerID, padData[0]);
                pad.setOwnerName(padData[5]);
            }
        } else {
            System.out.print("Tpads Error: Failed to deserialize "+serialized+" := Incorrect Parameter Length");
        }
        return pad;
    }
}
