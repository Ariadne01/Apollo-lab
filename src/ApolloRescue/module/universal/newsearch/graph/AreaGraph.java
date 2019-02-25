package ApolloRescue.module.universal.newsearch.graph;

import ApolloRescue.module.universal.knd.ApolloGeoUtil;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntityURN;

import java.awt.*;

public class AreaGraph {
    public boolean vis = false;
    public Area area = null;
    public double cx = 0;
    public double cy = 0;
    public final static int AREA_TYPE_ROAD = 0;
    public final static int AREA_TYPE_BULDING = 1;
    public final static int AREA_TYPE_REFUGE = 2;
    public final static int AREA_TYPE_ROAD_HYDRANT = 3;
    public final static int AREA_TYPE_GAS_STATION = 4;
    public int areaType = AREA_TYPE_ROAD;
    public int forgetTime = 30;
    public final static int policeForgetTime = 30;
    public final static int ambulanceForgetTime = 30;
    public final static int fireBirgadeForgetTime = 30;
    public boolean isSmall = false;
    public boolean isBig = false;

    public boolean isBuilding() {
        return (areaType == AREA_TYPE_BULDING || areaType == AREA_TYPE_GAS_STATION || areaType == AREA_TYPE_REFUGE);
    }
    public AreaGraph(Area area, GraphModule wsg) {
        if (area == null || wsg == null) {
            return;
        }
        Polygon poly = (Polygon) (area.getShape());
        double area_ = ApolloGeoUtil.getArea(poly);
        if (area_ < 1000 * 1000 * 25) {
            isSmall = true;
        }
        if (area_ > (wsg.worldGridSize * wsg.worldGridSize * 4) / 6) {
            isBig = true;
        }
        this.area = area;
        this.vis = false;
        //this.wsg = wsg;
        this.cx = this.area.getX();
        this.cy = this.area.getY();
        //this.instanceAreaGrid = instanceAreaGrid;
        StandardEntityURN areaURN = this.area.getStandardURN();

        this.areaType = AREA_TYPE_ROAD;
        switch (areaURN) { // #toDo
            case REFUGE: {
                areaType = AREA_TYPE_REFUGE;
                break;
            }
            case GAS_STATION: {
                areaType = AREA_TYPE_GAS_STATION;
                break;
            }
            case POLICE_OFFICE:
            case AMBULANCE_CENTRE:
            case FIRE_STATION:
            case BUILDING: {
                areaType = AREA_TYPE_BULDING;
                break;
            }
            case HYDRANT: {
                areaType = AREA_TYPE_ROAD_HYDRANT;
                break;
            }
        }
        switch (wsg.ai.me().getStandardURN()) {
            case POLICE_FORCE: {
                forgetTime = policeForgetTime;
                break;
            }
            case AMBULANCE_TEAM: {
                forgetTime = ambulanceForgetTime;
                break;
            }
            case FIRE_BRIGADE: {
                forgetTime = fireBirgadeForgetTime;
                break;
            }
        }
    }
}
