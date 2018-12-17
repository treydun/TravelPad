package net.h31ix.travelpad.api;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import net.h31ix.travelpad.Travelpad;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>
 * Defines a new TravelPad on the map, this is only used after a pad has a name.
 */

//Their data will be stored in the config data structure only.
//New pads will be added to that data structure and a write will be performed asyncronously
//deleted pads will be removed from that data structure.
//Its just a list, of strings... easy to add or remove...

//Thoughts on organic sorting
//How many times has it been hit recently
//how many hits per week? reset it each sort? true organic popularity

public class Pad {

    private Location location;
    private String name;
    private UUID ownerUUID;

    private String ownerName = "";

    private boolean publicPad = false;
    private String description = "";
    private long lastUsed = 0L;
    private int prepaidTeleports = 0;
    private int direction = 0;


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
    public Location getLocation() {
        return location;
    }

    /**
     * Get the location of the pad that is safe for a player to teleport to
     *
     * @return location  Safe teleport location
     */
    public Location getTeleportLocation() {
        return new Location(location.getWorld(), location.getX(), location.getY() + 2, location.getZ());
    }

    public void setOwnerUUID(UUID ownerUUID){
        this.ownerUUID=ownerUUID;
    }

    /**
     * Get the owner of the pad
     *
     * @return owner Player who owns the pad's name
     */
    //public String getOwner() { return owner; }
    public UUID ownerUUID() {
        return ownerUUID;
    }

    public String ownerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    /**
     * Get the name of the pad
     *
     * @return name  Name of the pad
     */
    public String getName() {
        return name;
    }

    /**
     * (Re)Name the pad
     *
     * @param name Name of the pad
     */
    public void setName(String name) {
        this.name = name;
    }

    public void setPublic(boolean makePublic){
        publicPad=makePublic;
    }

    public boolean isPublic(){
        return publicPad;
    }

    public void setDescription(String description){
        this.description=description;
    }

    public String getDescription() {
        return description;
    }

    public void setDirection(int direction){
        //TODO: On meta import this method would need to be invoked to properly update the location
        //0 = Default Rotation
        switch (direction){
            case 0:
                this.location=new Location(this.location.getWorld(),
                        this.location.getBlockX(),
                        this.location.getBlockY(),
                        this.location.getBlockZ());
                break;
            case 1:
                this.location=new Location(this.location.getWorld(),
                        this.location.getBlockX(),
                        this.location.getBlockY(),
                        this.location.getBlockZ(),
                        0F,
                        90F);
                break;
            case 2:
                this.location=new Location(this.location.getWorld(),
                        this.location.getBlockX(),
                        this.location.getBlockY(),
                        this.location.getBlockZ(),
                        0F,
                        180F);
                break;
            case 3:
                this.location=new Location(this.location.getWorld(),
                        this.location.getBlockX(),
                        this.location.getBlockY(),
                        this.location.getBlockZ(),
                        0F,
                        270F);
                break;
        }
        this.direction=direction;
    }

    public int getDirection(){
        return direction;
    }

    public boolean hasMeta() {
        return publicPad || !description.isEmpty() || lastUsed!=0L || prepaidTeleports!=0 || direction!=0;
    }

    public void importMeta(Map<String, Object> meta){
        if(!meta.isEmpty()){
            for(String key:meta.keySet()){
                switch(key){
                    case "description":
                        description=(String) meta.get(description);
                        break;
                    case "lastused":
                        lastUsed=Long.parseLong((String) meta.get("lastused"));
                        break;
                    case "prepaidteleports":
                        prepaidTeleports=Integer.parseInt((String) meta.get("prepaidteleports"));
                    case "direction":
                        direction = Integer.parseInt((String) meta.get("direction"));
                    case "public":
                        publicPad=true;
                }
            }
        }
    }

    public Map<String, Object> getMeta(){
        Map<String, Object> meta = new HashMap<>();
        if(publicPad){
            meta.put("public",true);
        }
        if(!description.isEmpty()){
            meta.put("description",description);
        }
        if(lastUsed!=0L){
            meta.put("lastused", lastUsed);
        }
        if(prepaidTeleports!=0){
            meta.put("prepaidteleports",prepaidTeleports);
        }
        if(direction!=0){
            meta.put("direction",direction);
        }
        return meta;
    }

    @Override
    public String toString() {
        return name + " " + Travelpad.formatLocation(location) + " " + ownerUUID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pad pad = (Pad) o;
        return Objects.equals(location, pad.location) &&
                Objects.equals(name, pad.name) &&
                Objects.equals(ownerUUID, pad.ownerUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, name, ownerUUID);
    }

    public static String serialize(Pad pad) {
        StringBuilder padString = new StringBuilder(pad.getName());
        padString.append(Travelpad.DELIMINATOR);
        padString.append(pad.getLocation().getWorld().getName());
        padString.append(Travelpad.DELIMINATOR);
        padString.append(pad.getLocation().getBlockX());
        padString.append(Travelpad.DELIMINATOR);
        padString.append(pad.getLocation().getBlockY());
        padString.append(Travelpad.DELIMINATOR);
        padString.append(pad.getLocation().getBlockZ());
        padString.append(Travelpad.DELIMINATOR);
        padString.append(pad.ownerUUID.toString());
        return padString.toString();
    }

    public static Pad deserialize(String serialized) {
        String[] padData = serialized.split("/");
        Pad pad = null;

        if (padData.length == 6) {
            //Tpads 3.0 Name/World/X/Y/Z/OwnerUUID
            World world = Bukkit.getWorld(padData[1]);
            if (world != null) {
                int x = Integer.parseInt(padData[2]);
                int y = Integer.parseInt(padData[3]);
                int z = Integer.parseInt(padData[4]);
                Location location = new Location(world, x, y, z);
                UUID ownerID = UUID.fromString(padData[5]);
                pad = new Pad(location, ownerID, padData[0]);
            } else {
                Travelpad.log(Travelpad.PLUGIN_PREFIX_COLOR+ ChatColor.RED+"Failed to load a location with "+padData[1]+" world");
            }
        } else if (padData.length == 7) {
            //Tpads 2.0 Name/X/Y/Z/World/OwnerName/OwnerUUID
            World world = Bukkit.getWorld(padData[4]);
            if (world != null) {
                int x = Integer.parseInt(padData[1]);
                int y = Integer.parseInt(padData[2]);
                int z = Integer.parseInt(padData[3]);
                Location location = new Location(world, x, y, z);
                UUID ownerID = UUID.fromString(padData[6]);
                pad = new Pad(location, ownerID, padData[0]);
                pad.setOwnerName(padData[5]);
            } else {
                Travelpad.log(Travelpad.PLUGIN_PREFIX_COLOR+ ChatColor.RED+"Failed to load a location with "+padData[4]+" world");
            }
        } else {
            System.out.print("Tpads Error: Failed to deserialize " + serialized + " := Incorrect Parameter Length");
        }
        return pad;
    }
}
