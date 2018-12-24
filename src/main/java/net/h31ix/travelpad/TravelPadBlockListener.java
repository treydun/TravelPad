package net.h31ix.travelpad;

import net.h31ix.travelpad.api.Pad;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class TravelPadBlockListener implements Listener {

    private Travelpad plugin;

    public TravelPadBlockListener(Travelpad plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand().equals(EquipmentSlot.HAND)) {
            Block block = event.getClickedBlock();
            if (block.getType() == plugin.Config().center) {
                if (block.getRelative(BlockFace.EAST).getType() == plugin.Config().outline
                        && block.getRelative(BlockFace.WEST).getType() == plugin.Config().outline
                        && block.getRelative(BlockFace.NORTH).getType() == plugin.Config().outline
                        && block.getRelative(BlockFace.SOUTH).getType() == plugin.Config().outline) {
                    if (plugin.Manager().getPadAt(block.getLocation()) == null) {
                        Player player = event.getPlayer();
                        if (plugin.canCreate(player)) {
                            plugin.create(block.getLocation(), player);
                        }
                    } else {
                        event.getPlayer().sendMessage(Travelpad.PLUGIN_PREFIX_COLOR + "There is already a Travelpad at this location!");
                    }
                }
            } else if (event.getHand().equals(EquipmentSlot.HAND) && (block.getType() == Material.getMaterial("SIGN_POST") || block.getType() == Material.getMaterial("WALL_SIGN"))) {
                if (!event.isCancelled()) {
                    {
                        BlockState bState = block.getState();
                        Sign sign = (Sign) bState;
                        if (sign.getLine(0).equalsIgnoreCase("[Travelpad]")
                                || sign.getLine(0).equalsIgnoreCase("Travelpad")) {
                            if (plugin.Manager().getPad(sign.getLine(1)) != null) {
                                sign.setLine(0, ChatColor.GREEN + "[Travelpad]");
                            } else {
                                sign.setLine(0, ChatColor.RED + "[Travelpad]");
                                event.getPlayer().sendMessage(Travelpad.PLUGIN_PREFIX_COLOR + ChatColor.RED + "Error, unable to find the pad " + sign.getLine(1));
                            }
                        }
                        if (sign.getLine(0).equalsIgnoreCase(ChatColor.GREEN + "{Travelpad]")) {
                            //To validate every teleport or not... If invalid it wouldnt work, turn it red?
                            //Prob a good idea for when pads get removed without the sign users knowing
                            if (plugin.Manager().getPad(sign.getLine(1)) != null) {
                                event.getPlayer().performCommand("t tp " + sign.getLine(1));
                            } else {
                                sign.setLine(0, ChatColor.RED + "[Travelpad]");
                                event.getPlayer().sendMessage(Travelpad.PLUGIN_PREFIX_COLOR + ChatColor.RED + "Error, unable to find the pad " + sign.getLine(1));
                            }
                        }
                        //Legacy sign support
                        if (sign.getLine(1).startsWith("/t tp ")) {
                            event.getPlayer().performCommand(sign.getLine(1).substring(1));
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        //TODO: Optimize... (Switched to single lookup for efficiency. Needs more thought)
        Block block = event.getBlock();
        Location location = block.getLocation();
        //Was getPadNear()
        if (plugin.Manager().getPadAt(location) != null || plugin.Manager().getUnnamedPadAt(location) != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Travelpad.PLUGIN_PREFIX_COLOR + ChatColor.RED + "Why are you placing a block where a tpad is, this is a bug!");
        } else {
            if (block.getType() == plugin.Config().center) {
                if (block.getRelative(BlockFace.EAST).getType() == plugin.Config().outline
                        && block.getRelative(BlockFace.WEST).getType() == plugin.Config().outline
                        && block.getRelative(BlockFace.NORTH).getType() == plugin.Config().outline
                        && block.getRelative(BlockFace.SOUTH).getType() == plugin.Config().outline) {
                    Player player = event.getPlayer();
                    if (plugin.canCreate(player)) {
                        plugin.create(block.getLocation(), player);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        //TODO: Optimize... (Switched to single lookup for efficiency. Needs more thought)
        Block block = event.getBlock();
        if (block.getType() == plugin.Config().center) {
            //Was getPadNear()
            Pad pad = plugin.Manager().getPadAt(block.getLocation());
            if (pad != null) {
                if (event.getPlayer().hasPermission("travelpad.create")) {
                    if (plugin.Config().anyBreak || pad.ownerUUID().equals(event.getPlayer().getUniqueId())) {
                        plugin.delete(pad);
                    } else {
                        event.getPlayer().sendMessage(ChatColor.RED + plugin.Lang().command_deny_permission());
                        event.setCancelled(true);
                    }
                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + plugin.Lang().command_deny_permission());
                }
            } else if (plugin.Manager().getUnnamedPadAt(block.getLocation()) != null) {
                event.setCancelled(true);
            }
        }
        /* remove the 'any' block break check. resource heavy with lots of pads
        else {
            Location location = block.getLocation();
            Pad pad = plugin.getPadAt(location);
            UnnamedPad upad = plugin.getUnnamedPadAt(location);
            if (pad != null || upad != null) {
                event.setCancelled(true);
            }
        } */
    }
}
