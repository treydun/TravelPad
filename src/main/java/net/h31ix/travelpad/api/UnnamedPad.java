package net.h31ix.travelpad.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * <p>
 * Defines a new Unnamed TravelPad on the map, this is only used before a pad is named by the player.
 */

public class UnnamedPad {

    private Location location;
    private Player owner;

    public UnnamedPad(Location location, Player owner) {
        this.location = location;
        this.owner = owner;
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
    public Player getOwner() {
        return owner;
    }

}

