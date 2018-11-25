package net.h31ix.travelpad.api;

import net.h31ix.travelpad.Travelpad;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * <p>
 * Defines a new Unnamed TravelPad on the map, this is only used before a pad is named by the player.
 */

public class UnnamedPad {

    private Location location;
    private UUID ownerUUID;

    public UnnamedPad(Location location, UUID ownerUUID) {
        this.location = location;
        this.ownerUUID = ownerUUID;
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
     * Get the owner of the pad
     *
     * @return owner Player who owns the pad's name
     */
    public UUID getOwner() {
        return ownerUUID;
    }

    public String serialize() {
        StringBuilder builder = new StringBuilder(location.getWorld().getName());
        builder.append(Travelpad.DELIMINATOR);
        builder.append(location.getBlockX());
        builder.append(Travelpad.DELIMINATOR);
        builder.append(location.getBlockY());
        builder.append(Travelpad.DELIMINATOR);
        builder.append(location.getBlockZ());
        builder.append(Travelpad.DELIMINATOR);
        builder.append(ownerUUID.toString());
        return builder.toString();
    }

    public static UnnamedPad deserialize(String serialized){
        String[] padData = serialized.split("/");
        UnnamedPad uPad=null;
        if(padData.length==5) {
            World world = Bukkit.getWorld(padData[0]);
            if (world != null) {
                int x = Integer.parseInt(padData[1]);
                int y = Integer.parseInt(padData[2]);
                int z = Integer.parseInt(padData[3]);
                Location location = new Location(world, x, y, z);
                UUID ownerID = UUID.fromString(padData[4]);
                uPad = new UnnamedPad(location, ownerID);
            }
        }
        return uPad;
    }
}

