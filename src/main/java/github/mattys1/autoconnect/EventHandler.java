package github.mattys1.autoconnect;

import github.mattys1.autoconnect.connection.ConnectionManager;
import github.mattys1.autoconnect.connection.ConnectionPosition;
import github.mattys1.autoconnect.keybinds.KeyBinder;
import github.mattys1.autoconnect.keybinds.KeyBinds;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Optional;
import java.util.function.Supplier;

public class EventHandler {
	private static final ConnectionManager connectionManager = new ConnectionManager();

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

		final Supplier<Optional<ConnectionPosition>> getWhatPlayerIsLookingAt = () -> {
			final Vec3d lookVec =  player.getLookVec();
			final Vec3d eyePos = player.getPositionEyes(1.0F);
			final int playerReach = 5;

			RayTraceResult collision = player.world.rayTraceBlocks(eyePos, eyePos.add(lookVec.scale(playerReach)));

			return collision != null ?
					Optional.of(new ConnectionPosition(collision.getBlockPos(), collision.sideHit))
					: Optional.empty();
		};

		pressedBind.ifPresent(entry -> {
			final KeyBinds keyCode = entry.getKey();

			switch (keyCode) {
				case BEGIN_CONNECTION: {
					Optional<ConnectionPosition> start = getWhatPlayerIsLookingAt.get();

					start.ifPresent(connectionManager::beginConnection);
					break;
				}
				case CONFIRM_CONNECTION:{
					Optional<ConnectionPosition> start = getWhatPlayerIsLookingAt.get();

					start.ifPresent(connectionManager::confirmConnection);
					break;
				}
				case CANCEL_CONNECTION: {
					connectionManager.cancelConnection();
					break;
				}

				default:
					break;
			}
		});

	}
}
