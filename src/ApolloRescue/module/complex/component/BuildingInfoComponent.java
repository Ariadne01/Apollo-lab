package ApolloRescue.module.complex.component;

import java.util.ArrayList;
import java.util.List;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.entities.BuildingModel;
import ApolloRescue.module.universal.entities.RoadModel;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;


public class BuildingInfoComponent implements IComponent{

    private ApolloWorld world;
    protected ScenarioInfo scenarioInfo;
    protected AgentInfo agentInfo;
    protected WorldInfo worldInfo;

    public BuildingInfoComponent(ApolloWorld world, ScenarioInfo scenarioInfo, AgentInfo agentInfo, WorldInfo worldInfo) {
        this.world = world;
        this.scenarioInfo = scenarioInfo;
        this.agentInfo = agentInfo;
        this.worldInfo = worldInfo;
    }


    @Override
    public void init() {

    }

    @Override
    public void update() {


        for (BuildingModel apolloBuilding : world.getBuildingsModel()) {
            if (world.getBuildingsSeen().contains(apolloBuilding.getSelfBuilding())) {
                boolean reachable = false;
                if (apolloBuilding.isOneEntranceOpen(world)) {
                    reachable = true;
                }
                RoadModel apolloRoad;
                if (reachable) {
                    boolean tempReachable = false;
                    for (Road road : BuildingInfoComponent.getEntranceRoads(world, apolloBuilding.getSelfBuilding())) {
                        apolloRoad = world.getApolloRoad(road.getID());
                        if (apolloRoad.isReachable()) {
                            tempReachable = true;
                            break;
                        }
                    }
                    if (!tempReachable) {
                        reachable = false;
                    }
                }
                apolloBuilding.setReachable(reachable);
            } else {
                if (apolloBuilding.getSelfBuilding() instanceof Refuge) {
                    apolloBuilding.resetOldReachable(ApolloConstants.REFUGE_PASSABLY_RESET_TIME);
                } else {
                    apolloBuilding.resetOldReachable(ApolloConstants.BUILDING_PASSABLY_RESET_TIME);
                }
            }
            apolloBuilding.getCivilianPossibly().clear();

            //the following instruction remove Burnt buildings from visitedBuildings and add it into emptyBuildings list.
            if (isBuildingBurnt(apolloBuilding.getSelfBuilding())) {
                world.setBuildingVisited(apolloBuilding.getID(), false);
            }
        }



    }

    private boolean isBuildingBurnt(Building building) {
        if (building == null || !building.isFierynessDefined()) {
            return false;
        }
        int fieriness = building.getFieryness();

        return fieriness != 0 && fieriness != 4 && fieriness != 5;
    }


    /**
     * Returns a list of {@link rescuecore2.standard.entities.Road} containing roads that ends to {@code building}
     *
     * @param world
     * @param building building to find entrance roads
     * @return List of entrance roads
     * @author Siavash
     */
    public static List<Road> getEntranceRoads(ApolloWorld world, Building building) {
        ArrayList<Road> entranceRoads = new ArrayList<Road>();
        if (building != null && building.getNeighbours() != null) {
            for (EntityID entityID : building.getNeighbours()) {
                Area area = (Area) world.getEntity(entityID);
                if (area instanceof Road) {
                    entranceRoads.add((Road) area);
                }
            }
        }
        return entranceRoads;


        // throw new NotImplementedException();
    }

    public static List<Area> getEntranceAreas(ApolloWorld world, Building building) {
        ArrayList<Area> entranceAreas = new ArrayList<Area>();
        if (building != null && building.getNeighbours() != null) {
            for (EntityID entityID : building.getNeighbours()) {
                Area area = (Area) world.getEntity(entityID);
                entranceAreas.add(area);
            }
        }
        return entranceAreas;
    }

    /**
     * check is this building have fieriness 1,2,3,6,7,8 or not!
     * this building probably have no alive human!
     * @param building building that want know have this condition or not
     * @return answer
     */
    public static boolean hasPossibleAliveHuman (Building building) {
        return (building.isFierynessDefined() && !(building.getFieryness() == 0 || building.getFieryness() == 4 || building.getFieryness() == 5));
    }



}
