package ApolloRescue.module.universal.entities;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.entities.RoadModel;
import adf.agent.info.WorldInfo;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

public class EdgeModel {

    private boolean isPassable;
    private Line2D line;
    private Point2D start;
    private Point2D end;
    private Point2D middle;
    private boolean isBlocked;
    private boolean isAbsolutelyBlocked;
    private Integer blockedSize;
    private Edge parent;
    private Line2D openPart;
    private Pair<EntityID, EntityID> neighbours;
    private double length;
    private boolean tooSmall;
    protected ApolloWorld world;

    public EdgeModel(ApolloWorld world, Edge edge, EntityID parentID) {
        parent = edge;
        this.world = world;
        neighbours = new Pair<>(parentID, edge.getNeighbour());
        initialize(edge.isPassable(), edge.getStart(), edge.getEnd());
    }

    public EdgeModel(boolean passable, Point2D start, Point2D end) {
        parent = null;
        initialize(passable, start, end);
    }


    public EdgeModel(Edge edge, EntityID parentID) {
        parent = edge;
        neighbours = new Pair<EntityID, EntityID>(parentID, edge.getNeighbour());
        initialize(edge.isPassable(), edge.getStart(), edge.getEnd());
    }

    private void initialize(boolean passable, Point2D start, Point2D end) {
        this.isAbsolutelyBlocked = false;
        this.isBlocked = false;
        this.blockedSize = null;
        this.start = start;
        this.end = end;
        this.middle = Util.getMiddle(start, end);
        this.line = new Line2D(start, end);
        this.isPassable = passable;
        this.openPart = new Line2D(start, end);
        length = Util.distance(start, end);
        tooSmall = length < ApolloConstants.AGENT_PASSING_THRESHOLD;
    }

    public boolean isPassable() {
        return isPassable;
    }

    public boolean isOtherSideBlocked(WorldInfo world) {
        if (isPassable()) {
            EdgeModel edgeModel = getOtherSideEdge(world);
            if (edgeModel != null && edgeModel.isBlocked()) {
                return true;
            }
        }
        return false;
    }

    public EdgeModel getOtherSideEdge(WorldInfo world) {
//        Area neighbour = world.getEntity(getNeighbours().second(), Area.class);
        Area neighbour = (Area) world.getEntity(getNeighbours().second());
        if (neighbour instanceof Road) {
            RoadModel roadModelNeighbour = getApolloRoad(neighbour.getID());
            return roadModelNeighbour.getEdgeInPoint(getMiddle());
        }
        return null;
    }

    private RoadModel getApolloRoad(EntityID id) {
        return world.getRoadModel(id);
    }

    public Line2D getLine() {
        return line;
    }

    public Point2D getStart() {
        return start;
    }

    public Point2D getEnd() {
        return end;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public boolean isAbsolutelyBlocked() {
        return isAbsolutelyBlocked;
    }

    public Integer getBlockedSize() {
        return blockedSize;
    }

    public void setBlockedSize(Integer blockedSize) {
        if (blockedSize == null || blockedSize < this.blockedSize) {
            this.blockedSize = blockedSize;
        }
    }

    public void setBlocked(boolean blocked) {
        if(!blocked){
            setAbsolutelyBlocked(false);
        }
        isBlocked = blocked;
    }

    public void setAbsolutelyBlocked(boolean absolutelyBlocked) {
        if(absolutelyBlocked){
            setBlocked(true);
        }
        isAbsolutelyBlocked = absolutelyBlocked;
    }

    public Edge getParent() {
        return parent;
    }

    public Point2D getMiddle() {
        return middle;
    }

    public Pair<EntityID, EntityID> getNeighbours() {
        return neighbours;
    }

    public boolean equals(EdgeModel other) {
        return (other.getLine().equals(getLine()));
    }

    public Line2D getOpenPart() {
        return openPart;
    }

    public void setOpenPart(Line2D openPart) {
        this.openPart = openPart;
    }

    public double getLength() {
        return length;
    }

    public boolean isTooSmall() {
        return tooSmall;
    }
}
