package github.mattys1.autoconnect.connection.pathing;

import org.junit.jupiter.api.Test;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class PathfindingGraphTest {

    @Test
    public void testSimplePath() {
        BlockPosVertex start = new BlockPosVertex(new BlockPos(0, 0, 0));
        BlockPosVertex middle = new BlockPosVertex(new BlockPos(1, 0, 0));
        BlockPosVertex end = new BlockPosVertex(new BlockPos(2, 0, 0));

        PathfindingGraph graph = new PathfindingGraph(start);

        graph.addVertex(middle);
        graph.addVertex(end);
        graph.addEdge(start, middle);
        graph.addEdge(middle, end);

        List<BlockPos> path = graph.findPath(end);

        assertEquals(2, path.size());
        assertEquals(new BlockPos(1, 0, 0), path.get(0));
        assertEquals(new BlockPos(2, 0, 0), path.get(1));
    }

    @Test
    public void test2DPathWithObstacle() {
        BlockPosVertex start = new BlockPosVertex(new BlockPos(0, 0, 0));
        PathfindingGraph graph = new PathfindingGraph(start);

        BlockPosVertex v10 = new BlockPosVertex(new BlockPos(1, 0, 0));
        BlockPosVertex v20 = new BlockPosVertex(new BlockPos(2, 0, 0));
        BlockPosVertex v01 = new BlockPosVertex(new BlockPos(0, 1, 0));
        BlockPosVertex v21 = new BlockPosVertex(new BlockPos(2, 1, 0));
        BlockPosVertex v02 = new BlockPosVertex(new BlockPos(0, 2, 0));
        BlockPosVertex v12 = new BlockPosVertex(new BlockPos(1, 2, 0));
        BlockPosVertex v22 = new BlockPosVertex(new BlockPos(2, 2, 0));

        graph.addVertex(v10);
        graph.addVertex(v20);
        graph.addVertex(v01);
        graph.addVertex(v21);
        graph.addVertex(v02);
        graph.addVertex(v12);
        graph.addVertex(v22);

        graph.addEdge(start, v10);
        graph.addEdge(v10, v20);
        graph.addEdge(v02, v12);
        graph.addEdge(v12, v22);

        graph.addEdge(start, v01);
        graph.addEdge(v01, v02);
        graph.addEdge(v20, v21);
        graph.addEdge(v21, v22);

        List<BlockPos> path = graph.findPath(v22);

        assertEquals(4, path.size());
        assertEquals(List.of(
                v10.pos,
                v20.pos,
                v21.pos,
                v22.pos
        ), path);
    }

    @Test
    public void testNoPath() {
        PathfindingGraph graph = new PathfindingGraph(new BlockPosVertex(new BlockPos(0, 0, 0)));
        BlockPosVertex unreachable = new BlockPosVertex(new BlockPos(10, 10, 0));
        graph.addVertex(unreachable);

        List<BlockPos> path = graph.findPath(unreachable);

        assertTrue(path.isEmpty());
    }
}
