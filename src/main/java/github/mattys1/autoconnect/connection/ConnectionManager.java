package github.mattys1.autoconnect.connection;

import com.google.common.collect.ImmutableList;
import github.mattys1.autoconnect.Log;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import oshi.util.tuples.Pair;

import java.util.*;

public class ConnectionManager {
    private boolean isPlanActive = false;
    private ConnectionPosition startPos = null;
    private ConnectionPosition endPos = null;

    public ConnectionPosition getEndPos() { return endPos; }

    private AxisAlignedBB getBoundingBoxOfConnectionArea() {
        return new AxisAlignedBB(startPos.coordinates(), endPos.coordinates());
    }

private ImmutableList<BlockPos> getEmptySpaceAroundBoundingBox(final AxisAlignedBB boundingBox) {
    assert startPos != null && endPos != null : "Attempted to get space without defining connection first";

    final World world = Minecraft.getMinecraft().world;

    assert boundingBox.minX <= boundingBox.maxX &&
           boundingBox.minY <= boundingBox.maxY &&
           boundingBox.minZ <= boundingBox.maxZ
            : String.format("Invalid bounding box: min(%.1f,%.1f,%.1f), max(%.1f,%.1f,%.1f)",
                    boundingBox.minX, boundingBox.minY, boundingBox.minZ,
                    boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);

    final ImmutableList.Builder<BlockPos> placeablePositions = new ImmutableList.Builder<>();
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
        final ImmutableList<BlockPos> placeableList = getEmptySpaceAroundBoundingBox(boundingBox);

//        placeableList.forEach((pos) -> {
//            RenderGlobal.drawBoundingBox(pos.getX() - 0.1, pos.getY() - 0.1, pos.getZ() - 0.1,
//                    pos.getX() + 1.1, pos.getY() + 1.1, pos.getZ() + 1.1, 1.0f, 0.0f, 0.0f, 1.0f);
//        });


        Log.info("placeable list: {}", placeableList);

        startPos = null;
        endPos = null;
        isPlanActive = false;
    }

    public void cancelConnection() {
        assert isPlanActive : "Attempted to cancel connection while one was not in progress";
        assert startPos != null : "Attempted to cancel connection, that hadn't begun";

        Log.info("cancelling connection, {}", startPos);

        startPos = null;
        isPlanActive = false;
    }

    public void updateEndPos(final ConnectionPosition end) {
        assert isPlanActive : "Attempted to update end pos on inactive plan";
        assert startPos != null : "Attempted to update connection end without defined start";

        endPos = end;

        Log.info("updating end pos, {}", endPos);
    }

    public void dbg_renderBoundingBoxOfConnection() {
        if(!isPlanActive || startPos == null || endPos == null) {
            return;
        }

        final var boundingBox = getBoundingBoxOfConnectionArea();
        Log.info("Rendering bounding box: {}", boundingBox);

//        RenderGlobal.drawSelectionBoundingBox(boundingBox, 1.0f, 1.0f, 1.0f, 1.0f);

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
