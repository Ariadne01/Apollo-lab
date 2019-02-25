package ApolloRescue.extaction.clear;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.complex.component.RoadInfoComponent;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.entities.BlockadeModel;
import ApolloRescue.module.universal.entities.EdgeModel;
import ApolloRescue.module.universal.entities.RoadModel;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import javolution.util.FastSet;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.*;
import java.util.List;

public class ClearHelper {

    public static final int SECURE_RANGE = 1000;

    public WorldInfo worldInfo;
    public ScenarioInfo scenarioInfo;
    public AgentInfo agentInfo;

    public int MaxClearRad;
    public int maxClearDistance;
    public int DefaultLength = 8000;
    public int DefaultRad = 500;
    public double clearRange;
    public double clearDistance ;

    public ApolloWorld world;

//    public GuideLineHelper guideLineHelper = null;

    public ClearHelper(ApolloWorld world, WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo) {
        this.worldInfo = world.getWorldInfo();
        this.agentInfo = world.getAgentInfo();
        this.scenarioInfo = world.getScenarioInfo();
        this.world = world;

        MaxClearRad = world.getScenarioInfo().getClearRepairRad();
        maxClearDistance = world.getScenarioInfo().getClearRepairDistance();// TODO：如果一直维护同一个的话上面那些参数会不会改变
        clearRange = scenarioInfo.getClearRepairDistance();
        clearDistance = scenarioInfo.getClearRepairDistance();

    }

    public Set<Road> getRoadsSeen() {
        return world.getRoadsSeen();
    }

    public RoadModel getRoadModel(EntityID roadId) {
        return world.getRoadModel(roadId);
    }

//    public GuideLineHelper getGuideLineHelper() {
//        if(guideLineHelper != null)
//            return guideLineHelper;
//        return null;
//    }

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
        return getDistance(p, center);
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
        return getDistance(p, center);
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
        return getDistance(p, center);
    }

    /**
     * 获取一个Point2D到Human的距离
     *
     * @param p
     * @param h
     * @return 返回距离为整数
     */
    public static int getDistance(Point2D p, Human h) {
        Point2D center = new Point2D(h.getX(), h.getY());
        return getDistance(p, center);
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

        l = getLine(edge);
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
        double surface = surface(new java.awt.geom.Area(
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
//            System.out.println("iter is null");
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


    /**
     * this method create a list of guide line used by pf to clearing area
     *
     * @param path             list of area id calculated by path planning algorithms(such as AStar). this list must be started by
     *                         current agent position id and should not be null or empty.
     * @param destinationPoint
     * @return list of guideline indexed like path indexes
     */
    public List<GuideLine> getPathGuidelines(List<EntityID> path, Point2D destinationPoint) {
        List<GuideLine> guideLineList = new ArrayList<GuideLine>();

        if (path == null) {
            return null;
        }

        Area sourceArea;
        if (path.isEmpty()) {
            sourceArea = (Area) worldInfo.getEntity(agentInfo.getPosition());
        } else {
            sourceArea = (Area) worldInfo.getEntity(path.get(0));
        }
        Point2D firstPoint = Util.getPoint(worldInfo.getLocation(sourceArea));
        if (path.size() > 1) {
            Edge edgeTo = agentInfo.getPositionArea().getEdgeTo(path.get(1));
            if (edgeTo != null) {
                Point2D midPointTo = Util.getMiddle(edgeTo.getLine());
                Point2D agentPosition = Util.getPoint(worldInfo.getLocation(agentInfo.getID()));//current agent location
                Line2D agentEdgeLine = new Line2D(agentPosition, midPointTo);
                Line2D agentAreaLine = new Line2D(agentPosition, firstPoint);
                Line2D areaEdgeLine = new Line2D(firstPoint, midPointTo);
                double theta = Util.angleBetween2Lines(agentAreaLine, areaEdgeLine);
                double alpha = Util.angleBetween2Lines(agentAreaLine, agentEdgeLine);
                if (alpha < 80 && theta > 80) {
//                    world.printData("theta = " + theta + " alpha = " + alpha);
                    firstPoint = agentPosition;
                }
            }
        }
        //guideLineList.add(new GuideLine(sourcePoint, firstPoint));

        Area area;
        Edge edge;
        GuideLine guideLine;
        for (int i = 0; i < path.size() - 1; i++) {
            EntityID id = path.get(i);
            area = (Area) worldInfo.getEntity(id);
            edge = area.getEdgeTo(path.get(i + 1));
            if (edge == null) {
                break;
            }
            Point2D middle = Util.getMiddle(edge.getLine());
            guideLine = new GuideLine(firstPoint, middle);
            guideLineList.add(guideLine);
            firstPoint = middle;
        }

        //add last path area guideline
        Point2D lastPoint;
        if (destinationPoint == null) {
            Area lastArea = (Area) worldInfo.getEntity(path.get(path.size() - 1));
            lastPoint = Util.getPoint(worldInfo.getLocation(lastArea));
        } else {
            lastPoint = destinationPoint;
        }

        if (firstPoint.equals(lastPoint)) {
//            System.out.println("first point and last point are equal");
        } else {
            guideLine = new GuideLine(firstPoint, lastPoint);
            guideLineList.add(guideLine);
        }

        return guideLineList;
    }


    /**
     * this function merge similar guidelines
     * merge criteria is similarity of current guide line "Slope" and "angle" than next one.
     *
     * @param guideLines
     * @param path
     * @param target
     * @return
     */
    public GuideLine getTargetGuideLine(List<GuideLine> guideLines, List<EntityID> path, EntityID target, double clearDistance) {
        if (guideLines.isEmpty()) {
            return null;
        }
        double secureAngle = 30d;
        GuideLine guideLine = guideLines.get(0);

        GuideLine targetGuideLine;
        Point2D selfPosition = Util.getPoint(worldInfo.getLocation(agentInfo.getID()));
//        if (Util.distance(guideLine.getEndPoint(), selfPosition) > Util.distance(guideLine.getOrigin(), selfPosition)) {//this condition prevents problem of too long area.
//            targetGuideLine = guideLine;
//        } else {
        targetGuideLine = guideLine;
//        targetGuideLine.setAreas(guideLine.getAreas());
//        }
        List<EntityID> areas = new ArrayList<EntityID>();
        areas.add(path.get(0));
        double angle;
        List<GuideLine> tempGuidelines = new ArrayList<GuideLine>();
        boolean firsLine = true;
        for (int i = 1; i < guideLines.size(); i++) {
            guideLine = guideLines.get(i);
            angle = Util.angleBetween2Lines(targetGuideLine, guideLine);
            double distance = (targetGuideLine.getLength() + guideLine.getLength());
            double distanceCost = 1 - (guideLine.getLength() / distance);//area distance cost (cost < 1)
            if ((firsLine && distance < clearDistance / 5) || angle < (secureAngle * distanceCost)) {
                Vector2D newDirection = targetGuideLine.getDirection().add(guideLine.getDirection());
                targetGuideLine = new GuideLine(targetGuideLine.getOrigin(), newDirection);
                tempGuidelines.add(targetGuideLine);

                if (path.size() > i) {
                    areas.add(path.get(i));
                }
                targetGuideLine.getAreas().addAll(areas);
            } else {
                break;
            }
            firsLine = false;
        }
        targetGuideLine.setAreas(areas);

        for (int j = 0; j < tempGuidelines.size(); j++) {
            //check through list to prevent distance fault
            GuideLine line1 = tempGuidelines.get(j);
            double a = Util.angleBetween2Lines(line1, targetGuideLine);
            double dist = line1.getLength() * Math.sin(Math.toRadians(a));
            if (dist > scenarioInfo.getClearRepairRad()) {
                targetGuideLine = line1;
                break;
            }
        }
        return targetGuideLine;
    }

    /**
     * determine whether @point2D in the any blockade of the {@code road} or not;
     *
     * @param point2D the point we want know is it in the blockade or not
     * @param road    the road which we want check
     * @return
     */
    public boolean isInAnyBlockade(Point2D point2D, Road road) {
        if (!road.isBlockadesDefined() || road.getBlockades().isEmpty()) {
            return false;
        }

        Blockade blockade;
        for (EntityID blockadeID : road.getBlockades()) {
            blockade = (Blockade) worldInfo.getEntity(blockadeID);
            Polygon polygon = Util.getPolygon(blockade.getApexes());
            if (polygon.contains(point2D.getX(), point2D.getY())) {
                return true;
            }
        }
        return false;
    }

    public boolean isOnBlockade(Point2D point2D, List<Area> areas) {
        Blockade blockade;
        Polygon polygon;
        for (Area area : areas) {
            if (!area.isBlockadesDefined()) {
                continue;
            }
            for (EntityID blockadeID : area.getBlockades()) {
                blockade = (Blockade) worldInfo.getEntity(blockadeID);
                polygon = Util.getPolygon(blockade.getApexes());
                if (polygon.contains(point2D.getX(), point2D.getY())) {
                    return true;
                }

            }
        }
        return false;
    }


    public boolean anyBlockadeIntersection(Area area, Line2D targetLine) {
        if (!(area instanceof Road) || targetLine == null) {
            return false;
        }
        RoadModel road = getRoadModel(area.getID());
        for (BlockadeModel blockade : road.getBlockadesModel()) {
            if (Util.hasIntersection(blockade.getPolygon(), targetLine)) {
                return true;
            }
        }
        return false;
    }


    public List<Point2D> blockadesIntersections(Area area, Line2D targetLine) {
        if (!(area instanceof Road) || targetLine == null) {
            return new ArrayList<Point2D>();
        }
        List<Point2D> points = new ArrayList<Point2D>();
        RoadModel road = getRoadModel(area.getID());
        for (BlockadeModel blockade : road.getBlockadesModel()) {
            points.addAll(Util.intersections(blockade.getPolygon(), targetLine));
        }
        return points;
    }

    /**
     * This method returns blockades which block the middle point of a road or two passing edges of way roads
     *
     * @param pathToGo entityID list of path entities to a target
     * @param target   target which police want to reach
     * @return blockades which might make troubles for path planning of any agents
     */
    public boolean shouldCheckForBlockadesOnWay(List<EntityID> pathToGo, EntityID target) {

        boolean shouldCheck = false;
        Set<BlockadeModel> obstacles_BlockadeModels = new HashSet<BlockadeModel>();
        Set<Blockade> blockades = new FastSet<Blockade>();


        EdgeModel sourceEdge = null;
        Pair<Integer, Integer> nextMiddlePoint = null;
        Pair<Integer, Integer> sourceMiddlePoint = null;


        if (pathToGo.isEmpty()) {
            shouldCheck = false;
        } else if (pathToGo.size() == 1) {
            if (worldInfo.getEntity(pathToGo.get(0)) instanceof Road) {
                Road road = (Road) worldInfo.getEntity(pathToGo.get(0));
                if (road.isBlockadesDefined()) {
                    if (worldInfo.getEntity(target) instanceof Human) {
                        Human human = (Human) worldInfo.getEntity(target);
                        if (Util.isOnBlockade(worldInfo, agentInfo, human) || Util.isNearBlockade(worldInfo, human)) {
                            shouldCheck = true;
                        } else {
                            shouldCheck = false;
                        }

                    } else {
                        Building building = (Building) worldInfo.getEntity(target);
                        Set<Edge> edges = RoadInfoComponent.getEdgesBetween((Area) worldInfo.getEntity(pathToGo.get(0)), building);
                        if (edges != null && !edges.isEmpty()) {
                            Edge edge = edges.iterator().next();
                            int middleX = (edge.getStartX() + edge.getEndX()) / 2;
                            int middleY = (edge.getStartY() + edge.getEndY()) / 2;

                            for (EntityID blockadeEntityID : road.getBlockades()) {
                                if (Util.findDistanceTo((Blockade) worldInfo.getEntity(blockadeEntityID), middleX, middleY) < ApolloConstants.AGENT_SIZE) {
                                    shouldCheck = true;
                                } else {
                                    shouldCheck = false;
                                }
                            }

                        }
                    }


                } else {
                    shouldCheck = true;
                }

            }
        } else {// if seenPath contains more than one entity
            int j;
            EntityID sourceAreaID = null;
            EntityID nextAreaID = null;
            Area sourceArea = null;
            Area nextArea = null;
            Set<Edge> edgeSet;
            RoadModel road = null;
            EdgeModel nextEdge = null;
            List<EntityID> neighbours;
            for (int i = 0; i < pathToGo.size() - 1; i++) {
                j = i + 1;
                sourceAreaID = pathToGo.get(i);
                nextAreaID = pathToGo.get(j);
                sourceArea = (Area) worldInfo.getEntity(sourceAreaID);
                nextArea = (Area) worldInfo.getEntity(nextAreaID);
                if (sourceArea instanceof Road) {
                    road = getRoadModel(sourceAreaID);
//                    neighbours = sourceArea.getNeighboursByEdge();
//                    for (EntityID neighbourID : neighbours) {
//                        // the neighbour of this area is also in my way, so it should be cleared
//                        if (pathToGo.contains(neighbourID) && pathToGo.indexOf(neighbourID)>pathToGo.indexOf(sourceAreaID)) {
//                    edgeSet = RoadInfoComponent.getEdgesBetween(sourceArea, nextArea);
                    Edge edge = sourceArea.getEdgeTo(nextArea.getID());
                    if (edge != null) {
                        nextEdge = road.getEdgeModel(edge);
                        nextMiddlePoint = new Pair<Integer, Integer>((int) nextEdge.getMiddle().getX(), (int) nextEdge.getMiddle().getY());
                        if (sourceEdge == null) {

                            if (agentInfo.getPositionArea().equals(sourceArea)) {
                                obstacles_BlockadeModels.addAll(getRoadObstacles(road, worldInfo.getLocation(agentInfo.getID()), nextMiddlePoint));
                            } else {
                                obstacles_BlockadeModels.addAll(getRoadObstacles(road, worldInfo.getLocation(sourceArea), nextMiddlePoint));
                            }
                        } else {
                            sourceMiddlePoint = new Pair<Integer, Integer>((int) sourceEdge.getMiddle().getX(), (int) sourceEdge.getMiddle().getY());
                            obstacles_BlockadeModels.addAll(getRoadObstacles(road, sourceMiddlePoint, nextMiddlePoint));
                        }

                        if (!obstacles_BlockadeModels.isEmpty()) {
                            shouldCheck = true;
                        }
                    }


                    if (shouldCheck) {
                        break;
                    }
//                        }
//                    }
//                    edgeSet = RoadInfoComponent.getEdgesBetween(sourceArea, nextArea);
//                    nextEdge=null;
//                    if(edgeSet.iterator().hasNext()){
//                        nextEdge=road.getEdgeModel(edgeSet.iterator().next());
//                    }
                    sourceEdge = nextEdge;


                } else {
                    if (nextArea instanceof Road) {
                        road = getRoadModel(nextAreaID);
                        neighbours = sourceArea.getNeighbours();
                        for (EntityID neighbourID : neighbours) {
                            // the neighbour of this area is also in my way, so it should be cleared
                            if (pathToGo.contains(neighbourID) && pathToGo.indexOf(neighbourID) > pathToGo.indexOf(sourceAreaID)) {
                                edgeSet = RoadInfoComponent.getEdgesBetween(sourceArea, nextArea);
                                for (Edge edge : edgeSet) {
                                    nextEdge = road.getEdgeModel(edge);
                                    nextMiddlePoint = new Pair<Integer, Integer>((int) nextEdge.getMiddle().getX(), (int) nextEdge.getMiddle().getY());
                                    if (sourceEdge == null) {

                                        if (agentInfo.getPositionArea().equals(sourceArea)) {
                                            obstacles_BlockadeModels.addAll(getRoadObstacles(road, worldInfo.getLocation(agentInfo.getID()), nextMiddlePoint));
                                        } else {
                                            obstacles_BlockadeModels.addAll(getRoadObstacles(road, worldInfo.getLocation(sourceArea), nextMiddlePoint));
                                        }
                                    } else {
                                        sourceMiddlePoint = new Pair<Integer, Integer>((int) sourceEdge.getMiddle().getX(), (int) sourceEdge.getMiddle().getY());
                                        obstacles_BlockadeModels.addAll(getRoadObstacles(road, sourceMiddlePoint, nextMiddlePoint));
                                    }

                                    if (!obstacles_BlockadeModels.isEmpty()) {
                                        shouldCheck = true;
                                    }
                                }

                                if (shouldCheck) {
                                    break;
                                }

                            }
                        }

                        if (shouldCheck) {
                            break;
                        }

                        edgeSet = RoadInfoComponent.getEdgesBetween(sourceArea, nextArea);
                        nextEdge = null;
                        if (edgeSet.iterator().hasNext()) {
                            nextEdge = road.getEdgeModel(edgeSet.iterator().next());
                        }
                        sourceEdge = nextEdge;
                    } else {
                        sourceEdge = null;
                    }
                }
            }


            if (!shouldCheck) {


                //find blockades of last Entity in the sequence
                if (nextArea instanceof Road) {
                    //target is an agent which is on the road
                    StandardEntity entity = worldInfo.getEntity(target);
                    if (sourceEdge != null) {
                        Set<BlockadeModel> apolloBlockades = getRoadObstacles(getRoadModel(nextArea.getID()), worldInfo.getLocation(entity), new Pair<Integer, Integer>((int) sourceEdge.getMiddle().getX(), (int) sourceEdge.getMiddle().getY()));
                        if (apolloBlockades != null && !apolloBlockades.isEmpty()) {
                            shouldCheck = true;
                        }
                    }
                } else {

                    //do nothing
                }

            }
        }


        return shouldCheck;
    }

    public boolean isInClearRange(Blockade target) {
        Pair<Integer, Integer> location = worldInfo.getLocation(agentInfo.getID());
        return Util.findDistanceTo(target, location.first(), location.second()) < maxClearDistance;
    }

    public Pair<Blockade, Integer> findNearestBlockade(Set<Blockade> blockades) {

        if (blockades == null || blockades.isEmpty()) {
            return null;
        }

        Pair<Integer, Integer> location = worldInfo.getLocation(agentInfo.getID());
        int x = location.first();
        int y = location.second();


        int minDistance = Integer.MAX_VALUE;
        Pair<Blockade, Integer> nearestBlockadePair = new Pair<Blockade, Integer>(blockades.iterator().next(), 0);
        int tempDistance;
        for (Blockade blockade : blockades) {
//            tempDistance = Util.distance(world.getSelfHuman().getX(), world.getSelfHuman().getY(), blockade.getX(), blockade.getY());
            tempDistance = Util.findDistanceTo(blockade, x, y);
            if (tempDistance < minDistance) {
                minDistance = tempDistance;
                nearestBlockadePair = new Pair<Blockade, Integer>(blockade, minDistance);
            }
        }

        return nearestBlockadePair;
    }

    /**
     * THis method gets obstacles which has intersect/s with a line from {@code firstPoint} to {@code secondPoint}
     *
     * @param road     the road we want found obstacles of
     * @param firstPoint  head point of the expressed line
     * @param secondPoint end point of the expressed line
     * @return set of {@code BlockadeModel} which has intersect to the expressed line
     */
    public Set<BlockadeModel> getRoadObstacles(RoadModel road, Pair<Integer, Integer> firstPoint, Pair<Integer, Integer> secondPoint) {
        Set<BlockadeModel> obstacles = new HashSet<BlockadeModel>();
        for (BlockadeModel blockade : road.getBlockadesModel()) {
            Point2D sourcePoint = Util.getPoint(firstPoint);
            Point2D endPoint = Util.getPoint(secondPoint);
            if (blockade.getPolygon().contains(firstPoint.first(), firstPoint.second()) || Util.intersections(blockade.getPolygon(), new Line2D(sourcePoint, endPoint)).size() > 0
                    || Util.findDistanceTo(blockade.getParent(), firstPoint.first(), firstPoint.second()) < 500
                    || Util.findDistanceTo(blockade.getParent(), secondPoint.first(), secondPoint.second()) < 500) {
                obstacles.add(blockade);
            }
        }

        return obstacles;
    }


    public Set<Blockade> getBlockadesInRange(Pair<Integer, Integer> targetLocation, Set<Blockade> blockadeSeen, int range) {
        Set<Blockade> blockades = new HashSet<Blockade>();
        for (Blockade blockade : blockadeSeen) {
            if (Util.findDistanceTo(blockade, targetLocation.first(), targetLocation.second()) < range) {
                blockades.add(blockade);
            }
        }
        return blockades;
    }

    public Set<Blockade> getBlockadesInRange(Road road, Set<Blockade> blockadeSeen) {

        Set<Blockade> blockadeSet = new FastSet<Blockade>();
        for (Blockade blockade : blockadeSeen) {
            for (Edge edge : road.getEdges()) {
                if (edge.isPassable() && Util.findDistanceTo(blockade, road.getX(), road.getY()) < 1200) {
                    blockadeSet.add(blockade);
                }

            }
        }

        return blockadeSet;

    }

    public Set<Blockade> getTargetRoadBlockades(Road road) {
        Set<Blockade> blockades = new FastSet<Blockade>();
        if (worldInfo.getChanged().getChangedEntities().contains(road.getID()) && road.isBlockadesDefined()) {
            for (EntityID entityID : road.getBlockades()) {
                blockades.add((Blockade) worldInfo.getEntity(entityID));
            }
        }
        return blockades;
    }

    /**
     * this method calculate and return 2 parallel line around clear length lines at the specific distance from each of them to avoid rounding fault
     *
     * @param clearLine1  clear length line1
     * @param clearLine2  clear length line2
     * @param clearRadius clear radius
     * @param distance    distance of wanted lines from each length lines
     * @return pair of parallel line in the specific distance from entry lines {@code clearLine1} and {@code clearLine2}
     */

    public Pair<Line2D, Line2D> getClearSecureLines(Line2D clearLine1, Line2D clearLine2, double clearRadius, double distance) {
        Line2D l1, l2;
        double x1, y1, x2, y2;
        Point2D origin1, endPoint1;
        Point2D origin2, endPoint2;
        double ratio = distance / (clearRadius * 2);
        double minX1, maxX1, minY1, maxY1;
        double minX2, maxX2, minY2, maxY2;

        origin1 = clearLine1.getOrigin();
        endPoint1 = clearLine1.getEndPoint();
        origin2 = clearLine2.getOrigin();
        endPoint2 = clearLine2.getEndPoint();

        minX1 = Math.min(origin1.getX(), origin2.getX());
        maxX1 = Math.max(origin1.getX(), origin2.getX());
        minY1 = Math.min(origin1.getY(), origin2.getY());
        maxY1 = Math.max(origin1.getY(), origin2.getY());
        minX2 = Math.min(endPoint1.getX(), endPoint2.getX());
        maxX2 = Math.max(endPoint1.getX(), endPoint2.getX());
        minY2 = Math.min(endPoint1.getY(), endPoint2.getY());
        maxY2 = Math.max(endPoint1.getY(), endPoint2.getY());

        x1 = ratio * (maxX1 - minX1);
        y1 = ratio * (maxY1 - minY1);
        x2 = ratio * (maxX2 - minX2);
        y2 = ratio * (maxY2 - minY2);

        Point2D p1 = new Point2D(
                origin1.getX() == maxX1 ? maxX1 - x1 : minX1 + x1,
                origin1.getY() == maxY1 ? maxY1 - y1 : minY1 + y1);
        Point2D p2 = new Point2D(
                endPoint1.getX() == maxX2 ? maxX2 - x2 : minX2 + x2,
                endPoint1.getY() == maxY2 ? maxY2 - y2 : minY2 + y2);
        l1 = new Line2D(p1, p2);


        p1 = new Point2D(
                origin2.getX() == maxX1 ? maxX1 - x1 : minX1 + x1,
                origin2.getY() == maxY1 ? maxY1 - y1 : minY1 + y1);
        p2 = new Point2D(
                endPoint2.getX() == maxX2 ? maxX2 - x2 : minX2 + x2,
                endPoint2.getY() == maxY2 ? maxY2 - y2 : minY2 + y2);
        l2 = new Line2D(p1, p2);

        l1 = Util.clipLine(l1, Util.lineLength(l1) - scenarioInfo.getClearRepairRad() / 2 + 100, true);
        l2 = Util.clipLine(l2, Util.lineLength(l2) - scenarioInfo.getClearRepairRad() / 2 + 100, true);
        return new Pair<Line2D, Line2D>(l1, l2);
    }

    public List<StandardEntity> getEntities(List<EntityID> idList) {
        List<StandardEntity> entities = new ArrayList<>();
        for (EntityID id : idList) {
            entities.add(worldInfo.getEntity(id));
        }
        return entities;
    }

    public Pair<Integer, Integer> getSelfLocation() {
        return worldInfo.getLocation(agentInfo.getID());
    }


    public boolean anyBlockadeIntersection(Collection<Area> areasSeenInPath, Line2D targetLine, boolean secure) {
        Line2D line;
        if (secure) {
            double length = Util.lineLength(targetLine);
            double secureSize = 510 + SECURE_RANGE;
            if (length - secureSize <= 0) {
                world.printData("The clear line is too short.....");
                return false;
            }
            line = Util.improveLine(targetLine, -secureSize);
        } else {
            line = targetLine;
        }
        for (Area area : areasSeenInPath) {
            if (anyBlockadeIntersection(area, line)) {
                return true;
            }
        }
        return false;
    }

    public Point2D anyBlockadeIntersection(GuideLine guideLine) {
        List<Area> areas = new ArrayList<Area>();
        for (StandardEntity entity : getEntities(guideLine.getAreas())) {
            areas.add((Area) entity);
        }
        Point2D nearestPoint = null;
        Point2D agentLocation = Util.getPoint(getSelfLocation());
        int minDist = Integer.MAX_VALUE;
        for (Area area : areas) {

            List<Point2D> intersects = blockadesIntersections(area, guideLine);
            for (Point2D point2D : intersects) {
                int dist = Util.distance(point2D, agentLocation);
                if (dist < minDist) {
                    minDist = dist;
                    nearestPoint = point2D;
                }
            }
        }
        return nearestPoint;
    }

    public Line2D getTargetClearLine(List<Area> areasSeenInPath, double range, GuideLine guideLine) {
        Point2D agentPosition = Util.getPoint(getSelfLocation());

        if (!isNeedToClear(agentPosition, areasSeenInPath, guideLine)) {
            return null;
        }
        return getClearLine(agentPosition, guideLine, range);
    }

    public boolean isNeedToClear(Point2D agentLocation, List<Area> areasSeenInPath, Line2D guideLine) {
        if (guideLine == null) {
            return false;
        }
        if (Util.distance(guideLine, agentLocation) > ApolloConstants.AGENT_SIZE) {
            return true;
        }
//        List<Area> areasSeenInPath = getAreasSeenInPath(path);
        for (Area area : areasSeenInPath) {
            if (area.isBlockadesDefined()) {
                for (EntityID blockID : area.getBlockades()) {
                    Blockade blockade = (Blockade) worldInfo.getEntity(blockID);
                    if (blockade != null) {
                        Polygon blockadePoly = Util.getPolygon(blockade.getApexes());
                        if (!Util.isPassable(blockadePoly, guideLine, ApolloConstants.AGENT_PASSING_THRESHOLD)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public Line2D getClearLine(Point2D agentPosition, GuideLine guideLine, double range) {
        Line2D targetLine = new Line2D(agentPosition, guideLine.getDirection());

        ////////////////////////////////////////////////////////////////////
        //rotate target line for containing traffic simulator move points //
//        Polygon clearRectangle = Util.clearAreaRectangle(targetLine.getOrigin(), targetLine.getEndPoint(), getClearRadius());
        ////////////////////////////////////////////////////////////////////

        return Util.clipLine(targetLine, range - SECURE_RANGE);
    }



    public Point2D getBetterPosition(Line2D guideline, double distanceThreshold) {
        Point2D agentLocation = Util.getPoint(getSelfLocation());
        Point2D betterPosition = null;
        Point2D pointOnGuideline = Util.closestPoint(guideline, agentLocation);

        Area selfPosition = (Area) getSelfPosition();
        if (!selfPosition.getShape().contains(pointOnGuideline.getX(), pointOnGuideline.getY())) {
            List<Point> pointList = Util.getPointList(selfPosition.getApexList());
            Line2D line;
            Point2D p1, p2, nearestPoint = null;
            Point point;
            int minDistance = Integer.MAX_VALUE;
            for (int i = 0; i < pointList.size(); i++) {
                point = pointList.get(i);
                p1 = new Point2D(point.getX(), point.getY());
                point = pointList.get((i + 1) % pointList.size());
                p2 = new Point2D(point.getX(), point.getY());
                line = new Line2D(p1, p2);
                Point2D intersection = Util.getIntersection(line, guideline);
                int distance = Util.distance(agentLocation, intersection);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestPoint = intersection;
                }
            }

            betterPosition = nearestPoint;
        } else {
            betterPosition = pointOnGuideline;
        }

        if (betterPosition == null || Util.distance(betterPosition, agentLocation) < distanceThreshold) {
            //it means guideline is too close to me. so no need to move on it
            return null;
        }

        return betterPosition;
    }

    public Area getSelfPosition() {
        return agentInfo.getPositionArea();
    }


    public int getClearRadius() {
        return scenarioInfo.getClearRepairRad();
    }

    /**
     * @param path
     * @return
     */
    public List<Area> getAreasSeenInPath(List<EntityID> path) {
        List<Area> areasSeenInPath = new ArrayList<Area>();
        Area area;
        for (EntityID id : path) {
            area = (Area) worldInfo.getEntity(id);
            if (worldInfo.getChanged().getChangedEntities().contains(id)) {
                areasSeenInPath.add(area);
            } else {
                break;
            }
        }
        return areasSeenInPath;
    }


    public Point2D getTargetClearPoint(List<EntityID> path, double range) {
        if (path == null || range < 0) {
            return null;
        }
//        final double minimumRangeThreshold = range * clearRangeYieldCoefficient;

        Area area;
        Point2D targetPoint = null;
        Point2D positionPoint = Util.getPoint(getSelfLocation());
        if (path.size() <= 1) {
            area = (Area) worldInfo.getEntity(getSelfPosition().getID());
            Point2D areaCenterPoint = Util.getPoint(worldInfo.getLocation(area));
            targetPoint = Util.clipLine(new Line2D(positionPoint, areaCenterPoint), range).getEndPoint();
        } else if (path.size() > 1) {
            area = (Area) worldInfo.getEntity(path.get(0));
            Edge edge = area.getEdgeTo(path.get(1));
            if (edge == null) {
                return null;
            }
            Point2D areaCenterPoint = Util.getPoint(worldInfo.getLocation(area));
            Point2D edgeCenterPoint = Util.getMiddle(edge.getLine());
            Point2D targetPoint2D = Util.clipLine(new Line2D(areaCenterPoint, edgeCenterPoint), range).getEndPoint();
//            targetPoint = Util.clipLine(new Line2D(positionPoint, edgeCenterPoint), range).getEndPoint();
            //guideline is the line from agent location area center toward edge to next area
            //to avoid bad shape clearing, guideline help pfs to clear in one direction in path
            double deltaX = positionPoint.getX() - areaCenterPoint.getX();
            double deltaY = positionPoint.getY() - areaCenterPoint.getY();
//            if(Util.contains())
            targetPoint2D = new Point2D(targetPoint2D.getX() + deltaX, targetPoint2D.getY() + deltaY);//rotate line to set it as parallel of guideline
            Polygon clearPoly = Util.clearAreaRectangle(positionPoint, targetPoint2D, getClearRadius());
            if (clearPoly.contains(edgeCenterPoint.getX(), edgeCenterPoint.getY())) {
                targetPoint = Util.clipLine(new Line2D(positionPoint, targetPoint2D), range).getEndPoint();
            } else {
                targetPoint = Util.clipLine(new Line2D(positionPoint, edgeCenterPoint), range).getEndPoint();
            }
        }

        List<Area> areasSeenInPath = getAreasSeenInPath(path);
        //target point is point that agent want to clear up to it.
//        List<EntityID> checkedAreas = new ArrayList<EntityID>();
//        Polygon polygon = null;
        if (targetPoint != null) {
            Line2D targetLine = new Line2D(positionPoint, targetPoint);
//            polygon = Util.clearAreaRectangle(positionPoint.getX(), positionPoint.getY(), targetPoint.getX(), targetPoint.getY(), getClearRadius());
//            Area neighbour;
//            cleaningBefore = false;
//            for (Road road : roadsSeenInPath) {//3 loop
//                for (EntityID id : road.getNeighboursByEdge()) {
//                    if (checkedAreas.contains(id) || path.contains(id)) {
//                        //this area checked before...
//                        continue;
//                    }
//                    checkedAreas.add(id);
//                    neighbour = worldInfo.getEntity(id, Area.class);
//                    if (!(neighbour instanceof Road)) {
//                        //this area is not road! so no blockades is in it.
//                        continue;
//                    }
//                    targetLine = normalizeClearLine(neighbour, targetLine, polygon, minimumRangeThreshold);
//                    if (targetLine == null) {
//                        return null;
//                    }
//                }
//            }
            Pair<Line2D, Line2D> clearLengthLines = Util.clearLengthLines(Util.clearAreaRectangle(Util.getPoint(getSelfLocation()), targetLine.getEndPoint(), getClearRadius()), getClearRadius());
            double distance = (getClearRadius() - ApolloConstants.AGENT_SIZE * 0.5) / 2;

            Pair<Line2D, Line2D> clearSecureLines = getClearSecureLines(clearLengthLines.first(), clearLengthLines.second(), getClearRadius(), distance);
            // MrlPersonalData.VIEWER_DATA.setPFClearAreaLines(agentInfo.getID(), targetLine, clearSecureLines.first(), clearSecureLines.second());
            if (anyBlockadeIntersection(areasSeenInPath, targetLine, true) ||
                    anyBlockadeIntersection(areasSeenInPath, clearSecureLines.first(), false) ||
                    anyBlockadeIntersection(areasSeenInPath, clearSecureLines.second(), false)) {
                return targetLine.getEndPoint();
            } else {
//                return beforeClearPoint(targetLine);
            }
        }
        return null;
    }


}