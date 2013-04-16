package tamerial.ctf;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import tamerial.ctf.interactions.FlamethrowerInteraction;

public class CaptureTheFlag extends JavaPlugin implements Listener {
	public Game game;
	double accumulatedTicks = 0;
	public EventListener eventListener;
	public CommandListener commandListener;
	
	@Override
    public void onEnable(){
		this.saveDefaultConfig();
		
		game = new Game(new Teams(), new CapturePoints());
		
		eventListener = new EventListener();
		eventListener.setGame(game);
		eventListener.getInteractHandlers().add(new FlamethrowerInteraction(game, "pyro", Material.BLAZE_POWDER, Material.BLAZE_POWDER));
		//eventListener.getInteractHandlers().add(new HealAttackInteraction(game, "medic", Material.GOLD_SPADE, 2));
		
		
		commandListener = new CommandListener();
		commandListener.setGame(game);
		
		String gameMode = getConfig().getString("ctf.mode");
		if (gameMode != null) {
			game.setGameMode(GameMode.valueOf(gameMode.toUpperCase()));
		}
		
		if (getConfig().getString("ctf.world") != null && !getConfig().getString("ctf.world").equals(""))
			Game.world = getConfig().getString("ctf.world");
		else {
			Game.world = "world";
			System.out.println(ChatColor.RED + "Unable to load World from Config, defaulting to world ('world')");
		}
		
		// Load spawn coordinates
		game.blueSpawn = ConfigLoader.getCoordsLocation(Bukkit.getWorld(Game.world), this.getConfig().getString("ctf.blueSpawn"));
		game.redSpawn = ConfigLoader.getCoordsLocation(Bukkit.getWorld(Game.world), this.getConfig().getString("ctf.redSpawn"));
		
		int[] coords = ConfigLoader.getCoords(this.getConfig().getString("ctf.neutralSpawn"));
		Bukkit.getWorld(Game.world).setSpawnLocation(coords[0], coords[1], coords[2]);
		game.neutralSpawn = new Location(Bukkit.getWorld(Game.world), coords[0], coords[1], coords[2]);
		
		
		// Prepare capture points
		for (String capturePoint : this.getConfig().getStringList("ctf.capturePoints")) {
			System.out.println("Loading capture point with location: \"" + capturePoint + "\"");
			
			CapturePoint newCapturePoint = new CapturePoint(ConfigLoader.getCoordsLocation(Bukkit.getWorld(Game.world), capturePoint));
			
			if (game.getGameMode() == GameMode.DEFEND) {
				newCapturePoint.setAssualtingTeam(1);
				newCapturePoint.setAutoNeutral(true);
			}
			
			game.getCapturePoints().add(newCapturePoint);
		}
		
		
		// Prepare capturing
		game.capturePer20Ticks = getConfig().getDouble("ctf.capturePer20Ticks");
		
		
		// Prepare scoreboards
		game.gameScoreboard = new GameScoreboard(game);
		
		
		// Set up game environment variables, and game capture points
		game.prepareCapturePoints(getConfig());
		game.getCapturePoints().preparePointsOfIntrest();
		
		
		// Periodic game update
		final FileConfiguration runnableConfig = this.getConfig();
		Bukkit.getScheduler().runTaskTimer(this, new Runnable(){

			@Override
			public void run() {
				game.gameScoreboard.setScore(ChatColor.GREEN + "Time:", game.timeLeft);
				
				if (!game.isGameOver) {
					// Timing logic
					accumulatedTicks+=10;
					
					if (accumulatedTicks >= 20) {
						accumulatedTicks -= 20;
						
						game.timeLeft--;
						
						if (game.timeLeft < 0)
							game.timeLeft = 0;
						
						if (game.timeLeft <= 0) {
							int winningTeam = game.getWinningTeam();
							
							if (winningTeam == 0) {
								winningTeam = game.getLeadingTeam();
							}
							
							if (winningTeam != 0) {
								Game.broadcast("The " + ((winningTeam == -1) ? (ChatColor.BLUE + "Blue") : (ChatColor.RED + "Red")) + ChatColor.RESET + " team has won!");
								
								if (winningTeam == -1)
									game.blueWins++;
								
								if (winningTeam == 1)
									game.redWins++;
								
								game.gameScoreboard.setScore(ChatColor.RED + "Red Wins:", game.redWins);
								game.gameScoreboard.setScore(ChatColor.BLUE + "Blue Wins:", game.blueWins);
							}
							else {
								Game.broadcast(ChatColor.GREEN + "Neither team won this round.");
							}
							game.end();
						}
					}
					
					game.getCapturePoints().preparePointsOfIntrest();
					
					// Capturing logic
					for (int i = 0; i < game.getCapturePoints().size(); i++) {
						CapturePoint capPoint = game.getCapturePoints().get(i);
						
						int blueMembers = capPoint.getNearbyPlayersOnTeam(3.2, game.teams, -1).size();
						int redMembers = capPoint.getNearbyPlayersOnTeam(3.2, game.teams, 1).size();
						
						// If the mode is defend, only allow changes on the point of contention.
						if (game.getGameMode() == GameMode.DEFEND) {
							if (!capPoint.isPointOfIntrest()) {
								continue;
							}
						}
						
						// Only run when there are members capturing, but not when there are members from both teams
						if (!(blueMembers!=0 && redMembers!=0) && (blueMembers + redMembers > 0)) {
							int capturingTeam = (blueMembers > redMembers) ? -1 : 1;
							
							// Only advance if the point is capturable, and if the team able to capture is the team that is supposed to be capturing.
							if (capPoint.isCapturable() && (capPoint.getAssualtingTeam() == 0 || capPoint.getAssualtingTeam() == capturingTeam)) {
								double previousCaptureProgress = capPoint.getCaptureProgress();
								boolean changed = capPoint.setCaptureProgress((capPoint.getCaptureProgress() + game.capturePer20Ticks * capturingTeam * (Math.max(blueMembers, redMembers))));
								
								if (changed && Math.abs(capPoint.getCaptureProgress()) == 16) {
									Game.broadcast((capturingTeam==1?(ChatColor.RED+"Red"):(ChatColor.BLUE+"Blue")) + ChatColor.RESET + " team has captured point #" + Integer.toString(i+1) + "!");
								}
								
								if (changed && (Math.abs(previousCaptureProgress) == 16 || previousCaptureProgress == 0)) {
									Game.broadcast((capturingTeam==1?(ChatColor.RED+"Red"):(ChatColor.BLUE+"Blue")) + ChatColor.RESET + " team is taking control of point #" + Integer.toString(i+1) + "!");
								}
							}
						}
						
						// Revert back to 0
						if (capPoint.isAutoNeutral()) {
							boolean revert = false;
							boolean extraRevert = false;
							
							if (capPoint.getAssualtingTeam() == 0) {
								if (blueMembers == 0 && redMembers == 0) {
									revert = true;
								}
							}
							else {
								if (capPoint.getAssualtingTeam() == -1 && blueMembers == 0) {
									revert = true;
									
									if (redMembers > 0)
										extraRevert = true;
								}
								
								if (capPoint.getAssualtingTeam() == 1 && redMembers == 0) {
									revert = true;
									
									if (blueMembers > 0)
										extraRevert = true;
								}
							}
							
							if (revert) {
								double revertAmount = extraRevert ? (game.capturePer20Ticks/2) : (game.capturePer20Ticks/4);
								double progress = capPoint.getCaptureProgress();
								
								if (Math.abs(progress) < revertAmount) {
									capPoint.setCaptureProgress(0);
								}
								else {
									int direction = (int) (Math.signum(progress) * -1);
									capPoint.setCaptureProgress(capPoint.getCaptureProgress() + (revertAmount * direction));
								}
							}
						}
					}
					
					int winningTeam = game.getWinningTeam();
					if (winningTeam != 0) {
						Game.broadcast("The " + ((winningTeam == -1) ? (ChatColor.BLUE + "Blue") : (ChatColor.RED + "Red")) + ChatColor.RESET + " team has won!");
						
						if (winningTeam == -1)
							game.blueWins++;
						
						if (winningTeam == 1)
							game.redWins++;
						
						game.gameScoreboard.setScore(ChatColor.RED + "Red Wins:", game.redWins);
						game.gameScoreboard.setScore(ChatColor.BLUE + "Blue Wins:", game.blueWins);
						
						game.end();
					}
				}
				else {
					if ((Bukkit.getWorld(Game.world).getPlayers().size() > 1 || game.forceStart) && (!game.forcePause)) {
						game.gameOverTicks++;
					}
					
					if (game.gameOverTicks == 30) {
						Game.broadcast(ChatColor.GREEN + "The game is: Conquer!");
						Game.broadcast(ChatColor.GREEN + "Capture all control points to win");
					}
					
					if (game.gameOverTicks >= 40) {
						for (CapturePoint point : game.getCapturePoints()) {
							point.setCaptureProgress(0);
						}
						
						Game.broadcast(ChatColor.GREEN + "Starting new game!");
						
						for (Player player : Bukkit.getWorld(Game.world).getPlayers()) {
							player.setHealth(0);
						}
						
						game.begin(runnableConfig);
					}
				}
			}}, 
			0,10
		);
		
		// Periodically apply potions every 4 seconds
		Bukkit.getScheduler().runTaskTimer(this, new Runnable(){
			@Override
			public void run() {
					for (Player player : Bukkit.getWorld(Game.world).getPlayers()) {
						String selectedClass = game.getSelectedClass(player.getName());
						if (selectedClass != null) {
							game.outfits.get(selectedClass).applyPotions(player);
					}
				}
			}}, 
			0,20 * 4
		);
		
    	
		// Prepare outfits
		for (Map<?, ?> outfit : getConfig().getMapList("ctf.outfits")) {
			Outfit newOutfit = ConfigLoader.loadOutfit(outfit);
			game.outfits.put(newOutfit.name.trim().toLowerCase(), newOutfit);
		}
		
		getServer().getPluginManager().registerEvents(eventListener, this);
    }
	
    @Override
    public void onDisable() {
    	this.saveConfig();
    }
    
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
    	return commandListener.handleCommand(sender, cmd, label, args);
    }
}
