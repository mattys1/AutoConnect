package github.mattys1.autoconnect.connection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import github.mattys1.autoconnect.Config;
import github.mattys1.autoconnect.Log;
import github.mattys1.autoconnect.connection.pathing.RouteBuilder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.*;

public class Connection {
    private ConnectionPosition startPos;
    private ConnectionPosition endPos;
    private final RouteBuilder builder;

    public Connection(final ConnectionPosition pStartPos) {
        startPos = pStartPos;
        endPos = pStartPos;
        builder = new RouteBuilder(startPos.getAdjacentOfFace());

        Log.info("beggining connection, {}", pStartPos);
    }

    public ConnectionPosition getEndPos() { return endPos; }

    private AxisAlignedBB getBoundingBoxOfConnectionArea() {
       final Vec3i padding = new Vec3i(Config.SEARCH_MARGIN, Config.SEARCH_MARGIN, Config.SEARCH_MARGIN);

        return new AxisAlignedBB(
                startPos.coordinates(),
                endPos.coordinates()
        ).grow(padding.getX(), padding.getY(), padding.getZ());
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

    public void confirmConnection() {
        assert startPos != null && endPos != null : "Attempted to confirm connection, but the start vectors were not set";

        Log.info("confirming connection {}", endPos);

        final List<BlockPos> route = builder.getRoute();

        Log.info("Calculated route to destination: {}", route);

        for(final var pos : route) {
            Minecraft.getMinecraft().world
//                    .spawnParticle(
//                            EnumParticleTypes.SMOKE_LARGE,
//                            pos.getX() + 0.5,              // x coordinate (center of block)
//                            pos.getY() + 0.5,              // y coordinate
//                            pos.getZ() + 0.5,              // z coordinate
//                            0.0,                           // x velocity
//                            0.0,                           // y velocity
//                            0.0                            // z velocity
//                    );
                    .setBlockState(pos, Blocks.GLASS.getDefaultState());
        }

//        final var boundingBox = getBoundingBoxOfConnectionArea();
//        final ImmutableSet<BlockPos> placeableSet = getEmptySpaceAroundBoundingBox(boundingBox);
//
////        placeableList.forEach((pos) -> {
////            RenderGlobal.drawBoundingBox(pos.getX() - 0.1, pos.getY() - 0.1, pos.getZ() - 0.1,
////                    pos.getX() + 1.1, pos.getY() + 1.1, pos.getZ() + 1.1, 1.0f, 0.0f, 0.0f, 1.0f);
////        });
//
//
//        Log.info("placeable list: {}", placeableSet);
//
//        builder.clear();
//        startPos = null;
//        endPos = null;
    }

    public void updateEndPos(final ConnectionPosition end) {
        assert startPos != null : "Attempted to update connection end without defined start";

        endPos = end;

        final ImmutableSet<BlockPos> placeables = getEmptySpaceAroundBoundingBox(getBoundingBoxOfConnectionArea());

        Log.info("updating end pos, {}", endPos);

        builder.addPositionsToRoute(placeables);
        builder.setGoal(end.getAdjacentOfFace());
    }

    public void dbg_renderBoundingBoxOfConnection() {
        if(startPos == null || endPos == null) {
            return;
        }

        final var boundingBox = getBoundingBoxOfConnectionArea();

        RenderGlobal.drawBoundingBox(
                boundingBox.minX, boundingBox.minY, boundingBox.minZ,
                boundingBox.maxX + 1, boundingBox.maxY + 1, boundingBox.maxZ + 1,
                1.0f, 1.0f, 1.0f, 1.0f
        );

    }
}
