package ApolloRescue.module.universal.tools.comparator;

import ApolloRescue.module.universal.tools.Ruler;
import rescuecore2.standard.entities.Edge;

import java.util.Comparator;

public class EdgeLengthSorter implements Comparator<Edge> {

	@Override
	public int compare(Edge o1, Edge o2) {
		if (null == o1 || null == o2) {
			return 0;
		}

		int dis1 = Ruler.getDistance(o1.getStart(), o1.getEnd());
		int dis2 = Ruler.getDistance(o2.getStart(), o2.getEnd());

		return dis2 - dis1;

	}

}
