//package ApolloRescue.module.universal.tools.comparator;
//
//import apollo.tools.geometry.Ruler;
//
//import java.awt.*;
//import java.util.Comparator;
//
//public class PointSorter implements Comparator<Point> {
//
//	private Point reference;
//
//	public PointSorter(Point reference) {
//		this.reference = reference;
//	}
//
//	@Override
//	public int compare(Point o1, Point o2) {
//		if (null == o1 || null == o2) {
//			return 0;
//		}
//		int dis0 = Ruler.getDistance(o1, reference);
//		int dis1 = Ruler.getDistance(o2, reference);
//
//		return dis0 - dis1;
//	}
//
//}
