package tamerial.ctf;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class Game {
	public Game(Teams teams, CapturePoints capturePoints) {
		this.teams = teams;
		this.setCapturePoints(capturePoints);
		
		setBlueRequiredCaptures(new ArrayList<Integer>());
		setRedRequiredCaptures(new ArrayList<Integer>());
		
		new ArrayList<String>();
		
		this.outfits = new HashMap<String, Outfit>();
		this.selectedClasses = new HashMap<String, String>();
	}
	
	/**
	 * The game mode to use
	 */
	private GameMode gameMode = GameMode.CONQUER;
	
	/**
	 * The world this game takes place in
	 */
	public static String world = "world";
	
	/**
	 * The time left in the current round.
	 */
	public int timeLeft = 60 * 15;
	
	/**
	 * The total number of times the blue team has won.
	 */
	public int redWins = 0;
	
	/**
	 * The total number of times the red team has won.
	 */
	public int blueWins = 0;

	public double capturePer20Ticks = .2;
	public Map<String, Outfit> outfits;
	public Map<String, String> selectedClasses;
	
	private ArrayList<Integer> blueRequiredCaptures;
	private ArrayList<Integer> redRequiredCaptures;
	
	/**
	 * The location where blue team will spawn, when the game starts.
	 */
	public Location blueSpawn;
	
	/**
	 * The location where red team will spawn, when the game starts
	 */
	public Location redSpawn;
	
	/**
	 * The location where players will spawn immediately after they die, and where they wait.
	 * Should be away from places where players can reach.
	 */
	public Location neutralSpawn;
	
	public GameScoreboard gameScoreboard;
	
	/**
	 * The teamservice
	 */
	public Teams teams;
	
	/**
	 * The capturepoints service
	 */
	private CapturePoints capturePoints;
	
	/**
	 * Whether the game is over 
	 */
	public boolean isGameOver = true;
	
	/**
	 * Forces a game to begin, even if there aren't enough players
	 */
	public boolean forceStart = false;
	
	/**
	 * Forces a game to not begin until forcepause is false.
	 */
	public boolean forcePause = false;
	
	/**
	 * The number of 10-ticks that have passed since the game over.
	 */
	public int gameOverTicks = 0;
	
	/**
	 * Whether players will automatically respawn
	 */
	public boolean canAutoRespawn = false;
	
	/**
	 * The points that blue team automatically holds at the start of the round
	 */
	public List<String> bluePoints;
	
	/**
	 * The points that blue team automatically holds at the start of the round
	 */
	public List<String> redPoints;
	
	/**
	 * Causes the game to end
	 */
	public void end() {
		isGameOver = true;
		gameOverTicks = 0;
		canAutoRespawn = false;
		timeLeft = 60 * 15;
		
		
		
		for (Player player : Bukkit.getWorld(Game.world).getPlayers()) {
			player.teleport(this.neutralSpawn);
		}
		
		Game.broadcast("Starting new game in 20 seconds, select your class!");
	}
	
	/**
	 * Causes the game to begin, by settings up the various game variables to the initial position 
	 */
	public void begin(FileConfiguration config) {
		isGameOver = false;
		gameOverTicks = 0;
		canAutoRespawn = true;
		forceStart = false;
		forcePause = false;
		timeLeft = 60 * 15;
		this.prepareCapturePoints(config);
		this.capturePoints.preparePointsOfIntrest();
	}

	/**
	 * Gets the capture points
	 * @return
	 */
	public CapturePoints getCapturePoints() {
		return capturePoints;
	}

	/**
	 * Sets the capture points
	 * @param capturePoints
	 */
	public void setCapturePoints(CapturePoints capturePoints) {
		this.capturePoints = capturePoints;
	}

	/**
	 * Gets the capture points required for a blue victory
	 * @return
	 */
	public ArrayList<Integer> getBlueRequiredCaptures() {
		return blueRequiredCaptures;
	}

	public void setBlueRequiredCaptures(ArrayList<Integer> blueRequiredCaptures) {
		this.blueRequiredCaptures = blueRequiredCaptures;
	}

	public ArrayList<Integer> getRedRequiredCaptures() {
		return redRequiredCaptures;
	}

	public void setRedRequiredCaptures(ArrayList<Integer> redRequiredCaptures) {
		this.redRequiredCaptures = redRequiredCaptures;
	}
	
	/**
	 * Prepares the capture points for a new round
	 */
	public void prepareCapturePoints(FileConfiguration config) {
		// Clear the points
		for (CapturePoint point : capturePoints) {
			point.resetCaptureProgress();
		}
		
		// Set the pre-captured points
		ConfigLoader.setPreCaptured(
				this,
				config.getStringList("ctf.pointsBlue"),
				config.getStringList("ctf.pointsRed"));
		
		
		// Add capture points required by the teams to win
		ConfigLoader.setRequired(
				this,
				config.getStringList("ctf.blueWin"),
				config.getStringList("ctf.redWin"));
		
		
	}
	
	/**
	 * Changes a player's class.
	 * @param player
	 * The player to change
	 * @param className
	 * The name of the class to change the player to
	 * @return
	 * Whether the class was changed successfully.
	 */
	public boolean changeClass(Player player, String className) {
		String formattedClass = className.trim().toLowerCase();
		
		if (outfits.containsKey(formattedClass)) {
			selectedClasses.put(player.getName(), formattedClass);
			
			System.out.println("Assigning " + player.getName() + " to class " + formattedClass);
			
			// TODO: Only kill if game is started and player is not in box
			double distanceToSpawn = player.getLocation().distanceSquared(this.neutralSpawn);
			if (distanceToSpawn > 100 || this.canAutoRespawn) {
				player.setHealth(0);
			}
			
			player.sendMessage(ChatColor.GREEN + "Class changed to " + formattedClass + ChatColor.RESET);
			
			return true;
		}
		else {
			player.sendMessage(ChatColor.RED + "Invalid class name.  Use /class to view available classes" + ChatColor.RESET);
			return false;
		}
	}
	
	/**
	 * Gets the class a player has selected.
	 * @param player
	 * The name of the player to check.
	 * @return
	 * The class the player has selected, or null if they haven't selected.
	 */
	public String getSelectedClass(String player) {
		if (selectedClasses.containsKey(player)) {
			return selectedClasses.get(player);
		}
		else {
			return null;
		}
	}
	
	/**
	 * Gets the team who has won.  This does not mean the team that is in the lead.
	 * A return value of 0 means no team has yet won.
	 * A non-zero return value means the game should end. 
	 * @return
	 */
	public int getWinningTeam() {
		if (getGameMode() == GameMode.CONQUER) {
			if (this.capturePoints.getCaptured(-1).size() == this.capturePoints.size()) {
				return -1;
			}
			else if (this.capturePoints.getCaptured(1).size() == this.capturePoints.size()) {
				return 1;
			}
		}
		else if (getGameMode() == GameMode.DEFEND) {
			if (this.capturePoints.getCaptured(1).size() >= this.capturePoints.size()) {
				return 1;
			}
			else if (this.timeLeft <= 0) {
				// The time has run out but Red doesn't have all points, blue wins
				return -1;
			}
		}
		
		return 0;
	}
	
	/**
	 * Gets the team who is in the lead, E.G. who would win if the time ended without the objective completed
	 * A return value of 0 is less likely than with getWinningTeam, but still possible at the start
	 * @return
	 */
	public int getLeadingTeam() {
		if (getGameMode() == GameMode.CONQUER) {
			int blueCaptured = this.capturePoints.getCaptured(-1).size();
			int redCaptured = this.capturePoints.getCaptured(1).size();
			
			// Captured points count for 20 points, uncaptured count for the amount they current are leaning.
			double total = this.capturePoints.getCaptureAmount(0) + (-4 * blueCaptured) +  (4 * redCaptured);
			
			return (int) Math.signum(total);
		}
		else if (getGameMode() == GameMode.DEFEND) {
			// If Red team has captured all of the points, Return red, otherwise blue is still winning
			if (this.capturePoints.getCaptured(1).size() >= this.capturePoints.size()) {
				return 1;
			}
			else {
				return -1;
			}
		}
		return 0;
	}
	
	/**
	 * Broadcasts a message to all players in the game world.
	 * @param message
	 */
	public static void broadcast(String message) {
		for (Player player : Bukkit.getWorld(Game.world).getPlayers()) {
			player.sendMessage(message);
		}
	}

	public GameMode getGameMode() {
		return gameMode;
	}

	public void setGameMode(GameMode gameMode) {
		this.gameMode = gameMode;
	}
}
