package ApolloRescue.module.complex.component.Info;

import ApolloRescue.extaction.clear.ClearHelperUtils;
import ApolloRescue.module.universal.Util;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PathInfo {
    private List<EntityID> mainRoadIDList;//还没有清理过的任务ID
    private List<Area> mainRoadAreaList;//保留原始任务area
    private List<Road> mainRoads;//保留原始任务
    private int x;
    private int y;
    private int width;
    private int length;
    private Area center;
    private boolean hasArrived = false;

    public PathInfo(List<Area> areaList, List<EntityID> path) {
        mainRoadIDList = path;
        mainRoadAreaList = areaList;
        calCenter();
        calWidth();
        init();
    }

    //初始化PathInfo中包含的道路
    private void init() {
        mainRoads = new ArrayList<Road>();
        for (Area area : mainRoadAreaList) {
            if (area instanceof Road) {
                Road road = (Road) area;
                mainRoads.add(road);
            }
        }
    }

    private void calCenter() {
        center = mainRoadAreaList.get(mainRoadIDList.size() / 2);
        x = center.getX();
        y = center.getY();
    }

    private void calWidth() {
        int dis = 0;
        long surface = 0;
        Area lastArea = null;// 记录上一个区域
//		for (Area a : mainRoadAreaList) {
//			surface += ClearHelperUtils.getTotalSurface(a);
//			if (lastArea == null) {// 没有上一个区域
//				lastArea = a;
//			} else {// 有上一个区域
//				Edge commonEdge = lastArea.getEdgeTo(a.getID());
//				Point2D EdgeCenter = ClearHelperUtils.getEdgeCenter(commonEdge);
//				Point2D lastAreaCenter = new Point2D(lastArea.getX(),
//						lastArea.getY());
//				Point2D areaCenter = new Point2D(a.getX(), a.getY());
//				dis += ClearHelperUtils.getDistance(lastAreaCenter, EdgeCenter);
//				dis += ClearHelperUtils.getDistance(areaCenter, EdgeCenter);
//				lastArea = a;
//			}
//		}
        for (int index = 0;index<mainRoadAreaList.size();index++){
            Area a = mainRoadAreaList.get(index);
            if (index != 0|| index!= mainRoadAreaList.size()-1){//除了第一个和最后一个加入节点面积
                surface += ClearHelperUtils.getTotalSurface(a);
            }
            if (lastArea == null) {// 没有上一个区域
                lastArea = a;
            } else {// 有上一个区域
                Edge commonEdge = lastArea.getEdgeTo(a.getID());
                Point2D EdgeCenter = ClearHelperUtils.getEdgeCenter(commonEdge);
                Point2D lastAreaCenter = new Point2D(lastArea.getX(),
                        lastArea.getY());
                Point2D areaCenter = new Point2D(a.getX(), a.getY());
                if (index !=1){
                    dis += ClearHelperUtils.getDistance(lastAreaCenter, EdgeCenter);
                }
                if (index !=mainRoadAreaList.size()-1){
                    dis += ClearHelperUtils.getDistance(areaCenter, EdgeCenter);
                }
                lastArea = a;
            }
        }
        if (dis == 0) {
            width = 0;
        } else {
            width = (int)(surface * 1000 * 1000 / dis);
            length = dis;
        }
    }

    public List<EntityID> getMainRoad() {
        return mainRoadIDList;
    }

    /**
     *
     * @return Roads
     */
    public List<Road> getMainRoads() {
        return mainRoads;
    }

    public EntityID getMainRoadStart() {
        if (mainRoadIDList == null || mainRoadIDList.size() == 0) {
            return null;
        } else {
            return mainRoadIDList.get(0);
        }
    }

    public EntityID getMainRoadEnd() {
        if (mainRoadIDList == null || mainRoadIDList.size() == 0) {
            return null;
        } else {
            return mainRoadIDList.get(mainRoadIDList.size() - 1);
        }
    }

    public void reverse() {// 将道路的头节点和尾节点对调
        Collections.reverse(mainRoadAreaList);
        Collections.reverse(mainRoadIDList);
        Collections.reverse(mainRoads);
//		for (int index = 0; index < mainRoadIDList.size() / 2; index++) {
//			EntityID temp;
//			temp = mainRoadIDList.get(index);
//			mainRoadIDList.set(index,
//					mainRoadIDList.get(mainRoadIDList.size() - 1 - index));
//			mainRoadIDList.set(mainRoadIDList.size() - 1 - index, temp);//反转IDlist
//			Area tempArea;
//			tempArea = mainRoadAreaList.get(index);
//			mainRoadAreaList.set(index,
//					mainRoadAreaList.get(mainRoadAreaList.size() - 1 - index));
//			mainRoadAreaList.set(mainRoadAreaList.size() - 1 - index, tempArea);//反转Arealist
//		}
    }

    public int getWidth() {
        return width;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Area getCenter() {
        return center;
    }

    public int getLength(){
        return length;
    }

    public List<Area> getMainRoadAreaList(){
        return mainRoadAreaList;
    }

    public void updateMainRoadClearedPlace(EntityID selfLocationID){
        if (mainRoadIDList.contains(selfLocationID)){//自身在主干道上
            int index = mainRoadIDList.indexOf(selfLocationID);
            mainRoadIDList = mainRoadIDList.subList(index, mainRoadIDList.size());//截取未清理部分
//			mainRoadAreaList = mainRoadAreaList.subList(index, mainRoadAreaList.size());//截取未清理部分
        }
    }
    public void setArrived(){
        hasArrived = true;
    }
    public boolean isArrived(){
        return hasArrived;
    }
}
