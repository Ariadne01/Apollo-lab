package ApolloRescue.module.complex.firebrigade;

import ApolloRescue.module.algorithm.clustering.ApolloFireZone;
import ApolloRescue.module.algorithm.clustering.Cluster;
import ApolloRescue.module.universal.entities.BuildingModel;
import rescuecore2.standard.entities.Human;

public abstract class AbstractFBTargetSelector implements ITargetSelector<ExtinguishTarget> {

    protected FireBrigadeWorld world;
    protected Human me;
    protected ApolloFireZone targetZone;
 //   protected PartitionAssignComponent partitionAssignComponent;
 //   protected FireZoneManagerComponent fireZoneComponent;
    protected FireBrigadeTools fireBrigadeTools;

    /** last time extinguish target*/
    protected BuildingModel lastTarget;

    /** this time extinguish target*/
    protected BuildingModel thisTimeTarget;

    protected AbstractFBTargetSelector(FireBrigadeWorld world) {
        this.world = world;
        this.me = (Human) world.getWorldInfo().getEntity(world.getAgentInfo().getID());
    //    this.partitionAssignComponent = world.getComponent(PartitionAssignComponent.class);
     //   this.fireZoneComponent = world.getComponent(FireZoneManagerComponent.class);
        this.fireBrigadeTools = new FireBrigadeTools(world);
    }

    public abstract ExtinguishTarget getTarget(ApolloFireZone apolloFireZone);

    /**
     * @author Yangjiedong
     * @param targetZone
     * @return 如果需要获得目标则返回true
     */
    public abstract boolean shouldGetTarget(ApolloFireZone targetZone);

}

