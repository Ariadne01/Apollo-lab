package ApolloRescue.module.complex.firebrigade.search;

import ApolloRescue.module.complex.firebrigade.ExtinguishTarget;
import ApolloRescue.module.universal.ApolloWorld;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.worldmodel.EntityID;

public abstract class AbstractFireZoneSearch implements IFireZoneSearch {
    protected ApolloWorld world;
    protected FireSearchTargetSelector fireSearchTargetSelector;
    protected ExtinguishTarget lastFireBrigadeTarget = null;

    public AbstractFireZoneSearch(ApolloWorld world) {
        this.world = world;
        this.fireSearchTargetSelector = new FireSearchTargetSelector(world);
    }

    /**
     *
     * @param fireBrigadeTarget
     * @return smallest
     */
    public EntityID getExecutor(ExtinguishTarget fireBrigadeTarget) {
        EntityID smallestId = null;

        if (world.getMyDistanceTo(fireBrigadeTarget.getTarget().getID()) > world
                .getMaxExtinguishDistance()) {
            return smallestId;
        }

        for (FireBrigade next : world.getFireBrigadesSeen()) {
            if (smallestId == null
                    || smallestId.getValue() < next.getID().getValue()) {
                smallestId = next.getID();
            }
        }

        return smallestId;
    }
}
