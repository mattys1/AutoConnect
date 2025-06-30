package github.mattys1.autoconnect;

import github.mattys1.autoconnect.keybinds.KeyBinder;
import github.mattys1.autoconnect.keybinds.KeyBinds;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventHandler {
	private static final Logger log = LoggerFactory.getLogger(EventHandler.class);

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onPlayerJoin(final PlayerLoggedInEvent event) {
		Log.info("Player logged in, event received: {}", event);

		final TextComponentString message = new TextComponentString("xd");

		event.player.sendMessage(message);
	}

	@SubscribeEvent
	public void onEvent(KeyInputEvent event) {
		EntityPlayer player = Minecraft.getMinecraft().player;

		final var pressedBind = KeyBinder.bindings.entrySet().stream()
			.filter(bind -> bind.getValue().isPressed())
			.findFirst(); // assuming that only one bind can be pressed in a tick

		player.sendMessage(new TextComponentString("keybind pressed: ".concat(pressedBind.toString())));

		pressedBind.ifPresent(entry -> {
			final KeyBinds keyCode = entry.getKey();

			switch (keyCode) {
				case BEGIN_CONNECTION:
					Log.error("begin connection");
					assert false : "begin connection not implemented";
					break;
				case CONFIRM_CONNECTION:
					assert false : "confirm connection not implemented";
				break;
				case CANCEL_CONNECTION:
					assert false : "cancel connection not implemented";
				break;

				default:
					break;
			}
		});

	}
}
