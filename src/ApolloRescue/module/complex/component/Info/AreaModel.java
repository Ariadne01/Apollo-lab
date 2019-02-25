package ApolloRescue.module.complex.component.Info;

import ApolloRescue.module.universal.tools.Planarizable;
import rescuecore2.standard.entities.Area;

public class AreaModel implements Planarizable {
    private Area self;

    public AreaModel(Area self) {
        super();
        this.self = self;
    }

    public Area getSelf() {
        return self;
    }

    public void setSelf(Area self) {
        this.self = self;
    }

    @Override
    public int x() {
        return self.getX();
    }

    @Override
    public int y() {
        return self.getY();
    }

}
