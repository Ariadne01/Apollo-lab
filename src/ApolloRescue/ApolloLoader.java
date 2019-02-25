package ApolloRescue;

import ApolloRescue.tactics.SampleTacticsAmbulanceCentre;
import ApolloRescue.tactics.SampleTacticsAmbulanceTeam;
import ApolloRescue.tactics.SampleTacticsFireBrigade;
import ApolloRescue.tactics.SampleTacticsFireStation;
import ApolloRescue.tactics.SampleTacticsPoliceForce;
import ApolloRescue.tactics.SampleTacticsPoliceOffice;
import adf.component.AbstractLoader;
import adf.component.tactics.TacticsAmbulanceTeam;
import adf.component.tactics.TacticsFireBrigade;
import adf.component.tactics.TacticsPoliceForce;
import adf.component.tactics.TacticsAmbulanceCentre;
import adf.component.tactics.TacticsFireStation;
import adf.component.tactics.TacticsPoliceOffice;

/**
 * 实验2
 */
public class ApolloLoader extends AbstractLoader {
    @Override
    public String getTeamName() {
        return "ApolloRescue";
    }

    @Override
    public TacticsAmbulanceTeam getTacticsAmbulanceTeam() {
        return new SampleTacticsAmbulanceTeam();
    }

    @Override
    public TacticsFireBrigade getTacticsFireBrigade() {
        return new SampleTacticsFireBrigade();
    }

    @Override
    public TacticsPoliceForce getTacticsPoliceForce() {
        return new SampleTacticsPoliceForce();
    }

    @Override
    public TacticsAmbulanceCentre getTacticsAmbulanceCentre() {
        return new SampleTacticsAmbulanceCentre();
    }

    @Override
    public TacticsFireStation getTacticsFireStation() {
        return new SampleTacticsFireStation();
    }

    @Override
    public TacticsPoliceOffice getTacticsPoliceOffice() {
        return new SampleTacticsPoliceOffice();
    }
}
