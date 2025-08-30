package github.mattys1.autoconnect.connection.pathing;

import com.jcraft.jorbis.Block;
import github.mattys1.autoconnect.Log;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jgrapht.Graph;
import org.jgrapht.alg.util.UnorderedPair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jheaps.annotations.VisibleForTesting;
import org.lwjgl.opengl.GL11;

import javax.swing.text.html.Option;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

class Cluster {
    public final Vec3i pos;
    private final Long2ObjectOpenHashMap<Node> nodeByPosition = new Long2ObjectOpenHashMap<>();
    private final EnumMap<EnumFacing, List<Portal>> portalsBySide = new EnumMap<>(EnumFacing.class);
    private final HashMap<UnorderedPair<Portal, Portal>, List<BlockPos>> routeBeetweenPortals = new HashMap<>();
    private Optional<Portal> blockPortal = Optional.empty(); // terrible

    private void definePlaceablesInChunk() {
        final AxisAlignedBB boundingBox = getChunkBounds();
        final World world = Minecraft.getMinecraft().world;

        assert boundingBox.minX <= boundingBox.maxX &&
                boundingBox.minY <= boundingBox.maxY &&
                boundingBox.minZ <= boundingBox.maxZ
                : String.format("Invalid bounding box: min(%.1f,%.1f,%.1f), max(%.1f,%.1f,%.1f)",
                boundingBox.minX, boundingBox.minY, boundingBox.minZ,
                boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);

        for (int x = (int) boundingBox.minX; x <= boundingBox.maxX; x++) {
            for (int y = (int) boundingBox.minY; y <= boundingBox.maxY; y++) {
                for (int z = (int) boundingBox.minZ; z <= boundingBox.maxZ; z++) {
                    final BlockPos pos = new BlockPos(x, y, z);

                    if (world.isAirBlock(pos)) {
                        assert world.isValid(pos) : "Attempting to put invalid block pos into chunk";
                        nodeByPosition.put(pos.toLong(), new Node());
                    }
                }
            }
        }
    }

    public void setRouteBetweenBlockAndPortals(final BlockPos pos, SimpleWeightedGraph<Portal, DefaultWeightedEdge> portalGraph) {
        if(!Cluster.fromBlockCoordinates(pos.getX(), pos.getY(), pos.getZ()).equals(this)) {
//            assert !Cluster.fromBlockCoordinates(pos.getX(), pos.getY(), pos.getZ()).equals(this) : String.format("fuck me, created: %s, this: %s, portal graph: %s", Cluster.fromBlockCoordinates(pos.getX(), pos.getY(), pos.getZ()), this, portalGraph.toString());
            Log.info("Pos: {} not contained in cluster: {}", pos, this);
            return;
        }

        final List<Portal> allPortals = new ArrayList<>(portalsBySide.values().stream().flatMap(List::stream).toList());
        final Portal portalFromPos = new Portal(this, this, pos, pos); // hack

        if(blockPortal.isPresent() && !blockPortal.get().equals(portalFromPos)) {
//            portalGraph.vertexSet().stream().filter(Portal::isBlock).findAny().ifPresent(portalGraph::removeVertex);
            blockPortal = Optional.of(portalFromPos);
        }

        // not sure if we should always add it
        if(blockPortal.isEmpty() && allPortals.isEmpty()) {
            portalGraph.addVertex(portalFromPos);
            blockPortal = Optional.of(portalFromPos);
        }
        
        for(final Portal portal : allPortals) {
            final var pair = new UnorderedPair<>(portalFromPos, portal);
            if (routeBeetweenPortals.get(pair) != null) {
                continue;
            }

            final List<BlockPos> route = findPath(portalFromPos.getPortalPosForCluster(this), portal.getPortalPosForCluster(this)); // TODO: FIXME: handle invalid routes
            if (route.isEmpty()) continue;

            routeBeetweenPortals.put(pair, route);

            portalGraph.addVertex(portalFromPos);
            portalGraph.addVertex(portal);
            final var edge = portalGraph.addEdge(portalFromPos, portal);
            if(edge == null) {
                continue;
            }

            portalGraph.setEdgeWeight(edge, route.size());
        }
    }

    public Optional<List<BlockPos>> getRouteBetweenPortals(final Portal start, final Portal goal) {
//        assert routeBeetweenPortals.get(new UnorderedPair<>(start, goal)) != null :
//                String.format("Route between portals, start: %s and end: %s does not exist.", start, goal);

        return Optional.ofNullable(routeBeetweenPortals.get(new UnorderedPair<>(start, goal)));
    }

    public List<BlockPos> findPath(final BlockPos start, final BlockPos goal) {
        assert nodeByPosition.get(start.toLong()) != null && nodeByPosition.get(goal.toLong()) != null : "Cluster doesnt contain start or goal";

        for(Node node : nodeByPosition.values()) {
            node.g = 1_000_000_000;
            node.f = 0;
            node.h = 0;
            node.parent = null;
        }

        final BiFunction<BlockPos, BlockPos, Integer> heuristic =
                (a, b) -> Math.abs(a.getX() - b.getX())
                        + Math.abs(a.getY() - b.getY())
                        + Math.abs(a.getZ() - b.getZ());

        final Function<BlockPos, List<BlockPos>> reconstructPath = (BlockPos currentPos) -> {
            final List<BlockPos> path = new ArrayList<>();
            path.add(start);

            while(currentPos != null && !currentPos.equals(start) /*&& !start.equals(nodeByPosition.get(currentPos.toLong()).parent) */) {
                assert !path.contains(currentPos) : String.format("Path cycle detected, %s %n start: %s, goal %s", path, start, goal);
                path.add(currentPos);
                currentPos = nodeByPosition.get(currentPos.toLong()).parent;
            }

            return path;
        };

        PriorityQueue<BlockPos> openQueue = new PriorityQueue<>(
                Comparator
                        .comparingInt((BlockPos p) -> {
                            Node n = nodeByPosition.get(p.toLong());
                            return n.f;
                        })
                        .thenComparingInt(p -> {
                            Node n = nodeByPosition.get(p.toLong());
                            return n.h;
                        })
        );
        Set<BlockPos> openSet = new HashSet<>();
        Set<BlockPos> closedSet = new HashSet<>();

        final Node startNode = nodeByPosition.get(start.toLong());
        startNode.g = 0;
        startNode.h = heuristic.apply(start, goal);
        startNode.f = startNode.g + startNode.h;

        openQueue.add(start);
        openSet.add(start);

        while(!openQueue.isEmpty()) {
            final BlockPos currentPos = openQueue.poll();
            openSet.remove(currentPos);

            if (currentPos.equals(goal)) {
                return reconstructPath.apply(currentPos);
            }

            closedSet.add(currentPos);
            final Node current = nodeByPosition.get(currentPos.toLong());

            for(final BlockPos neighbourPos : getNeighboursOf(currentPos)) {
                if (closedSet.contains(neighbourPos)) {
                    continue;
                }

                final Node neighbour = nodeByPosition.get(neighbourPos.toLong());
                final int candidateG = current.g + heuristic.apply(currentPos, neighbourPos);

                if (!openSet.contains(neighbourPos)) {
                    neighbour.parent = currentPos;
                    neighbour.g = candidateG;
                    neighbour.h = heuristic.apply(neighbourPos, goal);
                    neighbour.f = neighbour.g + neighbour.h;

                    openQueue.add(neighbourPos);
                    openSet.add(neighbourPos);
                } else if(candidateG < neighbour.g) {
                    neighbour.parent = currentPos;
                    neighbour.g = candidateG;
                    neighbour.f = candidateG + neighbour.h;

                    openQueue.remove(neighbourPos);
                    openQueue.add(neighbourPos);
                }
            }
        }

        return Collections.emptyList();
    }

    private Vec3i positionDifference(Cluster other) {
        int dx = other.pos.getX() - this.pos.getX();
        int dy = other.pos.getY() - this.pos.getY();
        int dz = other.pos.getZ() - this.pos.getZ();

        return new Vec3i(dx, dy, dz);
    }

    // TODO: actually use inbuilt functions in the enum
    private AxisAlignedBB getEdge(EnumFacing direction) {
        AxisAlignedBB b = getChunkBounds();
        return switch (direction) {
            case UP -> new AxisAlignedBB(b.minX, b.maxY, b.minZ, b.maxX, b.maxY, b.maxZ);
            case DOWN -> new AxisAlignedBB(b.minX, b.minY, b.minZ, b.maxX, b.minY, b.maxZ);
            case NORTH -> new AxisAlignedBB(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.minZ);
            case SOUTH -> new AxisAlignedBB(b.minX, b.minY, b.maxZ, b.maxX, b.maxY, b.maxZ);
            case EAST -> new AxisAlignedBB(b.maxX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
            case WEST -> new AxisAlignedBB(b.minX, b.minY, b.minZ, b.minX, b.maxY, b.maxZ);
        };
    }

    private void putPortal(EnumFacing adjacentDirection, Portal portal) {
        if (portalsBySide.containsKey(adjacentDirection)) {
            portalsBySide.get(adjacentDirection).add(portal);
        } else {
            portalsBySide.put(adjacentDirection, new ArrayList<>(List.of(portal)));
        }
    }

    private List<BlockPos> getNeighboursOf(final BlockPos pos) {
        final List<Vec3i> neighbourVectors = List.of(
                new Vec3i(1, 0, 0),
                new Vec3i(0, 0, 1),
                new Vec3i(-1, 0, 0),
                new Vec3i(0, 0, -1),
                new Vec3i(0, 1, 0),
                new Vec3i(0, -1, 0)
        );

        final List<BlockPos> neighbours = new ArrayList<>();
        for (final var neighbourOffset : neighbourVectors) {
            final BlockPos neighbourPos = new BlockPos(
                    pos.getX() + neighbourOffset.getX(),
                    pos.getY() + neighbourOffset.getY(),
                    pos.getZ() + neighbourOffset.getZ()
            );

            if (nodeByPosition.get(neighbourPos.toLong()) != null) {
                neighbours.add(neighbourPos);
            }
        }

        return neighbours;
    }

    // TODO: handle portals for adjacent
    private void fillPortal(BlockPos nodePos, Cluster adjacent, EnumFacing adjacentDirection) {
        final var startNode = nodeByPosition.get(nodePos.toLong());
        assert startNode != null;
        if (startNode.traversedInSides.contains(adjacentDirection)) return;

        final List<BlockPos> validPositions = new ArrayList<>(); // for postprocessing down the line
        final Stack<BlockPos> nodePositions = new Stack<>();
        nodePositions.add(nodePos);
        final Vec3i diffWithAdjacent = positionDifference(adjacent);

        List<Vec3i> neighbourNodeOffsets = switch(adjacentDirection) {
            case UP, DOWN -> List.of(
                    new Vec3i(1, 0, 0),
                    new Vec3i(0, 0, 1),
                    new Vec3i(-1, 0, 0),
                    new Vec3i(0, 0, -1)
            );
            case NORTH, SOUTH -> List.of(
                    new Vec3i(1, 0, 0),
                    new Vec3i(0, 1, 0),
                    new Vec3i(-1, 0, 0),
                    new Vec3i(0, -1, 0)
            );
            case EAST, WEST -> List.of(
                    new Vec3i(0, 1, 0),
                    new Vec3i(0, 0, 1),
                    new Vec3i(0, -1, 0),
                    new Vec3i(0, 0, -1)
            );
        };

        while(!nodePositions.empty()) {
            var pos = nodePositions.pop();
            final Node n = nodeByPosition.get(pos.toLong());
            assert n != null;

            if (n.traversedInSides.contains(adjacentDirection)) {
                continue;
            }
            n.traversedInSides.add(adjacentDirection);

            final Node adjacentCandidate = adjacent.nodeByPosition.get(pos.add(diffWithAdjacent).toLong());
            if (adjacentCandidate == null) {
                continue;
            }

            adjacentCandidate.traversedInSides.add(adjacentDirection.getOpposite());
            adjacentCandidate.validCandidateInSides.add(adjacentDirection.getOpposite());

            n.validCandidateInSides.add(adjacentDirection);
            validPositions.add(pos);

            for (final var offset : neighbourNodeOffsets) {
                final BlockPos neighbourPos = pos.add(offset);
                if (nodeByPosition.get(neighbourPos.toLong()) == null) {
                    continue;
                }

                nodePositions.add(neighbourPos);
            }
        }

        if (validPositions.isEmpty()) {
            return;
        }

        //TODO: perform some sort of refinement to avoid wild path direction changes near portals
        putPortal(adjacentDirection, new Portal(this, adjacent, validPositions.getFirst(), validPositions.getFirst().add(diffWithAdjacent)));
        adjacent.putPortal(adjacentDirection.getOpposite(), new Portal(adjacent, this, validPositions.getFirst().add(diffWithAdjacent), validPositions.getFirst()));
    }

    // TODO: actually use inbuilt functions in the enum
    private EnumFacing getAdjacentDirection(Cluster adjacent) {
        final var diff = positionDifference(adjacent);

        if (diff.getX() == 1 && diff.getY() == 0 && diff.getZ() == 0) return EnumFacing.EAST;
        if (diff.getX() == -1 && diff.getY() == 0 && diff.getZ() == 0) return EnumFacing.WEST;
        if (diff.getX() == 0 && diff.getY() == 1 && diff.getZ() == 0) return EnumFacing.UP;
        if (diff.getX() == 0 && diff.getY() == -1 && diff.getZ() == 0) return EnumFacing.DOWN;
        if (diff.getX() == 0 && diff.getY() == 0 && diff.getZ() == 1) return EnumFacing.SOUTH;
        if (diff.getX() == 0 && diff.getY() == 0 && diff.getZ() == -1) return EnumFacing.NORTH;

        throw new IllegalArgumentException("Clusters aren't adjacent");
    }

    public boolean isAdjacent(Cluster other) {
        Vec3i diff = positionDifference(other);
        int dx = Math.abs(diff.getX());
        int dy = Math.abs(diff.getY());
        int dz = Math.abs(diff.getZ());
        return (dx + dy + dz) == 1;
    }

    public void definePortals(Cluster adjacent) {
        final var adjacentDirection = getAdjacentDirection(adjacent);
        final var edge = getEdge(adjacentDirection);

        for (int x = (int) edge.minX; x <= edge.maxX; x++) {
            for (int y = (int) edge.minY; y <= edge.maxY; y++) {
                for (int z = (int) edge.minZ; z <= edge.maxZ; z++) {
                    final BlockPos pos = new BlockPos(x, y, z);
                    if (!nodeByPosition.containsKey(pos.toLong())) {
                        continue;
                    }

                    fillPortal(pos, adjacent, adjacentDirection);
                }
            }
        }
    }

    public void addPathsBeetwenPortals(SimpleWeightedGraph<Portal, DefaultWeightedEdge> portalGraph) {
        final List<Portal> allPortals = new ArrayList<>(portalsBySide.values().stream().flatMap(List::stream).toList());
//        if (Cluster.fromBlockCoordinates(start.getX(), start.getY(), start.getZ()).pos.equals(this.pos)) {
//            allPortals.add(start);
//        } else if (Cluster.fromBlockCoordinates(goal.getX(), goal.getY(), goal.getZ()).pos.equals(this.pos)) {
//            allPortals.add(goal);
//        }

        for (final Portal startPortal : allPortals) {
            for (final Portal goalPortal : allPortals) {
                final var pair = new UnorderedPair<>(startPortal, goalPortal);
                if (routeBeetweenPortals.get(pair) != null) {
                    continue;
                }

                final List<BlockPos> route = findPath(startPortal.getPortalPosForCluster(this), goalPortal.getPortalPosForCluster(this)); // TODO: FIXME: handle invalid routes
                if (route.isEmpty()) continue;

                routeBeetweenPortals.put(pair, route);

                portalGraph.addVertex(startPortal);
                portalGraph.addVertex(goalPortal);
                if(startPortal.equals(goalPortal)) {
                    continue;
                }

                final var edge = portalGraph.addEdge(startPortal, goalPortal);
                if(edge == null) {
                    Log.warn("Edge between {} and {} is null", startPortal, goalPortal);
                    continue;
                }

                portalGraph.setEdgeWeight(edge, route.size());
            }
        }
    }

    @VisibleForTesting
    public void dbg_renderPortals() {
        if (portalsBySide.isEmpty()) return;

        GlStateManager.pushMatrix();
        // Save relevant state
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        // Avoid z-fighting with wireframe lines
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(1f, 1f);
        GlStateManager.depthMask(false); // do not write depth so lines drawn after stay visible

        final float a = 0.35f;
        final double eps = 0.002; // shrink to sit just inside the block
        for (List<Portal> portals : portalsBySide.values()) {
            for (Portal portal : portals) {
                final BlockPos pos = portal.getPortalPosForCluster(this);

                AxisAlignedBB bb = new AxisAlignedBB(
                        pos.getX() + eps, pos.getY() + eps, pos.getZ() + eps,
                        pos.getX() + 1 - eps, pos.getY() + 1 - eps, pos.getZ() + 1 - eps
                );
                RenderGlobal.renderFilledBox(bb, 0.0f, 1.0f, 0.0f, a);
            }
        }

        // Restore
        GlStateManager.depthMask(true);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPopAttrib();
        GlStateManager.popMatrix();
    }

    private Cluster(Vec3i vec) {
        pos = vec;

        assert ((java.util.function.BooleanSupplier) () -> {
            ChunkPos cp = new ChunkPos(pos.getX(), pos.getZ());
            AxisAlignedBB bounds = getChunkBounds();
            boolean ok =
                    bounds.minX == cp.getXStart() &&
                            bounds.minZ == cp.getZStart() &&
                            bounds.maxX == cp.getXEnd() &&
                            bounds.maxZ == cp.getZEnd();
            if (!ok) {
                throw new AssertionError(String.format(
                        "Invalid chunk bounds, calculated %s, expected %s",
                        bounds,
                        new AxisAlignedBB(
                                cp.getXStart(),
                                pos.getY() << 4,
                                cp.getZStart(),
                                cp.getXEnd(),
                                (pos.getY() << 4) + 15,
                                cp.getZEnd()
                        )
                ));
            }
            return true;
        }).getAsBoolean();

        definePlaceablesInChunk();
    }

    public static Cluster fromBlockCoordinates(final int x, final int y, final int z) {
        final Vec3i pos = new Vec3i(x >> 4, y >> 4, z >> 4);
        assert pos.getX() == new ChunkPos(new BlockPos(x, y, z)).x
                && pos.getZ() == new ChunkPos(new BlockPos(x, y, z)).z
                : String.format("Chunk pos from block incorrect, calculated: %s, actual: %s", pos, new ChunkPos(new BlockPos(x, y, z)));

        return new Cluster(pos);
    }

    public void removePortalsWith(Cluster other) {
        assert this.isAdjacent(other) : String.format("Attempting to remove portals with non-adjacent cluster, this: %s, other %s", this, other);

        final EnumFacing direction = getAdjacentDirection(other);
        for(final var portal : portalsBySide.get(direction)) {
            final var node = nodeByPosition.get(portal.getPortalPosForCluster(other).toLong());
            if(node == null) {
                Log.warn("Orphaned node in portal, %s", portal);
                continue;
            }

            node.traversedInSides.clear();
            node.validCandidateInSides.clear();
        }

        portalsBySide.remove(direction);
    }

    public static Cluster fromChunkCoordinates(final int x, final int y, final int z) {
        return new Cluster(new Vec3i(x, y, z));
    }

    public AxisAlignedBB getChunkBounds() {
        return new AxisAlignedBB(
                new BlockPos(pos.getX() << 4, pos.getY() << 4, pos.getZ() << 4),
                new BlockPos((pos.getX() << 4) + 15, (pos.getY() << 4) + 15, (pos.getZ() << 4) + 15)
        );
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cluster oCluster = (Cluster) o;
        return pos.equals(oCluster.pos);
    }

    @Override
    public String toString() {
        return String.format("PathChunk{pos=%s}", pos);
    }
}
