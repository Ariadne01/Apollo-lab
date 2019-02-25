package ApolloRescue.module.complex.search;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.complex.component.CivilianInfoComponent;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.entities.BuildingModel;
import ApolloRescue.module.universal.entities.Path;
import ApolloRescue.module.universal.entities.RoadModel;
import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

public class HumanSearchMethod {
    private Set<EntityID> shouldDiscoverBuildings;
    private Set<EntityID> shouldFindCivilians;
    private Set<EntityID> unreachableCivilians;
    private ApolloWorld world;
    private AgentInfo agentInfo;
    private WorldInfo worldInfo;
    private Map<EntityID, Integer> notVisitable;
    private Set<EntityID> validBuildings;

    public HumanSearchMethod(ApolloWorld world, AgentInfo ai, WorldInfo wi) {
        this.world = world;
        agentInfo = ai;
        worldInfo = wi;
        notVisitable = new HashMap<>();
    }


    public void initialize() {
        shouldFindCivilians = new HashSet<>();
        shouldDiscoverBuildings = new HashSet<>();
        unreachableCivilians = new HashSet<>();
        validBuildings = new HashSet<>();
    }

    public void update() {

//        if (searchInPartition) {
//            validBuildings.clear();
//            validPaths.clear();
//            Partition myPartition = world.getPartitionManager().findHumanPartition(world.getSelfHuman());
//            if (myPartition == null) {
//                validBuildings.addAll(world.getBuildingIDs());
//                validPaths.addAll(world.getPaths());
//            } else {
//                Set<Partition> humanPartitionsMap = world.getPartitionManager().findHumanPartitionsMap(world.getSelfHuman());
//
//                for (Partition partition : humanPartitionsMap) {
//                    validBuildings.addAll(partition.getBuildingIDs());
//                    validPaths.addAll(partition.getPaths());
//                }
//
//
//            }
//            shouldDiscoverBuildings.retainAll(validBuildings);
//        } else {
        validBuildings.addAll(world.getBuildingIDs());
//        }


        setShouldFindCivilians();
        shouldFindCivilians.removeAll(unreachableCivilians);
        setShouldDiscoverBuildings();

        removeZeroBrokennessBuildings();
        removeBurningBuildings();
        removeVisitedBuildings();
        if (!(agentInfo.me() instanceof PoliceForce)) {
            removeUnreachableBuildings();
        }
        updateCivilianPossibleValues();
//        ApolloPersonalData.VIEWER_DATA.setCivilianData(agentInfo.getID(), shouldDiscoverBuildings, civilianInProgress, buildingInProgress);

    }

    private void removeUnreachableBuildings() {
        List<EntityID> toRemove = new ArrayList<>();
        BuildingModel BuildingModel;
        for (EntityID bID : shouldDiscoverBuildings) {
            BuildingModel = world.getBuildingModel(bID);
            if (!BuildingModel.isVisitable()) {
                toRemove.add(bID);
            }
        }
        for (EntityID bID : notVisitable.keySet()) {
            if (notVisitable.get(bID) < ApolloConstants.BUILDING_PASSABLY_RESET_TIME) {
                toRemove.add(bID);
            }
        }
        shouldDiscoverBuildings.removeAll(toRemove);

        // ApolloPersonalData.VIEWER_DATA.setUnreachableBuildings(agentInfo.getID(), new HashSet<>(toRemove));
    }

    private void removeVisitedBuildings() {
        shouldDiscoverBuildings.removeAll(world.getVisitedBuildings());
    }

    //    @Override
//    public Path getNextPath() {
//        if (shouldDiscoverPaths.isEmpty()) {
//            return null;
//        }
//        pathInProgress = shouldDiscoverPaths.remove(0);
//        return pathInProgress;
//    }
//
    Area getNextArea() {
        EntityID greatestValue = null;
        double maxValue = 0;
        BuildingModel BuildingModel;
        for (EntityID buildingID : shouldDiscoverBuildings) {
            BuildingModel = world.getBuildingModel(buildingID);
            double value = BuildingModel.getCivilianPossibleValue();
            if (value > maxValue) {
                maxValue = value;
                greatestValue = buildingID;
            }
        }

        if (greatestValue == null) {
            return null;
        }

        return world.getEntity(greatestValue, Area.class);
    }


    Area getBetterTarget(Area presentTarget) {
        Area bestArea = getNextArea();
        BuildingModel presentBuildingTarget = world.getBuildingModel(presentTarget.getID());
        if (bestArea instanceof Building) {
            BuildingModel bestBuilding = world.getBuildingModel(bestArea.getID());
            if (bestBuilding.getCivilianPossibleValue() >= presentBuildingTarget.getCivilianPossibleValue() * 2) {
                return bestArea;
            }
        }
        return null;
    }

    /**
     * set civilian possible value every cycle.
     * number of civilian whom voice of them heard around / time to arrive
     * finally *2 value for buildings that in a same path with current agent.
     */
    private void updateCivilianPossibleValues() {
        BuildingModel BuildingModel;
        for (EntityID bID : shouldDiscoverBuildings) {
            BuildingModel = world.getBuildingModel(bID);
            double civilianPossibleValue = BuildingModel.getCivilianPossibly().size();
            if (civilianPossibleValue != 0) {
                StandardEntity position = world.getSelfPosition();
                double distance = Util.distance(worldInfo.getLocation(agentInfo.getID()), worldInfo.getLocation(BuildingModel.getSelfBuilding()));
                double timeToArrive = distance / ApolloConstants.MEAN_VELOCITY_OF_MOVING;
                if (timeToArrive > 0) {
                    civilianPossibleValue /= timeToArrive;
                    //set double value for buildings that inside current path!
                    if (position instanceof Road) {
                        RoadModel roadModel = world.getRoadModel(position.getID());
                        for (Path path : roadModel.getPaths()) {
                            if (path.getBuildings().contains(BuildingModel.getSelfBuilding())) {
                                civilianPossibleValue *= 2;
                            }
                        }
                    }
                } else {
                    civilianPossibleValue = 0;
                }
            }
            BuildingModel.setCivilianPossibleValue(civilianPossibleValue);
        }
    }


    private void removeBurningBuildings() {
        Building building;
        Set<EntityID> toRemove = new HashSet<>();
        for (EntityID buildingID : shouldDiscoverBuildings) {
            building = (Building) worldInfo.getEntity(buildingID);
            if (building.isFierynessDefined() && building.getFieryness() > 0 && building.getFieryness() != 4) {
                toRemove.add(buildingID);
            }
        }
        if (toRemove.size() > 0) {
            System.out.print("");
        }
        shouldDiscoverBuildings.removeAll(toRemove);
    }

    private void setShouldFindCivilians() {
        shouldFindCivilians.clear();
        for (EntityID civId : world.getAllCivilians()) {
            StandardEntity civEntity = world.getEntity(civId);

            Civilian civilian = (Civilian) civEntity;
            if (civilian == null || !civilian.isPositionDefined()) { // ljy：TODO check: civilian != null
                shouldFindCivilians.add(civId);
            }
        }
    }

    /**
     * Fill buildings that have civilian possibly to discover them!
     * It will get information of possible buildings of civilians whom heard voice of them from CivilianInfoComponent
     */
    private void setShouldDiscoverBuildings() {
        CivilianInfoComponent civilianInfoComponent = world.getComponent(CivilianInfoComponent.class);
        Set<EntityID> possibleBuildings;
        BuildingModel BuildingModel;
        shouldDiscoverBuildings.clear();
        for (EntityID civId : shouldFindCivilians) {
            possibleBuildings = new HashSet<>(civilianInfoComponent.getPossibleBuildings(civId));
            for (EntityID possibleBuildingID : possibleBuildings) {
                BuildingModel = world.getBuildingModel(possibleBuildingID);
//                if(BuildingModel.isVisited()){
//                    continue;
//                }
                BuildingModel.addCivilianPossibly(civId);
                shouldDiscoverBuildings.add(possibleBuildingID);
            }
        }
    }

    private void removeZeroBrokennessBuildings() {
        Building building;
        List<EntityID> toRemove = new ArrayList<>();
        for (EntityID buildingID : shouldDiscoverBuildings) {
            building = (Building) worldInfo.getEntity(buildingID);
            if (building.isBrokennessDefined() && building.getBrokenness() == 0) {
                toRemove.add(buildingID);
            }
        }
        shouldDiscoverBuildings.removeAll(toRemove);
    }
}
