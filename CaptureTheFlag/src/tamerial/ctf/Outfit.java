package tamerial.ctf;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class Outfit {
	public static ItemStack getColoredLeather(Material leatherPart, Color color, List<String> lore) {
		ItemStack item = new ItemStack(leatherPart);
    	LeatherArmorMeta armorMeta = (LeatherArmorMeta)item.getItemMeta();
		armorMeta.setColor(color);
		armorMeta.setLore(lore);
		item.setItemMeta(armorMeta);
		return item;
	}
	
	public Outfit() {
		items = new ArrayList<ItemStack>();
	}
	
	//public ItemStack helmet;
	public ItemStack chestplate;
	public ItemStack leggings;
	public ItemStack boots;
	public List<ItemStack> items;
	
	public String name;
	
	public void applyTo(Player player) {
		System.out.println("Player: " + player);
		System.out.println("Inventory: " + player.getInventory());
		PlayerInventory inv = player.getInventory();
		
		//inv.setHelmet(helmet.clone());
		if (chestplate != null)
			inv.setChestplate(chestplate.clone());
		if (leggings != null)
			inv.setLeggings(leggings.clone());
		if (boots != null)
			inv.setBoots(boots.clone());
		
		for (ItemStack item : items) {
			inv.addItem(item);
			//System.out.println("Adding item: " + item.toString());
		}
	}
	
	/**
	 * Gets whether an itemstack is an armor.
	 * @param item
	 * The itemstack to check
	 * @return
	 */
	public static boolean isItemArmor(ItemStack item) {
    	String typeString = item.getType().toString();

    	if (typeString.indexOf("HELMET") != -1)
    		return true;
    	if (typeString.indexOf("CHESTPLATE") != -1)
    		return true;
    	if (typeString.indexOf("LEGGINGS") != -1)
    		return true;
    	if (typeString.indexOf("BOOTS") != -1)
    		return true;
    	
    	return false;
    }
	
	public static boolean isItemLeatherArmor(ItemStack item) {
    	String typeString = item.getType().toString();

    	if (typeString.indexOf("LEATHER_") != -1)
    		return true;
    	
    	return false;
    }
}
