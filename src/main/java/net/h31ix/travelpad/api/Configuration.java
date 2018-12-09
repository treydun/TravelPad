package net.h31ix.travelpad.api;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.h31ix.travelpad.Travelpad;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class Configuration {

    private Travelpad plugin;
    private File configFile = new File("plugins/TravelPad/config.yml");
    private FileConfiguration config;
    private File padsFile = new File("plugins/TravelPad/pads.yml");
    private FileConfiguration padsYaml;
    private File padsMetaFile = new File("plugins/TravelPad/padmeta.yml");
    private FileConfiguration padsMeta;

    public boolean requireItem = false;
    public boolean takeItem = false;
    public Material itemType = null;

    private boolean chargeCreate = false;
    public double createAmount = 0;
    private boolean refundDelete = false;
    public double deleteAmount = 0;

    public boolean chargeTeleport = false;
    public double teleportAmount = 0;

    public boolean economyEnabled = false;

    public boolean anyBreak = false;

    private boolean emitWater = false;

    public Material center = Material.OBSIDIAN;
    public Material outline = Material.BRICKS;

    public Configuration(Travelpad plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        Travelpad.log("Loading config from disk");
        loadConfigFromDisk();
        Travelpad.log("Propogating values");
        importConfigValues();
        Travelpad.log("Loading pads from disk");
        loadPadsFromDisk();
        if(padsYaml.getStringList("pads").isEmpty()){
            File legacyPadsFile = new File("plugins/TravelPad/pads2.yml");
            if(legacyPadsFile.exists()){
                Travelpad.log("Legacy pad file detected, upgrading data");
                FileConfiguration legacyPads = YamlConfiguration.loadConfiguration(legacyPadsFile);
                if(!legacyPads.getStringList("pads").isEmpty()) {
                    for(String legacyPadString:legacyPads.getStringList("pads")) {
                        addPad(Pad.serialize(Pad.deserialize(legacyPadString)));
                    }
                    Travelpad.log("Import finished, saving...");
                }
            }
        }
        Travelpad.log("Loading PadMeta from disk");
        loadPadMetaFromDisk();
    }

    public List<String> getPads() {
        return padsYaml.getStringList("pads");
    }

    public List<String> getUnvPads() {
        return padsYaml.getStringList("unv");
    }

    public void addPad(String pad) {
        addPad(pad, false);
    }

    public void addPad(String pad, boolean save) {
        padsYaml.getStringList("pads").add(pad);
        if (save) {
            saveAsync();
        }
    }

    public void addUnnamedPad(String pad) {
        addUnnamedPad(pad, false);
    }

    public void addUnnamedPad(String pad, boolean save) {
        padsYaml.getStringList("unv").add(pad);
        if (save) {
            saveAsync();
        }
    }

    public void removePad(String pad) {
        removePad(pad, false);
    }

    public void removePad(String pad, boolean save) {
        padsYaml.getStringList("pads").remove(pad);
        if (save) {
            saveAsync();
        }
    }

    public void removeUnnamedPad(String pad) {
        removeUnnamedPad(pad, false);
    }

    public void removeUnnamedPad(String pad, boolean save) {
        padsYaml.getStringList("unv").remove(pad);
        if (save) {
            saveAsync();
        }
    }

    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                save();
            }
        });
    }

    public Set<String> getPublicPads() {
        return padsMeta.getKeys(false);
    }

    public Map<String, Object> getPadMeta(String padName) {
        return padsMeta.getConfigurationSection(padName).getValues(false);
    }

    public void addPadMeta(String padName, Map<String, Object> meta) {
        padsMeta.set(padName, meta);
        try {
            padsMeta.save(padsMetaFile);
        } catch (IOException e) {
            e.printStackTrace();
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

    public void reload() {
        load();
    }

    private void loadConfigFromDisk() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void importConfigValues() {
        if (config.getString("Portal Options.Allow any player to break") == null) {
            config.set("Portal Options.Allow any player to break", false);
            try {
                config.save(configFile);
            } catch (IOException ex) {
                Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (config.getString("Portal Options.Emit water on creation") == null) {
            config.set("Portal Options.Emit water on creation", true);
            try {
                config.save(configFile);
            } catch (IOException ex) {
                Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (config.getString("Portal Options.Center block name") == null) {
            config.set("Portal Options.Center block name", "OBSIDIAN");
            try {
                config.save(configFile);
            } catch (IOException ex) {
                Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (config.getString("Portal Options.Outline block name") == null) {
            config.set("Portal Options.Outline block name", "BRICKS");
            try {
                config.save(configFile);
            } catch (IOException ex) {
                Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        center = Material.valueOf(config.getString("Portal Options.Center block name"));
        outline = Material.valueOf(config.getString("Portal Options.Outline block name"));
        anyBreak = config.getBoolean("Portal Options.Allow any player to break");
        emitWater = config.getBoolean("Portal Options.Emit water on creation");
        requireItem = config.getBoolean("Teleportation Options.Require item");
        if (requireItem) {
            takeItem = config.getBoolean("Teleportation Options.Take item");
            itemType = Material.valueOf(config.getString("Teleportation Options.Item name"));
        }
        chargeCreate = config.getBoolean("Portal Options.Charge on creation");
        refundDelete = config.getBoolean("Portal Options.Refund on deletion");
        chargeTeleport = config.getBoolean("Teleportation Options.Charge player");
        if (chargeCreate || refundDelete || chargeTeleport) {
            economyEnabled = true;
        }
        if (chargeCreate) {
            createAmount = config.getDouble("Portal Options.Creation charge");
        }
        if (refundDelete) {
            deleteAmount = config.getDouble("Portal Options.Deletion return");
        }
        if (chargeTeleport) {
            teleportAmount = config.getDouble("Teleportation Options.Charge amount");
        }
    }

    private void loadPadsFromDisk() {
        padsYaml = YamlConfiguration.loadConfiguration(padsFile);
    }

    private void loadPadMetaFromDisk() {
        padsMeta = YamlConfiguration.loadConfiguration(padsMetaFile);
    }

    public void save() {
        try {
            padsYaml.save(padsFile);
        } catch (IOException ex) {
            Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
        }
        Travelpad.log("Pads List saved to disk.");
    }

    public boolean emitsWater() {
        return emitWater;
    }
}