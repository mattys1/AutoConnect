package github.mattys1.autoconnect.connection.pathing;

import net.minecraft.util.math.BlockPos;

// this is dumb, but basically the only thing that should diffrentiate vertices is the encoded positions, so only that
// needs to be compared and hashed.
public class BlockPosVertex {
    public static final long INFINITY = 1000000000;

    public BlockPos pos;
    public long rhs = INFINITY;
    public long g = INFINITY;
//    public ArrayList<BlockPosVertex> edges;

    public BlockPosVertex(BlockPos aPos) {
        pos = aPos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockPosVertex vertex = (BlockPosVertex) o;
        return pos.equals(vertex.pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }

}
