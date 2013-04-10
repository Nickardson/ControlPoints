package tamerial.ctf;

import java.util.ArrayList;

public class CapturePoints extends ArrayList<CapturePoint> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CapturePoints() {
		
	}
	
	/**
	 * Gets whether a team has captured all
	 * @param game
	 * @param team
	 * @return
	 */
	public boolean isTeamFullyCaptured(Game game, int team) {
		boolean hasAll = true;
		ArrayList<Integer> requiredCaptures;
		
		if (team == -1) {
			requiredCaptures = game.getBlueRequiredCaptures();
		}
		else {
			requiredCaptures = game.getRedRequiredCaptures();
		}
		
		for (int pointId : requiredCaptures) {
			double captureProgress = this.get(pointId).getCaptureProgress();
			if (Math.abs(captureProgress) < 15.999 || Math.signum(team) != Math.signum(captureProgress)) {
				hasAll = false;
				break;
			}
		}

		return hasAll;
	}
	
	/**
	 * Gets an ArrayList of the points that team has captured.
	 * @param team
	 * The ID of the team
	 * @return
	 */
	public ArrayList<CapturePoint> getCaptured(int team) {
		ArrayList<CapturePoint> captured = new ArrayList<CapturePoint>();
		
		for (CapturePoint point : this) {
			if (point.getCaptureProgress() == 16 * team) {
				captured.add(point);
			}
		}
		
		return captured;
	}
	
	/**
	 * Gets an ArrayList of points that team has partially under control
	 * @param team
	 * The team ID
	 * @param cutoff
	 * The minimum amount (absolute value) the point has to be captured for it to count as belonging to a team
	 * @return
	 */
	public ArrayList<CapturePoint> getControlled(int team, int cutoff) {
		ArrayList<CapturePoint> captured = new ArrayList<CapturePoint>();
		
		for (CapturePoint point : this) {
			if (Math.abs(point.getCaptureProgress()) >= cutoff && Math.signum(point.getCaptureProgress()) == team) {
				captured.add(point);
			}
		}
		
		return captured;
	}
	
	/**
	 * Gets the sum of the capture progress.  If the number is positive, Red has more progress, negative and Blue has more progress overall.
	 * @return
	 */
	public double getCaptureAmount(double min) {
		double total = 0;
		for (CapturePoint point : this) {
			double prog = point.getCaptureProgress();
			if (Math.abs(prog) >= min)
				total += prog;
		}
		
		return total;
	}
}
