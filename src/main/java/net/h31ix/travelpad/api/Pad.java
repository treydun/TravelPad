package net.h31ix.travelpad.api;

import net.h31ix.travelpad.Travelpad;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

/**
 * <p>
 * Defines a new TravelPad on the map, this is only used after a pad has a name.
 */
public class Pad implements Comparable {

    private Location location;
    private String name;
    private UUID ownerUUID;

    private transient String ownerName = "";
    private transient int usedSince = 0;

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
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Get the location of the pad that is safe for a player to teleport to
     *
     * @return location  Safe teleport location
     */
    public Location getTeleportLocation() {
        return new Location(location.getWorld(), location.getX(), location.getY() + 2, location.getZ(), location.getYaw(), 0);
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
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

    public void setPublic(boolean makePublic) {
        publicPad = makePublic;
    }

    public boolean isPublic() {
        return publicPad;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setLastUsed() {
        usedSince++;
        System.currentTimeMillis();
    }

    public void resetStats() {
        usedSince = 0;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public int getUsedSince() {
        return usedSince;
    }

    public void setPrepaid(int prepaid) {
        this.prepaidTeleports = prepaid;
    }

    public boolean chargePrepaid() {
        if (prepaidTeleports >= 1) {
            prepaidTeleports--;
            return true;
        }
        return false;
    }

    public int prepaidsLeft() {
        return prepaidTeleports;
    }

    public boolean hasMeta() {
        return publicPad || !description.isEmpty() || lastUsed != 0L || prepaidTeleports != 0;
    }

    public void importMeta(Map<String, Object> meta) {
        if (!meta.isEmpty()) {
            for (String key : meta.keySet()) {
                switch (key) {
                    case "description":
                        description = (String) meta.get("description");
                        break;
                    case "lastused":
                        lastUsed = Long.parseLong((String) meta.get("lastused"));
                        break;
                    case "prepaidteleports":
                        prepaidTeleports = Integer.parseInt((String) meta.get("prepaidteleports"));
                    case "public":
                        publicPad = true;
                }
            }
        }
    }

    public Map<String, Object> getMeta() {
        Map<String, Object> meta = new HashMap<>();
        if (publicPad) {
            meta.put("public", true);
        }
        if (!description.isEmpty()) {
            meta.put("description", description);
        }
        if (lastUsed != 0L) {
            meta.put("lastused", lastUsed);
        }
        if (prepaidTeleports != 0) {
            meta.put("prepaidteleports", prepaidTeleports);
        }
        //If failed to find any meta to store, purge from metastore via null
        if (meta.isEmpty()) {
            return null;
        } else {
            return meta;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(name);
        builder.append(" ");
        builder.append(Travelpad.formatLocation(location));
        builder.append(" ");
        builder.append(ownerUUID);
        if (ownerName != null && !ownerName.isEmpty()) {
            builder.append(" ");
            builder.append(ownerName);
        }
        if (publicPad) {
            builder.append(" ");
            builder.append("public: true");
        }
        if (description != null && !description.isEmpty()) {
            builder.append(" ");
            builder.append(description);
        }
        if (lastUsed != 0) {
            builder.append(" ");
            builder.append("last used:");
            builder.append(lastUsed);
        }
        if (prepaidTeleports != 0) {
            builder.append(" ");
            builder.append("prepaid:");
            builder.append(prepaidTeleports);
        }
        return builder.toString();
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
        if (pad.getLocation().getYaw() != 0.0F) {
            padString.append(pad.getLocation().getYaw());
            padString.append(Travelpad.DELIMINATOR);
        }
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
                Location location = new Location(world, x + .5, y + .5, z + .5);
                UUID ownerID = UUID.fromString(padData[5]);
                pad = new Pad(location, ownerID, padData[0]);
            } else {
                Travelpad.error("Failed to load a location with " + padData[1] + " world");
            }
        } else if (padData.length == 7) {
            //Tpads 2.0 Name/X/Y/Z/World/OwnerName/OwnerUUID
            if (Travelpad.isInteger.matcher(padData[1]).matches()) {
                World world = Bukkit.getWorld(padData[4]);
                if (world != null) {
                    int x = Integer.parseInt(padData[1]);
                    int y = Integer.parseInt(padData[2]);
                    int z = Integer.parseInt(padData[3]);
                    Location location = new Location(world, x + .5F, y + .5F, z + .5F);
                    UUID ownerID = UUID.fromString(padData[6]);
                    pad = new Pad(location, ownerID, padData[0]);
                    pad.setOwnerName(padData[5]);
                } else {
                    Travelpad.error("Failed to load a location with " + padData[4] + " world");
                }
            } else {
                //Tpads 3.1 Name/World/X/Y/Z/NSEW/OwnerUUID
                World world = Bukkit.getWorld(padData[1]);
                if (world != null) {
                    int x = Integer.parseInt(padData[2]);
                    int y = Integer.parseInt(padData[3]);
                    int z = Integer.parseInt(padData[4]);
                    float direction = Float.parseFloat(padData[5]);
                    Location location = new Location(world, x + .5, y + .5, z + .5, direction, 0.0F);
                    UUID ownerID = UUID.fromString(padData[6]);
                    pad = new Pad(location, ownerID, padData[0]);
                } else {
                    Travelpad.error("Failed to load a location with " + padData[1] + " world");
                }
            }
        } else {
            System.out.print("Tpads Error: Failed to deserialize " + serialized + " := Incorrect Parameter Length");
        }
        return pad;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Pad) {
            Pad otherPad = (Pad) o;
            //if (this.lastUsed > otherPad.lastUsed) {
            if (weightedScore(this) > weightedScore(otherPad)) {
                return 1;
            } else {
                return -1;
            }
        }
        return 0;
    }

    public static int weightedScore(Pad pad) {
        int score = 0;
        if (pad.lastUsed != 0) {
            score = new Long(System.currentTimeMillis() - pad.lastUsed).intValue();
        }

        Long lastSeen = Travelpad.getLastSeen(pad.ownerUUID);
        if (lastSeen != -1) {
            score = score + lastSeen.intValue();
        } else {
            score = score + (1000*60*60*24*7);
            Travelpad.error("Failed to load pad owners bukkit seen time? "+pad.toString());

        }

        if (pad.usedSince != 0) {
            score = score / pad.usedSince;
        }
        //Lower is better
        Travelpad.log(pad.getName() + " score:" + score);
        return score;
    }
}

class SortByLastUsed implements Comparator<Pad> {

    @Override
    public int compare(Pad pad1, Pad pad2) {
        if (pad1.getLastUsed() > pad2.getLastUsed()) {
            return 1;
        } else if (pad1.getLastUsed() < pad2.getLastUsed()) {
            return -1;
        } else {
            return 0;
        }
    }
}

class SortByMostUsed implements Comparator<Pad> {

    @Override
    public int compare(Pad pad1, Pad pad2) {
        if (pad1.getUsedSince() > pad2.getUsedSince()) {
            return 1;
        } else if (pad1.getUsedSince() < pad2.getUsedSince()) {
            return -1;
        } else {
            return 0;
        }
    }
}
