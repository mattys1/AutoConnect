package github.mattys1.autoconnect.connection.pathing;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.*;
import java.util.stream.Collectors;

public class RouteBuilder {
    //    private final SimpleGraph<BlockPosVertex, DefaultEdge> placeableGraph = new SimpleGraph<>(DefaultEdge.class);
    private Set<BlockPos> oldPlaceables = Collections.emptySet();
    private final BlockPosVertex start;
    private BlockPosVertex end;
    private final Long2ObjectOpenHashMap<BlockPosVertex> vertexByPos = new Long2ObjectOpenHashMap<>();

    public RouteBuilder(final BlockPos startPos) {
        start = new BlockPosVertex(startPos);
        end = new BlockPosVertex(startPos);

        addPositionsToRoute(List.of(start.pos));

        vertexByPos.put(startPos.toLong(), start);
    }

    public void addPositionsToRoute(final List<BlockPos> newPlaceables) {
//        final Set<BlockPos> inOldButNotNew = oldPlaceables.stream()
//                .filter((pos) -> !newPlaceables.contains(pos))
//                .collect(Collectors.toUnmodifiableSet());
//
//        final Set<BlockPos> inNewButNotOld = newPlaceables.stream()
//                .filter((pos) -> !oldPlaceables.contains(pos))
//                .collect(Collectors.toUnmodifiableSet());
//
//        if (inOldButNotNew.isEmpty() && inNewButNotOld.isEmpty()) {
//            return;
//        }
//
//        final var toRemove = vertexByPos.values().stream()
//                .filter(v -> inOldButNotNew.contains(v.pos))
//                .collect(Collectors.toSet());
//
//        for (final var v : toRemove) {
//            vertexByPos.remove(v.pos.toLong());
//        }
//
////        Set<BlockPosVertex> vertsBeforeAdd = Set.copyOf(placeableGraph.vertexSet());
//
//        inNewButNotOld.stream()
//                .map(BlockPosVertex::new)
//                .forEach(v -> {
//                    vertexByPos.put(v.pos.toLong(), v);
//                });
//
//        oldPlaceables = newPlaceables;
//
//        assert vertexByPos.values().stream().map(v -> v.pos).collect(Collectors.toUnmodifiableSet()).size()
//                == vertexByPos.values().stream().map(v -> v.pos).toList().size()
//                : String.format(
//                "There are duplicates in the graph, set size %d, list size %d",
//                vertexByPos.values().stream().map(v -> v.pos).collect(Collectors.toUnmodifiableSet()).size(),
//                vertexByPos.values().stream().map(v -> v.pos).toList().size()
//        );
//
//        assert ((java.util.function.BooleanSupplier) () -> {
//            var actualList = vertexByPos.values().stream()
//                    .map(v -> v.pos)
//                    .sorted()
//                    .toList();
//            var expectedList = oldPlaceables.stream()
//                    .sorted()
//                    .toList();
//
//            if (actualList.equals(expectedList)) {
//                return true;
//            }
//
//            var extras = actualList.stream()
//                    .filter(p -> !expectedList.contains(p))
//                    .toList();
//            var missing = expectedList.stream()
//                    .filter(p -> !actualList.contains(p))
//                    .toList();
//
//            throw new AssertionError(String.format(
//                    "Vertex map not equal to position list on update graph:%n  extras: %s%n  missing: %s",
//                    extras,
//                    missing
//            ));
//        }).getAsBoolean();
        vertexByPos.clear();
        for(final var pos : newPlaceables) {
            vertexByPos.put(pos.toLong(), new BlockPosVertex(pos));
        }
    }

    public void setGoal(BlockPos end) {
        final BlockPosVertex goal = new BlockPosVertex(end);
        assert vertexByPos.containsValue(goal) : String.format("Attempting to make a nonexistent vertex the goal, vertex set: %s, goal: %s",
                vertexByPos.values().stream().map(v -> v.pos).toList(), goal.pos);

        this.end = goal;
    }

    public List<BlockPos> getRoute() {
//    final var path = new AStarShortestPath<>(
//                placeableGraph,
//                (v1, v2) -> {
//                    BlockPos pos1 = v1.pos;
//                    BlockPos pos2 = v2.pos;
//                    return Math.abs(pos1.getX() - pos2.getX()) +
//                            Math.abs(pos1.getY() - pos2.getY()) +
//                            Math.abs(pos1.getZ() - pos2.getZ());
//                }).getPath(start, end);
//
//        return path != null ? path.getVertexList().stream().map(v -> v.pos).toList() : Collections.emptyList();
        return Collections.emptyList();
    }

}