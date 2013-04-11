package tamerial.ctf;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class CaptureTheFlag extends JavaPlugin implements Listener {
	public Game game;
	double accumulatedTicks = 0;
	public EventListener eventListener;
	
	@Override
    public void onEnable(){
		this.saveDefaultConfig();
		
		game = new Game(new Teams(), new CapturePoints());
		eventListener = new EventListener();
		eventListener.setGame(game);
		
		if (getConfig().getString("ctf.world") != null)
			Game.world = getConfig().getString("ctf.world");
		
		
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
			game.getCapturePoints().add(newCapturePoint);
		}
		
		game.prepareCapturePoints(this.getConfig());
		
		
		// Prepare capturing
		game.capturePer20Ticks = getConfig().getDouble("ctf.capturePer20Ticks");
		
		
		// Prepare scoreboards
		game.gameScoreboard = new GameScoreboard(game);
		
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
					
					// Capturing logic
					for (int i = 0; i < game.getCapturePoints().size(); i++) {
						CapturePoint capPoint = game.getCapturePoints().get(i);
						
						int blueMembers = capPoint.getNearbyPlayersOnTeam(3.5, game.teams, -1).size();
						int redMembers = capPoint.getNearbyPlayersOnTeam(3.5, game.teams, 1).size();
						
						// Only run when there are members capturing, but not when there are members from both teams
						if (!(blueMembers!=0 && redMembers!=0) && (blueMembers + redMembers > 0)) {
							int capturingTeam = (blueMembers > redMembers) ? -1 : 1;
							
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
		
		// Periodically apply potions every 10 seconds
		Bukkit.getScheduler().runTaskTimer(this, new Runnable(){
			@Override
			public void run() {
				for (Player player : Bukkit.getWorld(Game.world).getPlayers())
				if (game.selectedClasses.containsKey(player.getName())) {
					game.outfits.get(game.selectedClasses.get(player.getName())).applyPotions(player);
				}
			}}, 
			0,200
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
    	if (cmd.getName().equalsIgnoreCase("start")) {
    		game.gameOverTicks = 29;
    		game.forceStart = true;
    		
    		return true;
    	}
    	else if (cmd.getName().equalsIgnoreCase("pause")) {
    		game.forcePause = !game.forcePause;
    		sender.sendMessage(ChatColor.GREEN + "Game " + (game.forcePause ? "paused" : "unpaused") + "!");
    		
    		return true;
    	}
    	else if (cmd.getName().equalsIgnoreCase("progress")) {
    		if (args.length >= 2) {
    			try {
	    			int capturePointId = Integer.parseInt(args[0]);
	    			double progress = Double.parseDouble(args[1]);
	    			game.getCapturePoints().get(capturePointId).setCaptureProgress(progress);
	    			
	    			return true;
    			} 
    			catch (Exception err) {
    				sender.sendMessage(ChatColor.RED + "Failed to execute, is the flag ID out of bounds?" + ChatColor.RESET);
    				return false;
    			}
    		}
    	}
    	else if (cmd.getName().equalsIgnoreCase("join")) {
    		if (sender instanceof Player) {
    			if (args.length >= 1) {
    				if (args[0].trim().equalsIgnoreCase("blue")) {
    					//if (game.teams.getPlayersOnTeam(-1).size() <= game.teams.getPlayersOnTeam(1).size()) {
	    					game.teams.setTeam(game, sender.getName(), -1);
	    					sender.sendMessage("You have joined the " + ChatColor.BLUE + "Blue" + ChatColor.RESET + " team!");
	    					
	    					return true;
    					//}
    					//else {
    					//	sender.sendMessage(ChatColor.RED + "You cannot make the teams unbalanced by more than 1 player." + ChatColor.RESET);
    					//}
    				}
    				else if (args[0].trim().equalsIgnoreCase("red")) {
    					//if (game.teams.getPlayersOnTeam(1).size() <= game.teams.getPlayersOnTeam(-1).size()) {
    	    				game.teams.setTeam(game, sender.getName(), 1);
    	    				sender.sendMessage("You have joined the " + ChatColor.RED + "Red" + ChatColor.RESET + " team!");
    	    				
    	    				return true;
	    				//}
						//else {
						//	sender.sendMessage(ChatColor.RED + "You cannot make the teams unbalanced by more than 1 player." + ChatColor.RESET);
						//	return true;
						//}
    				}
    				else {
    					return false;
    				}
    			}
    		}
    	}
    	else if (cmd.getName().equalsIgnoreCase("forcejoin")) {
    		if (args.length >= 2) {
	    		try {
	    			game.teams.setTeam(game, args[0], args[1].trim().equalsIgnoreCase("blue") ? -1 : 1);
	    			
	    			return true;
	    		}
	    		catch (Exception err) {
	    			sender.sendMessage(ChatColor.RED + "Forcejoin failed.  Malformed team number?" + ChatColor.RESET);
	    		}
    		}
    		
    		return false;
    	}
    	else if (cmd.getName().equalsIgnoreCase("class")) {
    		if (sender instanceof Player) {
    			if (args.length >= 1) {
    				game.changeClass((Player)sender, args[0]);
    			}
    			else {
    				String list = "";
    				for (String outfit : game.outfits.keySet()) {
    					list += outfit + ", ";
    				}
    				
    				list = list.substring(0, list.length()-2);
    				
    				sender.sendMessage("Available classes: ");
    				sender.sendMessage(list);
    				
    				return true;
    			}
    		}
    	}
    	else if (cmd.getName().equalsIgnoreCase("gametime")) {
    		if (args.length >= 1) {
    			try {
    				int time = Integer.parseInt(args[0]);
    				
    				if (time < 0) {
    					throw new NumberFormatException();
    				}
    				
    				game.timeLeft = time;
    				
    				return true;
    			}
    			catch (NumberFormatException err) {
    				sender.sendMessage(ChatColor.RED + "Could not set game time.  Make sure gametime is a positive integer.");
    				return false;
    			}
    		}
    	}
    	return false; 
    }
}
