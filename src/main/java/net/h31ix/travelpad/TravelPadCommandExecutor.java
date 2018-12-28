package net.h31ix.travelpad;

import com.buildatnight.legacyutils.Format;
import net.h31ix.travelpad.api.Pad;
import net.h31ix.travelpad.event.TravelPadTeleportEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class TravelPadCommandExecutor implements CommandExecutor {
    private Travelpad plugin;

    public TravelPadCommandExecutor(Travelpad plugin) {
        this.plugin = plugin;
    }

    /**
     * Comments: Switched method to case based, They dont NEED to return, i may remove that to make it a cleaner read
     * Not going to depend on spigots help context..
     *
     * @param sender Console or In game Player
     * @param cmd The command executed
     * @param alias The alias used if an alias was used
     * @param args Parameters of the command
     * @return whether the command executed properly. False triggers Bukkits default Help from Plugin.yml
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args != null && args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "reload":
                    return reload(sender);
                case "help":
                case "h":
                    return showHelp(sender);
                case "set":
                case "s":
                    return set(sender, args);
                case "identify":
                case "i":
                    return identify(sender);
                case "delete":
                case "d":
                    return delete(sender, args);
                case "list":
                case "l":
                    return list(sender, args);
                case "name":
                case "n":
                    return name(sender, args);
                case "teleport":
                case "tele":
                case "tp":
                case "t":
                    return teleport(sender, args);
                case "prepay":
                case "pre":
                    return prepay(sender, args);
                default:
                    return false;
            }
        } else {
            return publicPadList(sender);
        }
    }

    /**
     * Reloads the config from disk and repopulates, if the sender has permission
     *
     * @param sender
     * @return true;
     */
    private boolean reload(CommandSender sender) {
        if (sender.hasPermission("tavelpad.reload")) {
            plugin.Config().reload();
        } else {
            plugin.errorMessage(sender, plugin.Lang().command_deny_permission());
        }
        return true;
    }

    private boolean identify(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Pad pad = plugin.Manager().getPadNear(player.getLocation());
            if (pad != null) {
                plugin.message(player, plugin.Lang().identify_found_message() + ChatColor.WHITE + " " + pad.getName());
            } else {
                plugin.errorMessage(player, plugin.Lang().identify_notfound_message());
            }
        } else {
            plugin.errorMessage(sender, plugin.Lang().command_deny_console());
        }
        return true;
    }

    private boolean delete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            //TODO: Shouldnt this really be a proximal check method? getPadNear()??
            //I guess falling back to forcing them to delete it by name is a lot safer
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (plugin.getPads(player) > 1) {
                    plugin.errorMessage(player, plugin.Lang().delete_deny_multi());
                } else {
                    if (plugin.hasPad(player)) {
                        Object[] pads = plugin.Manager().getPadsFrom(player.getUniqueId()).toArray();
                        plugin.Manager().deletePad((Pad) pads[0]);
                    } else {
                        plugin.errorMessage(player, plugin.Lang().delete_deny_noportal());
                    }
                }
            } else {
                plugin.errorMessage(sender, plugin.Lang().command_deny_console());
            }
        } else if (args.length == 2) {
            //Deleting by name
            Pad pad = plugin.Manager().getPad(args[1]);
            if (pad != null) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (!(plugin.Manager().getPad(args[1])).ownerUUID().equals(player.getUniqueId())) {
                        if (player.hasPermission("travelpad.delete.all") || player.hasPermission("travelpad.delete.any")) {
                            plugin.Manager().deletePad(pad);
                        } else {
                            plugin.errorMessage(player, plugin.Lang().command_deny_permission());
                        }
                    } else {
                        plugin.Manager().deletePad(plugin.Manager().getPad(args[1]));
                    }
                } else {
                    //Allow console to delete pads by name
                    plugin.Manager().deletePad(pad);
                }
            } else {
                plugin.errorMessage(sender, plugin.Lang().delete_deny_notfound());
            }
        }
        return true;
    }

    private boolean list(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("travelpad.list")) {
                    List<Pad> ppads = plugin.Manager().getPadsFrom(player.getUniqueId());
                    player.sendMessage(Travelpad.PLUGIN_PREFIX_COLOR + "Your telepads are:");
                    for (Pad p : ppads) {
                        TextComponent padName = new TextComponent(" * " + Format.firstLetterCaps(p.getName()));
                        padName.setColor(ChatColor.GREEN);
                        padName.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/t tp " + p.getName()));
                        padName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("Click Me!! To go to " + Format.firstLetterCaps(p.getName()))}));
                        player.spigot().sendMessage(padName);
                    }
                } else {
                    plugin.errorMessage(player, plugin.Lang().command_deny_permission());
                }
            } else {
                plugin.errorMessage(sender, plugin.Lang().command_deny_console());
            }
            return true;
        } else if (args.length == 2) {
            if (args[1].equalsIgnoreCase("all") && sender.hasPermission("travelpad.list.all")) {
                List<Pad> pads = plugin.Manager().getPads();
                if (pads != null && !pads.isEmpty()) {
                    for (Pad p : pads) {
                        sender.sendMessage(p.getName() + ":" + Pad.serialize(p));
                    }
                    return true;
                } else {
                    sender.sendMessage("Unable to find any pads");
                    return true;
                }
            } else if (args[1].equalsIgnoreCase("Admin") && sender.hasPermission("travelpad.list.admin")) {
                List<Pad> pads = plugin.Manager().getPadsFrom(Travelpad.ADMIN_UUID);
                if (pads != null && !pads.isEmpty()) {
                    plugin.message(sender, args[1] + "'s " + ChatColor.GREEN + "telepads are:");
                    for (Pad p : pads) {
                        TextComponent padName = new TextComponent(" * " + Format.firstLetterCaps(p.getName()));
                        padName.setColor(ChatColor.GREEN);
                        padName.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/t tp " + p.getName()));
                        padName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("Click Me!! To go to " + Format.firstLetterCaps(p.getName()))}));
                        if (sender instanceof Player) {
                            //TODO: Recasting each time in a loop is dumbie
                            ((Player) sender).spigot().sendMessage(padName);
                        } else {
                            sender.sendMessage(padName.toLegacyText());
                        }
                    }
                }
            } else if (sender.hasPermission("travelpad.list.others")) {
                UUID ownerID = plugin.getPlayerUUIDbyName(args[1]);
                if (ownerID != null) {
                    List<Pad> ppads = plugin.Manager().getPadsFrom(ownerID);
                    if (ppads != null && !ppads.isEmpty()) {
                        plugin.message(sender, args[1] + "'s " + ChatColor.GREEN + "telepads are:");
                        for (Pad p : ppads) {
                            TextComponent padName = new TextComponent(" * " + Format.firstLetterCaps(p.getName()));
                            padName.setColor(ChatColor.GREEN);
                            padName.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/t tp " + p.getName()));
                            padName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("Click Me!! To go to " + Format.firstLetterCaps(p.getName()))}));
                            if (sender instanceof Player) {
                                //TODO: Recasting each time in a loop is dumbie
                                ((Player) sender).spigot().sendMessage(padName);
                            } else {
                                sender.sendMessage(padName.toLegacyText());
                            }
                        }
                    } else {
                        plugin.errorMessage(sender, plugin.Lang().list_no_pads() + args[1]);
                    }
                } else {
                    plugin.errorMessage(sender, plugin.Lang().list_no_player() + args[1]);
                }
            } else {
                plugin.errorMessage(sender, plugin.Lang().command_deny_permission());
            }
            return true;
        }
        return false;
    }

    private boolean name(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args[1].contains("/")) {
                plugin.errorMessage(player, "Please do not use '/' in the TravelPad name!");
            } else {
                if (!plugin.Manager().padExists(args[1])) {
                    String name = args[1];
                    boolean set = plugin.namePad(player, name);
                    if (set) {
                        plugin.message(player, plugin.Lang().name_message() + ChatColor.WHITE + " " + name);
                        return true;
                    } else {
                        plugin.errorMessage(player, plugin.Lang().name_deny_nopad());
                    }
                } else {
                    plugin.errorMessage(player, plugin.Lang().name_deny_inuse());
                }
            }
        } else {
            plugin.errorMessage(sender, plugin.Lang().command_deny_console());
        }
        return false;
    }

    private boolean teleport(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 2) {
                if (player.hasPermission("travelpad.teleport") || player.hasPermission("travelpad.tp")) {
                    Pad originPad = null;
                    //No charge
                    if (!player.hasPermission("travelpad.tpanywhere")) {
                        originPad = plugin.Manager().getPadNear(player.getLocation()); //EXPENSIVE OPERATION
                    }
                    if (originPad != null || player.hasPermission("travelpad.tpanywhere")) {
                        Pad destinationPad = plugin.Manager().getPad(args[1]);
                        if (destinationPad != null) {
                            if (plugin.canAffordTeleport(player)) {
                                TravelPadTeleportEvent e = new TravelPadTeleportEvent(destinationPad, originPad, player);
                                plugin.getServer().getPluginManager().callEvent(e);
                                if (!e.isCancelled()) {
                                    Location loc = e.getTo().getTeleportLocation();
                                    plugin.teleport(player, loc);
                                }
                            } else {
                                plugin.errorMessage(player, "Not enough money!");
                            }
                        } else {
                            plugin.errorMessage(player, plugin.Lang().teleport_deny_notfound());
                        }
                    } else {
                        plugin.errorMessage(player, plugin.Lang().teleport_deny_loc());
                    }
                } else {
                    plugin.errorMessage(player, plugin.Lang().command_deny_permission());
                }
                return true;
            } else {
                showHelp(sender);
            }
        } else {
            plugin.errorMessage(sender, plugin.Lang().command_deny_console());
        }
        return true;
    }

    private boolean prepay(CommandSender sender, String[] args) {
        if (args.length == 3) {
            Pad pad = plugin.Manager().getPad(args[1]);
            if (pad != null) {
                int prepaid = Integer.parseInt(args[2]);
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (pad.ownerUUID().equals(player.getUniqueId()) || player.hasPermission("travelpad.admin") || player.hasPermission("travelpad.prepay.others")) {
                        //TODO: Should we include some sort of refund/undo system for this?
                        if (player.getInventory().contains(Material.ENDER_EYE, prepaid)) {
                            int stacks = prepaid / (new ItemStack(Material.ENDER_EYE).getMaxStackSize());
                            int remainder = prepaid % (new ItemStack(Material.ENDER_EYE).getMaxStackSize());
                            while (stacks > 0) {
                                player.getInventory().remove(new ItemStack(Material.ENDER_EYE, 64));
                                stacks--;
                            }
                            player.getInventory().remove(new ItemStack(Material.ENDER_EYE, remainder));
                            //TODO: charge success, update pad prepaids to reflect new balance (make sure to add to, not replace)
                        } else if (plugin.charge(player, prepaid * plugin.Config().teleportAmount)) {
                            //TODO: Charge success, update pad prepaids to reflect new balance (make sure to add to, not replace)
                        } else {
                            plugin.errorMessage(player, plugin.Lang().create_deny_money());
                        }
                    } else {
                        sender.sendMessage("No permission Place Holder");
                    }
                } else {
                    plugin.errorMessage(sender, plugin.Lang().command_deny_console());
                }
            } else {
                plugin.errorMessage(sender, "NO_PAD_BY_THAT_NAME");
            }
        }
        //Invalid number of arguments error message
        return false;
    }

    private boolean set(CommandSender sender, String[] args) {
        //0 = set | 1 = padname | 2 == parameter
        //Proximal support?
        if (args.length >= 3) {
            Pad pad = plugin.Manager().getPad(args[1]);
            if (pad != null) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (pad.ownerUUID().equals(player.getUniqueId()) || player.hasPermission("travelpad.admin")) {
                        switch (args[2].toLowerCase()) {
                            case "public":
                                pad.setPublic(true);
                                plugin.Meta().saveMeta(pad.getName());
                                plugin.message(sender, pad.getName()+" set public");
                                return true;
                            case "private":
                                pad.setPublic(false);
                                plugin.message(sender, pad.getName()+" set private");
                                return true;
                            case "direction":
                                //TODO: Finish orientation command
                                if (args[3].toLowerCase().equals("n")) {

                                } else if (args[3].toLowerCase().equals("s")) {

                                }
                                return true;
                            case "description":
                                StringBuilder builder = new StringBuilder();
                                for (int i = 3; i < args.length; i++) {
                                    builder.append(args[i]);
                                    if (i < args.length - 1) {
                                        builder.append(" ");
                                    }
                                }
                                if (builder.length() > 0) {
                                    pad.setDescription(builder.toString());
                                    plugin.Meta().saveMeta(pad.getName());
                                }
                                plugin.message(sender, pad.getName()+"'s description set to: "+builder.toString());
                                return true;
                            case "admin":
                                //Remove old pad from config (No save)
                                plugin.Config().removePad(Pad.serialize(pad));
                                //Flush from Manager
                                plugin.Manager().flushPad(pad);
                                //Set new owner
                                pad.setOwnerUUID(Travelpad.ADMIN_UUID);
                                //Add new pad back to manager (Triggers save)
                                plugin.Manager().addPad(pad);
                                return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean publicPadList(CommandSender sender) {

        return true;
    }

    private boolean showHelp(CommandSender sender) {
        sender.sendMessage(Travelpad.PLUGIN_PREFIX_COLOR + "Error in command. Heres some hints!");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [teleport/tp] (SomePadName)");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [identify/i]");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [list/l]");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [name/n] (Name)");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [delete/d] (Name)");
        return true;
    }
}
