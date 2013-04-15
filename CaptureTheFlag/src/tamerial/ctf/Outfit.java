package tamerial.ctf;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;

public class Outfit {
	public static Color redColor = Color.fromRGB(242, 61, 61);
	public static Color blueColor = Color.fromRGB(44, 98, 222);
	
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
		potions = new ArrayList<PotionEffect>();
	}
	
	/**
	 * Whether the outfit should have a team-colored helmet.
	 */
	public boolean helmet = true;
	
	/**
	 * The chestplate that will be cloned onto the player
	 */
	public ItemStack chestplate;
	
	/**
	 * The legs that will be cloned onto the player
	 */
	public ItemStack leggings;
	
	/**
	 * The boots that will be cloned onto the player
	 */
	public ItemStack boots;
	
	/**
	 * The items that the player will have
	 */
	public List<ItemStack> items;
	
	/**
	 * Whether to color all leather items to the player's team color
	 */
	public boolean colorAllLeatherToTeam = false;
	
	/**
	 * The name of this outfit (eg: class name)
	 */
	public String name;
	
	/**
	 * A list of potion effects that the wearer of this outfit will always have.
	 */
	public List<PotionEffect> potions;
	
	/**
	 * Applies the potions to the player
	 * @param player
	 * The player to apply the potions to.
	 */
	public void applyPotions(Player player) {
		for (PotionEffect potionEffect : potions) {
			player.addPotionEffect(potionEffect, true);
		}
	}
	
	/**
	 * Applies the outfit to a player.
	 * This includes armor and potion effects.
	 * @param player
	 * The player to apply the outfit to
	 * @param team
	 * The team ID, used for colored leather
	 */
	public void applyTo(Player player, int team) {
		PlayerInventory inv = player.getInventory();
		
		Color leatherColor = Color.WHITE;
		
		if (team == -1)
			leatherColor = Outfit.blueColor;
		
		if (team == 1)
			leatherColor = Outfit.redColor;
		
		if (chestplate != null) {
			if (colorAllLeatherToTeam) {
				inv.setChestplate(Outfit.colorLeatherArmor(chestplate.clone(), leatherColor));
			}
			else {
				inv.setChestplate(chestplate.clone());
			}
		}
		if (leggings != null) {
			if (colorAllLeatherToTeam) {
				inv.setLeggings(Outfit.colorLeatherArmor(leggings.clone(), leatherColor));
			}
			else {
				inv.setLeggings(leggings.clone());
			}
		}
		if (boots != null) {
			if (colorAllLeatherToTeam) {
				inv.setBoots(Outfit.colorLeatherArmor(boots.clone(), leatherColor));
			}
			else {
				inv.setBoots(boots.clone());
			}
		}
		
		for (ItemStack item : items) {
			if (item != null) {
				inv.addItem(item);
			}
		}
		
		this.applyPotions(player);
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
	
	/**
	 * Gets whether an item is Leather armor.
	 * @param item
	 * The item to check
	 * @return
	 * Whether the item is leather.
	 */
	public static boolean isItemLeatherArmor(ItemStack item) {
    	String typeString = item.getType().toString();

    	if (typeString.indexOf("LEATHER_") != -1)
    		return true;
    	
    	return false;
    }
	
	/**
	 * If the item is leather, then colors the item
	 * @param item
	 * The item to possibly set the color of
	 * @param color
	 * The color to set it to
	 * @return
	 * The same itemstack, colored.
	 */
	public static ItemStack colorLeatherArmor(ItemStack item, Color color) {
		if (Outfit.isItemLeatherArmor(item)) {
			LeatherArmorMeta meta = (LeatherArmorMeta)item.getItemMeta();
			meta.setColor(color);
			item.setItemMeta(meta);
		}
		return item;
	}
}
