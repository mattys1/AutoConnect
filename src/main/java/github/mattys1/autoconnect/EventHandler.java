package github.mattys1.autoconnect;

import github.mattys1.autoconnect.connection.ConnectionManager;
import github.mattys1.autoconnect.connection.ConnectionPosition;
import github.mattys1.autoconnect.keybinds.KeyBinder;
import github.mattys1.autoconnect.keybinds.KeyBinds;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Optional;

public class EventHandler {
	private static final ConnectionManager connectionManager = new ConnectionManager();

	private Vec3d previousPlayerEyePos = null; // TODO: this should be moved

	private Optional<ConnectionPosition> getWhatPlayerIsLookingAt() {
		EntityPlayer player = Minecraft.getMinecraft().player;

		if(player == null) {
			Log.warn("tried getting look info of player that is null");
			return Optional.empty();
		}

		final Vec3d lookVec = player.getLookVec();
		final Vec3d eyePos = player.getPositionEyes(1.0F);
		final int playerReach = 5;

		RayTraceResult collision = player.world.rayTraceBlocks(eyePos, eyePos.add(lookVec.scale(playerReach)));

		return collision != null ?
				Optional.of(new ConnectionPosition(collision.getBlockPos(), collision.sideHit))
				: Optional.empty();
	}

	@SubscribeEvent
	public void onEvent(TickEvent.PlayerTickEvent event) {
		if(
				event.phase == TickEvent.Phase.START
				|| !connectionManager.active()
		) {
			return;
		}

		final ConnectionPosition playerLookPos = getWhatPlayerIsLookingAt().orElse(null);
		final EntityPlayer player = event.player;

		if(
				playerLookPos == null || playerLookPos.equals(connectionManager.getEndPos())
						&& previousPlayerEyePos.equals(player.getPositionEyes(1.0F))
		) {
			return;
		}

		previousPlayerEyePos = player.getPositionEyes(1.0F);

		connectionManager.updateEndPos(playerLookPos);
	}

	@SubscribeEvent
	public void onEvent(KeyInputEvent event) {

		final var pressedBind = KeyBinder.bindings.entrySet().stream()
			.filter(bind -> bind.getValue().isPressed())
			.findFirst(); // assuming that only one bind can be pressed in a tick

		pressedBind.ifPresent(entry -> {
			final KeyBinds keyCode = entry.getKey();

			switch (keyCode) {
				case BEGIN_CONNECTION: {
					Optional<ConnectionPosition> start = getWhatPlayerIsLookingAt();

					start.ifPresent(connectionManager::beginConnection);
					break;
				}
				case CONFIRM_CONNECTION:{
					connectionManager.confirmConnection();
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
