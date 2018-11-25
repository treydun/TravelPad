package net.h31ix.travelpad;

import net.h31ix.travelpad.api.Configuration;
import net.h31ix.travelpad.api.Pad;
import net.h31ix.travelpad.api.TravelPadManager;
import net.h31ix.travelpad.api.UnnamedPad;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class TravelPadBlockListener implements Listener {

    private Travelpad plugin;
    private TravelPadManager manager;
    private Configuration config;

    public TravelPadBlockListener(Travelpad plugin) {
        this.plugin = plugin;
        manager = plugin.Manager();
        this.config = manager.config;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block.getType() == config.center) {
                if (block.getRelative(BlockFace.EAST).getType() == config.outline && block.getRelative(BlockFace.WEST).getType() == config.outline && block.getRelative(BlockFace.NORTH).getType() == config.outline && block.getRelative(BlockFace.SOUTH).getType() == config.outline) {
                    if (plugin.getPadAt(block.getLocation()) == null) {
                        Player player = event.getPlayer();
                        if (plugin.canCreate(player)) {
                            plugin.create(block.getLocation(), player);
                        }
                    } else {
                        event.getPlayer().sendMessage(Travelpad.PLUGIN_PREFIX_COLOR + "There is already a Tpad at this location!");
                    }
                }
            } else if (block.getType() == Material.getMaterial("SIGN_POST") || block.getType() == Material.getMaterial("WALL_SIGN")) {
                if (!event.isCancelled()) {
                    {
                        BlockState bState = block.getState();
                        Sign sign = (Sign) bState;
                        if (sign.getLine(1).startsWith("/t tp ")) {
                            event.getPlayer().performCommand(sign.getLine(1).substring(1));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        Pad cpad = plugin.getPadAt(location);
        UnnamedPad upad = plugin.getUnnamedPadAt(location);
        if (cpad != null || upad != null) {
            event.setCancelled(true);
        } else {
            if (block.getType() == config.center) {
                if (block.getRelative(BlockFace.EAST).getType() == config.outline && block.getRelative(BlockFace.WEST).getType() == config.outline && block.getRelative(BlockFace.NORTH).getType() == config.outline && block.getRelative(BlockFace.SOUTH).getType() == config.outline) {
                    Player player = event.getPlayer();
                    if (plugin.canCreate(player)) {
                        plugin.create(block.getLocation(), player);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == config.center) {
            Pad pad = manager.getPadAt(block.getLocation());
            UnnamedPad upad = plugin.getUnnamedPadAt(block.getLocation());
            if (pad != null) {
                if (event.getPlayer().hasPermission("travelpad.create")) {
                    if (manager.config.anyBreak || pad.ownerUUID().equals(event.getPlayer().getUniqueId())) {
                        plugin.delete(pad);
                    } else {
                        event.getPlayer().sendMessage(ChatColor.RED + manager.l.command_deny_permission());
                        event.setCancelled(true);
                    }
                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + manager.l.command_deny_permission());
                }
            } else if (upad != null) {
                event.setCancelled(true);
            }
        } else {
            Location location = block.getLocation();
            Pad pad = plugin.getPadAt(location);
            UnnamedPad upad = plugin.getUnnamedPadAt(location);
            if (pad != null || upad != null) {
                event.setCancelled(true);
            }
        }
    }

}
