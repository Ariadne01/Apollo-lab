//package ApolloRescue.module.universal.tools.comparator;
//
//import ApolloRescue.extaction.clear.ClearHelperUtils;
//import rescuecore2.misc.geometry.Point2D;
//import rescuecore2.misc.geometry.Vector2D;
//import rescuecore2.standard.entities.Blockade;
//import rescuecore2.standard.entities.Human;
//
//import java.util.Comparator;
//import java.util.Set;
//
///**
// *按照可清理的面积排序
// *
// */
//public class PointClearAreaSorter implements Comparator<Point2D> {
//
//	private Set<Blockade> blockades;
//	private Point2D agentPoint;
//	private Human huamn;
//	private int clearDistance;
//
//	public PointClearAreaSorter(Set<Blockade> blocks, Human human, int distance) {
//		this.blockades = blocks;
//		this.agentPoint = new Point2D(human.getX(), human.getY());
//		this.huamn = human;
//		this.clearDistance = distance;
//	}
//
//
//	@Override
//	public int compare(Point2D o1, Point2D o2) {
//		double area1 = calcBlockadeArea(agentPoint, o1);
//		// System.out.println(o1 + " area: " + area1);
//		double area2 = calcBlockadeArea(agentPoint, o2);
//		// System.out.println(o2 + " area: " + area2);
//		if (area1 - area2 > 0) {
//			return -1;
//		} else if (area1 - area2 < 0) {
//			return 1;
//		} else
//			return 0;
//	}
//
//	private double calcBlockadeArea(Point2D agentPoint, Point2D p) {
//		// 生成clear Area
//		Vector2D v = new Vector2D(p.getX() - agentPoint.getX(), p.getY() - agentPoint.getY());
//		Point2D originalPoint = new Point2D(p.getX(), p.getY());
//		Point2D newPoint = originalPoint.plus(v.normalised().scale(clearDistance));
//		java.awt.geom.Area clearArea = ClearHelperUtils.getClearArea(huamn, (int) newPoint.getX(),
//				(int) newPoint.getY(),
//				ClearHelperUtils.getDistance(agentPoint, new Point2D((int) newPoint.getX(), (int) newPoint.getY())),
//				500);
//		if (null != clearArea) {
//			return ClearHelperUtils.calcBlockedArea(clearArea, blockades);
//		} else {
//			return 0;
//		}
//	}
//
//}
