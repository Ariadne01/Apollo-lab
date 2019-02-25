package ApolloRescue.module.universal.tools.comparator;

import ApolloRescue.module.universal.ApolloWorld;
import rescuecore2.worldmodel.EntityID;

import java.util.Comparator;

/**
 * This comparator sorts area by distance and compare ID when the distance is equal.</br>
 *
 */
public class AreaIDBasedDistanceComparator implements Comparator<EntityID>{
	
	private EntityID reference;
	private ApolloWorld model;
	
	public AreaIDBasedDistanceComparator(EntityID referenceID, ApolloWorld world) {
		this.reference = referenceID;
		this.model = world;
	}

	@Override
	public int compare(EntityID o1, EntityID o2) {
		if(o1 == null || o2 == null){
			return 0;
		}
		
		int dis_1 = model.getDistance(reference, o1);
		int dis_2 = model.getDistance(reference, o2);
		
		return dis_1 - dis_2;
	}
}
