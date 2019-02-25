package ApolloRescue.module.universal.tools.comparator;


import ApolloRescue.module.complex.component.Info.PathInfo;
import ApolloRescue.module.universal.Util;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;

import java.util.Comparator;

public class PathInfoComparator implements Comparator<PathInfo> {

	private StandardEntity selfPotition;

	public PathInfoComparator(StandardEntity selfPotition) {
		this.selfPotition = selfPotition;
	}

	@Override
	public int compare(PathInfo o1, PathInfo o2) {
		Area area = null;
		if (selfPotition instanceof Area) // 自己所在的路和建筑物在这里赋值
			area = (Area) selfPotition;

		Point2D p1 = new Point2D(o1.getX(), o1.getY());
		Point2D p2 = new Point2D(o2.getX(), o2.getY());
		Point2D selfPoint = new Point2D(area.getX(), area.getY());
		int dis1 = Util.distance(selfPoint, p1);
		int dis2 = Util.distance(selfPoint, p2);
		if (dis1 > dis2)
			return 1;
		else if (dis1 < dis2)
			return -1;
		else
			return 0;

	}

}
