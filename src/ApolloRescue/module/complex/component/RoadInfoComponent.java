package ApolloRescue.module.complex.component;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.entities.*;
import ApolloRescue.module.universal.newsearch.graph.GraphModule;
import ApolloRescue.module.universal.newsearch.graph.MyEdge;

import ApolloRescue.module.universal.newsearch.graph.Node;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
//import maps.convert.legacy2gml.RoadInfo; //ljy：RoadInfo 里导
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

public class RoadInfoComponent implements IComponent {
    protected ScenarioInfo scenarioInfo;
    protected AgentInfo agentInfo;
    protected WorldInfo worldInfo;
    protected ModuleManager moduleManager;
    protected DevelopData developData;
    protected ApolloWorld world;
//    protected Map<EntityID, RoadInfo> roadInfoMap ;
    private Map<EntityID, EntityID> pathIdMap;

    public RoadInfoComponent(ApolloWorld world,AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        this.world = world;
        this.worldInfo = wi;
        this.agentInfo = ai;
        this.scenarioInfo = si;
        this.moduleManager = moduleManager;
        this.developData = developData;
//		roadInfoMap = new FastMap<EntityID, RoadInfo>();
        pathIdMap = new HashMap<>();
    }

    public EntityID getPathId(EntityID id) {
//        return roadInfoMap.get(id).getPathId();
        return pathIdMap.get(id);
    }

    public void setPathId(EntityID roadId, EntityID pathId) {
//        roadInfoMap.get(roadId).setPathId(pathId);
        pathIdMap.put(roadId, pathId);
    }

    @Override
    public void init() {
  //      initRoadInfoMap();
    }

    private int lastUpdateTime = -1;

    public void update() {//ljy: need to modify
        if (agentInfo.me() instanceof PoliceForce) {

        } else if (lastUpdateTime < agentInfo.getTime()) {
            lastUpdateTime = agentInfo.getTime();
            Set<Road> roadsSeen = world.getRoadsSeen();


            Collection<StandardEntity> roads = world.getRoads();
            for (StandardEntity entity : roads) {
                RoadModel roadModel = world.getRoadModel(entity.getID());
                if (roadsSeen.contains(entity)) {
                    updatePassably(roadModel);
                } else {
                    roadModel.resetOldPassably();
                }
            }

        }

        //       MrlPersonalData.VIEWER_DATA.setGraphEdges(agentInfo.getID(), getGraphModule());
    }

//    private void initRoadInfoMap() {
//        for (StandardEntity standardEntity : world.getRoads()) {
//            Road road = (Road) standardEntity;
//            roadInfoMap.put(road.getID(), new RoadInfo(getNeighbours(road), road.getEdges()));
//        }
//    }

    /**
     * in method hameye building-haye mottasel be ye entrance ro peida mikone.
     *
     * @param entrance : target entrance.
     * @return : list of buildings of this entrance.
     */
    public List<Building> getBuildingsOfThisEntrance(EntityID entrance) {
        int loop = 0;
        List<Building> buildings = new ArrayList<Building>();
        List<Area> neighbours = new ArrayList<Area>();
        Area tempArea;
        Area neighbour;
        neighbours.add((Area) world.getEntity(entrance));

        while (!neighbours.isEmpty() && loop < 20) {
            loop++;
            tempArea = neighbours.get(0);
            neighbours.remove(0);

            for (EntityID entityID : tempArea.getNeighbours()) {
                neighbour = (Area) world.getEntity(entityID);
                if (neighbour instanceof Building) {
                    if (!buildings.contains((Building) neighbour)) {
                        buildings.add((Building) neighbour);
                        neighbours.add(neighbour);
                    }
                }
            }
        }
        return buildings;
    }

//    public void setRoadPassable(EntityID id, Boolean passable) {
//        roadInfoMap.get(id).setPassable(passable);
//    }

//    public void setIsolated(EntityID id, Boolean isolated) {
//        roadInfoMap.get(id).setIsolated(isolated);
//    }

//    public boolean canSendMessage(EntityID id, int time) {
//        RoadInfo roadInfo = roadInfoMap.get(id);
//        if (roadInfo.getLastMessageTime() + 5 < time) {
//            roadInfo.setLastMessageTime(time);
//            return true;
//        } else if (roadInfo.getLastMessageTime() + 1 == time) {
//            return true;
//        }
//        return false;
//    }

    //Mahdi Taherian=========>>
    public void updatePassably(RoadModel roadModel) {
//        if ( !(world.getAgentInfo().me() instanceof Human) || world.getAgentInfo().me() instanceof PoliceForce) {
//            return;
//        }

        for (int i = 0; i < roadModel.getEdgeModels().size() - 1; i++) {
            EdgeModel edge1 = roadModel.getEdgeModels().get(i);
            if (!edge1.isPassable()) {
                continue;
            }
            for (int j = i + 1; j < roadModel.getEdgeModels().size(); j++) {
                EdgeModel edge2 = roadModel.getEdgeModels().get(j);
                if (!edge2.isPassable()) {
                    continue;
                }
                setMyEdgePassably(roadModel, edge1, edge2, isPassable(roadModel, edge1, edge2, false));
            }
        }
    }

    /**
     * agar 1 area hich edgi ke be edge digar rah dashte bashad nadashte bashad passable nist
     *
     * @param roadId
     * @return
     */
    public boolean isPassable(EntityID roadId) {
        RoadModel roadModel = world.getRoadModel(roadId);
        return roadModel.isPassable();
    }

    /**
     * @param roadModel
     * @param from
     * @param to
     * @return
     */
    public boolean isPassable(RoadModel roadModel, EdgeModel from, EdgeModel to, boolean hardWalk) {

        if (!from.getNeighbours().first().equals(to.getNeighbours().first())) {
//            throw new IncorrectInputException("this 2 edge is not in a same area!!!");
//            System.err.println("this 2 edge is not in a same area!!!");  XXX
            return false;
        }
        if (hardWalk ? from.isAbsolutelyBlocked() || to.isAbsolutelyBlocked() : from.isBlocked() || to.isBlocked())
            return false;
        Pair<List<EdgeModel>, List<EdgeModel>> edgesBetween = getEdgesBetween(roadModel, from, to, false);

        int count = roadModel.getBlockadesModel().size();
        List<EdgeModel> blockedEdges = new ArrayList<EdgeModel>();
        if (count == 1) {
            blockedEdges.addAll(roadModel.getBlockadesModel().get(0).getBlockedEdges());
        } else if (count > 1) {
            for (int i = 0; i < count - 1; i++) {
                BlockadeModel block1 = roadModel.getBlockadesModel().get(i);
                for (int j = i + 1; j < count; j++) {
                    BlockadeModel block2 = roadModel.getBlockadesModel().get(j);
                    if (isBlockedTwoSides(block1, edgesBetween)) {
                        return false;
                    }
                    if (isBlockedTwoSides(block2, edgesBetween)) {
                        return false;
                    }
                    if (isInSameSide(block1, block2, edgesBetween)) {
                        continue;
                    }
//                    double distance = Util.distance(block1.getPolygon(), block2.getPolygon());

                    if (Util.isPassable(block1.getPolygon(), block2.getPolygon(), hardWalk ? ApolloConstants.AGENT_MINIMUM_PASSING_THRESHOLD : ApolloConstants.AGENT_PASSING_THRESHOLD)) {
                        blockedEdges.removeAll(block1.getBlockedEdges());
                        blockedEdges.addAll(block1.getBlockedEdges());
                        blockedEdges.removeAll(block2.getBlockedEdges());
                        blockedEdges.addAll(block2.getBlockedEdges());
                    }
                }
            }
        } else if (count == 0) {
            return !(from.isBlocked() || to.isBlocked());
        }
        return !(Util.containsEach(blockedEdges, edgesBetween.first()) && Util.containsEach(blockedEdges, edgesBetween.second()));
    }


    /**
     * @param from
     * @param to
     * @return
     */
    public boolean isPassable(Area from, Area to) {
        if (!from.getNeighbours().contains(to.getID())) {
//            System.err.println("");  XXX
            return false;
        }
        if (to instanceof Road) {
            RoadModel road = world.getRoadModel(to.getID());
            for (EdgeModel edgeModel : road.getOpenEdges()) {
                if (edgeModel.getNeighbours().second().equals(from.getID())) {
                    return true;
                }
            }
        } else {
            if (from instanceof Road) {
                RoadModel road = world.getRoadModel(from.getID());
                for (EdgeModel edgeModel : road.getOpenEdges()) {
                    if (edgeModel.getNeighbours().second().equals(to.getID())) {
                        return true;
                    }
                }
            }
        }
        return true;
    }

    public Boolean isSeen(EntityID id) {
        return world.getRoadModel(id).isSeen();
    }

//    public List<EntityID> getNeighbours(EntityID id) {
//        return roadInfoMap.get(id).getNeighbours();
//    }

    public List<EntityID> getNeighbours(Road road) {
        List<EntityID> neighbours = new ArrayList<EntityID>();

        for (Edge next : road.getEdges()) {
            if (next.isPassable()) {
                neighbours.add(next.getNeighbour());
            }
        }

        return neighbours;
    }




    public boolean isSeenAndBlocked(EntityID buildingID, EntityID roadEntrance) {
        RoadModel roadModel = world.getRoadModel(roadEntrance);
        if (!roadModel.isSeen())
            return false;

        for (EdgeModel edgeModel : roadModel.getEdgeModelsTo(buildingID)) {
            if (roadModel.getReachableEdges(edgeModel) == null || roadModel.getReachableEdges(edgeModel).size() > 0) {
                return true;
            }
        }
        return false;
    }





    private boolean isInSameSide(BlockadeModel block1, BlockadeModel block2, Pair<List<EdgeModel>, List<EdgeModel>> edgesBetween) {
        return edgesBetween.first().containsAll(block1.getBlockedEdges()) &&
                edgesBetween.first().containsAll(block2.getBlockedEdges()) ||
                edgesBetween.second().containsAll(block1.getBlockedEdges()) &&
                        edgesBetween.second().containsAll(block2.getBlockedEdges());

    }

    private boolean isBlockedTwoSides(BlockadeModel block1, Pair<List<EdgeModel>, List<EdgeModel>> edgesBetween) {
        return Util.containsEach(edgesBetween.first(), block1.getBlockedEdges()) &&
                Util.containsEach(edgesBetween.second(), block1.getBlockedEdges());
    }

    private void setMyEdgePassably(RoadModel road, EdgeModel edge1, EdgeModel edge2, boolean passably) {
        if (!(agentInfo.me() instanceof Human) || !edge1.getNeighbours().first().equals(edge2.getNeighbours().first())) {
            return;
        }

        GraphModule graphModule = world.getGraphModule();
        Node node1 = graphModule.getNode(edge1.getMiddle());
        Node node2 = graphModule.getNode(edge2.getMiddle());
        MyEdge myEdge = graphModule.getMyEdge(road.getParent().getID(), new Pair<Node, Node>(node1, node2));
        if (passably) {
            road.addReachableEdges(edge1, edge2);
//            road.addReachableEdges(edge2, edge1);
        } else {
            road.removeReachableEdges(edge1, edge2);
//            road.removeReachableEdges(edge2, edge1);
        }
        myEdge.setPassable(passably);
    }

    /**
     * 1road va 2ta edge ke dakhele un hastand ro migire va
     * tamame edgehaei ke bein un 2ta edge va dakhele un road hastand ro be shekle 1 Pair bar migardoone.
     *
     * @param road           roade morede barresi
     * @param edge1          edge avali
     * @param edge2          edge 2vomi
     * @param justImPassable in parameter bara ine ke faghat edgehaye impassable ezafe she ya na.
     * @return 1pair az edge haye ye samte un 2edge va edge haye samte digeshoon
     */
    public Pair<List<EdgeModel>, List<EdgeModel>> getEdgesBetween(RoadModel road, EdgeModel edge1, EdgeModel edge2, boolean justImPassable) {
        List<EdgeModel> leftSideEdges = new ArrayList<EdgeModel>();
        List<EdgeModel> rightSideEdges = new ArrayList<EdgeModel>();
        rescuecore2.misc.geometry.Point2D startPoint1 = edge1.getStart();
        rescuecore2.misc.geometry.Point2D endPoint1 = edge1.getEnd();
        rescuecore2.misc.geometry.Point2D startPoint2 = edge2.getStart();
        rescuecore2.misc.geometry.Point2D endPoint2 = edge2.getEnd();

        boolean finishedLeft = false;
        boolean finishedRight = false;
        for (EdgeModel edge : road.getEdgeModels()) {
            if (finishedLeft && finishedRight)
                break;
            for (EdgeModel ed : road.getEdgeModels()) {
                if (finishedLeft && finishedRight)
                    break;
                if (ed.equals(edge1) || ed.equals(edge2)) {
                    continue;
                }
                if (startPoint1.equals(startPoint2) || startPoint1.equals(endPoint2)) {
                    finishedLeft = true;
                }
                if (endPoint1.equals(startPoint2) || endPoint1.equals(endPoint2)) {
                    finishedRight = true;
                }

                if (ed.getStart().equals(startPoint1) && !finishedLeft && !leftSideEdges.contains(ed)) {
                    startPoint1 = ed.getEnd();
                    if (!justImPassable || !ed.isPassable())
                        leftSideEdges.add(ed);
                    continue;
                }
                if (ed.getEnd().equals(startPoint1) && !finishedLeft && !leftSideEdges.contains(ed)) {
                    startPoint1 = ed.getStart();
                    if (!justImPassable || !ed.isPassable())
                        leftSideEdges.add(ed);
                    continue;
                }
                if (ed.getStart().equals(endPoint1) && !finishedRight && !rightSideEdges.contains(ed)) {
                    endPoint1 = ed.getEnd();
                    if (!justImPassable || !ed.isPassable())
                        rightSideEdges.add(ed);
                    continue;
                }
                if (ed.getEnd().equals(endPoint1) && !finishedRight && !rightSideEdges.contains(ed)) {
                    endPoint1 = ed.getStart();
                    if (!justImPassable || !ed.isPassable())
                        rightSideEdges.add(ed);
                    continue;
                }
            }
        }
        return new Pair<List<EdgeModel>, List<EdgeModel>>(leftSideEdges, rightSideEdges);
    }

    public Set<EntityID> getReachableNeighbours(Area area) {
        Set<EntityID> reachableNeighbours = new HashSet<EntityID>();
        if (area == null)
            return reachableNeighbours;
        if (area instanceof Road) {
            RoadModel roadModel = world.getRoadModel(area.getID());
            if (roadModel.getOpenEdges().isEmpty()) {
                return reachableNeighbours;
            }
        }
        for (EntityID neighbourID : area.getNeighbours()) {
            Area neighbour = world.getEntity(neighbourID, Area.class);
            if (neighbour instanceof Road) {
                RoadModel road = world.getRoadModel(neighbour.getID());
                for (EdgeModel edgeModel : road.getEdgeModelsTo(area.getID())) {
                    if (!edgeModel.isBlocked()) {
                        reachableNeighbours.add(neighbourID);
                        break;
                    }
                }
            } else {
                if (area instanceof Road) {
                    RoadModel road = world.getRoadModel(area.getID());
                    for (EdgeModel edgeModel : road.getEdgeModelsTo(neighbourID)) {
                        if (!edgeModel.isBlocked()) {
                            reachableNeighbours.add(neighbourID);
                            break;
                        }
                    }
                } else {
                    reachableNeighbours.add(neighbourID);
                }
            }
        }
        return reachableNeighbours;
    }
//

    /**
     * in method 1 list Edge haye beyne 2ta AREA ro bar migardoone
     *
     * @param area1 area_e avali
     * @param area2 areaei ke mikhaeim edge beine un va areae avali(area1) ro hesab konim
     * @return listi az edge haye beine in 2 (in edge ha baraye area1 hastand yani neighbour_shoon area2 hast)...
     */
    public static Set<Edge> getEdgesBetween(Area area1, Area area2) {
        Set<Edge> edgesBetween = new HashSet<Edge>();
        if (!area1.getNeighbours().contains(area2.getID()))
            return edgesBetween;
        for (Edge edge : area1.getEdges()) {
            if (edge.isPassable() && edge.getNeighbour().equals(area2.getID())) {
                edgesBetween.add(edge);
            }
        }
        return edgesBetween;
    }

    public static int totalRepairCost(WorldInfo worldInfo, Area area) {
        int total = 0;
        if (area.isBlockadesDefined()) {
            for (EntityID blockId : area.getBlockades()) {
                Blockade blockade = (Blockade) worldInfo.getEntity(blockId);
                if (blockade != null) {
                    total += blockade.getRepairCost();
                }
            }
        } else {
            total = -1;
        }
        return total;
    }


    public boolean isOpenOrNotSeen(EntityID buildingID, EntityID roadEntrance) {
        RoadModel roadModel = world.getRoadModel(roadEntrance);
        BuildingModel buildingModel = world.getBuildingModel(buildingID);
        if (buildingModel == null || roadModel == null) {
            world.printData(buildingID + " is not an Building or " + roadEntrance + " is not a Road Entrance...");
            return false;
        }
        if (!roadModel.isSeen())
            return true;

        HashSet<EdgeModel> edgeModels;
        List<EdgeModel> toRemove = new ArrayList<EdgeModel>();
        for (EdgeModel edgeModel : roadModel.getEdgeModelsTo(buildingID)) {
            edgeModels = new HashSet<EdgeModel>(roadModel.getReachableEdges(edgeModel));
            edgeModels.removeAll(roadModel.getEdgeModelsTo(buildingID));
            for (Entrance entrance : buildingModel.getEntrances()) {
                for (EdgeModel neighbourEdge : edgeModels) {
                    if (neighbourEdge.getNeighbours().second().equals(entrance.getNeighbour().getID())
                            || neighbourEdge.isOtherSideBlocked(worldInfo)) {
                        toRemove.add(neighbourEdge);
                    }
                }
            }
            edgeModels.removeAll(toRemove);
            if (!edgeModel.isBlocked() && edgeModels.size() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * get neighbours of type Building
     *
     * @param world is disaster space
     * @param road  to find connected buildings
     * @return neighbour buildings
     */
    public static List<Building> getConnectedBuildings(ApolloWorld world, Road road) {
        List<Building> buildings = new ArrayList<Building>();
        StandardEntity standardEntity;

        for (EntityID id : road.getNeighbours()) {
            standardEntity = world.getEntity(id);
            if ((standardEntity instanceof Building) && !buildings.contains((Building) standardEntity)) {
                buildings.add((Building) standardEntity);
            }
        }
        return buildings;
    }

    public ArrayList<Road> getConnectedRoads(EntityID id) {
        ArrayList<Road> neighbours = new ArrayList<Road>();

        Area area = (Area) worldInfo.getEntity(id);

        for (EntityID entityID : area.getNeighbours()) {
            StandardEntity standardEntity = worldInfo.getEntity(entityID);
            if (standardEntity instanceof Road) {
                neighbours.add((Road) standardEntity);
            }
        }

        return neighbours;
    }


//    public void updateBlockadesValue(MrlRoad road , EdgeModel from , EdgeModel to){
//        if(!road.isBlockadesDefined()){
//            return;
//        }
//
//        Pair<List<EdgeModel>,List<EdgeModel>> edgesBetween = getEdgesBetween(road,from,to,false);
//        for(MrlBlockade blockade : road.getMrlBlockades()){
//            if(blockade.getBlockedEdges().contains(from) || blockade.getBlockedEdges().contains(to)){
//                blockade.setValue(BlockadeValue.VERY_IMPORTANT);
//                continue;
//            }
//
//            if(Util.containsEach(blockade.getBlockedEdges(),edgesBetween.first()) &&
//                    Util.containsEach(blockade.getBlockedEdges(),edgesBetween.second())){
//                blockade.setValue(BlockadeValue.VERY_IMPORTANT);
//            }
//        }
//
//        for(int i=0 ; i<road.getMrlBlockades().size()-1 ; i++){
//            List<EdgeModel> blockedEdges = new ArrayList<EdgeModel>();
//            MrlBlockade blockade1 = road.getMrlBlockades().get(i);
//            if(blockade1.getValue().equals(BlockadeValue.VERY_IMPORTANT))
//                continue;
//            blockedEdges.addAll(blockade1.getBlockedEdges());
//            for (int j=i+1;j<road.getMrlBlockades().size();j++){
//                MrlBlockade blockade2 = road.getMrlBlockades().get(j);
//                if(blockade2.getValue().equals(BlockadeValue.VERY_IMPORTANT))
//                    continue;
//                blockedEdges.addAll(blockade2.getBlockedEdges());
//                if(Util.distance(blockade1.getPolygon(),blockade2.getPolygon())<MRLConstants.AGENT_PASSING_THRESHOLD){
//                    if(Util.containsEach(blockedEdges,edgesBetween.first()) &&
//                            Util.containsEach(blockedEdges,edgesBetween.second())){
//                        blockade1.setValue(BlockadeValue.IMPORTANT);
//                        blockade2.setValue(BlockadeValue.IMPORTANT);
//                    }
//                }
//            }
//        }
//
//        rescuecore2.misc.geometry.Line2D myEdgeLine = new rescuecore2.misc.geometry.Line2D(from.getMiddle(),to.getMiddle());
//        for(MrlBlockade blockade : road.getMrlBlockades()){
//            if(blockade.getValue().equals(BlockadeValue.WORTHLESS)){
//                if(Util.intersections(blockade.getPolygon(),myEdgeLine).size()>0){
//                    blockade.setValue(BlockadeValue.ORNERY);
//                }
//            }
//        }
//
//
//    }


}
