package github.mattys1.autoconnect.connection.pathing;

import com.google.common.collect.ImmutableSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.jgrapht.graph.DefaultEdge;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RouteBuilder {
    private PathfindingGraph<BlockPosVertex, DefaultEdge> placeableGraph = new PathfindingGraph<>(DefaultEdge.class);
    private Set<BlockPos> oldPlaceables = Collections.emptySet();
    private final BlockPosVertex start;
    private BlockPosVertex end;

    public RouteBuilder(final BlockPos startPos) {
        start = new BlockPosVertex(startPos);
        end = new BlockPosVertex(startPos);

        addPositionsToRoute(ImmutableSet.of(start.pos));
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

        Set<BlockPosVertex> vertsBeforeAdd = Set.copyOf(placeableGraph.vertexSet());

        inNewButNotOld.stream()
                .map(BlockPosVertex::new)
                .forEach(v -> placeableGraph.addVertex(v));

        final var newVertices = placeableGraph.vertexSet().stream()
                .filter(v -> inNewButNotOld.contains(v.pos))
                .collect(Collectors.toUnmodifiableSet());

        /* TODO: if no placeables were removed, we know the bounding box must have only increased therefore, we only need to
        check the sides, not positions in the box */
        for(final var nv : newVertices) {
            for(final var ov : vertsBeforeAdd) {
                BlockPos diff = ov.pos.subtract(nv.pos);
                diff = new BlockPos(
                        Math.abs(diff.getX()),
                        Math.abs(diff.getY()),
                        Math.abs(diff.getZ())
                );

                if(diff.compareTo(new Vec3i(1,1,1)) > 0 || diff.compareTo(new Vec3i(0,0,0)) == 0) {
                    continue;
                }

                var test = Set.copyOf(placeableGraph.vertexSet());
                assert placeableGraph.vertexSet().stream()
                        .anyMatch(v -> v == nv)
                        && placeableGraph.vertexSet().stream()
                        .anyMatch(v -> v == ov)
                        : "what a piece of shit library";

                placeableGraph.addEdge(nv, ov);
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

    // FIXME: this kind of sucks, instead connectionmanager should be replaced with instances of a connection class and the gc should handle cleanup
    public void clear() {
        placeableGraph = new PathfindingGraph<>(DefaultEdge.class);
        oldPlaceables = Collections.emptySet();
    }
}
