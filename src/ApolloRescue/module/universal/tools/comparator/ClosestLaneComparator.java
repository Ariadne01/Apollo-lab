package ApolloRescue.module.universal.tools.comparator;

import ApolloRescue.extaction.clear.ClearHelperUtils;
import ApolloRescue.module.universal.ApolloWorld;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Edge;

import java.util.Comparator;

public class ClosestLaneComparator implements Comparator<Edge> {
	private ApolloWorld world;
	private Point2D selfPoint;

	public ClosestLaneComparator(ApolloWorld world) {
		this.world = world;
		selfPoint = new Point2D(world.getSelfLocation().first(), world.getSelfLocation().second());
	}

	@Override
	public int compare(Edge p1, Edge p2) {
		int dis1 = closestDistance(p1, selfPoint);
		int dis2 = closestDistance(p2, selfPoint);
		return dis1 - dis2;
	}

	private int closestDistance(Edge e, Point2D selfPoint) {
		return ClearHelperUtils.getDistance(selfPoint, e);
	}
}
