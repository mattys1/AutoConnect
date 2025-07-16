package github.mattys1.autoconnect.connection.pathing;

import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;

import java.util.List;
import java.util.function.Supplier;

class PathfindingGraph<V, E> extends SimpleGraph<V, E> {
    public PathfindingGraph(Class<? extends E> edgeClass) {
        super(edgeClass);
    }

    public PathfindingGraph(Supplier<V> vertexSupplier, Supplier<E> edgeSupplier, boolean weighted) {
        super(vertexSupplier, edgeSupplier, weighted);
    }
}