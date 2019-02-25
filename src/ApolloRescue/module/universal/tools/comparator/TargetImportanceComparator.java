//package ApolloRescue.module.universal.tools.comparator;
//
//
//import java.util.Comparator;
//
//public class TargetImportanceComparator implements Comparator<ClearTarget> {
//
//	@Override
//	public int compare(ClearTarget t1, ClearTarget t2) {
//		double b1 = t1.getWeight();
//		double b2 = t2.getWeight();
//		if (b1 < b2)
//			return 1;
//		if (b1 == b2) {
//			// if my distance to t1 is less than distance to t2
//			if (t1.getDistanceToIt() > t2.getDistanceToIt()) {
//				return 1;
//			} else if (t1.getDistanceToIt() == t2.getDistanceToIt()) {
//				return 0;
//			}
//		}
//
//		return -1;
//	}
//}
