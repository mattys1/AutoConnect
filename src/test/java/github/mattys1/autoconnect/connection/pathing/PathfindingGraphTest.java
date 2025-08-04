package github.mattys1.autoconnect.connection.pathing;

import github.mattys1.autoconnect.Log;
import io.netty.handler.timeout.TimeoutException;
import net.minecraft.util.math.AxisAlignedBB;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
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
    private void connectVertices(PathfindingGraph graph) {
        for(final var v1 : graph.vertexSet()) {
            for(final var v2 : graph.vertexSet()) {
                BlockPos diff = v2.pos.subtract(v1.pos);
                int absX = Math.abs(diff.getX());
                int absY = Math.abs(diff.getY());
                int absZ = Math.abs(diff.getZ());

                if((absX == 1 && absY == 0 && absZ == 0) ||
                        (absX == 0 && absY == 1 && absZ == 0) ||
                        (absX == 0 && absY == 0 && absZ == 1)) {
                    graph.addEdge(v1, v2);
                }
            }
        }
    }

    private List<BlockPos> getReferencePath(PathfindingGraph graph, BlockPosVertex start, BlockPosVertex end) {
        final var path = new AStarShortestPath<>(
                graph,
                (v1, v2) -> {
                    BlockPos pos1 = v1.pos;
                    BlockPos pos2 = v2.pos;
                    return Math.abs(pos1.getX() - pos2.getX()) +
                            Math.abs(pos1.getY() - pos2.getY()) +
                            Math.abs(pos1.getZ() - pos2.getZ());
                }).getPath(start, end).getVertexList().stream().map(v -> v.pos).toList();
        return path.subList(1, path.size() - 1);
    }

    @Test
    public void testSimplePath() {
        BlockPosVertex start = new BlockPosVertex(new BlockPos(0, 56, 0));
        BlockPosVertex middle = new BlockPosVertex(new BlockPos(1, 56, 0));
        BlockPosVertex end = new BlockPosVertex(new BlockPos(2, 56, 0));

        PathfindingGraph graph = new PathfindingGraph(start);

        graph.addVertex(middle);
        graph.addVertex(end);
        connectVertices(graph);

        List<BlockPos> path = graph.findPath(end);

//        assertEquals(2, path.size());
//        assertEquals(new BlockPos(1, 0, 0), path.get(0));
//        assertEquals(new BlockPos(2, 0, 0), path.get(1));
        assertEquals(getReferencePath(graph, start, end), path);
    }

    @Test
    public void testNotWorkingSimplePath() {
        BlockPosVertex start = new BlockPosVertex(new BlockPos(-2638, 56, 379));
        BlockPosVertex middle = new BlockPosVertex(new BlockPos(-2638, 57, 379));
        BlockPosVertex end = new BlockPosVertex(new BlockPos(-2639, 57, 379));

        PathfindingGraph graph = new PathfindingGraph(start);

        graph.addVertex(middle);
        graph.addVertex(end);
        connectVertices(graph);

        List<BlockPos> path = graph.findPath(end);

//        assertEquals(Stream.of(middle, end).map(v -> v.pos).toList(), path);
        assertEquals(getReferencePath(graph, start, end), path);
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

        connectVertices(graph);

        List<BlockPos> path = graph.findPath(v22);

//        assertEquals(4, path.size());
//        assertEquals(List.of(
//                v10.pos,
//                v20.pos,
//                v21.pos,
//                v22.pos
//        ), path);
        assertEquals(getReferencePath(graph, start, v22), path);
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

        connectVertices(graph);

        final BlockPosVertex v23 = new BlockPosVertex(new BlockPos(2, 3, 0));
        final BlockPosVertex v32 = new BlockPosVertex(new BlockPos(3, 2, 0));

        graph.addVertex(v23);
        graph.addVertex(v32);

        connectVertices(graph);

        graph.removeVertex(v21);

        List<BlockPos> path = graph.findPath(v23);

//        assertEquals(List.of(
//                v01.pos,
//                v02.pos,
//                v12.pos,
//                v22.pos,
//                v23.pos
//        ), path);

        assertEquals(getReferencePath(graph, start, v23), path);

    }

    @Test
    public void testPathInCube() {
        final AxisAlignedBB box = new AxisAlignedBB(new BlockPos(0, 0 ,0), new BlockPos(3, 3, 3));
        final BlockPosVertex start = new BlockPosVertex(new BlockPos(box.minX, box.minY, box.minZ));
        final BlockPosVertex end = new BlockPosVertex(new BlockPos(box.maxX, box.maxY, box.maxZ));

        final PathfindingGraph graph = new PathfindingGraph(start);

        for(var x = box.minX + 1; x <= box.maxX; x++) {
            for(var y = box.minY; y <= box.maxY; y++) {
                for(var z = box.minZ; z <= box.maxZ; z++) {
                    final BlockPosVertex vert = new BlockPosVertex(new BlockPos(x,y,z));

                    if(vert.equals(start)) {
                        continue;
                    }

                    graph.addVertex(vert);
                }
            }
        }

        connectVertices(graph);

        final List<BlockPos> path = graph.findPath(end);

        assertEquals(getReferencePath(graph, start, end), path);
    }

    @Test
    public void testDumbLeak() {
        BlockPosVertex start = new BlockPosVertex(new BlockPos(-2642, 57, 383));
        BlockPosVertex end = new BlockPosVertex(new BlockPos(-2644, 56, 383));

        PathfindingGraph graph = new PathfindingGraph(start);

        List<BlockPosVertex> vertices = List.of(
                new BlockPosVertex(new BlockPos(-2643, 57, 383)),
                new BlockPosVertex(new BlockPos(-2641, 57, 384)),
                new BlockPosVertex(new BlockPos(-2641, 56, 383)),
                new BlockPosVertex(new BlockPos(-2642, 56, 384)),
                new BlockPosVertex(new BlockPos(-2641, 56, 384)),
                new BlockPosVertex(new BlockPos(-2641, 57, 382)),
                new BlockPosVertex(new BlockPos(-2642, 56, 382)),
                new BlockPosVertex(new BlockPos(-2643, 57, 384)),
                new BlockPosVertex(new BlockPos(-2641, 57, 383)),
                new BlockPosVertex(new BlockPos(-2641, 56, 382)),
                new BlockPosVertex(new BlockPos(-2642, 57, 384)),
                new BlockPosVertex(new BlockPos(-2643, 56, 384)),
                new BlockPosVertex(new BlockPos(-2643, 56, 383)),
                new BlockPosVertex(new BlockPos(-2643, 56, 382)),
                new BlockPosVertex(new BlockPos(-2643, 57, 382)),
                new BlockPosVertex(new BlockPos(-2642, 57, 382)),
                new BlockPosVertex(new BlockPos(-2644, 57, 384)),
                new BlockPosVertex(new BlockPos(-2644, 57, 383)),
                new BlockPosVertex(new BlockPos(-2644, 57, 382)),
                new BlockPosVertex(new BlockPos(-2644, 56, 384)),
                new BlockPosVertex(new BlockPos(-2644, 56, 382)),
                new BlockPosVertex(new BlockPos(-2645, 57, 384)),
                new BlockPosVertex(new BlockPos(-2645, 56, 384)),
                new BlockPosVertex(new BlockPos(-2645, 57, 383)),
                new BlockPosVertex(new BlockPos(-2645, 57, 382)),
                new BlockPosVertex(new BlockPos(-2645, 56, 382)),
                end
        );

        for (BlockPosVertex vertex : vertices) {
            graph.addVertex(vertex);
        }

        connectVertices(graph);

        List<BlockPos> path = graph.findPath(end);

//        assertNotEquals(path, Collections.emptyList());
        assertEquals(getReferencePath(graph, start, end), path);
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
