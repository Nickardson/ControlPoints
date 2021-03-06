package tamerial.ctf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Teams {
	public Map<String, Integer> playerTeams = new HashMap<String, Integer>();
	
	/**
	 * Puts a player on a certain team, and sets their team color
	 * @param player
	 * @param team
	 */
	public void setTeam(Game game, String player, int team) {
		playerTeams.put(player, new Integer(team));
		
		game.gameScoreboard.setTeam(Bukkit.getOfflinePlayer(player), team);
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
	
	/**
	 * Gets whether two players are on the same team
	 * @param a
	 * The name of one player
	 * @param b
	 * The name of another player
	 * @return
	 * Whether they are on the same team
	 */
	public boolean onSameTeam(String a, String b) {
		return getTeam(a) == getTeam(b);
	}
	
	/**
	 * Gets a list of the players who are on the given team.
	 * @param team
	 * The team ID
	 * @return
	 * The list of players on that team
	 */
	public ArrayList<Player> getPlayersOnTeam(int team) {
		ArrayList<Player> players = new ArrayList<Player>();
		
		for (Player player : Bukkit.getWorld(Game.world).getPlayers()) {
			if (getTeam(player.getName()) == team) {
				players.add(player);
			}
		}
		
		return players;
	}
}
