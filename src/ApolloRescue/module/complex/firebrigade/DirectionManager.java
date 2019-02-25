package ApolloRescue.module.complex.firebrigade;

import ApolloRescue.module.algorithm.clustering.ApolloFireZone;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.entities.BuildingModel;
import ApolloRescue.module.universal.entities.DirectionObject;
import ApolloRescue.module.universal.entities.DirectionSide;
import javolution.util.FastList;
import javolution.util.FastSet;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.GasStation;
import rescuecore2.standard.entities.Refuge;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DirectionManager {
    private ApolloWorld world;
    private double REFUGE_COEFFICIENT = 0.001;
    private double GAS_STATION_COEFFICIENT = 0.4;
    private int theta = 20;// 30;
    private math.geom2d.Point2D targetPoint;

    public DirectionManager(ApolloWorld world) {
        this.world = world;
    }

    public Point findFarthestPointOfMap(ApolloFireZone fireCluster) {

        math.geom2d.Point2D fireClusterCenter = new math.geom2d.Point2D(
                fireCluster.getCenter());
        java.util.List<Point2D> points = new ArrayList<Point2D>();
        points.add(new rescuecore2.misc.geometry.Point2D(0, 0));
        points.add(new rescuecore2.misc.geometry.Point2D(0, world
                .getMapHeight()));
        points.add(new rescuecore2.misc.geometry.Point2D(world.getMapWidth(), 0));
        points.add(new rescuecore2.misc.geometry.Point2D(world.getMapWidth(),
                world.getMapHeight()));

        rescuecore2.misc.geometry.Point2D farthestPointOfMap = Util
                .findFarthestPoint(
                        fireCluster.getConvexHullObject().getConvexPolygon(), points);

        targetPoint = new math.geom2d.Point2D(farthestPointOfMap.getX(),
                farthestPointOfMap.getY());

        double degree = 0;
        while (degree < 360) {
            if (fireCluster.hasBuildingInDirection(
                    new Point((int) targetPoint.getX(), (int) targetPoint
                            .getY()), true, false)) {
                break;
            }
            targetPoint = targetPoint
                    .rotate(fireClusterCenter, Math.toRadians((world.getSelf()
                            .getID().getValue() % 2) == 0 ? theta : -theta));
            degree += theta;
        }
        return new Point((int) targetPoint.getX(), (int) targetPoint.getY());

    }

    // our firezone center is corrective
    /**
     * create max 8 and min 3 cube with fire cluster bound2D and calculate
     * buildings area in each polygon.
     * <p/>
     * ____ ____ ____max map x,y | | | | |____|____|____| | |####| |
     * |____|####|____| | | | | y |____|____|____| x
     *
     * @param fireCluster
     *            target cluster
     * @param clusters
     *            all fire clusters
     * @return point for direction target
     */
    public Point findDirectionPointInMap(ApolloFireZone fireCluster,
                                         java.util.List<ApolloFireZone> clusters) {

        List<DirectionObject> polygons = new FastList<DirectionObject>();
        Rectangle2D fireClusterShape = fireCluster.getConvexHullObject().getConvexPolygon().getBounds2D();
        math.geom2d.Point2D fireClusterCenter = new math.geom2d.Point2D(
                fireCluster.getCenter());
        int mapMaxX = (int) world.getBounds().getWidth();
        int mapMaxY = (int) world.getBounds().getHeight();
        int clusterMinX = (int) fireClusterShape.getMinX();
        int clusterMaxX = (int) fireClusterShape.getMaxX();
        int clusterMinY = (int) fireClusterShape.getMinY();
        int clusterMaxY = (int) fireClusterShape.getMaxY();
        // boolean IAmOddIndex = (world.getSelf().getID().getValue() % 2) == 0;
        boolean IAmOddIndex = (world.getFireBrigadeList().indexOf(
                world.getSelfHuman()) % 2) == 0;
        Polygon polygon;
        if (clusterMinY > 0) {
            if (clusterMinX > 0) {
                polygon = new Polygon();
                polygon.addPoint(0, 0);
                polygon.addPoint(clusterMinX, 0);
                polygon.addPoint(clusterMinX, clusterMinY);
                polygon.addPoint(0, clusterMinY);
                polygons.add(new DirectionObject(polygon,
                        DirectionSide.Southwest));
            }

            polygon = new Polygon();
            polygon.addPoint(clusterMinX, 0);
            polygon.addPoint(clusterMaxX, 0);
            polygon.addPoint(clusterMaxX, clusterMinY);
            polygon.addPoint(clusterMinX, clusterMinY);
            polygons.add(new DirectionObject(polygon, DirectionSide.South));

            if (clusterMaxX < mapMaxX) {
                polygon = new Polygon();
                polygon.addPoint(clusterMaxX, 0);
                polygon.addPoint(mapMaxX, 0);
                polygon.addPoint(mapMaxX, clusterMinY);
                polygon.addPoint(clusterMaxX, clusterMinY);
                polygons.add(new DirectionObject(polygon,
                        DirectionSide.Southeast));
            }
        }
        if (clusterMinX > 0) {
            polygon = new Polygon();
            polygon.addPoint(0, clusterMinY);
            polygon.addPoint(clusterMinX, clusterMinY);
            polygon.addPoint(clusterMinX, clusterMaxY);
            polygon.addPoint(0, clusterMaxY);
            polygons.add(new DirectionObject(polygon, DirectionSide.West));
        }
        if (clusterMaxX < mapMaxX) {
            polygon = new Polygon();
            polygon.addPoint(clusterMaxX, clusterMinY);
            polygon.addPoint(mapMaxX, clusterMinY);
            polygon.addPoint(mapMaxX, clusterMaxY);
            polygon.addPoint(clusterMaxX, clusterMaxY);
            polygons.add(new DirectionObject(polygon, DirectionSide.East));
        }
        if (clusterMaxY < mapMaxY) {
            if (clusterMinX > 0) {
                polygon = new Polygon();
                polygon.addPoint(0, clusterMaxY);
                polygon.addPoint(clusterMinX, clusterMaxY);
                polygon.addPoint(clusterMinX, mapMaxY);
                polygon.addPoint(0, mapMaxY);
                polygons.add(new DirectionObject(polygon,
                        DirectionSide.Northwest));
            }

            polygon = new Polygon();
            polygon.addPoint(clusterMinX, clusterMaxY);
            polygon.addPoint(clusterMaxX, clusterMaxY);
            polygon.addPoint(clusterMaxX, mapMaxY);
            polygon.addPoint(clusterMinX, mapMaxY);
            polygons.add(new DirectionObject(polygon, DirectionSide.North));

            if (clusterMaxX < mapMaxX) {
                polygon = new Polygon();
                polygon.addPoint(clusterMaxX, clusterMaxY);
                polygon.addPoint(mapMaxX, clusterMaxY);
                polygon.addPoint(mapMaxX, mapMaxY);
                polygon.addPoint(clusterMaxX, mapMaxY);
                polygons.add(new DirectionObject(polygon,
                        DirectionSide.Northeast));
            }
        }
        double maxValue = Double.MIN_VALUE;
        DirectionObject selectedDirection = null;
        Set<DirectionSide> ignoredSides = new FastSet<DirectionSide>();
        for (DirectionObject direction : polygons) {
            Polygon poly = direction.getPolygon();
            boolean hasFire = false;
            // int minWidth = (int) Math.min(poly.getBounds2D().getWidth(),
            // poly.getBounds2D().getHeight()) / 5;

            for (ApolloFireZone c : clusters) {
                if (!c.isDying()
                        && poly.contains(c.getCenter())) {
                    // int clusterMaxWidth = (int)
                    // Math.max(poly.getBounds2D().getWidth(),
                    // poly.getBounds2D().getHeight());
                    if (!c.isControllable()) {
                        // if (clusterMaxWidth > minWidth) {
                        hasFire = true;
                        break;
                    }
                }
            }
            if (hasFire) {
                ignoredSides.add(direction.getSide());
                direction.setValue(0.0);
                continue;
            }
            double value = 0;
            int refugeNo = 0;
            int gasStationNo = 0;
            for (BuildingModel building : world.getBuildingsModel()) {
                // TODO not border &&
                // !world.getBorderBuildings().contains(building.getID())
                if (building.getEstimatedFieryness() == 0
                        && poly.contains(building.getSelfBuilding().getX(),
                        building.getSelfBuilding().getY())
                        && !building.isMapSide()) {
                    if (building.getSelfBuilding() instanceof Refuge) {
                        // value += 1000;
                        refugeNo++;
                    } else if (building.getSelfBuilding() instanceof GasStation) {
                        // value += 1000;
                        gasStationNo++;
                    } else {
                        value += building.getSelfBuilding().getTotalArea();
                    }
                }
            }

            if (value == 0) {
                ignoredSides.add(direction.getSide());
            } else {
                value += (refugeNo * REFUGE_COEFFICIENT * value);
                value += (gasStationNo * GAS_STATION_COEFFICIENT * value);
            }
            direction.setValue(value);
            direction.setRefugeNo(refugeNo);
            direction.setGasStationNo(gasStationNo);
            if (maxValue < value) {
                maxValue = value;
                selectedDirection = direction;
            }
        }

        if (selectedDirection != null) {
            Polygon selectedPolygon = selectedDirection.getPolygon();
            targetPoint = new math.geom2d.Point2D(selectedPolygon.getBounds2D()
                    .getCenterX(), selectedPolygon.getBounds2D().getCenterY());
            double degree = 0;
            while (degree < 360) {
                if (isIgnoredDirection(selectedDirection.getSide(),
                        (int) degree, !IAmOddIndex, ignoredSides)) {
                    targetPoint = targetPoint.rotate(fireClusterCenter,
                            Math.toRadians(IAmOddIndex ? theta : -theta));
                    degree += theta;
                    continue;
                }
                if (fireCluster.hasBuildingInDirection(new Point(
                                (int) targetPoint.getX(), (int) targetPoint.getY()),
                        true, false)) {
                    break;
                }
                targetPoint = targetPoint.rotate(fireClusterCenter,
                        Math.toRadians(IAmOddIndex ? theta : -theta));
                degree += theta;
            }
            // debounceDirectionSwitch(targetPoint);
        } else {
            // world.printData("I can't find the best direction. but goto(findFarthestPointOfMap)");
            findFarthestPointOfMap(fireCluster);
        }

        return new Point((int) targetPoint.getX(), (int) targetPoint.getY());
    }

    private boolean isIgnoredDirection(DirectionSide side, int degree,
                                       boolean clockwise, Set<DirectionSide> ignoredSides) {
        int ord = side.ordinal();
        if (!clockwise) {
            degree = 360 - degree;
        }

        int nowOrd;

        switch (degree) {
            case 0:
                nowOrd = ord;
                break;
            case 20:
                nowOrd = ord;
                break;
            case 40:
                nowOrd = 1 + ord;
                break;
            case 60:
                nowOrd = 1 + ord;
                break;
            case 80:
                nowOrd = 2 + ord;
                break;
            case 100:
                nowOrd = 2 + ord;
                break;
            case 120:
                nowOrd = 3 + ord;
                break;
            case 140:
                nowOrd = 3 + ord;
                break;
            case 160:
                nowOrd = 4 + ord;
                break;
            case 180:
                nowOrd = 4 + ord;
                break;
            case 200:
                nowOrd = 4 + ord;
                break;
            case 220:
                nowOrd = 5 + ord;
                break;
            case 240:
                nowOrd = 5 + ord;
                break;
            case 260:
                nowOrd = 6 + ord;
                break;
            case 280:
                nowOrd = 6 + ord;
                break;
            case 300:
                nowOrd = 7 + ord;
                break;
            case 320:
                nowOrd = 7 + ord;
                break;
            case 340:
                nowOrd = ord;
                break;
            default:
                nowOrd = ord;
        }
        nowOrd = nowOrd % DirectionSide.values().length;
        DirectionSide nowDir = DirectionSide.values()[nowOrd];

        return ignoredSides.contains(nowDir);
    }
}
