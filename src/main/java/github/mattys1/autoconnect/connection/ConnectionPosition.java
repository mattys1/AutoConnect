package github.mattys1.autoconnect.connection;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public record ConnectionPosition(BlockPos coordinates, EnumFacing face) {}