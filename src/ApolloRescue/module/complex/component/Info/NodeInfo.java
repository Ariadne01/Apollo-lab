package ApolloRescue.module.complex.component.Info;

import ApolloRescue.module.universal.newsearch.node.Node;
import rescuecore2.standard.entities.Area;

import java.util.ArrayList;
import java.util.List;

public class NodeInfo {

    private Area area;
    private List<Node> connectedNodes;
    private List<Node> connectedTerminalNodes;  //Leaf node?

    public NodeInfo(Area area){
        this.area = area;
        connectedNodes = new ArrayList<Node>();
        connectedTerminalNodes = new ArrayList<Node>();
    }

    public void addConnectedNode(Node node){
        if(node!=null){
            connectedNodes.add(node);
        }
    }

    public List<Node> getConnectedNodes(){
        return connectedNodes;
    }

    public void addTerminalNode(Node node){
        this.connectedTerminalNodes.add(node);
    }

    public List<Node> getTerminalNodes(){
        return this.connectedTerminalNodes;

    }

}
