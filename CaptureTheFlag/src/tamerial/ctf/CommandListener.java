package tamerial.ctf;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandListener {
	private Game game;
	
	/**
	 * Handles a command.
	 * @param sender
	 * The person or console who sent the command
	 * @param cmd
	 * The command itself
	 * @param label
	 * The name of the command
	 * @param args
	 * The arguments passed to it
	 * @return
	 * Whether the command was used properly.
	 */
	public boolean handleCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("start")) {
    		getGame().gameOverTicks = 29;
    		getGame().forceStart = true;
    		
    		return true;
    	}
    	else if (cmd.getName().equalsIgnoreCase("pause")) {
    		getGame().forcePause = !getGame().forcePause;
    		sender.sendMessage(ChatColor.GREEN + "Game " + (getGame().forcePause ? "paused" : "unpaused") + "!");
    		
    		return true;
    	}
    	else if (cmd.getName().equalsIgnoreCase("progress")) {
    		if (args.length >= 2) {
    			try {
	    			int capturePointId = Integer.parseInt(args[0]);
	    			double progress = Double.parseDouble(args[1]);
	    			getGame().getCapturePoints().get(capturePointId).setCaptureProgress(progress);
	    			
	    			return true;
    			} 
    			catch (Exception err) {
    				sender.sendMessage(ChatColor.RED + "Failed to execute, is the flag ID out of bounds?" + ChatColor.RESET);
    				return false;
    			}
    		}
    	}
    	else if (cmd.getName().equalsIgnoreCase("join")) {
    		if (sender instanceof Player) {
    			if (args.length >= 1) {
    				if (args[0].trim().equalsIgnoreCase("blue")) {
    					//if (game.teams.getPlayersOnTeam(-1).size() <= game.teams.getPlayersOnTeam(1).size()) {
	    					getGame().teams.setTeam(getGame(), sender.getName(), -1);
	    					sender.sendMessage("You have joined the " + ChatColor.BLUE + "Blue" + ChatColor.RESET + " team!");
	    					
	    					return true;
    					//}
    					//else {
    					//	sender.sendMessage(ChatColor.RED + "You cannot make the teams unbalanced by more than 1 player." + ChatColor.RESET);
    					//}
    				}
    				else if (args[0].trim().equalsIgnoreCase("red")) {
    					//if (game.teams.getPlayersOnTeam(1).size() <= game.teams.getPlayersOnTeam(-1).size()) {
    	    				getGame().teams.setTeam(getGame(), sender.getName(), 1);
    	    				sender.sendMessage("You have joined the " + ChatColor.RED + "Red" + ChatColor.RESET + " team!");
    	    				
    	    				return true;
	    				//}
						//else {
						//	sender.sendMessage(ChatColor.RED + "You cannot make the teams unbalanced by more than 1 player." + ChatColor.RESET);
						//	return true;
						//}
    				}
    				else {
    					return false;
    				}
    			}
    		}
    	}
    	else if (cmd.getName().equalsIgnoreCase("forcejoin")) {
    		if (args.length >= 2) {
	    		try {
	    			getGame().teams.setTeam(getGame(), args[0], args[1].trim().equalsIgnoreCase("blue") ? -1 : 1);
	    			
	    			return true;
	    		}
	    		catch (Exception err) {
	    			sender.sendMessage(ChatColor.RED + "Forcejoin failed.  Malformed team number?" + ChatColor.RESET);
	    		}
    		}
    		
    		return false;
    	}
    	else if (cmd.getName().equalsIgnoreCase("class")) {
    		if (sender instanceof Player) {
    			if (args.length >= 1) {
    				getGame().changeClass((Player)sender, args[0]);
    			}
    			else {
    				String list = "";
    				for (String outfit : getGame().outfits.keySet()) {
    					list += outfit + ", ";
    				}
    				
    				list = list.substring(0, list.length()-2);
    				
    				sender.sendMessage("Available classes: ");
    				sender.sendMessage(list);
    				
    				return true;
    			}
    		}
    	}
    	else if (cmd.getName().equalsIgnoreCase("gametime")) {
    		if (args.length >= 1) {
    			try {
    				int time = Integer.parseInt(args[0]);
    				
    				if (time < 0) {
    					throw new NumberFormatException();
    				}
    				
    				getGame().timeLeft = time;
    				
    				return true;
    			}
    			catch (NumberFormatException err) {
    				sender.sendMessage(ChatColor.RED + "Could not set game time.  Make sure gametime is a positive integer.");
    				return false;
    			}
    		}
    	}
    	return false;
	}

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}
}
