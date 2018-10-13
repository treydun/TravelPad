package net.h31ix.travelpad;


import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class TravelPadSignListener implements Listener {


	public TravelPadSignListener(Travelpad plugin)
	{	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK)
		{
			Block block = event.getClickedBlock();
			if (block.getType() == Material.getMaterial("SIGN_POST") || block.getType() == Material.getMaterial("WALL_SIGN"))
				if(!event.isCancelled()){
					{
						BlockState bState=block.getState();
						Sign sign=(Sign)bState;
						if (sign.getLine(1).startsWith("/t tp "))
						{
							event.getPlayer().performCommand(sign.getLine(1).substring(1));
						}
					}}
		}
	}
}