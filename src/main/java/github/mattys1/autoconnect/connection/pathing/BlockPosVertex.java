package github.mattys1.autoconnect.connection.pathing;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// this is dumb, but basically the only thing that should diffrentiate vertices is the encoded positions, so only that
// needs to be compared.
public class BlockPosVertex {
    public BlockPos pos;
    public int rhs = Integer.MAX_VALUE;
    public int g = Integer.MAX_VALUE;
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
