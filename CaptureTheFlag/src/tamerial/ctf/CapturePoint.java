package tamerial.ctf;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CapturePoint {
	public static int[] capturePointRingX = {+3, +3, +2, +1, +0, -1, -2, -3, -3, -3, -2, -1, +0, +1, +2, +3};
	public static int[] capturePointRingZ = {+0, +1, +2, +3, +3, +3, +2, +1, +0, -1, -2, -3, -3, -3, -2, -1};
	
	private double captureProgress;
	private Location location;
	
	/** 
	 * Whether the capture point can be changed from it's state
	 */
	private boolean capturable;
	
	/**
	 * Whether the capture point is allowed to slowly revert to neutral if not fully captured
	 */
	private boolean autoNeutral;
	
	/**
	 *  The team attacking the capture point.  If this == 0, then any team can capture the capture point.  Otherwise, only that team can change the capture point.
	 */
	private int assualtingTeam;
	
	public CapturePoint(Location loc, boolean capturable, boolean autoneutral) {
		this.captureProgress = 0;
		setLocation(loc);
		this.capturable = capturable;
		this.autoNeutral = autoneutral;
		this.assualtingTeam = 0;
	}
	
	public CapturePoint(Location loc, boolean capturable) {
		this(loc, capturable, false);
	}
	
	public CapturePoint(Location loc) {
		this(loc, true, false);
	}
	
	public boolean isCapturable() {
		return capturable;
	}
	
	public void setCapturable(boolean capturable) {
		this.capturable = capturable;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public void setLocation(Location loc) {
		location = loc;
	}
	
	/**
	 * Gets the current progress on this capture point
	 * @return
	 * Negative if blue, positive if red.
	 */
	public double getCaptureProgress() {
		return captureProgress;
	}
	
	/**
	 * Resets the capture progress of this capture point to 0.
	 */
	public void resetCaptureProgress() {
		captureProgress = 0;
		
		for (int i = 0; i < capturePointRingX.length; i++) {
			setTeamBlock(this.location.clone().add(capturePointRingX[i], 0, capturePointRingZ[i]), 0);
		}
	}
	
	/**
	 * Sets the capture progress of the capture point.
	 * Returns false if the capture point cannot be changed.
	 * @param progress
	 * The progress on the capture point
	 * @return
	 * Whether the capture point was able to be changed
	 */
	public boolean setCaptureProgress(double progress) {
		if (this.isCapturable()) {
			// Restrict the amount of progress made so it cannot exceed the blocks available
			if (progress < (capturePointRingX.length) * -1)
				progress = (capturePointRingX.length) * -1;
			
			if (progress > capturePointRingX.length)
				progress = (capturePointRingX.length);
			
			// Exit with failure if no change
			if (captureProgress == progress) {
				return false;
			}
			
			captureProgress = progress;
			
			// Turn the (double) progress into a block-representative value, and compensate for integer rounding on negatives
			int blockProgress;
			if (progress < 0)
				blockProgress = (int)(progress-.5);
			else
				blockProgress = (int)(progress);
			
			
			if (blockProgress <= -1) {
				// TODO: Flip this
				
				for (int i = 0; i < capturePointRingX.length; i++) {
					setTeamBlock(
							this.location.clone().add(capturePointRingX[i], 0, capturePointRingZ[i]), 
							(i+1 <= -blockProgress)?-1:0);
				}
			}
			else if (blockProgress >= 1) {
				for (int i = 0; i < capturePointRingX.length; i++) {
					setTeamBlock(
							this.location.clone().add(capturePointRingX[i], 0, capturePointRingZ[i]), 
							(i+1 <= blockProgress)?1:0);
				}
			}
			else {
				for (int i = 0; i < capturePointRingX.length; i++) {
					setTeamBlock(this.location.clone().add(capturePointRingX[i], 0, capturePointRingZ[i]), 0);
				}
			}
			
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Sets a certain block to a team's color.
	 * @param x
	 * Block's x coordinate
	 * @param y
	 * Block's y coordinate
	 * @param z
	 * Block's z coordinate
	 * @param team
	 * The team to set it to.  The sign will determine the team.
	 * - Blue
	 * = Neutral
	 * + Red
	 */
	public void setTeamBlock(int x, int y, int z, int team) {
		// Default to iron block
		int id = 42;
		
		if (team > 0) {
			// Block of Redstone
			id = 152;
		}
		else if (team < 0) {
			// Block of Lapis
			id = 22;
		}
		
		Bukkit.getWorld(Game.world).getBlockAt(x, y, z).setTypeId(id);
	}
	
	public void setTeamBlock(Location loc, int team) {
		setTeamBlock(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), team);
	}

	/**
	 * Gets whether this capture point automatically reverts to neutral
	 * @return
	 */
	public boolean isAutoNeutral() {
		return autoNeutral;
	}

	public void setAutoNeutral(boolean autoNeutral) {
		this.autoNeutral = autoNeutral;
	}

	public int getAssualtingTeam() {
		return assualtingTeam;
	}

	public void setAssualtingTeam(int assualtingTeam) {
		this.assualtingTeam = assualtingTeam;
	}
	
	/**
	 * Gets the world this capture point is in
	 * @return
	 * The world the capture point is in
	 */
	public World getWorld() {
		return this.getLocation().getWorld();
	}
	
	/**
	 * Gets all of the nearby alive players within a radius of the capture point
	 * @param radius
	 * The radius around this capture point to search
	 * @return
	 * A list of the nearby players
	 */
	public List<Player> getNearbyPlayers(double radius) {
		List<Player> nearList = new ArrayList<Player>();
		
		for (Player player : this.getWorld().getPlayers()) {
			if (player.getHealth() <= 0) {
				continue;
			}
			
			double distance = this.getLocation().distance(player.getLocation());
			
			if (distance < radius) {
				nearList.add(player);
			}
		}
		
		return nearList;
	}
	
	/**
	 * Gets all of the nearby alive players within a radius of the capture point, who are on a certain team.
	 * @param radius
	 * The radius around this capture point to search
	 * @param teamService
	 * The teamservice, used to look up what team the players are on
	 * @param team
	 * The team to match in the searched players
	 * @return
	 * A list of the nearby players on that team
	 */
	public List<Player> getNearbyPlayersOnTeam(double radius, Teams teamService, int team) {
		List<Player> nearList = new ArrayList<Player>();
		
		for (Player player : this.getNearbyPlayers(3.5)) {
			if (teamService.getTeam(player.getName()) == team) {
				nearList.add(player);
			}
		}
		
		return nearList;
	}
}
