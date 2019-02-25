package ApolloRescue.module.complex.firebrigade;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.algorithm.clustering.ApolloFireZone;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.entities.BuildingModel;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.PathPlanning;
import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

public class FireBrigadeTools {
    private static final int EXTINGUISH_DISTANCE_THRESHOLD = 5000;
    private FireBrigadeWorld world;
    private Human selfHuman;
    private Set<StandardEntity> readyFireBrigades;
    //    private HumanHelper humanHelper;
    private PathPlanning pathPlanning;


    public FireBrigadeTools(FireBrigadeWorld world) {
        this.world = world;
        this.selfHuman = world.getSelfHuman();
//        this.humanHelper = world.getHelper(HumanHelper.class);
        readyFireBrigades = new FastSet<StandardEntity>();
//        this.pathPlanning=pathPlanning;

    }

    private static double calcEffectiveWaterPerCycle(FireBrigadeWorld world, Point targetPoint) {
        int waterQuantity = world.getMaxWater();
        int maxPower = world.getMaxPower();
        int refillRate = world.getWaterRefillRate();
        int waterQuantityPerRefillRate = waterQuantity / refillRate;
        double waterQuantityPerMaxPower = waterQuantity / maxPower;
        return maxPower * (waterQuantityPerMaxPower / (waterQuantityPerMaxPower + waterQuantityPerRefillRate + (Util.findDistanceToNearest(world, world.getWorldInfo().getEntitiesOfType(StandardEntityURN.REFUGE), targetPoint) / ApolloConstants.MEAN_VELOCITY_OF_MOVING)));
    }

    public static int waterNeededToExtinguish(BuildingModel building) {
        return WaterCoolingEstimator.getWaterNeeded(building.getSelfBuilding().getGroundArea(), building.getSelfBuilding().getFloors(),
                building.getSelfBuilding().getBuildingCode(), building.getEstimatedTemperature(), 20);
    }

    public static int waterNeededToExtinguishNotEstimated(Building building) {
        return WaterCoolingEstimator.getWaterNeeded(building.getGroundArea(), building.getFloors(),
//                building.getBuildingCode(), building.getEstimatedTemperature(), 20);
                building.getBuildingCode(), building.getTemperature(), 20);
    }

    public static int calculateWaterPower(FireBrigadeWorld world, BuildingModel building) {
        return Math.min(((FireBrigade) world.getSelfHuman()).getWater(), Math.min(world.getMaxPower(), Math.max(500, waterNeededToExtinguish(building))));
    }

    public static int calculateWaterPower(int remainedWater, int maxPower, BuildingModel building) {
        return Math.min(remainedWater, Math.min(maxPower, Math.max(500, waterNeededToExtinguish(building))));
    }

    public static int calculateWaterPowerNotEstimated(int remainedWater, int maxPower, Building building) {
        return Math.min(remainedWater, Math.min(maxPower, Math.max(500, waterNeededToExtinguishNotEstimated(building))));
    }


    public static Map<EntityID, List<BuildingModel>> findMutualExtinguishLocation(List<BuildingModel> fieryBuildings) {
        throw new UnsupportedOperationException();
    }

    public static Set<EntityID> findAreaIDsInExtinguishRange(WorldInfo worldInfo, ScenarioInfo scenarioInfo, EntityID source) {
        Set<EntityID> result = new HashSet<>();
        int maxExtinguishDistance = scenarioInfo.getFireExtinguishMaxDistance() - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : worldInfo.getObjectsInRange(source, (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Area && worldInfo.getDistance(next.getID(), source) < maxExtinguishDistance) {
                result.add(next.getID());
            }
        }
        return result;
    }

    public static Set<BuildingModel> getBuildingsInMyExtinguishRange(FireBrigadeWorld world) {
        Set<BuildingModel> result = new FastSet<BuildingModel>();
        int maxExtinguishDistance = world.getMaxExtinguishDistance() - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : world.getObjectsInRange(world.getAgentInfo().getID(), (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Building) {
                BuildingModel building = world.getBuildingsModel(next.getID());
                if (world.getDistance(next.getID(), world.getAgentInfo().getID()) < maxExtinguishDistance) {
                    result.add(building);
                }
            }
        }
        return result;
    }

    public static List<BuildingModel> findBuildingsInExtinguishRangeOf(ApolloWorld world, WorldInfo worldInfo, ScenarioInfo scenarioInfo, EntityID source) {
        List<BuildingModel> result = new ArrayList<BuildingModel>();
        int maxExtinguishDistance = scenarioInfo.getFireExtinguishMaxDistance()- EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : worldInfo.getObjectsInRange(source, (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Building) {
                BuildingModel building = world.getBuildingsModel(next.getID());
                if (worldInfo.getDistance(next.getID(), source) < maxExtinguishDistance) {
                    result.add(building);
                }
            }
        }
        return result;
    }

    public static void refreshFireEstimator(ApolloWorld world) {
        for (StandardEntity entity : world.getBuildings()) {
            Building building = (Building) entity;
            int fieryness = building.isFierynessDefined() ? building.getFieryness() : 0;
            int temperature = building.isTemperatureDefined() ? building.getTemperature() : 0;
            BuildingModel apolloBuilding = world.getBuildingsModel(building.getID());



            //age estimator mige khamoosh sode yani khamoosh shode
            if (apolloBuilding.getEstimatedFieryness() > 4) {
                continue;
            }


            apolloBuilding.setEnergy(temperature * apolloBuilding.getCapacity());
            switch (fieryness) {
                case 0:
                    apolloBuilding.setFuel(apolloBuilding.getInitialFuel());
                    if (apolloBuilding.getEstimatedTemperature() >= apolloBuilding.getIgnitionPoint()) {
                        apolloBuilding.setEnergy(apolloBuilding.getIgnitionPoint() / 2);
                    }
                    break;
                case 1:
                    if (apolloBuilding.getFuel() < apolloBuilding.getInitialFuel() * 0.66) {
                        apolloBuilding.setFuel((float) (apolloBuilding.getInitialFuel() * 0.75));
                    } else if (apolloBuilding.getFuel() == apolloBuilding.getInitialFuel()) {
                        apolloBuilding.setFuel((float) (apolloBuilding.getInitialFuel() * 0.90));
                    }
                    break;

                case 2:
                    if (apolloBuilding.getFuel() < apolloBuilding.getInitialFuel() * 0.33
                            || apolloBuilding.getFuel() > apolloBuilding.getInitialFuel() * 0.66) {
                        apolloBuilding.setFuel((float) (apolloBuilding.getInitialFuel() * 0.50));
                    }
                    break;

                case 3:
                    if (apolloBuilding.getFuel() < apolloBuilding.getInitialFuel() * 0.01
                            || apolloBuilding.getFuel() > apolloBuilding.getInitialFuel() * 0.33) {
                        apolloBuilding.setFuel((float) (apolloBuilding.getInitialFuel() * 0.15));
                    }
                    break;

                case 8:
                    apolloBuilding.setFuel(0);
                    break;
            }
        }
    }

    public void calcClusterCondition(FireBrigadeWorld world, ApolloFireZone fireZone) {
        double effectiveWater = calcEffectiveWaterPerCycle(world, fireZone.getCenter());
        int neededWater = fireZone.calcNeededWaterToExtinguish() / 10;
        double totalEffectiveWater = effectiveWater * (world.getWorldInfo().getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE).size() / 2);
        if (neededWater < totalEffectiveWater) {
            fireZone.setCondition(ApolloFireZone.Condition.largeControllable);
        } else {
            fireZone.setCondition(ApolloFireZone.Condition.edgeControllable);
        }

//        MrlPersonalData.VIEWER_DATA.setFireClusterCondition(world.getSelf().getID(), fireCluster);
    }

    //selects some of top elements of the input list
    public SortedSet<Pair<EntityID, Double>> selectTop(int number, SortedSet<Pair<EntityID, Double>> inputList, Comparator<Pair<EntityID, Double>> comparator) {

        SortedSet<Pair<EntityID, Double>> outPut = new TreeSet<Pair<EntityID, Double>>(comparator);
        Pair<EntityID, Double> temp;
        for (int i = 0; i < number; i++) {
            if (!inputList.isEmpty()) {
                temp = inputList.first();
                inputList.remove(temp);
                outPut.add(temp);
            }
        }

        return outPut;
    }


    public List<Area> getAreasInExtinguishRange(EntityID source) {
        List<Area> result = new ArrayList<Area>();
        int maxExtinguishDistance = world.getMaxExtinguishDistance() - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : world.getObjectsInRange(source, (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Area && world.getDistance(next.getID(), source) <= maxExtinguishDistance) {
                result.add((Area) next);
            }
        }
        return result;
    }


    public Set<StandardEntity> getReadyFireBrigades() {
        return readyFireBrigades;
    }

    public BuildingModel findSmallestBuilding(List<BuildingModel> buildings) {
        int minArea = Integer.MAX_VALUE;
        BuildingModel smallestBuilding = null;
        for (BuildingModel building : buildings) {
            if (building.getSelfBuilding().getTotalArea() < minArea) {
                minArea = building.getSelfBuilding().getTotalArea();
                smallestBuilding = building;
            }
        }
        return smallestBuilding;

    }

    public BuildingModel findNewestIgnitedBuilding(List<BuildingModel> buildings) {
        int minTime = Integer.MAX_VALUE;
        int tempTime;
        BuildingModel smallestBuilding = null;
        for (BuildingModel building : buildings) {
            tempTime = world.getTime() - building.getIgnitionTime();
            if (tempTime < minTime) {
                minTime = tempTime;
                smallestBuilding = building;
            }
        }
        return smallestBuilding;
    }

 /*   public boolean amINeededForCluster(ApolloFireZone targetFireZone) {
        boolean needed = false;
        List<Pair<EntityID, Integer>> agentPairs = new ArrayList<>();
        for (StandardEntity fireBrigadeEntity : world.getFireBrigades()) {
            Human brigadeEntity = (Human) fireBrigadeEntity;
            int distance = Util.distance(world.getWorldInfo().getLocation(brigadeEntity.getPosition()), targetFireZone.getCenter());
            agentPairs.add(new Pair<>(fireBrigadeEntity.getID(), distance));
        }
        Collections.sort(agentPairs, ConstantComparators.DISTANCE_VALUE_COMPARATOR);
        double fireBrigadesEnergy = 0;
        int i = 1;
        for (Pair<EntityID, Integer> pair : agentPairs) {
            fireBrigadesEnergy += world.getMaxPower();
            if (pair.first().equals(world.getAgentInfo().getID())) {
                needed = true;
            }
            if (fireBrigadesEnergy >= targetFireZone.getClusterEnergy() / i++) {
                break;
            }
        }
        if (needed) {
            world.printData("I am needed for cluster: " + targetFireZone.getCenter());
        }
        return needed;
    }*/

  /*  public int findNumberOfNeededAgents(FireCluster cluster) {
//        return (int) Math.ceil(cluster.getClusterEnergy() / world.getMaxPower());
        return (int) Math.ceil(cluster.getClusterVolume() * 3 / world.getMaxPower());
    }*/

    /**
     * Effective buildings are those connected buildings that can ignite a put off fire
     *
     * @param building
     */
  /*  public List<BuildingModel> getBuildingsCanIgnite(BuildingModel building) {

        List<BuildingModel> candidateBuildings = new ArrayList<>();

        List<BuildingModel> connectedBuildings = building.getConnectedBuilding();

        double sumOfNeighbourEnergies = 0;
        for (int i = 0; i < connectedBuildings.size(); i++) {
            BuildingModel connectedBuilding = connectedBuildings.get(i);
            if (connectedBuilding.getSelfBuilding().isTemperatureDefined() && connectedBuilding.getSelfBuilding().getTemperature() > 40) {
                candidateBuildings.add(connectedBuilding);
            }
            double radiation = connectedBuilding.getRadiationEnergy();
            double connectionValue = building.getConnectedValues().get(i);
            sumOfNeighbourEnergies += radiation * connectionValue;
        }

        if ((sumOfNeighbourEnergies + building.getEnergy()) / building.getVolume() > 40) {
            return candidateBuildings;
        } else {
            return new ArrayList<>();
        }
    }*/

    public int findNumberOfNeededAgentsBasedOnValue(int numberOfAgents, double sumOfValues, double value, double clusterVolumeRatio, double sumOfVolumeRatios) {
//        final int groupCount = 5;
//        final int agentsInEachGroup = numberOfAgents / groupCount;
        final int agentsInEachGroup = 5;

        double rate = clusterVolumeRatio / sumOfVolumeRatios;
        int needed = (int) (rate * numberOfAgents);


        int groupNeeded = (int) Math.ceil(needed * 1.0d / agentsInEachGroup);
        needed = groupNeeded * agentsInEachGroup;
//        if (needed <= agentsInEachGroup) {
//            needed = agentsInEachGroup;
//        }

        int numberOfNeeded = (int) Math.ceil(value * 3 / world.getMaxPower());

        if (numberOfNeeded < needed) {
            groupNeeded = (int) Math.ceil(numberOfNeeded * 1.0d / agentsInEachGroup);
            needed = groupNeeded * agentsInEachGroup;
        }

        return needed;


    }
}
