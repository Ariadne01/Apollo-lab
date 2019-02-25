package ApolloRescue.module.complex.firebrigade;

import ApolloRescue.module.algorithm.clustering.ApolloFireZone;
import ApolloRescue.module.universal.entities.BuildingModel;
import rescuecore2.standard.entities.Area;

public class ExtinguishTarget {
    private int lifeCycle;
    private BuildingModel target;
    private ApolloFireZone fireZone;
    private Area waterPosition;
    private BuildingModel apolloBuilding;
    private static final int DEFAULT_EXTINGUISH_TIME = 3;

    public ExtinguishTarget(BuildingModel target, ApolloFireZone zone) {
        this.target = target;
        this.fireZone = zone;
    }

    public ExtinguishTarget(BuildingModel target) {
        this.target = target;
        this.lifeCycle = DEFAULT_EXTINGUISH_TIME;
    }

    public BuildingModel getTarget() {
        return target;
    }

    public BuildingModel getApolloBuilding() {
        return apolloBuilding;
    }

    public void setTarget(BuildingModel target) {
        this.target = target;
    }

    public ApolloFireZone getFireZone() {
        return fireZone;
    }

    public void setFireZone(ApolloFireZone fireZone) {
        this.fireZone = fireZone;
    }

    public Area getWaterPosition() {
        return waterPosition;
    }

    public void setWaterPosition(Area waterPosition) {
        this.waterPosition = waterPosition;
    }

    public int getLifeCycle() {
        return lifeCycle;
    }

    public void setLifeCycle(int lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

    /**
     *  decrease target lifecycle
     */
    public void decreaseLifeCycle() {
        this.lifeCycle--;
    }

}
