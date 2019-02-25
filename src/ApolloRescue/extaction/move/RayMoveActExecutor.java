package ApolloRescue.extaction.move;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.algorithm.convexhull.CompositeConvexHull;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.entities.BlockadeModel;
import ApolloRescue.module.universal.entities.EdgeModel;
import ApolloRescue.module.universal.entities.RoadModel;
import ApolloRescue.module.universal.tools.SmallestSurroundingRectangle;
import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RayMoveActExecutor {
    private ApolloWorld world;
    private double scaleSize;
    private LineOfSightForMovePerception perception;
    private boolean acceptInterrupt = true;
    private List<Point2D> movePointList;
//   this.agent = world.getPlatoonAgent();

    private WorldInfo worldInfo;
    private ScenarioInfo scenarioInfo;
    private AgentInfo agentInfo;


    public RayMoveActExecutor(ApolloWorld world, WorldInfo worldInfo, ScenarioInfo scenarioInfo, AgentInfo agentInfo) {
        this.worldInfo = worldInfo;
        this.scenarioInfo = scenarioInfo;
        this.agentInfo = agentInfo;
        this.scaleSize = ApolloConstants.AGENT_SIZE;
        this.perception = new LineOfSightForMovePerception(worldInfo, scenarioInfo, agentInfo);
        this.acceptInterrupt = true;
        movePointList = new ArrayList<>();
        this.world = world;

    }

    public Action execute(List<EntityID> plan) {//ljy：返回值 ActionState -> action
        //This needs here for complete previous task.
        if (continueTask()) {
            Action action = moveToPoints(movePointList);
            if (action != null) {//ljy: 新增
                return action;
            }
        }

        movePointList.clear();
        if (plan == null || plan.isEmpty()) {
            return null;//ljy：原ActionState.CANCELED;
        }

        Point2D location = Util.getPoint(getSelfLocation());
        Area currentPosition = agentInfo.getPositionArea();


        //find obstacle blockades which blocked my way.
        List<StandardEntity> obstacles = findObstacles(location, plan);//ljy: 传入的参数无实际用处
        if (obstacles.isEmpty()) {
            return null;
        }

        //make convex hull from obstacle blockades
        CompositeConvexHull mergedObstacles = mergeObstacles(obstacles);
//        SmallestSurroundingRectangle.get(mergedObstacles.getConvexHull());

        //scale blockades convex hull
//        scaleSize = Util.distance(mergedObstacles.getConvexPolygon(), location) + ApolloConstants.AGENT_SIZE / 2;
//        scaleSize = Math.max(scaleSize, ApolloConstants.AGENT_SIZE);
        Polygon scaledConvexHull = scale(mergedObstacles, scaleSize);

        //finding TargetPoint for escape obstacles

        Point2D centerOfCurrentLocation = Util.getPoint(worldInfo.getLocation(currentPosition));
        Point2D middleNextEdge = null;
        if (plan.size() > 1) {
            if (currentPosition instanceof Road) {
                RoadModel road = world.getApolloRoad(currentPosition.getID());

                for (EdgeModel edge : road.getEdgeModelsTo(plan.get(1))) {
                    if (!edge.isBlocked()) {
                        middleNextEdge = Util.getMiddle(edge.getOpenPart());
                        break;
                    }
                }
            }
            if (middleNextEdge == null) {
                middleNextEdge = Util.getMiddle(currentPosition.getEdgeTo(plan.get(1)).getLine());
            }
        } else {
            middleNextEdge = centerOfCurrentLocation;
        }

        Line2D guideLine = new Line2D(location, middleNextEdge);
        if (!Util.hasIntersection(mergedObstacles.getConvexPolygon(), guideLine)) {
            Util.improveLine(guideLine, ApolloConstants.AGENT_SIZE).getEndPoint();
            movePointList.add(Util.improveLine(guideLine, ApolloConstants.AGENT_SIZE).getEndPoint());
            Action action = moveToPoints(movePointList);
            if (action != null) {
                return action;
            }
        }
        Point2D targetPoint = findTargetPoint(scaledConvexHull, plan, location, guideLine, obstacles);


        Map<Line2D, List<Point2D>> pointsMadeForEscape;
        if (targetPoint != null) {
            int rayCount = 36;//ljy: 原类变量
            perception.setRayCount(rayCount);
            pointsMadeForEscape = perception.findEscapePoints(scaledConvexHull, obstacles, targetPoint);
            if (Util.hasIntersection(mergedObstacles.getConvexPolygon(), new Line2D(location, targetPoint))) {
                movePointList = findMovePointList(pointsMadeForEscape, targetPoint, guideLine);
            } else {
                movePointList = new ArrayList<>();
                movePointList.add(targetPoint);
            }
//            world.printData("###############################Ray Move Mode###############################");
            //MrlPersonalData.VIEWER_DATA.setScaledBlockadeData(agentInfo.getID(), obstacles, scaledConvexHull, pointsMadeForEscape, movePointList);
            Action action = moveToPoints(movePointList);
            if (action != null) {
                return action;
            }
        } else {
            //MrlPersonalData.VIEWER_DATA.setScaledBlockadeData(agentInfo.getID(), obstacles, scaledConvexHull, null, null);
        }


        return null;
    }


    private Pair<Integer, Integer> getSelfLocation() {
        return worldInfo.getLocation(agentInfo.getID());
    }

    private boolean continueTask() {
        return false;
    }

    private boolean anyBlockadeIntersection(Area area, Line2D targetLine) {
        RoadModel road = world.getApolloRoad(area.getID());
        for (BlockadeModel blockade : road.getBlockadesModel()) {
            if (Util.hasIntersection(blockade.getPolygon(), targetLine)) {
                return true;
            }
        }
        return false;
    }

    private Action moveToPoints(List<Point2D> movePointList) {
        if (movePointList != null && !movePointList.isEmpty()) {
            acceptInterrupt = false;
            Point2D movePoint = movePointList.remove(0);
            acceptInterrupt = movePointList.isEmpty();
            List<EntityID> thisPositionPlan = new ArrayList<>();
            thisPositionPlan.add(agentInfo.getPosition());
            return new ActionMove(thisPositionPlan, (int) movePoint.getX(), (int) movePoint.getY());
        } else {
            acceptInterrupt = true;
            return null;
        }
    }

    private Point2D findTargetPoint(Polygon convexHull, List<EntityID> plan, Point2D location, Line2D guideLine, List<StandardEntity> obstacles) {
        if (plan.size() < 2) {
            return null;
        }
        Area currentPosition = world.getEntity(plan.get(0), Area.class);

        //shibe khate amood bar @guideLine
        double m = -(1d / Util.slope(guideLine));
        //khode khate amood bar @guideLine
        Point2D endPoint = new Point2D(location.getX() + 10000, location.getY() + m * 10000);
        Line2D line2D = new Line2D(location, endPoint);
        Set<Point2D> intersections = Util.getIntersectionPoints(convexHull, line2D);
//        FOR1:
        for (Point2D next : intersections) {


            ConvexHull positionConvex = Util.convertToConvexHull(Util.getPolygon(currentPosition.getApexList()));

            GeometryFactory geometryFactory = new GeometryFactory();

            boolean contains = positionConvex.getConvexHull().contains(geometryFactory.createPoint(new Coordinate(next.getX(), next.getY())));
            if (contains) {
                // A line from agent position @location TO a temp target witch is on orthogonal line
                Point2D semiTarget = new Point2D(next.getX(), next.getY());
                Line2D line = new Line2D(location, semiTarget);

                //ignore in obstacle targetPoint
//                for (StandardEntity entity : obstacles) {
//                    if (obstacles instanceof Blockade) {
//                        Blockade blockade = (Blockade) entity;
//                        if (Util.hasIntersection(Util.getPolygon(blockade.getApexes()), line)) {
//                            continue FOR1;
//                        }
//                    }
//                }

                //line = Util.improveLine(line, ApolloConstants.AGENT_PASSING_THRESHOLD);
                Line2D expandedLine = Util.clipLine(line, scenarioInfo.getPerceptionLosMaxDistance());

//                Polygon boundPoly = Util.getPolygon(convexHull.getBounds2D());
                List<Line2D> lines = getSmallestBoundLines(convexHull);
                //MrlPersonalData.VIEWER_DATA.setObstacleBounds(agentInfo.getID(), lines);
//                List<Line2D> lines = Util.getLines(boundPoly);
//                List<Point2D> points = new ArrayList<>();
                for (Line2D ln : lines) {
                    Point2D intersection = Util.getIntersection(expandedLine, ln);
                    if (Util.contains(ln, intersection) && Util.contains(expandedLine, intersection)) {
//                        points.add(intersection);
                        return intersection;
                    }
                }
                return semiTarget;
            }
        }
        return null;
    }

    private List<Line2D> getSmallestBoundLines(Polygon polygon) {
        List<Line2D> line2Ds = new ArrayList<>();
        ConvexHull convexHull = Util.convertToConvexHull(polygon);

        com.vividsolutions.jts.geom.Polygon bound = SmallestSurroundingRectangle.get(convexHull.getConvexHull(), convexHull.getConvexHull().getFactory());
        Point2D p1, p2;
        int numPoints = bound.getNumPoints();
        for (int i = 0; i < numPoints; i++) {//5 time iteration
            Coordinate[] c = bound.getCoordinates();
            p1 = new Point2D(c[i].x, c[i].y);
            p2 = new Point2D(c[(i + 1) % numPoints].x, c[(i + 1) % numPoints].y);
            line2Ds.add(new Line2D(p1, p2));
        }

        return line2Ds;
    }


    //    @Override
    public boolean acceptInterrupt() {
        return acceptInterrupt;
    }

    private List<StandardEntity> findObstacles(Point2D location, List<EntityID> plan) {

        StandardEntity myPosition = agentInfo.getPositionArea();
//        List<StandardEntity> obstacles = new ArrayList<>();
        List<EntityID> obstacleIDs = new ArrayList<>();
        List<StandardEntity> restrictedObstacles = new ArrayList<>();

        // the distance of the nearest blockade witch is near to the current point
        // it may be in our road or in neighbour's one
        double threshold = ApolloConstants.AGENT_PASSING_THRESHOLD;

        if (myPosition instanceof Road) {
            Road myRoad = (Road) myPosition;
//            List<EntityID> blockadeIDs = myRoad.getBlockades();
            for (StandardEntity next : world.getEntities(myRoad.getNeighbours())) {
                if (next instanceof Road) {
                    Road neighbour = (Road) next;
                    if (neighbour.isBlockadesDefined()) {
                        obstacleIDs.addAll(neighbour.getBlockades());
                    }
                }
            }
//            obstacles.addAll(world.getEntities(blockadeIDs));

            obstacleIDs.addAll(myRoad.getBlockades());

            Blockade nearestBlockade = world.getEntity(Util.getNearest(worldInfo, obstacleIDs, agentInfo.getID()), Blockade.class);
            if (nearestBlockade == null) {
                return restrictedObstacles;
            }
            obstacleIDs.remove(nearestBlockade.getID());//temporarily removed because of 'find distance' step. if it contains itself in compare step distance got 0;


            for (EntityID obstacleID : obstacleIDs) {
                Blockade blockade;
                StandardEntity entity = worldInfo.getEntity(obstacleID);
                if (!(entity instanceof Blockade)) {
                    continue;
                }
                blockade = (Blockade) entity;
                double dist = Util.distance(Util.getPolygon(blockade.getApexes()), Util.getPolygon(nearestBlockade.getApexes()));
                if (dist < threshold) {
                    restrictedObstacles.add(blockade);
                }
            }
            restrictedObstacles.add(nearestBlockade);
        }

        return restrictedObstacles;
    }

    private CompositeConvexHull mergeObstacles(List<StandardEntity> obstacles) {
        CompositeConvexHull convexHull = new CompositeConvexHull();
        for (StandardEntity next : obstacles) {
            Blockade blockade;
            if (next instanceof Blockade) {
                blockade = (Blockade) next;
            } else {
                continue;
            }
            for (int i = 0; i < blockade.getApexes().length; i += 2) {
                convexHull.addPoint(blockade.getApexes()[i], blockade.getApexes()[i + 1]);
            }
        }

        return convexHull;
    }

    private Polygon scale(CompositeConvexHull mergedObstacles, double scaleSize) {
        return Util.scaleBySize3(mergedObstacles.getConvexPolygon(), scaleSize);
    }

    private List<Point2D> findMovePointList(Map<Line2D, List<Point2D>> rayPointList, Point2D targetPoint, Line2D guideLine) {
        double guidelineSlope = Util.slope(guideLine);
        Line2D bestLine = null;
        List<Point2D> bestPoint2DList = null;
        double minSlopeDiv = Double.MAX_VALUE;


        List<Point2D> result = new ArrayList<>();

        if (!rayPointList.isEmpty()) {
            for (Map.Entry<Line2D, List<Point2D>> entry : rayPointList.entrySet()) {
                double lineSlope = Util.slope(entry.getKey());
                double slopeDiv = Math.abs(1d - (lineSlope / guidelineSlope));
                if (slopeDiv < minSlopeDiv) {
                    minSlopeDiv = slopeDiv;
                    bestLine = entry.getKey();
                    bestPoint2DList = entry.getValue();
                }
            }
            if (bestLine != null && bestPoint2DList != null && !bestPoint2DList.isEmpty()) {
                Point2D p = bestPoint2DList.get(0);
                result.add(p);
                if (Util.distance(p, targetPoint) > ApolloConstants.AGENT_SIZE) {
                    result.add(targetPoint);
                }

            }
        } else {
            result.add(targetPoint);
        }
        return result;
    }
}
