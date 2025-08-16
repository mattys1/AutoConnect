package github.mattys1.autoconnect.connection.pathing;

import github.mattys1.autoconnect.Log;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jheaps.annotations.VisibleForTesting;
import org.lwjgl.opengl.GL11;

import java.util.*;

class Node {
    private int g;
    private int f;
    public boolean isValidPortalCandidate = false;
    public boolean isTraversed = false;
}

class Cluster {
    public final Vec3i pos;
    private final Long2ObjectOpenHashMap<Node> nodeByPosition = new Long2ObjectOpenHashMap<>();
    Object2ObjectArrayMap<EnumFacing, HashSet<BlockPos>> portalPositions = new Object2ObjectArrayMap<>();

    private void definePlaceablesInChunk() {
        final AxisAlignedBB boundingBox = getChunkBounds();
        final World world = Minecraft.getMinecraft().world;

        assert boundingBox.minX <= boundingBox.maxX &&
                boundingBox.minY <= boundingBox.maxY &&
                boundingBox.minZ <= boundingBox.maxZ
                : String.format("Invalid bounding box: min(%.1f,%.1f,%.1f), max(%.1f,%.1f,%.1f)",
                boundingBox.minX, boundingBox.minY, boundingBox.minZ,
                boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);

        for(int x = (int) boundingBox.minX; x <= boundingBox.maxX; x++) {
            for(int y = (int) boundingBox.minY; y <= boundingBox.maxY; y++) {
                for(int z = (int) boundingBox.minZ; z <= boundingBox.maxZ; z++) {
                    final BlockPos pos = new BlockPos(x, y, z);

                    if(world.isAirBlock(pos)) {
                        assert world.isValid(pos) : "Attempting to put invalid block pos into chunk";
                        nodeByPosition.put(pos.toLong(), new Node());
                    }
                }
            }
        }
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

    private void putPortal(EnumFacing adjacentDirection, BlockPos portalPos) {
        if(portalPositions.containsKey(adjacentDirection)) {
            portalPositions.get(adjacentDirection).add(portalPos);
        } else {
            portalPositions.put(adjacentDirection, new HashSet<>(Collections.singleton(portalPos)));
        }
    }

    // TODO: handle portals for adjacent
    private void fillPortal(BlockPos nodePos, Cluster adjacent, EnumFacing adjacentDirection) {
        final var startNode = nodeByPosition.get(nodePos.toLong());
        assert startNode != null;
        if(startNode.isTraversed) return;

        final List<BlockPos> validPositions = new ArrayList<>(); // for postprocessing down the line
        final Stack<BlockPos> nodePositions = new Stack<>();
        nodePositions.add(nodePos);
        final Vec3i diffWithAdjacent = positionDifference(adjacent);

        List<Vec3i> neighbourNodeOffsets;
        switch (adjacentDirection) {
            case UP, DOWN -> neighbourNodeOffsets = List.of(
                    new Vec3i(1, 0, 0),
                    new Vec3i(0, 0, 1),
                    new Vec3i(-1, 0, 0),
                    new Vec3i(0, 0, -1)
            );
            case NORTH, SOUTH -> neighbourNodeOffsets = List.of(
                    new Vec3i(1, 0, 0),
                    new Vec3i(0, 1, 0),
                    new Vec3i(-1, 0, 0),
                    new Vec3i(0, -1, 0)
            );
            case EAST, WEST -> neighbourNodeOffsets = List.of(
                    new Vec3i(0, 1, 0),
                    new Vec3i(0, 0, 1),
                    new Vec3i(0, -1, 0),
                    new Vec3i(0, 0, -1)
            );
            default -> throw new IllegalArgumentException("shut the hell up intellij");
        }

        while(!nodePositions.empty()) {
            var pos = nodePositions.pop();
            final Node n = nodeByPosition.get(pos.toLong());
            assert n != null;

            if(n.isTraversed) {
                continue;
            }
            n.isTraversed = true;

            final Node adjacentCandidate = adjacent.nodeByPosition.get(pos.add(diffWithAdjacent).toLong());
            if(adjacentCandidate == null) {
                continue;
            }

            adjacentCandidate.isTraversed = true;
            adjacentCandidate.isValidPortalCandidate = true;

            n.isValidPortalCandidate = true;
            validPositions.add(pos);

            for(final var offset : neighbourNodeOffsets) {
                final BlockPos neighbourPos = pos.add(offset);
                if(nodeByPosition.get(neighbourPos.toLong()) == null) {
                    continue;
                }

                nodePositions.add(neighbourPos);
            }
        }

        if(validPositions.isEmpty()) {
            return;
        }

        putPortal(adjacentDirection, validPositions.getFirst());
        adjacent.putPortal(adjacentDirection.getOpposite(), validPositions.getFirst().add(diffWithAdjacent));
    }

    // TODO: actually use inbuilt functions in the enum
    private EnumFacing getAdjacentDirection(Cluster adjacent) {
        final var diff = positionDifference(adjacent);

        if(diff.getX() == 1 && diff.getY() == 0 && diff.getZ() == 0) return EnumFacing.EAST;
        if(diff.getX() == -1 && diff.getY() == 0 && diff.getZ() == 0) return EnumFacing.WEST;
        if(diff.getX() == 0 && diff.getY() == 1 && diff.getZ() == 0) return EnumFacing.UP;
        if(diff.getX() == 0 && diff.getY() == -1 && diff.getZ() == 0) return EnumFacing.DOWN;
        if(diff.getX() == 0 && diff.getY() == 0 && diff.getZ() == 1) return EnumFacing.SOUTH;
        if(diff.getX() == 0 && diff.getY() == 0 && diff.getZ() == -1) return EnumFacing.NORTH;

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
        final var adjacentDiurection = getAdjacentDirection(adjacent);
        final var edge = getEdge(adjacentDiurection);

        for(int x = (int) edge.minX; x <= edge.maxX; x++) {
            for(int y = (int) edge.minY; y <= edge.maxY; y++) {
                for(int z = (int) edge.minZ; z <= edge.maxZ; z++) {
                    final BlockPos pos = new BlockPos(x, y, z);
                    if(!nodeByPosition.containsKey(pos.toLong())) {
                        continue;
                    }

                    fillPortal(pos, adjacent, adjacentDiurection);
                }
            }
        }

        Log.info("Portals in cluster, {}", portalPositions);
    }

    @VisibleForTesting
    public void dbg_renderPortals() {
        if (portalPositions.isEmpty()) return;

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
        for (HashSet<BlockPos> positions : portalPositions.values()) {
            for (BlockPos pos : positions) {
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

    public static Cluster fromChunkCoordinates(final int x, final int y, final int z) {
        return new Cluster(new Vec3i(x, y, z));
    }

    public AxisAlignedBB getChunkBounds() {
        return new AxisAlignedBB(
                new BlockPos(pos.getX() << 4, pos.getY() << 4, pos.getZ() << 4),
                new BlockPos((pos.getX() << 4) + 15, (pos.getY() << 4) + 15, (pos.getZ() << 4) + 15)
        );
    }

    public long toLong() {
        final int X_BITS = 28;
        final int Y_BITS = 8;
        final int Z_BITS = 28;

        final int Z_SHIFT = 0;
        final int Y_SHIFT = Z_SHIFT + Z_BITS;
        final int X_SHIFT = Y_SHIFT + Y_BITS;

        final long X_MASK = (1L << X_BITS) - 1L;
        final long Y_MASK = (1L << Y_BITS) - 1L;
        final long Z_MASK = (1L << Z_BITS) - 1L;

        return ((long)this.pos.getX() & X_MASK) << X_SHIFT | ((long)this.pos.getY() & Y_MASK) << Y_SHIFT | ((long) this.pos.getZ() & Z_MASK);
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

public class RouteBuilder {
    private final BlockPosVertex start;
    private BlockPosVertex end;
    private final HashSet<Cluster> clusters = new HashSet<>();

    public RouteBuilder(final BlockPos startPos) {
        start = new BlockPosVertex(startPos);
        end = new BlockPosVertex(startPos);

//        addPositionsToRoute(List.of(start.pos));
    }

    // start with chunks in the bounding box between start and end
    private void setChunksBetweenStartAndGoal() {
        final Cluster startCluster = Cluster.fromBlockCoordinates(start.pos.getX(), start.pos.getY(), start.pos.getZ());
        final Cluster endCluster = Cluster.fromBlockCoordinates(end.pos.getX(), end.pos.getY(), end.pos.getZ());

        clusters.add(startCluster);
        clusters.add(endCluster);

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
//
//
        for(var x = mins.getX(); x <= maxes.getX(); x++) {
            for(var y = mins.getY(); y <= maxes.getY(); y++) {
                for(var z = mins.getZ(); z <= maxes.getZ(); z++) {
                    final Cluster cluster = Cluster.fromChunkCoordinates(x, y, z);
                    if(clusters.contains(cluster)) {
                        continue;
                    }

                    clusters.add(cluster);
                }
            }
        }

        // could maybe remove them smarter instead of everything outside the bounding box.
        clusters.removeIf(c -> c.pos.getX() < mins.getX() || c.pos.getX() > maxes.getX()
                || c.pos.getY() < mins.getY() || c.pos.getY() > maxes.getY()
                || c.pos.getZ() < mins.getZ() || c.pos.getZ() > maxes.getZ());
    }

    // this should check only new and edge clusters
    public void processPortals() {
        for(final Cluster cluster : clusters) {
            for(final Cluster cluster1 : clusters) {
                if(!cluster.isAdjacent(cluster1)) {
                    continue;
                }

                cluster.definePortals(cluster1);


            }
        }
    };

    public void setGoal(BlockPos end) {
        final BlockPosVertex goal = new BlockPosVertex(end);
//        assert vertexByPos.containsValue(goal) : String.format("Attempting to make a nonexistent vertex the goal, vertex set: %s, goal: %s",
//                vertexByPos.values().stream().map(v -> v.pos).toList(), goal.pos);
        final boolean hasGoalChangedChunk = Cluster.fromBlockCoordinates(end.getX(), end.getY(), end.getZ()).pos.equals(
               Cluster.fromBlockCoordinates(goal.pos.getX(), goal.pos.getY(), goal.pos.getZ()).pos
        ); // dumb

        final var oldGoal = this.end;
        this.end = goal;

        if(hasGoalChangedChunk) {
            setChunksBetweenStartAndGoal();
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