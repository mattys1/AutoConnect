package github.mattys1.autoconnect.connection.pathing;

import com.google.common.collect.ImmutableSet;
import github.mattys1.autoconnect.Log;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

class PathChunk {
    public final Vec3i pos;

    private PathChunk(Vec3i vec) {
        pos = vec;
    }

    public static PathChunk fromBlockCoordinates(final int x, final int y, final int z) {
        final Vec3i pos = new Vec3i(x >> 4, y >> 4, z >> 4);
        return new PathChunk(pos);
    }

    public static PathChunk fromChunkCoordinates(final int x, final int y, final int z) {
        return new PathChunk(new Vec3i(x, y, z));
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
        PathChunk oChunk = (PathChunk) o;
        return pos.equals(oChunk.pos);
    }

    @Override
    public String toString() {
        return String.format("PathChunk{pos=%s}", pos);
    }
}

public class RouteBuilder {
    private final BlockPosVertex start;
    private BlockPosVertex end;
    private final Long2ObjectOpenHashMap<BlockPosVertex> vertexByPos = new Long2ObjectOpenHashMap<>();
    private final HashSet<PathChunk> chunks = new HashSet<>();

    public RouteBuilder(final BlockPos startPos) {
        start = new BlockPosVertex(startPos);
        end = new BlockPosVertex(startPos);

//        addPositionsToRoute(List.of(start.pos));

        vertexByPos.put(startPos.toLong(), start);
    }

    // start with chunks in the bounding box between start and end
    private void setChunksBetweenStartAndGoal() {
        final PathChunk startChunk = PathChunk.fromBlockCoordinates(start.pos.getX(), start.pos.getY(), start.pos.getZ());
        final PathChunk endChunk = PathChunk.fromBlockCoordinates(end.pos.getX(), end.pos.getY(), end.pos.getZ());

        chunks.add(startChunk);
        chunks.add(endChunk);

        final Vec3i mins = new Vec3i(
                Math.min(startChunk.pos.getX(), endChunk.pos.getX()),
                Math.min(startChunk.pos.getY(), endChunk.pos.getY()),
                Math.min(startChunk.pos.getZ(), endChunk.pos.getZ())
        );

        final Vec3i maxes = new Vec3i(
                Math.max(startChunk.pos.getX(), endChunk.pos.getX()),
                Math.max(startChunk.pos.getY(), endChunk.pos.getY()),
                Math.max(startChunk.pos.getZ(), endChunk.pos.getZ())
        );
//
//
        for(var x = mins.getX(); x <= maxes.getX(); x++) {
            for(var y = mins.getY(); y <= maxes.getY(); y++) {
                for(var z = mins.getZ(); z <= maxes.getZ(); z++) {
                    final PathChunk chunk = PathChunk.fromChunkCoordinates(x, y, z);
                    if(chunks.contains(chunk)) {
                        continue;
                    }

                    chunks.add(chunk);
                }
            }
        }

        // could maybe remove them smarter instead of everything outside the bounding box.
        chunks.removeIf(c -> c.pos.getX() < mins.getX() || c.pos.getX() > maxes.getX()
                || c.pos.getY() < mins.getY() || c.pos.getY() > maxes.getY()
                || c.pos.getZ() < mins.getZ() || c.pos.getZ() > maxes.getZ());
    }

    public void setGoal(BlockPos end) {
        final BlockPosVertex goal = new BlockPosVertex(end);
//        assert vertexByPos.containsValue(goal) : String.format("Attempting to make a nonexistent vertex the goal, vertex set: %s, goal: %s",
//                vertexByPos.values().stream().map(v -> v.pos).toList(), goal.pos);
        final boolean hasGoalChangedChunk = PathChunk.fromBlockCoordinates(end.getX(), end.getY(), end.getZ()).pos.equals(
               PathChunk.fromBlockCoordinates(goal.pos.getX(), goal.pos.getY(), goal.pos.getZ()).pos
        ); // dumb

        final var oldGoal = this.end;
        this.end = goal;

        if(hasGoalChangedChunk) {
            setChunksBetweenStartAndGoal();
            Log.info("Chunks between start and goal: {}", chunks);
        }
    }

    public void dbg_displayChunks() {
        for(final var chunk : chunks) {
            final AxisAlignedBB box = chunk.getChunkBounds();

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