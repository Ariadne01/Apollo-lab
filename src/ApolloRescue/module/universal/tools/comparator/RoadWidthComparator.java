package ApolloRescue.module.universal.tools.comparator;


import ApolloRescue.module.complex.component.Info.PathInfo;

import java.util.Comparator;

public class RoadWidthComparator implements Comparator<PathInfo> {// up

	@Override
	public int compare(PathInfo o1, PathInfo o2) {
		return o1.getWidth() - o2.getWidth();
	}

}
