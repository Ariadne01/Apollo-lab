package ApolloRescue.module.algorithm.clustering;

import ApolloRescue.module.algorithm.convexhull.CompositeConvexHull;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.entities.BuildingModel;
import ApolloRescue.module.universal.Util;
import javolution.util.FastSet;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

public class ApolloFireZone extends Cluster {

    protected ApolloWorld world;
    // protected EntityID ID; // self id
    /** fire size according to burning building size*/
    protected FireZoneSize fireZoneSize = FireZoneSize.UnKnown;
//	protected List<BuildingModel> burningBuildings; // 加入的燃烧节点，可能有将要燃烧的建筑

    /** outer & fieryness 1,2,3*/
    protected List<BuildingModel> dangerBuildings;

    /** outer buildings*/
    protected Set<BuildingModel> outerBuildings;
//	protected List<BuildingModel> valuableBuildings; // 高价值的建筑 外围+危险
//	protected List<BuildingModel> allBuildings; // all buildings in Polygon 未用
//												// TODO

    protected final int SMALL_FIREZONE_SIZE = 4;
    protected final int MIDIUM_FIREZONE_SIZE = 15;
    protected boolean isDying;
    protected boolean controllable;

    int idCounter;
    protected static final int CLUSTER_ENERGY_COEFFICIENT = 50;// 70;
    protected static final int CLUSTER_ENERGY_SECOND_COEFFICIENT = 20;
    // use for Direction
    protected List<BuildingModel> highValueBuildings = new ArrayList<BuildingModel>();
    protected List<BuildingModel> buildings = new ArrayList<BuildingModel>(); // just fieryness 1,2,3
    public enum Condition {smallControllable, largeControllable, edgeControllable, unControllable}
    private Condition condition;

    /**
     * 火区的评估值
     */
    public double estimateValue = 0;

    public ApolloFireZone(ApolloWorld world, List<BuildingModel> buildings) {
        this.world = world;
//		this.burningBuildings = buildings;
        this.dangerBuildings = new ArrayList<>();
//		this.valuableBuildings = new ArrayList<BuildingModel>();
        this.outerBuildings = new HashSet<>();
//		this.allBuildings = new ArrayList<BuildingModel>();
//		this.isDying = true;
//		this.controllable = true;
        idCounter = 0;
        this.buildings = new ArrayList<BuildingModel>();
    }

    public ApolloFireZone(ApolloWorld world) {
        super();
        this.world = world;
//		this.burningBuildings = new ArrayList<BuildingModel>();
        this.dangerBuildings = new ArrayList<>();
//		this.valuableBuildings = new ArrayList<BuildingModel>();
        this.outerBuildings = new HashSet<>();
//		this.allBuildings = new ArrayList<BuildingModel>();
//		this.isDying = true;
//		this.controllable = true;
        idCounter = 0;
        this.buildings = new ArrayList<BuildingModel>();
    }

    private void setOuterDangerBuildings() {
        if(dangerBuildings!=null){
            dangerBuildings.clear();
            dangerBuildings.addAll(buildings);
            for(StandardEntity entity : getBorderEntities()) {
                BuildingModel b = world.getBuildingModel(entity.getID());
                if(b.isDanger() && !dangerBuildings.contains(b) && b.canFire()) {
                    dangerBuildings.add(b);
                }
            }

        }else{
            dangerBuildings = new ArrayList<BuildingModel>();
            dangerBuildings.clear();
            dangerBuildings.addAll(buildings);
            for(StandardEntity entity : getBorderEntities()) {
                BuildingModel b = world.getBuildingModel(entity.getID());
                if(b.isDanger() && !dangerBuildings.contains(b) && b.canFire()) {
                    dangerBuildings.add(b);
                }
            }
        }


    }

    private void setOuterBuildings() {
        if(outerBuildings == null){
            System.out.println("the outerbuildings is null!!!");
            outerBuildings = new HashSet<BuildingModel>();
        }
        outerBuildings.clear();
        for(StandardEntity entity : getBorderEntities()) {
            BuildingModel b = world.getBuildingModel(entity.getID());
            outerBuildings.add(b);

        }

    }

//	/**
//	 * 用于每周期更新
//	 */
//	public void update(ApolloWorld world) {
//		// dying & controllable
//		setFireZoneCondition();
//		setBuildingsFizeZoneID();
//		// 删除没有价值的燃烧建筑
//		removeUnValuableBuildings();
//		// 加入近距离的加油站
//		for (StandardEntity entity : world.getGasStations()) {
//			BuildingModel gas = world.getBuildingModel(entity.getID());
//			if (gas != null && distance(gas.x(), gas.y()) < 10000) {
//				// XXX
//				ConsoleDebug.printFB("近距离加油站，水量：" + gas.getWaterQuantity(),
//						OutputType.ConsoleErr);
//				if (gas.getWaterQuantity() <= 0
//						&& !burningBuildings.contains(gas)) {
//					burningBuildings.add(gas);
//				}
//			}
//		}
//		// 更新高价值建筑
//		setValuableBuildings();
//	}

//	public List<BuildingModel> getBurningBuildings() {
//		return burningBuildings;
//	}
//
//	public void setBurningBuilding(List<BuildingModel> buildings) {
//		this.burningBuildings = buildings;
//	}
//
//	public boolean isEmpty() {
//		if (burningBuildings.size() == 0) {
//			return true;
//		} else
//			return false;
//	}
//
    /**
     * Outer & (danger or burning)
     *
     * @return
     */
    public List<BuildingModel> getDangerBuildings() {
        return dangerBuildings;
    }

    //	/**
//	 * fieryness = 1, 2 use to mark the fire zone spread ability
//	 *
//	 * @param dangerBuildings
//	 */
//	@Deprecated
//	public void setDangerBuildings(List<BuildingModel> dangerBuildings) {
//		this.dangerBuildings = dangerBuildings;
//	}
//
//	public String toString() {
//		return "partition( " + "ID " + id + "-- burningBuildings "
//				+ burningBuildings + ")";
//	}
//
    /**
     * Polygon outer building
     * by Polygon scale
     * @return
     */
    public Set<BuildingModel> getOuterBuildings() {
        return outerBuildings;
    }

//	public void setOuterBuildings(List<BuildingModel> outerBuildings) {
//		this.outerBuildings = outerBuildings;
//	}

    /**
     * up to outer burningbuilding's size < 4 small mediun < 15
     *
     * @return
     */
    public FireZoneSize getFireZoneSize() {
        if (getBuildings().size() < SMALL_FIREZONE_SIZE) {
            fireZoneSize = FireZoneSize.Small;
        } else if (getBuildings().size() < MIDIUM_FIREZONE_SIZE) {
            fireZoneSize = FireZoneSize.Medium;
        } else {
            fireZoneSize = FireZoneSize.Large;
        }
        return fireZoneSize;
    }
//
//	public void setFireZoneSize(FireZoneSize fireZoneSize) {
//		this.fireZoneSize = fireZoneSize;
//	}

    /**
     * 获取与凸包外围的最近距离
     *
     * @param x
     * @param y
     * @return
     */
    public int distance(int x, int y) {
        int dis = Integer.MAX_VALUE;
        int dis2 = 0;


        for (StandardEntity out : getBorderEntities()) {
            dis2 = (int) Util.distance(x, y, world.getWorldInfo().getLocation(out).first(), world.getWorldInfo().getLocation(out).second());
            if (dis2 < dis) {
                dis = dis2;
            }
        }
        return dis;
    }

//	/**
//	 * 当凸包中燃烧建筑物不少于4个时候，删除凸包中燃烧建筑评估燃烧度为3的建筑, 建筑物总面积 < 600
//	 */
//	private void removeUnValuableBuildings() {
//		if (getBurningBuildings().size() < 5) {
//			return;
//		}
//		List<BuildingModel> removeList = new ArrayList<BuildingModel>();
//		for (BuildingModel b : getBurningBuildings()) {
//			int totalArea = 0;
//			if (b.getSelfBuilding().isTotalAreaDefined()) {
//				totalArea = b.getSelfBuilding().getTotalArea();
//			}
//			if (b.getEstimatedFieryness() == 3 && totalArea < 600) {
//				removeList.add(b);
//			}
//		}
//		getBurningBuildings().removeAll(removeList);
//	}
//
//	/**
//	 * 火区中含单个建筑groundArea < 200，或者火区着火建筑面积 < 200
//	 *
//	 * @return
//	 */
//	public boolean isLessDanger() {
//		if (burningBuildings.size() == 1) {
//			Building b = burningBuildings.get(0).getSelfBuilding();
//			if (b.isGroundAreaDefined() && b.getGroundArea() < 200) {
//				return true;
//			}
//		}
//		int totalGroundArea = 0;
//		for (BuildingModel b : burningBuildings) {
//			if (b.getSelfBuilding().isGroundAreaDefined()) {
//				totalGroundArea += b.getSelfBuilding().getGroundArea();
//			}
//		}
//		if (totalGroundArea < 200) {
//			return true;
//		}
//		return false;
//	}
//
//	public List<BuildingModel> getValuableBuildings() {
//		return valuableBuildings;
//	}
//
//	private void setValuableBuildings() {
//		valuableBuildings.clear();
//		for (BuildingModel b : getOuterBuildings()) {
//			if (b.canFire()) {
//				valuableBuildings.add(b);
//			}
//		}
//		// valuableBuildings.addAll(getOuterBuildings());
//		for (BuildingModel b : getBurningBuildings()) {
//			if (b.isDanger() && b.canFire()) {
//				if (!valuableBuildings.contains(b)) {
//					valuableBuildings.add(b);
//				}
//			}
//		}
//	}


    // -------------------------------------------------------------------------------------------------

    public void setControllable(double clusterEnergy) {
        double fireBrigadeEnergy = world.getWorldInfo().getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE).size() * world.getScenarioInfo().getFireExtinguishMaxSum();
        boolean controllable = (clusterEnergy / CLUSTER_ENERGY_COEFFICIENT) < fireBrigadeEnergy;
        if (!isControllable() && controllable) {
            controllable = (clusterEnergy / CLUSTER_ENERGY_SECOND_COEFFICIENT) < fireBrigadeEnergy;
        }
        setControllable(controllable);
//        if (world.getSelf().getID().getValue() == 538173677) {
//            world.printData("cluster is controllable "+ controllable+".  clusterEnergy = " + (clusterEnergy)+"  val = " + (clusterEnergy / 70) + "  fireBrigadeEnergy = " + fireBrigadeEnergy);
//        }
    }

    /**
     * any building with temperature bigger than zero should be considered in convex hull
     */
    @Override
    public void updateConvexHull() {
        BuildingModel buildingModel;
        convexHull = new CompositeConvexHull();
        convexObject.setConvexPolygon(convexHull.getConvexPolygon());
        if (convexObject != null && convexObject.getConvexPolygon() != null && convexObject.getConvexPolygon().npoints != 0) {
            for (int i = 0; i < convexObject.getConvexPolygon().npoints; i++) {
                convexHull.addPoint(convexObject.getConvexPolygon().xpoints[i],
                        convexObject.getConvexPolygon().ypoints[i]);
            }
        }

        for (StandardEntity entity : entities) {
            if (entity instanceof Building) {
                buildingModel = world.getBuildingModel(entity.getID());

                /*if (isDying && building.getEstimatedFieryness() > 0 && building.getEstimatedFieryness() < 3) {
                    setDying(false);
                }*/

                if (isEdge && !world.getMapSideBuildings().contains(buildingModel.getID())) {
                    setEdge(false);
                }

                //try {
//                    if (membershipChecker.checkMembership(building)) {
//                        convexHull.addPoint(building.getSelfBuilding().getX(),
//                                building.getSelfBuilding().getY());
                for (int i = 0; i < buildingModel.getSelfBuilding().getApexList().length; i += 2) {
                    convexHull.addPoint(buildingModel.getSelfBuilding().getApexList()[i], buildingModel.getSelfBuilding().getApexList()[i + 1]);
                }

//                    }
                /*} catch (Exception e) {
                    e.printStackTrace();
                }*/
            }
        }


//        sizeOfBuildings();  mostafas commented this
        //if (world.getTime() % 5 == 0) {
        List<BuildingModel> dangerBuildings = new ArrayList<BuildingModel>();
        double clusterEnergy = 0;
        for (StandardEntity entity : getEntities()) {
            BuildingModel burningBuilding = world.getBuildingModel(entity.getID());
            if (burningBuilding.getEstimatedFieryness() == 1) {
                dangerBuildings.add(burningBuilding);
                clusterEnergy += burningBuilding.getEnergy();
            }
            if (burningBuilding.getEstimatedFieryness() == 2) {
                dangerBuildings.add(burningBuilding);
                clusterEnergy += burningBuilding.getEnergy();
            }
            if (burningBuilding.getEstimatedFieryness() == 3 && burningBuilding.getEstimatedTemperature() > 150) {
                dangerBuildings.add(burningBuilding);
            }
        }

        setDying(dangerBuildings.isEmpty());
        setControllable(clusterEnergy);
        buildings = dangerBuildings;
        //}
        convexObject.setConvexPolygon(convexHull.getConvexPolygon());
        setBorderEntities();
        setCentre();
//        sizeOfBuildings();
//        setTotalDistance();

        setOuterDangerBuildings();  //XXX test
        setOuterBuildings();
    }

    public int calcNeededWaterToExtinguish() {
        int neededWater = 0;
        for (StandardEntity entity : getBorderEntities()) {
            neededWater += ExtinguishHelper.waterNeededToExtinguish(world.getBuildingModel(entity.getID()));
        }
        return neededWater;
    }

    public void updateCondition() {
        double fireClusterArea = getBoundingBoxArea();
        double worldArea = (world.getMapHeight() / 1000) * (world.getMapWidth() / 1000);
        double percent = fireClusterArea / worldArea;
        if (percent > 0.80) {
            setCondition(Condition.unControllable);
            return;
        }
        if (percent > 0.15) {
            setCondition(Condition.edgeControllable);
            return;
        }
        if (percent > 0.04) {
            setCondition(Condition.largeControllable);
            return;
        }
        if (percent >= 0.00) {
            setCondition(Condition.smallControllable);
        }
    }

    @Override
    public void updateValue() {
        //updateCondition();
    }

    private void setBorderEntities() {
        Building building;
        borderEntities.clear();

        if (convexObject.getConvexPolygon().npoints == 0) // I don't know why this happens, this should be checked TODO check this if something comes wrong here
            return;

        smallBorderPolygon = scalePolygon(convexObject.getConvexPolygon(), 0.9);
        bigBorderPolygon = scalePolygon(convexObject.getConvexPolygon(), 1.1);

        for (StandardEntity entity : entities) {

            if (entity instanceof Refuge) {
                continue;
            }
            if (!(entity instanceof Building)) {
                continue;
            }
            building = (Building) entity;
            int vertexes[] = building.getApexList();
            for (int i = 0; i < vertexes.length; i += 2) {

                if ((bigBorderPolygon.contains(vertexes[i], vertexes[i + 1])) && !(smallBorderPolygon.contains(vertexes[i], vertexes[i + 1]))) {
                    borderEntities.add(building);
                    break;
                }
            }
        }
    }

    /**
     * just fieryness 1,2,3
     * @return
     */
    public List<BuildingModel> getBuildings() {
        return buildings;
    }

    private void setCentre() {
        int sumX = 0;
        int sumY = 0;
        for (int x : convexObject.getConvexPolygon().xpoints) {
            sumX += x;
        }

        for (int y : convexObject.getConvexPolygon().ypoints) {
            sumY += y;
        }

        if (convexObject.getConvexPolygon().npoints > 0) {
            center = new Point(sumX / convexObject.getConvexPolygon().npoints, sumY / convexObject.getConvexPolygon().npoints);
        } else {
            center = new Point(0, 0);
        }

    }

    public boolean isExpandableToCenterOfMap() {
        if (isEdge()) {
            Point mapCenter = new Point(world.getMapWidth() >> 1, world.getMapHeight() >> 1);
            double distanceFireClusterToCenter = Util.distance(center, mapCenter);
            for (EntityID entityID : world.getMapSideBuildings()) {
                Building building = (Building) world.getEntity(entityID);
                double distanceBuildingToCenter = Util.distance(world.getWorldInfo().getLocation(building), mapCenter);
                if (distanceBuildingToCenter <= distanceFireClusterToCenter) {
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
    }

    //edited by mostafaS
    public boolean hasBuildingInDirection(Point targetPoint, boolean limitDirection, boolean useAllFieryness) {

        highValueBuildings = new ArrayList<BuildingModel>();
//        List<StandardEntity> borderDirectionBuildings = new ArrayList<StandardEntity>();
        setTriangle(targetPoint, limitDirection);
        Set<BuildingModel> entitySet = new FastSet<BuildingModel>();
        entitySet.addAll(buildings);
        entitySet.removeAll(ignoredBorderEntities); //todo: check this ignoredBorderEntities mostafas
        if (isDying() || getConvexHullObject() == null) {
            return !highValueBuildings.isEmpty();
        }
        if (getConvexHullObject() == null || convexObject.CONVEX_POINT == null || convexObject.CENTER_POINT == null || convexObject.getTriangle() == null) {
            return !highValueBuildings.isEmpty();
        }

        Polygon triangle = convexObject.getTriangle();

        for (BuildingModel building : entitySet) {
            if (!isCandidate(building)) {
                if (!useAllFieryness || !isOldCandidate(building)) {
                    continue;
                }
            }

            int vertexes[] = building.getSelfBuilding().getApexList();
            for (int i = 0; i < vertexes.length; i += 2) {
                if (triangle.contains(vertexes[i], vertexes[i + 1])) {
                    highValueBuildings.add(building);
//                    borderDirectionBuildings.add(building.getSelfBuilding());
                    break;
                }
            }
        }
//        FIXME viewer
        return !highValueBuildings.isEmpty();
    }

    public List<BuildingModel> getBuildingsInDirection() {
        return highValueBuildings;
    }

    private boolean isOldCandidate(BuildingModel b) {
        return b.getEstimatedFieryness() == 3;
    }

    private boolean isCandidate(BuildingModel b) {
        return (b.getEstimatedFieryness() == 1 || b.getEstimatedFieryness() == 2);
//        return !(b.getEstimatedFieryness() == 2 || b.getEstimatedFieryness() == 3 || b.getEstimatedFieryness() == 8);
    }

    private void setTriangle(Point targetPoint, boolean limitDirection) {
        Polygon convexPoly = convexObject.getConvexPolygon();
        double radiusLength;
        if (limitDirection) {
            radiusLength = Math.max(world.getBounds().getHeight(), world.getBounds().getWidth()) / 2;
//            radiusLength = Util.distance(convexHull.getConvexPolygon(), new rescuecore2.misc.geometry.Point2D(targetPoint.getX(), targetPoint.getY()));
        } else {
            radiusLength = Math.sqrt(Math.pow(convexPoly.getBounds().getHeight(), 2) + Math.pow(convexPoly.getBounds().getWidth(), 2));
        }

        Point convexPoint = new Point((int) convexPoly.getBounds().getCenterX(), (int) convexPoly.getBounds().getCenterY());
        targetPoint = getFinalDirectionPoints(targetPoint, convexPoint, Math.min(convexPoly.getBounds2D().getWidth(), convexPoly.getBounds2D().getHeight()) * 5);
        Point[] points = getPerpendicularPoints(targetPoint, convexPoint, radiusLength);
        Point point1 = points[0];
        Point point2 = points[1];

        convexObject.CENTER_POINT = targetPoint;
//        FIXME viewer
        convexObject.FIRST_POINT = point1;
        convexObject.SECOND_POINT = point2;
        convexObject.CONVEX_POINT = convexPoint;
        Polygon trianglePoly = new Polygon();
        trianglePoly.addPoint(point1.x, point1.y);
        trianglePoly.addPoint(convexPoint.x, convexPoint.y);
        trianglePoly.addPoint(point2.x, point2.y);

        convexObject.setTrianglePolygon(trianglePoly);
        {//get other side of triangle
            double distance;
            if (limitDirection) {
                distance = Math.max(world.getBounds().getHeight(), world.getBounds().getWidth()) / 2;
//                distance = Util.distance(convexHull.getConvexPolygon(), new rescuecore2.misc.geometry.Point2D(targetPoint.getX(), targetPoint.getY()));
            } else {
                distance = point1.distance(point2) / 3;
            }
            points = getPerpendicularPoints(point2, point1, distance);
            if (convexPoint.distance(points[0]) >= convexPoint.distance(points[1])) {
                trianglePoly.addPoint(points[0].x, points[0].y);
                convexObject.OTHER_POINT2 = new Point(points[0].x, points[0].y);
            } else {
                trianglePoly.addPoint(points[1].x, points[1].y);
                convexObject.OTHER_POINT2 = new Point(points[1].x, points[1].y);
            }

            points = getPerpendicularPoints(point1, point2, distance);
            if (convexPoint.distance(points[0]) >= convexPoint.distance(points[1])) {
                trianglePoly.addPoint(points[0].x, points[0].y);
                convexObject.OTHER_POINT1 = new Point(points[0].x, points[0].y);
            } else {
                trianglePoly.addPoint(points[1].x, points[1].y);
                convexObject.OTHER_POINT1 = new Point(points[1].x, points[1].y);
            }
        }
    }

    private static Point[] getPerpendicularPoints(Point2D point1, Point2D point2, double radiusLength) {
        double x1 = point1.getX();
        double y1 = point1.getY();
        double x2 = point2.getX();
        double y2 = point2.getY();

        double m1 = (y1 - y2) / (x1 - x2);
        double m2 = (-1 / m1);
        double a = Math.pow(m2, 2) + 1;
        double b = (-2 * x1) - (2 * Math.pow(m2, 2) * x1);
        double c = (Math.pow(x1, 2) * (Math.pow(m2, 2) + 1)) - Math.pow(radiusLength, 2);

        double x3 = ((-1 * b) + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
        double x4 = ((-1 * b) - Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
        double y3 = (m2 * x3) - (m2 * x1) + y1;
        double y4 = (m2 * x4) - (m2 * x1) + y1;

        Point perpendicular1 = new Point((int) x3, (int) y3);
        Point perpendicular2 = new Point((int) x4, (int) y4);
        return new Point[]{perpendicular1, perpendicular2};
    }

    private static Point getFinalDirectionPoints(Point2D point1, Point2D point2, double radiusLength) {
        double x1 = point1.getX();
        double y1 = point1.getY();
        double x2 = point2.getX();
        double y2 = point2.getY();

//        double m1 = (y1 - y2) / (x1 - x2);
//        double a = Math.pow(m1, 2) + 1;
//        double b = (-2 * x1) - (2 * Math.pow(m1, 2) * x1);
//        double c = (Math.pow(x1, 2) * (Math.pow(m1, 2) + 1)) - Math.pow(radiusLength, 2);
//
//        double x3 = ((-1 * b) + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a);
//        double y3 = (m1 * x3) - (m1 * x1) + y1;

        double d = Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
        double r = radiusLength / d;

        double x3 = r * x1 + (1 - r) * x2;
        double y3 = r * y1 + (1 - r) * y2;

        Point perpendicular = new Point((int) x3, (int) y3);
        return perpendicular;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

}
