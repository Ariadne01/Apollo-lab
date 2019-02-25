package ApolloRescue.module.universal.newsearch.node;

import ApolloRescue.module.complex.component.AreaInfoComponent;
import ApolloRescue.module.complex.component.Info.NodeInfo;
import ApolloRescue.module.universal.ApolloWorld;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeFactory {
    ApolloWorld world;
    List<Node> nodes;
    Map<EntityID, Node> areaNode;
    // List<Node> shownNodes_;
    double averageWidth = 0;

    public NodeFactory(ApolloWorld world) {
        this.world = world;
        this.nodes = new ArrayList<Node>();
        this.areaNode = new HashMap<EntityID, Node>();
    }

    // ������е�Node
    public void createNodes() {
        findNodes();

    }

    public List<Node> getNodes() {
        return nodes;
    }

    // �ҳ�WorldModel�е�����Node SetNodes()
    public void findNodes() {
        if (nodes != null) {
            for (Entity entity : world.getWorldInfo().getAllEntities()) {
                if (entity instanceof Road || entity instanceof Building) {
                    Area area;
                    area = (Area) entity;
                    // Add ��һ��Area���������
                    addAreaNode(area);

                }
            }
        }
    }

    public void addAreaNode(Area area) {

        Node node;
        node = new Node(area, world);
        node.setPassRate(world);

        if (!nodes.contains(node)) {
            NodeInfo info;
            averageWidth += Math.max(area.getShape().getBounds().getWidth(),
                    area.getShape().getBounds().getHeight());

            nodes.add(node); // Add this node to nodes
            node.setId(nodes.size());
            areaNode.put(area.getID(), node);

            // Set Area Info Map
            info = world.getComponent(AreaInfoComponent.class).getNodeInfo(
                    area.getID());

            for (EntityID id : area.getNeighbours()) {
                Entity neighbor;
                neighbor = world.getEntity(id);
                if (neighbor instanceof Area) {
                    Area otherArea;
                    otherArea = (Area) neighbor;
                    Node otherNode = new Node(otherArea, world);
                    otherNode.setPassRate(world);
                    node.addConnectedNode(otherNode);// ����һ��
                    info.addConnectedNode(otherNode);
                }
            }

        }
    }

    // �����Area����Node
    public static Node getNode(Point point, Area area) {
        Node node;
        node = new Node(point.x, point.y);
        node.setArea(area);
        return node;

    }

    // ***���areaNode
    public Map<EntityID, Node> getAreaNode() {
        return this.areaNode;
    }

    public double getAverageWidth() {
        return averageWidth / nodes.size();
    }
}
