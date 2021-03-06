package ApolloRescue.module.complex.component.Info;

import javolution.util.FastMap;
import rescuecore2.standard.entities.Edge;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoadInfo {

    List<EntityID> neighbours = new ArrayList<EntityID>();
    EntityID pathId;
    Boolean passable;
    Boolean isolated;
    Boolean seen;
    Integer value;
    int lastMessageTime = -5;
    Map<Edge, Boolean> edgesInfoMap;

    public RoadInfo(List<EntityID> neighbours, List<Edge> edgeList) {
        this.neighbours = neighbours;
        edgesInfoMap = new FastMap<Edge, Boolean>();
        for (Edge edge : edgeList){
            edgesInfoMap.put(edge,edge.isPassable());
        }
        isolated = false;
    }

    public void setPathId(EntityID pathId) {
        this.pathId = pathId;
    }


    public void setPassable(Boolean passable) {
        this.passable = passable;
    }

    public void setIsolated(Boolean isolated) {
        this.isolated = isolated;
    }

    public void setSeen(Boolean seen) {
        this.seen = seen;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public void setLastMessageTime(int lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public List<EntityID> getNeighbours() {
        return neighbours;
    }

    public EntityID getPathId() {
        return pathId;
    }

    public Boolean isPassable() {
        return passable;
    }

    public Boolean isIsolated() {
        return isolated;
    }

    public Boolean isSeen() {
        return seen;
    }

    public Integer getValue() {
        return value;
    }

    public int getLastMessageTime() {
        return lastMessageTime;
    }

    public boolean isThisEdgePassable(Edge edge){
        return edgesInfoMap.get(edge);
    }

    public void setEdgePassably(Edge edge , boolean passably){
        edgesInfoMap.put(edge,passably);
    }
}
