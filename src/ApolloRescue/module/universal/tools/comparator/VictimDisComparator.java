//package ApolloRescue.module.universal.tools.comparator;
//
//import ApolloRescue.module.universal.ApolloWorld;
//import ApolloRescue.module.universal.Util;
//import rescuecore2.misc.Pair;
//import rescuecore2.standard.entities.Human;
//import rescuecore2.standard.entities.StandardEntity;
//
//import java.util.Comparator;
//
//public class VictimDisComparator implements Comparator<StandardEntity> {
//
//	private Pair<Integer, Integer> victimPair;
//	private ApolloWorld world;
//
//	public VictimDisComparator(Pair<Integer, Integer> victimPair, ApolloWorld world) {
//		this.victimPair = victimPair;
//		this.world = world;
//
//	}
//
//	@Override
//	public int compare(StandardEntity o1, StandardEntity o2) {
//		Human at1 = (Human) o1;
//		Human at2 = (Human) o2;
//		int dis1 = Util.distance(at1.getLocation(world), victimPair);
//		int dis2 = Util.distance(at2.getLocation(world), victimPair);
//		if (dis1 > dis2)
//			return 1;
//		if (dis1 < dis2)
//			return -1;
//
//		return 0;
//	}
//
//}
