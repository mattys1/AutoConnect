package github.mattys1.autoconnect;

import github.mattys1.autoconnect.connection.ConnectionManager;
import github.mattys1.autoconnect.connection.ConnectionPosition;
import github.mattys1.autoconnect.keybinds.KeyBinder;
import github.mattys1.autoconnect.keybinds.KeyBinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Optional;

public class EventHandler {
	private final ConnectionManager connectionManager = new ConnectionManager();

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
						|| event.side == Side.SERVER // IMPORTANT
						|| !connectionManager.active()
		) {
			return;
		}

		final Optional<ConnectionPosition> playerLookPos = getWhatPlayerIsLookingAt();
		final EntityPlayer player = event.player;

		if(
				playerLookPos.isEmpty() || playerLookPos.get().equals(connectionManager.getEndPos())
						&& previousPlayerEyePos.equals(player.getPositionEyes(1.0F))
		) {
			return;
		}

		previousPlayerEyePos = player.getPositionEyes(1.0F);

		connectionManager.updateEndPos(playerLookPos.get());
	}

	@SubscribeEvent
	public void onRenderWorldLast(RenderWorldLastEvent event) {
		// Get the player's position
		Minecraft mc = Minecraft.getMinecraft();
		Entity entity = mc.getRenderViewEntity();
		if (entity == null) return;

		// Save the current render state
		GlStateManager.pushMatrix();

		// Adjust rendering for player position (offset by partial ticks)
		double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * event.getPartialTicks();
		double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * event.getPartialTicks();
		double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * event.getPartialTicks();

		// Apply translation to render properly
		GlStateManager.translate(-d0, -d1, -d2);

		// Call your debug render method
		connectionManager.dbg_renderBoundingBoxOfConnection();

		// Restore the render state
		GlStateManager.popMatrix();
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
