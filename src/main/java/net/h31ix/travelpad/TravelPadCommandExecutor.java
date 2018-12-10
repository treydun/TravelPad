package net.h31ix.travelpad;

import com.buildatnight.legacyutils.Format;
import com.buildatnight.unity.Unity;
import net.h31ix.travelpad.api.Pad;
import net.h31ix.travelpad.event.TravelPadTeleportEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class TravelPadCommandExecutor implements CommandExecutor {
    private Travelpad plugin;

    public TravelPadCommandExecutor(Travelpad plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("tavelpad.reload")) {
            plugin.Config().reload();
            return true;
        }
        if (!(sender instanceof Player)) {
            plugin.errorMessage(sender,plugin.Lang().command_deny_console());
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 1 && (args[0].equalsIgnoreCase("identify") || args[0].equalsIgnoreCase("i"))) {
            Pad pad = plugin.Manager().getPadNear(player.getLocation());
            if (pad != null) {
                plugin.message(player,plugin.Lang().identify_found_message() + ChatColor.WHITE + " " + pad.getName());
            } else {
                plugin.errorMessage(player,plugin.Lang().identify_notfound_message());
            }
            return true;
        } else if (args.length >= 1 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("d"))) {
            if (args.length == 1) {
                if (plugin.getPads(player) > 1) {
                    plugin.errorMessage(player,plugin.Lang().delete_deny_multi());
                } else {
                    if (plugin.hasPad(player)) {
                        Object[] pads = plugin.Manager().getPadsFrom(player.getUniqueId()).toArray();
                        plugin.Manager().deletePad((Pad) pads[0]);
                    } else {
                        plugin.errorMessage(player,plugin.Lang().delete_deny_noportal());
                    }
                }
            } else if (args.length == 2) {
                Pad pad = plugin.Manager().getPad(args[1]);
                if (pad != null) {
                    if (!(plugin.Manager().getPad(args[1])).ownerUUID().equals(player.getUniqueId())) {
                        if (player.hasPermission("travelpad.delete.all") || player.hasPermission("travelpad.delete.any")) {
                            plugin.Manager().deletePad(plugin.Manager().getPad(args[1]));
                        } else {
                            plugin.errorMessage(player,plugin.Lang().command_deny_permission());
                        }
                    } else {
                        plugin.Manager().deletePad(plugin.Manager().getPad(args[1]));
                    }
                } else {
                    plugin.errorMessage(player,plugin.Lang().delete_deny_notfound());
                }
            }
            return true;
        } else if (args.length >= 1 && (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("l"))) {
            if (args.length == 1 && player.hasPermission("travelpad.list")) {
                List<Pad> ppads = plugin.Manager().getPadsFrom(player.getUniqueId());
                player.sendMessage(Travelpad.PLUGIN_PREFIX_COLOR + "Your telepads are:");
                for (Pad p : ppads) {
                    TextComponent padName = new TextComponent(" * " + Format.firstLetterCaps(p.getName()));
                    padName.setColor(ChatColor.GREEN);
                    padName.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/t tp " + p.getName()));
                    padName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("Click Me!! To go to " + Format.firstLetterCaps(p.getName()))}));
                    player.spigot().sendMessage(padName);
                }
            } else if (args.length == 2 && player.hasPermission("travelpad.list.others")) {
                OfflinePlayer pl = Bukkit.getPlayer(args[1]);
                if (pl == null) {
                    pl = Bukkit.getOfflinePlayer(Unity.getUnityHandle().getCache().fetchUUID(args[1]));
                }
                List<Pad> ppads = plugin.Manager().getPadsFrom(pl.getUniqueId());
                plugin.message(player,args[1] + "'s " + ChatColor.GREEN + "telepads are:");
                for (Pad p : ppads) {
                    TextComponent padName = new TextComponent(" * " + Format.firstLetterCaps(p.getName()));
                    padName.setColor(ChatColor.GREEN);
                    padName.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/t tp " + p.getName()));
                    padName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{new TextComponent("Click Me!! To go to " + Format.firstLetterCaps(p.getName()))}));
                    player.spigot().sendMessage(padName);
                }
            }
            return true;
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("teleport") || args[0].equalsIgnoreCase("tp"))) {
            if (player.hasPermission("travelpad.teleport") || player.hasPermission("travelpad.tp")) {
                Pad originPad = plugin.Manager().getPadNear(player.getLocation());
                if (originPad != null || player.hasPermission("travelpad.tpanywhere")) {
                    Pad destinationPad = plugin.Manager().getPad(args[1]);
                    if (destinationPad!=null) {
                        if (plugin.canAffordTeleport(player)) {
                            TravelPadTeleportEvent e = new TravelPadTeleportEvent(destinationPad, originPad, player);
                            plugin.getServer().getPluginManager().callEvent(e);
                            if (!e.isCancelled()) {
                                Location loc = e.getTo().getTeleportLocation();
                                plugin.teleport(player, loc);
                            }
                        } else {
                            plugin.errorMessage(player,"Not enough money!");
                        }
                    } else {
                        plugin.errorMessage(player,plugin.Lang().teleport_deny_notfound());
                    }
                } else {
                    plugin.errorMessage(player,plugin.Lang().teleport_deny_loc());
                }
            } else {
                plugin.errorMessage(player,plugin.Lang().command_deny_permission());
            }
            return true;
        } else if (args.length > 1 && (args[0].equalsIgnoreCase("name") || args[0].equalsIgnoreCase("n"))) {
            if (args[1].contains("/")) {
                plugin.errorMessage(player,"Please do not use '/' in the TravelPad name!");
            } else {
                if (plugin.Manager().nameIsValid(args[1])) {
                    String name = args[1];
                    boolean set = plugin.namePad(player, name);
                    if (set) {
                        plugin.message(player,plugin.Lang().name_message() + ChatColor.WHITE + " " + name);
                    } else {
                        plugin.errorMessage(player,plugin.Lang().name_deny_nopad());
                    }
                } else {
                    plugin.errorMessage(player,plugin.Lang().name_deny_inuse());
                }
            }
            return true;
        }
        player.sendMessage(Travelpad.PLUGIN_PREFIX_COLOR + "Error in command. Heres some hints!");
        player.sendMessage(ChatColor.GREEN + " /travelpad [teleport/tp] (SomePadName)");
        player.sendMessage(ChatColor.GREEN + " /travelpad [identify/i]");
        player.sendMessage(ChatColor.GREEN + " /travelpad [list/l]");
        player.sendMessage(ChatColor.GREEN + " /travelpad [name/n] (Name)");
        player.sendMessage(ChatColor.GREEN + " /travelpad [delete/d] (Name)");
        return true;
    }
}
