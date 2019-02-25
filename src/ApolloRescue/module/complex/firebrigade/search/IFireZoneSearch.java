package ApolloRescue.module.complex.firebrigade.search;

import ApolloRescue.module.complex.firebrigade.ExtinguishTarget;
import rescuecore2.standard.entities.Area;

public interface IFireZoneSearch {

    /** To judge if i should search the fire.*/
    public boolean shouldSearch(ExtinguishTarget extinguishTarget);

    /** To update data every cycle.*/
    public void update();

    /**
     * To get next search area in fire zone.</br>
     * @return next search {@code Area}
     */
    public Area getNextArea();

    /** To fire search target selector.*/
    public FireSearchTargetSelector getTargetSelector();

}
