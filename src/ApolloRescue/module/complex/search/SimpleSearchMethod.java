package ApolloRescue.module.complex.search;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.entities.BuildingModel;
import ApolloRescue.module.universal.entities.Path;
import ApolloRescue.module.universal.entities.RoadModel;
import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import rescuecore2.standard.entities.Road;

import javax.swing.text.html.parser.Entity;
import java.util.*;

public class SimpleSearchMethod {
    private boolean searchInPartition = false;
    private List<Path> shouldDiscoverPaths;
    private List<Path> discoveredPaths;

    private Path pathInProgress;
    private ApolloWorld world;

    protected Set<Path> validPaths;


    public SimpleSearchMethod(ApolloWorld world, AgentInfo ai, WorldInfo wi) {
        this.world = world;
        validPaths = new HashSet<>();
    }

    //    @Override
    public void update() {
//        if (isPartitionChanged()) {
//            resetSearch();
//        }

        if (searchInPartition) {
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
        } else {
            validPaths.addAll(world.getPaths());
        }
        setShouldDiscoverPaths();
    }

    //    @Override
    public void initialize() {
        discoveredPaths = new ArrayList<>();

        shouldDiscoverPaths = new ArrayList<>();

        setPathBuildingsMap();
        pathInProgress = null;
    }

    public Path getNextPath() {
        Path nextPath = null;
        if (pathInProgress == null) {
            if (ApolloConstants.DEBUG_SEARCH)
                world.printData("no path in progress... choose present path.");
            if (!shouldDiscoverPaths.isEmpty()) {
                nextPath = getMyPath();
                if (!shouldDiscoverPaths.contains(nextPath)) {
                    nextPath = shouldDiscoverPaths.get(0);
                }
            } else {
                if (ApolloConstants.DEBUG_SEARCH)
                    world.printData("shouldDiscoverPath is empty!!! going to reset it.\nDiscovered paths:" + discoveredPaths.size() + "\nValid paths:" + validPaths.size());
                discoveredPaths.clear();
                setShouldDiscoverPaths();
            }
//                nextPath = shouldDiscoverPaths.get(0);
//            } else {
//                nextPath = null;
//            }
        } else {
            Set<Path> neighbours = pathInProgress.getNeighbours();
            for (Path path : neighbours) {
                if (shouldDiscoverPaths.contains(path)) {
                    nextPath = path;
                    break;
                }
            }
            if (nextPath == null) {
                if (shouldDiscoverPaths.isEmpty()) {
                    if (ApolloConstants.DEBUG_SEARCH)
                        world.printData("shouldDiscoverPath is empty!!! going to reset it.\nDiscovered paths:" + discoveredPaths.size() + "\nValid paths:" + validPaths.size());
                    discoveredPaths.clear();
                    setShouldDiscoverPaths();
                } else {
                    nextPath = randomWalk(shouldDiscoverPaths);
                }
            }
        }
        if (nextPath != null) {
            shouldDiscoverPaths.remove(nextPath);
            if (!discoveredPaths.contains(nextPath))
                discoveredPaths.add(nextPath);
        }
        pathInProgress = nextPath;
        return pathInProgress;
    }

    private void setPathBuildingsMap() {
//        Set<Building> buildings = new HashSet<>();
//        for (Path path : world.getPaths()) {
//            buildings.clear();
//            for (Area area : path.getBuildings()) {
//                buildings.add((Building) area);
//            }
//        }
    }

    private void setShouldDiscoverPaths() {
        shouldDiscoverPaths.clear();
        if (searchInPartition) {
            shouldDiscoverPaths.addAll(validPaths);
        } else {
            shouldDiscoverPaths.addAll(world.getPaths());
        }
        shouldDiscoverPaths.removeAll(discoveredPaths);
    }

    private Path getMyPath() {
        Path myPath;
        if (world.getSelfPosition() instanceof Road) {
            RoadModel road = world.getRoadModel(world.getSelfPosition().getID());
            myPath = road.getPaths().get(0);
        } else {
            BuildingModel buildingmodel = world.getBuildingModel(world.getSelfPosition().getID());
            Road roadEntrance = buildingmodel.getEntrances().get(0).getNeighbour();
            RoadModel road = world.getRoadModel(roadEntrance.getID());
            myPath = road.getPaths().get(0);
        }
        if (myPath == null) {
            if (ApolloConstants.DEBUG_SEARCH)
                world.printData("myPath = null");
        }
        return myPath;
    }

    private void resetSearch() {
        shouldDiscoverPaths.clear();
        pathInProgress = null;
    }

    private Path randomWalk(List<Path> paths){
        int size = paths.size();
        Random random = new Random(System.currentTimeMillis());
        int index = Math.abs(random.nextInt()) % size;
        return paths.get(index);
    }

//    private void setShouldDiscoverBuildings(Path path) {
//        shouldDiscoverBuildings = pathBuildingsMap.get(path);
//    }
    
}
