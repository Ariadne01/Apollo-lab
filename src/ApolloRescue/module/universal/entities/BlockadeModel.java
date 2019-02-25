package ApolloRescue.module.universal.entities;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.algorithm.convexhull.PolygonUtil;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Blockade;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BlockadeModel {
    private Polygon blockade;
    private Blockade parent;
    private RoadModel position;
    private Polygon transformedPolygon;
    private java.util.List<EdgeModel> blockedEdges;
    private int repairCost;
    private BlockadeValue value;
    private List<Point2D> apexPoints;
    private int groundArea;

    public BlockadeModel(RoadModel road, Blockade blockade, Polygon polygon) {
        initialize(road, blockade, polygon);
    }

    private void initialize(RoadModel road, Blockade blockade, Polygon polygon) {
        this.parent = blockade;
        this.blockade = polygon;
        blockedEdges = new ArrayList<EdgeModel>();
        position = road;
        repairCost = blockade.getRepairCost();
        this.apexPoints = new ArrayList<Point2D>();
        setApexPoint();
    }

    public void create(Polygon polygon) {
        blockade = polygon;
    }

    public void createTransformedPolygon(ScreenTransform t) {
        int xs[] = new int[blockade.npoints];
        int ys[] = new int[blockade.npoints];
        for (int i = 0; i < blockade.npoints; i++) {
            xs[i] = t.xToScreen(blockade.xpoints[i]);
            ys[i] = t.yToScreen(blockade.ypoints[i]);
        }
        transformedPolygon = new Polygon(xs, ys, blockade.npoints);
    }

    public Polygon getPolygon() {
        return blockade;
    }

    public Polygon getTransformedPolygon() {
        return transformedPolygon;
    }

    //根据Line2D获得分离的障碍物模型
    public Pair<BlockadeModel, BlockadeModel> split(Line2D lineSegment) {
        Pair<Polygon, Polygon> splitPolygon = PolygonUtil.split(parent.getApexes(), lineSegment);
        BlockadeModel block1 = new BlockadeModel(position, parent, splitPolygon.first());
        BlockadeModel block2 = new BlockadeModel(position, parent, splitPolygon.second());
        return new Pair<BlockadeModel, BlockadeModel>(block1, block2);
    }

    //根据两个点调用上面的方法获得分离的障碍物模型
    public Pair<BlockadeModel, BlockadeModel> split(Point point1, Point point2) {
        Point2D startPoint = new Point2D(point1.getX(), point1.getY());
        Point2D endPoint = new Point2D(point2.getX(), point2.getY());
        return split(new Line2D(startPoint, endPoint));
    }

    public java.util.List<EdgeModel> getBlockedEdges() {
        return blockedEdges;
    }

    public void addBlockedEdges(EdgeModel edgeModel) {
        if (!blockedEdges.contains(edgeModel))
            blockedEdges.add(edgeModel);
    }

    private void setApexPoint() {
        apexPoints.clear();

        int[] apexes = parent.getApexes();
        int count = apexes.length / 2;
        for (int i = 0; i < count; ++i) {
            apexPoints.add(new Point2D(apexes[i * 2], apexes[(i * 2) + 1]));
        }
//        Polygon shape = new Polygon(xs, ys, count);
//        int points = blockade.npoints;
//        for (int i = 0; i < points; i ++) {
//            apexPoints.add(new Point2D(blockade.xpoints[i], blockade.ypoints[i]));
//        }
        computeGroundArea();
    }

    private void computeGroundArea() {
        double area = GeometryTools2D.computeArea(apexPoints) * ApolloConstants.SQ_MM_TO_SQ_M;
        groundArea = (int) Math.abs(area);
    }

    public Pair<BlockadeModel, BlockadeModel> split(EdgeModel edge) {
        return split(edge.getLine());
    }

    public RoadModel getPosition() {
        return position;
    }

    public Blockade getParent() {
        return parent;
    }

    public int getRepairCost() {
        return repairCost;
    }

    public BlockadeValue getValue() {
        return value;
    }

    public void setValue(BlockadeValue value) {
        this.value = value;
    }

    public int getGroundArea() {
        return groundArea;
    }

    @Override
    public boolean equals(Object obj) {
        BlockadeModel otherBlockade;
        if (obj instanceof BlockadeModel) {
            otherBlockade = (BlockadeModel) obj;
            return getPolygon().equals(otherBlockade.getPolygon());
        }
        return false;
    }
}