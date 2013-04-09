package tamerial.ctf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

public class CaptureTheFlag extends JavaPlugin implements Listener {
	public double capturePer20Ticks = 1;
	public Map<String, Outfit> outfits;
	public Map<String, String> selectedClasses;
	
	public Game game;
	
	@Override
    public void onEnable(){
		this.saveDefaultConfig();
		
		game = new Game(new Teams(), new CapturePoints());
		
		// Load spawn coordinates
		game.blueSpawn = ConfigLoader.getCoordsLocation(Bukkit.getWorld("world"), this.getConfig().getString("ctf.blueSpawn"));
		game.redSpawn = ConfigLoader.getCoordsLocation(Bukkit.getWorld("world"), this.getConfig().getString("ctf.redSpawn"));
		
		int[] coords = ConfigLoader.getCoords(this.getConfig().getString("ctf.neutralSpawn"));
		Bukkit.getWorld("world").setSpawnLocation(coords[0], coords[1], coords[2]);
		game.neutralSpawn = new Location(Bukkit.getWorld("world"), coords[0], coords[1], coords[2]);
		
		
		// Prepare capture points
		for (String capturePoint : this.getConfig().getStringList("ctf.capturePoints")) {
			System.out.println("Loading capture point with location: \"" + capturePoint + "\"");
			
			CapturePoint newCapturePoint = new CapturePoint(ConfigLoader.getCoordsLocation(Bukkit.getWorld("world"), capturePoint));
			game.getCapturePoints().add(newCapturePoint);
		}
		
		capturePer20Ticks = getConfig().getDouble("ctf.capturePer20Ticks");
		
		game.prepareCapturePoints(this.getConfig());
		
		// Periodic check
		final FileConfiguration runnableConfig = this.getConfig();
		Bukkit.getScheduler().runTaskTimer(this, new Runnable(){

			@Override
			public void run() {
				if (!game.isGameOver) {
					// Capturing logic
					for (int i = 0; i < game.getCapturePoints().size(); i++) {
						CapturePoint capPoint = game.getCapturePoints().get(i);
						
						int blueMembers = capPoint.getNearbyPlayersOnTeam(3.5, game.teams, -1).size();
						int redMembers = capPoint.getNearbyPlayersOnTeam(3.5, game.teams, 1).size();
						
						// Only run when capturing teams XOR == 0, but there are actually capturing teams
						if (!(blueMembers!=0 && redMembers!=0) && (blueMembers + redMembers > 0)) {
							int capturingTeam = (blueMembers > redMembers) ? -1 : 1;
							
							if (capPoint.isCapturable() && (capPoint.getAssualtingTeam() == 0 || capPoint.getAssualtingTeam() == capturingTeam)) {
								double previousCaptureProgress = capPoint.getCaptureProgress();
								boolean changed = capPoint.setCaptureProgress((capPoint.getCaptureProgress() + capturePer20Ticks * capturingTeam * (Math.max(blueMembers, redMembers))));
								
								if (changed && Math.abs(capPoint.getCaptureProgress()) == 16) {
									Bukkit.broadcastMessage((capturingTeam==1?(ChatColor.RED+"Red"):(ChatColor.BLUE+"Blue")) + ChatColor.RESET + " team has captured point #" + Integer.toString(i+1) + "!");
								}
								
								if (changed && (Math.abs(previousCaptureProgress) == 16 || previousCaptureProgress == 0)) {
									Bukkit.broadcastMessage((capturingTeam==1?(ChatColor.RED+"Red"):(ChatColor.BLUE+"Blue")) + ChatColor.RESET + " team is taking control of point #" + Integer.toString(i+1) + "!");
								}
							}
						}
					}
					
					// If it is not possible for a team to win by captures, then don't count captures for that team
					boolean blueHasAll = (game.getBlueRequiredCaptures().size() != 0);
					boolean redHasAll = (game.getRedRequiredCaptures().size() != 0);
					
					blueHasAll = game.getCapturePoints().isTeamFullyCaptured(game, -1);
					redHasAll = game.getCapturePoints().isTeamFullyCaptured(game, 1);
					
					if ((blueHasAll || redHasAll) && !(blueHasAll && redHasAll)) {
						int winningTeam = blueHasAll ? -1 : 1;
						Bukkit.broadcastMessage("The " + ((winningTeam == -1) ? (ChatColor.BLUE + "Blue") : (ChatColor.RED + "Red")) + ChatColor.RESET + " team has won!");
						
						for (Player player : Bukkit.getOnlinePlayers()) {
							player.teleport(game.neutralSpawn);
						}
						
						game.end();
						
						Bukkit.broadcastMessage("Starting new game in 20 seconds, select your class!");
					}
				}
				else {
					if ((Bukkit.getOnlinePlayers().length > 1 || game.forceStart) && (!game.forcePause)) {
						game.gameOverTicks++;
					}
					
					if (game.gameOverTicks == 30) {
						Bukkit.broadcastMessage(ChatColor.GREEN + "The game is: Conquer!");
						Bukkit.broadcastMessage(ChatColor.GREEN + "Capture all control points to win");
					}
					
					if (game.gameOverTicks >= 40) {
						for (CapturePoint point : game.getCapturePoints()) {
							point.setCaptureProgress(0);
						}
						
						Bukkit.broadcastMessage(ChatColor.GREEN + "Starting new game!");
						
						for (Player player : Bukkit.getOnlinePlayers()) {
							player.setHealth(0);
						}
						
						game.begin(runnableConfig);
					}
				}
			}}, 
			0,10
		);
		
		
		// Prepare outfits
		outfits = new HashMap<String, Outfit>();
		selectedClasses = new HashMap<String, String>();
		
		List<Map<?, ?>> outfitList = getConfig().getMapList("ctf.outfits");
		
		for (Map<?, ?> outfit : outfitList) {
			Outfit newOutfit = ConfigLoader.loadOutfit(outfit);
			outfits.put(newOutfit.name.trim().toLowerCase(), newOutfit);
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
    	}
    	else if (cmd.getName().equalsIgnoreCase("progress")) {
    		if (args.length == 2) {
    			try {
	    			int capturePointId = Integer.parseInt(args[0]);
	    			double progress = Double.parseDouble(args[1]);
	    			game.getCapturePoints().get(capturePointId).setCaptureProgress(progress);
    			} 
    			catch (Exception err) {
    				System.out.println(err.getLocalizedMessage());
    				sender.sendMessage("Failed to execute");
    			}
    		}
    	}
    	else if (cmd.getName().equalsIgnoreCase("join")) {
    		if (sender instanceof Player) {
    			if (args.length >= 1) {
    				// TODO: Add forced-balancing
    				if (args[0].trim().equalsIgnoreCase("blue")) {
    					game.teams.setTeam(sender.getName(), -1);
    					sender.sendMessage("You have joined the " + ChatColor.BLUE + "Blue" + ChatColor.RESET + " team!");
    					
    					//((Player)sender).setBedSpawnLocation(blueSpawn, true);
    				}
    				else if (args[0].trim().equalsIgnoreCase("red")) {
    					game.teams.setTeam(sender.getName(), 1);
    					sender.sendMessage("You have joined the " + ChatColor.RED + "Red" + ChatColor.RESET + " team!");
    					
    					//((Player)sender).setBedSpawnLocation(redSpawn, true);
    				}
    				else {
    					return false;
    				}
    				
    				
    			}
    		}
    	}
    	else if (cmd.getName().equalsIgnoreCase("class")) {
    		if (sender instanceof Player) {
    			if (args.length >= 1) {
    				if (game.teams.getTeam(sender.getName()) != 0) {
	    				String formattedClass = args[0].trim().toLowerCase();
	    				
	    				if (outfits.containsKey(formattedClass)) {
	    					selectedClasses.put(sender.getName(), formattedClass);
	    					
	    					System.out.println("Assigning " + sender.getName() + " to class " + formattedClass);
	    					
	    					// TODO: Only kill if game is started and player is not in box
	    					double distanceToSpawn = ((Player)sender).getLocation().distanceSquared(game.neutralSpawn);
	    					System.out.println("Distance to spawn: " + distanceToSpawn);
	    					if (game.canAutoRespawn) {
	    						((Player) sender).setHealth(0);
	    					}
	    					
	    					sender.sendMessage(ChatColor.GREEN + "Class changed to " + formattedClass + ChatColor.RESET);
	    					
	    					return true;
	    				}
	    				else {
	    					sender.sendMessage(ChatColor.RED + "Invalid class name.  Use /class to view available classes" + ChatColor.RESET);
	    				}
    				}
    				else {
    					sender.sendMessage(ChatColor.RED + "You must be on a team to select a class." + ChatColor.RESET);
    				}
    			}
    			else {
    				String list = "";
    				for (String outfit : outfits.keySet()) {
    					list += outfit + ", ";
    				}
    				
    				list = list.substring(0, list.length()-2);
    				
    				sender.sendMessage("Available classes: ");
    				sender.sendMessage(list);
    				
    				return true;
    			}
    		}
    	}
    	
    	return false; 
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
    	//event.getPlayer().setBedSpawnLocation(allSpawn, true);
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
    	if (game.canAutoRespawn) {
	    	Player player = event.getPlayer();
	    	String playerName = player.getName();
	    	if (player.getLocation().distanceSquared(game.neutralSpawn) < 100) {
	    		if (selectedClasses.containsKey(playerName)) {
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
    	
    	int playerTeam = game.teams.getTeam(event.getPlayer().getName());
    	//if (playerTeam != 0) {
    		Color color = Color.WHITE;
    		
    		if (playerTeam == -1) {
    			color = Color.fromRGB(44, 98, 222);
    			//event.getPlayer().teleport(blueSpawn);
    			//getServer().dispatchCommand(getServer().getConsoleSender(), "/tp "+event.getPlayer().getName()+" "+blueSpawn.getBlockX()+" "+blueSpawn.getBlockY()+" "+blueSpawn.getBlockZ());
    			
    			System.out.println("Teleporting blue to " + game.blueSpawn.toString());
    		}
    		else if (playerTeam == 1) {
    			color = Color.fromRGB(242, 61, 61);
    			//event.getPlayer().teleport(redSpawn);
    			//getServer().dispatchCommand(getServer().getConsoleSender(), "/tp "+event.getPlayer().getName()+" "+redSpawn.getBlockX()+" "+redSpawn.getBlockY()+" "+redSpawn.getBlockZ());
    			
    			System.out.println("Teleporting red to " + game.redSpawn.toString());
    		}
    		
	        event.getPlayer().getInventory().setHelmet(
	        		Outfit.getColoredLeather(
	        				Material.LEATHER_HELMET, 
	        				color,
	        				Arrays.asList(ChatColor.WHITE + event.getPlayer().getName() + "'s standard issue team identification hat" + ChatColor.RESET))
	        );
    	//}
    	
	    if (selectedClasses.containsKey(event.getPlayer().getName())) {
	    	outfits.get(selectedClasses.get(event.getPlayer().getName())).applyTo(event.getPlayer());
	    }
	       
	    // Remove all potion effects
	    for (PotionEffect effect : event.getPlayer().getActivePotionEffects()) {
	    	event.getPlayer().removePotionEffect(effect.getType());
	    }
    }
    
    @EventHandler
    public void onTakeDamage(EntityDamageByEntityEvent event) {
    	if (event.getEntity() instanceof Player) {
    		Player damagedPlayer = (Player)event.getEntity();
    		
    		if (event.getCause() == DamageCause.ENTITY_ATTACK || event.getCause() == DamageCause.PROJECTILE) {
    			if (event.getDamager() instanceof Player) {
    				Player damagingPlayer = (Player)event.getDamager();
    				
    				if (game.teams.onSameTeam(damagedPlayer.getName(), damagingPlayer.getName()))
    					event.setCancelled(true);
    			}
    			else if (event.getDamager() instanceof Arrow) {
    				Arrow arrow = (Arrow)event.getDamager();
    				if (arrow.getShooter() instanceof Player) {
    					Player damagingPlayer = (Player)arrow.getShooter();
    					
    					if (game.teams.onSameTeam(damagedPlayer.getName(), damagingPlayer.getName()))
        					event.setCancelled(true);
    				}
    			}
    		}
    		else if (event.getCause() == DamageCause.FALL) {
    			if (selectedClasses.containsKey(damagedPlayer.getName())) {
    				if (selectedClasses.get(damagedPlayer.getName()).equalsIgnoreCase("scout")) {
    					event.setCancelled(true);
    				}
    			}
    		}
    		
    		System.out.println("Player damaged by: " + event.getCause());
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
	    				if (selectedClasses.containsKey(event.getPlayer().getName())) {
	    					if (selectedClasses.get(event.getPlayer().getName()).equals("spy")) {
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
