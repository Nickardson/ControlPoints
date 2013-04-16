package tamerial.ctf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class EventListener implements Listener {
	private Game game;
	private ArrayList<InteractHandler> interactHandlers;
	
	public EventListener() {
		interactHandlers = new ArrayList<InteractHandler>();
	}
	
	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}
	

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
    	if (!event.getPlayer().getWorld().getName().equals(Game.world))
    		return;
    	
    	game.gameScoreboard.showScoreboard(event.getPlayer());
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
    	if (!event.getPlayer().getWorld().getName().equals(Game.world))
    		return;
    	
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
    	if (!event.getPlayer().getWorld().getName().equals(Game.world))
    		return;
    	
    	boolean helmet = true;
    	Outfit outfit = null;
    	String selectedClass = game.getSelectedClass(event.getPlayer().getName());
		if (selectedClass != null) {
			outfit = game.outfits.get(selectedClass);
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
	    
	    game.gameScoreboard.showScoreboard(event.getPlayer());
    }
    
    @SuppressWarnings("deprecation")
	@EventHandler
    public void onTakeDamage(EntityDamageByEntityEvent event) {
    	if (!event.getEntity().getWorld().getName().equals(Game.world))
    		return;
    	
    	if (event.getEntity() instanceof Player) {
    		Player damagedPlayer = (Player)event.getEntity();
    		
    		if (event.getCause() == DamageCause.ENTITY_ATTACK || event.getCause() == DamageCause.PROJECTILE) {
    			if (event.getDamager() instanceof Player) {
    				if (damagedPlayer.getLocation().distanceSquared(game.neutralSpawn) < 100)
    					event.setCancelled(true);
    				
    				Player damagingPlayer = (Player)event.getDamager();
    				
    				if (game.teams.onSameTeam(damagedPlayer.getName(), damagingPlayer.getName()) && !(damagedPlayer.getName().equals(damagingPlayer.getName()))) {
    					event.setCancelled(true);
    					
    					String damagerClass = game.getSelectedClass(damagingPlayer.getName());
    					if (damagerClass != null)
    						if (damagerClass.equalsIgnoreCase("medic")) {
    							// Heal player
    							ItemStack damagerItemInHand = damagingPlayer.getItemInHand();
    							if (damagerItemInHand.getType().equals(Material.GOLD_SPADE)) {
    								int oldHealth = damagedPlayer.getHealth();
    								damagedPlayer.setHealth(Math.min(damagedPlayer.getMaxHealth(), damagedPlayer.getHealth()+2));
    								
    								// If the new health is somehow lower, don't give durability, otherwise damage by the amount of health given to the player
    								damagerItemInHand.setDurability(
    										(short) (damagerItemInHand.getDurability()+Math.max(damagedPlayer.getHealth()-oldHealth, 0))
    										);
    								
    								if (damagerItemInHand.getDurability() >= 32) {
    									damagingPlayer.setItemInHand(null);
    								}
    								
    								damagingPlayer.updateInventory();
    							}
    						}
    				}
    			}
    			else if (event.getDamager() instanceof Arrow) {
    				Arrow arrow = (Arrow)event.getDamager();
    				if (arrow.getShooter() instanceof Player) {
    					Player damagingPlayer = (Player)arrow.getShooter();
    					
    					if (game.teams.onSameTeam(damagedPlayer.getName(), damagingPlayer.getName()) && !(damagedPlayer.getName().equals(damagingPlayer.getName())))
        					event.setCancelled(true);
    				}
    				
    				// TODO: Test damaging
    				String selectedClass = game.getSelectedClass(damagedPlayer.getName());
    				if (selectedClass != null) {
    					if (selectedClass.equalsIgnoreCase("spy")) {
	    					event.setCancelled(true);
	    					
	    					damagedPlayer.damage(event.getDamage(), ((Arrow)event.getDamager()).getShooter());
	    				}
	    			}
    			}
    		}
    		else if (event.getCause() == DamageCause.FALL) {
    			String selectedClass = game.getSelectedClass(damagedPlayer.getName());
    			if (selectedClass != null) {
    				if (selectedClass.equalsIgnoreCase("agent")) {
    					event.setCancelled(true);
    				}
    			}
    		}
    	}
    }
    
    @EventHandler
    public void onDied(PlayerDeathEvent event) {
    	if (!event.getEntity().getWorld().getName().equals(Game.world))
    		return;
    	
    	// Remove all potion effects, which fixes the infinite invisibility bug
	    for (PotionEffect effect : event.getEntity().getActivePotionEffects()) {
	    	event.getEntity().removePotionEffect(effect.getType());
	    }
	    
	    event.getDrops().clear();
    }
    
    @EventHandler
    public void onChangeArmor(InventoryClickEvent event) {
    	if (!event.getWhoClicked().getWorld().getName().equals(Game.world))
    		return;
    	
    	if (event.getSlotType() == SlotType.ARMOR || Outfit.isItemArmor(event.getCursor())) {
    		event.setCancelled(true);
    	}
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
    	if (!event.getPlayer().getWorld().getName().equals(Game.world))
    		return;
    	
    	if (event.hasItem()) {
    		if (event.getItem() != null) {
	    		for (InteractHandler handler : interactHandlers) {
	    			handler.apply(event);
	    		}
    		}
    	}
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
    	if (!event.getBlock().getWorld().getName().equals(Game.world))
    		return;
    	
    	event.setCancelled(true);
    }
    
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
    	if (event.getEntityType().equals(EntityType.SMALL_FIREBALL)) {
    		List<Entity> nearbyEntities = event.getEntity().getNearbyEntities(2,2,2);
    		for (Entity entity : nearbyEntities) {
    			if (entity.getType().equals(EntityType.SMALL_FIREBALL)) 
    				continue;
    			
    			if (entity instanceof Player) {
    				int damagedTeam = game.teams.getTeam(((Player) entity).getName());
    				int damagerTeam = game.teams.getTeam((((Player) event.getEntity().getShooter()).getName()));
    				
    				if (damagedTeam == damagerTeam) {
    					continue;
    				}
    			} 
    			
    			double fireTicks = Math.max(
    					(20 * 10) - Math.max(entity.getLocation().distanceSquared(event.getEntity().getLocation())*10,0),
    					0);
    			
    			if (fireTicks > entity.getFireTicks()) {
    				entity.setFireTicks((int)fireTicks);
    			}
    		}
    	}
    }
    
	public ArrayList<InteractHandler> getInteractHandlers() {
		return interactHandlers;
	}

	public void setInteractHandlers(ArrayList<InteractHandler> interactHandlers) {
		this.interactHandlers = interactHandlers;
	}
}
