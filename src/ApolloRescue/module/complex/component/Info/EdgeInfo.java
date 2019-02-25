package ApolloRescue.module.complex.component.Info;

import rescuecore2.misc.Pair;

public class EdgeInfo {

    public EdgeInfo() {
    }

    protected Integer length;
    protected Pair<Integer, Integer> middle;
    protected Boolean onEntrance = false;

    public void setLength(Integer length) {
        this.length = length;
    }

    public void setMiddle(Pair<Integer, Integer> middle) {
        this.middle = middle;
    }

    public void setOnEntrance(Boolean onEntrance) {
        this.onEntrance = onEntrance;
    }

    public Integer getLength() {
        return length;
    }

    public Pair<Integer, Integer> getMiddle() {
        return middle;
    }

    public Boolean isOnEntrance() {
        return onEntrance;
    }
}
