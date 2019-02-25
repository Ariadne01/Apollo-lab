package ApolloRescue.module.algorithm.clustering;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.complex.firebrigade.FireBrigadeWorld;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.entities.BuildingModel;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import javolution.util.FastSet;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExtinguishHelper {
    public static double WATER_COEFFICIENT = 20f;
    public static int FLOOR_HEIGHT = 7;
    private static final int EXTINGUISH_DISTANCE_THRESHOLD = 5000;

    protected static double getBuildingEnergy(int buildingCode, int groundArea, int floors, double temperature) {
        return temperature * getBuildingCapacity(buildingCode, groundArea, floors);
    }

    protected static double getBuildingCapacity(int buildingCode, int groundArea, int floors) {
        double thermoCapacity;
        switch (buildingCode) {
            case 0:
                //wooden
                thermoCapacity = 1.1;
                break;
            case 1:
                //steel
                thermoCapacity = 1.0;
                break;
            default:
                //concrete
                thermoCapacity = 1.5;
                break;
        }
        return thermoCapacity * groundArea * floors * FLOOR_HEIGHT;
    }

    public static int getWaterNeeded(int groundArea, int floors, int buildingCode, double temperature, double finalTemperature) {
        int waterNeeded = 0;
        double currentTemperature = temperature;
        int step = 500;
        while (true) {
            currentTemperature = waterCooling(groundArea, floors, buildingCode, currentTemperature, step);
            waterNeeded += step;
            if (currentTemperature <= finalTemperature){
                break;
            }
        }
        if (ApolloConstants.DEBUG_WATER_COOLING) {
//            System.out.println("water cooling predicts: " + waterNeeded);
        }
        return waterNeeded;
    }


    private static double waterCooling(int groundArea, int floors, int buildingCode, double temperature, int water) {
        if (water > 0) {
            double effect = water * WATER_COEFFICIENT;
            return (getBuildingEnergy(buildingCode, groundArea, floors, temperature) - effect) / getBuildingCapacity(buildingCode, groundArea, floors);
        } else
            throw new RuntimeException("WTF water=" + water);
    }

    public static int waterNeededToExtinguish(BuildingModel building) {
        return getWaterNeeded(building.getSelfBuilding().getGroundArea(), building.getSelfBuilding().getFloors(),
                building.getSelfBuilding().getBuildingCode(), building.getEstimatedTemperature(), 20);
    }

//    public static int calculateWaterPower(WorldInfo world, Building building) {
//        return Math.min(((FireBrigade) world.getSelfHuman()).getWater(), Math.min(world.getMaxPower(), Math.max(500, waterNeededToExtinguish(building))));
//    }
//
    /**
     * 计算需要多少水来灭火
     * @param world
     * @param
     * @return
     */
    /*public static int getPower(FireBrigadeWorld world, BuildingModel building) {
    	if(!building.isBurning())
    	{
    		return Math.min(((FireBrigade) world.getSelfHuman()).getWater(), Math.min(world.getMaxPower(), Math.max(500, waterNeededToExtinguish(building))));
    	}
    	if(!world.canSee(building.getSelfBuilding())&&building.getEstimatedFieryness()==3)
    	{
    		return Math.min(((FireBrigade) world.getSelfHuman()).getWater(), Math.min(world.getMaxPower(), Math.max(500, waterNeededToExtinguish(building))));
    	}
        return Math.min(((FireBrigade) world.getSelfHuman()).getWater(),world.getMaxPower());
    }

    public static Set<Building> getBuildingsInMyExtinguishRange(WorldInfo world) {
        Set<Building> result = new FastSet<Building>();
        int maxExtinguishDistance = world.getMaxExtinguishDistance() - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : world.getObjectsInRange(world.getSelf().getID(), (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Building) {
            	Building building = getBuilding(next.getID());
                if (world.getDistance(next.getID(), world.getSelf().getID()) < maxExtinguishDistance) {
                    result.add(building);
                }
            }
        }
        return result;
    }*/

    public static List<BuildingModel> findBuildingsInExtinguishRangeOf(ApolloWorld world, EntityID source) {
        List<BuildingModel> result = new ArrayList<BuildingModel>();
        int maxExtinguishDistance = world.getMaxExtinguishDistance() - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : world.getObjectsInRange(source, (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Building) {
                BuildingModel building = world.getBuildingModel(next.getID());
                if (world.getDistance(next.getID(), source) < maxExtinguishDistance) {
                    result.add(building);
                }
            }
        }
        return result;
   }

    public static List<EntityID> findAreaIDsInExtinguishRange(WorldInfo world, ScenarioInfo si, EntityID source) {
        List<EntityID> result = new ArrayList<EntityID>();
        int maxExtinguishDistance = si.getFireExtinguishMaxDistance() - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : world.getObjectsInRange(source, (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Area && world.getDistance(next.getID(), source) < maxExtinguishDistance) {
                result.add(next.getID());
            }
        }
        return result;
    }

    public static Set<BuildingModel> getBuildingsInMyExtinguishRange(FireBrigadeWorld world) {
        Set<BuildingModel> result = new FastSet<BuildingModel>();
        int maxExtinguishDistance = world.getMaxExtinguishDistance() - EXTINGUISH_DISTANCE_THRESHOLD;
        for (StandardEntity next : world.getObjectsInRange(world.getSelf().getID(), (int) (maxExtinguishDistance * 1.5))) {
            if (next instanceof Building) {
                BuildingModel building = world.getBuildingModel(next.getID());
                if (world.getDistance(next.getID(), world.getSelf().getID()) < maxExtinguishDistance) {
                    result.add(building);
                }
            }
        }
        return result;
    }


}
