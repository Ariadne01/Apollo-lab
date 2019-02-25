package ApolloRescue.module.universal.tools.comparator;

import ApolloRescue.extaction.clear.ClearHelperUtils;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Edge;

import java.util.Comparator;

public class BuildingDoorDistanceComparator implements Comparator<Edge> {
	private Point2D selfPoint = null;

	public BuildingDoorDistanceComparator(Point2D selfPoint) {
		this.selfPoint = selfPoint;
	}

	@Override
	public int compare(Edge arg0, Edge arg1) {
		Point2D c1 = new Point2D((arg0.getStartX() + arg0.getEndX()) / 2, (arg0.getStartY() + arg0.getEndY()) / 2);
		Point2D c2 = new Point2D((arg1.getStartX() + arg1.getEndX()) / 2, (arg1.getStartY() + arg1.getEndY()) / 2);
		int dis1 = ClearHelperUtils.getDistance(c1, selfPoint);
		int dis2 = ClearHelperUtils.getDistance(c2, selfPoint);
		return dis1 - dis2;
	}

}
