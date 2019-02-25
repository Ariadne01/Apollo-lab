package ApolloRescue.module.universal.tools.comparator;

import rescuecore2.standard.entities.StandardEntity;

import java.util.Comparator;

public class IDComparator implements Comparator<StandardEntity> {
	
	public int compare(StandardEntity r1, StandardEntity r2) {
		if (r1.getID().getValue() > r2.getID().getValue()) {
			return 1;
		}
		if (r1.getID().getValue() == r2.getID().getValue()) {
			return 0;
		} else {
			return -1;
		}
	}
	
}
