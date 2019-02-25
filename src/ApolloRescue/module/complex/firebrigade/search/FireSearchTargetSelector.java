package ApolloRescue.module.complex.firebrigade.search;

import ApolloRescue.module.complex.firebrigade.ExtinguishTarget;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.entities.BuildingModel;
import javolution.util.FastSet;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.Set;

public class FireSearchTargetSelector {
    private ApolloWorld world;
    private ExtinguishTarget fireBrigadeTarget;
    private Set<EntityID> exploreTargets;
    private EntityID target;

    public FireSearchTargetSelector(ApolloWorld world) {
        this.world = world;
        exploreTargets = new HashSet<EntityID>();
//        initialize();
    }

    public void update() {
        exploreTargets.remove(world.getSelfPosition().getID());
        if (world.getSelfPosition().getID().equals(target)) {
            target = null;
        }
    }

    public void setTargetFire(ExtinguishTarget fireBrigadeTarget, boolean exploreAll) {
        this.fireBrigadeTarget = fireBrigadeTarget;
        fillExplorePlaces(exploreAll);
    }

    /**
     * this method find all areas for explore in this cluster.
     * we now use a simple area selector for this method. but M.Amin try to find best solution. ;)
     *
     * @param exploreAll for explore all of fire cluster
     */
    private void fillExplorePlaces(boolean exploreAll) {
        Set<BuildingModel> borderBuildings = new FastSet<BuildingModel>();
        Set<BuildingModel> allBuildings = new FastSet<BuildingModel>();

        borderBuildings.add(fireBrigadeTarget.getTarget());
        allBuildings.add(fireBrigadeTarget.getTarget());

        if (exploreAll) {
            Set<StandardEntity> entitySet = new FastSet<StandardEntity>(fireBrigadeTarget.getFireZone().getBorderEntities());
            entitySet.removeAll(fireBrigadeTarget.getFireZone().getIgnoredBorderEntities());

            for (StandardEntity entity : entitySet) {
                borderBuildings.add(world.getBuildingModel(entity.getID()));
                allBuildings.add(world.getBuildingModel(entity.getID()));
            }
        }
        for (BuildingModel neighbour : borderBuildings) {
            for (BuildingModel b : neighbour.getConnectedBuilding()) {
                if (world.getDistance(b.getSelfBuilding().getID(), neighbour.getSelfBuilding().getID()) < world.getViewDistance()) {
                    allBuildings.add(b);
                }
            }
        }

        exploreTargets = FireSearchTools.findMaximalCovering(allBuildings);

        target = null;
    }

    public void removeUnreachableArea(EntityID areaId) {
        exploreTargets.remove(areaId);
        target = null;

    }

    public Set<EntityID> getExploreTargets() {
        return exploreTargets;
    }

    @Deprecated
    public void initialize() {
        exploreTargets = new HashSet<EntityID>();
        update();
    }

    public Area getNextArea() {
        if (exploreTargets != null) {
            update();
            if (target == null) {
                int distance;
                int minDistance = Integer.MAX_VALUE;
                for (EntityID areaId : exploreTargets) {
                    distance = world.getDistance(world.getSelf().getID(), areaId);
                    if (distance < minDistance) {
                        minDistance = distance;
                        target = areaId;
                    }
                }
            }
        }
        if (target != null) {
            Area area = (Area) world.getEntity(target);
            return area;
        }
        return null;
    }
}
