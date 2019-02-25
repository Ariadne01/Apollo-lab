//package ApolloRescue.module.universal.tools.comparator;
//
//import apollo.tools.geometry.Ruler;
//
//import java.awt.*;
//import java.util.Comparator;
//
//public class PointToCenterPointSorter implements Comparator<Point> {
//
//	private Point referencePoint;
//
//	public PointToCenterPointSorter(Point areaCenterPoint) {
//		this.referencePoint = areaCenterPoint;
//	}
//
//	@Override
//	public int compare(Point p1, Point p2) {
//
//		if (null == p1 || null == p2) {
//			return 0;
//		}
//
//		int dis1 = Ruler.getDistance(referencePoint, p1);
//		int dis2 = Ruler.getDistance(referencePoint, p2);
//
//		return dis1 - dis2;
//
//	}
//
//}
