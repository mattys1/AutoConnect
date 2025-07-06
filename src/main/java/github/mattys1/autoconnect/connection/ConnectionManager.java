package github.mattys1.autoconnect.connection;

import com.google.common.collect.ImmutableList;
import github.mattys1.autoconnect.Log;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.BiFunction;

public class ConnectionManager {
    private boolean isPlanActive = false;
    private ConnectionPosition startPos = null;
    private ConnectionPosition endPos = null;

    public ConnectionPosition getEndPos() { return endPos; }

    private ImmutableList<BlockPos> getEmptySpaceAroundConnectionArea() {
        assert startPos != null && endPos != null : "Attempted to get space without defining connection first";

        final BiFunction<BlockPos, BlockPos, BlockPos> assembleSmallerBlockPos =
                (b1, b2) -> new BlockPos(
                        Math.min(b1.getX(),b2.getX()),
                        Math.min(b1.getY(),b2.getY()),
                        Math.min(b1.getZ(),b2.getZ())
                );

        final BiFunction<BlockPos, BlockPos, BlockPos> assembleBiggerBlockPos =
                (b1, b2) -> new BlockPos(
                        Math.max(b1.getX(),b2.getX()),
                        Math.max(b1.getY(),b2.getY()),
                        Math.max(b1.getZ(),b2.getZ())
                );
        final World world = Minecraft.getMinecraft().world;

        final BlockPos smallerVals = assembleSmallerBlockPos.apply(startPos.coordinates(), endPos.coordinates());
        final BlockPos biggerVals = assembleBiggerBlockPos.apply(startPos.coordinates(), endPos.coordinates());

        assert smallerVals.getX() <= biggerVals.getX()
                && smallerVals.getY() <= biggerVals.getY()
                && smallerVals.getZ() <= biggerVals.getZ()
                : String.format("Smaller: %s, bigger: %s", smallerVals, biggerVals);

        final ImmutableList.Builder<BlockPos> placeablePositions = new ImmutableList.Builder<>();// assume it's only air for now
        for(int x = smallerVals.getX(); x <= biggerVals.getX(); x++) {
            for(int y = smallerVals.getY(); y <= biggerVals.getY(); y++) {
                for(int z = smallerVals.getZ(); z <= biggerVals.getZ(); z++) {
                    final IBlockState block = world.getBlockState(new BlockPos(x, y, z));

                    if(Objects.requireNonNull(block.getBlock().getRegistryName()).toString().equals("minecraft:air")) {
                        placeablePositions.add(new BlockPos(x, y, z));
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

        final ImmutableList<BlockPos> placeableList = getEmptySpaceAroundConnectionArea();

        placeableList.forEach((pos)
                -> Minecraft.getMinecraft().world.spawnParticle(
                        EnumParticleTypes.VILLAGER_HAPPY, pos.getX(), pos.getY(), pos.getZ(), 0, 0, 0
        ));


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

    public boolean active() {
        return isPlanActive;
    }
}
