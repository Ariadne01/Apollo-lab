package ApolloRescue.module.universal.tools.comparator;


import ApolloRescue.extaction.clear.ClearHelperUtils;
import ApolloRescue.module.complex.component.Info.PathInfo;
import ApolloRescue.module.universal.ApolloWorld;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Road;

import java.util.Comparator;

public class CloseClearPathComparator implements Comparator<PathInfo> {
	private ApolloWorld world;
	private Point2D selfPoint;
	private boolean considerWidth = false;

	public CloseClearPathComparator(ApolloWorld world) {
		this.world = world;
		selfPoint = new Point2D(world.getSelfLocation().first(), world.getSelfLocation().second());
	}

	public CloseClearPathComparator(ApolloWorld world, boolean considerWidth) {
		this.world = world;
		selfPoint = new Point2D(world.getSelfLocation().first(), world.getSelfLocation().second());
		this.considerWidth = considerWidth;
	}

	@Override
	public int compare(PathInfo p1, PathInfo p2) {
		int dis1 = closestDistance(p1, selfPoint);
		int dis2 = closestDistance(p2, selfPoint);
		if (considerWidth) {
			int roadDif = p2.getWidth() - p1.getWidth();
			if (Math.abs(roadDif) < 1000) {
				return dis1 - dis2;
			} else {
				return roadDif;
			}
		} else {
			return dis1 - dis2;
		}
	}

	private int closestDistance(PathInfo p, Point2D selfPoint) {
		int dis1 = ClearHelperUtils.getDistance(selfPoint, (Road) world.getEntity(p.getMainRoadStart()));
		int dis2 = ClearHelperUtils.getDistance(selfPoint, (Road) world.getEntity(p.getMainRoadEnd()));
		if (dis1 > dis2) {
			return dis2;
		} else {
			return dis1;
		}
	}
}
