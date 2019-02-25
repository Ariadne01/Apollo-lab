package ApolloRescue.module.algorithm.clustering;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ApolloRescue.module.universal.entities.BuildingModel;
import ApolloRescue.module.algorithm.convexhull.MathUtils;
import ApolloRescue.module.algorithm.convexhull.ConvexHullEdge;

import rescuecore2.worldmodel.EntityID;

public class ConvexHull {
    private EntityID ID;
    private List<BuildingModel> apexBuildings;
    private Point centerPoint;
    private BuildingModel centerBuilding;
    private int burningBuildingNum;
    private int area;
    private int groundArea; // mended
    private List<ConvexHullEdge> edges;
    private List<BuildingModel> allEdgeBuildings;
    private List<BuildingModel> allBuildingsOnHull;
    private double radius; // 凸包半径
    private static final int MaxValue = 100000000;
    private Polygon polygon = null;

    com.poths.rna.data.ConvexHull convexHull = new com.poths.rna.data.ConvexHull();

    public ConvexHull() {
        apexBuildings = new ArrayList<BuildingModel>();
        edges = new ArrayList<ConvexHullEdge>();
        allEdgeBuildings = new ArrayList<BuildingModel>();
    }

    public void setBuildingsOnHull(List<BuildingModel> buildings) {
        this.allBuildingsOnHull = buildings;
    }

    public EntityID getId() {
        return this.ID;
    }

    public void setId(EntityID id) {
        this.ID = id;
    }

    public Point getCenterPoint() {
        return this.centerPoint;
    }

    public void setCenterPoint(Point point) {
        this.centerPoint = point;
    }

    public void setCenterPoint() {
        int maxX = 0, maxY = 0, minX = MaxValue, minY = MaxValue;
        if (!apexBuildings.isEmpty()) {
            BuildingModel p1 = null, p2 = null, p3 = null, p4 = null;
            for (BuildingModel building : apexBuildings) {
                if (maxX < building.getSelfBuilding().getX()) { // p1X最大的建筑
                    maxX = building.getSelfBuilding().getX();
                    p1 = building;
                }
                if (maxY < building.getSelfBuilding().getY()) { // p2Y最大的建筑
                    maxY = building.getSelfBuilding().getY();
                    p2 = building;
                }
                if (minX > building.getSelfBuilding().getX()) { // p3X最大小的建筑
                    minX = building.getSelfBuilding().getX();
                    p3 = building;
                }
                if (minY > building.getSelfBuilding().getY()) { // p4Y最小的建筑
                    minY = building.getSelfBuilding().getY();
                    p4 = building;
                }
            }
            if (p1 != null && p2 != null && p3 != null && p4 != null) {
                int x = (p1.getSelfBuilding().getX()
                        + p2.getSelfBuilding().getX()
                        + p3.getSelfBuilding().getX() + p4.getSelfBuilding()
                        .getX()) / 4;
                int y = (p1.getSelfBuilding().getY()
                        + p2.getSelfBuilding().getY()
                        + p3.getSelfBuilding().getY() + p4.getSelfBuilding()
                        .getY()) / 4;
                centerPoint = new Point(x, y);
                List<BuildingModel> buildings = new ArrayList<BuildingModel>();
                buildings.add(p1);
                buildings.add(p2);
                buildings.add(p3);
                buildings.add(p4);
                setRadius(buildings);
            }

        }
        // 计算中心建筑
        calcCenter();
    }

    private void calcCenter() {
        double x = 0, y = 0;
        for (BuildingModel a : apexBuildings) {
            x += a.x();
            y += a.y();
        }
        if (this.apexBuildings.size() != 0) {
            x = x / apexBuildings.size();
            y = y / apexBuildings.size();
        }

        int ix = (int) x;
        int iy = (int) y;
        double mindist = Double.MAX_VALUE;
        for (BuildingModel a : apexBuildings) {
            double temp = MathUtils.manhattanDistance(a.getSelfBuilding(), ix,
                    iy);
            if (temp < mindist) {
                mindist = temp;
                centerBuilding = a;
            }
        }
    }

    /**
     * 火区中心建筑，由凸包边上建筑计算
     *
     * @return
     */
    public BuildingModel getCenter() {
        if (centerBuilding == null) {
            calcCenter();
        }
        return centerBuilding;
    }

    public void setRadius(List<BuildingModel> burningBuildings) {
        if (burningBuildings != null && !burningBuildings.isEmpty()) {
            double radius = 0;
            for (BuildingModel building : burningBuildings) {
                double buildingWidth = getBuildingWidth(building);
                double distance = getDistance(centerPoint.getX(),
                        centerPoint.getY(), building.getSelfBuilding().getX(),
                        building.getSelfBuilding().getY());
                radius += distance + buildingWidth / 2;
            }

            this.radius = radius / burningBuildings.size();
        }

    }

    protected int getDistance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return (int) Math.hypot(dx, dy);
    }

    public double getBuildingWidth(BuildingModel burningBuilding) {

        double w_1 = burningBuilding.getSelfBuilding().getShape().getBounds()
                .getHeight();
        double w_2 = burningBuilding.getSelfBuilding().getShape().getBounds()
                .getWidth();
        double width = ((w_1 + w_2) / 2);
        return width;
    }

    public List<BuildingModel> getApexBuildings() {
        return apexBuildings;
    }

    public void setApexBuildings(List<BuildingModel> buildings) {
        this.apexBuildings = buildings;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ID == null) ? 0 : ID.hashCode());
        result = prime * result
                + ((apexBuildings == null) ? 0 : apexBuildings.hashCode());
        result = prime * result
                + ((centerPoint == null) ? 0 : centerPoint.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConvexHull other = (ConvexHull) obj;
        if (ID == null) {
            if (other.ID != null)
                return false;
        } else if (!ID.equals(other.ID))
            return false;
        if (apexBuildings == null) {
            if (other.apexBuildings != null)
                return false;
        } else if (!apexBuildings.equals(other.apexBuildings))
            return false;
        if (centerPoint == null) {
            if (other.centerPoint != null)
                return false;
        } else if (!centerPoint.equals(other.centerPoint))
            return false;
        return true;
    }

    public double getArea() {
        return area;
    }

    public void setArea(int area) {
        this.area = area;
    }

    public double getGroundArea() { // mended
        return groundArea;
    }

    public void setGroundArea(int area) { // mended
        this.groundArea = area;
    }

    public int getBurningBuildingNum() {
        return burningBuildingNum;
    }

    /**
     * for Viewer
     *
     * @return Point
     */
    public List<Point> getBurningBuildingPoint() {
        List<Point> result = new ArrayList<Point>();
        for (BuildingModel bb : apexBuildings) {
            if (bb != null && bb.getSelfBuilding() != null) {
                result.add(new Point(bb.getSelfBuilding().getX(), bb
                        .getSelfBuilding().getY()));
            }
        }
        return result;
    }

    public void setBurningBuildingNum(int burningBuildingNum) {
        this.burningBuildingNum = burningBuildingNum;
    }

    public List<ConvexHullEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<ConvexHullEdge> edges) {
        this.edges = edges;
    }

    public void setAllEdgesBuildings(List<ConvexHullEdge> edges) {
        if (!edges.isEmpty()) {
            for (ConvexHullEdge edge : edges) {
                if (edge == null) {
                    continue;
                }
                for (BuildingModel build : edge.getNearBuildings()) {
                    if (!this.allEdgeBuildings.contains(build)) {
                        this.allEdgeBuildings.add(build);
                    }
                }
            }
        }
    }

    /**
     * @author dyg XXX
     * @return
     */
    public List<BuildingModel> getAllNearBuildings() {
        List<BuildingModel> buildings = new ArrayList<BuildingModel>();
        if (!this.getEdges().isEmpty()) {
            for (ConvexHullEdge edge : this.getEdges()) {
                buildings.addAll(edge.getNearBuildings());
            }
        }
        return buildings;
    }

    public List<BuildingModel> getAllEdgeBuildings() {
        return this.allEdgeBuildings;

    }

    public void clear() {
        this.allEdgeBuildings.clear();
        this.apexBuildings.clear();
        this.edges.clear();
    }

    public List<BuildingModel> getHullBuildings() {
        List<BuildingModel> hullBuildings = new ArrayList<BuildingModel>();
        hullBuildings.addAll(apexBuildings);
        hullBuildings.addAll(allEdgeBuildings);
        return hullBuildings;
    }

    public List<BuildingModel> getHullBuildingsNew() {
        this.allBuildingsOnHull.addAll(apexBuildings);
        this.allBuildingsOnHull.addAll(allEdgeBuildings);
        return this.allBuildingsOnHull;
    }

    public boolean isSmallHull() {
        if (!isEmpty()) {
            if (this.getHullBuildings().size() != 0
                    && this.getHullBuildings().size() < 3) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean isEmpty() {
        if (this.allEdgeBuildings.size() == 0 && this.apexBuildings.size() == 0) {
            return true;
        }
        return false;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public String toString() {
        return "convexHull " + "(" + "ID " + ID + " buildings " + apexBuildings
                + " )";
    }

    /**
     * 凸包的几何图形 由Apexs构成
     *
     * @return
     */
    public Polygon getPolygon() {
        setPolygon();
        return polygon;
    }

    public void setPolygon() {
        Polygon p = new Polygon();
        for (BuildingModel apex : getApexBuildings()) {
            p.addPoint(apex.x(), apex.y());
        }
        this.polygon = p;
    }

    /**
     * 缩放凸包
     *
     * @param convex
     * @param scale
     * @return
     */
    public Polygon getScaleConvexHull(Polygon convex, float scale) {
        Polygon p = new Polygon();
        int size = convex.npoints;
        int[][] convexArray = new int[2][size];
        int[] xpoints = new int[size];
        int[] ypoints = new int[size];
        for (int i = 0; i < size; i++) {
            xpoints[i] = convex.xpoints[i];
            ypoints[i] = convex.ypoints[i];
        }
        convexArray[0] = xpoints;
        convexArray[1] = ypoints;
        scaleConvex(convexArray, scale);
        p.xpoints = convexArray[0];
        p.ypoints = convexArray[1];
        p.npoints = size;
        return p;
    }

    /**
     * 对凸包进行缩放
     *
     * @param convex
     * @param scale
     */
    public static void scaleConvex(int[][] convex, float scale) {
        int size = convex[0].length;
        double Cx, Cy;
        Cx = Cy = 0d;
        for (int i = 0; i < size; i++) {
            Cx += convex[0][i];
            Cy += convex[1][i];
        }
        Cx /= size;
        Cy /= size;

        for (int i = 0; i < size; i++) {
            convex[0][i] = (int) ((convex[0][i] - Cx) * scale + Cx);
            convex[1][i] = (int) ((convex[1][i] - Cy) * scale + Cy);
        }
    }

    //--------------------------------------------------------------------------------
    // TODO for direction
    private Polygon triangle;
    public Point CENTER_POINT;
    public java.awt.Point FIRST_POINT;
    public java.awt.Point SECOND_POINT;
    public java.awt.Point CONVEX_POINT;
    //-------------
    public java.awt.Point OTHER_POINT1;
    public java.awt.Point OTHER_POINT2;
    public Set<Point2D> CONVEX_INTERSECT_POINTS;
    public Set<Line2D> CONVEX_INTERSECT_LINES;
    public Polygon DIRECTION_POLYGON;

    public void setTrianglePolygon(Polygon shape) {
        int xs[] = new int[shape.npoints];
        int ys[] = new int[shape.npoints];
        for (int i = 0; i < shape.npoints; i++) {
            xs[i] = shape.xpoints[i];
            ys[i] = shape.ypoints[i];
        }
        triangle = new Polygon(xs, ys, shape.npoints);
    }

    public Polygon getTriangle() {
        return triangle;
    }

    public void addPoint(int x, int y) {
        convexHull.addPoint(new com.poths.rna.data.Point(x, y));
    }

    public Polygon convex() {
        List<com.poths.rna.data.Point> pointList = convexHull.getPointList();
        int xPoints2[] = new int[pointList.size()];
        int yPoints2[] = new int[pointList.size()];

        for (int i = 0; i < pointList.size(); i++) {
            xPoints2[i] = (int) pointList.get(i).getX();
            yPoints2[i] = (int) pointList.get(i).getY();
        }

        return new Polygon(xPoints2, yPoints2, pointList.size());
    }


}

