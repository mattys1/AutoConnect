package github.mattys1.autoconnect.connection.pathing;

import com.google.common.collect.ImmutableList;
import edu.princeton.cs.algorithms.IndexMinPQ;
import github.mattys1.autoconnect.Log;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.NeighborCache;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.spongepowered.asm.mixin.Debug;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.*;

record Key(long k1, long k2) {
    public int compare(final Key other) {
        int k1Comparison = Long.compare(this.k1, other.k1);
        if(k1Comparison != 0) {
            return k1Comparison;
        }

        return Long.compare(this.k2, other.k2);
    }
}

// TODO: making this mutable and not creating new objects on every update may save on performance, question is how much
record BlockPosVertexEntry(BlockPosVertex vert, Key key) implements Comparable<BlockPosVertexEntry> {
    @Override
    public int compareTo(@NotNull BlockPosVertexEntry other) {
        return this.key.compare(other.key);
    }
}

record EntryHandle(BlockPosVertex vert, int handle) {}

class PathfindingGraph extends SimpleGraph<BlockPosVertex, DefaultEdge> {
    private final BlockPosVertex start;
    private BlockPosVertex oldGoal;
    private final IndexMinPQ<BlockPosVertexEntry> inconsistent;
    private final NeighborCache<BlockPosVertex, DefaultEdge> neighborCache;
    private final HashMap<BlockPosVertex, Integer> handleMap; // yeah
    private int topIdx;

    private long heuristic(final BlockPosVertex vertex) {
        return Math.abs(vertex.pos.getX() - start.pos.getX()) +
               Math.abs(vertex.pos.getY() - start.pos.getY()) +
               Math.abs(vertex.pos.getZ() - start.pos.getZ());
    }

    private Key calculateKey(final BlockPosVertex vertex) {
        return new Key(
                Math.min(vertex.g, vertex.rhs) + heuristic(vertex),
                Math.min(vertex.g, vertex.rhs)
        );
    }
//    public PathfindingGraph() {
//        super(DefaultEdge.class);
//    }

    private Key queueTopKey() {
        assert !inconsistent.isEmpty();
        assert inconsistent.minKey() != null;

        return inconsistent.minKey().key();
    }

    private EntryHandle queueTopVert() {
        assert inconsistent.minKey() != null;

        return new EntryHandle(inconsistent.minKey().vert(), inconsistent.minIndex());
    }

    private void pushTop(BlockPosVertexEntry entry) {
        inconsistent.insert(topIdx, entry);
        handleMap.put(entry.vert(), topIdx);

        topIdx++; // will overflow, doesn't matter
    }

    private void popTop() {
        final BlockPosVertexEntry entry = inconsistent.minKey();

        inconsistent.delMin();
        handleMap.remove(entry.vert());

    }

    private void updateVertex(BlockPosVertex vert, int handle) {
        if(vert.g != vert.rhs) {
            if(handle != -1 && inconsistent.contains(handle)) {
                inconsistent.changeKey(handle, new BlockPosVertexEntry(vert, calculateKey(vert)));
            } else {
                pushTop(new BlockPosVertexEntry(vert, calculateKey(vert)));
            }
        } else if(handle != -1 && inconsistent.contains(handle)) {
            inconsistent.delete(handle);
        }
    }

    private void computeShortestPath(final BlockPosVertex end) {
        while(
                (!inconsistent.isEmpty() &&
                queueTopKey().compare(calculateKey(start)) < 0)
                        || start.rhs != start.g
        ) {
            final EntryHandle top = queueTopVert();
            final Key keyOld = queueTopKey();
            final Key keyNew = calculateKey(top.vert());

            if(keyOld.compare(keyNew) < 0) {
                inconsistent.changeKey(top.handle(), new BlockPosVertexEntry(
                        top.vert(),
                        keyNew
                ));
            } else if(top.vert().g > top.vert().rhs) {
                top.vert().g = top.vert().rhs;
                popTop();

                final List<BlockPosVertex> neighbours = neighborCache.neighborListOf(top.vert());
                for(final var neighbour : neighbours) {
                    if(!neighbour.equals(end)) {
                        neighbour.rhs = Math.min(neighbour.rhs, 1 + top.vert().g); // this can probably be optimized since we know that the movement cost to every neighbour is 1
                    }

//                    assert handleMap.get(neighbour) != null : String.format("Can't get smallest neighbour from handle map, map: %s\n neighbour: %s, neighbours: %s", handleMap.toString(), neighbour.toString(), neighbours);
                    if(handleMap.getOrDefault(neighbour, -1) == null) {
                        Log.info("Neighbour not in inconsistent, queue: {}, neighbour: {}, handlemap: {}", inconsistent, neighbour, handleMap);
                    }

                    updateVertex(neighbour, handleMap.getOrDefault(neighbour, -1));
                }
            } else {
                final long gOld = top.vert().g;
                top.vert().g = BlockPosVertex.INFINITY;

                final List<BlockPosVertex> neighbours = neighborCache.neighborListOf(top.vert());
                neighbours.add(top.vert());
                for(final var neighbour : neighbours) {
                    if(neighbour.rhs == 1 + gOld && !neighbour.equals(end)) {
                        final var minDistance = neighbours.stream()
                                .map(n -> {
                                    int vertsToCross = 0;

                                    if(neighborCache.neighborsOf(neighbour).contains(n)) {
                                       vertsToCross = 1;
                                    } else if(!neighbour.equals(n)) {
                                        vertsToCross = 2; // have to traverse to `top` and then have to move to `n`
                                    }

                                    return vertsToCross + n.g;
                                }).min(Long::compareTo);

                        neighbour.rhs = minDistance.orElseThrow();
                    }

                    updateVertex(neighbour, handleMap.getOrDefault(neighbour, -1));
                }
            }
        }
    }

    public PathfindingGraph(BlockPosVertex oStart) {
        super(DefaultEdge.class);
        neighborCache = new NeighborCache<>(this);
        handleMap = new HashMap<>();

        start = oStart;
//        start.rhs = 0;
        inconsistent = new IndexMinPQ<>(10000000); // should be big enough for everyone

        oldGoal = start;
        oldGoal.rhs = 0;

        pushTop(new BlockPosVertexEntry(oldGoal, new Key(heuristic(oldGoal), 0)));
        this.addVertex(start);

        topIdx = 1;
    }

    @Override
    public boolean removeVertex(BlockPosVertex v) {
        Integer handle = handleMap.get(v);
        if(handle != null) {
            assert inconsistent.contains(handle) : "handle in handle map during removal but not in queue";

            inconsistent.delete(handle);
            handleMap.remove(v);
        }

        return super.removeVertex(v);
    }

    public List<BlockPos> findPath(final BlockPosVertex end) {
        assert this.containsVertex(end) : "Graph doesn't contain end vertex for pathfinding";

        Log.info("Finding path in graph: {}", this);
        Log.info("Graph has positions: {}, start: {}, end {}", this.vertexSet().stream().map(v -> v.pos).toList(), this.start.pos, end.pos);

        oldGoal.rhs = BlockPosVertex.INFINITY;

        updateVertex(oldGoal, handleMap.getOrDefault(oldGoal, -1));
        end.rhs = 0;
        updateVertex(end, -1);
        assert !inconsistent.isEmpty() : "Inconsistent is empty after calling updateVertex in findPath";

        computeShortestPath(end);
        Log.info("Graph state after computeShortestPath: {}", this);

        final ArrayList<BlockPos> path = new ArrayList<>();
//        final HashSet<BlockPos> visited = new HashSet<>();

        BlockPosVertex next = start;
        if (next.g >= BlockPosVertex.INFINITY) {
            return path;
        }

        while(!next.equals(end)) {
            List<BlockPosVertex> neighbors = neighborCache.neighborListOf(next);
            if(neighbors.contains(end)) {
                next = end;
            } else {
                next = neighbors.stream()
                        .filter(v -> v.g < BlockPosVertex.INFINITY)
                        .min(Comparator.comparingLong(v -> v.g))
                        .orElse(new BlockPosVertex(new BlockPos(-1,-1,-1)));
            }

            if(next.g >= BlockPosVertex.INFINITY || next.pos.equals(end.pos)) {
                return path;
            }

            assert !path.contains(next.pos) :
                    String.format("Path cycle detected: current path: %s, duplicate vertex: %s, graph: %s, goal %s",
                            path, next, this, end
                    );

            Log.info("Finalizing path retrieval, position: {}", next.pos);
            path.add(next.pos);
        }

        oldGoal = end;

        assert !path.contains(end.pos) : "path contains the end vertex even though it shouldn't";
        return path;
    }

    @Override
    public String toString() {
        return String.format("Graph[vertices: %s, edges: %s start: %s]", vertexSet(), edgeSet(), start);
    }
}