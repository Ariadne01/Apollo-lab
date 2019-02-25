package ApolloRescue.module.universal.tools.comparator;

import ApolloRescue.module.universal.entities.Entrance;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;

import java.util.Comparator;

public class EntranceDistanceSorter implements Comparator<Entrance>{

    private StandardEntity reference;
    private StandardWorldModel world;

    public EntranceDistanceSorter(StandardEntity reference, StandardWorldModel world) {
        this.reference = reference;
        this.world = world;
    }
   @Override
    public int compare(Entrance a, Entrance b) {
	     int d1 = world.getDistance(reference, a.getNeighbour());
         int d2 = world.getDistance(reference, b.getNeighbour());
         return d1 - d2;
}

}
