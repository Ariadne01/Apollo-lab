package ApolloRescue.module.universal.tools.comparator;

import rescuecore2.standard.entities.Human;

import java.util.Comparator;

public class BuriednessComparator implements Comparator<Human> {

	@Override
	public int compare(Human o1, Human o2) {
		int buriedness1 = o1.getBuriedness();
		int buriedness2 = o2.getBuriedness();

		if (buriedness1 > buriedness2) {
			return 1;
		}
		
		if (buriedness1 < buriedness2) {
			return -1;
		}
		
		return 0;
	}

}
