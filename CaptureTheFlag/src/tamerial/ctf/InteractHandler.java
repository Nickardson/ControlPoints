package tamerial.ctf;

import org.bukkit.event.player.PlayerInteractEvent;

public abstract class InteractHandler {
	private Game game;
	public InteractHandler(Game game) {
		this.setGame(game);
	}
	
	public abstract void handle(PlayerInteractEvent event);
	public abstract boolean isApplicable(PlayerInteractEvent event);
	
	/**
	 * If this InteractHandler should handle the event, then it will handle it.
	 * @param event
	 */
	public boolean apply(PlayerInteractEvent event) {
		if (isApplicable(event)) {
			handle(event);
			return true;
		}
		
		return false;
	}
	
	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}
}
