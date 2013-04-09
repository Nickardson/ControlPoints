package tamerial.ctf;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;

public class ConfigLoader {
	/**
	 * Gets 3 comma-seperated coordinates from a string
	 * @param input
	 * The string, eg: "1, 2, 3"
	 * @return
	 * An array with a length of 3
	 */
    public static int[] getCoords(String input) {
    	String[] split = input.split(", ");
		int[] coords = new int[3];
		
		for (int i = 0; i < 3; i++)
			coords[i] = Integer.parseInt(split[i]);
		
		return coords;
    }
    
    /**
     * Gets a location based off 3 comma-seperated coordinates
     * @param world
     * The world the location is in
     * @param input
     * The string, eg: "1, 2, 3"
     * @return
     */
    public static Location getCoordsLocation(World world, String input) {
    	String[] split = input.split(", ");
		int[] coords = new int[3];
		
		for (int i = 0; i < 3; i++)
			coords[i] = Integer.parseInt(split[i]);
		
		return new Location(world, coords[0], coords[1], coords[2]);
    }
    
    /**
     * Sets the precaptured control points, based off a list of strings, which represent each flag ID
     * @param game
     * @param bluePointsString
     * @param redPointsString
     */
    public static void setPreCaptured(Game game, List<String> bluePointsString, List<String> redPointsString) {
    	for (String pointName : bluePointsString) {
			int pointId = Integer.parseInt(pointName);
			
			if (pointId >= 0 && pointId < game.getCapturePoints().size()) {
				CapturePoint point = game.getCapturePoints().get(pointId);
				point.setCaptureProgress(-16);
			}
		}
		
		for (String pointName : redPointsString) {
			int pointId = Integer.parseInt(pointName);
			
			if (pointId >= 0 && pointId < game.getCapturePoints().size()) {
				CapturePoint point = game.getCapturePoints().get(pointId);
				point.setCaptureProgress(16);
			}
		}
    }
    
    /**
     * Sets the required control points for each respective team to capture to win, based off a list of strings, which represent each flag ID
     * @param game
     * @param blueWinString
     * @param redWinString
     */
    public static void setRequired(Game game, List<String> blueWinString, List<String> redWinString) {
    	for (String blueWin : blueWinString) {
			int win = Integer.parseInt(blueWin);
			if (win >= 0)
				game.getBlueRequiredCaptures().add(win);
		}
		
		for (String redWin : redWinString) {
			int win = Integer.parseInt(redWin);
			if (win >= 0)
				game.getRedRequiredCaptures().add(win);
		}
    }
}
