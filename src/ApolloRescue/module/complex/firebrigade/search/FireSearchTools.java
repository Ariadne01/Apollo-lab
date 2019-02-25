package ApolloRescue.module.complex.firebrigade.search;

import ApolloRescue.module.universal.entities.BuildingModel;
import javolution.util.FastMap;
import javolution.util.FastSet;
import rescuecore2.worldmodel.EntityID;

import java.util.Map;
import java.util.Set;

public class FireSearchTools {

    /**
     * main method for find maximal covering.
     *
     * @param buildings buildings for find covering.
     * @return a set of target areas Id
     */
    public static Set<EntityID> findMaximalCovering(Set<BuildingModel> buildings) {
        Map<EntityID, Set<BuildingModel>> areasMap = new FastMap<EntityID, Set<BuildingModel>>();
        Map<BuildingModel, Set<EntityID>> buildingsMap = new FastMap<BuildingModel, Set<EntityID>>();

        // fill buildings and possible areas map
        for (BuildingModel building : buildings) {
            buildingsMap.put(building, new FastSet<EntityID>(building.getVisibleFrom()));

            for (EntityID id : building.getVisibleFrom()) {
                Set<BuildingModel> bs = areasMap.get(id);
                if (bs == null) {
                    bs = new FastSet<BuildingModel>();
                }
                bs.add(building);
                areasMap.put(id, bs);
            }
        }

        // call maximal covering method
        Set<EntityID> areas = processMatrix(buildingsMap, areasMap);

        return areas;
    }

    private static Set<EntityID> processMatrix(Map<BuildingModel, Set<EntityID>> buildingsMap, Map<EntityID, Set<BuildingModel>> areasMap) {

        //step one
        Set<BuildingModel> buildingsToRemove = new FastSet<BuildingModel>();
        int i = 0, j;
        for (BuildingModel building1 : buildingsMap.keySet()) {
            j = 0;
            if (!buildingsToRemove.contains(building1)) {
                for (BuildingModel building2 : buildingsMap.keySet()) {
                    if (i > j++ || building1.equals(building2) || buildingsToRemove.contains(building2)) { //continue;
                    } else if (buildingsMap.get(building1).containsAll(buildingsMap.get(building2))) {
                        buildingsToRemove.add(building1);
                    } else if (buildingsMap.get(building2).containsAll(buildingsMap.get(building1))) {
                        buildingsToRemove.add(building2);
                    }
                }
            }
            i++;
        }
        for (BuildingModel b : buildingsToRemove) {
            buildingsMap.remove(b);
            for (Set<BuildingModel> bs : areasMap.values()) {
                bs.remove(b);
            }
        }

        // step two
        i = 0;
        Set<EntityID> areasToRemove = new FastSet<EntityID>();
        for (EntityID area1 : areasMap.keySet()) {
            j = 0;
            if (!areasToRemove.contains(area1)) {
                for (EntityID area2 : areasMap.keySet()) {
                    if (i > j++ || area1.equals(area2) || areasToRemove.contains(area2)) { //continue;
                    } else if (areasMap.get(area1).containsAll(areasMap.get(area2))) {
                        areasToRemove.add(area2);
                    } else if (areasMap.get(area2).containsAll(areasMap.get(area1))) {
                        areasToRemove.add(area1);
                    }
                }
            }
            i++;
        }
        for (EntityID id : areasToRemove) {
            areasMap.remove(id);
            for (Set<EntityID> ids : buildingsMap.values()) {
                ids.remove(id);
            }
        }

        // call again
        if (!areasToRemove.isEmpty() || !buildingsToRemove.isEmpty()) {
            return processMatrix(buildingsMap, areasMap);
        }
        return areasMap.keySet();
    }
}
