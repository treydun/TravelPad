package net.h31ix.travelpad;

import net.h31ix.travelpad.api.Pad;
import net.h31ix.travelpad.api.UnnamedPad;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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

    private final String TRAVELPAD_SIGN_TAG = (ChatColor.GREEN + "[Travelpad]").intern();
    private final String TRAVELPAD_SIGN_ERROR_TAG = (ChatColor.RED+ "[Travelpad]").intern();

    private Travelpad plugin;

    public TravelPadBlockListener(Travelpad plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
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
                        plugin.errorMessage(event.getPlayer(), "There is already a Travelpad at this location!");
                    }
                }
            } else if (block.getType() == Material.SIGN || block.getType() == Material.WALL_SIGN) {
                BlockState bState = block.getState();
                Sign sign = (Sign) bState;
                if (sign.getLine(0).equals(TRAVELPAD_SIGN_TAG)) {
                    if (plugin.Manager().getPad(sign.getLine(1)) != null) {
                        event.getPlayer().performCommand("t tp " + sign.getLine(1));
                    } else {
                        sign.setLine(0, TRAVELPAD_SIGN_ERROR_TAG);
                        sign.update(true);
                        plugin.errorMessage(event.getPlayer(), "Error, unable to find the pad " + sign.getLine(1));
                    }
                } else if (sign.getLine(0).toLowerCase().contains("travelpad")) {
                    if (plugin.Manager().getPad(sign.getLine(1)) != null) {
                        sign.setLine(0, TRAVELPAD_SIGN_TAG);
                        sign.update(true);
                        event.getPlayer().performCommand("t tp " + sign.getLine(1));
                    } else {
                        sign.setLine(0, TRAVELPAD_SIGN_ERROR_TAG);
                        sign.update(true);
                        plugin.errorMessage(event.getPlayer(), "Error, unable to find the pad " + sign.getLine(1));
                    }
                }
                //Legacy sign support
                if (sign.getLine(1).startsWith("/t tp ")) {
                    event.getPlayer().performCommand(sign.getLine(1).substring(1));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        if (plugin.Manager().getPadAt(location) != null || plugin.Manager().getUnnamedPadAt(location) != null) {
            event.setCancelled(true);
            plugin.errorMessage(event.getPlayer(), "Why are you placing a block where a tpad is, this is a bug!");
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
        Block block = event.getBlock();
        if (block.getType() == plugin.Config().center) {
            Pad pad = plugin.Manager().getPadAt(block.getLocation());
            if (pad != null) {
                if (event.getPlayer().hasPermission("travelpad.delete")) {
                    if (plugin.Config().anyBreak || plugin.Manager().isOwner(event.getPlayer(), pad) || event.getPlayer().hasPermission("travelpad.delete.any")) {
                        plugin.message(event.getPlayer(), plugin.Lang().delete_approve());
                        plugin.delete(pad);
                    } else {
                        plugin.errorMessage(event.getPlayer(), plugin.Lang().command_deny_permission());
                        event.setCancelled(true);
                    }
                } else {
                    plugin.errorMessage(event.getPlayer(), plugin.Lang().command_deny_permission());
                    event.setCancelled(true);
                }
            } else if (plugin.Manager().getUnnamedPadAt(block.getLocation()) != null) {
                UnnamedPad uPad = plugin.Manager().getUnnamedPadAt(block.getLocation());
                if (uPad.OwnerUUID().equals(event.getPlayer().getUniqueId())) {
                    plugin.message(event.getPlayer(), plugin.Lang().delete_approve());
                    plugin.Manager().removePad(uPad);
                } else {
                    plugin.errorMessage(event.getPlayer(), plugin.Lang().command_deny_permission());
                    event.setCancelled(true);
                }
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
