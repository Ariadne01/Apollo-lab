package ApolloRescue.module.algorithm.convexhull;

import java.util.ArrayList;
import java.util.List;

import ApolloRescue.module.universal.entities.BuildingModel;
import ApolloRescue.module.universal.tools.Ruler;

import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Edge;
import rescuecore2.worldmodel.EntityID;




public class ConvexHullEdge {
    private EntityID id;
    private BuildingModel start;
    private BuildingModel end;
    private List<BuildingModel> nearBuildings;
    private static final double value = 8000;

    ConvexHullEdge(BuildingModel start, BuildingModel end) {
        this.start = start;
        this.end = end;
        nearBuildings = new ArrayList<BuildingModel>();
    }

    public BuildingModel getStart() {
        return start;
    }

    public void setStart(BuildingModel start) {
        this.start = start;
    }

    public BuildingModel getEnd() {
        return end;
    }

    public void setEnd(BuildingModel end) {
        this.end = end;
    }

    public List<BuildingModel> getNearBuildings() {
        return nearBuildings;
    }

    public void setNearBuildings(List<BuildingModel> buildings) {
        this.nearBuildings = buildings;

    }

    public Boolean IsNearToMe(BuildingModel building) {
        if (start != null && end != null && building != null) {
            double k = (end.getSelfBuilding().getY() - start.getSelfBuilding().getY())
                    / (end.getSelfBuilding().getX() - start.getSelfBuilding().getX());
            double distance = (Math.abs(k * building.getSelfBuilding().getX()
                    - building.getSelfBuilding().getY() - k
                    * start.getSelfBuilding().getX() + start.getSelfBuilding().getY()))
                    / (Math.sqrt(1 + k * k));
            if (distance < value) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public double getDistance(BuildingModel building) {
        if (start != null && end != null && building != null) {
            if (start.equals(end)) {
                return value * 10;
            }
            Point2D p1 = new Point2D(start.getSelfBuilding().getX(), start
                    .getSelfBuilding().getY());
            Point2D p2 = new Point2D(end.getSelfBuilding().getX(), end
                    .getSelfBuilding().getY());
            Point2D center = new Point2D(building.getSelfBuilding().getX(),
                    building.getSelfBuilding().getY());
            Edge edge = new Edge(p1, p2);
            double distance = Ruler.getDistance(center, edge);
            // double
            // k=(end.getBuilding().getY()-start.getBuilding().getY())/(end.getBuilding().getX()-start.getBuilding().getX());
            // double
            // distance=(Math.abs(k*building.getBuilding().getX()-building.getBuilding().getY()-k*start.getBuilding().getX()+start.getBuilding().getY()))/(Math.sqrt(1+k*k));
            return distance;
        }
        return value * 10;
    }

    public EntityID getId() {
        return id;
    }

    public void setId(EntityID id) {
        this.id = id;
    }

    public String toString() {
        return "( " + start + " , " + end + " ) " + " ( " + nearBuildings
                + " ) ";
    }

}
