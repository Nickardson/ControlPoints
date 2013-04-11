package tamerial.ctf;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class GameScoreboard {
	private Scoreboard scoreboard;
	private org.bukkit.scoreboard.Team redTeam;
	private org.bukkit.scoreboard.Team blueTeam;
	private Objective scoreboardObjective;
	private ScoreboardManager scoreboardManager;
	
	public GameScoreboard(Game game) {
		scoreboardManager = Bukkit.getScoreboardManager();
		
		scoreboard = scoreboardManager.getNewScoreboard();
		redTeam = scoreboard.registerNewTeam("Red");
		redTeam.setPrefix(ChatColor.RED+"");
		redTeam.setCanSeeFriendlyInvisibles(true);
		redTeam.setAllowFriendlyFire(false);
		
		blueTeam = scoreboard.registerNewTeam("Blue");
		blueTeam.setPrefix(ChatColor.BLUE+"");
		blueTeam.setCanSeeFriendlyInvisibles(true);
		blueTeam.setAllowFriendlyFire(false);
		
		Objective objective = scoreboard.registerNewObjective("test", "playerKillCount");
		objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
		objective.setDisplayName("Kills");
		
		scoreboardObjective = scoreboard.registerNewObjective("stats", "dummy");
		scoreboardObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
		scoreboardObjective.setDisplayName("Stats");
		
		Score score = scoreboardObjective.getScore(Bukkit.getOfflinePlayer(ChatColor.GREEN + "Time:"));
		score.setScore(game.timeLeft);
		
		Score score1 = scoreboardObjective.getScore(Bukkit.getOfflinePlayer(ChatColor.RED + "Red Wins:"));
		score1.setScore(game.redWins);
		
		Score score2 = scoreboardObjective.getScore(Bukkit.getOfflinePlayer(ChatColor.BLUE + "Blue Wins:"));
		score2.setScore(game.blueWins);
		
		for (Player player : Bukkit.getWorld(Game.world).getPlayers()) {
			player.setScoreboard(scoreboard);
		}
	}
	
	public Scoreboard getScoreboard() {
		return scoreboard;
	}
	public void setScoreboard(Scoreboard scoreboard) {
		this.scoreboard = scoreboard;
	}
	
	/**
	 * Puts a player on a Scoreboard team
	 * @param player
	 * @param team
	 */
	public void setTeam(OfflinePlayer player, int team) {
		if (team == -1) {
			if (redTeam.hasPlayer(player)) {
				redTeam.removePlayer(player);
			}
			
			blueTeam.addPlayer(player);
		}
		else if (team == 1) {
			if (blueTeam.hasPlayer(player)) {
				blueTeam.removePlayer(player);
			}
			
			redTeam.addPlayer(player);
		}
	}
	
	public void showScoreboard(Player player) {
		player.setScoreboard(this.scoreboard);
	}
	
	public int getScore(String field) {
		Score score = scoreboardObjective.getScore(Bukkit.getOfflinePlayer(field));
		return score.getScore();
	}
	
	public void setScore(String field, int value) {
		Score score = scoreboardObjective.getScore(Bukkit.getOfflinePlayer(field));
		score.setScore(value);
	}
}
