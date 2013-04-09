package tamerial.ctf;

import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ConfigLoader {
	/**
	 * Gets 3 comma-seperated coordinates from a string
	 * @param input
	 * The string, eg: "1, 2, 3"
	 * @return
	 * An array with a length of 3
	 */
    public static int[] getCoords(String input) {
    	String[] split = input.split(", ");
		int[] coords = new int[3];
		
		for (int i = 0; i < 3; i++)
			coords[i] = Integer.parseInt(split[i]);
		
		return coords;
    }
    
    /**
     * Gets a location based off 3 comma-seperated coordinates
     * @param world
     * The world the location is in
     * @param input
     * The string, eg: "1, 2, 3"
     * @return
     */
    public static Location getCoordsLocation(World world, String input) {
    	String[] split = input.split(", ");
		int[] coords = new int[3];
		
		for (int i = 0; i < 3; i++)
			coords[i] = Integer.parseInt(split[i]);
		
		return new Location(world, coords[0], coords[1], coords[2]);
    }
    
    /**
     * Sets the precaptured control points, based off a list of strings, which represent each flag ID
     * @param game
     * @param bluePointsString
     * @param redPointsString
     */
    public static void setPreCaptured(Game game, List<String> bluePointsString, List<String> redPointsString) {
    	for (String pointName : bluePointsString) {
			int pointId = Integer.parseInt(pointName);
			
			if (pointId >= 0 && pointId < game.getCapturePoints().size()) {
				CapturePoint point = game.getCapturePoints().get(pointId);
				point.setCaptureProgress(-16);
			}
		}
		
		for (String pointName : redPointsString) {
			int pointId = Integer.parseInt(pointName);
			
			if (pointId >= 0 && pointId < game.getCapturePoints().size()) {
				CapturePoint point = game.getCapturePoints().get(pointId);
				point.setCaptureProgress(16);
			}
		}
    }
    
    /**
     * Sets the required control points for each respective team to capture to win, based off a list of strings, which represent each flag ID
     * @param game
     * @param blueWinString
     * @param redWinString
     */
    public static void setRequired(Game game, List<String> blueWinString, List<String> redWinString) {
    	game.getBlueRequiredCaptures().clear();
    	game.getRedRequiredCaptures().clear();
    	
    	for (String blueWin : blueWinString) {
			int win = Integer.parseInt(blueWin);
			if (win >= 0)
				game.getBlueRequiredCaptures().add(win);
		}
		
		for (String redWin : redWinString) {
			int win = Integer.parseInt(redWin);
			if (win >= 0)
				game.getRedRequiredCaptures().add(win);
		}
    }
    
    @SuppressWarnings("unchecked")
	public static ItemStack loadItem(Map<?, ?> map) {
    	System.out.println(map);
    	
    	Material itemMaterial = Material.AIR;
    	int itemCount = 1;
    	short itemDamage = 0;
    	String name = "";
    	
    	if (map.containsKey("name")) {
	    	try {
	    		name = (String)map.get("name");
	    	}
	    	catch (NullPointerException err) {
	    		System.out.println(ChatColor.RED + "Error:  Bad name in outfit item definition.");
	    	}
    	}
    	
    	// Load material
    	if (map.containsKey("material")) {
	    	try {
	    		itemMaterial = Material.valueOf(((String)map.get("material")).trim().toUpperCase());
	    	}
	    	catch (IllegalArgumentException err) {
	    		System.out.println(ChatColor.RED + "Error:  Bad material in outfit item definition.");
	    	}
    	}
    	
    	// Load itemcount
    	if (map.containsKey("count")) {
	    	try {
	    		itemCount = (Integer)map.get("count");
	    	}
	    	catch (NumberFormatException err) {
	    		System.out.println(ChatColor.RED + "Error:  Bad item count in outfit item definition.");
	    	}
    	}
    	
    	if (map.containsKey("damage")) {
	    	try {
	    		itemDamage = ((Integer)map.get("damage")).shortValue();
	    	}
	    	catch (NumberFormatException err) {
	    		System.out.println(ChatColor.RED + "Error:  Bad item damage in outfit item definition.");
	    	}
    	}
    	
    	if (itemMaterial != Material.AIR) {
    		ItemStack itemStack = new ItemStack(itemMaterial, itemCount, itemDamage);
    		
    		if (!name.equals("")) {
    			ItemMeta meta = itemStack.getItemMeta();
    			meta.setDisplayName(name);
    			itemStack.setItemMeta(meta);
    		}
    		if (map.containsKey("color")) {
    			int[] rgb = ConfigLoader.getCoords((String)map.get("color"));
    			
    			if (Outfit.isItemLeatherArmor(itemStack)) {
    				try {
    					LeatherArmorMeta meta = (LeatherArmorMeta)itemStack.getItemMeta();
    					meta.setColor(Color.fromRGB(rgb[0], rgb[1], rgb[2]));
    					itemStack.setItemMeta(meta);
    				}
    				catch (IllegalArgumentException err) {
    					System.out.println(ChatColor.RED + "Error:  Bad item color in outfit item definition.");
    				}
    			}
    		}
    		
    		// Enchant item
    		if (map.containsKey("enchants")) {
        		try {
        			List<String> enchants = (List<String>)map.get("enchants");
        			
        			for (String enchant : enchants) {
        				String[] enchantSplit = enchant.split(", ");
        				
        				int enchantId = Integer.parseInt(enchantSplit[0]);
        				
        				int enchantLevel = 1;
        				if (enchantSplit.length >= 2) {
        					Integer.parseInt(enchantSplit[1]);
        				}
        				
        				itemStack.addEnchantment(Enchantment.getById(enchantId), enchantLevel);
        			}
        		}
    	    	catch (Exception err) {
    	    	}
        	}
    		
        	return itemStack;
    	}
    	
    	return null;
    }
    
    public static PotionEffect loadPotionEffect(Map<?, ?> map) {
    	
    	if (map.containsKey("effect")) {
    		PotionEffectType type = PotionEffectType.getByName(((String)map.get("effect")).trim().toUpperCase());
    		
    		int duration = 2000;
    		int amplifier = 0;
    		
    		if (map.containsKey("duration")) {
    			duration = (Integer)map.get("duration");
    		}
    		
    		if (map.containsKey("amplifier")) {
    			amplifier = (Integer)map.get("amplifier");
    		}
    		
    		if (type != null) {
    			return new PotionEffect(type, duration, amplifier);
    		}
    	}
    	
    	return null;
    }
    
    /**
     * Loads an outfit from a YML config file.
     * @param map
     * @return
     */
    @SuppressWarnings("unchecked")
	public static Outfit loadOutfit(Map<?, ?> map) {
    	Outfit newOutfit = new Outfit();
    	
    	if (map.containsKey("name")) {
    		newOutfit.name = (String)map.get("name");
    	}
    	
    	if (map.containsKey("chestplate")) {
    		newOutfit.chestplate = ConfigLoader.loadItem((Map<?, ?>)map.get("chestplate"));
    	}
    	
    	if (map.containsKey("leggings")) {
    		newOutfit.leggings = ConfigLoader.loadItem((Map<?, ?>)map.get("leggings"));
    	}
    	
    	if (map.containsKey("boots")) {
    		newOutfit.boots = ConfigLoader.loadItem((Map<?, ?>)map.get("boots"));
    	}
    	
    	if (map.containsKey("items")) {
	    	for (Map<?, ?> item : ((List<Map<?, ?>>)map.get("items"))) {
	    		newOutfit.items.add(ConfigLoader.loadItem(item));
	    	}
    	}
    	
    	if (map.containsKey("potions")) {
    		for (Map<?, ?> potionEffect : ((List<Map<?, ?>>)map.get("potions"))) {
    			PotionEffect newPotionEffect = loadPotionEffect(potionEffect);
    			
    			if (newPotionEffect != null) {
    				newOutfit.potions.add(newPotionEffect);
    				
    				System.out.println(newPotionEffect);
    			}
    		}
    	}
    	
    	//new PotionEffect(PotionEffectType.getByName("SLOW"), 6000, 1);
    	
    	
    	
    	return newOutfit;
    }
}
