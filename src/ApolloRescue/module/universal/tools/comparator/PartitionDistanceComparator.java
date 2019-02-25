//package ApolloRescue.module.universal.tools.comparator;
//
//
//import ApolloRescue.module.universal.Util;
//
//import java.util.Comparator;
//
///**
// * This comparator sorts civilian partition ascend.</br>
// * Date: Sep 21, 2014
// * @author langley
// *
// */
//public class PartitionDistanceComparator implements Comparator<CivilianPartition> {
//	private CivilianPartition self;
//
//	public PartitionDistanceComparator(CivilianPartition self) {
//		this.self = self;
//	}
//
//	@Override
//	public int compare(CivilianPartition o1, CivilianPartition o2) {
//		int dis1 = Util.distance(self.getPartition().getCenter(), o1.getPartition().getCenter());
//		int dis2 = Util.distance(self.getPartition().getCenter(), o2.getPartition().getCenter());
//		return dis1 - dis2;
//	}
//
//}
