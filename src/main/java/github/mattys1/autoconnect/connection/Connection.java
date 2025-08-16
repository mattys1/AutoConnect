package github.mattys1.autoconnect.connection;

import github.mattys1.autoconnect.Config;
import github.mattys1.autoconnect.Log;
import github.mattys1.autoconnect.connection.pathing.RouteBuilder;
import github.mattys1.autoconnect.gui.Colours;
import github.mattys1.autoconnect.gui.Messanger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.*;

import static java.lang.Math.abs;

public class Connection {
    private final ConnectionPosition startPos;
    private ConnectionPosition endPos;
    private final RouteBuilder builder;
    private final InventoryManager inventoryManager;
    private List<BlockPos> currentPath = Collections.emptyList();

    private Connection(
            final ConnectionPosition pStartPos,
            InventoryManager invManager
    ) {
        startPos = pStartPos;
        endPos = pStartPos;
        builder = new RouteBuilder(startPos.getAdjacentOfFace());
        inventoryManager = invManager;
    }

    public static Optional<Connection> create(final ConnectionPosition pStartPos, final EntityPlayer player) {
        return InventoryManager.create(player.inventory)
                .map(man -> new Connection(pStartPos, man));
    }

    public void onInventoryUpdate() {

    }

    public ConnectionPosition getEndPos() { return endPos; }

    private AxisAlignedBB getBoundingBoxOfConnectionArea() {
        final Vec3i padding = new Vec3i(Config.SEARCH_MARGIN, Config.SEARCH_MARGIN, Config.SEARCH_MARGIN);
        final AxisAlignedBB box = new AxisAlignedBB(
                startPos.coordinates(),
                endPos.coordinates()
        ).grow(padding.getX(), padding.getY(), padding.getZ());

        return new AxisAlignedBB(
                box.minX, Math.clamp(box.minY, 0, 255), box.minZ,
                box.maxX, Math.clamp(box.maxY, 0, 255), box.maxZ
        );
    }

    private List<BlockPos> getEmptySpaceAroundBoundingBox(final AxisAlignedBB boundingBox) {
        assert startPos != null && endPos != null : "Attempted to get space without defining connection first";

        final World world = Minecraft.getMinecraft().world;

        assert boundingBox.minX <= boundingBox.maxX &&
                boundingBox.minY <= boundingBox.maxY &&
                boundingBox.minZ <= boundingBox.maxZ
                : String.format("Invalid bounding box: min(%.1f,%.1f,%.1f), max(%.1f,%.1f,%.1f)",
                boundingBox.minX, boundingBox.minY, boundingBox.minZ,
                boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);

        final List<BlockPos> placeablePositions = new ArrayList<>();
        for(int x = (int) boundingBox.minX; x <= boundingBox.maxX; x++) {
            for(int y = (int) boundingBox.minY; y <= boundingBox.maxY; y++) {
                for(int z = (int) boundingBox.minZ; z <= boundingBox.maxZ; z++) {
                    final BlockPos pos = new BlockPos(x, y, z);

                    if(world.isAirBlock(pos)) {
                        placeablePositions.add(pos);
                    }
                }
            }
        }

        assert new HashSet<>(placeablePositions).stream().sorted().toList().equals(
            placeablePositions.stream().sorted().toList()
        ) : "space around box contains duplicates";

        return placeablePositions;
    }

    public void confirmConnection() {
        assert startPos != null && endPos != null : "Attempted to confirm connection, but the start vectors were not set";

        Log.info("confirming connection {}", endPos);
//        final List<BlockPos> route = builder.getRoute();

        Log.info("Calculated route to destination: {}", currentPath);

        for(int i = 1; i < currentPath.size(); i++) {
            Minecraft.getMinecraft().world
                    .setBlockState(currentPath.get(i), Blocks.GLASS.getDefaultState());
        }
    }

    public void renderConnectionStatus() {
        if(currentPath.equals(Collections.emptyList())) {
            Messanger.writeAboveHotbar(
                    "Couldn't calculate path to destination!",
                    Colours.RED
            );
        } else {
            Messanger.writeAboveHotbar(String.format(
                    "Building path using: %s. Have %s in inventory of %s needed to build line. %s",
                    new ItemStack(inventoryManager.connectionItem).getDisplayName(),
                    inventoryManager.getConnectionItemCount(),
                    currentPath.size(),
                    inventoryManager.haveEnoughFor(currentPath.size()) ? "" : "Not enough items in inventory!"
            ), inventoryManager.haveEnoughFor(currentPath.size()) ? Colours.GREEN : Colours.YELLOW);
        }
    }

    public void updateEndPos(final ConnectionPosition end) {
        assert startPos != null : "Attempted to update connection end without defined start";

        builder.setGoal(end.coordinates());
        builder.processPortals();

        endPos = end;


//        long start = System.nanoTime();
//        final List<BlockPos> placeables = getEmptySpaceAroundBoundingBox(getBoundingBoxOfConnectionArea());
//        long endTime = System.nanoTime();
//        Log.info("Got empty space in {}ms, with {} air blocks", abs(start - endTime) / 1_000_000., placeables.size());
//
//        start = System.nanoTime();
//        builder.addPositionsToRoute(placeables);
//        endTime = System.nanoTime();
//        Log.info("Built graph in {}ms, with {} blocks", abs(start - endTime) / 1_000_000., placeables.size());
//
//        builder.setGoal(end.getAdjacentOfFace());
//
//        start = System.nanoTime();
//        currentPath = builder.getRoute();
//        endTime = System.nanoTime();
//        Log.info("Got route in {}ms, with {} blocks", abs(start - endTime) / 1_000_000., placeables.size());

//        Log.info("Have {} {}, and need {}", inventoryManager.getConnectionItemCount(), inventoryManager.connectionItem, currentPath.size());
    }

    public void dbg_renderConnectionInWorld() {
//        if(startPos == null || endPos == null) {
//            return;
//        }
//
//        final var boundingBox = getBoundingBoxOfConnectionArea();
//
//        RenderGlobal.drawBoundingBox(
//                boundingBox.minX, boundingBox.minY, boundingBox.minZ,
//                boundingBox.maxX + 1, boundingBox.maxY + 1, boundingBox.maxZ + 1,
//                1.0f, 1.0f, 1.0f, 1.0f
//        );

        builder.dbg_displayChunks();
    }
}
