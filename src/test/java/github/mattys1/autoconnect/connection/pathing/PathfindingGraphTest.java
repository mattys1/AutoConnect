package github.mattys1.autoconnect.connection.pathing;

import github.mattys1.autoconnect.Log;
import io.netty.handler.timeout.TimeoutException;
import org.junit.jupiter.api.Test;
import net.minecraft.util.math.BlockPos;
import org.openjdk.nashorn.internal.ir.annotations.Ignore;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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
    public void testNotWorkingSimplePath() {
        BlockPosVertex start = new BlockPosVertex(new BlockPos(-2638, 56, 379));
        BlockPosVertex middle = new BlockPosVertex(new BlockPos(-2638, 57, 379));
        BlockPosVertex end = new BlockPosVertex(new BlockPos(-2639, 57, 379));

        PathfindingGraph graph = new PathfindingGraph(start);

        graph.addVertex(middle);
        graph.addVertex(end);
        graph.addEdge(start, middle);
        graph.addEdge(middle, end);

        List<BlockPos> path = graph.findPath(end);

        assertEquals(Stream.of(middle, end).map(v -> v.pos).toList(), path);
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
    public void testRepeatedCall2D() {
        final BlockPosVertex start = new BlockPosVertex(new BlockPos(0, 0, 0));
        final PathfindingGraph graph = new PathfindingGraph(start);

        final BlockPosVertex v10 = new BlockPosVertex(new BlockPos(1, 0, 0));
        final BlockPosVertex v20 = new BlockPosVertex(new BlockPos(2, 0, 0));
        final BlockPosVertex v01 = new BlockPosVertex(new BlockPos(0, 1, 0));
        final BlockPosVertex v21 = new BlockPosVertex(new BlockPos(2, 1, 0));
        final BlockPosVertex v02 = new BlockPosVertex(new BlockPos(0, 2, 0));
        final BlockPosVertex v12 = new BlockPosVertex(new BlockPos(1, 2, 0));
        final BlockPosVertex v22 = new BlockPosVertex(new BlockPos(2, 2, 0));

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

        final BlockPosVertex v23 = new BlockPosVertex(new BlockPos(2, 3, 0));
        final BlockPosVertex v32 = new BlockPosVertex(new BlockPos(3, 2, 0));

        graph.addVertex(v23);
        graph.addEdge(v22, v23);

        graph.addVertex(v32);
        graph.addEdge(v22, v32);

        graph.removeVertex(v21);

        List<BlockPos> path = graph.findPath(v23);

        assertEquals(List.of(
                v01.pos,
                v02.pos,
                v12.pos,
                v22.pos,
                v23.pos
        ), path);

    }

    @Test
    public void testDumbLeak() {
    BlockPos startPos = new BlockPos(-2642, 57, 383);
    BlockPos endPos = new BlockPos(-2644, 56, 383);

    BlockPosVertex start = new BlockPosVertex(startPos);
    PathfindingGraph graph = new PathfindingGraph(start);

    Map<BlockPos, BlockPosVertex> vertexMap = new HashMap<>();
    vertexMap.put(startPos, start);

    List<BlockPos> positions = List.of(
        new BlockPos(-2643, 57, 383),
        new BlockPos(-2641, 57, 384),
        new BlockPos(-2641, 56, 383),
        new BlockPos(-2642, 56, 384),
        new BlockPos(-2641, 56, 384),
        new BlockPos(-2641, 57, 382),
        new BlockPos(-2642, 56, 382),
        new BlockPos(-2643, 57, 384),
        new BlockPos(-2641, 57, 383),
        new BlockPos(-2641, 56, 382),
        new BlockPos(-2642, 57, 384),
        new BlockPos(-2643, 56, 384),
        new BlockPos(-2643, 56, 383),
        new BlockPos(-2643, 56, 382),
        new BlockPos(-2643, 57, 382),
        new BlockPos(-2642, 57, 382),
        new BlockPos(-2644, 57, 384),
        new BlockPos(-2644, 57, 383),
        new BlockPos(-2644, 57, 382),
        new BlockPos(-2644, 56, 384),
        new BlockPos(-2644, 56, 382),
        new BlockPos(-2645, 57, 384),
        new BlockPos(-2645, 56, 384),
        new BlockPos(-2645, 57, 383),
        new BlockPos(-2645, 57, 382),
        new BlockPos(-2645, 56, 382)
    );

        for (BlockPos pos : positions) {
            if (!vertexMap.containsKey(pos)) {
                BlockPosVertex vertex = new BlockPosVertex(pos);
                graph.addVertex(vertex);
                vertexMap.put(pos, vertex);
            }
        }

        for (BlockPos pos1 : positions) {
            for (BlockPos pos2 : positions) {
                if (!pos1.equals(pos2) && isAdjacent(pos1, pos2)) {
                    graph.addEdge(vertexMap.get(pos1), vertexMap.get(pos2));
                }
            }
        }

        BlockPosVertex end = vertexMap.get(endPos);

        List<BlockPos> path = graph.findPath(end);

        assertNotEquals(path, Collections.emptyList());
}

private boolean isAdjacent(BlockPos pos1, BlockPos pos2) {
    int dx = Math.abs(pos1.getX() - pos2.getX());
    int dy = Math.abs(pos1.getY() - pos2.getY());
    int dz = Math.abs(pos1.getZ() - pos2.getZ());
    return (dx + dy + dz == 1);
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
