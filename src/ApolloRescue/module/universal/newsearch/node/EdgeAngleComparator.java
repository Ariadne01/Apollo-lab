package ApolloRescue.module.universal.newsearch.node;

import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Edge;

import java.util.Comparator;

public class EdgeAngleComparator implements Comparator<Edge> {
    private Point2D edgeVector;
    private Point2D start;

    public EdgeAngleComparator(Edge edge) {
        this.edgeVector = new Point2D(edge.getEndX() - edge.getStartX(),
                edge.getEndY() - edge.getStartY());
        start = edge.getStart();
    }

    @Override
    public int compare(Edge p1, Edge p2) {
        // TODO Auto-generated method stub

        Point2D p1_center = getEdgeCenter(p1), p2_center = getEdgeCenter(p2);
        Point2D vector_1 = new Point2D(p1_center.getX() - start.getX(),
                p1_center.getY() - start.getY());
        Point2D vector_2 = new Point2D(p2_center.getX() - start.getX(),
                p2_center.getY() - start.getY());
        double value_1 = getAngleValue(edgeVector, vector_1);
        double value_2 = getAngleValue(edgeVector, vector_2);
        double result = value_1 - value_2;
        if (result > 0) {
            return 1;
        } else if (result < 0) {
            return -1;
        } else
            return 0;
    }

    public Point2D getEdgeCenter(Edge edge) {
        if (edge != null) {
            int x = (edge.getStartX() + edge.getEndX()) / 2;
            int y = (edge.getStartY() + edge.getEndY()) / 2;
            Point2D point = new Point2D(x, y);
            return point;
        }
        return null;

    }

    public double getAngleValue(Point2D p1, Point2D p2) {
        double m = (p1.getX() * p2.getX() + p1.getY() * p2.getY())
                / (Math.hypot(p1.getX(), p1.getY()) * Math.hypot(p2.getX(),
                p2.getY()));
        return Math.abs(m);
    }

}