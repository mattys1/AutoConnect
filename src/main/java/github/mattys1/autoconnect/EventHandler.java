package github.mattys1.autoconnect;

import github.mattys1.autoconnect.connection.Connection;
import github.mattys1.autoconnect.connection.ConnectionPosition;
import github.mattys1.autoconnect.keybinds.KeyBinder;
import github.mattys1.autoconnect.keybinds.KeyBinds;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Optional;

public class EventHandler {
	private Optional<Connection> connection = Optional.empty(); // TODO: support multiple connections in a queue. once one finishes, the other one is calculated.

	private Optional<Vec3d> previousPlayerEyePos = Optional.empty(); // TODO: this should be moved

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
	public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if(
				connection.isEmpty()
						|| event.phase == TickEvent.Phase.START
						|| event.side == Side.SERVER // IMPORTANT
		) {
			return;
		}

		final Optional<ConnectionPosition> playerLookPos = getWhatPlayerIsLookingAt();
		final EntityPlayer player = event.player;

		if(
				playerLookPos.isEmpty() || playerLookPos.get().equals(connection.get().getEndPos())
						&& previousPlayerEyePos.isPresent()
						&& new BlockPos(previousPlayerEyePos.get()).equals(new BlockPos(player.getPositionEyes(1.0F)))
		) {
			return;
		}

		final var world = Minecraft.getMinecraft().world;
		final BlockPos adjacentBlock = playerLookPos.get().getAdjacentOfFace();
		if(!world.isAirBlock(adjacentBlock)) {
			return;
		}

		previousPlayerEyePos = Optional.ofNullable(player.getPositionEyes(1.0f));

		connection.get().updateEndPos(playerLookPos.get());
	}

	@SubscribeEvent
	public void onRenderWorldLast(RenderWorldLastEvent event) {
		if(connection.isEmpty()) return;

		Minecraft mc = Minecraft.getMinecraft();
		Entity entity = mc.getRenderViewEntity();
		if (entity == null) return;

		GlStateManager.pushMatrix();

		double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * event.getPartialTicks();
		double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * event.getPartialTicks();
		double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * event.getPartialTicks();

		GlStateManager.translate(-d0, -d1, -d2);

		connection.get().dbg_renderBoundingBoxOfConnection();

		GlStateManager.popMatrix();
	}

	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {
		final var pressedBind = KeyBinder.bindings.entrySet().stream()
			.filter(bind -> bind.getValue().isPressed())
			.findFirst(); // assuming that only one bind can be pressed in a tick

		pressedBind.ifPresent(entry -> {
			final KeyBinds keyCode = entry.getKey();

			switch (keyCode) {
				case BEGIN_CONNECTION: {
					Optional<ConnectionPosition> start = getWhatPlayerIsLookingAt();

					start.ifPresent(pos -> { connection = Optional.of(new Connection(pos)); });
					break;
				}
				case CONFIRM_CONNECTION:{
					connection.ifPresent(Connection::confirmConnection);
					connection = Optional.empty();
					break;
				}
				case CANCEL_CONNECTION: {
					connection = Optional.empty();
					break;
				}

				default:
					break;
			}
		});

	}
}
