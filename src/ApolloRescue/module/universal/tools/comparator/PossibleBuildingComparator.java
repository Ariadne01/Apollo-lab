package ApolloRescue.module.universal.tools.comparator;

import rescuecore2.worldmodel.EntityID;

import java.util.Comparator;
import java.util.Map.Entry;

public class PossibleBuildingComparator implements Comparator<Entry<EntityID, Integer>> {
	// 出现的次数多的建筑物优先
	@Override
	public int compare(Entry<EntityID, Integer> arg0, Entry<EntityID, Integer> arg1) {
		return arg1.getValue() - arg0.getValue();
	}

}
