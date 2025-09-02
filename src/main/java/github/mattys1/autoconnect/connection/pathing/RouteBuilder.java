package github.mattys1.autoconnect.connection.pathing;

import github.mattys1.autoconnect.Config;
import github.mattys1.autoconnect.Log;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.*;
import java.util.stream.Collectors;

public class RouteBuilder {
    private final BlockPos start;
    private BlockPos end = null;
    private BlockPos oldEnd = null; // hack?
    private final Set<Cluster> clusters = new HashSet<>();
    private final Long2ObjectOpenHashMap<Cluster> clusterByPosition = new Long2ObjectOpenHashMap<>();
    private final SimpleWeightedGraph<Portal, DefaultWeightedEdge> portalGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

    public RouteBuilder(final BlockPos startPos) {
        start = startPos;
//        end = startPos;

//        addPositionsToRoute(List.of(start.pos));
    }

    private List<Cluster> getNeighboursOf(final Cluster cluster) {
        final List<Vec3i> neighbourVectors = List.of(
                new Vec3i(1, 0, 0),
                new Vec3i(0, 0, 1),
                new Vec3i(-1, 0, 0),
                new Vec3i(0, 0, -1),
                new Vec3i(0, 1, 0),
                new Vec3i(0, -1, 0)
        );

        final List<Cluster> neighbours = new ArrayList<>();
        for(final var neighbourOffset : neighbourVectors) {
            final Vec3i neighbourPos = new Vec3i(
                    cluster.pos.getX() + neighbourOffset.getX(),
                    cluster.pos.getY() + neighbourOffset.getY(),
                    cluster.pos.getZ() + neighbourOffset.getZ()
            );

            final Cluster neighbour = clusterByPosition.get(Cluster.getPackedPos(neighbourPos));
            if(neighbour != null) neighbours.add(neighbour);
        }

        return neighbours;
    }

    // start with chunks in the bounding box between start and end
    private void setClustersBetweenStartAndGoal() {
        final Cluster startCluster = Cluster.fromBlockCoordinates(start.getX(), start.getY(), start.getZ());
        final Cluster endCluster = Cluster.fromBlockCoordinates(end.getX(), end.getY(), end.getZ());

        clusters.add(startCluster);
        clusters.add(endCluster);

        clusterByPosition.put(startCluster.getPackedPos(), startCluster);
        clusterByPosition.put(endCluster.getPackedPos(), endCluster);

        final Vec3i mins = new Vec3i(
                Math.min(startCluster.pos.getX(), endCluster.pos.getX()),
                Math.min(startCluster.pos.getY(), endCluster.pos.getY()),
                Math.min(startCluster.pos.getZ(), endCluster.pos.getZ())
        );

        final Vec3i maxes = new Vec3i(
                Math.max(startCluster.pos.getX(), endCluster.pos.getX()),
                Math.max(startCluster.pos.getY(), endCluster.pos.getY()),
                Math.max(startCluster.pos.getZ(), endCluster.pos.getZ())
        );

        for(var x = mins.getX(); x <= maxes.getX(); x++) {
            for(var y = mins.getY(); y <= maxes.getY(); y++) {
                for(var z = mins.getZ(); z <= maxes.getZ(); z++) {
                    final Cluster cluster = Cluster.fromChunkCoordinates(x, y, z);
                    if(clusters.contains(cluster)) {
                        continue;
                    }

                    clusters.add(cluster);
                    clusterByPosition.put(startCluster.getPackedPos(), startCluster);
                }
            }
        }

        final Set<Cluster> outside = clusters.stream().filter(c -> c.pos.getX() < mins.getX() || c.pos.getX() > maxes.getX()
                || c.pos.getY() < mins.getY() || c.pos.getY() > maxes.getY()
                || c.pos.getZ() < mins.getZ() || c.pos.getZ() > maxes.getZ()).collect(Collectors.toSet());

        for(final var c : outside) {
            getNeighboursOf(c).forEach( neigbhour -> {
                        if(outside.contains(neigbhour)) {
                            return;
                        }

                        neigbhour.removePortalsWith(c);
                    }
            );

            clusterByPosition.remove(c.getPackedPos());
        }

        clusters.removeAll(outside);
    }

    // this should check only new and edge clusters in the future
    public void processPortals() {
        if(!hasGoalChangedChunk()) {
            return;
        }

        for(final Cluster cluster : clusters) {
            for(final Cluster cluster1 : clusters) {
                if(!cluster.isAdjacent(cluster1)) {
                    continue;
                }

                cluster.definePortals(cluster1);
            }
        }
    };

    final boolean hasGoalChangedChunk() {
        return oldEnd == null || !Cluster.fromBlockCoordinates(end.getX(), end.getY(), end.getZ()).pos.equals(
                Cluster.fromBlockCoordinates(oldEnd.getX(), oldEnd.getY(), oldEnd.getZ()).pos
        ); // dumb
    }

    public void setGoal(final BlockPos end) {
        oldEnd = this.end;
//        assert vertexByPos.containsValue(goal) : String.format("Attempting to make a nonexistent vertex the goal, vertex set: %s, goal: %s",
//                vertexByPos.values().stream().map(v -> v.pos).toList(), goal.pos);
        this.end = end;

        if(hasGoalChangedChunk()) {
            setClustersBetweenStartAndGoal();
            Log.info("Chunks between start and goal: {}", clusters);
        }
    }

    public void dbg_displayChunks() {
        for(final var chunk : clusters) {
            final AxisAlignedBB box = chunk.getChunkBounds();

            chunk.dbg_renderPortals();

            RenderGlobal.drawBoundingBox(
                    box.minX, box.minY, box.minZ,
                    box.maxX + 1, box.maxY + 1, box.maxZ + 1,
                    1.0f, 1.0f, 1.0f, 1.0f
            );
        }
    }

    public List<BlockPos> getRoute() {
        if(hasGoalChangedChunk()) {
            for(final var cluster : clusters) {
                cluster.addPathsBeetwenPortals(portalGraph);
                cluster.setRouteBetweenBlockAndPortals(start, portalGraph);
            }
        }

        if(clusters.size() <= 1) {
            return clusters.stream().findFirst().get().findPath(start, end);
        }

        clusters.forEach(c -> c.setRouteBetweenBlockAndPortals(end, portalGraph));

        final Cluster startCluster = Cluster.fromBlockCoordinates(start.getX(), start.getY(), start.getZ());
        final Cluster endCluster = Cluster.fromBlockCoordinates(end.getX(), end.getY(), end.getZ());

        Log.info("Portal graph: {}", portalGraph);

        final var path = new DijkstraShortestPath<>(portalGraph).getPath(
                new Portal(startCluster, startCluster, start, start),
                new Portal(endCluster, endCluster, end, end)
        );
        if(path == null) {
//            return Collections.emptyList();
        }

        assert path.getVertexList().size() != 1 : "Path shouldn't go through only one portal, path: " + path.getVertexList();

        ArrayList<BlockPos> fullPath = new ArrayList<>();
        for(int i = 1; i < path.getVertexList().size(); i++) {
            final Portal entry = path.getVertexList().get(i - 1);
            final Portal exit = path.getVertexList().get(i);

            fullPath.addAll(entry.getConnectingRoute(exit).orElseThrow());
        }

//        return path != null ? path.getVertexList().stream().flatMap() : Collections.emptyList();
        return fullPath;

//        return Collections.emptyList();
    }

}