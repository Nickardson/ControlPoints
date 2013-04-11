package tamerial.ctf;

import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

public class EventListener implements Listener {
	private Game game;

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
	    
	    game.gameScoreboard.showScoreboard(event.getPlayer());
    }
    
    @EventHandler
    public void onTakeDamage(EntityDamageByEntityEvent event) {
    	if (!event.getEntity().getWorld().getName().equals(Game.world))
    		return;
    	
    	if (event.getEntity() instanceof Player) {
    		Player damagedPlayer = (Player)event.getEntity();
    		
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
    				
    				// TODO: Test damaging
    				if (game.selectedClasses.containsKey(damagedPlayer.getName())) {
	    				if (game.selectedClasses.get(damagedPlayer.getName()).equalsIgnoreCase("spy")) {
	    					event.setCancelled(true);
	    					
	    					damagedPlayer.damage(event.getDamage(), ((Arrow)event.getDamager()).getShooter());
	    				}
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
	    		if (event.getItem().getType() == Material.POTION) {
	    			try {
		    			PotionMeta potionMeta = (PotionMeta)event.getItem().getItemMeta();
		    			if (potionMeta.hasDisplayName()) {
			    			if (potionMeta.getDisplayName().equalsIgnoreCase("Cloaking Potion")) {
			    				if (game.selectedClasses.containsKey(event.getPlayer().getName())) {
			    					if (game.selectedClasses.get(event.getPlayer().getName()).equals("spy")) {
			    						event.getPlayer().getInventory().setHelmet(null);
					    			}
			    				}
			    			}
		    			}
	    			}
	    			catch (NullPointerException ex) {
	    				// TODO: Ensure this can be removed
	    			}
	    		}
    		}
    	}
    }
}
