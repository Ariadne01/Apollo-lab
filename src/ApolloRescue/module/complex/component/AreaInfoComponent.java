package ApolloRescue.module.complex.component;

import ApolloRescue.extaction.clear.ClearHelper;
import ApolloRescue.module.complex.component.Info.AreaInfo;
import ApolloRescue.module.complex.component.Info.EdgeInfo;
import ApolloRescue.module.complex.component.Info.NodeInfo;
import ApolloRescue.module.complex.component.Info.PathInfo;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.newsearch.node.Node;
import ApolloRescue.module.universal.newsearch.node.NodeFactory;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

public class AreaInfoComponent implements IComponent {
    protected ApolloWorld world;
    private Map<EntityID, NodeInfo> areaInfoMap;
    protected List<Node> nodes;
    protected Map<EntityID, Node> areaNode;
    private double areaAverageWidth;
    protected List<PathInfo> pathInfoList = new ArrayList<PathInfo>();
    protected ClearHelper newClearHelper;

    private WorldInfo worldInfo;
    private AgentInfo agentInfo;
    private ScenarioInfo scenarioInfo;


    public AreaInfoComponent(ApolloWorld world, WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo ) {
        this.world = world;
        this.worldInfo = worldInfo;
        this.agentInfo = agentInfo;
        this.scenarioInfo = scenarioInfo;

        areaInfoMap = new HashMap<EntityID, NodeInfo>();
        nodes = new ArrayList<Node>();
        areaNode = new HashMap<EntityID, Node>();
        newClearHelper = new ClearHelper(world, worldInfo, agentInfo, scenarioInfo);
    }

    @Override
    public void init() {
        for (StandardEntity entity : world.getAreas()) {

            setEdgesInfo((Area) entity);
        }
        initializeNodes();
    }

    @Override
    public void update() {
        // TODO Auto-generated method stub
    }

    // Area Info
    public NodeInfo getNodeInfo(EntityID id) {
        NodeInfo info;
        if (areaInfoMap.containsKey(id)) {

            info = areaInfoMap.get(id); //
            return info;
        } else {
            Area area;
            area = world.getEntity(id, Area.class);
            if (area != null) {
                info = new NodeInfo(area);
                areaInfoMap.put(id, info);
                return info;
            }
        }
        return null;
    }

    // Nodes
    public void initializeNodes() { // Nodes

        NodeFactory factory;
        factory = new NodeFactory(world);
        factory.createNodes(); // createNodes
        this.nodes = factory.getNodes();
        this.areaNode = factory.getAreaNode();
        this.areaAverageWidth = factory.getAverageWidth();
    }

    // areaNode
    public Map<EntityID, Node> getAreaNode() {
        return this.areaNode;
    }

    // Nodes
    public List<Node> getNodes() {
        return this.nodes;
    }

    public double getAreaAverageWidth() {
        return areaAverageWidth;
    }

    public Node getNode(EntityID areaID) {
        if (areaNode.containsKey(areaID)) {
            return areaNode.get(areaID);
        }
        return null;
    }



    private void setEdgesInfo(Area area) {
        int length;
        Edge longestEdge = null;
        int longer = Integer.MIN_VALUE;
        Edge smallestEdge = null;
        int smaller = Integer.MAX_VALUE;
        int sumOfLengths = 0;

        AreaInfo areaInfo = new AreaInfo();

        for (Edge edge : area.getEdges()) {
            EdgeInfo edgeInfo = new EdgeInfo();

            length = Util.distance(edge.getStartX(), edge.getStartY(),
                    edge.getEndX(), edge.getEndY());
            edgeInfo.setLength(length);
            if (world.getEntity(edge.getNeighbour()) instanceof Building) {
                edgeInfo.setOnEntrance(true);
            }
            edgeInfo.setMiddle(EdgeInfoComponent.getEdgeMiddle(edge));

            areaInfo.addEdge(edge, edgeInfo);

            sumOfLengths += length;

            if (length > longer) {
                longer = length;
                longestEdge = edge;
            }
            if (length < smaller) {
                smaller = length;
                smallestEdge = edge;
            }
        }

        if (longestEdge != null) {
            areaInfo.setLongestEdge(longestEdge);
        }
        if (smallestEdge != null) {
            areaInfo.setSmallestEdge(smallestEdge);
        }
        areaInfo.setMilieu(sumOfLengths);
    }

    // public AreaInfo getAreaInfoMap(EntityID id) {
    // return areaInfoMap.get(id);
    // }

    public static int totalRepairCost(ApolloWorld world, Area area) {
        int total = 0;
        if (area.isBlockadesDefined()) {
            for (EntityID blockId : area.getBlockades()) {
                Blockade blockade = (Blockade) world.getEntity(blockId);
                if (blockade != null) {
                    total += blockade.getRepairCost();
                }
            }
        } else {
            total = -1;
        }
        return total;
    }

    public static Edge getLongestEdge(Area area) {
        double length;
        double longer = Double.MIN_VALUE;
        Edge longestEdge = null;

        for (Edge edge : area.getEdges()) {
            length = EdgeInfoComponent.getEdgeLength(edge);
            if (length > longer) {
                longer = length;
                longestEdge = edge;
            }
        }
        return longestEdge;
    }

    public static Edge getSmallestEdge(Area area) {
        double length;
        double smaller = Double.MAX_VALUE;
        Edge smallestEdge = null;

        for (Edge edge : area.getEdges()) {
            length = EdgeInfoComponent.getEdgeLength(edge);
            if (length < smaller) {
                smaller = length;
                smallestEdge = edge;
            }
        }
        return smallestEdge;
    }

    public List<EntityID> whoIsIn(EntityID areaId) {
        List<EntityID> list = new ArrayList<EntityID>();
        Collection<StandardEntity> entities = world.getObjectsInRange(areaId,
                getViewDistance());

        for (StandardEntity entity : entities) {
            if ((entity instanceof Human)
                    && ((Human) entity).isPositionDefined()
                    && ((Human) entity).getPosition().equals(areaId)) {
                list.add(entity.getID());
            }
        }

        return list;
    }

    public int getViewDistance(){
        return world.getScenarioInfo().getPerceptionLosMaxDistance();
    }
    public boolean isEmptyBuilding(EntityID areaId) {
        Collection<StandardEntity> entities = world.getObjectsInRange(areaId,
                getViewDistance());

        for (StandardEntity entity : entities) {
            if ((entity instanceof Human)
                    && ((Human) entity).isPositionDefined()
                    && ((Human) entity).getPosition().equals(areaId)) {
                return true;
            }
        }

        return true;
    }
}
