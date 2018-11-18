package net.h31ix.travelpad.api;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import net.h31ix.travelpad.Travelpad;
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

public class Pad implements ConfigurationSerializable {

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

    private static final String DELIMINATOR = "/";

    public static String serialize(Pad pad) {
        StringBuilder padString = new StringBuilder(pad.getName());
        padString.append(DELIMINATOR);
        padString.append(pad.getLocation().getX());
        padString.append(DELIMINATOR);
        padString.append(pad.getLocation().getY());
        padString.append(DELIMINATOR);
        padString.append(pad.getLocation().getZ());
        padString.append(DELIMINATOR);
        padString.append(pad.getLocation().getWorld().getUID());
        padString.append(DELIMINATOR);
        padString.append(pad.ownerName);
        padString.append(DELIMINATOR);
        padString.append(pad.ownerUUID);
        return padString.toString();
    }

    public static Pad deserialize(String serialized) {
        String[] padData = serialized.split("/");
        Pad pad=null;
        if(padData.length==7) {
            //Legacy Pad
            Location location = new Location(Bukkit.getServer().getWorld(padData[4]), Integer.parseInt(padData[1]), Integer.parseInt(padData[2]), Integer.parseInt(padData[3]));
            if(location!=null){
                pad = new Pad(location, UUID.fromString(padData[6]), padData[0]);
                pad.setOwnerName(padData[5]);
            }
        } else if (padData.length==10) {
            //Modern Pad - Param Length may change
            //
        } else {
            System.out.print("Tpads Error: Failed to deserialize "+serialized+" := Incorrect Parameter Length");
        }
        return pad;
    }

    public static Pad deserialize(Map<String, Object> map){
        Pad pad = new Pad((Location)map.get("location"), UUID.fromString((String)map.get("ownerUUID")), (String)map.get("name"));
        if(map.containsKey("ownerName")){
            pad.setOwnerName((String)map.get("ownerName"));
        }
        return pad;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("location", location);
        map.put("name", name);
        map.put("ownerUUID", ownerUUID);
        if(!ownerName.isEmpty())
            map.put("ownerName", ownerName);
        if(publicPad)
            map.put("public", true);
        if(!description.isEmpty())
            map.put("description", description);
        if(lastUsed>0L)
            map.put("lastUsed", lastUsed);
        if(prepaidTeleports>0)
            map.put("prepaidTeleports", prepaidTeleports);
        return map;
    }
}
