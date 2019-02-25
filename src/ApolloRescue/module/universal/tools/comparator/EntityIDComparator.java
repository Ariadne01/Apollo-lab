package ApolloRescue.module.universal.tools.comparator;

import rescuecore2.worldmodel.EntityID;

import java.util.Comparator;

public class EntityIDComparator implements Comparator<EntityID> {
    @Override
    public int compare(EntityID id1, EntityID id2) {
        if(id1.getValue() < id2.getValue()) {
            return -1;
        }else if(id1.getValue() > id2.getValue()) {
            return 1;
        }else
            return 0;
    }
}
