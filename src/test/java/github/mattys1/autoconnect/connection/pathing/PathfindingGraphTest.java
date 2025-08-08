package github.mattys1.autoconnect.connection.pathing;

import github.mattys1.autoconnect.Log;
import io.netty.handler.timeout.TimeoutException;
import net.minecraft.util.math.AxisAlignedBB;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.junit.jupiter.api.Assumptions;
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

    private void assertEqualsReferencePathLength(PathfindingGraph graph, BlockPosVertex start, BlockPosVertex end) {
       assertEquals(getReferencePath(
               graph, start, end
       ).size(), graph.findPath(end).size());
    }

    private PathfindingGraph getCubeGraph(double x, double y, double z) {
        final AxisAlignedBB box = new AxisAlignedBB(0., 0., 0., x, y, z);
        final PathfindingGraph graph = new PathfindingGraph(new BlockPosVertex(new BlockPos(0,0,0)));

        for(var i = box.minX + 1; i <= box.maxX; i++) {
            for(var j = box.minY; j <= box.maxY; j++) {
                for(var k = box.minZ; k <= box.maxZ; k++) {
                    final BlockPosVertex vert = new BlockPosVertex(new BlockPos(i,j,k));

                    if(vert.pos.equals(new BlockPos(0,0,0))) {
                        continue;
                    }

                    graph.addVertex(vert);
                }
            }
        }

        connectVertices(graph);

        return graph;
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

        assertEqualsReferencePathLength(graph, start, end);
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

        assertEqualsReferencePathLength(graph, start, end);
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

        assertEqualsReferencePathLength(graph, start, v22);
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

        graph.removeVertex(v21);

        connectVertices(graph);

        assertEqualsReferencePathLength(graph, start, v23);

    }

    @Test
    public void testRepeatedCallsInChangingCube() {
        final var start = new BlockPosVertex(new BlockPos(0,0,0));
        final var end = new BlockPosVertex(new BlockPos(3,3,3));

        final var graph = getCubeGraph(3,3,3);
        var path = graph.findPath(end);

        Assumptions.assumeTrue(path.size() == getReferencePath(graph, start, end).size());

        path = graph.findPath(end);
        Log.info(path.toString());

        assertEqualsReferencePathLength(graph, start, end);
    }

    @Test
    public void testPathInCube() {
        final BlockPosVertex end = new BlockPosVertex(new BlockPos(3,3,3));

        final var graph = getCubeGraph(3,3,3);

        connectVertices(graph);

        assertEqualsReferencePathLength(graph, new BlockPosVertex(new BlockPos(0,0,0)), end);
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

        assertEqualsReferencePathLength(graph, start, end);
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
