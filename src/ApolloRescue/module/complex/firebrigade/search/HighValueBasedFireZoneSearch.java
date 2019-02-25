package ApolloRescue.module.complex.firebrigade.search;

import ApolloRescue.module.complex.firebrigade.ExtinguishTarget;
import ApolloRescue.module.complex.firebrigade.FireBrigadeWorld;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.CommandTypes;
import adf.agent.communication.standard.bundle.centralized.CommandFire;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.worldmodel.EntityID;

public class HighValueBasedFireZoneSearch extends AbstractFireZoneSearch {
    protected ExtinguishTarget exploringTarget = null;
    protected Area preFBTargetArea = null;
    private int timeToExtinguish = MAX_TIME_TO_EXTINGUISH;
    private static final int MAX_TIME_TO_EXTINGUISH = 4;
    private static final int MAX_TIME_TO_EXTINGUISH_CL = 4;
    private boolean reachedToFirstTarget = true;
    FireBrigadeWorld fireBrigadeWorld = (FireBrigadeWorld)world;

    public HighValueBasedFireZoneSearch(ApolloWorld world) {
        super(world);
    }

    @Override
    public boolean shouldSearch(ExtinguishTarget fireBrigadeTarget) {
        boolean returnVal = false;
        Area area = fireSearchTargetSelector.getNextArea();

        if (lastFireBrigadeTarget != null) {
            Integer lastClusterId = lastFireBrigadeTarget.getFireZone().getId();
            Integer exploringClusterId = (exploringTarget == null ? null : exploringTarget.getFireZone().getId());
            Integer newClusterId = (fireBrigadeTarget == null ? null : fireBrigadeTarget.getFireZone().getId());
            boolean targetChanged = !lastClusterId.equals(newClusterId) || (exploringClusterId != null && !exploringClusterId.equals(newClusterId));
            boolean fireIsNear = fireBrigadeTarget != null && world.getDistance(fireBrigadeTarget.getTarget().getID(), world.getSelfPosition().getID()) <= world.getViewDistance();


            if (!reachedToFirstTarget && area != null && !fireIsNear) {
                if (preFBTargetArea != null && preFBTargetArea.getID().equals(world.getSelfPosition().getID())) {
                    reachedToFirstTarget = true;
//                    world.printData("reachedToFirstTarget " + preFBTargetArea);
                } else {
                    lastFireBrigadeTarget = fireBrigadeTarget;
                    preFBTargetArea = area;
                    return true;
                }
            }

            if (targetChanged && area != null && !fireIsNear) {
                lastFireBrigadeTarget = fireBrigadeTarget;
//                world.printData("continue explore goto " + area);
                return true;
            }

            // choose fb
            EntityID clusterLeaderId = getExecutor(lastFireBrigadeTarget);
            if (clusterLeaderId != null && world.getAgentInfo().getID().equals(clusterLeaderId)) {
                if (fireBrigadeTarget == null || targetChanged || timeToExtinguish == 0) {
                    timeToExtinguish = MAX_TIME_TO_EXTINGUISH;
                    if (world.isCommunicationLess()) {
                        timeToExtinguish = MAX_TIME_TO_EXTINGUISH_CL;
                    }
                    fireSearchTargetSelector.setTargetFire(lastFireBrigadeTarget, fireBrigadeTarget == null);
                    preFBTargetArea = fireSearchTargetSelector.getNextArea();

                    exploringTarget = lastFireBrigadeTarget;
                    reachedToFirstTarget = false;
                    returnVal = true;
                } else if (fireBrigadeWorld.getLastCommand() == CommandFire.ACTION_EXTINGUISH) {
                    timeToExtinguish--;
                } else if (!(fireBrigadeWorld.getLastCommand() == CommandFire.ACTION_EXTINGUISH) && !world.isCommunicationLess()) {
                    timeToExtinguish = MAX_TIME_TO_EXTINGUISH;
                }
            }
        } else if (area != null) {
            returnVal = true;
        }
        lastFireBrigadeTarget = fireBrigadeTarget;

        return returnVal;
    }

    @Override
    public void update() {
        // TODO Auto-generated method stub
    }

    @Override
    public Area getNextArea() {
        return fireSearchTargetSelector.getNextArea();
    }

    @Override
    public FireSearchTargetSelector getTargetSelector() {
        return fireSearchTargetSelector;
    }
}
