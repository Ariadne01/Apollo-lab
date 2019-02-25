package ApolloRescue.module.complex.firebrigade;

import java.util.*;

import ApolloRescue.module.algorithm.clustering.ApolloFireZone;
import ApolloRescue.module.algorithm.clustering.Cluster;
import ApolloRescue.module.universal.ConstantComparators;
import ApolloRescue.module.universal.entities.BuildingModel;
import ApolloRescue.module.algorithm.clustering.FireZoneSize;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;


/**
 * This is basic fire target selector.</br>
 * Choose the best target just for self.</br>
 *
 * Date: Apr 10, 2014
 * @author langley
 *
 */
public class GreedyFireBrigadeTargetSelector extends AbstractFBTargetSelector{
    private final static Log log = LogFactory.getLog(GreedyFireBrigadeTargetSelector.class);

    private FireZoneBuildingEstimator fireZoneBuildingEstimator;
  //  private JobState jobState;

    public GreedyFireBrigadeTargetSelector(FireBrigadeWorld world) {
        super(world);
        this.fireZoneBuildingEstimator = new FireZoneBuildingEstimator(world);
    }

    @Override
    //TODO
    public ExtinguishTarget getTarget(ApolloFireZone targetCluster) {
    	/*if(!shouldGetTarget(targetCluster)){
    		return null;
    	}*/

        ExtinguishTarget fireBrigadeTarget = null;

        if (targetCluster != null) {
            thisTimeTarget = getHighValueTarget( targetCluster);
            if (thisTimeTarget != null) {
                lastTarget = thisTimeTarget;
                fireBrigadeTarget = new ExtinguishTarget(thisTimeTarget,  targetCluster);

            }
        }

        return fireBrigadeTarget;

    }

    private BuildingModel getHighValueTarget(ApolloFireZone fireCluster) {
        List<BuildingModel> buildings = fireCluster.getBuildings();
        Map<EntityID,Double> buildingCostMap=new HashMap<>();
        BuildingModel targetBuilding = null;
        SortedSet<Pair<EntityID, Double>> sortedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        fireZoneBuildingEstimator.updateFor(fireCluster, lastTarget);

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
            lastTarget = thisTimeTarget;
            thisTimeTarget = world.getBuildingsModel(sortedBuildings.first().first());
            targetBuilding = thisTimeTarget;
        }
        System.out.println("now return in this ");
        return targetBuilding;
    }


    @Deprecated
    public ExtinguishTarget getTarget() {
        throw new UnsupportedOperationException("not support");
    }

    //TODO need to test       2016 08 31
    //TODO
    @Override
    public boolean shouldGetTarget(ApolloFireZone targetZone) {
//		System.out.println("Agent Id: " + me.getID() + "\t调用了shouldGetTarget()" + "  Communication High : " + world.isCommunicationHigh());
        if(targetZone != null) {
            if(world.getTime() < 51 && world.isCommunicationHigh() && targetZone.getFireZoneSize() == FireZoneSize.Small) {
                if(targetZone.distance(world.getWorldInfo().getLocation(world.getAgentInfo().getID()).first(), world.getWorldInfo().getLocation(world.getAgentInfo().getID()).second()) >
                        (Math.min(world.getMapHeight(), world.getMapWidth())*2/5)) {
//					System.out.println("判断不需要获取目标");
                    return false;
                }
            }
        }
		/*if(jobState==JobState.MovingToRefuge||jobState==JobState.MovingToHydrant){
			return false;
		}*/

        return true;
    }

}

