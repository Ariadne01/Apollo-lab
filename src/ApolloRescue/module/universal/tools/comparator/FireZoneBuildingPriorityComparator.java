package ApolloRescue.module.universal.tools.comparator;


import ApolloRescue.module.universal.entities.BuildingModel;

import java.util.Comparator;

/**
 * Sort building priority from highest to lowest.
 * 
 * @author Langley
 */
public class FireZoneBuildingPriorityComparator implements Comparator<BuildingModel> {

	@Override
	public int compare(BuildingModel o1, BuildingModel o2) {
		return o2.priority - o1.priority;
	}

}
