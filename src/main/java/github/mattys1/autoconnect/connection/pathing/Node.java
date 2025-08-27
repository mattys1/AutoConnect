package github.mattys1.autoconnect.connection.pathing;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

class Node {
    public int g = 1_000_000_000;
    public int f;
    public int h;
    public BlockPos parent = null;
    public Set<EnumFacing> validCandidateInSides = new HashSet<>();
    public Set<EnumFacing> traversedInSides = new HashSet<>();
}
