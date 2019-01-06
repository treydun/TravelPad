package net.h31ix.travelpad;

import com.buildatnight.travelpad.cl.netgamer.TabText;
import net.h31ix.travelpad.api.Pad;
import net.h31ix.travelpad.event.TravelPadTeleportEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

import static net.h31ix.travelpad.Travelpad.NEWLINE;
import static net.h31ix.travelpad.Travelpad.clickablePad;

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
            plugin.Config().reload();
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
                    plugin.sendLine(sender, Travelpad.PLUGIN_CHAT_HEADER);
                    plugin.sendLine(sender, "Name: " + pad.getName());
                    plugin.sendLine(sender, "Owner: " + pad.ownerName());
                    plugin.sendLine(sender, "Loc: " + Travelpad.formatLocation(pad.getLocation()));
                    plugin.sendLine(sender, "Public: " + pad.isPublic());
                    plugin.sendLine(sender, "Desc: " + pad.getDescription());
                    plugin.sendLine(sender, "Prepaid: "+pad.prepaidsLeft());
                    if (sender.hasPermission("travelpad.info.all")) {
                        plugin.sendLine(sender, "LastUsed: " + pad.getLastUsed());
                        plugin.sendLine(sender, "Weighted Score: " + Pad.weightedScore(pad));
                    }
                } else {
                    plugin.errorMessage(sender, "No permission :(");
                    return true;
                }
            } else {
                plugin.errorMessage(sender, "No find pad :(");
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
                    TextComponent builder = new TextComponent("[Your Pads]");
                    builder.addExtra(NEWLINE);
                    builder.addExtra(fancyList(pads));
                    plugin.message(sender,builder);
                    //player.sendMessage(Travelpad.PLUGIN_PREFIX_COLOR + "Your telepads are:");
                    //for (Pad p : ppads) {
                    //    TextComponent padName = new TextComponent(" * " + Format.firstLetterCaps(p.getName()));
                    //    padName.setColor(ChatColor.GREEN);
                    //    padName.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/t tp " + p.getName()));
                     //   padName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("Click Me!! To go to " + Format.firstLetterCaps(p.getName()))}));
                     //   player.spigot().sendMessage(padName);
                    //}
                } else {
                    plugin.errorMessage(player, plugin.Lang().command_deny_permission());
                }
            } else {
                plugin.errorMessage(sender, plugin.Lang().command_deny_console());
            }
            return true;
        } else if (args.length == 2) {
            switch (args[1].toLowerCase()) {
                case "public":
                case "p":
                    if (sender.hasPermission("travelpad.list.public")) {
                        List<Pad> pads = plugin.Manager().getPublicPads();
                        if (pads != null && !pads.isEmpty()) {
                            TextComponent builder = new TextComponent("[Public Pads]");
                            builder.addExtra(NEWLINE);
                            builder.addExtra(fancyList(pads));
                            //for (Pad pad : pads) {
                            //    builder.addExtra(plugin.clickablePad(pad));
                            //    builder.addExtra(NEWLINE);
                                //sender.sendMessage(Pad.serialize(pad));
                            //}
                            plugin.message(sender, builder);
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
                            TextComponent builder = new TextComponent("[All Pads]");
                            builder.addExtra(NEWLINE);
                            builder.addExtra(fancyList(pads));
                            //for (Pad pad : pads) {
                            //    builder.addExtra(plugin.clickablePad(pad));
                            //    builder.addExtra(NEWLINE);
                                //sender.sendMessage(Pad.serialize(pad));
                            //}
                            plugin.message(sender, builder);
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
                            TextComponent builder = new TextComponent("[Admin Pads]");
                            builder.addExtra(NEWLINE);
                            builder.addExtra(fancyList(pads));
                            plugin.message(sender, builder);
                            //plugin.message(sender, args[1] + "'s " + ChatColor.GREEN + "telepads are:");
                            //TextComponent component = new TextComponent();
                            //for (Pad p : pads) {                                  TODO: Remember first letter caps?
                            //    TextComponent padName = new TextComponent(" * " + Format.firstLetterCaps(p.getName()));
                            //    padName.setColor(ChatColor.GREEN);
                            //    padName.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/t tp " + p.getName()));
                            //    padName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("Click Me!! To go to " + Format.firstLetterCaps(p.getName()))}));
                            //    component.addExtra(padName);
                            //    component.addExtra(NEWLINE);
                            //}
                            //if (sender instanceof Player) {
                            //    ((Player) sender).spigot().sendMessage(component);
                            //} else {
                            //    sender.sendMessage(component.toLegacyText());
                            //}
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
                                TextComponent builder = new TextComponent("[");
                                builder.addExtra(Bukkit.getPlayer(ownerID).getName());
                                builder.addExtra("'s Pads]");
                                builder.addExtra(fancyList(pads));
                                plugin.message(sender,builder);
                                //plugin.message(sender, args[1] + "'s " + ChatColor.GREEN + "telepads are:");
                                //TextComponent component = new TextComponent();
                                //for (Pad p : ppads) {
                                //    TextComponent padName = new TextComponent(" * " + Format.firstLetterCaps(p.getName()));
                                //    padName.setColor(ChatColor.GREEN);
                                //    padName.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/t tp " + p.getName()));
                                //    padName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("Click Me!! To go to " + Format.firstLetterCaps(p.getName()))}));
                                //    component.addExtra(padName);
                                //    component.addExtra(NEWLINE);
                                //}
                                //if (sender instanceof Player) {
                                //    ((Player) sender).spigot().sendMessage(component);
                                //} else {
                                //    sender.sendMessage(component.toLegacyText());
                                //}
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
                    boolean allow = false;
                    Pad originPad = null;
                    if (player.hasPermission("travelpad.teleport.anywhere")) {
                        allow = true;
                    } else if (player.getInventory().contains(plugin.Config().anywhereItem)) {
                        ItemStack anywhereItemstack = new ItemStack(plugin.Config().anywhereItem, 1);
                        player.getInventory().removeItem(anywhereItemstack);
                        allow = true;
                    } else {
                        originPad = plugin.Manager().getPadNear(player.getLocation()); //EXPENSIVE OPERATION
                        if (originPad != null) {
                            allow = true;
                        }
                    }
                    if (allow) {
                        Pad destinationPad = plugin.Manager().getPad(args[1]);
                        if (destinationPad != null) {
                            if (destinationPad.equals(originPad)) {
                                plugin.errorMessage(player, "You are already standing at that pad?");
                                return true;
                            }
                            if (destinationPad.prepaidsLeft()>0 || plugin.canAffordTeleport(player)) {
                                TravelPadTeleportEvent e = new TravelPadTeleportEvent(destinationPad, originPad, player);
                                plugin.getServer().getPluginManager().callEvent(e);
                                if (!e.isCancelled()) {
                                    if (destinationPad.isPublic() && !plugin.Manager().isOwner(sender, destinationPad)) {
                                        destinationPad.setLastUsed();
                                        plugin.Meta().saveMeta(destinationPad.getName());
                                    }
                                    Location loc = e.getTo().getTeleportLocation();
                                    if(destinationPad.chargePrepaid())
                                        player.setMetadata("prepaid", new FixedMetadataValue(plugin, true));
                                    plugin.teleport(player, loc);
                                }
                            } else {
                                plugin.errorMessage(player, plugin.Lang().travel_deny_money());
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
                            pad.setPrepaid(pad.prepaidsLeft()+prepaid);
                            plugin.Meta().saveMeta(pad.getName());
                            plugin.message(sender, pad.getName()+" now  has "+pad.prepaidsLeft()+" teleports prepaid");
                        } else if (plugin.charge(player, prepaid * plugin.Config().teleportAmount)) {
                            pad.setPrepaid(pad.prepaidsLeft()+prepaid);
                            plugin.Meta().saveMeta(pad.getName());
                            plugin.message(sender, pad.getName()+" now  has "+pad.prepaidsLeft()+" teleports prepaid");
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
                            //Remove old pad from config (No save)
                            plugin.Config().removePad(Pad.serialize(pad));
                            //Flush from Manager
                            plugin.Manager().flushPad(pad);
                            //Set new owner
                            pad.setOwnerUUID(Travelpad.ADMIN_UUID);
                            //Add new pad back to manager (Triggers save)
                            plugin.Manager().addPad(pad);
                            plugin.message(sender, pad.getName() + " transferred to admin ownership");
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
    private TextComponent publicPadMainPage = new TextComponent();

    private boolean publicPadList(CommandSender sender) {
        if ((System.currentTimeMillis() - cacheTime) > (1000 * 60 * 5)) {
            Travelpad.log("Refreshing public pad cache");
            publicPadMainPage = new TextComponent();
            publicPadMainPage.addExtra(Travelpad.PLUGIN_HEADER_COMPONENT);
            publicPadMainPage.addExtra(NEWLINE);
            publicPadMainPage.addExtra(" [Top Admin Pads]");
            publicPadMainPage.addExtra(NEWLINE);

            Iterator<Pad> adminPadIterator = plugin.Manager().getPublicAdminPads().iterator();
            int i = 0;
            while (adminPadIterator.hasNext()) {
                if (i >= 3) {
                    break;
                }
                i++;
                Pad pad = adminPadIterator.next();
                BaseComponent component = Travelpad.clickablePad(pad);
                component.addExtra(" ");
                component.addExtra(pad.getDescription());
                publicPadMainPage.addExtra(component);
                publicPadMainPage.addExtra(NEWLINE);
            }
            publicPadMainPage.addExtra(" [Top Player Pads]");
            publicPadMainPage.addExtra(NEWLINE);

            Iterator<Pad> playerPublicPadIterator = plugin.Manager().getPublicPlayerPads().iterator();
            int i2 = 0;
            while (playerPublicPadIterator.hasNext()) {
                if (i2 >= 7) {
                    break;
                }
                i2++;
                Pad pad = playerPublicPadIterator.next();
                BaseComponent component = Travelpad.clickablePad(pad);
                component.addExtra(" ");
                component.addExtra(pad.getDescription());
                publicPadMainPage.addExtra(component);
                publicPadMainPage.addExtra(NEWLINE);
            }
            cacheTime = System.currentTimeMillis();
        }
        plugin.message(sender, publicPadMainPage);
        StringBuilder builder=new StringBuilder();
        //builder.append('\n');
        for(Pad pad:plugin.Manager().getPublicPads()){
            builder.append(pad.getName());
            builder.append('`');
            builder.append(pad.getDescription());
            builder.append('\n');
        }
        TabText tt = new TabText(builder.toString());
        tt.setPageHeight(30);
        tt.setTabs(new int[]{16});
        if(sender instanceof Player){
            plugin.sendLine(sender, tt.getPage(1, false));
        } else {
            plugin.sendLine(sender, tt.getPage(1, true));
        }

        return true;
    }

    private BaseComponent fancyList(List<Pad> pads) {
        /*
        BaseComponent[] padList = fancyList(pads);
        if(padList.length>10){
            //TODO: Paginate
            for (int i=0; i<=10; i++){
                builder.addExtra(padList[i]);
            }
        } else {
            builder.addExtra(new TextComponent(padList));
        }
        */
        BaseComponent[] padsList = new BaseComponent[pads.size()];
        int index = 0;
        for(Iterator<Pad> paderator = pads.iterator(); paderator.hasNext();){
            Pad pad = paderator.next();
            padsList[index] = getFancyLine(pad);
            index++;
            if(index>10){
                //Kickout to prevent overflows
                break;
            }
        }
        return new TextComponent(padsList);
    }

    private BaseComponent getFancyLine(Pad pad){
        ComponentBuilder builder = new ComponentBuilder("");
        builder.append("(");
        builder.color(ChatColor.DARK_GRAY);
        builder.append(clickablePad(pad));
        builder.color(ChatColor.GREEN);
        builder.append(")");
        builder.color(ChatColor.DARK_GRAY);
        if (!pad.getDescription().isEmpty()) {
            //TODO: ADD Padding from TabText
            TextComponent component = new TextComponent("        ");
            component.addExtra(pad.getDescription());
            component.setColor(ChatColor.GRAY);
            builder.append(component);
        }
        builder.append(NEWLINE);
        return new TextComponent(builder.create());
    }

    private boolean showHelp(CommandSender sender) {
        sender.sendMessage(Travelpad.PLUGIN_PREFIX_COLOR + "Error in command. Heres some hints!");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [teleport/tp] (Pad Name)");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [info/i]");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [list/l]");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [name/n] (Pad Name)");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [delete/d] (Pad Name)");
        sender.sendMessage(ChatColor.GREEN + " /travelpad [set/s] (Name) [Public | Description]");
        return true;
    }

    private List<String> previousCompletions = new ArrayList<>();
    private String[] previousArgs = new String[]{};
    private UUID previousSender = UUID.randomUUID();

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        //Only cache players results, console doesnt spam multiple tab events
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (previousSender.equals(player.getUniqueId())) {
                if (args.length == previousArgs.length) {
                    if (Arrays.equals(args, previousArgs)) {
                        Travelpad.log("Cached " + (System.currentTimeMillis() - startTime) + " ms");
                        return previousCompletions;
                    } else if (args[args.length - 1].length() > (previousArgs[previousArgs.length - 1]).length()) {
                        //If growing parameter assume we only need a derivative of the previous results
                        for (String previous : previousCompletions) {
                            if (previous.startsWith(args[args.length - 1])) {
                                completions.add(previous);
                            }
                        }
                        Travelpad.log("SemiCached " + (System.currentTimeMillis() - startTime) + " ms");
                        return completions;
                    }
                }
            }
        }

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
                completions.add("name");
            }
            if ("set".startsWith(args[0])) {
                completions.add("set");
            }
            if ("delete".startsWith(args[0])) {
                completions.add("delete");
            }
            if ("list".startsWith(args[0])) {
                completions.add("list");
            }
            if ("reload".startsWith(args[0])) {
                if (sender.hasPermission("travelpad.reload")) {
                    completions.add("reload");
                }
            }
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "tp":
                    completions.addAll(playersPads(sender, args[1]));
                    completions.addAll(publicPads(args[1]));
                    //completions.add("your recently visited pads"); 3-5 of them? is it even worth it?
                    break;
                case "set":
                    completions.addAll(playersPads(sender, args[1]));
                    if (sender.hasPermission("travelpad.admin")) {
                        //Or all? Seems excessive...
                        completions.addAll(adminPads(args[1]));
                    }
                    break;
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
                case "delete":
                    if (sender.hasPermission("travelpad.delete.any")) {
                        completions.addAll(getAllPads(args[1]));
                    } else if (sender.hasPermission("travelpad.admin")) {
                        completions.addAll(adminPads(args[1]));
                        completions.addAll(playersPads(sender, args[1]));
                    } else {
                        completions.addAll(playersPads(sender, args[1]));
                    }
                    break;
                case "info":
                    if (sender.hasPermission("travelpad.info.any")) {
                        completions.addAll(getAllPads(args[1]));
                    } else if (sender.hasPermission("travelpad.admin")) {
                        completions.addAll(adminPads(args[1]));
                        completions.addAll(playersPads(sender, args[1]));
                    } else {
                        completions.addAll(playersPads(sender, args[1]));
                    }
                    break;
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("set")) {
                Pad pad = plugin.Manager().getPad(args[1]);
                if (pad == null) {
                    return Arrays.asList("Pad Not Found");
                }
                if ("public".startsWith(args[2])) {
                    if (!pad.isPublic())
                        completions.add("public");
                }
                if ("private".startsWith(args[2])) {
                    if (pad.isPublic())
                        completions.add("private");
                }
                if ("description".startsWith(args[2])) {
                    completions.add("description");
                }
                if ("admin".startsWith(args[2])) {
                    if (sender.hasPermission("travelpad.admin")) {
                        completions.add("admin");
                    }
                }
            }
        }
        if (sender instanceof Player) {
            Player player = (Player) sender;
            previousSender = player.getUniqueId();
        }
        previousCompletions = completions;
        previousArgs = args;

        //TODO: remove Debugging Timings
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

        return completions;
    }

    public List<String> playersPads(CommandSender sender, String arg) {
        List<String> playersPads = new ArrayList<>();
        if (sender instanceof Player) {
            Player player = (Player) sender;
            for (Pad p : plugin.Manager().getPadsFrom(player.getUniqueId())) {
                if (!p.isPublic() && p.getName().toLowerCase().startsWith(arg)) {
                    playersPads.add(p.getName());
                }
            }
        }
        return playersPads;
    }

    public List<String> publicPads(String arg) {
        List<String> publicPads = new ArrayList<>();
        for (Pad pad : plugin.Manager().getPublicPads()) {
            if (pad.getName().startsWith(arg)) {
                publicPads.add(pad.getName());
            }
        }
        return publicPads;
    }

    public List<String> adminPads(String arg) {
        List<String> adminPads = new ArrayList<>();
        for (Pad pad : plugin.Manager().getPadsFrom(Travelpad.ADMIN_UUID)) {
            if (pad.getName().startsWith(arg)) {
                adminPads.add(pad.getName());
            }
        }
        return adminPads;
    }

    public List<String> getPlayersByName(String arg) {
        List<String> playersNames = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            //A possibly more efficient method from https://stackoverflow.com/questions/19154117/startswith-method-of-string-ignoring-case#comment69214810_19154117
            //if(p.getName().regionMatches(true, 0, arg, 0, arg.length())) {
            if (p.getName().startsWith(arg)) {
                playersNames.add(p.getName());
            }
        }
        return playersNames;
    }

    public List<String> getAllPads(String arg) {
        List<String> allPads = new ArrayList<>();
        for (Pad pad : plugin.Manager().getPads()) {
            if (pad.getName().startsWith(arg)) {
                allPads.add(pad.getName());
            }
        }
        return allPads;
    }
}
