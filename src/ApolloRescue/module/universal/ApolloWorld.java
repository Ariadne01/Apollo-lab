package ApolloRescue.module.universal;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;


import ApolloRescue.ApolloConstants;
import ApolloRescue.module.algorithm.convexhull.BorderEntities;
import ApolloRescue.module.complex.component.*;
import ApolloRescue.module.complex.firebrigade.FireBrigadeWorld;
import ApolloRescue.module.universal.entities.BuildingModel;
import ApolloRescue.module.universal.entities.Entrance;
import ApolloRescue.module.universal.entities.Paths;
import ApolloRescue.module.universal.entities.RoadModel;

import ApolloRescue.module.universal.newsearch.graph.GraphModule;
import ApolloRescue.module.universal.tools.comparator.EntityIDComparator;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.AbstractModule;
import firesimulator.world.Wall;
import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.messages.Command;
import rescuecore2.misc.Handy;
import rescuecore2.misc.Pair;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.*;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Property;

import static rescuecore2.standard.entities.StandardEntityURN.AMBULANCE_TEAM;
import static rescuecore2.standard.entities.StandardEntityURN.FIRE_BRIGADE;
import static rescuecore2.standard.entities.StandardEntityURN.POLICE_FORCE;


public class ApolloWorld extends AbstractModule {

    protected ScenarioInfo scenarioInfo;
    protected AgentInfo agentInfo;
    protected WorldInfo worldInfo;
    protected ModuleManager moduleManager;

    public boolean shouldPrecompute = false;
    private boolean precomputed = false;
    private boolean resumed = false;
    private boolean preparated = false;

    public int maxID = 0;
    public float rayRate = 0.0025f;

    private Set<EntityID> changes = new FastSet<EntityID>();
//    protected List<IComponent> helpers = new ArrayList<>();//ljy: MRL中helper包中的类 IComponent的实现类
//    helpers == components

    protected Human selfHuman;
    protected StandardAgent self;
    protected Building selfBuilding;

    protected int minX, minY, maxX, maxY;
    private int maxPower;
    protected int worldTotalArea;
    protected Long uniqueMapNumber;

    /** The last command */
    private int lastCommand;

    // 记录多个周期一直在<身边的同类Agent,Agent--连续出现次数>
    protected Map<Human, Integer> sameAgentNear = new HashMap<Human, Integer>();
    private int[] biggestGatherEvent; //<gatherNum, gatherTime>
    /**
     * 历史坐标
     */
    private List<Pair<Integer, Integer>> historyLocation;
    private int lastMassiveGatherTime;
    private Pair<Integer, Integer> lastMassiveGatherLocation;
    /**
     * 各周期内FB数量
     */
    private List<Integer> companions;
    private Map<EntityID, Integer> PFNearby;


    protected boolean CommunicationLess = false;
    protected boolean isCommunicationLow = false;
    protected boolean isCommunicationMedium = false;
    protected boolean isCommunicationHigh = false;
    public boolean isWaterRefillRateInHydrantSet;
    public boolean isWaterRefillRateInRefugeSet;

    protected boolean isMapHuge = false;
    protected boolean isMapMedium = false;
    protected boolean isMapSmall = false;
    protected Set<Road> roadsSeen;
    protected List<BuildingModel> apolloBuildings;
    protected Set<Building> buildingsSeen;
    protected Set<Civilian> civiliansSeen;
    protected Set<Blockade> blockadesSeen;
    protected Set<FireBrigade> fireBrigadesSeen = new HashSet<FireBrigade>();
    protected Set<StandardEntity> roads;
    protected Set<RoadModel> apolloRoads;
    protected Map<EntityID, RoadModel> apolloRoadIdMap;
    protected Collection<StandardEntity> civilians;
    protected Set<StandardEntity> areas;
    protected Set<StandardEntity> humans;
    protected Set<StandardEntity> agents;
    protected Set<StandardEntity> platoonAgents;
    protected Set<StandardEntity> hydrants;
    protected Set<StandardEntity> gasStations;
    protected Map<EntityID, EntityID> entranceRoads = new FastMap<EntityID, EntityID>();
    protected List<EntityID> unvisitedBuildings = new ArrayList<EntityID>();
    protected Set<EntityID> visitedBuildings = new FastSet<EntityID>();
    protected Set<EntityID> thisCycleEmptyBuildings = new FastSet<EntityID>();
    protected Set<EntityID> emptyBuildings;
    protected Set<StandardEntity> buildings;
    protected Set<EntityID> borderBuildings;
    public BorderEntities borderFinder;
    protected Map<EntityID, BuildingModel> apolloBuildingIdMap;
    protected List<BuildingModel> shouldCheckInsideBuildings = new ArrayList<BuildingModel>();
    protected PropertyComponent propertyComponent;
    protected Set<EntityID> burningBuildings;
    private Set<EntityID> allCivilians;
    private Set<EntityID> heardCivilians;
    protected int lastAfterShockTime = 0;
    protected int aftershockCount = 0;

    protected List<BuildingModel> buildingModels;
    protected Map<EntityID, BuildingModel> tempBuildingsMap;
    Map<String, Building> buildingXYMap = new FastMap<String, Building>();
    protected Map<String, Road> roadXYMap;
    protected Set<EntityID> mapSideBuildings;   // PF & FB fire zone
    protected Set<BuildingModel> estimatedBurningBuildings = new FastSet<BuildingModel>(); // FB & PF
    private Set<EntityID> possibleBurningBuildings;


    // IComponent
    protected List<IComponent> components = new ArrayList<>();
    protected List<FireBrigade> fireBrigadeList = new ArrayList<FireBrigade>();
    protected List<PoliceForce> policeForceList = new ArrayList<PoliceForce>();
    protected List<AmbulanceTeam> ambulanceTeamList = new ArrayList<AmbulanceTeam>();
    public List<EntityID> viewerEmptyBuildings = new ArrayList<EntityID>();
    protected List<Civilian> civilianSeenInMap = new ArrayList<Civilian>(); //从一开始所有看见的！

    protected GraphModule graphModule;
    protected Paths paths;


    public ApolloWorld(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.worldInfo = wi;
        this.agentInfo = ai;
        this.scenarioInfo = si;
        this.moduleManager = moduleManager;
        this.developData = developData;
        this.roadsSeen = new HashSet<>();
        buildingsSeen = new HashSet<>();
        civiliansSeen = new HashSet<>();
        blockadesSeen = new HashSet<>();
        apolloRoads = new HashSet<>();
        burningBuildings = new HashSet<>();
        apolloRoadIdMap = new HashMap<>();
        emptyBuildings = new FastSet<EntityID>();
        thisCycleEmptyBuildings = new HashSet<>();
        civilians = new HashSet<>();
        buildings = new HashSet<>(getBuildingsWithURN());
        entranceRoads = new HashMap<>();
        buildingXYMap = new HashMap<>();
        apolloBuildingIdMap = new HashMap<>();
        this.mapSideBuildings = new FastSet<EntityID>();
        burningBuildings = new HashSet<>();
        allCivilians = new HashSet<>();

        buildings = new HashSet<>(getBuildingsWithURN());
        roads = new HashSet<>(getRoadsWithURN());
        areas = new HashSet<>(getAreasWithURN());
        humans = new HashSet<>(getHumansWithURN());
        agents = new HashSet<>(getAgentsWithURN());
        platoonAgents = new HashSet<>(getPlatoonAgentsWithURN());

        hydrants = new HashSet<>(getHydrantsWithURN());
        gasStations = new HashSet<>(getGasStationsWithUrn());
        graphModule = new GraphModule(ai, wi, si, moduleManager, developData);

        apolloRoadIdMap = new HashMap<>();
        entranceRoads = new HashMap<>();
        buildingXYMap = new HashMap<>();
        roadXYMap = new HashMap<>();

        possibleBurningBuildings = new HashSet<>();
        heardCivilians = new HashSet<>();

        createUniqueMapNumber();


        components.add(new PropertyComponent(this));
        components.add(new BuildingInfoComponent(this, scenarioInfo, agentInfo, worldInfo));
        components.add(new RoadInfoComponent(this, ai, wi, si, moduleManager, developData));
        components.add(new CivilianInfoComponent(this, si, ai, wi));
        components.add(new VisibilityInfoComponent(this, si, ai, wi));


        this.self = self;
        if (worldInfo.getEntity(agentInfo.getID()) instanceof Building) {
            selfBuilding = (Building) worldInfo.getEntity(agentInfo.getID());
//            this.centre = (MrlCentre) self;
        } else {
//            this.platoonAgent = (MrlPlatoonAgent) self;
            selfHuman = (Human) worldInfo.getEntity(agentInfo.getID());
        }

        propertyComponent = this.getComponent(PropertyComponent.class);

        calculateMapDimensions();
        setMapInfo();
        StandardEntity entity = worldInfo.getEntity(agentInfo.getID());
        if (entity instanceof FireBrigade || entity instanceof PoliceForce || entity instanceof AmbulanceTeam) {
//            System.err.println("calling createApolloBuildings .......");
            createBuildingsModel();
            //createApolloBuildings();

        }

        paths = new Paths(this, worldInfo, agentInfo, scenarioInfo);
        createApolloRoads();
        borderBuildings = new HashSet<>();
        borderFinder = new BorderEntities(this);

//        policeForceList = getPoliceForceList();
        this.mapSideBuildings = new FastSet<EntityID>();
        this.companions = new ArrayList<Integer>();
        this.PFNearby = new HashMap<EntityID, Integer>();
        this.biggestGatherEvent = new int[2];
        this.historyLocation = new ArrayList<Pair<Integer, Integer>>();

        //Jingyi Lu  TODO: Check （暂时）
//        for (StandardEntity standardEntity :  worldInfo.getEntitiesOfType(
//                StandardEntityURN.POLICE_FORCE, StandardEntityURN.FIRE_BRIGADE,
//                StandardEntityURN.AMBULANCE_TEAM)) {
//            if (standardEntity instanceof FireBrigade) {
//                fireBrigadeList.add((FireBrigade) standardEntity);
//            } else if (standardEntity instanceof PoliceForce) {
//                policeForceList.add((PoliceForce) standardEntity);
//            } else if (standardEntity instanceof AmbulanceTeam) {
//                ambulanceTeamList.add((AmbulanceTeam) standardEntity);
//            }
//            if (maxID < standardEntity.getID().getValue()) {
//                maxID = standardEntity.getID().getValue();
//            }
//        }

    }


    @Override
    public AbstractModule calc() {
        return this;
    }

    /**
     * To update basic world info every cycle.</br>
     * <b>Note: </b>child agent override this method will update after component.</br>
     */


    public synchronized static ApolloWorld load(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, DevelopData developData) {
        ApolloWorld world = null;
        try {
//          System.err.println(" getting ApolloWorldHelper .......");
            if (agentInfo.me().getStandardURN() == FIRE_BRIGADE) {
                world = moduleManager.getModule("ApolloRescue.module.complex.firebrigade.FireBrigadeWorld", "ApolloRescue.module.complex.firebrigade.FireBrigadeWorld");
                //world = new FireBrigadeWorld(agentInfo,worldInfo,scenarioInfo,moduleManager,developData);
                // System.out.println("FBWORLD");
            } /* else if(agentInfo.me().getStandardURN() == POLICE_FORCE){ // Jingyi Lu
                world = moduleManager.getModule("ApolloRescue.extaction.clear.PoliceForceWorld", "ApolloRescue.module.universal.ApolloWorld");
            } */
            else {
                world = moduleManager.getModule("ApolloRescue.ApolloWorld", "ApolloRescue.module.universal.ApolloWorld");
                //world = new ApolloWorld(agentInfo,worldInfo,scenarioInfo,moduleManager,developData);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            world = null;
        }
        if (world == null) {
//            System.err.println(" creating ApolloWorldHelper .......");
            if (worldInfo.getEntity(agentInfo.getID()) instanceof FireBrigade) {
                world = new FireBrigadeWorld(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
            } else {
                world = new ApolloWorld(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
            }
        }
        return world;
    }

    private void createApolloRoads() {
        apolloRoads = new HashSet<>();
        apolloRoadIdMap = new HashMap<>();
        for (StandardEntity rEntity : getRoads()) {
            Road road = (Road) rEntity;
            RoadModel apolloRoad = new RoadModel(agentInfo, worldInfo, scenarioInfo, moduleManager, road, this);
            apolloRoads.add(apolloRoad);
            apolloRoadIdMap.put(road.getID(), apolloRoad);
            String xy = road.getX() + "," + road.getY();
            roadXYMap.put(xy, road);
        }


//        MrlPersonalData.VIEWER_DATA.setViewRoadsMap(self.getID(), mrlRoads);
    }

    private void createApolloBuildings() {

        tempBuildingsMap = new HashMap<>();
        apolloBuildings = new ArrayList<>();
        BuildingModel apolloBuilding;
        Building building;

        for (StandardEntity standardEntity : getBuildings()) {
            building = (Building) standardEntity;
            String xy = building.getX() + "," + building.getY();
            buildingXYMap.put(xy, building);

            apolloBuilding = new BuildingModel(standardEntity, this, worldInfo, agentInfo);

            if ((standardEntity instanceof Refuge)
                    || (standardEntity instanceof FireStation)
                    || (standardEntity instanceof PoliceOffice)
                    || (standardEntity instanceof AmbulanceCentre)) {  //todo all of these buildings may be flammable..............
                apolloBuilding.setFlammable(false);
            }
            apolloBuildings.add(apolloBuilding);
            tempBuildingsMap.put(standardEntity.getID(), apolloBuilding);
            apolloBuildingIdMap.put(building.getID(), apolloBuilding);

            // ina bejaye building helper umade.
            unvisitedBuildings.add(standardEntity.getID());
            worldTotalArea += apolloBuilding.getSelfBuilding().getTotalArea();

        }
        shouldCheckInsideBuildings.clear();

        //related to FBLegacyStrategy and Zone operations
        if (getSelfHuman() instanceof FireBrigade) {


            apolloBuildings.parallelStream().forEach(b -> {
                Collection<StandardEntity> neighbour = worldInfo.getObjectsInRange(b.getSelfBuilding(), Wall.MAX_SAMPLE_DISTANCE);
//            Collection<StandardEntity> fireNeighbour = getObjectsInRange(b.getSelfBuilding(), Wall.MAX_FIRE_DISTANCE);
                List<EntityID> neighbourBuildings = new ArrayList<EntityID>();
                for (StandardEntity entity : neighbour) {
                    if (entity instanceof Building) {
                        neighbourBuildings.add(entity.getID());
                        b.addApolloBuildingNeighbour(tempBuildingsMap.get(entity.getID()));
                    }
                }
                b.setNeighbourIdBuildings(neighbourBuildings);


            });

//            for (MrlBuilding b : mrlBuildings) {
//                Collection<StandardEntity> neighbour = worldInfo.getObjectsInRange(b.getSelfBuilding(), Wall.MAX_ SAMPLE_DISTANCE);
////            Collection<StandardEntity> fireNeighbour = getObjectsInRange(b.getSelfBuilding(), Wall.MAX_FIRE_DISTANCE);
//                List<EntityID> neighbourBuildings = new ArrayList<EntityID>();
//                for (StandardEntity entity : neighbour) {
//                    if (entity instanceof Building) {
//                        neighbourBuildings.add(entity.getID());
//                        b.addMrlBuildingNeighbour(tempBuildingsMap.get(entity.getID()));
//                    }
//                }
//                b.setNeighbourIdBuildings(neighbourBuildings);
//            }
        }


        for (BuildingModel b : apolloBuildings) {
            //MTN
            if (b.getEntrances() != null) {
                building = b.getSelfBuilding();
                List<Road> rEntrances = BuildingInfoComponent.getEntranceRoads(this, building);
                for (Road road : rEntrances) {
                    entranceRoads.put(road.getID(), b.getID());
                }

/*

                boolean shouldCheck = true;
//                if (rEntrances != null) {
//                    if (rEntrances.size() == 0)
//                        shouldCheck = false;
                VisibilityInfoComponent visibilityHelper = getHelper(VisibilityInfoComponent.class);
                for (Road road : rEntrances) {
                    boolean shouldCheckTemp = !visibilityHelper.isInsideVisible(new Point(road.getX(), road.getY()), new Point(building.getX(), building.getY()), building.getEdgeTo(road.getID()), scenarioInfo.getPerceptionLosMaxDistance());
                    if (!shouldCheckTemp) {
                        shouldCheck = false;
                        break;
//                    }
                    }
                }
                b.setShouldCheckInside(shouldCheck);
                if (shouldCheck) {
                    shouldCheckInsideBuildings.add(b);
                }
*/


            }
//            b.setNeighbourFireBuildings(fireNeighbours);
//            MrlPersonalData.VIEWER_DATA.setMrlBuildingsMap(b);

        }

//        MrlPersonalData.VIEWER_DATA.setViewerBuildingsMap(self.getID(), mrlBuildings);
    }

    public List<StandardEntity> getBuildingsInShape(Shape shape) {
        List<StandardEntity> result = new ArrayList<StandardEntity>();
        for (StandardEntity next : getBuildings()) {
            Area area = (Area) next;
            if (shape.contains(area.getShape().getBounds2D()))
                result.add(next);
        }
        return result;
    }

    //与getBuildingsModel一样,重复。请使用本方法
    public List<BuildingModel> getApolloBuildings() {
        return buildingModels;
    }

    public BuildingModel getBuildingsModel(EntityID id) {
        return apolloBuildingIdMap.get(id);
    }

    public Set<StandardEntity> getBuildings() {
        return buildings;
    }


    public <T extends StandardEntity> T getEntity(EntityID id, Class<T> c) {
        StandardEntity entity;

        entity = getEntity(id);
        if (c.isInstance(entity)) {
            T castedEntity;

            castedEntity = c.cast(entity);
            return castedEntity;
        } else {
            return null;
        }
    }

    public Set<BuildingModel> getEstimatedBurningBuildings() {
        return estimatedBurningBuildings;
    }

    /**
     * pf & fb
     */
    protected void setEstimatedBurningBuildings() {
        estimatedBurningBuildings.clear();
        for (BuildingModel buildingModel : getBuildingsModel()) {
            if (buildingModel.getEstimatedFieryness() >= 1 && buildingModel.getEstimatedFieryness() <= 3) {
                estimatedBurningBuildings.add(buildingModel);
            }
        }
    }

    public List<PoliceForce> getPoliceForceList() {
        return policeForceList;
    }

    public List<BuildingModel> getBuildingsModel() {
        return buildingModels;
    }

    public BuildingModel getBuildingModel(EntityID id) {
        return tempBuildingsMap.get(id);
    }

    public Map<EntityID, BuildingModel> getApolloBuildingIdMap() {
        return apolloBuildingIdMap;
    }

    public Set<Civilian> getCiviliansSeen() {
        return civiliansSeen;
    }

    private void createBuildingsModel() {

        tempBuildingsMap = new FastMap<EntityID, BuildingModel>();
        buildingModels = new ArrayList<BuildingModel>();
        BuildingModel buildingModel;
        Building building;

        for (StandardEntity standardEntity : getBuildings()) {
            building = (Building) standardEntity;
            String xy = building.getX() + "," + building.getY();
            buildingXYMap.put(xy, building);

            buildingModel = new BuildingModel(standardEntity, this, worldInfo, agentInfo);

            if ((standardEntity instanceof Refuge)
                    || (standardEntity instanceof FireStation)
                    || (standardEntity instanceof PoliceOffice)
                    || (standardEntity instanceof AmbulanceCentre)) { // todo
                buildingModel.setFlammable(false);
            }
            buildingModels.add(buildingModel);
            tempBuildingsMap.put(standardEntity.getID(), buildingModel);
            apolloBuildingIdMap.put(building.getID(), buildingModel);

            unvisitedBuildings.add(standardEntity.getID());
            viewerEmptyBuildings.add(standardEntity.getID());
            worldTotalArea += buildingModel.getSelfBuilding().getTotalArea();

        }
        shouldCheckInsideBuildings.clear();

        for (BuildingModel b : buildingModels) {
            Collection<StandardEntity> neighbour = worldInfo.getObjectsInRange(b.getSelfBuilding(), Wall.MAX_SAMPLE_DISTANCE);
            // Collection<StandardEntity> fireNeighbour =
            // getObjectsInRange(b.getSelfBuilding(), Wall.MAX_FIRE_DISTANCE);
            List<EntityID> neighbourBuildings = new ArrayList<EntityID>();
            // List<EntityID> fireNeighbours = new ArrayList<EntityID>();
            for (StandardEntity entity : neighbour) {
                if (entity instanceof Building) {
                    neighbourBuildings.add(entity.getID());
                    b.addBuildingModelNeighbour(tempBuildingsMap.get(entity
                            .getID()));
                }
            }


            b.setNeighbourIdBuildings(neighbourBuildings);
            // MTN
            if (b.getEntrances() != null) {
                building = b.getSelfBuilding();
                List<Road> rEntrances = BuildingInfoComponent.getEntranceRoads(
                        this, building);
                for (Road road : rEntrances) {
                    entranceRoads.put(road.getID(), b.getID());
                }

                /*boolean shouldCheck = true;
                // if (rEntrances != null) {
                // if (rEntrances.size() == 0)
                // shouldCheck = false;
                VisibilityComponent visibilityComponent = getComponent(VisibilityComponent.class);
                for (Road road : rEntrances) {
                    boolean shouldCheckTemp = !visibilityComponent
                            .isInsideVisible(
                                    new Point(road.getX(), road.getY()),
                                    new Point(building.getX(), building.getY()),
                                    building.getEdgeTo(road.getID()),
                                    viewDistance);
                    if (!shouldCheckTemp) {
                        shouldCheck = false;
                        break;
                        // }
                    }
                }*/
                /*b.setShouldCheckInside(shouldCheck);
                if (shouldCheck) {
                    shouldCheckInsideBuildings.add(b);
                }
                */

            }
            // // b.setNeighbourFireBuildings(fireNeighbours);
        }
    }


    /**
     * this method remove input building from {@code visitedBuildings}, add it in the {@code unvisitedBuilding} and prepare
     * message that should be send.<br/><br/>
     * use this must visit it to make sure whether it is empty.
     *
     * @param buildingID  {@code EntityID} of building that visited!
     * @param sendMessage {@code boolean} to sent visited building message
     */
    public void setBuildingVisited(EntityID buildingID, boolean sendMessage) {
        BuildingModel buildingModel = getBuildingModel(buildingID);
        /*if (commonAgent == null) {
            return;
        }*/
        if (!buildingModel.isVisited()) {
            buildingModel.setVisited();
            visitedBuildings.add(buildingID);
            unvisitedBuildings.remove(buildingID);
        }
        updateEmptyBuildingState(buildingModel, sendMessage);
    }

    private Collection<StandardEntity> getBuildingsWithURN() {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.GAS_STATION);
    }

    private Collection<StandardEntity> getHydrantsWithURN() {
        return worldInfo.getEntitiesOfType(StandardEntityURN.HYDRANT);
    }

    /**
     * map size move time > 60 big; > 30 medium ; small
     */
    private void setMapInfo() {
        double mapDimension = Math.hypot(getMapWidth(), getMapHeight());
        double rate = mapDimension / ApolloConstants.MEAN_VELOCITY_OF_MOVING;
        if (rate > 60) {
            isMapHuge = true;
        } else if (rate > 30) {
            isMapMedium = true;
        } else {
            isMapSmall = true;
        }
    }

    private Collection<StandardEntity> getGasStationsWithUrn() {
        return worldInfo.getEntitiesOfType(StandardEntityURN.GAS_STATION);
    }

    private Collection<StandardEntity> getAreasWithURN() {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.REFUGE,
                StandardEntityURN.ROAD,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.HYDRANT,
                StandardEntityURN.GAS_STATION);
    }

    private Collection<StandardEntity> getHumansWithURN() {
        return worldInfo.getEntitiesOfType(
                StandardEntityURN.CIVILIAN,
                FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM);
    }

    private Collection<StandardEntity> getAgentsWithURN() {
        return worldInfo.getEntitiesOfType(
                FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.POLICE_OFFICE,
                StandardEntityURN.AMBULANCE_CENTRE);
    }

    private Collection<StandardEntity> getPlatoonAgentsWithURN() {
        return worldInfo.getEntitiesOfType(
                FIRE_BRIGADE,
                StandardEntityURN.POLICE_FORCE,
                StandardEntityURN.AMBULANCE_TEAM);
    }


    public Collection<StandardEntity> getFireBrigades() {
        return worldInfo.getEntitiesOfType(FIRE_BRIGADE);
    }

    public Collection<StandardEntity> getAmbulanceTeams() {
        return worldInfo.getEntitiesOfType(AMBULANCE_TEAM);
    }

    public Collection<StandardEntity> getPoliceForces() {
        return worldInfo.getEntitiesOfType(POLICE_FORCE);
    }

//    // Jingyi Lu
//    public List<PoliceForce> getPoliceForceList() {
//        Collection<StandardEntity> standardPolices = getPoliceForces();
//        for(StandardEntity standardEntity : standardPolices ){
//            if(standardEntity instanceof PoliceForce){
//                policeForceList.add((PoliceForce)standardEntity);
//            }
//        }
//        return policeForceList;
//    }

    public Collection<StandardEntity> getAreas() {
        return areas;
    }

    private Collection<StandardEntity> getRoadsWithURN() {
        return worldInfo.getEntitiesOfType(StandardEntityURN.ROAD, StandardEntityURN.HYDRANT);
    }


    public void updateEmptyBuildingState(BuildingModel buildingModel, boolean sendMessage) {
        if (!buildingModel.isVisited()) {
            return;
        }

        if (!emptyBuildings.contains(buildingModel.getID()) && buildingModel.getCivilians().isEmpty()) {
            if (sendMessage) {
                thisCycleEmptyBuildings.add(buildingModel.getID());
            }
            emptyBuildings.add(buildingModel.getID());
        }

        if (emptyBuildings.contains(buildingModel.getID()) && !buildingModel.getCivilians().isEmpty()) {
            emptyBuildings.remove(buildingModel.getID());
        }
    }

    private void calculateMapDimensions() {
        this.minX = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.maxY = Integer.MIN_VALUE;
        Pair<Integer, Integer> pos;
        List<StandardEntity> invalidEntities = new ArrayList<>();
        for (StandardEntity standardEntity : worldInfo.getAllEntities()) {
            pos = worldInfo.getLocation(standardEntity);
            if (pos.first() == Integer.MIN_VALUE || pos.first() == Integer.MAX_VALUE || pos.second() == Integer.MIN_VALUE || pos.second() == Integer.MAX_VALUE) {
                invalidEntities.add(standardEntity);
                continue;
            }
            if (pos.first() < this.minX)
                this.minX = pos.first();
            if (pos.second() < this.minY)
                this.minY = pos.second();
            if (pos.first() > this.maxX)
                this.maxX = pos.first();
            if (pos.second() > this.maxY)
                this.maxY = pos.second();
        }
        if (!invalidEntities.isEmpty()) {
            System.out.println("##### WARNING: There is some invalid entities ====> " + invalidEntities.size());
        }
    }


    public Building getBuildingInPoint(int x, int y) {
        String xy = x + "," + y;
        return buildingXYMap.get(xy);
    }

    public List<EntityID> getUnvisitedBuildings() {
        return unvisitedBuildings;
    }

    public Set<EntityID> getVisitedBuildings() {
        return visitedBuildings;
    }

    public Set<EntityID> getThisCycleEmptyBuildings() {
        return thisCycleEmptyBuildings;
    }

    public List<EntityID> getViewerEmptyBuildings() {
        return viewerEmptyBuildings;
    }

    public int getWorldTotalArea() {
        return worldTotalArea;
    }

    public List<BuildingModel> getShouldCheckInsideBuildings() {
        return shouldCheckInsideBuildings;
    }

    /**
     * Map of entrance RoadID to BuildingID.
     */
    public Map<EntityID, EntityID> getEntranceRoads() {
        return entranceRoads;
    }

    public boolean isEntrance(Road road) {
        return entranceRoads.containsKey(road.getID());
    }

    public int getMinX() {
        return this.minX;
    }

    public int getMinY() {
        return this.minY;
    }

    public int getMaxX() {
        return this.maxX;
    }

    public int getMaxY() {
        return this.maxY;
    }

    public int getMapWidth() {
        return maxX - minX;
    }

    public int getMapHeight() {
        return maxY - minY;
    }

    public List<FireBrigade> getFireBrigadeList() {
        return fireBrigadeList;
    }

    public int getMaxPower() {
        return maxPower;
    }

    public Human getSelfHuman() {
        return selfHuman;
    }

    public void setMaxPower(int maxPower) {
        this.maxPower = maxPower;
    }

    public ScenarioInfo getScenarioInfo() {
        return scenarioInfo;
    }

    public void setScenarioInfo(ScenarioInfo scenarioInfo) {
        this.scenarioInfo = scenarioInfo;
    }

    public AgentInfo getAgentInfo() {
        return agentInfo;
    }

    public void setAgentInfo(AgentInfo agentInfo) {
        this.agentInfo = agentInfo;
    }

    public WorldInfo getWorldInfo() {
        return worldInfo;
    }

    public void setModuleManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    public ModuleManager getModuleManager() {
        return this.moduleManager;
    }

    public void setDevelopData(DevelopData developData) {
        this.developData = developData;
    }

    public DevelopData getDevelopData() {
        return this.developData;
    }

    public void setWorldInfo(WorldInfo worldInfo) {
        this.worldInfo = worldInfo;
    }

    /**
     * fire zone use.</br>
     * <b>Note: </b>fb & pf self world initial before.</br>
     *
     * @return
     */
    public Set<EntityID> getMapSideBuildings() {
        return mapSideBuildings;
    }

    public StandardEntity getEntity(EntityID id) {
        return worldInfo.getEntity(id);
    }

    public Rectangle2D getBounds() {
        return worldInfo.getBounds();
    }

    public Building getSelfBuilding() {
        return selfBuilding;
    }

    public StandardEntity getSelfPosition() {
        if (worldInfo.getEntity(agentInfo.getID()) instanceof Building) {
            return selfBuilding;
        } else {
            return agentInfo.getPositionArea();
        }
    }


    public void setCommunicationLess(boolean CL) {
        this.CommunicationLess = CL;
    }

    public boolean isCommunicationLess() {
        return CommunicationLess;
    }

    public boolean isCommunicationLow() {
        return isCommunicationLow;
    }

    public void setCommunicationLow(boolean communicationLow) {
        isCommunicationLow = communicationLow;
    }

    public boolean isCommunicationMedium() {
        return isCommunicationMedium;
    }

    public void setCommunicationMedium(boolean communicationMedium) {
        isCommunicationMedium = communicationMedium;
    }

    public boolean isCommunicationHigh() {
        return isCommunicationHigh;
    }

    public void setCommunicationHigh(boolean communicationHigh) {
        isCommunicationHigh = communicationHigh;
    }

    private void createUniqueMapNumber() {
        long sum = 0;
        for (StandardEntity building : getBuildings()) {
            Building b = (Building) building;
            int[] ap = b.getApexList();
            for (int anAp : ap) {
                if (Long.MAX_VALUE - sum <= anAp) {
                    sum = 0;
                }
                sum += anAp;
            }
        }
        uniqueMapNumber = sum;

//        System.out.println("Unique Map Number=" + uniqueMapNumber);
    }

    public String getMapName() {
        return getUniqueMapNumber().toString();
    }

    public Long getUniqueMapNumber() {
        return uniqueMapNumber;
    }

    @Override
    public ApolloWorld precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        if (precomputed) {
            return this;
        }
        precomputed = true;
        components.forEach(IComponent::init);
        shouldPrecompute = true;
        return this;
    }


    @Override
    public ApolloWorld resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        if (resumed) {
            return this;
        }
        resumed = true;
        shouldPrecompute = false;
        components.forEach(IComponent::init);
        return this;
    }


    @Override
    public ApolloWorld preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        if (preparated) {
            return this;
        }
        preparated = true;
        shouldPrecompute = false;
        components.forEach(IComponent::init);
        return this;
    }

    private int lastUpdateTime = -1;


    @Override
    public ApolloWorld updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        if (lastUpdateTime == agentInfo.getTime()) {
            return this;
        }
        lastUpdateTime = agentInfo.getTime();
        reflectMessage(messageManager);

        roadsSeen.clear();
        buildingsSeen.clear();
        blockadesSeen.clear();
        civiliansSeen.clear();
        fireBrigadesSeen.clear();
        civilians = worldInfo.getEntitiesOfType(StandardEntityURN.CIVILIAN);
        for (StandardEntity civEntity : civilians) {
            allCivilians.add(civEntity.getID());
        }

        Collection<Command> heard = agentInfo.getHeard();
        heardCivilians.clear();
        if (heard != null) {
            for (Command next : heard) {
                if (next instanceof AKSpeak && ((AKSpeak) next).getChannel() == 0 && !next.getAgentID().equals(agentInfo.getID())) {// say messages
                    AKSpeak speak = (AKSpeak) next;
                    Collection<EntityID> platoonIDs = Handy.objectsToIDs(getAgents());
                    if (!platoonIDs.contains(speak.getAgentID())) {//Civilian message
                        processCivilianCommand(speak);
                        allCivilians.add(speak.getAgentID());
                    }
                }
            }
        }
        changes=agentInfo.getChanged().getChangedEntities();
        for (EntityID changedId : worldInfo.getChanged().getChangedEntities()) {
            StandardEntity entity = worldInfo.getEntity(changedId);
            if (entity instanceof Civilian) {
                Civilian civilian = (Civilian) entity;
                civiliansSeen.add(civilian);
            } else if (entity instanceof Building) {
                Building building = (Building) entity;
                //Checking for AFTER SHOCK occurrence
                Property brokennessProperty = building.getProperty(StandardPropertyURN.BROKENNESS.toString());
                if (brokennessProperty.isDefined()) {
                    int newBrokennessValue = -1;
                    for (Property p : worldInfo.getChanged().getChangedProperties(building.getID())) {
                        if (p.getURN().endsWith(brokennessProperty.getURN())) {
                            newBrokennessValue = (Integer) p.getValue();
                        }
                    }
                    if (building.getBrokenness() < newBrokennessValue) {
                        //after shock is occurred
                        if (propertyComponent.getPropertyTime(brokennessProperty) > getLastAfterShockTime()) {
                            setAftershockProperties(agentInfo.getTime(), agentInfo.getTime());
                        }
                    }
                }

                //Update seen building properties
                for (Property p : worldInfo.getChanged().getChangedProperties(building.getID())) {
                    building.getProperty(p.getURN()).takeValue(p);
                    propertyComponent.setPropertyTime(building.getProperty(p.getURN()), agentInfo.getTime());
                }

                BuildingModel apolloBuilding = getBuildingsModel(building.getID());
                if (agentInfo.me() instanceof FireBrigade) {
                    if (building.isFierynessDefined() && building.isTemperatureDefined()) {
                        apolloBuilding.setEnergy(building.getTemperature() * apolloBuilding.getCapacity());
                        apolloBuilding.updateValues(building);
                    }
                }
//                if (getEntity(building.getID()) == null) {
//                    addEntityImpl(building);
                propertyComponent.addEntityProperty(building, agentInfo.getTime());
//                }

                //updating burning buildings set
                if (building.getFieryness() > 0 && building.getFieryness() < 4) {
                    burningBuildings.add(building.getID());
                } else {
                    burningBuildings.remove(building.getID());
                }

                buildingsSeen.add(building);
                apolloBuilding.setSensed(agentInfo.getTime());
                if (building.isOnFire()) {
                    apolloBuilding.setIgnitionTime(agentInfo.getTime());
                }

            } else if (entity instanceof Road) {
                Road road = (Road) entity;
                roadsSeen.add(road);

                RoadModel apolloRoad = getApolloRoad(entity.getID());
                if (apolloRoad.isNeedUpdate()) {
                    apolloRoad.update();
                }
                apolloRoad.setLastSeenTime(agentInfo.getTime());
                apolloRoad.setSeen(true);

//                if (road.isBlockadesDefined()) {
//                    for (EntityID blockadeId : road.getBlockades()) {
//                        blockadesSeen.add((Blockade) worldInfo.getEntity(blockadeId));
//                    }
//                }
            }else if (entity instanceof FireBrigade) {
                FireBrigade fireBrigade = (FireBrigade) entity;

                // System.out.println(getTime()+" "+getSelf().getID()
                // +" agent: "+ fireBrigade.getID());

                // if (!(selfHuman instanceof AmbulanceTeam) ||
                // !fireBrigade.isBuriednessDefined() || fireBrigade.getHP()
                // == 0) {
//                for (Property p : changeSet.getChangedProperties(entityID)) {
//                    fireBrigade.getProperty(p.getURN()).takeValue(p);
//                    propertyComponent.setPropertyTime(
//                            fireBrigade.getProperty(p.getURN()), time);
//                }
//                // } else {
//                //
//                // Property p = changeSet.getChangedProperty(entityID,
//                // fireBrigade.getPositionProperty().getURN());
//                // fireBrigade.setPosition((EntityID) p.getValue());
//                // propertyComponent.setPropertyTime(fireBrigade.getPositionProperty(),
//                // time);
//                //
//                // p = changeSet.getChangedProperty(entityID,
//                // fireBrigade.getBuriednessProperty().getURN());
//                // fireBrigade.setBuriedness((Integer) p.getValue());
//                // propertyComponent.setPropertyTime(fireBrigade.getBuriednessProperty(),
//                // time);
//                //
//                // }
//
//                // if (getPlatoonAgent() != null) {
//                // getPlatoonAgent().markVisitedBuildings((Area)
//                // getEntity(fireBrigade.getPosition()));
//                // }
//
//                if (Util.isOnBlockade(this, fireBrigade)) {
//                    getComponent(HumanInfoComponent.class)
//                            .setLockedByBlockade(fireBrigade.getID(), true);
//                }
//                // add agent seen
////					if (!agentSeen.contains(fireBrigade)) {
//                agentSeen.add(fireBrigade);
////					}
                fireBrigadesSeen.add(fireBrigade);
            }else if (entity instanceof Blockade) {
                blockadesSeen.add((Blockade) entity);
            }
        }

        components.forEach(IComponent::update);
        updateSameAgentNear();
        if (agentInfo.getTime() - this.biggestGatherEvent[1] > 0 && (agentInfo.getTime() - this.biggestGatherEvent[1]) % 5 == 0)   //更新历史位置
            this.historyLocation.add(worldInfo.getLocation(agentInfo.getID()));


        return this;
    }

    public void processCivilianCommand(AKSpeak speak) {
        Civilian civilian = (Civilian) getEntity(speak.getAgentID());
        if (civilian == null) {
            civilian = new Civilian(speak.getAgentID());
            addNewCivilian(civilian);
        }
        if (!civilian.isPositionDefined()) {
            addHeardCivilian(civilian.getID());
        }
    }


    public int getLastAfterShockTime() {
        return lastAfterShockTime;
    }

    public int getAftershockCount() {
        return aftershockCount;
    }

    public void setAftershockProperties(int lastAfterShockTime, int aftershockCount) {
        if (this.aftershockCount < aftershockCount) {
            this.aftershockCount = aftershockCount;
            if (selfHuman != null) {
                postAftershockAction();
            }
        }
    }

    public void postAftershockAction() {
        this.printData("New aftershock occurred! Time: " + agentInfo.getTime() + " Total: " + this.getAftershockCount());

        for (RoadModel apolloRoad : this.getApolloRoads()) {
            apolloRoad.getParent().undefineBlockades();
        }
    }

    public int getTime() {
        return agentInfo.getTime();
    }





    /**
     * All civilian defined in world model or their voice were heard.
     *
     * @return
     */
    public Set<EntityID> getAllCivilians() {
        return allCivilians;
    }


    public Collection<StandardEntity> getObjectsInRange(int x, int y, int range) {
        int newRange = (int) (0.64 * range);
        return worldInfo.getObjectsInRange(x, y, newRange);
    }

    public int getMaxExtinguishDistance() {
        return scenarioInfo.getFireExtinguishMaxDistance();

    }

    public Collection<StandardEntity> getObjectsInRange(EntityID entityID, int distance) {
        return worldInfo.getObjectsInRange(entityID, distance);
    }


    /**
     Get the distance between two entities.
     @param first The first entity.
     @param second The second entity.
     @return The distance between the two entities. A negative value indicates that one or both objects could not be located.
     */
    public int getDistance(StandardEntity first, StandardEntity second) {
//        Pair<Integer, Integer> a = first.getLocation(this);
        Pair<Integer, Integer> a = worldInfo.getLocation(first);
        Pair<Integer, Integer> b = worldInfo.getLocation(second);
        if (a == null || b == null) {
            return -1;
        }
        return Util.distance(a, b);
    }

    public int getDistance(EntityID first, EntityID second) {
        return worldInfo.getDistance(first, second);
    }

    public Set<EntityID> getBorderBuildings() {
        return borderBuildings;
    }

    public Set<EntityID> getBuildingIDs() {
        Set<EntityID> buildingIDs = new HashSet<>();
        Collection<StandardEntity> buildings = getBuildings();
        for (StandardEntity entity : buildings) {
            buildingIDs.add(entity.getID());
        }

        return buildingIDs;
    }

    public boolean isMapHuge() {
        return isMapHuge;
    }

    public boolean isMapMedium() {
        return isMapMedium;
    }

    public boolean isMapSmall() {
        return isMapSmall;
    }

    /**
     * include road & hydrant.
     *
     * @return {@code StandardEntity} collection
     */
    public Collection<StandardEntity> getRoads() {
        return worldInfo.getEntitiesOfType(StandardEntityURN.ROAD,
                StandardEntityURN.HYDRANT);
    }

    public List<EntityID> getRoadsList() {
        List<EntityID> list = new ArrayList<EntityID>();
        for (StandardEntity entity : getRoads()) {
            list.add(entity.getID());
        }
        return list;
    }

    public RoadModel getRoadModel(EntityID roadID) {
        return apolloRoadIdMap.get(roadID);
    }

    public <T extends IComponent> T getComponent(Class<T> c) {
//        for (IComponent component : components) {
//            if (c.isInstance(component)) {
////                return c.cast(component);
//                System.out.println("c.isInstance(component). component:" + component.getClass().getName());
//                break;
//            }
//        }
//        if (c.isInstance(new RoadInfoComponent(this, agentInfo, worldInfo, scenarioInfo, moduleManager, developData))) {
//            //System.out.println("This is RoadInfoComponent");
//            return c.cast(new RoadInfoComponent(this, agentInfo, worldInfo, scenarioInfo, moduleManager, developData));
//        } else if (c.isInstance(new BuildingInfoComponent(this, scenarioInfo, agentInfo, worldInfo))) {
//            //System.out.println("This is BuildingInfoComponent");
//            return c.cast(new BuildingInfoComponent(this, scenarioInfo, agentInfo, worldInfo));
//        } else if (c.isInstance(new PropertyComponent(this))) {
//            return c.cast(new PropertyComponent(this));
//        } else if (c.isInstance(new CivilianInfoComponent(this, scenarioInfo, agentInfo, worldInfo))) {
//            //System.out.println("This is CivilianInfoComponent");
//            return c.cast(new CivilianInfoComponent(this, scenarioInfo, agentInfo, worldInfo));
//        }

        ///////////////////////////////////////
        try {
            for (IComponent component : components) {
                if (c.isInstance(component)) {
                    return c.cast(component);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Component not available for:" + c);

    }

    @SuppressWarnings("rawtypes")
    public StandardAgent getSelf() {
        return self;
    }

    public void printData(String s) {
        System.out.println("Time:" + agentInfo.getTime() + " Me:" + agentInfo.me() + " \t- " + s);
    }

    public void putEntrance(EntityID buildingId, Entrance entrance) {
        entranceRoads.put(entrance.getID(), buildingId);
    }

    public Set<StandardEntity> getAgents() {
        return agents;
    }

    public List<StandardEntity> getAreasInShape(Shape shape) {
        List<StandardEntity> result = new ArrayList<StandardEntity>();
        for (StandardEntity next : getAreas()) {
            Area area = (Area) next;
            if (shape.contains(area.getShape().getBounds2D()))
                result.add(next);
        }
        return result;
    }

    public Set<Road> getRoadsSeen() {
        return roadsSeen;
    }

    public Set<Building> getBuildingsSeen() {
        return buildingsSeen;
    }

    public Set<Blockade> getBlockadesSeen() {
        return blockadesSeen;
    }

    public Paths getPaths() {
        return paths;
    }

    public GraphModule getGraphModule() {
        return graphModule;
    }

    public Set<RoadModel> getApolloRoads() {
        return apolloRoads;
    }

    public Map<EntityID, RoadModel> getApolloRoadIdMap() {
        return apolloRoadIdMap;
    }

    public RoadModel getApolloRoad(EntityID id) {
        return apolloRoadIdMap.get(id);
    }

//    public RoadInfoComponent getRoadInfoComponent(){
//        return (new RoadInfoComponent(this, agentInfo, worldInfo, scenarioInfo, moduleManager, developData));
//    }

    public Set<EntityID> getPossibleBurningBuildings() {
        return possibleBurningBuildings;
    }

    public void setPossibleBurningBuildings(Set<EntityID> possibleBurningBuildings) {
        this.possibleBurningBuildings = possibleBurningBuildings;
    }

    public void addNewCivilian(Civilian civilian) {
//        worldInfo.getRawWorld().addEntityImpl(civilian);//todo or should be worldInfo.addEntity(civilian);
        getComponent(PropertyComponent.class).addEntityProperty(civilian, getTime());
        getComponent(CivilianInfoComponent.class).setInfoMap(civilian.getID());
    }


    private void reflectMessage(MessageManager messageManager) {
        Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
        changedEntities.add(this.agentInfo.getID());
        int time = this.agentInfo.getTime();
        int receivedTime = -1;
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageBuilding.class)) {
            MessageBuilding mb = (MessageBuilding) message;
            if (!changedEntities.contains(mb.getBuildingID())) {
                MessageUtil.reflectMessage(this.worldInfo, mb);
                if (mb.isRadio()) {
                    receivedTime = time - 1;
                } else {
                    receivedTime = time - 5;
                }
                if (agentInfo.me() instanceof FireBrigade) {
                    processBurningBuilding(mb, receivedTime);
                }
            }
//            this.sentTimeMap.put(mb.getBuildingID(), time + this.sendingAvoidTimeReceived);
        }

//        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageFireBrigade.class)) {
//            MessageFireBrigade mb = (MessageFireBrigade) message;
//            MessageUtil.reflectMessage(this.worldInfo, mb);
//
//            processWaterMessage(mb.getAction(), mb.getTargetID());
//        }
    }

    private void processBurningBuilding(MessageBuilding burningBuildingMessage, int receivedTime) {
        Building building;
        building = (Building) this.getEntity(burningBuildingMessage.getBuildingID());
        if (propertyComponent.getPropertyTime(building.getFierynessProperty()) < receivedTime) {
//            if (building.isFierynessDefined() && building.getFieryness() == 8 && burningBuilding.getFieryness() != 8) {
//                System.out.println("aaaa");
//            }
//            if (building.getID().getValue() == 25393) {
//                world.printData("BurningBuilding\tSender=" + burningBuilding.getSender().getValue() + " Real Fire=" + (building.isFierynessDefined() ? building.getFieryness() : 0) + " message fire: " + burningBuilding.getFieryness());
//            }
            building.setFieryness(burningBuildingMessage.getFieryness());
            propertyComponent.setPropertyTime(building.getFierynessProperty(), receivedTime);
            building.setTemperature(burningBuildingMessage.getTemperature());
            propertyComponent.setPropertyTime(building.getTemperatureProperty(), receivedTime);
//                if ((platoonAgent instanceof MrlFireBrigade)) {
//                    MrlFireBrigadeWorld w = (MrlFireBrigadeWorld) world;
            BuildingModel mrlBuilding = this.getBuildingsModel(building.getID());
            switch (building.getFieryness()) {
                case 0:
                    mrlBuilding.setFuel(mrlBuilding.getInitialFuel());
                    break;
                case 1:
                    if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.66) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.75));
                    } else if (mrlBuilding.getFuel() == mrlBuilding.getInitialFuel()) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.90));
                    }
                    break;

                case 2:
                    if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.33
                            || mrlBuilding.getFuel() > mrlBuilding.getInitialFuel() * 0.66) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.50));
                    }
                    break;

                case 3:
                    if (mrlBuilding.getFuel() < mrlBuilding.getInitialFuel() * 0.01
                            || mrlBuilding.getFuel() > mrlBuilding.getInitialFuel() * 0.33) {
                        mrlBuilding.setFuel((float) (mrlBuilding.getInitialFuel() * 0.15));
                    }
                    break;
                case 4:
                case 5:
                case 6:
                case 7:
                    mrlBuilding.setWasEverWatered(true);
                    mrlBuilding.setEnergy(0);
                    break;

                case 8:
                    mrlBuilding.setFuel(0);
                    break;
            }
            mrlBuilding.setEnergy(building.getTemperature() * mrlBuilding.getCapacity());
//                    world.printData("burningBuilding:" + building+" f:"+burningBuildingMessage.getFieriness()+" temp:"+burningBuildingMessage.getTemperature());
//                }
            //updating burning buildings set
            if (building.getFieryness() > 0 && building.getFieryness() < 4) {
                this.getBurningBuildings().add(building.getID());
                mrlBuilding.setIgnitionTime(this.getTime());
            } else {
                this.getBurningBuildings().remove(building.getID());
            }
            mrlBuilding.updateValues(building);
        }
    }


    public Set<EntityID> getBurningBuildings() {
        return burningBuildings;
    }

    /**
     * Gets heard civilians at current cycle;<br/>
     * <br/>
     * <b>Note: </b> At each cycle the list will be cleared
     *
     * @return EntityIDs of heard civilians
     */
    public Set<EntityID> getHeardCivilians() {
        return heardCivilians;
    }

    /**
     * add civilian who speak of it was heard in current cycle!
     *
     * @param civID EntityID of civilian
     */
    public void addHeardCivilian(EntityID civID) {
//        MrlPersonalData.VIEWER_DATA.setHeardPositions(civID, getSelfLocation());

        if (!heardCivilians.contains(civID)) {
            heardCivilians.add(civID);
        }
    }

    public int getVoiceRange() {
        return scenarioInfo.getRawConfig().getIntValue(ApolloConstants.VOICE_RANGE_KEY);
    }

    public List<StandardEntity> getEntities(Set<EntityID> entityIDs) {
        List<StandardEntity> result = new ArrayList<StandardEntity>();
        for (EntityID next : entityIDs) {
            result.add(getEntity(next));
        }
        return result;
    }

    public List<StandardEntity> getEntities(List<EntityID> entityIDs) {
        List<StandardEntity> result = new ArrayList<StandardEntity>();
        for (EntityID next : entityIDs) {
            result.add(getEntity(next));
        }
        return result;
    }


    public int getViewDistance() {
        return scenarioInfo.getRawConfig().getIntValue(ApolloConstants.MAX_VIEW_DISTANCE_KEY);
    }

    /**
     * 判断是否能看见 需要视野内 & 小于视野距离
     *
     * @param entity
     * @return
     */
    public boolean canSee(StandardEntity entity) {
        boolean canSee;
        if (changes != null) {
            canSee = changes.contains(entity.getID())
                    && getDistance(getSelfPosition().getID(), entity.getID()) < getViewDistance();
        } else {
            canSee = true;
        }
        return canSee;
    }
    public boolean inRange(StandardEntity entity){
        return getDistance(getSelfPosition().getID(), entity.getID()) < getViewDistance();
    }

    // Jingyi Lu
    public boolean isInSameArea(Human human) {
        if (human == null) {
            return false;
        } else {
            EntityID selfID = this.getSelfPosition().getID();
            EntityID testID = human.getID();
            if (selfID.equals(testID)) {
                return true;
            } else {
                return true;
            }
        }
    }

    /**
     *
     * @param threshold
     *            人数阀值
     * @param times
     *            持续时间
     * @return
     */
    public boolean isSameAgentGather(int threshold, int times) {
        int nearNum = 0;
        for (int i : sameAgentNear.values()) {
            if (i > times) {
                nearNum++;
            }
        }
        if (nearNum > threshold) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否扎堆
     *
     * @return
     */
    public boolean isTooMuchPeople() {// 判断是否扎堆
        for (Map.Entry<EntityID, Integer> pf : PFNearby.entrySet()) {
            if (pf.getValue() >= 3) {// 有一个PF见的次数超过3次，判断扎堆
                return true;
            }
        }
        return false;
    }

    /**
     * 更新连续几个周期都在身边的智能体
     */
    protected void updateSameAgentNear() {
        List<StandardEntity> sameAgents = new ArrayList<StandardEntity>();
//        List<PoliceForce> samePFs = new ArrayList<>();
        Set<Human> temp = new HashSet<Human>(); //每周期更新一次
        if (getSelfHuman() instanceof AmbulanceTeam) {
            sameAgents.addAll(getAmbulanceTeams());
        } else if (getSelfHuman() instanceof FireBrigade) {
            sameAgents.addAll(getFireBrigades());
        } else if (getSelfHuman() instanceof PoliceForce) {
            sameAgents.addAll(getPoliceForces());
//            samePFs.addAll(getPoliceForceList());

//            for (PoliceForce pf : this.getPoliceForceList()) {
//                if (pf.getID().equals(this.getSelf().getID())) {// 就是自己
//                    continue;
//                }
//                if (PFNearby.containsKey(pf.getID())) {// 列表中已经有这个PF
//                    if (this.canSee(pf) && this.isInSameArea(pf)) {// 发现可以看见他，并且在同一地点上
//                        PFNearby.put(pf.getID(), PFNearby.get(pf.getID()) + 1);
//                    } else {// 看不见他，或者不在同一个地点
//                        PFNearby.put(pf.getID(), 0);
//                    }
//                } else {// 列表中没有这个PF
//                    PFNearby.put(pf.getID(), 0);
//                }
//            }
        }

        // 在视线内
        for (StandardEntity agent : sameAgents) {
            if (canSee(agent)) {
                temp.add((Human) agent);
            }
        }

        // 更新附近同类Agent
        List<Human> removeList = new ArrayList<Human>();
//		System.out.println("Agent Id : " + self.getID() + "\tsameAgentNear.keyset().size = " + sameAgentNear.keySet().size());
        for (Human h : sameAgentNear.keySet()) {
//			System.out.println("Time is" + this.time + "\tAgent Id : " + self.getID() + "\t进入循环");//TODO 如何获得系统周期 2016 0828
            // 仍然出现在身边
            if (temp.contains(h)) {
                int num = sameAgentNear.get(h) + 1;
                sameAgentNear.put(h, num);
                temp.remove(h);
            } else {
                //在移除列表加入当前待移除智能体
                removeList.add(h);
            }
        }
        //将移除列表的智能体全部移除
        for (Human h : removeList) {
            sameAgentNear.remove(h);
        }

        // 加入新来的
        for (Human h : temp) {
            sameAgentNear.put(h, 1);
        }

        //更新最大聚集事件
        if (this.biggestGatherEvent[0] <= sameAgentNear.keySet().size()) {
            biggestGatherEvent[0] = sameAgentNear.keySet().size();
            biggestGatherEvent[1] = this.agentInfo.getTime();
            if (!historyLocation.isEmpty())
                historyLocation.clear();
            historyLocation.add(getSelfLocation());
        }
        if (sameAgentNear.keySet().size() > 1) {
            lastMassiveGatherLocation = this.getSelfLocation();
            lastMassiveGatherTime = this.agentInfo.getTime();
        }
        companions.add(sameAgentNear.keySet().size());//记录这一周期身边相同的智能体数
    }


    private boolean isInitTimeGather; // 初始期大范围聚集

    public boolean isInitTimeGather() {
        return isInitTimeGather;
    }

    public boolean isGather() {
       // System.out.println("Time: " + this.getTime() + " Agent Id:" + self.getID() + "\t调用了world.isGather()");
        int nearNum = 0;
        for (int i : sameAgentNear.values()) {
            if (i > 3) {
                nearNum++;
            }
        }

        if (this.getTime() < 4 && sameAgentNear.keySet().size() > 1) {//added   20160828
            return true;
        }
        if (this.agentInfo.getTime() < 6 && sameAgentNear.keySet().size() > 10)
            isInitTimeGather = true;
        if (nearNum > 1) {
            return true;
        }
        return false;
    }

    public rescuecore2.misc.Pair<Integer, Integer> getSelfLocation() {
        return worldInfo.getLocation(agentInfo.getID());
    }

    /**
     * 判断我是否还需要维持上一个任务
     *
     * @return 返回true，继续工作，返回false任务切换
     */
    public boolean shouldIContinueTask() {
        List<EntityID> possiblePF = new ArrayList<EntityID>();
        for (Map.Entry<EntityID, Integer> pf : PFNearby.entrySet()) {
            if (pf.getValue() >= 3) {// 有一个PF见的次数超过3次，判断扎堆
                possiblePF.add(pf.getKey());// 加入列表
            }
        }
        possiblePF.add(this.getSelf().getID());// 加入自身
        if (possiblePF.size() <= 1) {// 本身就我一个人
            return true;
        }
        Collections.sort(possiblePF, new EntityIDComparator());
        if (!this.getSelf().getID().equals(possiblePF.get(0))) {// 自己不是最小编号的
            return true;
        } else {
            return false;
        }
    }

    /**
     * 将里面所有PF跟随次数清0
     */
    public void resetPFNearby() {
        for (Map.Entry<EntityID, Integer> pf : PFNearby.entrySet()) {
            pf.setValue(0);
        }
    }

    public boolean shouldILeave(int threshold, int times) {//避免PF扎堆
        List<EntityID> humanIDs = new ArrayList<EntityID>();
        int nearNum = 0;
        for (Map.Entry<Human, Integer> entry : sameAgentNear.entrySet()) {//Entry<Human, Integer> --- <身边的同类Agent,Agent连续出现次数>
            int i = entry.getValue();
            if (i > times) {
                nearNum++;
                humanIDs.add(entry.getKey().getID());
            }
        }
        if (nearNum > threshold) {// 超过阀值
            Collections.sort(humanIDs, new EntityIDComparator());
            if (humanIDs.get(0).getValue() == this.getSelf().getID().getValue()) {// 自己该留下
                return false;
            } else {// 自己该走
                // System.out.println("我该走了");
                sameAgentNear.clear();// 清空列表，避免重复删除任务
                return true;
            }
        }
        return false;
    }


    /**
     * 近10个周期无同伴比例超过0.5，且最后一次聚集发生在3周期以前，则返回true
     *
     * @return
     * @author Yangjiedong
     */
    public boolean isCompanionDecrease() {
        List<Integer> numerators = new ArrayList<Integer>();
        List<Integer> denominators = new ArrayList<Integer>();
        int numerator = 0; //分子
        int index = 0; //距离现在index个周期以前出现过4聚集
        System.out.println("Time: " + this.agentInfo.getTime() + " Agent Id:" + this.getSelfHuman().getID() + "\t companions size is : " + companions.size());
        System.out.println("companions :" + companions);
        if (companions.size() < 12)
            return false;
        for (int i = 0; i < companions.size() - 11; i++)
            if (companions.get(i) > 1) {
                numerator++;
                if (companions.get(i) > 3)
                    index = companions.size() - (i + 1);
            }
        for (int i = companions.size() - 11; i < companions.size(); i++) {
            if (companions.get(i) > 1) {
                numerator++;
                if (companions.get(i) > 3)
                    index = companions.size() - (i + 1);
            }
            numerators.add(numerator);
            denominators.add(i + 1);
        }
        double[] fractions = new double[numerators.size()];
        for (int i = 0; i < fractions.length; i++)
            fractions[i] = (1.0 * numerators.get(i)) / denominators.get(i);
        int noCompNum = 0;
        int hasCompNum = 0;
        for (int i = 1; i < fractions.length; i++)
            if (fractions[i - 1] > fractions[i]) {
                noCompNum++;
            } else {
                hasCompNum++;
            }
        System.out.print("fractions : ");
        for (int i = 0; i < fractions.length; i++)
            System.out.printf(" %.2f ", fractions[i]);
        System.out.println();
        System.out.println("no companion number : " + noCompNum + "  has companion number : " + hasCompNum + " indx : " + index);
        if ((1.0 * noCompNum) / (fractions.length - 1) > 0.75 && index > 2)
            return true;
        return false;
    }

    int judgeTime; // 孤独判断周期数

    public void setJudgeTime(int time) {
        judgeTime = time;
    }

    public int getJudgeTime() {
        return this.judgeTime;
    }

    public List<Pair<Integer, Integer>> getHistoryLocation() {
        return this.historyLocation;
    }

    public int[] getBiggestGatherEvent() {
        return this.biggestGatherEvent;
    }

    public boolean isBuildingBurnt(Building building) {
        if (building == null || !building.isFierynessDefined()) {
            return false;
        }
        int fieriness = building.getFieryness();

        return fieriness != 0 && fieriness != 4 && fieriness != 5;
    }

    public int getMyDistanceTo(StandardEntity entity) {
        return getDistance(getSelfPosition(), entity);
    }

    public int getMyDistanceTo(EntityID entityID) {
        return getDistance(getSelfPosition(), getEntity(entityID));
    }

    public Set<FireBrigade> getFireBrigadesSeen() {
        return fireBrigadesSeen;
    }

    protected CommandTypes getCommandType(String str) {
        if (str.equalsIgnoreCase("Move")) {
            return CommandTypes.Move;

        } else if (str.equalsIgnoreCase("Move To Point")) {
            return CommandTypes.MoveToPoint;

        } else if (str.equalsIgnoreCase("Random Walk")) {
            return CommandTypes.RandomWalk;

        } else if (str.equalsIgnoreCase("Rest")) {
            return CommandTypes.Rest;

        } else if (str.equalsIgnoreCase("Rescue")) {
            return CommandTypes.Rescue;

        } else if (str.equalsIgnoreCase("Load")) {
            return CommandTypes.Load;

        } else if (str.equalsIgnoreCase("Unload")) {
            return CommandTypes.Unload;

        } else if (str.equalsIgnoreCase("Clear")) {
            return CommandTypes.Clear;

        } else if (str.equalsIgnoreCase("Extinguish")) {
            return CommandTypes.Extinguish;
        }

        return CommandTypes.Empty;
    }

    public int getLastCommand() {
        return lastCommand;
    }


}
