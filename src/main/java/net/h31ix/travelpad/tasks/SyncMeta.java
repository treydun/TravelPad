package net.h31ix.travelpad.tasks;

import net.h31ix.travelpad.Travelpad;
import net.h31ix.travelpad.api.Pad;

import java.util.HashSet;
import java.util.Set;

public class SyncMeta implements Runnable {

    public Travelpad plugin;

    public Set<String> dirtyPads=new HashSet<>();

    private boolean enabled = true;

    public SyncMeta(Travelpad plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        while (enabled) {
            if (!dirtyPads.isEmpty()) {
                String[] dirty;
                synchronized (dirtyPads){
                    dirty = (String[]) dirtyPads.toArray();
                    dirtyPads.clear();
                }
                Travelpad.log("Syncing meta with data store");
                for(String dPad: dirty) {
                    Pad pad = plugin.Manager().getPad(dPad);
                    plugin.Config().addPadMeta(pad.getName(), pad.getMeta());
                }
                plugin.Config().saveMeta();
            }
            try {
                Thread.sleep(1000 * 60);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void disable() {
        enabled = false;
    }

    //Sync? should only be sync calling it
    public void saveMeta(String padName){
        synchronized (dirtyPads){
            dirtyPads.add(padName);
        }
    }
}
