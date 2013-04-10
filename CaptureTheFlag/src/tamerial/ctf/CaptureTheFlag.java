package tamerial.ctf;

import java.util.Arrays;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.ScoreboardManager;

public class CaptureTheFlag extends JavaPlugin implements Listener {
	public Game game;
	double accumulatedTicks = 0;
	int redWins = 0;
	int blueWins = 0;
	
	@Override
    public void onEnable(){
		this.saveDefaultConfig();
		
		game = new Game(new Teams(), new CapturePoints());
		
		if (getConfig().getString("ctf.world") != null) {
			Game.world = getConfig().getString("ctf.world");
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
			game.getCapturePoints().add(newCapturePoint);
		}
		
		game.prepareCapturePoints(this.getConfig());
		
		
		// Prepare capturing
		game.capturePer20Ticks = getConfig().getDouble("ctf.capturePer20Ticks");
		
		
		// Prepare scoreboards
		ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
		game.scoreboard = scoreboardManager.getNewScoreboard();
		game.redTeam = game.scoreboard.registerNewTeam("Red");
		game.redTeam.setPrefix(ChatColor.RED+"");
		game.redTeam.setCanSeeFriendlyInvisibles(true);
		game.redTeam.setAllowFriendlyFire(false);
		
		game.blueTeam = game.scoreboard.registerNewTeam("Blue");
		game.blueTeam.setPrefix(ChatColor.BLUE+"");
		game.blueTeam.setCanSeeFriendlyInvisibles(true);
		game.blueTeam.setAllowFriendlyFire(false);
		
		Objective objective = game.scoreboard.registerNewObjective("test", "playerKillCount");
		objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
		objective.setDisplayName("Kills");
		
		game.scoreboardObjective = game.scoreboard.registerNewObjective("stats", "dummy");
		game.scoreboardObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
		game.scoreboardObjective.setDisplayName("Stats");
		
		Score score = game.scoreboardObjective.getScore(Bukkit.getOfflinePlayer(ChatColor.GREEN + "Time:"));
		score.setScore(game.timeLeft);
		
		Score score1 = game.scoreboardObjective.getScore(Bukkit.getOfflinePlayer(ChatColor.RED + "Red Wins:"));
		score1.setScore(redWins);
		
		Score score2 = game.scoreboardObjective.getScore(Bukkit.getOfflinePlayer(ChatColor.BLUE + "Blue Wins:"));
		score2.setScore(blueWins);
		
		for (Player player : Bukkit.getWorld(Game.world).getPlayers()) {
			player.setScoreboard(game.scoreboard);
		}
		
		// Periodic game update
		final FileConfiguration runnableConfig = this.getConfig();
		Bukkit.getScheduler().runTaskTimer(this, new Runnable(){

			@Override
			public void run() {
				Score score = game.scoreboardObjective.getScore(Bukkit.getOfflinePlayer(ChatColor.GREEN + "Time:")); //Get a fake offline player
				score.setScore(game.timeLeft);
				
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
									blueWins++;
								
								if (winningTeam == 1)
									redWins++;
								
								Score score1 = game.scoreboardObjective.getScore(Bukkit.getOfflinePlayer(ChatColor.RED + "Red Wins:"));
								score1.setScore(redWins);
								
								Score score2 = game.scoreboardObjective.getScore(Bukkit.getOfflinePlayer(ChatColor.BLUE + "Blue Wins:"));
								score2.setScore(blueWins);
								
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
							blueWins++;
						
						if (winningTeam == 1)
							redWins++;
						
						Score score1 = game.scoreboardObjective.getScore(Bukkit.getOfflinePlayer(ChatColor.RED + "Red Wins:"));
						score1.setScore(redWins);
						
						Score score2 = game.scoreboardObjective.getScore(Bukkit.getOfflinePlayer(ChatColor.BLUE + "Blue Wins:"));
						score2.setScore(blueWins);
						
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
		
		getServer().getPluginManager().registerEvents(this, this);
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
    				// TODO: Add forced-balancing
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
    					if (game.teams.getPlayersOnTeam(1).size() <= game.teams.getPlayersOnTeam(-1).size()) {
    	    				game.teams.setTeam(game, sender.getName(), 1);
    	    				sender.sendMessage("You have joined the " + ChatColor.RED + "Red" + ChatColor.RESET + " team!");
    	    				
    	    				return true;
	    				}
						else {
							sender.sendMessage(ChatColor.RED + "You cannot make the teams unbalanced by more than 1 player." + ChatColor.RESET);
							return true;
						}
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
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
    	event.getPlayer().setScoreboard(game.scoreboard);
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
    	if (game.canAutoRespawn) {
	    	Player player = event.getPlayer();
	    	String playerName = player.getName();
	    	if (player.getLocation().distanceSquared(game.neutralSpawn) < 100) {
	    		if (game.selectedClasses.containsKey(playerName)) {
		    		int team = game.teams.getTeam(playerName);
		    		
		    		if (team == -1)
		    			player.teleport(game.blueSpawn.clone().add(0, 3, 0));
		    		
		    		if (team == 1)
		    			player.teleport(game.redSpawn.clone().add(0, 3, 0));
	    		}
	    	}
    	}
    }
    
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
    	boolean helmet = true;
    	Outfit outfit = null;
		if (game.selectedClasses.containsKey(event.getPlayer().getName())) {
			outfit = game.outfits.get(game.selectedClasses.get(event.getPlayer().getName()));
			helmet = outfit.helmet;
		}
		
    	int playerTeam = game.teams.getTeam(event.getPlayer().getName());
    	//if (playerTeam != 0) {
    		Color color = Color.WHITE;
    		
    		if (playerTeam == -1) {
    			color = Outfit.blueColor;
    		}
    		else if (playerTeam == 1) {
    			color = Outfit.redColor;
    		}
    		
	        if (helmet) {
	        	event.getPlayer().getInventory().setHelmet(
	        		Outfit.getColoredLeather(
	        				Material.LEATHER_HELMET, 
	        				color,
	        				Arrays.asList(ChatColor.WHITE + event.getPlayer().getName() + "'s standard issue team identification hat" + ChatColor.RESET))
	        			);
	        }
    	//}
    	
	    // Remove all potion effects
	    for (PotionEffect effect : event.getPlayer().getActivePotionEffects()) {
	    	event.getPlayer().removePotionEffect(effect.getType());
	    }
	    
	    if (outfit != null) {
	    	outfit.applyTo(event.getPlayer(), playerTeam);
	    }
	    
	    event.getPlayer().setScoreboard(game.scoreboard);
    }
    
    @EventHandler
    public void onTakeDamage(EntityDamageByEntityEvent event) {
    	if (event.getEntity() instanceof Player) {
    		Player damagedPlayer = (Player)event.getEntity();
    		
    		if (event.getCause() == DamageCause.PROJECTILE) {
    			if (game.selectedClasses.containsKey(damagedPlayer.getName())) {
    				if (game.selectedClasses.get(damagedPlayer.getName()).equalsIgnoreCase("spy")) {
    					event.setCancelled(true);
    					
    					damagedPlayer.damage(8, ((Arrow)event.getDamager()).getShooter());
    				}
    			}
    		}
    		
    		if (event.getCause() == DamageCause.ENTITY_ATTACK || event.getCause() == DamageCause.PROJECTILE) {
    			if (event.getDamager() instanceof Player) {
    				Player damagingPlayer = (Player)event.getDamager();
    				
    				if (game.teams.onSameTeam(damagedPlayer.getName(), damagingPlayer.getName()) && !(damagedPlayer.getName().equals(damagingPlayer.getName())))
    					event.setCancelled(true);
    			}
    			else if (event.getDamager() instanceof Arrow) {
    				Arrow arrow = (Arrow)event.getDamager();
    				if (arrow.getShooter() instanceof Player) {
    					Player damagingPlayer = (Player)arrow.getShooter();
    					
    					if (game.teams.onSameTeam(damagedPlayer.getName(), damagingPlayer.getName()) && !(damagedPlayer.getName().equals(damagingPlayer.getName())))
        					event.setCancelled(true);
    				}
    			}
    		}
    		else if (event.getCause() == DamageCause.FALL) {
    			if (game.selectedClasses.containsKey(damagedPlayer.getName())) {
    				if (game.selectedClasses.get(damagedPlayer.getName()).equalsIgnoreCase("scout")) {
    					event.setCancelled(true);
    				}
    			}
    		}
    	}
    }
    
    @EventHandler
    public void onDied(PlayerDeathEvent event) {
    	// Remove all potion effects, which fixes the infinite invisibility bug
	    for (PotionEffect effect : event.getEntity().getActivePotionEffects()) {
	    	event.getEntity().removePotionEffect(effect.getType());
	    }
	    
	    event.getDrops().clear();
    }
    
    @EventHandler
    public void onChangeArmor(InventoryClickEvent event) {
    	if (event.getSlotType() == SlotType.ARMOR || Outfit.isItemArmor(event.getCursor())) {
    		event.setCancelled(true);
    	}
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
    	if (event.hasItem()) {
    		if (event.getItem() != null) {
	    		if (event.getItem().getType() == Material.POTION) {
	    			PotionMeta potionMeta = (PotionMeta)event.getItem().getItemMeta();
	    			if (potionMeta.getDisplayName().equalsIgnoreCase("Cloaking Potion")) {
	    				if (game.selectedClasses.containsKey(event.getPlayer().getName())) {
	    					if (game.selectedClasses.get(event.getPlayer().getName()).equals("spy")) {
	    						event.getPlayer().getInventory().setHelmet(null);
			    			}
	    				}
	    			}
	    		}
    		}
    	}
    }
    
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
    	// Remove the arrows that stick to you on death
    	
    	if (event.getEntity().getType() == EntityType.ARROW) {
    		event.getEntity().remove();
    	}
    }
    
    
}
