package ApolloRescue.extaction.clear;

import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;

import java.awt.*;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

public class ClearHelperUtils {

    /**
     * 获得两个Point2D之间的距离
     *
     * @param p1
     * @param p2
     * @return 返回距离为整数
     */
    public static int getDistance(Point2D p1, Point2D p2) {//ljy: 2017放在Util里
        double dx, dy;
        int d;

        dx = p1.getX() - p2.getX();
        dy = p1.getY() - p2.getY();
        d = (int) Math.hypot(dx, dy);// 勾股定理
        return d;
    }

    /**
     * 获取一个Point2D到Building的距离，Building位置取中心
     *
     * @param p
     * @param b
     * @return 返回距离为整数
     */
    public static int getDistance(Point2D p, Building b) {
        Point2D center = new Point2D(b.getX(), b.getY());
        return ClearHelperUtils.getDistance(p, center);
    }

    /**
     * 获取一个Point2D到Road的距离，Road的位置取中心
     *
     * @param p
     * @param r
     * @return 返回距离为整数
     */
    public static int getDistance(Point2D p, Road r) {
        Point2D center = new Point2D(r.getX(), r.getY());
        return ClearHelperUtils.getDistance(p, center);
    }

    /**
     * 获取一个Point2D到Blockade的距离，Blockade的位置取中心
     *
     * @param p
     * @param b
     * @return 返回距离为整数
     */
    public static int getDistance(Point2D p, Blockade b) {
        Point2D center = new Point2D(b.getX(), b.getY());
        return ClearHelperUtils.getDistance(p, center);
    }

    /**
     * 获取一个Point2D到Human的距离
     *
     * @param point
     * @pance(p, center);
    }

    /**
     * 获取一个Point2D到Edge的距离，获取方法为点对边作垂线，求垂线段的长
     *
     * @param point
     * @param edge
     * @return
     */
    public static int getDistance(Point2D point, Edge edge) {
        int d;
        java.awt.geom.Line2D l;

        l = ClearHelperUtils.getLine(edge);
        Param line = CalParam(l.getP1(), l.getP2());
        Param verticalLine = getVerticalLine(line, point);
        Point2D p = getIntersectPoint(line, verticalLine);
        d = getDistance(p, point);
        return d;
    }// 获得点到边的距离

    /**
     * 用Edge获取Line2D
     *
     * @param edge
     * @return
     */
    public static java.awt.geom.Line2D getLine(Edge edge) {
        Point p1, p2;
        int x, y;
        java.awt.geom.Line2D line;

        x = edge.getStartX();
        y = edge.getStartY();
        p1 = new Point(x, y);
        x = edge.getEndX();
        y = edge.getEndY();
        p2 = new Point(x, y);
        line = new java.awt.geom.Line2D.Double(p1, p2);
        return line;
    }
    /**
     * 获取一个Area的面积
     *
     * @param a
     * @return 面积为double，如果area为null也返回0
     */
    public static double getTotalSurface(Area a) {
        if (a == null) {
            return 0;
        }
        if (a.getApexList() == null) {
            return 0;
        }
        int[] xPoints = new int[a.getApexList().length / 2];
        int[] yPoints = new int[a.getApexList().length / 2];
        for (int i = 0; i < a.getApexList().length; i += 2) {
            xPoints[i / 2] = a.getApexList()[i];
            yPoints[i / 2] = a.getApexList()[i + 1];
        }
        double surface = ClearHelperUtils.surface(new java.awt.geom.Area(
                new Polygon(xPoints, yPoints, xPoints.length))) * 0.001 * 0.001;
        return surface;
    }

    /**
     * 计算图形的面积
     *
     * @param area
     *            java.awt.geom.Area
     * @return 面积，为double
     */
    public static double surface(java.awt.geom.Area area) {
        if (null == area) {
            return 0;
        }
        PathIterator iter = area.getPathIterator(null);
        if (null == iter) {
            System.out.println("iter is null");
            return 0;
        }
        double sum_all = 0;
        while (!iter.isDone()) {
            List<double[]> points = new ArrayList<double[]>();
            while (!iter.isDone()) {
                double point[] = new double[2];
                int type = iter.currentSegment(point);
                iter.next();
                if (type == PathIterator.SEG_CLOSE) {
                    if (points.size() > 0)
                        points.add(points.get(0));
                    break;
                }
                points.add(point);
            }

            double sum = 0;
            for (int i = 0; i < points.size() - 1; i++) {
                sum += points.get(i)[0] * points.get(i + 1)[1]
                        - points.get(i)[1] * points.get(i + 1)[0];
            }

            sum_all += Math.abs(sum) / 2;
        }

        return sum_all;
    }
    /**
     * 计算直线参数的方法
     *
     * @param p1
     *            直线上第一个点
     * @param p2
     *            直线上第二个点
     * @return 直线方程的三个参数
     */
    public static Param CalParam(java.awt.geom.Point2D p1,
                                 java.awt.geom.Point2D p2) {
        double a, b, c;
        double x1 = p1.getX(), y1 = p1.getY(), x2 = p2.getX(), y2 = p2.getY();
        a = y2 - y1;
        b = x1 - x2;
        c = (x2 - x1) * y1 - (y2 - y1) * x1;
        if (b < 0) {
            a *= -1;
            b *= -1;
            c *= -1;
        } else if (b == 0 && a < 0) {
            a *= -1;
            c *= -1;
        }
        return new Param(a, b, c);
    }

    /**
     * 计算直线参数的方法
     *
     * @param p1
     *            直线上第一个点
     * @param p2
     *            直线上第二个点
     * @return 直线方程的三个参数
     */
    public static Param CalParam(Point2D p1, Point2D p2) {
        double a, b, c;
        double x1 = p1.getX(), y1 = p1.getY(), x2 = p2.getX(), y2 = p2.getY();
        a = y2 - y1;
        b = x1 - x2;
        c = (x2 - x1) * y1 - (y2 - y1) * x1;
        if (b < 0) {
            a *= -1;
            b *= -1;
            c *= -1;
        } else if (b == 0 && a < 0) {
            a *= -1;
            c *= -1;
        }
        return new Param(a, b, c);
    }

    /**
     * 获取一个点到一个直线的垂线的方程参数
     *
     * @param pm
     *            直线方程参数
     * @param p
     *            点
     * @return 点到直线的垂线的方程参数
     */
    public static Param getVerticalLine(Param pm, Point2D p) {
        double a = pm.a;
        double b = pm.b;
        // double c = pm.c;

        // double c1 = a*p.getY()
        double temp = a;
        a = b;
        b = -temp;
        double c1 = -a * p.getX() - b * p.getY();
        return new Param(a, b, c1);
    }

    /**
     * 获得两条直线的相交点
     *
     * @param pm1
     *            直线参数1
     * @param pm2
     *            直线参数2
     * @return 交点
     */
    public static Point2D getIntersectPoint(Param pm1, Param pm2) {
        return getIntersectPoint(pm1.a, pm1.b, pm1.c, pm2.a, pm2.b, pm2.c);
    }

    /**
     * 获得两条直线的相交点
     *
     * @param a1
     *            第一条直线的参数a
     * @param b1
     *            第一条直线的参数b
     * @param c1
     *            第一条直线的参数c
     * @param a2
     *            第二条直线的参数a
     * @param b2
     *            第二条直线的参数b
     * @param c2
     *            第二条直线的参数c
     * @return 交点
     */
    public static Point2D getIntersectPoint(double a1, double b1, double c1,
                                            double a2, double b2, double c2) {
        Point2D p = null;
        double m = a1 * b2 - a2 * b1;
        if (m == 0) {
            // 两条线平行
            // System.out.println("返回0");
            return null;
        }
        double x = (c2 * b1 - c1 * b2) / m;
        double y = (c1 * a2 - c2 * a1) / m;
        p = new Point2D((int) x, (int) y);
        return p;
    }

    /**
     * 从Apexs中获取所有坐标点的列表（有序的）
     *
     * @param Apexs
     * @return
     */
    public static List<Point2D> getPointsFromApexs(int[] Apexs) {
        List<Point2D> points = new ArrayList<Point2D>();
        for (int i = 0; i < Apexs.length; i += 2) {
            points.add(new Point2D(Apexs[i], Apexs[i + 1]));
        }
        return points;
    }

    /**
     * 从Edge的列表（必须有序，且闭环）中获取其中所有的点
     *
     * @param edges
     *            边的列表
     * @return 点的列表
     */
    public static List<Point2D> getPoint2DFromEdges(List<Edge> edges) {// TODO:test
        List<Point2D> points = new ArrayList<Point2D>();
        if (edges == null || edges.size() <= 2) {
            return null;
        } else {
            for (Edge e : edges) {
                points.add(e.getStart());
            }
            return points;
        }
    }

    /**
     * 从点的数组中（有序）获取边的列表
     *
     * @param points
     *            点的数组
     * @return
     */
    public static List<Edge> getEdgesFromPoint2D(Point2D[] points) {
        List<Edge> edges = new ArrayList<Edge>();
        if (null != points && points.length > 0) {
            for (int i = 0; i < points.length; i++) {
                // 如果是最有一个元素
                if (i == (points.length - 1)) {
                    Edge edge = new Edge((int) points[i].getX(),
                            (int) points[i].getY(), (int) points[0].getX(),
                            (int) points[0].getY());
                    edges.add(edge);
                } else {
                    Edge edge = new Edge((int) points[i].getX(),
                            (int) points[i].getY(), (int) points[i + 1].getX(),
                            (int) points[i + 1].getY());
                    edges.add(edge);
                }
            }
        }
        return edges;
    }

    /**
     * 从点的列表中获取边的列表
     *
     * @param points
     *            点的列表
     * @return 边的列表
     */
    public static List<Edge> getEdgesFromPoint2D(List<Point2D> points) {
        List<Edge> edges = new ArrayList<Edge>();
        if (null != points && points.size() > 0) {
            for (int i = 0; i < points.size(); i++) {
                // 如果是最后一个元素
                if (i == (points.size() - 1)) {
                    Edge edge = new Edge((int) points.get(i).getX(),
                            (int) points.get(i).getY(), (int) points.get(0)
                            .getX(), (int) points.get(0).getY());
                    edges.add(edge);
                } else {
                    Edge edge = new Edge((int) points.get(i).getX(),
                            (int) points.get(i).getY(), (int) points.get(i + 1)
                            .getX(), (int) points.get(i + 1).getY());
                    edges.add(edge);
                }
            }
        }
        return edges;
    }

    /**
     * 获取Edge的中点
     *
     * @param e
     * @return
     */
    public static Point2D getEdgeCenter(Edge e) {// 求边的中点
        if (e == null) {
            return null;
        } else {
            double dx = (e.getStartX() + e.getEndX()) / 2;
            double dy = (e.getStartY() + e.getEndY()) / 2;
            return new Point2D(dx, dy);

        }
    }
}