package net.h31ix.travelpad;

import com.buildatnight.travelpad.Statistics;
import net.h31ix.travelpad.api.Pad;
import net.h31ix.travelpad.event.TravelPadTeleportEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

import static net.h31ix.travelpad.Travelpad.*;

public class TravelPadCommandExecutor implements TabExecutor {
    private Travelpad plugin;

    public TravelPadCommandExecutor(Travelpad plugin) {
        this.plugin = plugin;
    }

    /**
     * Comments: Switched method to case based, They dont NEED to return, i may remove that to make it a cleaner read
     * Not going to depend on spigots help context..
     *
     * @param sender Console or In game Player
     * @param cmd    The command executed
     * @param alias  The alias used if an alias was used
     * @param args   Parameters of the command
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
                case "info":
                case "i":
                    return info(sender, args);
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
            plugin.reload();
            cacheTime = 0;
        } else {
            plugin.errorMessage(sender, plugin.Lang().command_deny_permission());
        }
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length == 1) {
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
        } else if (args.length == 2) {
            Pad pad = plugin.Manager().getPad(args[1]);
            if (pad != null) {
                //TODO: TextComponentize
                if (sender.hasPermission("travelpad.info.others") || plugin.Manager().isOwner(sender, pad)) {
                    ComponentBuilder builder = new ComponentBuilder(plugin.getHeader().duplicate());
                    TextComponent name = new TextComponent("\nName: ");
                    TextComponent padName = new TextComponent(pad.getName());
                    padName.setColor(ChatColor.GREEN);
                    name.addExtra(padName);
                    builder.append(name);

                    TextComponent owner = new TextComponent("\nOwner: ");
                    TextComponent ownerName = new TextComponent(pad.ownerName());
                    ownerName.setColor(ChatColor.GREEN);
                    owner.addExtra(ownerName);
                    builder.append(owner);

                    TextComponent location = new TextComponent("\nLocation: ");
                    TextComponent locationName = new TextComponent(locationString(pad.getLocation()));
                    locationName.setColor(ChatColor.GREEN);
                    location.addExtra(locationName);
                    builder.append(location);

                    if (!pad.getDescription().isEmpty()) {
                        TextComponent description = new TextComponent("\nDescription: ");
                        TextComponent descriptionText = new TextComponent(pad.getDescription());
                        descriptionText.setColor(ChatColor.GRAY);
                        description.addExtra(descriptionText);
                        builder.append(description);
                    }
                    if (pad.isPublic()) {
                        BaseComponent line = NEWLINE_INDENT.duplicate();
                        TextComponent isPublic = new TextComponent("Public!");
                        isPublic.setColor(ChatColor.GREEN);
                        line.addExtra(isPublic);
                        builder.append(line);
                    }
                    if (pad.prepaidsLeft() > 0) {
                        BaseComponent line = NEWLINE_INDENT.duplicate();
                        TextComponent isPrepaid = new TextComponent("Prepaid!");
                        isPrepaid.setColor(ChatColor.GREEN);
                        line.addExtra(isPrepaid);
                        builder.append(line);
                    }
                    plugin.message(sender, new TextComponent(builder.create()));
                    /*
                    BaseComponent component = plugin.getHeader().duplicate();
                    component.addExtra(new TextComponent("\nName: "));
                    TextComponent name = new TextComponent(pad.getName());
                    name.setColor(ChatColor.GREEN);
                    component.addExtra("\nOwner: ");
                    TextComponent owner = new TextComponent(pad.ownerName());
                    owner.setColor(ChatColor.GREEN);
                    component.addExtra(owner);
                    component.addExtra("\nLoc: ");
                    TextComponent location = new TextComponent(Travelpad.formatLocation(pad.getLocation()));
                    location.setColor(ChatColor.GREEN);
                    component.addExtra(location);
                    //plugin.message(sender, plugin.getHeader());
                    //plugin.sendLine(sender, "Name: " + pad.getName());
                    //plugin.sendLine(sender, "Owner: " + pad.ownerName());
                    //plugin.sendLine(sender, "Loc: " + Travelpad.formatLocation(pad.getLocation()));
                    if (pad.isPublic())

                        //plugin.sendLine(sender, "Public: " + pad.isPublic());
                    if (!pad.getDescription().isEmpty())
                        plugin.sendLine(sender, "Desc: " + pad.getDescription());
                    if (pad.prepaidsLeft() > 0)
                        plugin.sendLine(sender, "Prepaid: " + pad.prepaidsLeft());
                    if (sender.hasPermission("travelpad.info.all")) {
                        plugin.sendLine(sender, "LastUsed: " + pad.getLastUsed());
                        plugin.sendLine(sender, "Weighted Score: " + pad.getWeight());
                    }
                    */
                } else {
                    plugin.errorMessage(sender, plugin.Lang().command_deny_permission());
                    return true;
                }
            } else {
                plugin.errorMessage(sender, plugin.Lang().identify_notfound_message());
            }
        } else {
            plugin.errorMessage(sender, "Invalid number of parameters");
        }
        return true;
    }

    private boolean delete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            //TODO: Consider getPadNear() but more dangerous
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (plugin.padsPlayerHas(player) > 1) {
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
                    List<Pad> pads = plugin.Manager().getPadsFrom(player.getUniqueId());
                    if (pads != null && pads.size() > 0) {
                        BaseComponent[] padList = new BaseComponent[pads.size() + 1];
                        padList[0] = new TextComponent("[Your pads]");
                        int offset = 1;
                        for (Pad pad : pads) {
                            padList[offset] = getFancyLine(pad);
                            offset++;
                        }
                        plugin.sendPagination(sender, padList, "Your Pad List");
                    } else {
                        plugin.message(player, plugin.Lang().list_no_pads()+ player.getName());
                    }
                } else {
                    plugin.errorMessage(player, plugin.Lang().command_deny_permission());
                }
            } else {
                plugin.errorMessage(sender, plugin.Lang().command_deny_console());
            }
            return true;
        } else if (args.length >= 2) {
            switch (args[1].toLowerCase()) {
                case "public":
                case "p":
                    if (sender.hasPermission("travelpad.list.public")) {
                        List<Pad> publicPads = null;
                        if (args.length == 2) {
                            publicPads = plugin.Manager().getPublicPads();
                        } else if (args.length >= 3) {
                            if ("sortby".equalsIgnoreCase(args[2]))
                                switch (args[3].toLowerCase()) {
                                    case "name":
                                        publicPads = plugin.Manager().getPublicPads()
                                                .stream()
                                                .sorted(Pad.byName)
                                                .collect(Collectors.toList());
                                        break;
                                    case "namereversed":
                                        publicPads = plugin.Manager().getPublicPads()
                                                .stream()
                                                .sorted(Pad.byName.reversed())
                                                .collect(Collectors.toList());
                                        break;
                                    case "lastused":
                                        publicPads = plugin.Manager().getPublicPads()
                                                .stream()
                                                .sorted(Pad.byLastUsed)
                                                .collect(Collectors.toList());
                                        break;
                                    case "mostused":
                                        publicPads = plugin.Manager().getPublicPads()
                                                .stream()
                                                .sorted(Pad.byMostUsed)
                                                .collect(Collectors.toList());
                                        break;
                                }
                        }
                        if (publicPads != null && !publicPads.isEmpty()) {
                            BaseComponent[] padList = new BaseComponent[publicPads.size() + 1];
                            padList[0] = new TextComponent("[Public Pads]");
                            int offset = 1;
                            for (Pad pad : publicPads) {
                                padList[offset] = getFancyLine(pad);
                                offset++;
                            }
                            plugin.sendPagination(sender, padList, "Public Pad List");
                        } else {
                            sender.sendMessage("Unable to find any pads");
                        }
                    } else {
                        plugin.errorMessage(sender, plugin.Lang().command_deny_permission());
                    }
                    return true;
                case "all":
                    if (sender.hasPermission("travelpad.list.all")) {
                        List<Pad> pads = plugin.Manager().getPads();
                        if (pads != null && !pads.isEmpty()) {
                            BaseComponent[] padList = new BaseComponent[pads.size() + 1];
                            padList[0] = new TextComponent("[All Pads]");
                            int offset = 1;
                            for (Pad pad : pads) {
                                padList[offset] = getFancyLine(pad);
                                offset++;
                            }
                            plugin.sendPagination(sender, padList, "All Pad List");
                        } else {
                            sender.sendMessage("Unable to find any pads");
                        }
                    } else {
                        plugin.errorMessage(sender, plugin.Lang().command_deny_permission());
                    }
                    return true;
                case "admin":
                    if (sender.hasPermission("travelpad.list.admin")) {
                        List<Pad> pads = plugin.Manager().getPadsFrom(Travelpad.ADMIN_UUID);
                        if (pads != null && !pads.isEmpty()) {
                            BaseComponent[] padList = new BaseComponent[pads.size() + 1];
                            padList[0] = new TextComponent("[Admin Pads]");
                            int offset = 1;
                            for (Pad pad : pads) {
                                padList[offset] = getFancyLine(pad);
                                offset++;
                            }
                            plugin.sendPagination(sender, padList, "Admin Pad List");
                        } else {
                            plugin.errorMessage(sender, plugin.Lang().list_no_pads());
                        }
                    } else {
                        plugin.errorMessage(sender, plugin.Lang().command_deny_permission());
                    }
                    return true;
                default:
                    if (sender.hasPermission("travelpad.list.others")) {
                        UUID ownerID = plugin.getPlayerUUIDbyName(args[1]);
                        if (ownerID != null) {
                            List<Pad> pads = plugin.Manager().getPadsFrom(ownerID);
                            if (pads != null && !pads.isEmpty()) {
                                BaseComponent[] padList = new BaseComponent[pads.size() + 1];
                                TextComponent builder = new TextComponent("[");
                                builder.addExtra(args[1]);
                                builder.addExtra("'s Pads]");
                                padList[0] = builder;
                                int offset = 1;
                                for (Pad pad : pads) {
                                    padList[offset] = getFancyLine(pad);
                                    offset++;
                                }
                                plugin.sendPagination(sender, padList, args[1] + "'s pads");
                            } else {
                                plugin.errorMessage(sender, plugin.Lang().list_no_pads() + args[1]);
                            }
                        } else {
                            plugin.errorMessage(sender, plugin.Lang().list_no_player() + args[1]);
                        }
                    } else {
                        plugin.errorMessage(sender, plugin.Lang().command_deny_permission());
                    }
            }
            return true;
        } else {
            plugin.errorMessage(sender, "Incorrect number of parameters");
        }
        return false;
    }

    private boolean name(CommandSender sender, String[] args) {
        //0 = name 1 = padname
        if (sender instanceof Player) {
            if (args.length == 1) {
                plugin.message(sender, "Usage: /travelpad name (NameOfPad)");
                return true;
            } else if (args.length == 2) {
                Player player = (Player) sender;
                if (args[1].contains("/")) {
                    plugin.errorMessage(player, "Please do not use '/' in the TravelPad name!");
                } else if (args[1].length() >= 16) {
                    plugin.errorMessage(player, "Please limit pad names to 16 characters or less");
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
                plugin.errorMessage(sender, "Incorrect number of arguments");
                return showHelp(sender);
            }
        } else {
            plugin.errorMessage(sender, plugin.Lang().command_deny_console());
        }
        return true;
    }

    private boolean teleport(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 2) {
                if (player.hasPermission("travelpad.teleport")) {
                    Pad destinationPad = plugin.Manager().getPad(args[1]);
                    if (null == destinationPad) {
                        plugin.errorMessage(player, plugin.Lang().teleport_deny_notfound());
                        return true;
                    }
                    Pad originPad = null;
                    if (!player.hasPermission("travelpad.teleport.anywhere")) {
                        if (!player.getInventory().contains(plugin.Config().anywhereItem)) {
                            originPad = plugin.Manager().getPadNear(player.getLocation()); //EXPENSIVE OPERATION
                            if (null == originPad) {
                                plugin.errorMessage(player, plugin.Lang().teleport_deny_loc());
                                return true;
                            }
                        } else {
                            Statistics.tickStat("MagmaCreamPaid");
                            ItemStack anywhereItemstack = new ItemStack(plugin.Config().anywhereItem, 1);
                            player.getInventory().removeItem(anywhereItemstack);
                            player.sendMessage("Charged 1x Magma Cream to teleport from 'anywhere'");
                        }
                    }
                    if (destinationPad.equals(originPad)) {
                        plugin.errorMessage(player, "You are already standing at that pad?");
                        return true;
                    }

                    if (destinationPad.prepaidsLeft() > 0 || plugin.canAffordTeleport(player)) {
                        TravelPadTeleportEvent e = new TravelPadTeleportEvent(destinationPad, originPad, player);
                        plugin.getServer().getPluginManager().callEvent(e);
                        if (!e.isCancelled()) {
                            if (!player.hasPermission("travelpad.teleport.free")) {
                                if (!destinationPad.chargePrepaid()) {
                                    if (plugin.Config().requireItem || plugin.Config().takeItem) {
                                        ItemStack itemToTake = new ItemStack(plugin.Config().itemType, 1);
                                        //If item is required, legacy setting compatibility.
                                        if (plugin.Config().requireItem && player.getInventory().contains(plugin.Config().itemType, 1)) {
                                            if (plugin.Config().takeItem) {
                                                player.getInventory().removeItem(itemToTake);
                                                plugin.message(player, plugin.Lang().travel_approve_item().replace("%item%", ChatColor.GREEN + itemToTake.getType().name().replaceAll("_", " ") + ChatColor.GRAY));
                                                //player.sendMessage(ChatColor.GOLD + itemToTake.getType().name().toLowerCase().replaceAll("_", " ") + " " + l.travel_approve_item());
                                            } else {
                                                plugin.message(player, "You used %item% to teleport");
                                            }
                                        } else if (plugin.Config().requireItem) {
                                            plugin.errorMessage(player, plugin.Lang().travel_deny_item() + " " + itemToTake.getType().name().toLowerCase().replaceAll("_", " "));
                                            return true;
                                        }

                                        //If item is not required but can be taken due to setting (Our new hybrid mode)
                                        if (!plugin.Config().requireItem && (plugin.Config().chargeTeleport || plugin.Config().takeItem)) {
                                            if (player.getInventory().contains(plugin.Config().itemType, 1)) {
                                                Statistics.tickStat("EnderEyeCharge");
                                                player.getInventory().removeItem(itemToTake);
                                                //TODO: fix this ugly message a bit more
                                                plugin.message(player, plugin.Lang().travel_approve_item().replace("%item%", ChatColor.GREEN + "(1) " + itemToTake.getType().name().toLowerCase().replaceAll("_", " ") + ChatColor.GRAY));
                                            } else {
                                                if (plugin.Config().chargeTeleport) {
                                                    if (!plugin.charge(player, plugin.Config().teleportAmount)) {
                                                        plugin.errorMessage(player, plugin.Lang().travel_deny_money());
                                                        return true;
                                                    } else {
                                                        Statistics.tickStat("CashPayment");
                                                        player.sendMessage(ChatColor.GOLD + plugin.Lang().charge_message() + " " + plugin.Config().teleportAmount);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    player.sendMessage("TP'd for free!");
                                    Statistics.tickStat("Prepaid" + destinationPad.getName());
                                }
                            } // Had free teleport permission

                            //Pull location to teleport from pads 'location + 1.25'
                            final Location destination = e.getTo().getTeleportLocation();
                            if (!plugin.Manager().isSafe(e.getTo().getLocation(), player)) {
                                plugin.errorMessage(player, plugin.Lang().travel_unsafe());
                                return true;
                            }

                            if (destinationPad.isPublic() && !plugin.Manager().isOwner(sender, destinationPad)) {
                                destinationPad.setLastUsed();
                                plugin.Meta().saveMeta(destinationPad.getName());
                            }

                            final World world = player.getWorld();
                            final Location playerLocation = player.getLocation();
                            world.playSound(playerLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0);
                            world.playEffect(playerLocation, Effect.ENDER_SIGNAL, Effect.ENDER_SIGNAL.getData());
                            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                                @Override
                                public void run() {
                                    destination.getChunk().load();
                                }
                            }, 1);

                            //Delay teleportation to allow the server time to load the chunk.
                            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                                @Override
                                public void run() {
                                    if (player.isOnline()) {
                                        player.teleport(destination, PlayerTeleportEvent.TeleportCause.PLUGIN);
                                        player.sendMessage(ChatColor.GREEN + plugin.Lang().travel_message());
                                        for (int i = 0; i != 32; i++) {
                                            player.getWorld().playEffect(destination.add(plugin.getRandom(), plugin.getRandom(), plugin.getRandom()), Effect.SMOKE, 3);
                                        }
                                    }
                                }
                            }, 3);
                        } else {
                            plugin.errorMessage(player, "A plugin is blocking this Travelpad from being accessed");
                        }
                    } else {
                        plugin.errorMessage(player, plugin.Lang().travel_deny_money());
                    }
                } else {
                    plugin.errorMessage(player, plugin.Lang().command_deny_permission());
                }
            } else {
                showHelp(sender);
            }
        } else {
            plugin.errorMessage(sender, plugin.Lang().command_deny_console());
        }
        return true;
    }

    private boolean prepay(CommandSender sender, String[] args) {
        if (true) {
            sender.sendMessage(Travelpad.PLUGIN_PREFIX_COLOR + "This feature isnt implemented yet");
            return true;
        }
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
                            pad.setPrepaid(pad.prepaidsLeft() + prepaid);
                            plugin.Meta().saveMeta(pad.getName());
                            plugin.message(sender, pad.getName() + " now  has " + pad.prepaidsLeft() + " teleports prepaid");
                        } else if (plugin.charge(player, prepaid * plugin.Config().teleportAmount)) {
                            pad.setPrepaid(pad.prepaidsLeft() + prepaid);
                            plugin.Meta().saveMeta(pad.getName());
                            plugin.message(sender, pad.getName() + " now  has " + pad.prepaidsLeft() + " teleports prepaid");
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
            return true;
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
                if (plugin.Manager().isOwner(sender, pad) || sender.hasPermission("travelpad.admin")) {
                    switch (args[2].toLowerCase()) {
                        case "public":
                        case "pub":
                            plugin.Manager().setPublic(pad, true);
                            plugin.message(sender, pad.getName() + " set public");
                            return true;
                        case "private":
                        case "priv":
                            plugin.Manager().setPublic(pad, false);
                            plugin.message(sender, pad.getName() + " set private");
                            return true;
                        case "direction":
                        case "dir":
                            plugin.Manager().setDirection(pad, args[3].toLowerCase());
                            if (pad.getLocation().getYaw() == 0.0) {
                                plugin.message(sender, pad.getName() + " set to South");
                            } else if (pad.getLocation().getYaw() == 90.0) {
                                plugin.message(sender, pad.getName() + " set to West");
                            } else if (pad.getLocation().getYaw() == 180.0) {
                                plugin.message(sender, pad.getName() + " set to North");
                            } else if (pad.getLocation().getYaw() == -90.0) {
                                plugin.message(sender, pad.getName() + " set to East");
                            }
                            return true;
                        case "description":
                        case "desc":
                            StringBuilder builder = new StringBuilder();
                            for (int i = 3; i < args.length; i++) {
                                builder.append(args[i]);
                                if (i < args.length - 1) {
                                    builder.append(" ");
                                }
                            }
                            //if (builder.length() > 0) { //Need way to null out description, this is it
                            pad.setDescription(builder.toString());
                            plugin.Meta().saveMeta(pad.getName());
                            //}
                            plugin.message(sender, pad.getName() + "'s description set to: " + builder.toString());
                            return true;
                        case "admin":
                            plugin.message(sender, pad.getName() + " transferred to admin ownership");
                            plugin.refund(pad.ownerUUID());
                            //Remove old pad from config (No save)
                            plugin.Config().removePad(Pad.serialize(pad));
                            //Flush from Manager
                            plugin.Manager().flushPad(pad);
                            //Set new owner
                            pad.setOwnerUUID(Travelpad.ADMIN_UUID);
                            pad.setOwnerName("Admin");
                            //Add new pad back to manager (Triggers save)
                            plugin.Manager().addPad(pad);
                            return true;
                        default:
                            plugin.errorMessage(sender, "Failed to match " + args[2].toLowerCase() + " to any set options");
                            plugin.message(sender, "Hint: /travelpad set (PadName) [public/private/description/direction]");
                            break;
                    }
                } else {
                    plugin.errorMessage(sender, plugin.Lang().command_deny_permission());
                }
            } else {
                plugin.errorMessage(sender, "Unable to find a pad by that name");
            }

        } else {
            plugin.errorMessage(sender, "Wrong number of parameters");
        }
        return true;
    }

    //This is not to sort, this is only to cache the already sorted shorter data for the main screen
    private long cacheTime = 0;
    private BaseComponent[] publicPage = new BaseComponent[18];

    private boolean publicPadList(CommandSender sender) {
        if ((System.currentTimeMillis() - cacheTime) > (1000 * 60 * 5)) {
            Travelpad.log("Refreshing public pad cache");
            publicPage = new BaseComponent[18];
            publicPage[0] = new TextComponent("[Top Admin Pads]");
            //TODO: FINISH BUILDING MESSAGE (Sort options, better looking, etc)

            Iterator<Pad> adminPadIterator = plugin.Manager().getPublicAdminPads().iterator();
            int i = 1;
            while (adminPadIterator.hasNext()) {
                if (i > 5) {
                    break;
                }
                Pad pad = adminPadIterator.next();
                publicPage[i] = getFancyLine(pad);
                i++;
            }
            publicPage[i] = new TextComponent("[Top Player Pads]");
            i++;
            Iterator<Pad> playerPublicPadIterator = plugin.Manager().getPublicPlayerPads().iterator();
            int indexPlusLines = i + 10;
            while (playerPublicPadIterator.hasNext()) {
                if (i > indexPlusLines) {
                    break;
                }
                Pad pad = playerPublicPadIterator.next();
                publicPage[i] = getFancyLine(pad);
                i++;
            }
            cacheTime = System.currentTimeMillis();
        }
        plugin.sendPagination(sender, publicPage, "Top Public Pads");
        /*
        StringBuilder builder = new StringBuilder();
        for (Pad pad : plugin.Manager().getPublicPads()) {
            builder.append(pad.getName());
            builder.append('`');
            builder.append(pad.getDescription());
            builder.append('\n');
        }
        TabText tt = new TabText(builder.toString());
        tt.setPageHeight(30);
        tt.setTabs(new int[]{16});
        if (sender instanceof Player) {
            plugin.sendLine(sender, tt.getPage(1, false));
        } else {
            plugin.sendLine(sender, tt.getPage(1, true));
        }
        */
        return true;
    }

    /* Sorted FancyLists cache. Will save a lot of resorting */
//TODO: Finish the rest of the sorting options
    private long lastCached = 0;
    private BaseComponent[] sortByNameCache;
    private BaseComponent[] sortByNameReversedCache;
    private BaseComponent[] sortByWeightCache;
    private BaseComponent[] sortByWeightReversedCache;

    private BaseComponent[] fancyList(List<Pad> pads) {
        BaseComponent[] padsList = new BaseComponent[pads.size()];
        int index = 0;
        for (Iterator<Pad> paderator = pads.iterator(); paderator.hasNext(); ) {
            Pad pad = paderator.next();
            padsList[index] = getFancyLine(pad);
            index++;
        }
        return padsList;
    }

    private BaseComponent getFancyLine(Pad pad) {
        TextComponent listSymbol = new TextComponent(" > ");
        listSymbol.setColor(ChatColor.DARK_GRAY);
        BaseComponent padName = clickablePad(pad);
        listSymbol.addExtra(padName);
        if (!pad.getDescription().isEmpty()) {
            //TODO: Added overflow truncation here BUT it is not 'character width' aware and so its like using a hammer on a screw. Fix. Use TabText method
            int predescriptionLength = 3 + pad.getName().length() + 3;
            String description = (pad.getDescription().length() + predescriptionLength > 55) ? pad.getDescription().substring(0, 55 - predescriptionLength) : pad.getDescription();

            //TODO: ADD Padding from TabText instead of dash
            TextComponent component = new TextComponent(" - ");
            component.setColor(ChatColor.DARK_GRAY);
            component.addExtra(description);
            component.setColor(ChatColor.GRAY);
            listSymbol.addExtra(component);
        }
        return new TextComponent(listSymbol);
    }

    private boolean showHelp(CommandSender sender) {
        sender.sendMessage(Travelpad.PLUGIN_PREFIX_COLOR + "Error in command. Heres some hints!");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [teleport/tp] (Pad Name)");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [info/i]");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [list/l]");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [name/n] (Pad Name)");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [delete/d] (Pad Name)");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [set/s] (Pad Name) [Public | Description]");
        return true;
    }

    /**
     * TAB COMPLETION EXECUTOR
     */
    private List<String> previousCompletions = new ArrayList<>();
    private String[] previousArgs = new String[]{};
    private UUID previousSender = UUID.randomUUID();

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        //log("args.length=" + args.length + " " + Arrays.toString(args));
        List<String> completions = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        //Only cache players results, console doesnt spam multiple tab events
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (previousSender.equals(player.getUniqueId())) {
                if (args.length == previousArgs.length) {
                    if (Arrays.equals(args, previousArgs)) {
                        //Travelpad.log("Cached " + (System.currentTimeMillis() - startTime) + " ms");
                        return previousCompletions;
                    } else if (args[args.length - 1].length() > (previousArgs[previousArgs.length - 1]).length()) {
                        //If growing parameter assume we only need a derivative of the previous results
                        for (String previous : previousCompletions) {
                            if (previous.startsWith(args[args.length - 1])) {
                                completions.add(previous);
                            }
                        }
                        //Travelpad.log("SemiCached " + (System.currentTimeMillis() - startTime) + " ms");
                        return completions;
                    }
                }
            }
        }

        //Normalize all the parameters
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].toLowerCase();
        }

        if (args.length == 1) {
            if ("tp".startsWith(args[0])) {
                completions.add("tp");
            }
            if ("help".startsWith(args[0])) {
                completions.add("help");
            }
            if ("info".startsWith(args[0])) {
                completions.add("info");
            }
            if ("name".startsWith(args[0])) {
                if (sender.hasPermission("travelpad.create"))
                    completions.add("name");
            }
            if ("set".startsWith(args[0])) {
                if (sender.hasPermission("travelpad.set"))
                    completions.add("set");
            }
            if ("delete".startsWith(args[0])) {
                if (sender.hasPermission("travelpad.delete"))
                    completions.add("delete");
            }
            if ("list".startsWith(args[0])) {
                if (sender.hasPermission("travelpad.list"))
                    completions.add("list");
            }
            if ("reload".startsWith(args[0])) {
                if (sender.hasPermission("travelpad.reload")) {
                    completions.add("reload");
                }
            }
        }

        if (args.length >= 2) {
            switch (args[0].toLowerCase()) {
                case "t":
                case "tp":
                    completions.addAll(playersPads(sender, args[1], false));
                    completions.addAll(publicPads(args[1]));
                    //completions.add("your recently visited pads"); 3-5 of them? is it even worth it?
                    break;
                case "s":
                case "set":
                    if (args.length >= 3) {
                        Pad pad = plugin.Manager().getPad(args[1]);
                        if (pad == null) {
                            return Arrays.asList("Pad Not Found");
                        }
                        if ("public".startsWith(args[2])) {
                            if (args.length == 3)
                                if (!pad.isPublic())
                                    completions.add("public");
                        }
                        if ("private".startsWith(args[2])) {
                            if (args.length == 3)
                                if (pad.isPublic())
                                    completions.add("private");
                        }
                        if ("description".startsWith(args[2])) {
                            if (args.length == 3)
                                completions.add("description");
                        }
                        if ("direction".startsWith(args[2])) {
                            if (args.length >= 4) {
                                if ("north".startsWith(args[3])) {
                                    completions.add("north");
                                }
                                if ("south".startsWith(args[3])) {
                                    completions.add("south");
                                }
                                if ("east".startsWith(args[3])) {
                                    completions.add("east");
                                }
                                if ("west".startsWith(args[3])) {
                                    completions.add("west");
                                }
                            } else {
                                completions.add("direction");
                            }
                        }
                        if ("admin".startsWith(args[2])) {
                            if (sender.hasPermission("travelpad.admin")) {
                                if (args.length == 3)
                                    completions.add("admin");
                            }
                        }
                    } else {
                        completions.addAll(playersPads(sender, args[1], true));
                        if (sender.hasPermission("travelpad.admin")) {
                            //Or all? Seems excessive...
                            completions.addAll(adminPads(args[1]));
                        }
                    }
                    break;
                case "l":
                case "list":
                    if ("public".startsWith(args[1])) {
                        completions.add("public");
                    }
                    if ("all".startsWith(args[1])) {
                        if (sender.hasPermission("travelpad.list.all")) {
                            completions.add("all");
                        }
                    }
                    if ("admin".startsWith(args[1])) {
                        if (sender.hasPermission("travelpad.list.admin")) {
                            completions.add("admin");
                        }
                    }
                    if (sender.hasPermission("travelpad.list.others")) {
                        completions.addAll(getPlayersByName(args[1]));
                    }
                    break;
                case "d":
                case "delete":
                    if (sender.hasPermission("travelpad.delete.any")) {
                        completions.addAll(getAllPads(args[1]));
                    } else if (sender.hasPermission("travelpad.admin")) {
                        completions.addAll(adminPads(args[1]));
                        completions.addAll(playersPads(sender, args[1], true));
                    } else {
                        completions.addAll(playersPads(sender, args[1], true));
                    }
                    break;
                case "i":
                case "info":
                    if (sender.hasPermission("travelpad.info.any")) {
                        //TODO: !! This is potentially a very heavy operation, consider removing it...
                        completions.addAll(getAllPads(args[1]));
                    } else if (sender.hasPermission("travelpad.admin")) {
                        completions.addAll(adminPads(args[1]));
                        completions.addAll(playersPads(sender, args[1], true));
                    } else {
                        completions.addAll(playersPads(sender, args[1], true));
                    }
                    break;
            }
        }
        if (sender instanceof Player) {
            Player player = (Player) sender;
            previousSender = player.getUniqueId();
        }
        previousCompletions = completions;
        previousArgs = args;

        /*
        StringBuilder builder = new StringBuilder();
        //builder.append(cmd.getName());
        //builder.append(" ");
        //builder.append(label);
        builder.append("args:");
        for (int i = 0; i < args.length; i++) {
            builder.append(i);
            builder.append('[');
            builder.append(args[i]);
            builder.append(']');
            builder.append(" ");
        }
        Travelpad.log("UnCached " + (System.currentTimeMillis() - startTime) + " ms " + builder.toString());
        */
        return completions;
    }

    /**
     * Returns a list of all pads a player owns (Minus any public ones which are already used in publicPads)
     * The problem with this method is it ignores public pads, which shouldnt come up in set... only pads a player controls should
     *
     * @param sender the sender of the command, needs to always be a player to get the UUID else just returns empty
     * @param arg    starts with text to compare against the pad names
     * @return list of pad names a player owns minus any public ones
     */
    public List<String> playersPads(CommandSender sender, String arg, boolean includePublic) {
        List<String> playersPads = new ArrayList<>();
        UUID senderUUID = ADMIN_UUID;
        //Defaults to admin pads unless sender is in game player
        if (sender instanceof Player) {
            senderUUID = ((Player) sender).getUniqueId();
        }
        List<Pad> pads = plugin.Manager().getPadsFrom(senderUUID);
        if (pads != null && pads.size() > 0) {
            if (includePublic) {
                for (Pad pad : pads) {
                    if (pad.getName().toLowerCase().startsWith(arg.toLowerCase())) {
                        playersPads.add(pad.getName());
                    }
                }
            } else {
                for (Pad pad : pads) {
                    if (!pad.isPublic() && pad.getName().toLowerCase().startsWith(arg)) {
                        playersPads.add(pad.getName());
                    }
                }
            }
        }
        return playersPads;
    }

    /**
     * Returns a list of all public pad names that start with arg
     *
     * @param arg String to check padnames against
     * @return list of matching string names or empty list
     */
    public List<String> publicPads(String arg) {
        List<String> publicPadNames = new ArrayList<>();
        List<Pad> publicPads = plugin.Manager().getPublicPads();
        if (publicPads != null && !publicPads.isEmpty()) {
            for (Pad pad : publicPads) {
                if (pad.getName().toLowerCase().startsWith(arg.toLowerCase())) {
                    publicPadNames.add(pad.getName());
                }
            }
        }
        return publicPadNames;
    }

    /**
     * Returns a list of all admin pad names that start with arg
     *
     * @param arg String to check padnames against
     * @return list of matching string names or empty list
     */
    public List<String> adminPads(String arg) {
        List<String> adminPadNames = new ArrayList<>();
        List<Pad> adminPads = plugin.Manager().getPadsFrom(Travelpad.ADMIN_UUID);
        if (adminPads != null && !adminPads.isEmpty()) {
            for (Pad pad : adminPads) {
                if (pad.getName().toLowerCase().startsWith(arg.toLowerCase())) {
                    adminPadNames.add(pad.getName());
                }
            }
        }
        return adminPadNames;
    }

    public List<String> getPlayersByName(String arg) {
        List<String> playersNames = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            //A possibly more efficient method from https://stackoverflow.com/questions/19154117/startswith-method-of-string-ignoring-case#comment69214810_19154117
            //if(p.getName().regionMatches(true, 0, arg, 0, arg.length())) {
            if (p.getName().toLowerCase().startsWith(arg.toLowerCase())) {
                playersNames.add(p.getName());
            }
        }
        return playersNames;
    }

    /**
     * Returns a list of all pad names that start with arg
     *
     * @param arg String to check padnames against
     * @return list of matching string names or empty list
     */
    public List<String> getAllPads(String arg) {
        List<String> allPadNames = new ArrayList<>();
        List<Pad> allPads = plugin.Manager().getPads();
        if (allPads != null && !allPads.isEmpty()) {
            for (Pad pad : allPads) {
                if (pad.getName().toLowerCase().startsWith(arg.toLowerCase())) {
                    allPadNames.add(pad.getName());
                }
            }
        }
        return allPadNames;
    }
}
