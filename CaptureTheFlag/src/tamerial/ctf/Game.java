package tamerial.ctf;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;

public class Game {
	public Game(Teams teams, CapturePoints capturePoints) {
		this.teams = teams;
		this.setCapturePoints(capturePoints);
		
		setBlueRequiredCaptures(new ArrayList<Integer>());
		setRedRequiredCaptures(new ArrayList<Integer>());
		
	}
	
	private ArrayList<Integer> blueRequiredCaptures;
	private ArrayList<Integer> redRequiredCaptures;
	
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
	public void begin() {
		isGameOver = false;
		gameOverTicks = 0;
		canAutoRespawn = true;
		forceStart = false;
		forcePause = false;
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
}
