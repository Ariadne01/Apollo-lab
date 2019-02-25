package ApolloRescue.module.complex.component;

import ApolloRescue.module.universal.Util;
import com.poths.rna.data.Point;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;

import java.awt.geom.Line2D;

public class EdgeInfoComponent implements IComponent {

    public static Pair<Integer, Integer> getEdgeMiddle(Edge edge) {
        int x = (int) ((edge.getStartX() + edge.getEndX()) / 2.0);
        int y = (int) ((edge.getStartY() + edge.getEndY()) / 2.0);
        return new Pair<Integer, Integer>(x, y);
    }

    public static int getEdgeLength(Edge edge) {
        return Util.distance(edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY());
    }

    public EdgeInfoComponent() {
    }

    public static Edge getEdgeInThisPoint(Area area, Point point) {
        for (Edge edge : area.getEdges()) {
            Line2D line2D = new Line2D.Double(edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY());
//            if (line2D.contains(point)) {
            if (Util.contains(line2D, point)) {
                return edge;
            }
        }

        //throw new RuntimeException("No edge found in point " + point + " at " + area);
        return null;
    }

    public void init() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void update() {
        //To change body of implemented methods use File | Settings | File Templates.
    }


}
