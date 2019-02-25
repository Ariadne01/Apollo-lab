package ApolloRescue.module.universal.tools.comparator;


import ApolloRescue.module.algorithm.clustering.ApolloFireZone;

import java.util.Comparator;

/**
 * This comparator sorts fire zone value ascend.</br>
 * 
 * @author Langley
 *
 */
public class FireZoneValueComparator implements Comparator<ApolloFireZone> {

	@Override
	public int compare(ApolloFireZone id1, ApolloFireZone id2) {
		return (int) (id2.estimateValue - id1.estimateValue);
	}

}
