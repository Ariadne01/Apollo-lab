package ApolloRescue.module.complex;


import ApolloRescue.module.algorithm.clustering.ApolloFireClustering;
import ApolloRescue.module.algorithm.clustering.ApolloFireZone;
import ApolloRescue.module.algorithm.clustering.Cluster;
import ApolloRescue.module.complex.firebrigade.*;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.ConstantComparators;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.entities.BuildingModel;
import ApolloRescue.module.universal.tools.FireZoneBuildingPriorityComparator;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.communication.standard.bundle.information.*;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.BuildingDetector;
//import com.mrl.debugger.remote.VDClient;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.io.Serializable;
import java.util.*;
import java.util.List;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class ApolloBuildingDetector extends BuildingDetector {
    private EntityID result;

    private ApolloFireClustering clustering;

    private int sendTime;
    private int sendingAvoidTimeClearRequest;

    private Collection<EntityID> agentPositions;
    private Map<EntityID, Integer> sentTimeMap;
    private int sendingAvoidTimeReceived;
    private int sendingAvoidTimeSent;
    private FireBrigadeWorld world;
    private Map<EntityID, BuildingProperty> sentBuildingMap;
    private PathPlanning pathPlanning;

    private FireBrigadeTargetSelectorType targetSelectorType = FireBrigadeTargetSelectorType.FULLY_GREEDY;
    private ITargetSelector targetSelector;
    private BuildingModel lastSelectedBuilding;
    private FireZoneBuildingEstimator fireZoneBuildingEstimator;
    private BuildingModel thisTimeTarget;

    private int moveDistance;
    private EntityID lastPosition;
    private int positionCount;

    public ApolloBuildingDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        switch  (si.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.clustering = moduleManager.getModule("BuildingDetector.Clustering", "adf.component.module.algorithm.StaticClustering");
                this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case PRECOMPUTED:
                this.clustering = moduleManager.getModule("BuildingDetector.Clustering", "adf.component.module.algorithm.StaticClustering");
                this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case NON_PRECOMPUTE:
                this.clustering = moduleManager.getModule("BuildingDetector.Clustering", "adf.component.module.algorithm.StaticClustering");
                this.pathPlanning = moduleManager.getModule("ActionExtClear.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
        }
//        this.clustering = new ApolloFireClustering(ai,wi,si,moduleManager,developData);
        this.world = (FireBrigadeWorld) ApolloWorld.load(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
//        this.world = new FireBrigadeWorld(ai,wi,si,moduleManager,developData);
        this.sendTime = 0;
        this.sendingAvoidTimeClearRequest = developData.getInteger("ApolloBuildingDetector.sendingAvoidTimeClearRequest", 5);

        this.agentPositions = new HashSet<>();
        this.sentTimeMap = new HashMap<>();
        this.sentBuildingMap = new HashMap<>();
        this.sendingAvoidTimeReceived = developData.getInteger("ApolloBuildingDetector.sendingAvoidTimeReceived", 3);
        this.sendingAvoidTimeSent = developData.getInteger("ApolloBuildingDetector.sendingAvoidTimeSent", 5);
        this.fireZoneBuildingEstimator=new FireZoneBuildingEstimator(world);

        this.moveDistance = developData.getInteger("ApolloBuildingDetector.moveDistance", 40000);
    }

    @Override
    public BuildingDetector updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if(this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.clustering.updateInfo(messageManager);

        this.world.updateInfo(messageManager);
        this.pathPlanning.updateInfo(messageManager);


        this.reflectMessage(messageManager);
        this.sendEntityInfo(messageManager);

        /*if(this.result != null) {
            Building building = (Building)this.worldInfo.getEntity(this.result);
            if(building.getFieryness() >= 4) {
                messageManager.addMessage(new MessageBuilding(true, building));
            }
        }*/

        this.world.getPossibleBurningBuildings().removeAll(worldInfo.getChanged().getChangedEntities());

        worldInfo.getChanged().getChangedEntities().forEach(id -> {
            StandardEntity entity = worldInfo.getEntity(id);
            if (entity instanceof Building) {
                Building building = (Building) worldInfo.getEntity(id);
                if (building.isFierynessDefined() && building.getFieryness() > 0 /*|| building.isTemperatureDefined() && building.getTemperature() > 0*/) {
                    BuildingProperty buildingProperty = sentBuildingMap.get(id);
                    if (buildingProperty == null || buildingProperty.getFieryness() != building.getFieryness() || buildingProperty.getFieryness() == 1) {
//                        printDebugMessage("burningBuilding: " + building.getID());
                        messageManager.addMessage(new MessageBuilding(true, building));
                        messageManager.addMessage(new MessageBuilding(false, building));
                        sentBuildingMap.put(id, new BuildingProperty(building));
                    }
                }
            } else if (entity instanceof Civilian) {
                Civilian civilian = (Civilian) entity;
                if ((civilian.isHPDefined() && civilian.getHP() > 1000 && civilian.isDamageDefined() && civilian.getDamage() > 0)
                        || ((civilian.isPositionDefined() && !(worldInfo.getEntity(civilian.getPosition()) instanceof Refuge))
                        && (worldInfo.getEntity(civilian.getPosition()) instanceof Building))) {
                    messageManager.addMessage(new MessageCivilian(true, civilian));
                    messageManager.addMessage(new MessageCivilian(false, civilian));
//                    System.out.println(" CIVILIAN_MESSAGE: " + agentInfo.getTime() + " " + agentInfo.getID() + " --> " + civilian.getID());
                }

            }
        });

        int currentTime = this.agentInfo.getTime();
        Human agent = (Human)this.agentInfo.me();
        int agentX = agent.getX();
        int agentY = agent.getY();
        StandardEntity positionEntity = this.worldInfo.getPosition(agent);
        if(positionEntity instanceof Road) {
            Road road = (Road)positionEntity;
            if(road.isBlockadesDefined() && road.getBlockades().size() > 0) {
                for(Blockade blockade : this.worldInfo.getBlockades(road)) {
                    if(blockade == null || !blockade.isApexesDefined()) {
                        continue;
                    }
                    if(this.isInside(agentX, agentY, blockade.getApexes())) {
                        if ((this.sendTime + this.sendingAvoidTimeClearRequest) <= currentTime) {
                            this.sendTime = currentTime;
                            messageManager.addMessage(
                                    new CommandPolice(
                                            true,
                                            null,
                                            agent.getPosition(),
                                            CommandPolice.ACTION_CLEAR
                                    )
                            );
                            break;
                        }
                    }
                }
            }
            if(this.lastPosition != null && this.lastPosition.getValue() == road.getID().getValue()) {
                this.positionCount++;
                if(this.positionCount > this.getMaxTravelTime(road)) {
                    if ((this.sendTime + this.sendingAvoidTimeClearRequest) <= currentTime) {
                        this.sendTime = currentTime;
                        messageManager.addMessage(
                                new CommandPolice(
                                        true,
                                        null,
                                        agent.getPosition(),
                                        CommandPolice.ACTION_CLEAR
                                )
                        );
                    }
                }
            } else {
                this.lastPosition = road.getID();
                this.positionCount = 0;
            }
        }
        return this;
    }



    @Override
    public BuildingDetector calc() {
        try {


            //TODO @MRL extinguishNearbyWhenStuck();

           /* TODO @MRL
           Cluster targetCluster;
            if (world.isCommunicationLess() || world.isCommunicationLow()*//*|| world.getFireClusterManager().getClusters().size() >= 3*//*) {
                targetCluster = world.getFireClusterManager().findNearestCluster((world.getSelfLocation()));
            }else {
                targetCluster = world.getFireClusterManager().findMyBestCluster(lastSelectedBuilding);
            }*/


//          TODO @MRL

            Collection<StandardEntity> entities = this.worldInfo.getEntitiesOfType(
                    StandardEntityURN.BUILDING,
                    StandardEntityURN.GAS_STATION,
                    StandardEntityURN.AMBULANCE_CENTRE,
                    StandardEntityURN.FIRE_STATION,
                    StandardEntityURN.POLICE_OFFICE
            );

            Set<StandardEntity> fireBuildings = new HashSet<>();
            for (StandardEntity entity : entities) {
                if (((Building) entity).isOnFire()) {
                    fireBuildings.add(entity);
                }
            }

            if (this.world.getFireClustering().getClusterNumber() == 0) {
//               System.out.println("the number of clustering is 0!!!!!!!!!!!!!!!");
            }

            if (this.world.getFireClustering().getClusterNumber() > 0) {
                double minDistance = Double.MAX_VALUE;
                int nearestClusterIndex = 0;
                for (int i = 0; i < this.world.getFireClustering().getClusterConvexPolygons().size(); i++) {
                    double distance = Util.distance(this.world.getFireClustering().getClusterConvexPolygons().get(i), worldInfo.getLocation(agentInfo.getID()), false);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestClusterIndex = i;
                    }
                }
                Cluster targetCluster;

                targetCluster = world.getFireClustering().findNearestCluster((worldInfo.getLocation(agentInfo.getID())));
                if(targetCluster.getAllEntities().size()==1){

                }
                if (targetCluster != null) {
                    if (!targetCluster.isControllable() && targetSelectorType.equals(FireBrigadeTargetSelectorType.FULLY_GREEDY)) {
                        targetSelectorType = FireBrigadeTargetSelectorType.DIRECTION_BASED14;
                        //setTargetSelectorApproach();
                    } else if (targetCluster.isControllable() && targetSelectorType.equals(FireBrigadeTargetSelectorType.DIRECTION_BASED14)) {
                        targetSelectorType = FireBrigadeTargetSelectorType.FULLY_GREEDY;
                        //setTargetSelectorApproach();
                    }
                }
               // setTargetSelectorApproach();
		        if(targetCluster == null){
		            System.out.println("the targetCluster is null when I use the findNearestCluster method");
		        }
                if (targetCluster != null) {
		            if(targetSelectorType.equals(FireBrigadeTargetSelectorType.FULLY_GREEDY)) {
                        thisTimeTarget = getHighValueTarget((ApolloFireZone) targetCluster);
                    }
                    else if(targetSelectorType.equals(FireBrigadeTargetSelectorType.DIRECTION_BASED14)){
                        SortedSet<BuildingModel> sortedTarget;
                        sortedTarget = calculateBuildingValue((ApolloFireZone)targetCluster);
                        if(sortedTarget != null && !sortedTarget.isEmpty()) {
                            lastSelectedBuilding= thisTimeTarget;
                            thisTimeTarget = sortedTarget.last();
                        }
                    }
                }

//                ExtinguishTarget extinguishTarget = targetSelector.getTarget(targetCluster);

                if (thisTimeTarget != null) {
                    System.out.println("extinguishTarget is not null");
                    lastSelectedBuilding =thisTimeTarget;
                    findPossibleBurningBuildings(lastSelectedBuilding);
                } else {
                    lastSelectedBuilding = null;
                }


 /*   TODO @MRL
                // explore around last target
                if (exploreManager.isTimeToExplore(fireBrigadeTarget)) {
                    world.getPlatoonAgent().exploreAroundFireSearchManager.execute();
                }
*/
                if (lastSelectedBuilding != null) {
                    this.result = lastSelectedBuilding.getID();
                    world.printData(" YESSSSSS ... target is : " + result);
                } else {
                    world.printData(" no target found in targetCluster so look for target in sample cluster ...");
//                    this.result = this.calcTargetInCluster(nearestClusterIndex);
//                    this.result = null;
                }

            } else {
                this.result = null;
            }
            if (this.result == null) {
                world.printData(" my cluster target is null so look for target in the world ...");
//                this.result = this.calcTargetInWorld();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }
    private SortedSet<BuildingModel> calculateBuildingValue(ApolloFireZone zone) {

        Set<BuildingModel> outerBuildings = zone.getOuterBuildings();
        if(outerBuildings==null){
            System.out.println("zone is null");
        }
        SortedSet<BuildingModel> sortedTarget = new TreeSet<BuildingModel>(new FireZoneBuildingPriorityComparator());
        List<BuildingModel> inDirectionBuildings;
//        Point targetPoint = directionManager.findDirectionPointInMap(zone, fireZoneComponent.getClusters());
        inDirectionBuildings = zone.getBuildingsInDirection();

        fireZoneBuildingEstimator.updateFor(zone, lastSelectedBuilding);

//        if(inDirectionBuildings.isEmpty()) {
            calculateBuildingValueWithOuterBuildings(sortedTarget, outerBuildings);
//        }
//        calculateBuildingValueInDirection(sortedTarget, outerBuildings);
//        updateDirectionLayer(zone, targetPoint);
        return sortedTarget;
    }
    private void calculateBuildingValueWithOuterBuildings(SortedSet<BuildingModel> sortedTarget, Set<BuildingModel> outerBuildings) {
        for(BuildingModel buildingModel : outerBuildings) {
            buildingModel.priority = fireZoneBuildingEstimator.getCost(buildingModel);
            sortedTarget.add(buildingModel);
        }
    }
    private void findPossibleBurningBuildings(BuildingModel lastSelectedBuilding) {

        if (lastSelectedBuilding != null) {
//            Collection<StandardEntity> objectsInRange = worldInfo.getObjectsInRange(lastSelectedBuilding.getSelfBuilding(), range);
            List<BuildingModel> objectsInRange = lastSelectedBuilding.getConnectedBuilding();
            for (BuildingModel building : objectsInRange) {
                if (!building.getSelfBuilding().isOnFire() && worldInfo.getDistance(lastSelectedBuilding.getID(), building.getID()) < scenarioInfo.getPerceptionLosMaxDistance()) {
//                    BuildingModel building = worldHelper.getBuildingModel(entity.getID());
//                    if (agentInfo.getTime() - building.getSensedTime() > resetTime) {
                    world.getPossibleBurningBuildings().add(building.getID());
                }
//                    }
            }
        }

//        if (worldHelper.getPossibleBurningBuildings() != null) {
//            List<Integer> elementList = Util.fetchIdValueFormElementIds(worldHelper.getPossibleBurningBuildings());
//            VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlPossibleBurningBuildingsLayer", (Serializable) elementList);
//        }


    }
    private BuildingModel getHighValueTarget(ApolloFireZone fireCluster) {
        List<BuildingModel> buildings = fireCluster.getBuildings();
        Map<EntityID,Double> buildingCostMap=new HashMap<>();
        BuildingModel targetBuilding = null;
        SortedSet<Pair<EntityID, Double>> sortedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        fireZoneBuildingEstimator.updateFor(fireCluster,lastSelectedBuilding);

        for (BuildingModel building : buildings) {
            if (building.isBurning()) {
                int cost = fireZoneBuildingEstimator.getCost(building);
                building.BUILDING_VALUE = cost;
                building.priority = fireZoneBuildingEstimator.getCost(building);
                buildingCostMap.put(building.getID(), (double) cost);
                System.out.println("Building Id:" + building.getID() + "\t priority = " + building.priority);
//                System.out.println("This world is communication less : " + world.isCommunicationLess());
                sortedBuildings.add(new Pair<EntityID, Double>(building.getID(), building.BUILDING_VALUE));
            }
        }

        if (sortedBuildings != null && !sortedBuildings.isEmpty()) {
            lastSelectedBuilding = thisTimeTarget;
            thisTimeTarget = world.getBuildingsModel(sortedBuildings.first().first());
            targetBuilding = thisTimeTarget;
        }
        System.out.println("now return in this ");
        return targetBuilding;
    }

    private EntityID calcTargetInCluster(int nearestClusterIndex) {
        StandardEntity targetBuilding = null;
        if (nearestClusterIndex != -1) {
            Collection<StandardEntity> elements = this.world.getFireClustering().getClusterEntities(nearestClusterIndex);

//            if (MrlPersonalData.DEBUG_MODE) {
//                List<Integer> elementList = Util.fetchIdValueFormElements(elements);
//                VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlSampleBuildingsLayer", (Serializable) elementList);
//            }

            if (elements == null || elements.isEmpty()) {
                return null;
            }

            Set<StandardEntity> borderBuildings = findBorderElements(elements, world.getFireClustering().getClusterConvexPolygons().get(nearestClusterIndex));

            targetBuilding = findBestBuilding(borderBuildings, world.getFireClustering().getClusterConvexPolygons().get(nearestClusterIndex));
        }
        return targetBuilding != null ? targetBuilding.getID() : null;
    }

    private Set<StandardEntity> findBorderElements(Collection<StandardEntity> elements, Polygon nearestClusterPolygon) {
        Set<StandardEntity> borderElements = new HashSet<>();

        Collection<StandardEntity> entities = worldInfo.getEntitiesOfType(StandardEntityURN.BUILDING);
        elements = entities;
        if (elements != null) {
            elements.forEach(entity -> {
                Building building = (Building) entity;
                int vertexes[] = building.getApexList();
                for (int i = 0; i < vertexes.length; i += 2) {
                    double distance = Util.distance(nearestClusterPolygon, new Pair<>(vertexes[i], vertexes[i + 1]), false);
                    if (distance < scenarioInfo.getPerceptionLosMaxDistance() / 4) {
                        borderElements.add(entity);
                        break;
                    }
//                    if ((bigBorderPolygon.contains(vertexes[i], vertexes[i + 1])) && !(smallBorderPolygon.contains(vertexes[i], vertexes[i + 1]))) {
//                        borderEntities.add(building);
//                        break;
//                    }
                }


//                double distance = Util.distance(nearestClusterPolygon, worldInfo.getLocation(entity.getID()), false);
//                if (distance < scenarioInfo.getPerceptionLosMaxDistance() / 2) {
//                    borderElements.add(entity);
//                }
            });
        }

//        if (MrlPersonalData.DEBUG_MODE) {
//            List<Integer> elementList = Util.fetchIdValueFormElements(borderElements);
//            VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlBorderBuildingsLayer", (Serializable) elementList);
//        }


        return borderElements;
    }

    private StandardEntity findBestBuilding(Set<StandardEntity> borderBuildings, Polygon polygon) {
        StandardEntity bestBuilding = null;
        int minDistance = Integer.MAX_VALUE;

        List<BuildingProperty> buildingPropertyList = new ArrayList<>();
        for (StandardEntity borderEntity : borderBuildings) {
            Building building = (Building) borderEntity;
            int fieryness = building.isFierynessDefined() ? building.getFieryness() : 0;
            int temperature = building.isTemperatureDefined() ? building.getTemperature() : 0;
            BuildingProperty buildingProperty = new BuildingProperty(borderEntity.getID(), fieryness, temperature);
            buildingProperty.setValue(calculateValue(building, polygon));
            buildingPropertyList.add(buildingProperty);
        }

        double maxValue = Double.MIN_VALUE;
        BuildingProperty selectedBuildingProperty = null;
        for (BuildingProperty buildingProperty : buildingPropertyList) {
            if (buildingProperty.getValue() > maxValue) {
                maxValue = buildingProperty.getValue();
                selectedBuildingProperty = buildingProperty;
            }
        }

        if (selectedBuildingProperty == null) {
            StandardEntity nearestBuilding = findNearestBuilding(borderBuildings);
//            System.out.println("Nearest ... " + agentInfo.getID() + "  -->  " + nearestBuilding.getID());
            return nearestBuilding;
        } else {
//            System.out.println("BestValue ... " + agentInfo.getID() + "  -->  " + selectedBuildingProperty.getEntityID() + " value: " + selectedBuildingProperty.getValue());
            return worldInfo.getEntity(selectedBuildingProperty.getEntityID());
        }
    }

    private StandardEntity findNearestBuilding(Set<StandardEntity> borderBuildings) {
        StandardEntity bestBuilding = null;
        int minDistance = Integer.MAX_VALUE;
        for (StandardEntity borderEntity : borderBuildings) {
            int distance = worldInfo.getDistance(borderEntity.getID(), agentInfo.getID());
            if (distance < minDistance) {
                minDistance = distance;
                bestBuilding = borderEntity;
            }
        }
        return bestBuilding;
    }


    private static final double INITIAL_COST = 500;
    private static final double AGENT_SPEED = 32000;
    private static final double BASE_PER_MOVE_COST = 30;
    private static final double MAX_DISTANCE_COST = BASE_PER_MOVE_COST * 10;
    private static final double SHOULD_MOVE_COST = BASE_PER_MOVE_COST * 2.2;
    private static final double NOT_IN_CHANGESET_COST = BASE_PER_MOVE_COST * 1.2;


    private double perMoveCost;
    private double shouldMoveCost;
    private double notInChangeSetCost;

    private double calculateValue(Building building, Polygon clusterPolygon) {


        double value = 0;

        double clusterSize = Math.max(clusterPolygon.getBounds2D().getWidth(), clusterPolygon.getBounds2D().getHeight());
        double mapSize = Math.max(worldInfo.getBounds().getWidth(), worldInfo.getBounds().getHeight());
        double worldFireBuildingSituation = clusterSize / mapSize;
        double coefficient = worldFireBuildingSituation;
        this.perMoveCost = BASE_PER_MOVE_COST * coefficient;
        this.shouldMoveCost = SHOULD_MOVE_COST * coefficient;
        this.notInChangeSetCost = NOT_IN_CHANGESET_COST * coefficient;

        if (building.isFierynessDefined()) {
            switch (building.getFieryness()) {
                case 1:
                    value = 1000;
                    break;
                case 2:
                    value = 300;
                    break;
                case 3:
                    value = 100;
                    break;
            }
        }


        // distance and should move    //todo: change with pathPlaner mostafas
        double distance = Util.distance(agentInfo.getX(), agentInfo.getY(), building.getX(), building.getY());
        if (distance > scenarioInfo.getFireExtinguishMaxDistance()) {
            double timeToMove = (distance - scenarioInfo.getFireExtinguishMaxDistance()) / AGENT_SPEED;
            value = (value + 1) / (timeToMove + 0.01);
        } else {
            value = value + 200;
        }


        return value;
    }


    private EntityID calcTargetInWorld() {
        Collection<StandardEntity> entities = this.worldInfo.getEntitiesOfType(
                StandardEntityURN.BUILDING,
                StandardEntityURN.GAS_STATION,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.FIRE_STATION,
                StandardEntityURN.POLICE_OFFICE
        );
        StandardEntity me = this.agentInfo.me();
        List<StandardEntity> agents = new ArrayList<>(worldInfo.getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));
        Set<StandardEntity> fireBuildings = new HashSet<>();
        for (StandardEntity entity : entities) {
            if (((Building)entity).isOnFire()) {
                fireBuildings.add(entity);
            }
        }
        for(StandardEntity entity : fireBuildings) {
            if(agents.isEmpty()) {
                break;
            } else if(agents.size() == 1) {
                if(agents.get(0).getID().getValue() == me.getID().getValue()) {
                    return entity.getID();
                }
                break;
            }
            agents.sort(new DistanceSorter(this.worldInfo, entity));
            StandardEntity a0 = agents.get(0);
            StandardEntity a1 = agents.get(1);

            if(me.getID().getValue() == a0.getID().getValue() || me.getID().getValue() == a1.getID().getValue()) {
                return entity.getID();
            } else {
                agents.remove(a0);
                agents.remove(a1);
            }
        }
        System.out.println("return nothing");
        return null;
    }

    @Override
    public EntityID getTarget() {
        return this.result;
    }

    @Override
    public BuildingDetector precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if(this.getCountPrecompute() >= 2) {
            return this;
        }
        this.world.precompute(precomputeData);
//        this.clustering.precompute(precomputeData);
        this.pathPlanning.precompute(precomputeData);

        return this;
    }

    @Override
    public BuildingDetector resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if(this.getCountPrecompute() >= 2) {
            return this;
        }
        this.world.resume(precomputeData);
        this.clustering.resume(precomputeData);
        this.pathPlanning.resume(precomputeData);

        return this;
    }

    @Override
    public BuildingDetector preparate() {
        super.preparate();
        if (this.getCountPrecompute() >= 2)
        {
            return this;
        }
        return this;
    }

    private boolean isInside(double pX, double pY, int[] apex) {
        Point2D p = new Point2D(pX, pY);
        Vector2D v1 = (new Point2D(apex[apex.length - 2], apex[apex.length - 1])).minus(p);
        Vector2D v2 = (new Point2D(apex[0], apex[1])).minus(p);
        double theta = this.getAngle(v1, v2);

        for(int i = 0; i < apex.length - 2; i += 2) {
            v1 = (new Point2D(apex[i], apex[i + 1])).minus(p);
            v2 = (new Point2D(apex[i + 2], apex[i + 3])).minus(p);
            theta += this.getAngle(v1, v2);
        }
        return Math.round(Math.abs((theta / 2) / Math.PI)) >= 1;
    }

    private double getAngle(Vector2D v1, Vector2D v2) {
        double flag = (v1.getX() * v2.getY()) - (v1.getY() * v2.getX());
        double angle = Math.acos(((v1.getX() * v2.getX()) + (v1.getY() * v2.getY())) / (v1.getLength() * v2.getLength()));
        if(flag > 0) {
            return angle;
        }
        if(flag < 0) {
            return -1 * angle;
        }
        return 0.0D;
    }

    private class DistanceSorter implements Comparator<StandardEntity> {
        private StandardEntity reference;
        private WorldInfo worldInfo;

        DistanceSorter(WorldInfo wi, StandardEntity reference) {
            this.reference = reference;
            this.worldInfo = wi;
        }

        public int compare(StandardEntity a, StandardEntity b) {
            int d1 = this.worldInfo.getDistance(this.reference, a);
            int d2 = this.worldInfo.getDistance(this.reference, b);
            return d1 - d2;
        }
    }

    private void reflectMessage(MessageManager messageManager) {
        Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
        changedEntities.add(this.agentInfo.getID());
        int time = this.agentInfo.getTime();
        for(CommunicationMessage message : messageManager.getReceivedMessageList(MessageBuilding.class)) {
            MessageBuilding mb = (MessageBuilding)message;
            if(!changedEntities.contains(mb.getBuildingID())) {
                MessageUtil.reflectMessage(this.worldInfo, mb);
            }
            this.sentTimeMap.put(mb.getBuildingID(), time + this.sendingAvoidTimeReceived);
        }
    }

    private boolean checkSendFlags(){
        boolean isSendBuildingMessage = true;

        StandardEntity me = this.agentInfo.me();
        if(!(me instanceof Human)){
            return false;
        }
        Human agent = (Human)me;
        EntityID agentID = agent.getID();
        EntityID position = agent.getPosition();
        StandardEntityURN agentURN = agent.getStandardURN();
        EnumSet<StandardEntityURN> agentTypes = EnumSet.of(AMBULANCE_TEAM, FIRE_BRIGADE, POLICE_FORCE);
        agentTypes.remove(agentURN);

        this.agentPositions.clear();
        for(StandardEntity entity : this.worldInfo.getEntitiesOfType(agentURN)) {
            Human other = (Human)entity;
            if(isSendBuildingMessage) {
                if (other.getPosition().getValue() == position.getValue()) {
                    if (other.getID().getValue() > agentID.getValue()) {
                        isSendBuildingMessage = false;
                    }
                }
            }
            this.agentPositions.add(other.getPosition());
        }
        for(StandardEntityURN urn : agentTypes) {
            for(StandardEntity entity : this.worldInfo.getEntitiesOfType(urn)) {
                Human other = (Human) entity;
                if(isSendBuildingMessage) {
                    if (other.getPosition().getValue() == position.getValue()) {
                        if (urn == FIRE_BRIGADE) {
                            isSendBuildingMessage = false;
                        } else if (agentURN != FIRE_BRIGADE && other.getID().getValue() > agentID.getValue()) {
                            isSendBuildingMessage = false;
                        }
                    }
                }
                this.agentPositions.add(other.getPosition());
            }
        }
        return isSendBuildingMessage;
    }

    private void sendEntityInfo(MessageManager messageManager) {
        if(this.checkSendFlags()) {
            Building building = null;
            int currentTime = this.agentInfo.getTime();
            Human agent = (Human) this.agentInfo.me();
            for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
                StandardEntity entity = this.worldInfo.getEntity(id);
                if (entity instanceof Building) {
                    Integer time = this.sentTimeMap.get(id);
                    if (time != null && time > currentTime) {
                        continue;
                    }
                    Building target = (Building) entity;
                    if (!this.agentPositions.contains(target.getID())) {
                        building = this.selectBuilding(building, target);
                    } else if (target.getID().getValue() == agent.getPosition().getValue()) {
                        building = this.selectBuilding(building, target);
                    }
                }
            }
            if (building != null) {
                messageManager.addMessage(new MessageBuilding(true, building));
                this.sentTimeMap.put(building.getID(), currentTime + this.sendingAvoidTimeSent);
            }
        }
    }

    private Building selectBuilding(Building building1, Building building2) {
        if(building1 != null) {
            if(building2 != null) {
                if(building1.isOnFire() && building2.isOnFire()) {
                    if (building1.getFieryness() < building2.getFieryness()) {
                        return building2;
                    } else if (building1.getFieryness() > building2.getFieryness()) {
                        return building1;
                    }
                    if(building1.isTemperatureDefined() && building2.isTemperatureDefined()) {
                        return building1.getTemperature() < building2.getTemperature() ? building2 : building1;
                    }
                } else if (!building1.isOnFire() && building2.isOnFire()) {
                    return building2;
                }
            }
            return building1;
        }
        return building2 != null ? building2 : null;
    }

    private int getMaxTravelTime(Area area) {
        int distance = 0;
        List<Edge> edges = new ArrayList<>();
        for(Edge edge : area.getEdges()) {
            if(edge.isPassable()) {
                edges.add(edge);
            }
        }
        if(edges.size() <= 1) {
            return Integer.MAX_VALUE;
        }
        for(int i = 0; i < edges.size(); i++) {
            for(int j = 0; j < edges.size(); j++) {
                if(i != j) {
                    Edge edge1 = edges.get(i);
                    double midX1 = (edge1.getStartX() + edge1.getEndX()) / 2;
                    double midY1 = (edge1.getStartY() + edge1.getEndY()) / 2;
                    Edge edge2 = edges.get(j);
                    double midX2 = (edge2.getStartX() + edge2.getEndX()) / 2;
                    double midY2 = (edge2.getStartY() + edge2.getEndY()) / 2;
                    int d = this.getDistance(midX1, midY1, midX2, midY2);
                    if(distance < d) {
                        distance = d;
                    }
                }
            }
        }
        if(distance > 0) {
            return (distance / this.moveDistance) + ((distance % this.moveDistance) > 0 ? 1 : 0) + 1;
        }
        return Integer.MAX_VALUE;
    }

    private int getDistance(double fromX, double fromY, double toX, double toY) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        return (int)Math.hypot(dx, dy);
    }

    private void setTargetSelectorApproach() {

        switch (targetSelectorType) {
            case FULLY_GREEDY:
                targetSelector = new GreedyFireBrigadeTargetSelector( world);
                break;
            case DIRECTION_BASED14:
                targetSelector =new HybridFireBrigadeTargetSelector(world);
                break;
        }
    }


}
