package github.mattys1.autoconnect.connection.pathing;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

class Portal {
    private final Cluster c1;
    private final Cluster c2;
    private final BlockPos p1;
    private final BlockPos p2;
    private final boolean isBlock;

    public Portal(Cluster c1, Cluster c2, BlockPos p1, BlockPos p2) {
        this.c1 = c1;
        this.c2 = c2;
        this.p1 = p1;
        this.p2 = p2;
        this.isBlock = p1.equals(p2);
    }

    public BlockPos getPortalPosForCluster(Cluster cluster) {
        if (cluster.equals(c1)) return p1;
        if (cluster.equals(c2)) return p2;
        throw new IllegalArgumentException("Cluster not part of this portal");
    }

    private Cluster getClusterForPos(BlockPos pos) {
        if (pos == p1) return c1;
        if (pos == p2) return c2;
        throw new IllegalArgumentException("Pos not part of this portal");
    }

// feels wierd having this in Portal
    public Optional<List<BlockPos>> getConnectingRoute(final Portal other) {
        return Stream.of(
                c1.getRouteBetweenPortals(this, other),
                c2.getRouteBetweenPortals(this, other),
                other.c1.getRouteBetweenPortals(other, this),
                other.c2.getRouteBetweenPortals(other, this)
        ).flatMap(Optional::stream) // Java 9+. For Java 8: .filter(Optional::isPresent).map(Optional::get)
         .min(Comparator.comparingInt(List::size));
    }

    public boolean isBlock() {
        return isBlock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Portal other)) return false;
        boolean direct = c1.equals(other.c1) && c2.equals(other.c2) &&
                p1.equals(other.p1) && p2.equals(other.p2);
        boolean swapped = c1.equals(other.c2) && c2.equals(other.c1) &&
                p1.equals(other.p2) && p2.equals(other.p1);
        return direct || swapped;
    }

    @Override
    public int hashCode() {
        int hA = 31 * c1.hashCode() + p1.hashCode();
        int hB = 31 * c2.hashCode() + p2.hashCode();
        return (hA ^ hB) + (hA + hB);
    }

    @Override public String toString() {
        return String.format("Portal{(c1=%s p1=%s),(c2=%s p2=%s)}", c1, p1, c2, p2);
    }
}
