package net.h31ix.travelpad.tasks;

import net.h31ix.travelpad.Travelpad;
import net.h31ix.travelpad.api.Pad;

import java.util.HashSet;
import java.util.Set;

public class SyncMeta implements Runnable {

    public Travelpad plugin;

    private Set<String> dirtyPads = new HashSet<>();

    private boolean enabled = true;

    public SyncMeta(Travelpad plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (enabled) {
            if (!dirtyPads.isEmpty()) {
                String[] dirty = dirtyPads.toArray(new String[0]);
                dirtyPads.clear();
                Travelpad.log("Syncing "+dirty.length+" pads meta with data store");
                for (String dPad : dirty) {
                    Pad pad = plugin.Manager().getPad(dPad);
                    plugin.Config().addPadMeta(pad.getName(), pad.getMeta());
                }
                plugin.Config().saveMetaAsync();
            }
        }
    }

    public void disable() {
        enabled = false;
    }

    //Sync? should only be sync calling it
    public void saveMeta(String padName) {
        dirtyPads.add(padName);
    }

    public void forceSave(){
        if (!dirtyPads.isEmpty()) {
            String[] dirty = dirtyPads.toArray(new String[0]);
            dirtyPads.clear();
            Travelpad.log("Syncing "+dirty.length+" pads meta with data store");
            for (String dPad : dirty) {
                Pad pad = plugin.Manager().getPad(dPad);
                plugin.Config().addPadMeta(pad.getName(), pad.getMeta());
            }
            Travelpad.log("Writing meta to disk.");
            plugin.Config().saveMeta();
        }
    }
}
