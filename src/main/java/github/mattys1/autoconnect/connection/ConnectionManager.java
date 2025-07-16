package github.mattys1.autoconnect.connection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import github.mattys1.autoconnect.Config;
import github.mattys1.autoconnect.Log;
import github.mattys1.autoconnect.connection.pathing.RouteBuilder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

public class ConnectionManager {
    private boolean isPlanActive = false;
    private ConnectionPosition startPos = null;
    private ConnectionPosition endPos = null;
    private final RouteBuilder builder = new RouteBuilder();

    public ConnectionPosition getEndPos() { return endPos; }

    private AxisAlignedBB getBoundingBoxOfConnectionArea() {
       final Vec3i padding = new Vec3i(Config.SEARCH_MARGIN, Config.SEARCH_MARGIN, Config.SEARCH_MARGIN);

        return new AxisAlignedBB(
                startPos.coordinates().subtract(padding),
                endPos.coordinates().add(padding)
        );
    }

private ImmutableSet<BlockPos> getEmptySpaceAroundBoundingBox(final AxisAlignedBB boundingBox) {
    assert startPos != null && endPos != null : "Attempted to get space without defining connection first";

    final World world = Minecraft.getMinecraft().world;

    assert boundingBox.minX <= boundingBox.maxX &&
           boundingBox.minY <= boundingBox.maxY &&
           boundingBox.minZ <= boundingBox.maxZ
            : String.format("Invalid bounding box: min(%.1f,%.1f,%.1f), max(%.1f,%.1f,%.1f)",
                    boundingBox.minX, boundingBox.minY, boundingBox.minZ,
                    boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);

    final ImmutableSet.Builder<BlockPos> placeablePositions = ImmutableSet.builder();
    for(int x = (int) boundingBox.minX; x <= boundingBox.maxX; x++) {
        for(int y = (int) boundingBox.minY; y <= boundingBox.maxY; y++) {
            for(int z = (int) boundingBox.minZ; z <= boundingBox.maxZ; z++) {
                final BlockPos pos = new BlockPos(x, y, z);
                final IBlockState block = world.getBlockState(pos);

                if(Objects.requireNonNull(block.getBlock().getRegistryName()).toString().equals("minecraft:air")) {
                    placeablePositions.add(pos);
                }
            }
        }
    }

    return placeablePositions.build();
}

    public void beginConnection(final ConnectionPosition start) {
        assert !isPlanActive : "Attempted to start connection plan while one was in progress";
        assert startPos == null && endPos == null : "Attempted to start connection without proper cleanup of start and end vectors";

        Log.info("beggining connection, {}", start);
        startPos = start;

        isPlanActive = true;
    }

    public void confirmConnection() {
        assert isPlanActive : "Attempted to confirm connection while one was not in progress";
        assert startPos != null && endPos != null : "Attempted to confirm connection, but the start vectors were not set";

        Log.info("confirming connection {}", endPos);

        final var boundingBox = getBoundingBoxOfConnectionArea();
        final ImmutableSet<BlockPos> placeableSet = getEmptySpaceAroundBoundingBox(boundingBox);

//        placeableList.forEach((pos) -> {
//            RenderGlobal.drawBoundingBox(pos.getX() - 0.1, pos.getY() - 0.1, pos.getZ() - 0.1,
//                    pos.getX() + 1.1, pos.getY() + 1.1, pos.getZ() + 1.1, 1.0f, 0.0f, 0.0f, 1.0f);
//        });


        Log.info("placeable list: {}", placeableSet);

        builder.clear();
        startPos = null;
        endPos = null;
        isPlanActive = false;
    }

    public void cancelConnection() {
        assert isPlanActive : "Attempted to cancel connection while one was not in progress";
        assert startPos != null : "Attempted to cancel connection, that hadn't begun";

        Log.info("cancelling connection, {}", startPos);

        builder.clear();
        startPos = null;
        isPlanActive = false;
    }

    public void updateEndPos(final ConnectionPosition end) {
        assert isPlanActive : "Attempted to update end pos on inactive plan";
        assert startPos != null : "Attempted to update connection end without defined start";

        endPos = end;

        final ImmutableSet<BlockPos> placeables = getEmptySpaceAroundBoundingBox(getBoundingBoxOfConnectionArea());

        Log.info("updating end pos, {}", endPos);

        builder.addPositionsToRoute(placeables);
    }

    public void dbg_renderBoundingBoxOfConnection() {
        if(!isPlanActive || startPos == null || endPos == null) {
            return;
        }

        final var boundingBox = getBoundingBoxOfConnectionArea();

        RenderGlobal.drawBoundingBox(
                boundingBox.minX, boundingBox.minY, boundingBox.minZ,
                boundingBox.maxX + 1, boundingBox.maxY + 1, boundingBox.maxZ + 1,
                1.0f, 1.0f, 1.0f, 1.0f
        );

    }

    public boolean active() {
        return isPlanActive;
    }
}
