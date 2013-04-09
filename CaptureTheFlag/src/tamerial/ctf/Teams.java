package tamerial.ctf;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class Teams {
	public Map<String, Integer> playerTeams = new HashMap<String, Integer>();
	
	/**
	 * Puts a player on a certain team, and sets their team color
	 * @param player
	 * @param team
	 */
	public void setTeam(String player, int team) {
		playerTeams.put(player, new Integer(team));
		
		if (team == -1) {
			setTeamColor(player, ChatColor.BLUE);
		}
		else if (team == 1) {
			setTeamColor(player, ChatColor.RED);
		}
	}
	
	/**
	 * Sets a player's name so that their name is correctly colored
	 * @param player
	 * The name of the player
	 * @param color
	 * The color to set it to
	 */
	public void setTeamColor(String player, ChatColor color) {
		String newName = color + player + ChatColor.RESET;
		Bukkit.getPlayer(player).setDisplayName(newName);
		Bukkit.getPlayer(player).setPlayerListName(newName);
	}
	
	/**
	 * Gets the team a player is on
	 * @param player
	 * The name of the player to check.
	 * @return
	 * The team Id of the player
	 */
	public int getTeam(String player) {
		if (playerTeams.containsKey(player)) {
			return playerTeams.get(player).intValue();
		} else {
			return 0;
		}
	}
	
	/**
	 * Gets the name of the given team
	 * @param team
	 * The team ID
	 * @return
	 * The string name of the team
	 */
	public String getTeamName(int team) {
		if (team == -1) {
			return "Blue";
		}
		else {
			return "Red";
		}
	}
}
