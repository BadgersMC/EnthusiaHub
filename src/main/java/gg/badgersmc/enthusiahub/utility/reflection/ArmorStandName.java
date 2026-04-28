package gg.badgersmc.enthusiahub.utility.reflection;

import org.bukkit.entity.ArmorStand;

public class ArmorStandName {

	public static String getName(ArmorStand stand) {
		return stand.getCustomName();
	}
}
