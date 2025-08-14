package github.mattys1.autoconnect.connection.pathing;

import com.google.common.collect.ImmutableSet;
import net.minecraft.util.math.BlockPos;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.*;
import java.util.stream.Collectors;

public class RouteBuilder {
    private final SimpleGraph<BlockPosVertex, DefaultEdge> placeableGraph = new SimpleGraph<>(DefaultEdge.class);
    private Set<BlockPos> oldPlaceables = Collections.emptySet();
    private final BlockPosVertex start;
    private BlockPosVertex end;
    private final HashMap<BlockPos, BlockPosVertex> vertexByPos = new HashMap<>();

    public RouteBuilder(final BlockPos startPos) {
        start = new BlockPosVertex(startPos);
        end = new BlockPosVertex(startPos);

        addPositionsToRoute(ImmutableSet.of(start.pos));

        placeableGraph.addVertex(start);
        vertexByPos.put(startPos, start);
    }

    public void addPositionsToRoute(final ImmutableSet<BlockPos> newPlaceables) {
        final Set<BlockPos> inOldButNotNew = oldPlaceables.stream()
                .filter((pos) -> !newPlaceables.contains(pos))
                .collect(Collectors.toUnmodifiableSet());

        final Set<BlockPos> inNewButNotOld = newPlaceables.stream()
                .filter((pos) -> !oldPlaceables.contains(pos))
                .collect(Collectors.toUnmodifiableSet());

        if (inOldButNotNew.isEmpty() && inNewButNotOld.isEmpty()) {
            return;
        }

        final var toRemove = placeableGraph.vertexSet().stream()
                .filter(v -> inOldButNotNew.contains(v.pos))
                .collect(Collectors.toSet());
        placeableGraph.removeAllVertices(toRemove);

        for (final var v : toRemove) {
            vertexByPos.remove(v.pos);
        }

//        Set<BlockPosVertex> vertsBeforeAdd = Set.copyOf(placeableGraph.vertexSet());

        inNewButNotOld.stream()
                .map(BlockPosVertex::new)
                .forEach(v -> {
                    placeableGraph.addVertex(v);
                    vertexByPos.put(v.pos, v);
                });

        final var newVertices = placeableGraph.vertexSet().stream()
                .filter(v -> inNewButNotOld.contains(v.pos))
                .collect(Collectors.toUnmodifiableSet());

        final int[][] neighbourConstants = {
                {1, 0, 0}, {-1, 0, 0},
                {0, 1, 0}, {0, -1, 0},
                {0, 0, 1}, {0, 0, -1}
        };

        for (final var nv : newVertices) {
            for (int[] d : neighbourConstants) {
                BlockPos neighborPos = nv.pos.add(d[0], d[1], d[2]); // single allocation
                BlockPosVertex neighbor = vertexByPos.get(neighborPos);
                if (neighbor != null) {
                    placeableGraph.addEdge(nv, neighbor);
                }
            }
        }

        oldPlaceables = newPlaceables;

        assert placeableGraph.vertexSet().stream().map(v -> v.pos).collect(Collectors.toUnmodifiableSet()).size()
                == placeableGraph.vertexSet().stream().map(v -> v.pos).toList().size()
                : String.format(
                "There are duplicates in the graph, set size %d, list size %d",
                placeableGraph.vertexSet().stream().map(v -> v.pos).collect(Collectors.toUnmodifiableSet()).size(),
                placeableGraph.vertexSet().stream().map(v -> v.pos).toList().size()
        );

        assert ((java.util.function.BooleanSupplier) () -> {
            var actualList = placeableGraph.vertexSet().stream()
                    .map(v -> v.pos)
                    .sorted()
                    .toList();
            var expectedList = oldPlaceables.stream()
                    .sorted()
                    .toList();

            if (actualList.equals(expectedList)) {
                return true;
            }

            var extras = actualList.stream()
                    .filter(p -> !expectedList.contains(p))
                    .toList();
            var missing = expectedList.stream()
                    .filter(p -> !actualList.contains(p))
                    .toList();

            throw new AssertionError(String.format(
                    "Vertex graph not equal to position list on update graph:%n  extras: %s%n  missing: %s",
                    extras,
                    missing
            ));
        }).getAsBoolean();

        assert vertexByPos.size() == placeableGraph.vertexSet().size() : String.format("Vertex map and vertexes in graph sizes differ, map: %d, graph: %d", vertexByPos.size(), placeableGraph.vertexSet().size());
    }

public void setGoal(BlockPos end) {
    final BlockPosVertex goal = new BlockPosVertex(end);
    assert placeableGraph.containsVertex(goal) : String.format("Attempting to make a nonexistent vertex the goal, vertex set: %s, goal: %s",
            placeableGraph.vertexSet().stream().map(v -> v.pos).toList(), goal.pos);

    this.end = goal;
}

public List<BlockPos> getRoute() {
    final var path = new AStarShortestPath<>(
                placeableGraph,
                (v1, v2) -> {
                    BlockPos pos1 = v1.pos;
                    BlockPos pos2 = v2.pos;
                    return Math.abs(pos1.getX() - pos2.getX()) +
                            Math.abs(pos1.getY() - pos2.getY()) +
                            Math.abs(pos1.getZ() - pos2.getZ());
                }).getPath(start, end);

        return path != null ? path.getVertexList().stream().map(v -> v.pos).toList() : Collections.emptyList();
    }
}
