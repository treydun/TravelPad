package net.h31ix.travelpad;

import net.h31ix.travelpad.event.TravelPadExpireEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TravelPadListener implements Listener {

    private Travelpad plugin;

    public TravelPadListener(Travelpad plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPadExpire(TravelPadExpireEvent event) {
        if (plugin.Config().economyEnabled) {
            plugin.refundNoCreate(Bukkit.getPlayer(event.getPad().OwnerUUID()));
        }
    }
}
