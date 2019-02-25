package ApolloRescue.module.universal.entities;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.algorithm.convexhull.PolygonUtil;
import ApolloRescue.module.complex.component.EdgeInfoComponent;
import ApolloRescue.module.complex.component.RoadInfoComponent;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.newsearch.graph.GraphModule;
import ApolloRescue.module.universal.newsearch.graph.MyEdge;
import ApolloRescue.module.universal.newsearch.graph.Node;
import ApolloRescue.module.universal.tools.LimitedLineOfSightPerception;
import ApolloRescue.module.universal.tools.RayModel;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

public class RoadModel {

    public static Map<EntityID, Map<EntityID, RoadModel>> VIEWER_ROADS_MAP = new HashMap<EntityID, Map<EntityID, RoadModel>>();
    public static Map<Road, Set<Blockade>> VIEWER_ROAD_BLOCKADES = new FastMap<Road, Set<Blockade>>();
    private Map<EntityID, List<Polygon>> buildingVisitableParts;
    private Map<EdgeModel, HashSet<EdgeModel>> reachableEdges;
    private Set<BlockadeModel> veryImportantBlockades;
    private Set<BlockadeModel> importantBlockades;
    private List<BlockadeModel> blockadeModels;
    private List<EdgeModel> passableEdgeModels;
    private Set<EdgeModel> blockedEdges;
    private Polygon transformedPolygon;
    private HashSet<EdgeModel> openEdges;
    private List<Point2D> apexPoints;
    private List<RoadModel> childRoads;
    private List<EdgeModel> edgeModels;
    private Set<BlockadeModel> farNeighbourBlockades;
    private boolean isReachable;
    private int totalRepairCost;
    private boolean isPassable;
    private int lastSeenTime;
    private int lastUpdateTime;
    private int lastResetTime;
    private List<Edge> edges;
    private List<Path> paths;
    private Polygon polygon;
    private int groundArea;
    private boolean highway;
    private boolean freeway;
    private ApolloWorld world;
    private boolean isSeen;
    private int repairTime;
    private Road parent;
    private Set<EntityID> visibleFrom;
    private List<EntityID> observableAreas;
    private List<RayModel> lineOfSight;
    private List<BuildingModel> buildingsInExtinguishRange;
    private LimitedLineOfSightPerception lineOfSightPerception;

    private WorldInfo worldInfo;
    private AgentInfo agentInfo;
    private ScenarioInfo scenarioInfo;
    private DevelopData developData;
   // private List<MrlRay> lineOfSight;

    public RoadModel(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, Road parent, ApolloWorld world) {
        this.worldInfo = worldInfo;
        this.agentInfo = agentInfo;
        this.scenarioInfo = scenarioInfo;

        this.world = world;

//        this.roadHelper = roadHelper;
        this.parent = parent;
        initialize(parent, createEdgeModels(parent.getEdges()));
    }

//    public RoadModel(Road road, ApolloWorld world, List<EdgeModel> edgeModels) {
//        initialize(road, world, edgeModels);
//    }
//
//    public RoadModel(Road road, ApolloWorld world) {
//        this.parent = road;
//        initialize(road, world, createEdgeModels(road.getEdges()));
//    }

    private void initialize(Road road, List<EdgeModel> edgeModels) {
        setVisibleFrom(new FastSet<EntityID>());
        setObservableAreas(new ArrayList<EntityID>());
        this.highway = false;
        this.freeway = false;
        this.parent = road;


        this.edges = new ArrayList<Edge>(road.getEdges());
        this.apexPoints = new ArrayList<Point2D>();
        this.blockadeModels = new ArrayList<BlockadeModel>();
        this.childRoads = new ArrayList<RoadModel>();
        this.blockedEdges = new HashSet<EdgeModel>();
        this.reachableEdges = new FastMap<EdgeModel, HashSet<EdgeModel>>();
        paths = new ArrayList<Path>();
        this.isPassable = true;
        this.isReachable = true;
        this.lastSeenTime = 0;
        this.totalRepairCost = 0;
        this.repairTime = 0;
        lastResetTime = 0;
        farNeighbourBlockades = new HashSet<BlockadeModel>();
        lastUpdateTime = 0;
        this.importantBlockades = new HashSet<BlockadeModel>();
        this.veryImportantBlockades = new HashSet<BlockadeModel>();
        passableEdgeModels = new ArrayList<EdgeModel>();
        this.buildingVisitableParts = new HashMap<EntityID, List<Polygon>>();
        for (EdgeModel edgeModel : edgeModels) {
            if (edgeModel.isPassable()) {
                passableEdgeModels.add(edgeModel);
            }
        }

        for (Path p : world.getPaths()) {
            if (p.contains(road)) {
                paths.add(p);
            }
        }
        setSeen(false);

        setEdgeModels(edgeModels);
        this.openEdges = new HashSet<EdgeModel>(edgeModels);
        resetReachableEdges();

        this.lineOfSightPerception = new LimitedLineOfSightPerception(world);
    }



    public void update() {
        reset();
        setApolloBlockades();
//        setBlockadesFromViewer();
        for (EdgeModel edgeModel : edgeModels) {
            if (edgeModel.isPassable()) {
                edgeModel.setOpenPart(edgeModel.getLine());
                List<BlockadeModel> blockedStart = new ArrayList<BlockadeModel>();
                List<BlockadeModel> blockedEnd = new ArrayList<BlockadeModel>();
                for (BlockadeModel blockadeModel : blockadeModels) {
//                    if (world.getAgentInfo().me() instanceof Human /*&& world.getCommonAgent().isHardWalking()*/) {
                        if (Util.distance(blockadeModel.getPolygon(), edgeModel.getStart()) < ApolloConstants.AGENT_MINIMUM_PASSING_THRESHOLD) {
                            blockedStart.add(blockadeModel);
                        }
                        if (Util.distance(blockadeModel.getPolygon(), edgeModel.getEnd()) < ApolloConstants.AGENT_MINIMUM_PASSING_THRESHOLD) {
                            blockedEnd.add(blockadeModel);
                        }
//                    } else {
//                        if (Util.distance(blockadeModel.getPolygon(), edgeModel.getStart()) < ApolloConstants.AGENT_PASSING_THRESHOLD) {
//                            blockedStart.add(blockadeModel);
//                        }
//                        if (Util.distance(blockadeModel.getPolygon(), edgeModel.getEnd()) < ApolloConstants.AGENT_PASSING_THRESHOLD) {
//                            blockedEnd.add(blockadeModel);
//                        }
//                    }
//                    setEdgeModelOpenPart(EdgeModel, mrlBlockade);
                }
                setEdgeModelOpenPart(edgeModel);
                if (blockadeModels.size() == 1) {
                    if (Util.containsEach(blockedEnd, blockedStart)) {
                        blockadeModels.get(0).addBlockedEdges(edgeModel);
                        edgeModel.setBlocked(true);
                        edgeModel.setAbsolutelyBlocked(true);
                    }
                } else {
                    for (BlockadeModel block1 : blockedStart) {
                        for (BlockadeModel block2 : blockedEnd) {
//                            double distance = Util.distance(block1.getPolygon(), block2.getPolygon());
//                            if (world.getAgentInfo().me() instanceof Human /*&& world.getCommonAgent().isHardWalking()*/) {
                                if (Util.isPassable(block1.getPolygon(), block2.getPolygon(), ApolloConstants.AGENT_MINIMUM_PASSING_THRESHOLD)) {
                                    edgeModel.setAbsolutelyBlocked(true);
                                    block1.addBlockedEdges(edgeModel);
                                    block2.addBlockedEdges(edgeModel);
                                }
//                            } else {
//                                if (Util.isPassable(block1.getPolygon(), block2.getPolygon(), ApolloConstants.AGENT_PASSING_THRESHOLD)) {
//                                    edgeModel.setBlocked(true);
//                                    block1.addBlockedEdges(edgeModel);
//                                    block2.addBlockedEdges(edgeModel);
//                                }
//                            }
                        }
                    }
                }
                if (edgeModel.isBlocked()) {
                    blockedEdges.add(edgeModel);
                }
                isPassable = getReachableEdges(edgeModel) != null && !getReachableEdges(edgeModel).isEmpty();
            } else {
                for (BlockadeModel blockadeModel : blockadeModels) {
                    double distance = Util.distance(edgeModel.getLine(), blockadeModel.getPolygon());
//                    if (world.getAgentInfo().me() instanceof Human /*&& world.getCommonAgent().isHardWalking()*/) {
                        if (distance < ApolloConstants.AGENT_MINIMUM_PASSING_THRESHOLD) {
                            edgeModel.setAbsolutelyBlocked(true);
                            blockadeModel.addBlockedEdges(edgeModel);
                        }
//                    } else {
//                        if (world.getAgentInfo().me() instanceof Human /*&& world.getCommonAgent().isHardWalking()*/ ? distance < ApolloConstants.AGENT_MINIMUM_PASSING_THRESHOLD : distance < ApolloConstants.AGENT_PASSING_THRESHOLD) {
//                            edgeModel.setBlocked(true);
//                            blockadeModel.addBlockedEdges(edgeModel);
//                        }
                    }
                }
            }
//            boolean isOtherSideBlocked = EdgeModel.isOtherSideBlocked(world);
//            if (isOtherSideBlocked) {
//                EdgeModel.setBlocked(true);
//            }
//        }

        //check too small edge passably
        checkTooSmallEdgesPassably();
        if (world.getAgentInfo().me() instanceof Human) {
            for (EdgeModel edgeModel : passableEdgeModels) {
                //for edges that not blocked each side separately but each side blocked other one.
                if (!edgeModel.isBlocked() && !edgeModel.isTooSmall()) {
                    if (Util.lineLength(edgeModel.getOpenPart()) < /* (world.getCommonAgent().isHardWalking() ? ApolloConstants.AGENT_MINIMUM_PASSING_THRESHOLD :*/
                            ApolloConstants.AGENT_PASSING_THRESHOLD) {
                        blockedEdges.add(edgeModel);
                        edgeModel.setBlocked(true);
                    }
                }
            }
        }

        updateRepairCost();
        openEdges.removeAll(blockedEdges);
        if (/*world.getAgentInfo().me() instanceof Human && world.getAgentInfo().me() instanceof PoliceForce*/
                world.getAgentInfo().me() instanceof PoliceForce ) {
//            updateBlockadesValue();
        } else {
            updateNodesPassably();
        }
        lastUpdateTime = world.getTime();
    }

    private void checkTooSmallEdgesPassably() {

        for (EdgeModel edgeModel : passableEdgeModels) {
            if (edgeModel.isTooSmall()) {
                Set<EntityID> neighbours = new HashSet<EntityID>(parent.getNeighbours());
                neighbours.addAll(world.getEntity(edgeModel.getNeighbours().second(), Area.class).getNeighbours());
                RoadModel roadModel;
                FOR1:
                for (EntityID neighbourID : neighbours) {
                    roadModel = world.getRoadModel(neighbourID);
                    if (roadModel != null) {
                        for (BlockadeModel blockadeModel : roadModel.getBlockadesModel()) {
                            if (Util.distance(blockadeModel.getPolygon(), edgeModel.getMiddle()) < ApolloConstants.AGENT_PASSING_THRESHOLD) {
                                blockedEdges.add(edgeModel);
                                edgeModel.setBlocked(true);
                                break FOR1;
                            }
                        }
                    }
                }
            }
        }
    }

    private Set<EdgeModel> getConnectedEdges(EdgeModel edgeModel) {
        RoadModel ownerRoad = world.getRoadModel(edgeModel.getNeighbours().first());
        RoadModel neighbourRoad = world.getRoadModel(edgeModel.getNeighbours().second());
        Set<EdgeModel> connectedEdges = new HashSet<EdgeModel>();
        for (EdgeModel edge : ownerRoad.getEdgeModels()) {
            if (edgeModel.getStart().equals(edge.getStart()) ||
                    edgeModel.getStart().equals(edge.getEnd()) ||
                    edgeModel.getEnd().equals(edge.getStart()) ||
                    edgeModel.getEnd().equals(edge.getEnd())) {
                connectedEdges.add(edge);
            }
        }

        if (neighbourRoad != null) {//if neighbour instance of building...
            for (EdgeModel edge : neighbourRoad.getEdgeModels()) {
                if (edgeModel.getStart().equals(edge.getStart()) ||
                        edgeModel.getStart().equals(edge.getEnd()) ||
                        edgeModel.getEnd().equals(edge.getStart()) ||
                        edgeModel.getEnd().equals(edge.getEnd())) {
                    connectedEdges.add(edge);
                }
            }
        }

        //todo add other neighbours connected edges..........
        return connectedEdges;
    }


    private void updateNodesPassably() {
        if (!(world.getAgentInfo().me() instanceof Human))
            return;
        GraphModule graphModule = world.getGraphModule();
        for (EdgeModel edgeModel : passableEdgeModels) {
            Node node = graphModule.getNode(edgeModel.getMiddle());
            if (node == null) {
                continue;
            }

            if (edgeModel.isBlocked() || edgeModel.isOtherSideBlocked(worldInfo)) {
                node.setPassable(false, world.getTime());
            } else {
                node.setPassable(true, world.getTime());
            }
        }
    }

    public void addBuildingVisitableParts(EntityID buildingID, Polygon visitablePartsPolygon) {
        if (!buildingVisitableParts.containsKey(buildingID)) {
            buildingVisitableParts.put(buildingID, new ArrayList<Polygon>());
        }
        buildingVisitableParts.get(buildingID).add(visitablePartsPolygon);
    }

    private void setEdgeModelOpenPart(EdgeModel edgeModel) {
        Point2D p1 = null, p2 = null;
        int d1 = 0, d2 = 0;
        for (BlockadeModel blockadeModel : blockadeModels) {
            List<Point2D> pointList = Util.getPoint2DList(blockadeModel.getPolygon().xpoints, blockadeModel.getPolygon().ypoints);
            List<Point2D> centerPoints = new ArrayList<Point2D>();
            boolean isBlockedStart = false, isBlockedEnd = false;
            for (Point2D point : pointList) {
                if (Util.contains(edgeModel.getLine(), point, 100)) {
                    if (Util.distance(point, edgeModel.getLine().getOrigin()) <= 10/*point.equals(EdgeModel.getLine().getOrigin())*/) {
                        isBlockedStart = true;
                    } else if (Util.distance(point, edgeModel.getLine().getEndPoint()) <= 10/*point.equals(EdgeModel.getLine().getEndPoint())*/) {
                        isBlockedEnd = true;
                    } else {
                        centerPoints.add(point);
                    }
                }
            }

            for (Point2D centerPoint : centerPoints) {
                if (isBlockedEnd && isBlockedStart) {
                    p1 = edgeModel.getMiddle();
                    p2 = edgeModel.getMiddle();
                    break;
                } else if (isBlockedEnd) {
                    int dist = Util.distance(centerPoint, edgeModel.getLine().getEndPoint());
                    if (dist > d2) {
                        p2 = centerPoint;
                        d2 = dist;
                    }
                } else if (isBlockedStart) {
                    int dist = Util.distance(centerPoint, edgeModel.getLine().getOrigin());
                    if (dist > d1) {
                        p1 = centerPoint;
                        d1 = dist;
                    }
                }
            }
        }
        if (p1 == null) {
            p1 = edgeModel.getStart();
        }
        if (p2 == null) {
            p2 = edgeModel.getEnd();
        }
        EdgeModel otherSide = edgeModel.getOtherSideEdge(worldInfo);
        Line2D openPart = new Line2D(p1, p2);
        if (otherSide != null) {
            RoadModel neighbour = world.getRoadModel(otherSide.getNeighbours().first());
            if (neighbour.getLastUpdateTime() >= this.lastUpdateTime) {
                Line2D otherSideOpenPart = otherSide.getOpenPart();
                if (Util.lineLength(openPart) < Util.lineLength(otherSideOpenPart)) {
                    edgeModel.setOpenPart(openPart);
                    otherSide.setOpenPart(openPart);
                } else {
                    edgeModel.setOpenPart(otherSideOpenPart);
                    otherSide.setOpenPart(otherSideOpenPart);
                }
            } else {
                edgeModel.setOpenPart(openPart);
            }
        } else {
            edgeModel.setOpenPart(openPart);
        }
    }

    public HashSet<EdgeModel> getReachableEdges(EdgeModel from) {
        return reachableEdges.get(from);
    }

    public void addReachableEdges(EdgeModel from, EdgeModel to) {
        reachableEdges.get(from).add(to);
        reachableEdges.get(to).add(from);
    }

    public void removeReachableEdges(EdgeModel from, EdgeModel to) {
        reachableEdges.get(from).remove(to);
        reachableEdges.get(to).remove(from);
    }

    private void setApexPoint() {
        apexPoints.clear();
        for (EdgeModel edgeModel : edgeModels) {
            if (edgeModel == null) {
//                System.out.println("(RoadModel.class ==> EdgeModel == null)"); XXX
                continue;
            }
            if (!apexPoints.contains(edgeModel.getStart()))
                apexPoints.add(edgeModel.getStart());
            else if (!apexPoints.contains(edgeModel.getEnd()))
                apexPoints.add(edgeModel.getEnd());
        }
        createPolygon();
        computeGroundArea();
    }

    private void computeGroundArea() {
        double area = GeometryTools2D.computeArea(apexPoints) * ApolloConstants.SQ_MM_TO_SQ_M;
        groundArea = (int) Math.abs(area);
    }

    public List<EdgeModel> getEdgeModelsTo(EntityID neighbourID) {
        List<EdgeModel> EdgeModelList = new ArrayList<EdgeModel>();
        for (EdgeModel edgeModel : edgeModels) {
            if (edgeModel.isPassable() && edgeModel.getNeighbours().second().equals(neighbourID)) {
                EdgeModelList.add(edgeModel);
            }
        }
        return EdgeModelList;
    }

    private void createPolygon() {
        int count = apexPoints.size();
        int xs[] = new int[count];
        int ys[] = new int[count];
        for (int i = 0; i < count; i++) {
            xs[i] = (int) apexPoints.get(i).getX();
            ys[i] = (int) apexPoints.get(i).getY();
        }
        polygon = new Polygon(xs, ys, count);
    }

//    /**
//     * ye polygon migirim bad khat be kahte polygon ro ba in road moghayese mikonim(baraye mohasebeye passably)
//     * EdgeModel har khat ro migirim va dakhele yek list mirizim
//     * hala ba in liste edge ha ye mrlroad misazim ba parente hamin road...
//     *
//     * @param polygon polygon on the road that we want to convert to another road(useful for split)
//     * @return new mrlRoad
//     */
//    private RoadModel convertToRoad(Polygon polygon) {
//        List<EdgeModel> EdgeModelList = new ArrayList<EdgeModel>();
//        for (int i = 0; i < polygon.npoints; i++) {
//            int j = (i + 1) % polygon.npoints;
//            Point2D p1 = new Point2D(polygon.xpoints[i], polygon.ypoints[i]);
//            Point2D p2 = new Point2D(polygon.xpoints[j], polygon.ypoints[j]);
//            Point2D m1 = Util.getMiddle(p1, p2);
//            EdgeModel EdgeModelTemp = getEdgeInPoint(m1);
//            EdgeModel edgeModel;
//            if (EdgeModelTemp != null)
//                edgeModel = new EdgeModel(EdgeModelTemp.isPassable(), p1, p2);
//            else
//                edgeModel = new EdgeModel(true, p1, p2);
//            EdgeModelList.add(edgeModel);
//        }
//        return new RoadModel(parent, world, EdgeModelList);
//    }

    private void resetReachableEdges() {
        HashSet<EdgeModel> edgesInstead;
        for (EdgeModel edgeModel : edgeModels) {
            if (edgeModel.isPassable()) {
                edgesInstead = new HashSet<EdgeModel>(passableEdgeModels);
                edgesInstead.remove(edgeModel);
                reachableEdges.put(edgeModel, edgesInstead);
            }
        }
    }

    /**
     * yek point migirim va beine edge haye roademoon iterate mikonim ta bebinim kodoom EdgeModel in noghte ro dare
     * age hich kodoom in noghte ro nadashtand null bar migardoonim
     *
     * @param point point that we want found edge on it
     * @return EdgeModel which point on it.
     */
    public EdgeModel getEdgeInPoint(Point2D point) {
        for (EdgeModel edgeModel : edgeModels) {
            if (Util.contains(edgeModel.getLine(), point, 1.0)) {
                return edgeModel;
            }
        }
        return null;
    }

    public EdgeModel getEdgeModel(Edge edge) {
        Point2D middle = Util.getPoint(EdgeInfoComponent.getEdgeMiddle(edge));
        return getEdgeInPoint(middle);
    }

    /**
     * aval polygone road ro migirim bad be vasileye tabe split mikonim nesbat be khat
     * hala 2ta polygon darim
     * har polygon ro tabdil be road mikonim va return mikonim
     *
     * @param   edges monas'sef
     * @return 2 mrl roade jadid ba parent_e hamin road
     */
//    public Pair<RoadModel, RoadModel> splitRoad(rescuecore2.misc.geometry.Line2D line2D) {
//        Pair<Polygon, Polygon> splitPolygon = PolygonUtil.split(parent.getApexList(), line2D);
//        RoadModel road1 = convertToRoad(splitPolygon.first());
//        RoadModel road2 = convertToRoad(splitPolygon.second());
//        childRoads.add(road1);
//        childRoads.add(road2);
//        return new Pair<RoadModel, RoadModel>(road1, road2);
//    }

    private void setEdgeModels(List<EdgeModel> edges) {
        edgeModels = new ArrayList<EdgeModel>();
        edgeModels.addAll(edges);
        setApexPoint();
    }

    private List<EdgeModel> createEdgeModels(List<Edge> edges) {
        List<EdgeModel> edgeModels = new ArrayList<EdgeModel>();
        for (Edge edge : edges) {
            edgeModels.add(new EdgeModel(world, edge, parent.getID()));
        }
        return edgeModels;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public int getGroundArea() {
        return groundArea;
    }

    private void addBlockade(BlockadeModel blockade) {
        blockadeModels.add(blockade);
    }

    private void setBlockadesFromViewer() {
//        System.out.println("REMOVE IT(From RoadModel.class)");
        if (!VIEWER_ROAD_BLOCKADES.containsKey(parent)) {
            return;
        }
        blockadeModels.clear();
        for (Blockade blockade : VIEWER_ROAD_BLOCKADES.get(parent)) {
            Polygon blockPolygon = PolygonUtil.retainPolygon(getPolygon(), Util.getPolygon(blockade.getApexes()));
            if (blockPolygon == null || blockPolygon.npoints < 3) {
                continue;
            }
            BlockadeModel newBlockade = new BlockadeModel(this, blockade, blockPolygon);
            addBlockade(newBlockade);
        }
    }

    private void setApolloBlockades() {
        totalRepairCost = 0;
        blockadeModels.clear();
        if (!parent.isBlockadesDefined()) {
            return;
        }
        try {
            StandardEntity entity;
            for (EntityID blockID : parent.getBlockades()) {
                entity = world.getEntity(blockID);
                if (!(entity instanceof Blockade)) {
                    if (ApolloConstants.LAUNCH_VIEWER) {
                        world.printData(entity + " is not blockade!!!!!");
                    }
                    continue;
                }
                Blockade blockade = (Blockade) entity;
                Polygon blockPolygon = PolygonUtil.retainPolygon(getPolygon(), Util.getPolygon(blockade.getApexes()));
                if (blockPolygon == null) {
                    continue;
                }
                BlockadeModel newBlockade = new BlockadeModel(this, blockade, blockPolygon);
                addBlockade(newBlockade);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            //TODO: handle it
//            System.err.println("null exception");
        }
    }

    private void updateRepairCost() {
        totalRepairCost = 0;
        for (BlockadeModel blockadeModel : blockadeModels) {
            totalRepairCost += blockadeModel.getRepairCost();
        }
        int repairRate = world.getScenarioInfo().getRawConfig().getIntValue("clear.repair.rate");
        repairTime = (int) Math.ceil(totalRepairCost / repairRate);
    }

    private void updateBlockadesValue() {
        if (this.getBlockadesModel().size() == 0) {
            return;
        }
        importantBlockades.clear();
        for (int e1 = 0; e1 < passableEdgeModels.size() - 1; e1++) {
            EdgeModel from = getEdgeModels().get(e1);
            for (int e2 = e1; e2 < passableEdgeModels.size(); e2++) {
                EdgeModel to = getEdgeModels().get(e2);
                updateBlockadesValue(from, to);
            }
        }

    }

    private void updateBlockadesValue(EdgeModel from, EdgeModel to) {
        Pair<List<EdgeModel>, List<EdgeModel>> edgesBetween = world.getComponent(RoadInfoComponent.class).getEdgesBetween(this, from, to, false);
        for (BlockadeModel blockade : this.getBlockadesModel()) {
            blockade.setValue(BlockadeValue.WORTHLESS);
            if (blockade.getBlockedEdges().contains(from) || blockade.getBlockedEdges().contains(to)) {
                blockade.setValue(BlockadeValue.VERY_IMPORTANT);
                importantBlockades.add(blockade);
                veryImportantBlockades.add(blockade);
                continue;
            }

            if (Util.containsEach(blockade.getBlockedEdges(), edgesBetween.first()) &&
                    Util.containsEach(blockade.getBlockedEdges(), edgesBetween.second())) {
                blockade.setValue(BlockadeValue.VERY_IMPORTANT);
                veryImportantBlockades.add(blockade);
                importantBlockades.add(blockade);
            }
        }

        for (int i = 0; i < this.getBlockadesModel().size() - 1; i++) {
            List<EdgeModel> blockedEdges = new ArrayList<EdgeModel>();
            BlockadeModel blockade1 = this.getBlockadesModel().get(i);
            if (blockade1.getValue().equals(BlockadeValue.VERY_IMPORTANT))
                continue;
            blockedEdges.addAll(blockade1.getBlockedEdges());
            for (int j = i + 1; j < this.getBlockadesModel().size(); j++) {
                BlockadeModel blockade2 = this.getBlockadesModel().get(j);
                if (blockade2.getValue().equals(BlockadeValue.VERY_IMPORTANT))
                    continue;
                blockedEdges.addAll(blockade2.getBlockedEdges());
//                if (Util.distance(blockade1.getPolygon(), blockade2.getPolygon()) < ApolloConstants.AGENT_PASSING_THRESHOLD) {
                if (Util.isPassable(blockade1.getPolygon(), blockade2.getPolygon(), ApolloConstants.AGENT_PASSING_THRESHOLD)) {
                    if (Util.containsEach(blockedEdges, edgesBetween.first()) &&
                            Util.containsEach(blockedEdges, edgesBetween.second())) {
                        if (blockade1.getRepairCost() > blockade2.getRepairCost()) {
                            importantBlockades.add(blockade2);
                            blockade1.setValue(BlockadeValue.IMPORTANT_WITH_HIGH_REPAIR_COST);
                            blockade2.setValue(BlockadeValue.IMPORTANT_WITH_LOW_REPAIR_COST);
                        } else {
                            importantBlockades.add(blockade1);
                            blockade1.setValue(BlockadeValue.IMPORTANT_WITH_LOW_REPAIR_COST);
                            blockade2.setValue(BlockadeValue.IMPORTANT_WITH_HIGH_REPAIR_COST);
                        }
                    }
                }
            }
        }

        rescuecore2.misc.geometry.Line2D myEdgeLine = new rescuecore2.misc.geometry.Line2D(from.getMiddle(), to.getMiddle());
        for (BlockadeModel blockade : this.getBlockadesModel()) {
            if (blockade.getValue().equals(BlockadeValue.WORTHLESS)) {
                if (Util.intersections(blockade.getPolygon(), myEdgeLine).size() > 0) {
                    blockade.setValue(BlockadeValue.ORNERY);
                }
            }
        }
    }

    public Set<BlockadeModel> getObstacles(EdgeModel from, EdgeModel to) {
        Set<BlockadeModel> obstacles = new HashSet<BlockadeModel>();
        Pair<List<EdgeModel>, List<EdgeModel>> edgesBetween = world.getComponent(RoadInfoComponent.class).getEdgesBetween(this, from, to, false);
        for (BlockadeModel blockade : this.getBlockadesModel()) {
            if (blockade.getBlockedEdges().contains(from) || blockade.getBlockedEdges().contains(to)) {
                obstacles.add(blockade);
                continue;
            }

            if (Util.containsEach(blockade.getBlockedEdges(), edgesBetween.first()) &&
                    Util.containsEach(blockade.getBlockedEdges(), edgesBetween.second())) {
                obstacles.add(blockade);
            }
        }

        for (int i = 0; i < this.getBlockadesModel().size() - 1; i++) {
            List<EdgeModel> blockedEdges = new ArrayList<EdgeModel>();
            BlockadeModel blockade1 = this.getBlockadesModel().get(i);
            blockedEdges.addAll(blockade1.getBlockedEdges());
            for (int j = i + 1; j < this.getBlockadesModel().size(); j++) {
                BlockadeModel blockade2 = this.getBlockadesModel().get(j);
                blockedEdges.addAll(blockade2.getBlockedEdges());
//                if (Util.distance(blockade1.getPolygon(), blockade2.getPolygon()) < ApolloConstants.AGENT_PASSING_THRESHOLD) {

                if (world.getAgentInfo().me() instanceof Human /*&& world.getCommonAgent().isHardWalking()*/ ?
                        Util.isPassable(blockade1.getPolygon(), blockade2.getPolygon(), ApolloConstants.AGENT_MINIMUM_PASSING_THRESHOLD) :
                        Util.isPassable(blockade1.getPolygon(), blockade2.getPolygon(), ApolloConstants.AGENT_PASSING_THRESHOLD)) {
                    if (Util.containsEach(blockedEdges, edgesBetween.first()) &&
                            Util.containsEach(blockedEdges, edgesBetween.second())) {
                        if (blockade1.getRepairCost() > blockade2.getRepairCost()) {
                            obstacles.add(blockade2);
                        } else {
                            obstacles.add(blockade1);
                        }
                    }
                }
            }
        }
        return obstacles;
    }

    /**
     * agar 1 road 1 modat zamane khassi 2bare dide nashod ya payami dar ertebat ba un naresid reset mishe
     * be in soorat ke tamame yalhaye dakhele un + node haye un passable mishand va
     * edge haye un azx halate block kharej mishand
     * <p/>
     * zamane reset shodan bayad az meghdare repairCost/repair_rate (meghdar zamini ke bara pak kardane road lazeme) +
     * yek meghdare threshold baraye etminan be dast miad
     */
    public void resetOldPassably() {
        if (!isSeen() || !(world.getAgentInfo().me() instanceof Human) ||
                world.getAgentInfo().me() instanceof PoliceForce ||  lastResetTime > lastUpdateTime) {
            return;
        }
        if (isTimeToReset()) {
            reset();
        }
    }

    private boolean isTimeToReset() {
        int resetTime = getRepairTime() + ApolloConstants.ROAD_PASSABLY_RESET_TIME;
        return lastResetTime <= lastUpdateTime + resetTime && world.getTime() - lastSeenTime > resetTime;
    }

    public void reset() {
        blockedEdges.clear();
        openEdges.addAll(edgeModels);
        blockadeModels.clear();
        isPassable = true;
        isReachable = true;
        farNeighbourBlockades.clear();
        if (!(world.getAgentInfo().me() instanceof Human)) {
            return;
        }
        GraphModule graphModule = world.getGraphModule();
        for (EdgeModel edgeModel : edgeModels) {
            edgeModel.setBlocked(false);
            edgeModel.setAbsolutelyBlocked(false);
            EdgeModel otherEdge = edgeModel.getOtherSideEdge(worldInfo);
            edgeModel.setOpenPart(edgeModel.getLine());
            if (otherEdge != null) {
                RoadModel roadModel = world.getRoadModel(edgeModel.getNeighbours().second());
                if (roadModel.getLastUpdateTime() < lastUpdateTime) {
                    //mrlRoad.update();
                    otherEdge.setOpenPart(otherEdge.getLine());
                }
            }
            Area neighbour = world.getEntity(edgeModel.getNeighbours().second(), Area.class);
            if (edgeModel.isPassable()) {
                Node node = graphModule.getNode((edgeModel.getMiddle()));
                if (node == null) {
                    System.out.println("node == null in " + this.getID());
                    continue;
                }
                if (neighbour instanceof Road) {
                    RoadModel roadModel = world.getRoadModel(neighbour.getID());
                    EdgeModel neighbourEdge = roadModel.getEdgeInPoint(edgeModel.getMiddle());
                    if (neighbourEdge != null && !neighbourEdge.isBlocked()) {
                        node.setPassable(true, world.getTime());
                    }
                } else {
                    node.setPassable(true, world.getTime());
                }
            }
        }
        resetReachableEdges();
        for (MyEdge myEdge : graphModule.getMyEdgesInArea(getID())) {
            myEdge.setPassable(true);
        }
        lastResetTime = world.getTime();
    }

    public List<BlockadeModel> getBlockadesModel() {
        return blockadeModels;
    }
    /**
     * modify
     *
     * @return
     */
  /*  public boolean isNeedUpdate() {
        if (world.getPlatoonAgent() == null || world.getSelf() instanceof CommonCentre) {
            return false;
        }

        if ((parent.isBlockadesDefined() && parent.getBlockades().size() != getBlockadesModel().size()) ||
                world.getPlatoonAgent().isHardWalking() ||
                lastSeenTime == 0 ||
                lastSeenTime < world.getTime() - 10) {
            return true;
        }
        Blockade blockade;
        for (BlockadeModel blockadeModel : getBlockadesModel()) {
            blockade = blockadeModel.getParent();
            if (blockade == null || !parent.getBlockades().contains(blockadeModel.getParent().getID()) || blockade.getRepairCost() != blockadeModel.getRepairCost()) {
                return true;
            }
        }
        return false;
    }*/
    public boolean isNeedUpdate() {
        if (!(world.getAgentInfo().me() instanceof Human) /*|| world.getSelf() instanceof CommonCentre */) {
            return false;
        }
        if(!parent.isBlockadesDefined()){
            return true;
        }
        if (world.getAgentInfo().me() instanceof PoliceForce ||
                (parent.getBlockades().size() != getBlockadesModel().size()) ||
                /* world.getCommonAgent().isHardWalking() || */
                lastSeenTime == 0 ||
                lastSeenTime < world.getTime() - 10) {
            return true;
        }
        Blockade blockade;
        for (BlockadeModel apolloBlockade : getBlockadesModel()) {
            blockade = apolloBlockade.getParent();
            if (blockade == null || !parent.getBlockades().contains(apolloBlockade.getParent().getID()) || blockade.getRepairCost() != apolloBlockade.getRepairCost()) {
                return true;
            }
        }
        return false;
    }
    public boolean isHighway() {
        return highway;
    }

    public void setHighway(boolean highway) {
        this.highway = highway;
    }

    /**
     * free way is a kind of road which had a very long passable edge (more than 95% )
     *
     * @return if this road is a freeway return true , otherwise return false
     */
    public boolean isFreeway() {
        return freeway;
    }

    public void setFreeway(boolean isFreeway) {
        freeway = isFreeway;
    }

    public List<Path> getPaths() {
        return paths;
    }

    public void addPath(Path path) {
        this.paths.add(path);
    }

    public List<EdgeModel> getEdgeModels() {
        return edgeModels;
    }

    public void addNeighboursBlockades() {
        RoadModel neighbour;
        for (EntityID nID : parent.getNeighbours()) {
            neighbour = world.getRoadModel(nID);
            if (neighbour != null) {
                for (BlockadeModel blockadeModel : neighbour.getBlockadesModel()) {
                    if (!farNeighbourBlockades.contains(blockadeModel)) {
                        if (Util.isPassable(blockadeModel.getPolygon(), this.getPolygon(), ApolloConstants.AGENT_SIZE)) {
                            addBlockade(blockadeModel);
                        } else {
                            farNeighbourBlockades.add(blockadeModel);
                        }
                    }
                }
            }
        }
    }

    public Road getParent() {
        return parent;
    }

    public int getLastUpdateTime() {
        return lastUpdateTime;
    }

    public int getLastResetTime() {
        return lastResetTime;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public List<RoadModel> getChildRoads() {
        return childRoads;
    }

    public boolean isPassable() {
        return isPassable;
    }

    public Set<EdgeModel> getBlockedEdges() {
        return blockedEdges;
    }

    public HashSet<EdgeModel> getOpenEdges() {
        return openEdges;
    }

    public EntityID getID() {
        return parent.getID();
    }

    public boolean isReachable() {
        return isReachable;
    }

    public void setReachable(boolean reachable) {
        isReachable = reachable;
    }

    public void setSeen(boolean seen) {
        this.isSeen = seen;
    }

    public boolean isSeen() {
        return isSeen;
    }

    public void setLastSeenTime(int lastSeenTime) {
        this.lastSeenTime = lastSeenTime;
    }

    public int getLastSeenTime() {
        return lastSeenTime;
    }

    public int getRepairTime() {
        return repairTime;
    }

    public Set<BlockadeModel> getImportantBlockades() {
        return importantBlockades;
    }

    public Set<BlockadeModel> getVeryImportantBlockades() {
        return veryImportantBlockades;
    }

    /**
     * @return transformed polygon just for viewer
     */
    public Polygon getTransformedPolygon() {
        return transformedPolygon;
    }

    /**
     * transform polygon for viewer
     *
     * @param t viewer screen transform
     */
    public void createTransformedPolygon(ScreenTransform t) {

        int count = apexPoints.size();
        int xs[] = new int[count];
        int ys[] = new int[count];
        int i = 0;
        for (Point2D point2D : apexPoints) {
            xs[i] = t.xToScreen(point2D.getX());
            ys[i] = t.yToScreen(point2D.getY());
            i++;
        }
        transformedPolygon = new Polygon(xs, ys, count);
    }

    public Set<EntityID> getVisibleFrom() {
        return visibleFrom;
    }

    public void setVisibleFrom(Set<EntityID> visibleFrom) {
        this.visibleFrom = visibleFrom;
    }

    public List<EntityID> getObservableAreas() {
        if(observableAreas == null || observableAreas.isEmpty()) {
            observableAreas = lineOfSightPerception.getVisibleAreas(getID());
        }
        return observableAreas;
    }

    public void setObservableAreas(List<EntityID> observableAreas) {
        this.observableAreas = observableAreas;
    }

    public List<RayModel> getLineOfSight() {
        return lineOfSight;
    }

    public void setLineOfSight(List<RayModel> lineOfSight) {
        this.lineOfSight = lineOfSight;
    }

    public List<BuildingModel> getBuildingsInExtinguishRange() {
        return buildingsInExtinguishRange;
    }

    public void setBuildingsInExtinguishRange(List<BuildingModel> buildingsInExtinguishRange) {
        this.buildingsInExtinguishRange = buildingsInExtinguishRange;
    }

}
