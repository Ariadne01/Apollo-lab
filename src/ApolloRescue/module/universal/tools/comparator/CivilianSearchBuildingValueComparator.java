//package ApolloRescue.module.universal.tools.comparator;
//
//import ApolloRescue.module.universal.ApolloWorld;
//import ApolloRescue.module.universal.Util;
//import rescuecore2.standard.entities.Building;
//import rescuecore2.standard.entities.StandardEntity;
//
//import java.util.Comparator;
//
///**
// * A comparator that sort building civilian value descend and distance ascend.
// */
//public class CivilianSearchBuildingValueComparator implements Comparator<Building> {
//    private StandardEntity reference;
//    private ApolloWorld world;
//
//    /**
//     *
//     * @param reference The reference point to measure distances from.
//     * @param world     The world model.
//     */
//    public CivilianSearchBuildingValueComparator(StandardEntity reference, ApolloWorld world) {
//        this.reference = reference;
//        this.world = world;
//    }
//
//    @Override
//    public int compare(Building a, Building b) {
//    	double v1 = world.getBuildingModel(a.getID()).getCivilianPossibleValue();
//    	double v2 = world.getBuildingModel(b.getID()).getCivilianPossibleValue();
//        int d1 = Util.getDistance(reference, a);
//        int d2 = Util.getDistance(reference, b);
//
//        if(v1 - v2 > 0) {
//        	return -1;
//        }
//
//        if(v1 - v2 < 0) {
//        	return 1;
//        }
//
//        return d1 - d2;
//    }
//}
