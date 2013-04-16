package tamerial.ctf.interactions;

import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import tamerial.ctf.Game;
import tamerial.ctf.InteractHandler;

public abstract class ProjectileInteraction extends InteractHandler {
	private Material weaponMaterial;
	private Material ammoMaterial;
	private String className;
	
	public ProjectileInteraction(Game game, String className, Material weaponMaterial, Material ammoMaterial) {
		super(game);
		
		setClassName(className);
		setAmmoMaterial(ammoMaterial);
		setWeaponMaterial(weaponMaterial);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void handle(PlayerInteractEvent event) {
		PlayerInventory inventory = event.getPlayer().getInventory();
		
		// Remove 1 ammo
		for (int i = 0; i < inventory.getSize(); i++) {
			ItemStack item = inventory.getItem(i);
			
			if (item != null)
			if (item.getType().equals(getAmmoMaterial())) {
				if (item.getAmount() == 1) {
					inventory.setItem(i, new ItemStack(0));
				}
				else {
					item.setAmount(item.getAmount() - 1);
				}
				
				event.getPlayer().updateInventory();
				
				break;
			}
		}
		
		fire(event);
	}
	
	public abstract void fire(PlayerInteractEvent event);

	@Override
	public boolean isApplicable(PlayerInteractEvent event) {
		String selectedClass = getGame().getSelectedClass(event.getPlayer().getName());
		if (selectedClass != null) {
			if (selectedClass.equalsIgnoreCase(getClassName())) {
				
				if (event.hasItem()) {
					if (event.getItem().getType().equals(getWeaponMaterial())) {
						if (event.getPlayer().getInventory().contains(getAmmoMaterial())) {
							return true;
						}
					}
				}
				
			}
		}
		return false;
	}

	public Material getWeaponMaterial() {
		return weaponMaterial;
	}

	public void setWeaponMaterial(Material weaponMaterial) {
		this.weaponMaterial = weaponMaterial;
	}

	public Material getAmmoMaterial() {
		return ammoMaterial;
	}

	public void setAmmoMaterial(Material ammoMaterial) {
		this.ammoMaterial = ammoMaterial;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

}
