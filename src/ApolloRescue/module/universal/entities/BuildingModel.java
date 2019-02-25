package ApolloRescue.module.universal.entities;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;


import ApolloRescue.module.complex.component.RoadInfoComponent;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.tools.LimitedLineOfSightPerception;
import ApolloRescue.module.universal.tools.Planarizable;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import javolution.util.FastSet;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;


import ApolloRescue.ApolloConstants;
import ApolloRescue.module.universal.tools.RayModel;

public class BuildingModel implements Planarizable {
    public static Map<EntityID, Map<EntityID, BuildingModel>> VIEWER_BUILDINGS_MAP = new HashMap<EntityID, Map<EntityID, BuildingModel>>();

    /** Building priority*/
    public double BUILDING_VALUE;
    public int priority;
    private boolean probablyOnFire = false;
    private Building selfBuilding;
    private Point selfPoint;
    private boolean isMapSide = false;
    private List<BuildingModel> connectedBuilding;
    private List<Float> connectedValues;
    private Hashtable connectedBuildingsTable;
    private List<EntityID> neighbourIdBuildings;
    private List<EntityID> neighbourFireBuildings;
    private Collection<Wall> walls;
    private double totalWallArea;
    private ArrayList<Wall> allWalls;
    private List<Entrance> entrances;
    private Integer zoneId;
    private double cellCover;
    private boolean visited = false;
    private boolean shouldCheckInside;
    private Set<Human> humans;
    private Set<Civilian> civilians;
    private boolean isReachable;
    private boolean visitable;
    private ApolloWorld world;
    private int lastUpdateTime;
    private Set<EntityID> civilianPossibly;
    private double civilianPossibleValue;
    private List<Polygon> centerVisitShapes;
    private Map<EntityID, List<Polygon>> centerVisitRoadShapes;
    private Map<EntityID, List<Point>> centerVisitRoadPoints;
    private Map<Edge, Pair<Point2D, Point2D>> edgeVisibleCenterPoints;
    private boolean sensed;
    private int sensedTime = -1;
    private Set<EntityID> visibleFrom;
    private List<EntityID> observableAreas;
    private List<RayModel> lineOfSight;
    private double advantageRatio;// todo @Mostafa: Describe this
    private Set<EntityID> extinguishableFromAreas;
    private List<BuildingModel> buildingsInExtinguishRange;
    private Map<EntityID, Edge> entranceEdgeMap;
    private int ignitionTime = -1;
    // private EntityID hullId; // 凸包ID
    private double borderValue; // 凸包的边界距离权值
    private int mapCenterDistance; // 建筑距离Map中心的距离
    private boolean ignition;
    private double hitRate = 0; // fire select use
    protected int totalHits;
    protected int totalRays;
    private LimitedLineOfSightPerception lineOfSightPerception;

    protected WorldInfo worldInfo;
    protected AgentInfo agentInfo;
    protected ScenarioInfo scenarioInfo;

    public BuildingModel(StandardEntity entity, ApolloWorld world, WorldInfo worldInfo, AgentInfo agentInfo) {
        this.agentInfo = agentInfo;
        this.scenarioInfo = scenarioInfo;
        this.worldInfo = worldInfo;
        setVisibleFrom(new FastSet<EntityID>());
        setObservableAreas(new ArrayList<EntityID>());
        selfBuilding = (Building) entity;
        connectedBuildingsTable = new Hashtable(30);
        neighbourIdBuildings = new ArrayList<EntityID>();
        neighbourFireBuildings = new ArrayList<EntityID>();
        connectedBuilding = new ArrayList<BuildingModel>();
        entrances = new ArrayList<Entrance>();
        humans = new FastSet<Human>();
        civilians = new FastSet<Civilian>();
        this.isReachable = true;
        this.visitable = true;
        this.world = world;
        lastUpdateTime = 0;
        civilianPossibly = new HashSet<EntityID>();
        centerVisitShapes = new ArrayList<Polygon>();
        centerVisitRoadShapes = new HashMap<EntityID, List<Polygon>>();
        centerVisitRoadPoints = new HashMap<EntityID, List<Point>>();
        edgeVisibleCenterPoints = new HashMap<Edge, Pair<Point2D, Point2D>>();
        entranceEdgeMap = new HashMap<EntityID, Edge>();
        initWalls(world);
        initSimulatorValues();
        setEdgeVisibleCenterPoints();
        setMapCenterDistance();
        ignition = false;
        this.lineOfSightPerception = new LimitedLineOfSightPerception(world);
        if (worldInfo.getEntity(agentInfo.getID()) instanceof FireBrigade) {
            initWalls(world);
            initSimulatorValues();
        }
    }

    public void addBuildingModelNeighbour(BuildingModel buildingModel) {
        allWalls.addAll(buildingModel.getWalls());
    }

    public void initWalls(ApolloWorld world) {

        int fx = selfBuilding.getApexList()[0];
        int fy = selfBuilding.getApexList()[1];
        int lx = fx;
        int ly = fy;
        Wall w;
        walls = new ArrayList<Wall>();
        allWalls = new ArrayList<Wall>();

        for (int n = 2; n < selfBuilding.getApexList().length; n++) {
            int tx = selfBuilding.getApexList()[n];
            int ty = selfBuilding.getApexList()[++n];
            w = new Wall(lx, ly, tx, ty, this, world.rayRate);
            if (w.validate()) {
                walls.add(w);
                totalWallArea += FLOOR_HEIGHT * 1000 * w.length;
            }
            lx = tx;
            ly = ty;
        }

        w = new Wall(lx, ly, fx, fy, this, world.rayRate);
        walls.add(w);
        totalWallArea = totalWallArea / 1000000d;

    }

    public void initWallValues(ApolloWorld world) {

        for (Wall wall : walls) {
            wall.findHits(world, this);
            totalHits += wall.hits;
            totalRays += wall.rays;
        }
        // int c = 0;
        connectedBuilding = new ArrayList<BuildingModel>();
        connectedValues = new ArrayList<Float>();
        float base = totalRays;

        for (Enumeration e = connectedBuildingsTable.keys(); e
                .hasMoreElements(); /* c++ */) {
            BuildingModel b = (BuildingModel) e.nextElement();
            Integer value = (Integer) connectedBuildingsTable.get(b);
            connectedBuilding.add(b);
            connectedValues.add(value.floatValue() / base);
            // buildingNeighbours.add(b.getSelfBuilding().getID());
        }
        hitRate = totalHits * 1.0 / totalRays;

    }

    public List<Entrance> getEntrances() {
        return entrances;
    }

    public void addEntrance(Entrance entrance) {
        this.entrances.add(entrance);
    }

    public void setConnectedBuilding(List<BuildingModel> connectedBuilding) {
        this.connectedBuilding = connectedBuilding;
    }

    public List<BuildingModel> getConnectedBuilding() {
        return connectedBuilding;
    }

    public void setConnectedValues(List<Float> connectedValues) {
        this.connectedValues = connectedValues;
    }

    public List<Float> getConnectedValues() {
        return connectedValues;
    }

    public Collection<Wall> getWalls() {
        return walls;
    }

    public Hashtable getConnectedBuildingsTable() {
        return connectedBuildingsTable;
    }

    public ArrayList<Wall> getAllWalls() {
        return allWalls;
    }

    public void setNeighbourIdBuildings(List<EntityID> neighbourIdBuildings) {
        this.neighbourIdBuildings = neighbourIdBuildings;
    }

    public void setNeighbourFireBuildings(List<EntityID> neighbourFireBuildings) {
        this.neighbourFireBuildings = neighbourFireBuildings;
    }

    public List<EntityID> getNeighbourIdBuildings() {
        return neighbourIdBuildings;
    }

    public List<BuildingModel> getNeighborBuildings1() {
        List<BuildingModel> buildings = new ArrayList<BuildingModel>();
        Collection<StandardEntity> ranges = world.getObjectsInRange(
                this.getID(), 30000);
        if (ranges != null && !ranges.isEmpty()) {
            for (StandardEntity e : ranges) {
                if (e instanceof Building) {
                    BuildingModel b = world.getBuildingModel(this.getID());
                    if (b != null) {
                        buildings.add(b);
                    }
                }
            }
        }

        return buildings;
    }

    public List<EntityID> getNeighbourFireBuildings() {
        return neighbourFireBuildings;
    }

    public Integer getZoneId() {
        return zoneId;
    }

    public void setZoneId(Integer zoneId) {
        this.zoneId = zoneId;
    }

    public double getCellCover() {
        return cellCover;
    }

    public void setCellCover(double cellCover) {
        this.cellCover = cellCover;
    }

    public boolean shouldCheckInside() {
        return shouldCheckInside;
    }

    public void setShouldCheckInside(boolean shouldCheckInside) {
        this.shouldCheckInside = shouldCheckInside;
    }

    /**
     * 0 < fieryness < 4
     */
    public boolean isBurning() {
        return getEstimatedFieryness() > 0 && getEstimatedFieryness() < 4;
    }

    /**
     * Fieryness = 0/4/5 & 30 < Temp < 47 TODO
     *
     * @return
     */
    public boolean isDanger() {
        int fieryness = getEstimatedFieryness();
        if ((fieryness == 0 || fieryness == 4 || fieryness == 5)
                && getEstimatedTemperature() > 30
                && getEstimatedTemperature() < 47) {
            return true;
        }
        return false;
    }

    public double getBuildingRadiation() {
        double value = 0;
        // double totalArea = 0;
        BuildingModel b;

        for (int c = 0; c < connectedValues.size(); c++) {
            b = connectedBuilding.get(c);
            if (!b.isBurning()) {
                value += (connectedValues.get(c));

            }
        }
        return value * getEstimatedTemperature() / 1000;
    }

    public double getNeighbourRadiation() {
        double value = 0;
        // double totalArea = 0;
        BuildingModel b;
        int index;

        for (BuildingModel building : connectedBuilding) {
            if (building.isBurning()) {
                index = building.getConnectedBuilding().indexOf(this);
                if (index >= 0) {
                    value += (building.getConnectedValues().get(index) * building
                            .getEstimatedTemperature());
                }
            }
        }

        return value / 10000;
    }

    public double getBuildingAreaTempValue() {
        return Util.gauss2mf(selfBuilding.getTotalArea()
                * getEstimatedTemperature(), 10000, 30000, 20000, 40000);
    }

    public boolean isAllEntrancesOpen(ApolloWorld world) {
        RoadInfoComponent roadInfoComponent = world
                .getComponent(RoadInfoComponent.class);
        for (Entrance en : entrances) {
            if (roadInfoComponent.isOpenOrNotSeen(this.getID(), en
                    .getNeighbour().getID()))
                return false;
        }
        return true;
    }

    public boolean isOneEntranceOpen(ApolloWorld world) {
        RoadInfoComponent roadInfoComponent = world
                .getComponent(RoadInfoComponent.class);
        // Building building = world.getEntity(getID(), Building.class);
        // for (EntityID nID : building.getNeighbours()) {
        // StandardEntity entity = world.getEntity(nID);
        // if (entity instanceof Road) {
        // if (roadComponent.isOpenOrNotSeen(this.getID(), entity.getID())) {
        // return true;
        // }
        // } else {
        // return true;
        // }
        // }
        for (Entrance entrance : getEntrances()) {
            Road road = world.getEntity(entrance.getID(), Road.class);
            for (Building building : entrance.getBuildings()) {
                if (road.getNeighbours().contains(building.getID())) {
                    if (roadInfoComponent.isOpenOrNotSeen(building.getID(),
                            entrance.getID())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void addApolloBuildingNeighbour(BuildingModel apolloBuilding) {
        allWalls.addAll(apolloBuilding.getWalls());
//        connectedBuilding.add(mrlBuilding);
    }

    public boolean isOneEntranceSurlyOpen(ApolloWorld world) {
        RoadInfoComponent roadInfoComponent = world
                .getComponent(RoadInfoComponent.class);
        for (Entrance en : entrances) {
            if (!roadInfoComponent.isSeenAndBlocked(this.getID(), en
                    .getNeighbour().getID()))
                return true;
        }
        return false;
    }

    public boolean isAllEntrancesBlocked(ApolloWorld world) {
        return !isOneEntranceOpen(world);
    }

    // /////////////////////////////////FIRE SIMULATOR
    // PROPERTIES////////////////////////////////////
    static final int FLOOR_HEIGHT = 3;
    static float RADIATION_COEFFICIENT = 0.011f;
    static final double STEFAN_BOLTZMANN_CONSTANT = 0.000000056704;

    private int startTime = -1;
    private float fuel;
    private float initFuel = -1;
    private float volume;
    private double energy;
    private float prevBurned;
    private float capacity;
    private int waterQuantity;
    private boolean wasEverWatered = false;
    private boolean flammable = true;

    public static float woodIgnition = 47;
    public static float steelIgnition = 47;
    public static float concreteIgnition = 47;
    public static float woodCapacity = 1.1f;
    public static float steelCapacity = 1.0f;
    public static float concreteCapacity = 1.5f;
    public static float woodEnergy = 2400;
    public static float steelEnergy = 800;
    public static float concreteEnergy = 350;

    public void initSimulatorValues() {
        volume = selfBuilding.getGroundArea() * selfBuilding.getFloors()
                * FLOOR_HEIGHT;
        fuel = getInitialFuel();
        capacity = (volume * getThermoCapacity());
        energy = 0;
        initFuel = -1;
        prevBurned = 0;
    }

    public float getInitialFuel() {
        if (initFuel < 0) {
            initFuel = (getFuelDensity() * volume);
        }
        return initFuel;
    }

    private float getThermoCapacity() {
        switch (selfBuilding.getBuildingCode()) {
            case 0:
                return woodCapacity;
            case 1:
                return steelCapacity;
            default:
                return concreteCapacity;
        }
    }

    private float getFuelDensity() {
        switch (selfBuilding.getBuildingCode()) {
            case 0:
                return woodEnergy;
            case 1:
                return steelEnergy;
            default:
                return concreteEnergy;
        }
    }

    public float getIgnitionPoint() {
        switch (selfBuilding.getBuildingCode()) {
            case 0:
                return woodIgnition;
            case 1:
                return steelIgnition;
            default:
                return concreteIgnition;
        }
    }

    public float getConsume(double bRate) {
        if (fuel == 0) {
            return 0;
        }
        float tf = (float) (getEstimatedTemperature() / 1000f);
        float lf = fuel / getInitialFuel();
        float f = (float) (tf * lf * bRate);
        if (f < 0.005f)
            f = 0.005f;
        return getInitialFuel() * f;
    }

    public double getEstimatedTemperature() {
        double rv = energy / capacity;
        if (Double.isNaN(rv)) {
            new RuntimeException().printStackTrace();
        }
        if (rv == Double.NaN || rv == Double.POSITIVE_INFINITY
                || rv == Double.NEGATIVE_INFINITY)
            rv = Double.MAX_VALUE * 0.75;
        return rv;
    }

    // TODO
    public void setOriginalFieryness(int fieryness) {
        // TODO
        if (fieryness > 0)
            // inflamable=true;
            if (fieryness == 4)
                wasEverWatered = true;
        if (this.getEstimatedFieryness() != fieryness) {
            if ((fieryness) == 0 || (fieryness) == 4)
                fuel = getInitialFuel();
            else {
                if ((fieryness) == 1 || (fieryness) == 5)
                    fuel = (float) (getInitialFuel() * 0.66);
                else {
                    if ((fieryness) == 2 || (fieryness) == 6)
                        fuel = (float) (getInitialFuel() * 0.33);
                    else {
                        if ((fieryness) == 3 || (fieryness) == 7) {
                            fuel = (float) (getInitialFuel() * 0.23);
                        } else {
                            fuel = 0;
                        }
                    }
                }
            }
        }
        if (fieryness == 0 || (fieryness > 3 && fieryness < 8))
            ignition = false;

    }

    public void setOriginalTemprature(int temperature) {
        if (temperature > 0)
            setWaterQuantity(0);
        setTemprature(temperature);

        if (temperature >= getIgnitionPoint()) {
            setIgnition(true);
            // ignitionTime = -1;
        } else {
            // ignitionTime = 0;
            setIgnition(false);
        }
    }

    public void setTemprature(int temprature) {
        energy = getCapacity() * temprature;
    }

    public void setIgnition(boolean iginition) {
        this.ignition = iginition;
    }

    /**
     * 可见情况下返回真实燃烧度 XXX待测
     *
     * @return
     */
    public int getEstimatedFieryness() {// TODO
        // if(fuel==0)
        // return 8;
        // if(world.getChanges().contains(this.getID()))
        // {
        // if(this.getSelfBuilding().isFierynessDefined())
        // {
        // return this.getSelfBuilding().getFieryness();
        // }
        // }
        if (!isFlammable())
            return 0;
        if (getEstimatedTemperature() >= getIgnitionPoint()) {
            if (fuel >= getInitialFuel() * 0.66)
                return 1; // burning, slightly damaged
            if (fuel >= getInitialFuel() * 0.33)
                return 2; // burning, more damaged
            if (fuel > 0)
                return 3; // burning, severly damaged
        }
        if (fuel == getInitialFuel())
            if (wasEverWatered)
                return 4; // not burnt, but watered-damaged
            else
                return 0; // not burnt, no water damage
        if (fuel >= getInitialFuel() * 0.66)
            return 5; // extinguished, slightly damaged
        if (fuel >= getInitialFuel() * 0.33)
            return 6; // extinguished, more damaged
        if (fuel > 0)
            return 7; // extinguished, severely damaged
        return 8; // completely burnt down
    }

    public double getRadiationEnergy() {
        double t = getEstimatedTemperature() + 293; // Assume ambient
        // temperature is 293
        // Kelvin.
        double radEn = (t * t * t * t) * totalWallArea * RADIATION_COEFFICIENT
                * STEFAN_BOLTZMANN_CONSTANT;
        if (radEn == Double.NaN || radEn == Double.POSITIVE_INFINITY
                || radEn == Double.NEGATIVE_INFINITY)
            radEn = Double.MAX_VALUE * 0.75;
        if (radEn > getEnergy()) {
            radEn = getEnergy();
        }
        return radEn;
    }

    public void resetOldPassability(int resetTime) {
        if (world.getTime() - lastUpdateTime > resetTime) {
            setReachable(true);
            setVisitable(true);
        }
    }

    public int getRealFieryness() {
        return selfBuilding.getFieryness();
    }

    public int getRealTemperature() {
        return selfBuilding.getTemperature();
    }

    public Building getSelfBuilding() {
        return selfBuilding;
    }

    public float getVolume() {
        return volume;
    }

    public float getCapacity() {
        return capacity;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double v) {
        energy = v;
    }

    public float getPrevBurned() {
        return prevBurned;
    }

    public void setPrevBurned(float consumed) {
        prevBurned = consumed;
    }

    public boolean isFlammable() {
        return flammable;
    }

    public void setFlammable(boolean flammable) {
        this.flammable = flammable;
    }

    public float getFuel() {
        return fuel;
    }

    public void setFuel(float fuel) {
        this.fuel = fuel;
    }

    public int getWaterQuantity() {
        return waterQuantity;
    }

    public void setWaterQuantity(int i) {
        if (i > waterQuantity) {
            wasEverWatered = true;
        }
        waterQuantity = i;
    }

    public void increaseWaterQuantity(int i) {
        waterQuantity += i;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public void setWasEverWatered(boolean wasEverWatered) {
        this.wasEverWatered = wasEverWatered;
    }

    // --------------- TEMPORARY SIMULATION --------------------------
    private float tempFuel;
    private double tempEnergy;
    private float tempPrevBurned;
    private boolean tempFlammable = false;

    public void initTempSimulatorValues() {
        tempFuel = fuel;
        tempEnergy = energy;
        tempPrevBurned = prevBurned;
        tempFlammable = flammable;
    }

    public float getTempConsume(double bRate) {
        if (tempFuel == 0) {
            return 0;
        }
        float tf = (float) (getTempEstimatedTemperature() / 1000f);
        float lf = tempFuel / getInitialFuel();
        float f = (float) (tf * lf * bRate);
        if (f < 0.005f)
            f = 0.005f;
        return getInitialFuel() * f;
    }

    public double getTempEstimatedTemperature() {
        double rv = tempEnergy / capacity;
        if (Double.isNaN(rv)) {
            new RuntimeException().printStackTrace();
        }
        if (rv == Double.NaN || rv == Double.POSITIVE_INFINITY
                || rv == Double.NEGATIVE_INFINITY)
            rv = Double.MAX_VALUE * 0.75;
        return rv;
    }

    public int getTempEstimatedFieryness() {
        if (!isFlammable())
            return 0;
        if (getTempEstimatedTemperature() >= getIgnitionPoint()) {
            if (tempFuel >= getInitialFuel() * 0.66)
                return 1; // burning, slightly damaged
            if (tempFuel >= getInitialFuel() * 0.33)
                return 2; // burning, more damaged
            if (tempFuel > 0)
                return 3; // burning, severly damaged
        }
        if (tempFuel == getInitialFuel())
            // if (wasEverWatered)
            // return 4; // not burnt, but watered-damaged
            // else
            return 0; // not burnt, no water damage
        if (tempFuel >= getInitialFuel() * 0.66)
            return 5; // extinguished, slightly damaged
        if (tempFuel >= getInitialFuel() * 0.33)
            return 6; // extinguished, more damaged
        if (tempFuel > 0)
            return 7; // extinguished, severely damaged
        return 8; // completely burnt down
    }

    public double getTempRadiationEnergy() {
        double t = getTempEstimatedTemperature() + 293; // Assume ambient
        // temperature is 293
        // Kelvin.
        double radEn = (t * t * t * t) * totalWallArea * RADIATION_COEFFICIENT
                * STEFAN_BOLTZMANN_CONSTANT;
        if (radEn == Double.NaN || radEn == Double.POSITIVE_INFINITY
                || radEn == Double.NEGATIVE_INFINITY)
            radEn = Double.MAX_VALUE * 0.75;
        if (radEn > tempEnergy) {
            radEn = tempEnergy;
        }
        return radEn;
    }

    public Set<EntityID> getCivilianPossibly() {
        return civilianPossibly;
    }

    public void addCivilianPossibly(EntityID civID) {
        civilianPossibly.add(civID);
    }

    public double getCivilianPossibleValue() {
        return civilianPossibleValue;
    }

    public Map<Edge, Pair<Point2D, Point2D>> getEdgeVisibleCenterPoints() {
        return edgeVisibleCenterPoints;
    }

    /**
     * find two point around center that is parallel with passable edges of this
     * building with AGENT_SIZE range.
     */
    private void setEdgeVisibleCenterPoints() {
        Pair<Integer, Integer> location = world.getWorldInfo().getLocation(selfBuilding.getID());
        Point2D center = new Point2D(location.first(), location.second());
        for (Edge edge : selfBuilding.getEdges()) {
            if (edge.isPassable()) {
                Pair<Point2D, Point2D> twoPoints = Util.get2PointsAroundCenter(
                        edge, center, ApolloConstants.AGENT_SIZE);// Civilian
                // Size
                edgeVisibleCenterPoints.put(edge, twoPoints);
            }
        }
    }

    public void addCenterVisitShapes(Polygon shape) {
        centerVisitShapes.add(shape);
    }

    public void addCenterVisitRoadShapes(RoadModel roadModel, Polygon shape) {
        if (!centerVisitRoadShapes.containsKey(roadModel.getID())) {
            centerVisitRoadShapes.put(roadModel.getID(),
                    new ArrayList<Polygon>());
        }
        centerVisitRoadShapes.get(roadModel.getID()).add(shape);
        roadModel.addBuildingVisitableParts(getID(), shape);
    }

    public List<Polygon> getCenterVisitShapes() {
        return centerVisitShapes;
    }

    public Map<EntityID, List<Point>> getCenterVisitRoadPoints() {
        return centerVisitRoadPoints;
    }

    public void addCenterVisitRoadPoints(RoadModel roadModel, Point point) {
        if (!centerVisitRoadPoints.containsKey(roadModel.getID())) {
            centerVisitRoadPoints
                    .put(roadModel.getID(), new ArrayList<Point>());
        }
        centerVisitRoadPoints.get(roadModel.getID()).add(point);
    }

    public Map<EntityID, List<Polygon>> getCenterVisitRoadShapes() {
        return centerVisitRoadShapes;
    }

    public Pair<Point2D, Point2D> getCenterPointsFrom(Edge edge) {
        return edgeVisibleCenterPoints.get(edge);
    }

    public boolean isVisitable() {
        return visitable;
    }

    public void setVisitable(boolean visitable) {
        this.visitable = visitable;
    }

    public float getTempFuel() {
        return tempFuel;
    }

    public void setTempFuel(float tempFuel) {
        this.tempFuel = tempFuel;
    }

    public double getTempEnergy() {
        return tempEnergy;
    }

    public void setTempEnergy(double tempEnergy) {
        this.tempEnergy = tempEnergy;
    }

    public float getTempPrevBurned() {
        return tempPrevBurned;
    }

    public void setTempPrevBurned(float tempPrevBurned) {
        this.tempPrevBurned = tempPrevBurned;
    }

    public boolean isTempFlammable() {
        return tempFlammable;
    }

    public void setTempFlammable(boolean tempFlammable) {
        this.tempFlammable = tempFlammable;
    }

    public boolean isPutOff() {
        return getEstimatedFieryness() > 4 && getEstimatedFieryness() < 8;
    }

    /**
     * totally burnt, fieryness == 8
     *
     * @return
     */
    public boolean isBurned() {
        return getEstimatedFieryness() == 8;
    }

    public void setProbablyOnFire(boolean probablyOnFire) {
        this.probablyOnFire = probablyOnFire;
    }

    public boolean isProbablyOnFire() {
        return probablyOnFire;
    }

    @Override
    public String toString() {
        return "BuildingModel[" + selfBuilding.getID().getValue() + "]";
    }

    public EntityID getID() {
        return selfBuilding.getID();
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited() {
        this.visited = true;
    }

    public boolean isReachable() {
        return isReachable;
    }

    public void setReachable(boolean reachable) {
        lastUpdateTime = world.getTime();
        isReachable = reachable;
        // if (MRLConstants.LAUNCH_VIEWER) {
        // try {
        // if (world.getPlatoonAgent() != null) {
        // if
        // (!ApolloWorld.BLOCKED_BUILDINGS.containsKey(world.getPlatoonAgent().getID()))
        // {
        // ApolloWorld.BLOCKED_BUILDINGS.put(world.getPlatoonAgent().getID(),
        // new HashSet<EntityID>());
        // }
        // if (reachable) {
        // ApolloWorld.BLOCKED_BUILDINGS.get(world.getPlatoonAgent().getID()).remove(getID());
        // } else {
        // ApolloWorld.BLOCKED_BUILDINGS.get(world.getPlatoonAgent().getID()).add(getID());
        // }
        // }
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        // } FIXME viewer
    }

    public boolean intersectToLine2D(Line2D line) {
        for (Wall wall : getWalls()) {
            if (wall.getLine().intersectsLine(line))
                return true;
        }
        return false;
    }

    /**
     * human in this building, old comms version.
     * @deprecated
     * @return human set
     */
    public Set<Human> getHumans() {
        return humans;
    }

    /**
     * civilian in this building.
     * @return civilian set
     */
    public Set<Civilian> getCivilians() {
        return civilians;
    }

    public void setSensed(int time) {
        sensed = true;
        sensedTime = time;
    }

    public boolean isSensed() {
        return sensed;
    }

    /**
     *
     * @return 看见的时间
     */
    public int getSensedTime() {
        return sensedTime;
    }

    public void setCivilianPossibleValue(double civilianPossibleValue) {
        this.civilianPossibleValue = civilianPossibleValue;
    }

    public Set<EntityID> getVisibleFrom() {
        return visibleFrom;
    }

    public void setVisibleFrom(Set<EntityID> visibleFrom) {
        this.visibleFrom = visibleFrom;
    }

    public List<EntityID> getObservableAreas() {
        if(observableAreas == null || observableAreas.isEmpty()) {  // XXX
            observableAreas = lineOfSightPerception.getVisibleAreas(getID());
        }
        return observableAreas;
    }

    public void setObservableAreas(List<EntityID> observableAreas) {
        this.observableAreas = observableAreas;
    }

    public List<RayModel> getLineOfSight() {
        return lineOfSight;
    }

    public void setLineOfSight(List<RayModel> lineOfSight) {
        this.lineOfSight = lineOfSight;
    }

    public double getAdvantageRatio() {
        return advantageRatio;
    }

    public void setAdvantageRatio(double advantageRatio) {
        this.advantageRatio = advantageRatio;
    }

    public Set<EntityID> getExtinguishableFromAreas() {
        return extinguishableFromAreas;
    }

    public void setExtinguishableFromAreas(
            Set<EntityID> extinguishableFromAreas) {
        this.extinguishableFromAreas = extinguishableFromAreas;
    }

    public List<BuildingModel> getBuildingsInExtinguishRange() {
        return buildingsInExtinguishRange;
    }

    public void setBuildingsInExtinguishRange(
            List<BuildingModel> buildingsInExtinguishRange) {
        this.buildingsInExtinguishRange = buildingsInExtinguishRange;
    }

    public int getIgnitionTime() {
        return ignitionTime;
    }

    public void setIgnitionTime(int ignitionTime) {
        if (this.ignitionTime == -1) {
            this.ignitionTime = ignitionTime;
        }
    }

    @Override
    public int x() {
        return getSelfBuilding().getX();
    }

    @Override
    public int y() {
        return getSelfBuilding().getY();
    }

    // public void setHullId(EntityID hId) {
    // this.hullId = hId;
    // }

    // /**
    // * TODO 注意凸包ID是否及时更新，现在只能保证凸包中取出的建筑的ID为最新的
    // *
    // * @return
    // */
    // public EntityID getHullID() {
    // if (this.hullId != null) {
    // return hullId;
    // }
    // // System.out.println("id =null.....");
    // return null;
    // }

    /**
     * 凸包距离权值，必须先在broderestimator中设立
     *
     * @return
     */
    public double getBorderValue() {
        return borderValue;
    }

    /**
     * 凸包距离权值，必须先在broderestimator中设立
     *
     * @param borderValue
     */
    public void setBorderValue(double borderValue) {
        this.borderValue = borderValue;
    }

    /**
     * 设置建筑物是否处于地图边界
     *
     * @param b
     */
    public void setMapSide(boolean b) {
        isMapSide = b;
    }

    /**
     *
     * @return 建筑物是否处于地图边界
     */
    public boolean isMapSide() {
        return isMapSide;
    }

    /**
     * if Gas
     *
     * @return
     */
    public boolean isGasStation() {
        if (selfBuilding instanceof GasStation) {
            return true;
        }
        return false;
    }

    /**
     * 能否一周期浇灭
     *
     * @param water
     * @return
     */
    public boolean isExtinguishableInOneCycle(int water) {
        double dE = ((getEstimatedTemperature() - 40) * getCapacity());
        return (dE / 25) <= water;
    }

    public void setMapCenterDistance() {
        // int centerX = Math.abs(world.getMaxX() - world.getMinX()) / 2;
        // int centerY = Math.abs(world.getMaxX() - world.getMinY()) / 2;
        // mapCenterDistance = Util.distance(centerX, centerY, x(), y());
        // System.out.println("X:"+centerX+" Y:"+centerY+"  distance:"+mapCenterDistance);
        mapCenterDistance = Util.distance(
                (int) world.getBounds().getWidth() / 2, (int) world.getBounds()
                        .getHeight() / 2, x(), y());
        // System.out.println("22 X:"+world.getBounds().getWidth() / 2
        // +" Y:"+world.getBounds().getHeight() / 2 + " dis:"+distance);
    }

    /**
     * 返回距离地图中间的距离
     *
     * @return
     */
    public int getMapCenterDistance() {
        if (mapCenterDistance == 0) {
            setMapCenterDistance();
        }
        return mapCenterDistance;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BuildingModel) {
            return this.selfBuilding.getID().equals(
                    ((BuildingModel) o).getSelfBuilding().getID());
        }
        return false;
    }

    public List<BuildingModel> getNeighborBuildings() {
        List<BuildingModel> buildings = new ArrayList<BuildingModel>();
        for (EntityID id : getNeighbourIdBuildings()) {
            BuildingModel model = world.getBuildingModel(id);
            if (model != null) {
                buildings.add(model);
            }
        }
        return buildings;
    }

    /**
     * 判断自身是否可燃
     *
     * @return
     */
    public boolean canFire() {
        if (getSelfBuilding() instanceof Refuge
                || getSelfBuilding() instanceof PoliceOffice
                || getSelfBuilding() instanceof FireStation
                || getSelfBuilding() instanceof AmbulanceCentre) {
            return false;
        }
        return true;
    }

    /**
     *
     * @return self position point
     */
    public Point getSelfPoint() {
        if (selfPoint == null) {
            selfPoint = new Point(x(), y());
        }
        return selfPoint;
    }

    public double getHitRate() {
        return hitRate;
    }

    public void setHitRate(double hitRate) {
        this.hitRate = hitRate;
    }

    /**
     * {@link ApolloWorld} use to update value.</br>
     * <b>Note: </b> <code>setEnergy</code> before use
     * @param building
     */
    public void updateValues(Building building) {
        switch (building.getFieryness()) {
            case 0:
                this.setFuel(this.getInitialFuel());
                if (getEstimatedTemperature() >= getIgnitionPoint()) {
                    setEnergy(getIgnitionPoint() / 2);
                }
                break;
            case 1:
                if (getFuel() < getInitialFuel() * 0.66) {
                    setFuel((float) (getInitialFuel() * 0.75));
                } else if (getFuel() == getInitialFuel()) {
                    setFuel((float) (getInitialFuel() * 0.90));
                }
                break;

            case 2:
                if (getFuel() < getInitialFuel() * 0.33
                        || getFuel() > getInitialFuel() * 0.66) {
                    setFuel((float) (getInitialFuel() * 0.50));
                }
                break;

            case 3:
                if (getFuel() < getInitialFuel() * 0.01
                        || getFuel() > getInitialFuel() * 0.33) {
                    setFuel((float) (getInitialFuel() * 0.15));
                }
                break;

            case 8:
                setFuel(0);
                break;
        }
    }

    public void resetOldReachable(int resetTime) {
        if (agentInfo.getTime() - lastUpdateTime > resetTime) {
            setReachable(true);
            setVisitable(true);
        }
    }

    // wzp XXX
    public void createEntranceEdge() {
        for(Entrance entrance : getEntrances()) {
            Edge connectedEdge = getConnectedEdge(entrance);
            entranceEdgeMap.put(entrance.getID(), connectedEdge);
        }
    }

    public Edge getConnectedEdge(Entrance entrance) {
        int loop = 0;
        Edge connectedEdge = null;
        Road entranceRoad = entrance.getNeighbour();
        Set<EntityID> checkedSet = new HashSet<EntityID>();  //集合里全都是建筑物！
        Set<EntityID> toRemove = new HashSet<EntityID>();
        Set<EntityID> neighbours = new HashSet<EntityID>();
        checkedSet.add(selfBuilding.getID());

        while(connectedEdge == null && loop < 20) {   //循环最多20次！
            loop++;
            neighbours.clear();
            for(EntityID id : checkedSet) {
                Building checkedbuilding = (Building)world.getEntity(id);
                connectedEdge = entranceRoad.getEdgeTo(checkedbuilding.getID());
                if(connectedEdge != null) {
                    return connectedEdge;
                }else {
                    for(EntityID id1 : checkedbuilding.getNeighbours()) {
                        if(world.getEntity(id1) instanceof Building) {
                            neighbours.add(id1);
                        }
                    }
                }
            }
            toRemove.addAll(checkedSet);
            checkedSet.addAll(neighbours);
            checkedSet.removeAll(toRemove);
        }
        return null;
    }

    public Map<EntityID, Edge> getEntranceEdge() {
        return entranceEdgeMap;
    }

        public void cleanup() {
            allWalls.clear();
        }
}
