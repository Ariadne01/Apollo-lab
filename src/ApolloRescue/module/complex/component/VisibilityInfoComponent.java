package ApolloRescue.module.complex.component;

import ApolloRescue.module.algorithm.convexhull.PolygonUtil;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.entities.BlockadeModel;
import ApolloRescue.module.universal.entities.BuildingModel;
import ApolloRescue.module.universal.entities.RoadModel;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.Line2D;
import java.io.*;
import java.util.*;

public class VisibilityInfoComponent implements IComponent{
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(VisibilityInfoComponent.class);

    private static final IntersectionSorter INTERSECTION_SORTER = new IntersectionSorter();
    private int rayCount;
    private static final int DEFAULT_RAY_COUNT = 720;
    private static final String VIEW_DISTANCE_KEY = "perception.los.max-distance";
    private static final String RAY_COUNT_KEY = "perception.los.ray-count";
    private static final int DEFAULT_VIEW_DISTANCE = 30000;
    private int viewDistance;
    private ApolloWorld world;
    protected ScenarioInfo scenarioInfo;
    protected AgentInfo agentInfo;
    protected WorldInfo worldInfo;

    public VisibilityInfoComponent(ApolloWorld world, ScenarioInfo scenarioInfo, AgentInfo agentInfo, WorldInfo worldInfo) {
        this.world = world;
        this.scenarioInfo = scenarioInfo;
        this.agentInfo = agentInfo;
        this.worldInfo = worldInfo;
        rayCount = scenarioInfo.getRawConfig().getIntValue(RAY_COUNT_KEY, DEFAULT_RAY_COUNT);
        viewDistance = scenarioInfo.getPerceptionLosMaxDistance();
    }

    @Override
    public void init() {
        setBuildingsVisitablePart();
    }

    @Override
    public void update() {
        if (world.getSelfHuman() == null) {
            return;
        }
        BuildingModel BuildingModel;
        //if agent position is in building, and distance between them less than agent view distance, building is visited.
        if (world.getSelfPosition() instanceof Building) {
            BuildingModel = world.getBuildingModel(world.getSelfPosition().getID());
            int distance = Util.distance(worldInfo.getLocation(agentInfo.getID()), worldInfo.getLocation(BuildingModel.getSelfBuilding().getID()));
            if (distance < scenarioInfo.getPerceptionLosMaxDistance()) {
                world.setBuildingVisited(BuildingModel.getID(), true);

                //The following instructions are used to remove civilians which was seen before in this position
                //but now they were moved from here
                Set<Civilian> toRemoves = new HashSet<Civilian>();
                for (Civilian civ : BuildingModel.getCivilians()) {
                    if (!world.getCiviliansSeen().contains(civ)) {
                        toRemoves.add(civ);
                    }
                }
                BuildingModel.getCivilians().removeAll(toRemoves);
            }
        }

        for (Building building : world.getBuildingsSeen()) {
            BuildingModel = world.getBuildingModel(building.getID());
            if (BuildingModel.isVisited()) {
                world.updateEmptyBuildingState(BuildingModel, true);//进一步判断其是否为空
                continue;
            }
            if (canISeeInside(BuildingModel)) {//若未访问过
                world.setBuildingVisited(building.getID(), true);//进一步判断其是否为空
            }
            updateVisibility(BuildingModel);
        }
    }

    private void updateVisibility(BuildingModel BuildingModel) {//可访问性
        RoadModel roadModel = null;
        java.util.List<Polygon> polygons;
        BuildingModel.getCenterVisitRoadPoints().clear();
        Set<EntityID> roadSet = BuildingModel.getCenterVisitRoadShapes().keySet();
        if (roadSet.isEmpty()) {
            BuildingModel.setVisitable(BuildingModel.isReachable());
        } else {
            Set<Point> toAdd = new HashSet<Point>();
            boolean visitable = false;
            for (EntityID road : roadSet) {
                polygons = BuildingModel.getCenterVisitRoadShapes().get(road);
                roadModel = world.getRoadModel(road);
                if (!roadModel.isReachable()) {
                    continue;
                }
                for (Polygon polygon : polygons) {
                    Point point = Util.getPointInPolygon(polygon);//中心点
                    if (!roadModel.isSeen() || roadModel.getBlockadesModel().isEmpty()) {
                        visitable = true;
                        BuildingModel.addCenterVisitRoadPoints(roadModel, point);
                    } else {//  roadModel.isSeen() && 有障碍物
                        for (BlockadeModel blockade : roadModel.getBlockadesModel()) {
                            if (point != null) {
                                if (blockade.getPolygon().contains(point)) {
                                    toAdd.remove(point);
                                    break;
                                } else {
                                    visitable = true;
                                    toAdd.add(point);
                                }
                            }
                        }
                    }
                }
            }
            for (Point point : toAdd) {
                BuildingModel.addCenterVisitRoadPoints(roadModel, point);
            }
            BuildingModel.setVisitable(visitable);
        }
    }

    public boolean isInsideVisible(Point p1, Point p2, Edge edge, int range) {
        int dist = Util.distance(p1, p2);
        if (dist > range) {
            return false;
        }
        Point e1 = new Point(edge.getStartX(), edge.getStartY());
        Point e2 = new Point(edge.getEndX(), edge.getEndY());

        Line2D line1 = new Line2D.Double(p1, p2);
        Line2D line2 = new Line2D.Double(e1, e2);
        return line1.intersectsLine(line2);
    }

    /**
     * Determine list of shapes that agent can see center range of this building from into it!<br/>
     * <b color="RED">Note:</b> This function should call once.
     */
    public void createCenterVisitShapes(BuildingModel BuildingModel) {
        int xs[] = new int[6], ys[] = new int[6];
        for (Edge edge : BuildingModel.getSelfBuilding().getEdges()) {
            if (edge.isPassable()) {
                Pair<Point2D, Point2D> twoPoints = BuildingModel.getEdgeVisibleCenterPoints().get(edge);
                Point2D c1 = new Point2D(twoPoints.first().getX(), twoPoints.first().getY());
                Point2D c2 = new Point2D(twoPoints.second().getX(), twoPoints.second().getY());
                Point2D e1, e2, a1, a2;
                rescuecore2.misc.geometry.Line2D l1 = new rescuecore2.misc.geometry.Line2D(c1, edge.getStart());
                rescuecore2.misc.geometry.Line2D l2 = new rescuecore2.misc.geometry.Line2D(c2, edge.getEnd());
                if (!Util.intersects(l1, l2)) {
                    e1 = edge.getStart();
                    e2 = edge.getEnd();
                } else {
                    e1 = edge.getEnd();
                    e2 = edge.getStart();
                }
                int viewRange = scenarioInfo.getPerceptionLosMaxDistance();
                a1 = Util.getPointInDistance(new Line2D.Double(c1.getX(), c1.getY(), e1.getX(), e1.getY()), c1, viewRange);
                a2 = Util.getPointInDistance(new Line2D.Double(c2.getX(), c2.getY(), e2.getX(), e2.getY()), c2, viewRange);
                xs[0] = (int) c1.getX();
                xs[1] = (int) e1.getX();
                xs[2] = (int) a1.getX();
                xs[3] = (int) a2.getX();
                xs[4] = (int) e2.getX();
                xs[5] = (int) c2.getX();
                ys[0] = (int) c1.getY();
                ys[1] = (int) e1.getY();
                ys[2] = (int) a1.getY();
                ys[3] = (int) a2.getY();
                ys[4] = (int) e2.getY();
                ys[5] = (int) c2.getY();
                Polygon shape = new Polygon(xs, ys, 6);
                setRoadPartOfPolygon(shape, BuildingModel);
                BuildingModel.addCenterVisitShapes(shape);
            }
        }
    }

    private Set<StandardEntity> getVisibleEntities(BuildingModel BuildingModel) {
        Set<StandardEntity> visibleEntities = new HashSet<StandardEntity>();
        Area area1, area2;
        for (EntityID neighbourID : BuildingModel.getSelfBuilding().getNeighbours()) {
            area1 = (Area) worldInfo.getEntity(neighbourID);
            visibleEntities.add(area1);
            for (EntityID id : area1.getNeighbours()) {
                area2 = (Area) worldInfo.getEntity(id);
                visibleEntities.add(area2);
//                for (EntityID id2 : area2.getNeighboursByEdge()) {
//                    visibleEntities.add(world.getEntity(id2));
//                }
            }
        }
//        int viewRange = world.getPlatoonAgent().viewDistance;                //todo findVisible method has huge process time.......
//        Pair<Integer, Integer> location = BuildingModel.getSelfBuilding().getLocation(world);
//        Point2D point = new Point2D(location.first(), location.second());
//        findVisible(world.getSelfHuman(), point, world.getObjectsInRange(location.first(), location.second(), (int) (viewRange / 0.64)));
        return visibleEntities;
    }

    private void setRoadPartOfPolygon(Polygon shapePolygon, BuildingModel BuildingModel) {
        Set<StandardEntity> visibleEntities = new HashSet<StandardEntity>(getVisibleEntities(BuildingModel));
        for (StandardEntity entity : visibleEntities) {
            Polygon polygon;
            if (entity instanceof Road) {
                RoadModel roadModel = world.getRoadModel(entity.getID());
                polygon = PolygonUtil.retainPolygon(shapePolygon, roadModel.getPolygon());
                if (polygon.npoints > 2) {
                    BuildingModel.addCenterVisitRoadShapes(roadModel, polygon);
                }
            }
        }
    }

    public void setBuildingsVisitablePart() {
        long now = System.currentTimeMillis();
//        File file = new File("data/" + world.getMapName() + ".poly");
//        if (file.exists()) {
//            fillFromFile(file);
//        } else {
        for (BuildingModel BuildingModel : world.getBuildingsModel()) {
            createCenterVisitShapes(BuildingModel);
            updateVisibility(BuildingModel);
        }
//            if (!writeIntoFile(file)) {
//                System.out.println("lp:unable to write into file!");
//            }
//        }

//        System.out.print(" lp-(" + (System.currentTimeMillis() - now) + ")");
    }

    /**
     * this method write buildings visitable polygons into file with .poly extension by following pattern
     * EntityID(BuildingID) + " " + int(road counts)
     * foreach road:
     * EntityID(roadID) + " " + int(Polygon Count)
     * each polygon in separate line
     * int(x) + "," + int(y) + " " foreach point of polygon #i
     *
     * @param file file which data should be written in it
     * @return true if success to write and false if else!
     */
    private boolean writeIntoFile(File file) {
        PrintWriter printWriter;
        try {
            printWriter = new PrintWriter(file);
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        String result = "";
        Set<EntityID> centerVisitRoads;
        java.util.List<Polygon> polygonList;
        BuildingModel BuildingModel;
        Map<EntityID, java.util.List<Polygon>> centerVisitRoadShapes;
        java.util.List<Polygon> centerVisitShapes;
        for (int i = 0, BuildingModelsSize = world.getBuildingsModel().size(); i < BuildingModelsSize; i++) {
            BuildingModel = world.getBuildingsModel().get(i);
            centerVisitShapes = BuildingModel.getCenterVisitShapes();
            centerVisitRoadShapes = BuildingModel.getCenterVisitRoadShapes();
            int centerVisitRoadShapesSize = centerVisitRoadShapes.size();
            int centerVisitShapeSize = centerVisitShapes.size();
            result += BuildingModel.getID() + " " + centerVisitShapeSize + " " + centerVisitRoadShapesSize + "\n";
            for (Polygon shape : centerVisitShapes) {
                for (int ni = 0; ni < shape.npoints; ni++) {
                    result += shape.xpoints[ni] + "," + shape.ypoints[ni];
                    if (ni < shape.npoints - 1) {
                        result += " ";
                    }
                }
                result += "\n";
            }
            int index = 0;
            centerVisitRoads = centerVisitRoadShapes.keySet();
            for (EntityID roadID : centerVisitRoads) {
                result += roadID;
                polygonList = centerVisitRoadShapes.get(roadID);
                result += " " + polygonList.size() + "\n";
                int pi = 0;
                for (Polygon polygon : polygonList) {
                    for (int ni = 0; ni < polygon.npoints; ni++) {
                        result += polygon.xpoints[ni] + "," + polygon.ypoints[ni];
                        if (ni < polygon.npoints - 1) {
                            result += " ";
                        }
                    }
                    if (pi < polygonList.size() - 1) {
                        result += "\n";
                    }
                    pi++;
                }

                if (index < centerVisitRoadShapesSize - 1) {
                    result += "\n";
                }
                index++;
            }
            if (i < BuildingModelsSize - 1) {
                result += "\n";
            }
        }
        printWriter.print(result);
        printWriter.flush();
        printWriter.close();
        return true;
    }

    /**
     * fill data from file which written by writeIntoFile() function.
     *
     * @param file file which created by writeIntoFile form this class
     */
    private void fillFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            BuildingModel BuildingModel;
            RoadModel roadModel;
            EntityID id;
            line = reader.readLine();//BuildingID VisitableShape VisibleRoadCount
            while (line != null) {
                if (line.isEmpty()) {
                    line = reader.readLine();
                    continue;
                }
                String parts[] = line.split(" ");
                id = new EntityID(Integer.parseInt(parts[0]));
                int shapeCount = Integer.parseInt(parts[1]);
                int roadCount = Integer.parseInt(parts[2]);
                BuildingModel = world.getBuildingModel(id);
                if (shapeCount < 1) {
                    System.out.println("building(" + id + ") have zero shape count");
                }
                for (int j = 0; j < shapeCount; j++) {
                    line = reader.readLine();//Coordination with space between them
                    if (line.isEmpty()) {
                        continue;
                    }
                    parts = line.split(" ");
                    Polygon polygon;
                    int size = parts.length;
                    int xsArray[] = new int[size], ysArray[] = new int[size];
                    int index = 0;
                    for (String part : parts) {
                        String xy[] = part.split(",");
                        xsArray[index] = Integer.parseInt(xy[0]);
                        ysArray[index] = Integer.parseInt(xy[1]);
                        index++;
                    }

                    polygon = new Polygon(xsArray, ysArray, size);
                    BuildingModel.addCenterVisitShapes(polygon);
                }
                for (int ri = 0; ri < roadCount; ri++) {
                    line = reader.readLine();//RoadID PolygonCount
                    if (line.isEmpty()) {
                        continue;
                    }
                    parts = line.split(" ");
                    id = new EntityID(Integer.parseInt(parts[0]));
                    int count = Integer.parseInt(parts[1]);
                    for (int i = 0; i < count; i++) {
                        line = reader.readLine();//Coordination with " " between them
                        if (line.isEmpty()) {
                            continue;
                        }
                        parts = line.split(" ");
                        Polygon polygon;
                        int size = parts.length;
                        int xsArray[] = new int[size], ysArray[] = new int[size];
                        int index = 0;
                        for (String part : parts) {
                            String xy[] = part.split(",");
                            xsArray[index] = Integer.parseInt(xy[0]);
                            ysArray[index] = Integer.parseInt(xy[1]);
                            index++;
                        }

                        polygon = new Polygon(xsArray, ysArray, size);
                        roadModel = world.getRoadModel(id);
                        BuildingModel.addCenterVisitRoadShapes(roadModel, polygon);
                    }
                }
                updateVisibility(BuildingModel);
                line = reader.readLine();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * this method check whether entry building can be seen in this cycle by current agent or not.
     *
     * @param BuildingModel BuildingModel that should check
     * @return true if this cycle agent can see inside of building
     */
    public boolean canISeeInside(BuildingModel BuildingModel) {

        ArrayList<Building> buildingSeen = new ArrayList<Building>(world.getBuildingsSeen());
        if (!buildingSeen.contains(BuildingModel.getSelfBuilding())) {
            return false;
        }
        boolean canSee = false;
        boolean inBuilding = false;
        Pair<Integer, Integer> locationPair = worldInfo.getLocation(agentInfo.getID());
        rescuecore2.misc.geometry.Point2D locationPoint = new rescuecore2.misc.geometry.Point2D(locationPair.first(), locationPair.second());
        StandardEntity position = worldInfo.getPosition(agentInfo.getID());
        if (position instanceof Building) {
            if (position.equals(BuildingModel.getSelfBuilding())) {
                return true;
            }
            inBuilding = true;
        }
        
        FOR1:
        for (Polygon polygon : BuildingModel.getCenterVisitShapes()) {
            if (polygon.contains(locationPair.first(), locationPair.second())) {
                canSee = true;
                for (Edge edge : BuildingModel.getSelfBuilding().getEdges()) {
                    if (edge.isPassable()) {
                        Pair<Point2D, Point2D> centerPoints = BuildingModel.getCenterPointsFrom(edge);
                        FOR2:
                        for (Building building : buildingSeen) {
                            for (Edge edge1 : building.getEdges()) {
                                if (!edge1.isPassable()) {
                                    if (Util.intersects(edge1.getLine(), new rescuecore2.misc.geometry.Line2D(locationPoint, centerPoints.first())) ||
                                            Util.intersects(edge1.getLine(), new rescuecore2.misc.geometry.Line2D(locationPoint, centerPoints.second()))) {
                                        canSee = false;
                                        if (!inBuilding) {
                                            BuildingModel.getCenterVisitRoadShapes().remove(agentInfo.getPosition());
//                                            if (deleted != null)
//                                                world.printData("I'M in centerVisitShape But i can't see inside! now remove it from visitableParts." + world.getSelfPosition() + " From " + BuildingModel);
                                        }
                                        break FOR2;
                                    }
                                }
                            }
                        }
                        if (canSee) {
                            break FOR1;
                        }

                    }
                }
            }
        }
        return canSee;
    }
    ////////////////////////////////////////////////KERNEL FUNCTIONS///////////////////////////////////////>>>>>>>>>>>>>

    /**
     * this method gotten from kernel source
     */
    public Collection<StandardEntity> findVisible(StandardEntity agentEntity, rescuecore2.misc.geometry.Point2D location, Collection<StandardEntity> nearby) {
        Logger.debug("Finding visible entities from " + location);
        Logger.debug(nearby.size() + " nearby entities");
        Collection<LineInfo> lines = getAllLines(nearby);
        // Cast rays
        // CHECKSTYLE:OFF:MagicNumber
        double dAngle = Math.PI * 2 / rayCount;
        // CHECKSTYLE:ON:MagicNumber
        Collection<StandardEntity> result = new HashSet<StandardEntity>();
        for (int i = 0; i < rayCount; ++i) {
            double angle = i * dAngle;
            Vector2D vector = new Vector2D(Math.sin(angle), Math.cos(angle)).scale(viewDistance);
            Ray ray = new Ray(new rescuecore2.misc.geometry.Line2D(location, vector), lines);
            for (LineInfo hit : ray.getLinesHit()) {
                StandardEntity e = hit.getEntity();
                result.add(e);
            }
        }
        Logger.debug(agentEntity + " can see " + result);
        return result;
    }

    private Collection<LineInfo> getAllLines(Collection<StandardEntity> entities) {
        Collection<LineInfo> result = new HashSet<LineInfo>();
        for (StandardEntity next : entities) {
            if (next instanceof Building) {
                for (Edge edge : ((Building) next).getEdges()) {
                    rescuecore2.misc.geometry.Line2D line = edge.getLine();
                    result.add(new LineInfo(line, next, !edge.isPassable()));
                }
            }
            if (next instanceof Road) {
                for (Edge edge : ((Road) next).getEdges()) {
                    rescuecore2.misc.geometry.Line2D line = edge.getLine();
                    result.add(new LineInfo(line, next, false));
                }
            }
        }
        return result;
    }

    private static class Ray {
        /**
         * The ray itself.
         */
        /**
         * The visible length of the ray.
         */
        /**
         * List of lines hit in order.
         */
        private java.util.List<LineInfo> hit;

        public Ray(rescuecore2.misc.geometry.Line2D ray, Collection<VisibilityInfoComponent.LineInfo> otherLines) {
            java.util.List<Pair<LineInfo, Double>> intersections = new ArrayList<Pair<LineInfo, Double>>();
            // Find intersections with other lines
            for (LineInfo other : otherLines) {
                double d1 = ray.getIntersection(other.getLine());
                double d2 = other.getLine().getIntersection(ray);
                if (d2 >= 0 && d2 <= 1 && d1 > 0 && d1 <= 1) {
                    intersections.add(new Pair<LineInfo, Double>(other, d1));
                }
            }
            Collections.sort(intersections, INTERSECTION_SORTER);
            hit = new ArrayList<LineInfo>();
            for (Pair<LineInfo, Double> next : intersections) {
                LineInfo l = next.first();
                hit.add(l);
                if (l.isBlocking()) {
                    break;
                }
            }
        }

        public java.util.List<LineInfo> getLinesHit() {
            return Collections.unmodifiableList(hit);
        }
    }

    private static class LineInfo {
        private rescuecore2.misc.geometry.Line2D line;
        private StandardEntity entity;
        private boolean blocking;

        public LineInfo(rescuecore2.misc.geometry.Line2D line, StandardEntity entity, boolean blocking) {
            this.line = line;
            this.entity = entity;
            this.blocking = blocking;
        }

        public rescuecore2.misc.geometry.Line2D getLine() {
            return line;
        }

        public StandardEntity getEntity() {
            return entity;
        }

        public boolean isBlocking() {
            return blocking;
        }
    }

    private static class IntersectionSorter implements Comparator<Pair<LineInfo, Double>>, Serializable {
        @Override
        public int compare(Pair<LineInfo, Double> a, Pair<LineInfo, Double> b) {
            double d1 = a.second();
            double d2 = b.second();
            if (d1 < d2) {
                return -1;
            }
            if (d1 > d2) {
                return 1;
            }
            return 0;
        }
    }
}
