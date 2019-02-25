package ApolloRescue.module.complex.firebrigade.search;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.complex.firebrigade.FireBrigadeWorld;
import ApolloRescue.module.universal.JobState;
import ApolloRescue.module.universal.tools.ActionException;
import rescuecore2.standard.entities.Area;

public class FireZoneSearchMethod {
//    private ApolloFireBrigade me;
    private FireBrigadeWorld world;
    private IFireZoneSearch fireZoneSearch;
    private Area targetArea = null;

    public FireZoneSearchMethod(FireBrigadeWorld world) {
        this.world = world;
//        this.me = (ApolloFireBrigade) world.getCommonAgent();

        this.fireZoneSearch = world.getFireZoneSearch();
    }

    public void execute() throws ActionException {
        fireZoneSearch.update();

        targetArea = fireZoneSearch.getNextArea();
        if (targetArea == null) {
//            ConsoleDebug.printFB("target is null");
            return;
        }

        addSearchTargetLayer(targetArea);
        JobState status = manualMoveToArea(targetArea);

        if (status == JobState.SearchCancel) {
            fireZoneSearch.getTargetSelector().removeUnreachableArea(targetArea.getID());
            execute();
        }

        if (status == JobState.SearchFinish) {
//            ConsoleDebug.printFB("explored area = " + targetArea);
            execute();
        } else if (status == JobState.Searching) {

        }
    }

    public JobState manualMoveToArea(Area targetArea) throws ActionException {
        if (targetArea != null && !world.getSelfPosition().equals(targetArea)) {
 //           me.move(targetArea, ApolloConstants.IN_TARGET, false);
//            blackList.add(targetArea);
        } else if (targetArea != null && world.getSelfPosition().equals(targetArea)) {
            return JobState.SearchFinish;
        }
        return JobState.SearchCancel;
    }

    private void addSearchTargetLayer(Area target) {
        if(ApolloConstants.LAUNCH_VIEWER) {
//            ConvexHullLayer.CHECK_FIRE_AREA.put(me.getID(), target);
        }
    }
}
