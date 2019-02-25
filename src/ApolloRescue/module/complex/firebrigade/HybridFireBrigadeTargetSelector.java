package ApolloRescue.module.complex.firebrigade;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.algorithm.clustering.ApolloFireZone;
import ApolloRescue.module.algorithm.clustering.Cluster;
import ApolloRescue.module.universal.entities.BuildingModel;
import ApolloRescue.module.universal.tools.FireZoneBuildingPriorityComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class HybridFireBrigadeTargetSelector extends AbstractFBTargetSelector{

    private final static Log log = LogFactory.getLog(HybridFireBrigadeTargetSelector.class);

    private DirectionManager directionManager;
    private FireZoneBuildingEstimator fireZoneBuildingEstimator;

    public HybridFireBrigadeTargetSelector(FireBrigadeWorld world) {
        super(world);
        this.directionManager = new DirectionManager(world);
        this.fireZoneBuildingEstimator = new FireZoneBuildingEstimator(world);
    }

    /**
     * @deprecated
     */
    @Override
//    public ExtinguishTarget getTarget(Cluster cluster) {
//        throw new UnsupportedOperationException("not support");
//    }


    public ExtinguishTarget getTarget(ApolloFireZone targetZone) {
        ExtinguishTarget extinguishTarget = null;

        if (targetZone != null) {
            SortedSet<BuildingModel> sortedTarget;
            sortedTarget = calculateBuildingValue(targetZone);

            if (sortedTarget != null && !sortedTarget.isEmpty()) {
                lastTarget = thisTimeTarget;
                thisTimeTarget = sortedTarget.last();
                extinguishTarget = new ExtinguishTarget(thisTimeTarget, targetZone);
            }
        }

        return extinguishTarget;
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

        fireZoneBuildingEstimator.updateFor(zone, lastTarget);

        if(inDirectionBuildings.isEmpty()) {
            calculateBuildingValueWithOuterBuildings(sortedTarget, outerBuildings);
        }
        calculateBuildingValueInDirection(sortedTarget, outerBuildings);
//        updateDirectionLayer(zone, targetPoint);
        return sortedTarget;
    }

    // TODO combine
    private void calculateBuildingValueInDirection(SortedSet<BuildingModel> sortedTarget, Set<BuildingModel> inDirectiomBuildings) {
        for (BuildingModel buildingModel: inDirectiomBuildings) {
            buildingModel.priority = fireZoneBuildingEstimator.getCost(buildingModel);
            sortedTarget.add(buildingModel);
        }
    }

    private void calculateBuildingValueWithOuterBuildings(SortedSet<BuildingModel> sortedTarget, Set<BuildingModel> outerBuildings) {
        for(BuildingModel buildingModel : outerBuildings) {
            buildingModel.priority = fireZoneBuildingEstimator.getCost(buildingModel);
            sortedTarget.add(buildingModel);
        }
    }

//    /**
//     * FIXME update fire zone direction info.
//     * @param zone
//     * @param point
//     */
//    private void updateDirectionLayer(ApolloFireZone zone, Point point) {
//        if(ApolloConstants.LAUNCH_VIEWER) {
//            FireZoneDirection direction = new FireZoneDirection(zone, point);
//            ConvexHullLayer.DIRECTION_FIZEZONE.put(me.getID(), direction);
//        }
//    }

    @Override
    public boolean shouldGetTarget(ApolloFireZone targetZone) {
        return true;
    }

}
