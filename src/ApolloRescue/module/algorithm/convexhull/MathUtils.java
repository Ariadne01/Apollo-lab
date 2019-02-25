package ApolloRescue.module.algorithm.convexhull;

import rescuecore2.standard.entities.Area;

public class MathUtils {
    public static double manhattanDistance(Area a1, Area a2) {
        return Math.abs(a1.getX() - a2.getX()) + Math.abs(a1.getY() - a2.getY());
    }

    public static double manhattanDistance(Area a, int x2, int y2) {
        return Math.abs(a.getX() - x2) + Math.abs(a.getY() - y2);
    }
    /*public static double manhattanDistance(PathInfo a, int x2, int y2) {
        return Math.abs(a.getX() - x2) + Math.abs(a.getY() - y2);
    }

    public static double manhattanDistance(Area a, double x2, double y2) {
        return Math.abs(a.getX() - x2) + Math.abs(a.getY() - y2);
    }

    public static double manhattanDistance(IPositioning a, int x2, int y2) {
        return Math.abs(a.getX() - x2) + Math.abs(a.getY() - y2);
    }

    public static double manhattanDistance(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    public static double manhattanDistance(IPositioning o1, IPositioning o2) {
        return Math.abs(o1.getX() - o2.getX()) + Math.abs(o1.getY() - o2.getY());
    }

    public static double manhattanDistance(IPositioning o1, Area o2) {
        return Math.abs(o1.getX() - o2.getX()) + Math.abs(o1.getY() - o2.getY());
    }*/

    public static double dist(int p1x, int p1y, int p2x, int p2y) {   //曼哈顿距离
        int a = p1x - p2x;
        int b = p1y - p2y;
        return Math.hypot(a, b);
    }
}
