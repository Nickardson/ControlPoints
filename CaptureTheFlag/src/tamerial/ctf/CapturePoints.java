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
}
