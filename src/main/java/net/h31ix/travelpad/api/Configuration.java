package net.h31ix.travelpad.api;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.h31ix.travelpad.Travelpad;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class Configuration {

    private Travelpad plugin;
    private File configFile = new File("plugins/TravelPad/config.yml");
    private YamlConfiguration config;
    private File padsFile = new File("plugins/TravelPad/pads.yml");
    private YamlConfiguration padsYaml;
    private File padsMetaFile = new File("plugins/TravelPad/padmeta.yml");
    private YamlConfiguration padsMeta;

    //TODO: Use this to allow a metaSave to be scheduled or not,
    // still should lock updating pads out while doing the save itself.
    private AtomicBoolean saveScheduled = new AtomicBoolean(false);

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
        if (padsYaml.getStringList("pads").isEmpty()) {
            File legacyPadsFile = new File("plugins/TravelPad/pads2.yml");
            if (legacyPadsFile.exists()) {
                Travelpad.log("Legacy pad file detected, upgrading data");
                YamlConfiguration legacyPads = YamlConfiguration.loadConfiguration(legacyPadsFile);
                if (!legacyPads.getStringList("pads").isEmpty()) {
                    for (String legacyPadString : legacyPads.getStringList("pads")) {
                        //addPad(Pad.serialize(Pad.deserialize(legacyPadString)));
                        Pad pad = Pad.deserialize(legacyPadString);
                        if (pad != null) {
                            addPad(Pad.serialize(pad));
                        } else {
                            Travelpad.error("Error Parsing Serialized Pad: "+legacyPadString+" Skipping!");
                        }
                    }
                    Travelpad.log("Import finished, saving...");
                    savePads();
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
        List<String> padsList = getPads();
        padsList.add(pad);
        padsYaml.set("pads", padsList);
        if (save) {
            savePadsAsync();
        }
    }

    public void addUnnamedPad(String pad) {
        addUnnamedPad(pad, false);
    }

    public void addUnnamedPad(String pad, boolean save) {
        List<String> padsList = getUnvPads();
        padsList.add(pad);
        padsYaml.set("unv", padsList);
        if (save) {
            savePadsAsync();
        }
    }

    public void removePad(String pad) {
        removePad(pad, false);
    }

    public void removePad(String pad, boolean save) {
        List<String> padsList = getPads();
        padsList.remove(pad);
        padsYaml.set("pads", padsList);
        if (save) {
            savePadsAsync();
        }
    }

    public void removeUnnamedPad(String pad) {
        removeUnnamedPad(pad, false);
    }

    public void removeUnnamedPad(String pad, boolean save) {
        List<String> padsList = getUnvPads();
        padsList.remove(pad);
        padsYaml.set("unv", padsList);
        if (save) {
            savePadsAsync();
        }
    }

    public void savePadsAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                savePads();
            }
        });
    }

    public void saveMetaAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                saveMeta();
            }
        });
    }


    public Set<String> getPadsWithMeta() {
        return padsMeta.getKeys(false);
    }

    public boolean hasMeta(String padName){
        return padsMeta.contains(padName);
    }

    public Map<String, Object> getPadMeta(String padName) {
        ConfigurationSection section = padsMeta.getConfigurationSection(padName);
        if (section != null)
            return section.getValues(false);
        else
            return null;
    }

    public void removeMeta(String padName){
        padsMeta.set(padName, null);
        saveMetaAsync();
    }

    /**
     * Consider switching this to flag for async save
     * @param padName
     * @param meta
     */
    public void addPadMeta(String padName, Map<String, Object> meta) {
        padsMeta.set(padName, meta);
    }

    public void reload() {
        load();
    }

    private void loadConfigFromDisk() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void importConfigValues() {
        boolean save = false;
        requireItem = config.getBoolean("Teleportation Options.Require item");
        if (requireItem) {
            takeItem = config.getBoolean("Teleportation Options.Take item");
            itemType = Material.valueOf(config.getString("Teleportation Options.Item name"));
        }

        if (config.getString("Portal Options.Allow any player to break") == null) {
            config.set("Portal Options.Allow any player to break", false);
            save = true;
        }
        if (config.getString("Portal Options.Emit water on creation") == null) {
            config.set("Portal Options.Emit water on creation", true);
            save = true;
        }
        if (config.getString("Portal Options.Center block name") == null) {
            config.set("Portal Options.Center block name", "OBSIDIAN");
            save = true;
        }
        if (config.getString("Portal Options.Outline block name") == null) {
            config.set("Portal Options.Outline block name", "BRICKS");
            save = true;
        }
        center = Material.valueOf(config.getString("Portal Options.Center block name"));
        outline = Material.valueOf(config.getString("Portal Options.Outline block name"));
        anyBreak = config.getBoolean("Portal Options.Allow any player to break");
        emitWater = config.getBoolean("Portal Options.Emit water on creation");

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
        if (save) {
            try {
                config.save(configFile);
            } catch (IOException ex) {
                Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void loadPadsFromDisk() {
        padsYaml = YamlConfiguration.loadConfiguration(padsFile);
    }

    private void loadPadMetaFromDisk() {
        padsMeta = YamlConfiguration.loadConfiguration(padsMetaFile);
    }

    public void savePads() {
        try {
            padsYaml.save(padsFile);
        } catch (IOException ex) {
            Logger.getLogger(Configuration.class.getName()).log(Level.SEVERE, null, ex);
        }
        Travelpad.log("Pads List saved to disk.");
    }

    public void saveMeta(){
        //padsMeta.saveToString();
        try {
            padsMeta.save(padsMetaFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Travelpad.log("Pad meta saved to disk");
    }

    public boolean emitsWater() {
        return emitWater;
    }
}