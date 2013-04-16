package tamerial.ctf.interactions;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import tamerial.ctf.Game;

public class FlamethrowerInteraction extends ProjectileInteraction {

	public FlamethrowerInteraction(Game game, String className, Material weaponMaterial, Material ammoMaterial) {
		super(game, className, weaponMaterial, ammoMaterial);
	}

	@Override
	public void fire(PlayerInteractEvent event) {
		World world = event.getPlayer().getWorld();
		Vector lookVector = event.getPlayer().getLocation().getDirection().normalize();
		Fireball fireball = (Fireball)world.spawnEntity(
				event.getPlayer().getLocation().clone().add(
						lookVector.clone().multiply(2)
						).add(0, 2, 0), 
				EntityType.SMALL_FIREBALL);
		fireball.setIsIncendiary(true);
		fireball.setYield(0);
		fireball.setVelocity(lookVector.clone().multiply(.1));
		
		// Catch on fire on random
		if (Math.random() < .1) {
			event.getPlayer().setFireTicks(20 * 5);
		}
	}

}
