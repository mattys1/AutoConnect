package github.mattys1.autoconnect.connection.pathing;

import com.google.common.collect.ImmutableSet;
import net.minecraft.util.math.BlockPos;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RouteBuilder {
    private final SimpleGraph<BlockPosVertex, DefaultEdge> placeableGraph = new SimpleGraph<>(DefaultEdge.class);
    private Set<BlockPos> oldPlaceables = Collections.emptySet();
    private final BlockPosVertex start;
    private BlockPosVertex end;

    public RouteBuilder(final BlockPos startPos) {
        start = new BlockPosVertex(startPos);
        end = new BlockPosVertex(startPos);

        addPositionsToRoute(ImmutableSet.of(start.pos));

        placeableGraph.addVertex(start);
    }

    public void addPositionsToRoute(final ImmutableSet<BlockPos> newPlaceables) {
        final Set<BlockPos> inOldButNotNew = oldPlaceables.stream()
                .filter((pos) -> !newPlaceables.contains(pos))
                .collect(Collectors.toUnmodifiableSet());

        final Set<BlockPos> inNewButNotOld = newPlaceables.stream()
                .filter((pos) -> !oldPlaceables.contains(pos))
                .collect(Collectors.toUnmodifiableSet());

        if(inOldButNotNew.isEmpty() && inNewButNotOld.isEmpty()) { return; }

        final var toRemove = placeableGraph.vertexSet().stream()
                        .filter(v -> inOldButNotNew.contains(v.pos))
                        .collect(Collectors.toSet());
        placeableGraph.removeAllVertices(toRemove);

//        Set<BlockPosVertex> vertsBeforeAdd = Set.copyOf(placeableGraph.vertexSet());

        inNewButNotOld.stream()
                .map(BlockPosVertex::new)
                .forEach(v -> placeableGraph.addVertex(v));

        final var newVertices = placeableGraph.vertexSet().stream()
                .filter(v -> inNewButNotOld.contains(v.pos))
                .collect(Collectors.toUnmodifiableSet());

        /* TODO: if no placeables were removed, we know the bounding box must have only increased therefore, we only need to
        check the sides, not positions in the box */
        for(final var nv : newVertices) {
            for(final var v : placeableGraph.vertexSet()) {
                BlockPos diff = v.pos.subtract(nv.pos);
                int absX = Math.abs(diff.getX());
                int absY = Math.abs(diff.getY());
                int absZ = Math.abs(diff.getZ());

                if((absX == 1 && absY == 0 && absZ == 0) ||
                        (absX == 0 && absY == 1 && absZ == 0) ||
                        (absX == 0 && absY == 0 && absZ == 1)) {
                    placeableGraph.addEdge(nv, v);
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
        // add differences to the graph
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
