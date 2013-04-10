package tamerial.ctf;

public enum GameMode {
	/**
	 * Capture all flags within the time limit, if the time limit is reached, the team with the most capture progress wins 
	 */
	CONQUER,
	
	/**
	 * Blue team starts with all 3 points in their possession, Red team has to capture them all within the time limit or Blue wins
	 */
	DEFEND,
}
