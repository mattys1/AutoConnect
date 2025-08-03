package github.mattys1.autoconnect.connection;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.net.PortUnreachableException;

public record ConnectionPosition(BlockPos coordinates, EnumFacing face) {
    public BlockPos getAdjacentOfFace() {
        final var pos = this.coordinates;

        switch(this.face) {
            case DOWN -> {
                return new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ());
            }
            case UP -> {
                return new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
            }
            case NORTH -> {
                return new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1);
            }
            case SOUTH -> {
                return new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1);
            }
            case WEST -> {
                return new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ());
            }
            case EAST -> {
                return new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ());
            }
        }

        throw new IllegalStateException("Unexpected face:" + this.face);
    }
}