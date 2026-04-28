package net.badgersmc.enthusiahub.action.actions;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.action.Action;
import org.bukkit.entity.Player;

@Deprecated
public class BungeeAction implements Action {

	@Override
	public String getIdentifier() {
		return "BUNGEE";
	}

	@Override
	public void execute(EnthusiaHubPlugin plugin, Player player, String data) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("ConnectOther");
		out.writeUTF(player.getName());
		out.writeUTF(data);
		player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
	}
}
