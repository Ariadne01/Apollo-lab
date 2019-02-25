package ApolloRescue.module.algorithm.clustering;

import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.StaticClustering;
import rescuecore2.misc.Pair;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import ApolloRescue.module.universal.Util;

import java.util.*;

public class ApolloKMeans extends StaticClustering {
    private static final String KEY_CLUSTER_SIZE = "sample.clustering.size";
    private static final String KEY_CLUSTER_CENTER = "sample.clustering.centers";
    private static final String KEY_CLUSTER_ENTITY = "sample.clustering.entities.";
    private static final String KEY_ASSIGN_AGENT = "sample.clustering.assign";

    private int repeatPrecompute;
    private int repeatPreparate;

    private Map<Integer, Set<EntityID>> clusterEntityIdsMap;
    private Map<Integer, Set<StandardEntity>> clusterEntitiesMap;
    private Collection<StandardEntity> entities;

    private List<StandardEntity> centerList;
    private List<EntityID> centerIDs;
    private Map<Integer, List<StandardEntity>> clusterEntitiesList;
    private List<List<EntityID>> clusterEntityIDsList;

    private int clusterSize;

    private boolean assignAgentsFlag;

    private Map<EntityID, Set<EntityID>> shortestPathGraph;

    public ApolloKMeans(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.repeatPrecompute = developData.getInteger("ApolloRescue.module.algorithm.clustering.ApolloKMeans.repeatPrecompute", 7);
        this.repeatPreparate = developData.getInteger("ApolloRescue.module.algorithm.clustering.ApolloKMeans.repeatPreparate", 30);
        this.clusterSize = developData.getInteger("ApolloRescue.module.algorithm.clustering.clusterSize", GetLessAgentNumber());
        this.assignAgentsFlag = developData.getBoolean("ApolloRescue.module.algorithm.clustering.ApolloKMeans.assignAgentsFlag", true);
        this.clusterEntityIDsList = new ArrayList<>();
        this.centerIDs = new ArrayList<>();
        this.clusterEntitiesList = new HashMap<>();
        this.centerList = new ArrayList<>();
        this.clusterEntitiesMap = new HashMap<>();
        this.clusterEntityIdsMap = new HashMap<>();
        this.entities = wi.getEntitiesOfType(
                StandardEntityURN.ROAD,
                StandardEntityURN.HYDRANT,
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.GAS_STATION,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.POLICE_OFFICE
        );
    }
    private int GetLessAgentNumber(){
        int least = 10;
        List<StandardEntity> fireBrigadeList = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));
        List<StandardEntity> policeForceList = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE));
        List<StandardEntity> ambulanceTeamList = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM));
        least = fireBrigadeList.size();
        if (least > policeForceList.size()){
            least = policeForceList.size();
        }
        if (least > ambulanceTeamList.size()){
            least = ambulanceTeamList.size();
        }
        return least;
    }
    @Override
    public Clustering updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if(this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.centerList.clear();
        this.clusterEntitiesList.clear();
        return this;
    }

    @Override
    public Clustering precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if(this.getCountPrecompute() >= 2) {
            return this;
        }
        this.calcStandard();
//        this.entities = null;
//        // write
//        precomputeData.setInteger(KEY_CLUSTER_SIZE, this.clusterSize);
//        precomputeData.setEntityIDList(KEY_CLUSTER_CENTER, this.centerIDs);
//        for(int i = 0; i < this.clusterSize; i++) {
//            precomputeData.setEntityIDList(KEY_CLUSTER_ENTITY + i, this.clusterEntityIDsList.get(i));
//        }
//        precomputeData.setBoolean(KEY_ASSIGN_AGENT, this.assignAgentsFlag);
        return this;
    }

    @Override
    public Clustering resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if(this.getCountResume() >= 2) {
            return this;
        }
        this.calcStandard();
//        this.entities = null;
//        // read
//        this.clusterSize = precomputeData.getInteger(KEY_CLUSTER_SIZE);
//        this.centerIDs = new ArrayList<>(precomputeData.getEntityIDList(KEY_CLUSTER_CENTER));
//        this.clusterEntityIDsList = new ArrayList<>(this.clusterSize);
//        for(int i = 0; i < this.clusterSize; i++) {
//            this.clusterEntityIDsList.add(i, precomputeData.getEntityIDList(KEY_CLUSTER_ENTITY + i));
//        }
//        this.assignAgentsFlag = precomputeData.getBoolean(KEY_ASSIGN_AGENT);
        return this;
    }

    @Override
    public Clustering preparate() {
        super.preparate();
        if(this.getCountPreparate() >= 2) {
            return this;
        }
        this.calcStandard();
       // this.entities = null;
        return this;
    }

    @Override
    public int getClusterNumber() {
        //The number of clusters
        return this.clusterSize;
    }

    @Override
    public int getClusterIndex(StandardEntity entity) {
        return this.getClusterIndex(entity.getID());
    }

    @Override
    public int getClusterIndex(EntityID entityID) {
        for (int i = 0; i < this.clusterSize; i++) {
            if (this.clusterEntityIdsMap.get(i).contains(entityID)) {
                return i;
            }
        }
        return -1;
    }


    @Override
    public Collection<StandardEntity> getClusterEntities(int index) {
        /*List<StandardEntity> result = this.clusterEntitiesList.get(index);
        if(result == null || result.isEmpty()) {
            List<EntityID> list = this.clusterEntityIDsList.get(index);
            result = new ArrayList<>(list.size());
            for(int i = 0; i < list.size(); i++) {
                result.add(i, this.worldInfo.getEntity(list.get(i)));
            }
            this.clusterEntitiesList.put(index, result);
        }
        return result;*/

        return this.clusterEntitiesMap.get(index);
    }

    @Override
    public Collection<EntityID> getClusterEntityIDs(int index) {
        return this.clusterEntityIDsList.get(index);
    }

    @Override
    public Clustering calc() {
        return this;
    }

    private void calcStandard() {
        List<StandardEntity> allAgents = new ArrayList<>(worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));

        int numberOfAgentsPerCluster = 5;
        this.clusterSize = allAgents.size() / numberOfAgentsPerCluster;

        Double[][] data = new Double[entities.size()][2];
        Map<Double[], StandardEntity> entityMap = new HashMap<Double[], StandardEntity>();
        Pair<Integer, Integer> location;
        int i = 0;
        for (StandardEntity entity : entities) {
            location = worldInfo.getLocation(entity.getID());
            data[i][0] = location.first().doubleValue();
            data[i][1] = location.second().doubleValue();
            entityMap.put(data[i], entity);
            i++;
        }

        int tryCount = 10;
        KMeansOptimizer kmeans = new KMeansOptimizer(data, clusterSize, tryCount);
        kmeans.calculateClusters();
        Double[][] centers = kmeans.getClusterCenters();

        ArrayList<Double[]>[] clusters = kmeans.getClusters();
        Set<EntityID> entityIdSet;
        Set<StandardEntity> entities;

        int index = 0;
        for (ArrayList<Double[]> cluster : clusters) {
            entities = new HashSet<>();
            entityIdSet = new HashSet<>();
            if (cluster.isEmpty()) {
                continue;
            }
            for (Double[] clusterData : cluster) {
                StandardEntity entity = entityMap.get(clusterData);
                entities.add(entity);
                entityIdSet.add(entity.getID());
            }
            this.clusterEntityIdsMap.put(index, entityIdSet);
            this.clusterEntitiesMap.put(index, entities);
            index++;
        }


        List<Pair<Integer, Integer>> centerList = new ArrayList<>();
        Map<Integer, Integer> clusterIndexMap = new HashMap<>();
//        if (clusterSize < allAgents.size()) {
        int clusterIndex = 0;
        for (int agentIndex = 0; agentIndex < allAgents.size(); agentIndex++) {
            if (clusterIndex == clusterSize) {
                clusterIndex = 0;
            }
            centerList.add(new Pair<>(centers[clusterIndex][0].intValue(), centers[clusterIndex][1].intValue()));
            clusterIndexMap.put(agentIndex, clusterIndex);
            clusterIndex++;
        }


//        }


        int n = allAgents.size();
        int m = centerList.size();
        double[][] costMatrix = new double[n][m];

        Pair<Integer, Integer> center;
        for (int k = 0; k < n; k++) {
            for (int j = 0; j < m; j++) {
//                center = new Pair<>(centers[j][0].intValue(), centers[j][1].intValue());
                costMatrix[k][j] = Util.distance(worldInfo.getLocation(allAgents.get(k)), centerList.get(j));
            }
        }

        int[] assignment = computeVectorAssignments(costMatrix);

        for (int assignmentIndex = 0; assignmentIndex < assignment.length; assignmentIndex++) {
            this.clusterEntitiesMap.get(clusterIndexMap.get(assignmentIndex)).add(allAgents.get(assignment[assignmentIndex]));
            this.clusterEntityIdsMap.get(clusterIndexMap.get(assignmentIndex)).add(allAgents.get(assignment[assignmentIndex]).getID());
        }


        /*if (MrlPersonalData.DEBUG_MODE) {
            System.out.println("[" + this.getClass().getSimpleName() + "] Cluster : " + this.clusterSize);
        }*/

//        for (int k = 0; k < allAgents.size(); k++) {
//            int clusterIndex = k % clusterSize;
//            this.clusterEntitiesMap.get(clusterIndex).add(allAgents.get(k));
//            this.clusterEntityIdsMap.get(clusterIndex).add(allAgents.get(k).getID());
//        }
    }

    public int[] computeVectorAssignments(double[][] costMatrix) {
        HungarianAssignment hungarianAssignment = new HungarianAssignment(costMatrix);
        int[] temp = hungarianAssignment.execute();
        int[] result = null;
        List<Integer> tempList = new ArrayList<Integer>();
        if (temp != null) {
            for (int i : temp) {
                if (i != -1) {
                    tempList.add(i);
                }
            }
            result = new int[tempList.size()];
            for (int i = 0; i < temp.length; i++) {
                if (temp[i] != -1) {
                    result[temp[i]] = i;
                }
            }
        }
        return result;
    }

    private void calcPathBased(int repeat) {
        this.initShortestPath(this.worldInfo);
        Random random = new Random();
        List<StandardEntity> entityList = new ArrayList<>(this.entities);
        this.centerList = new ArrayList<>(this.clusterSize);
        this.clusterEntitiesList = new HashMap<>(this.clusterSize);

        for (int index = 0; index < this.clusterSize; index++) {
            this.clusterEntitiesList.put(index, new ArrayList<>());
            this.centerList.add(index, entityList.get(0));
        }
        for (int index = 0; index < this.clusterSize; index++) {
            StandardEntity centerEntity;
            do {
                centerEntity = entityList.get(Math.abs(random.nextInt()) % entityList.size());
            } while (this.centerList.contains(centerEntity));
            this.centerList.set(index, centerEntity);
        }
        for (int i = 0; i < repeat; i++) {
            this.clusterEntitiesList.clear();
            for (int index = 0; index < this.clusterSize; index++) {
                this.clusterEntitiesList.put(index, new ArrayList<>());
            }
            for (StandardEntity entity : entityList) {
                StandardEntity tmp = this.getNearEntity(this.worldInfo, this.centerList, entity);
                this.clusterEntitiesList.get(this.centerList.indexOf(tmp)).add(entity);
            }
            for (int index = 0; index < this.clusterSize; index++) {
                int sumX = 0, sumY = 0;
                for (StandardEntity entity : this.clusterEntitiesList.get(index)) {
                    Pair<Integer, Integer> location = this.worldInfo.getLocation(entity);
                    sumX += location.first();
                    sumY += location.second();
                }
                int centerX = sumX / clusterEntitiesList.get(index).size();
                int centerY = sumY / clusterEntitiesList.get(index).size();

                //this.centerList.set(index, getNearEntity(this.worldInfo, this.clusterEntitiesList.get(index), centerX, centerY));
                StandardEntity center = this.getNearEntity(this.worldInfo, this.clusterEntitiesList.get(index), centerX, centerY);
                if (center instanceof Area) {
                    this.centerList.set(index, center);
                } else if (center instanceof Human) {
                    this.centerList.set(index, this.worldInfo.getEntity(((Human) center).getPosition()));
                } else if (center instanceof Blockade) {
                    this.centerList.set(index, this.worldInfo.getEntity(((Blockade) center).getPosition()));
                }
            }
            if  (scenarioInfo.isDebugMode()) { System.out.print("*"); }
        }

        if  (scenarioInfo.isDebugMode()) { System.out.println(); }

        this.clusterEntitiesList.clear();
        for (int index = 0; index < this.clusterSize; index++) {
            this.clusterEntitiesList.put(index, new ArrayList<>());
        }
        for (StandardEntity entity : entityList) {
            StandardEntity tmp = this.getNearEntity(this.worldInfo, this.centerList, entity);
            this.clusterEntitiesList.get(this.centerList.indexOf(tmp)).add(entity);
        }
        //this.clusterEntitiesList.sort(comparing(List::size, reverseOrder()));
        if (this.assignAgentsFlag) {
            List<StandardEntity> fireBrigadeList = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));
            List<StandardEntity> policeForceList = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE));
            List<StandardEntity> ambulanceTeamList = new ArrayList<>(this.worldInfo.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM));
            this.assignAgents(this.worldInfo, fireBrigadeList);
            this.assignAgents(this.worldInfo, policeForceList);
            this.assignAgents(this.worldInfo, ambulanceTeamList);
        }

        this.centerIDs = new ArrayList<>();
        for(int i = 0; i < this.centerList.size(); i++) {
            this.centerIDs.add(i, this.centerList.get(i).getID());
        }
        for (int index = 0; index < this.clusterSize; index++) {
            List<StandardEntity> entities = this.clusterEntitiesList.get(index);
            List<EntityID> list = new ArrayList<>(entities.size());
            for(int i = 0; i < entities.size(); i++) {
                list.add(i, entities.get(i).getID());
            }
            this.clusterEntityIDsList.add(index, list);
        }
    }

    private void assignAgents(WorldInfo world, List<StandardEntity> agentList) {
        int clusterIndex = 0;
        while (agentList.size() > 0) {
            StandardEntity center = this.centerList.get(clusterIndex);
            StandardEntity agent = this.getNearAgent(world, agentList, center);
            this.clusterEntitiesList.get(clusterIndex).add(agent);
            agentList.remove(agent);
            clusterIndex++;
            if (clusterIndex >= this.clusterSize) {
                clusterIndex = 0;
            }
        }
    }

    private StandardEntity getNearEntityByLine(WorldInfo world, List<StandardEntity> srcEntityList, StandardEntity targetEntity) {
        Pair<Integer, Integer> location = world.getLocation(targetEntity);
        return this.getNearEntityByLine(world, srcEntityList, location.first(), location.second());
    }

    private StandardEntity getNearEntityByLine(WorldInfo world, List<StandardEntity> srcEntityList, int targetX, int targetY) {
        StandardEntity result = null;
        for(StandardEntity entity : srcEntityList) {
            result = ((result != null) ? this.compareLineDistance(world, targetX, targetY, result, entity) : entity);
        }
        return result;
    }

    private StandardEntity getNearAgent(WorldInfo worldInfo, List<StandardEntity> srcAgentList, StandardEntity targetEntity) {
        StandardEntity result = null;
        for (StandardEntity agent : srcAgentList) {
            Human human = (Human)agent;
            if (result == null) {
                result = agent;
            }
            else {
                if (this.comparePathDistance(worldInfo, targetEntity, result, worldInfo.getPosition(human)).equals(worldInfo.getPosition(human))) {
                    result = agent;
                }
            }
        }
        return result;
    }

    private StandardEntity getNearEntity(WorldInfo worldInfo, List<StandardEntity> srcEntityList, int targetX, int targetY) {
        StandardEntity result = null;
        for (StandardEntity entity : srcEntityList) {
            result = (result != null) ? this.compareLineDistance(worldInfo, targetX, targetY, result, entity) : entity;
        }
        return result;
    }

    private Point2D getEdgePoint(Edge edge) {
        Point2D start = edge.getStart();
        Point2D end = edge.getEnd();
        return new Point2D(((start.getX() + end.getX()) / 2.0D), ((start.getY() + end.getY()) / 2.0D));
    }


    private double getDistance(double fromX, double fromY, double toX, double toY) {
        double dx = fromX - toX;
        double dy = fromY - toY;
        return Math.hypot(dx, dy);
    }

    private double getDistance(Pair<Integer, Integer> from, Point2D to) {
        return getDistance(from.first(), from.second(), to.getX(), to.getY());
    }

    private double getDistance(Pair<Integer, Integer> from, Edge to) {
        return getDistance(from, getEdgePoint(to));
    }

    private double getDistance(Point2D from, Point2D to) {
        return getDistance(from.getX(), from.getY(), to.getX(), to.getY());
    }

    private double getDistance(Edge from, Edge to) {
        return getDistance(getEdgePoint(from), getEdgePoint(to));
    }

    private StandardEntity compareLineDistance(WorldInfo worldInfo, int targetX, int targetY, StandardEntity first, StandardEntity second) {
        Pair<Integer, Integer> firstLocation = worldInfo.getLocation(first);
        Pair<Integer, Integer> secondLocation = worldInfo.getLocation(second);
        double firstDistance = getDistance(firstLocation.first(), firstLocation.second(), targetX, targetY);
        double secondDistance = getDistance(secondLocation.first(), secondLocation.second(), targetX, targetY);
        return (firstDistance < secondDistance ? first : second);
    }

    private StandardEntity getNearEntity(WorldInfo worldInfo, List<StandardEntity> srcEntityList, StandardEntity targetEntity) {
        StandardEntity result = null;
        for (StandardEntity entity : srcEntityList) {
            result = (result != null) ? this.comparePathDistance(worldInfo, targetEntity, result, entity) : entity;
        }
        return result;
    }

    private StandardEntity comparePathDistance(WorldInfo worldInfo, StandardEntity target, StandardEntity first, StandardEntity second) {
        double firstDistance = getPathDistance(worldInfo, shortestPath(target.getID(), first.getID()));
        double secondDistance = getPathDistance(worldInfo, shortestPath(target.getID(), second.getID()));
        return (firstDistance < secondDistance ? first : second);
    }

    private double getPathDistance(WorldInfo worldInfo, List<EntityID> path) {
        if (path == null) return Double.MAX_VALUE;
        if (path.size() <= 1) return 0.0D;

        double distance = 0.0D;
        int limit = path.size() - 1;

        Area area = (Area)worldInfo.getEntity(path.get(0));
        distance += getDistance(worldInfo.getLocation(area), area.getEdgeTo(path.get(1)));
        area = (Area)worldInfo.getEntity(path.get(limit));
        distance += getDistance(worldInfo.getLocation(area), area.getEdgeTo(path.get(limit - 1)));

        for(int i = 1; i < limit; i++) {
            area = (Area)worldInfo.getEntity(path.get(i));
            distance += getDistance(area.getEdgeTo(path.get(i - 1)), area.getEdgeTo(path.get(i + 1)));
        }
        return distance;
    }

    private void initShortestPath(WorldInfo worldInfo) {
        Map<EntityID, Set<EntityID>> neighbours = new LazyMap<EntityID, Set<EntityID>>() {
            @Override
            public Set<EntityID> createValue() {
                return new HashSet<>();
            }
        };
        for (Entity next : worldInfo) {
            if (next instanceof Area) {
                Collection<EntityID> areaNeighbours = ((Area) next).getNeighbours();
                neighbours.get(next.getID()).addAll(areaNeighbours);
            }
        }
        for (Map.Entry<EntityID, Set<EntityID>> graph : neighbours.entrySet()) {// fix graphModule
            for (EntityID entityID : graph.getValue()) {
                neighbours.get(entityID).add(graph.getKey());
            }
        }
        this.shortestPathGraph = neighbours;
    }

    private List<EntityID> shortestPath(EntityID start, EntityID... goals) {
        return shortestPath(start, Arrays.asList(goals));
    }

    private List<EntityID> shortestPath(EntityID start, Collection<EntityID> goals) {
        List<EntityID> open = new LinkedList<>();
        Map<EntityID, EntityID> ancestors = new HashMap<>();
        open.add(start);
        EntityID next;
        boolean found = false;
        ancestors.put(start, start);
        do {
            next = open.remove(0);
            if (isGoal(next, goals)) {
                found = true;
                break;
            }
            Collection<EntityID> neighbours = shortestPathGraph.get(next);
            if (neighbours.isEmpty()) continue;

            for (EntityID neighbour : neighbours) {
                if (isGoal(neighbour, goals)) {
                    ancestors.put(neighbour, next);
                    next = neighbour;
                    found = true;
                    break;
                }
                else if (!ancestors.containsKey(neighbour)) {
                    open.add(neighbour);
                    ancestors.put(neighbour, next);
                }
            }
        } while (!found && !open.isEmpty());
        if (!found) {
            // No path
            return null;
        }
        // Walk back from goal to start
        EntityID current = next;
        List<EntityID> path = new LinkedList<>();
        do {
            path.add(0, current);
            current = ancestors.get(current);
            if (current == null) throw new RuntimeException("Found a node with no ancestor! Something is broken.");
        } while (current != start);
        return path;
    }

    private boolean isGoal(EntityID e, Collection<EntityID> test) {
        return test.contains(e);
    }
}

