package tamerial.ctf;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

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
		
		this.ignoreDeathsFor = new ArrayList<String>();
		
		this.outfits = new HashMap<String, Outfit>();
		this.selectedClasses = new HashMap<String, String>();
	}
	
	public double capturePer20Ticks = 1;
	public Map<String, Outfit> outfits;
	public Map<String, String> selectedClasses;
	
	private ArrayList<Integer> blueRequiredCaptures;
	private ArrayList<Integer> redRequiredCaptures;
	
	private ArrayList<String> ignoreDeathsFor;
	
	public Location blueSpawn;
	public Location redSpawn;
	public Location neutralSpawn;
	
	
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
	
	public List<String> bluePoints;
	public List<String> redPoints;
	
	/**
	 * Causes the game to end
	 */
	public void end() {
		isGameOver = true;
		gameOverTicks = 0;
		canAutoRespawn = false;
	}
	
	/**
	 * Causes the game to begin
	 */
	public void begin(FileConfiguration config) {
		isGameOver = false;
		gameOverTicks = 0;
		canAutoRespawn = true;
		forceStart = false;
		forcePause = false;
		
		this.prepareCapturePoints(config);
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
	
	public boolean changeClass(Player player, String className) {
		String formattedClass = className.trim().toLowerCase();
		
		if (outfits.containsKey(formattedClass)) {
			selectedClasses.put(player.getName(), formattedClass);
			
			System.out.println("Assigning " + player.getName() + " to class " + formattedClass);
			
			// TODO: Only kill if game is started and player is not in box
			double distanceToSpawn = player.getLocation().distanceSquared(this.neutralSpawn);

			if (this.canAutoRespawn) {
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
}
