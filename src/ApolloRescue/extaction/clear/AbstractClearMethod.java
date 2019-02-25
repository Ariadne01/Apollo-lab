package ApolloRescue.extaction.clear;


import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.entities.RoadModel;
import adf.agent.action.Action;
import adf.agent.action.common.ActionMove;
import adf.agent.action.police.ActionClear;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public abstract class AbstractClearMethod {
    protected ClearHelper clearHelper;
    protected WorldInfo worldInfo;
    protected AgentInfo agentInfo;
    protected ScenarioInfo scenarioInfo;
    protected ApolloWorld world;
    protected PathPlanning pathPlanning;


    protected List<EntityID> pathToGo = null;
    protected Point2D clearPoint = null;
    protected Point2D movePoint = null;
    protected ClearHelper newClearHelper;
    protected EntityID areaID = null;


//    public AbstractClearMethod(PoliceForceWorld world) {
//        this.world = world;
//        newClearHelper = new ClearHelper(world);
//        commonAgent = (ApolloPoliceForce) world.getCommonAgent();//agentInfo.me()
//    }

    protected AbstractClearMethod(ApolloWorld world, WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo, PathPlanning pathPlanning) {
        this.world = world;
        this.worldInfo = worldInfo;
        this.agentInfo = agentInfo;
        this.scenarioInfo = scenarioInfo;
        this.pathPlanning = pathPlanning;

        clearHelper = new ClearHelper(world, worldInfo, agentInfo, scenarioInfo);
//        initial();
    }

//    public void initial(){
//        movePoint = null;
//        clearPoint = null;
//        pathToGo = null;
//        areaID = null;
//    }

//    public abstract JobState getExpectedAct(StandardEntity target);
//
//    public abstract JobState getExpectedAct(List<EntityID> path);
//
//    public JobState getExpectedAct(Pair<StandardEntity, StandardEntity> pair) {
//        return null;
//    }
//

//
//    public List<EntityID> getPathToGo() {
//        return pathToGo;
//    }
//
//    public Point2D getClearPoint() {
//        return clearPoint;
//    }
//
//    public Point2D getMovePoint() {
//        return movePoint;
//    }
//
//    public EntityID getAreaID() {
//        return areaID;
//    }
//
//    /**
//     *
//     * @param path
//     */
//    public void sendRoadsClearedMessage(List<EntityID> path) {
//        for (EntityID id : path) {
//            if (id == null) {
//                continue;
//            }
//            StandardEntity entity = world.getEntity(id);
//            if (entity instanceof Road) {
//                Road road = (Road) entity;
//                sendRoadClearedMessage(road);
//            }
//        }
//    }
//
//    public void sendRoadClearedMessage(Road road) {
//        if (road != null) {
//            commonAgent.getPoliceMessageHelper().sendClearedRoadMessage(
//                    road.getID());
//        }
//    }
//
//    /**
//     * 发送清理完成的PathInfo
//     * @param pathInfo
//     * @deprecated
//     */
//    public void sendPathClearedMessage(PathInfo pathInfo) {
//        if (pathInfo != null && pathInfo.getCenter() != null) {
////			commonAgent.getPoliceMessageHelper().sendClearedPathMessage(
////					pathInfo.getCenter().getID());
//        }
//    }
//

    public abstract Action clearWay(List<EntityID> path, EntityID target);

    public abstract Action clearAroundTarget(Pair<Integer, Integer> targetLocation);

    protected Pair<Integer, Integer> getSelfLocation() {
        return worldInfo.getLocation(agentInfo.getID());
    }

    protected Area getSelfPosition() {
        return agentInfo.getPositionArea();
    }

    protected Action moveAction(List<EntityID> path) {
        return new ActionMove(path);
    }

    protected Action moveAction(int time, List<EntityID> path, int x, int y) {
        return new ActionMove(path, x, y);
    }

    protected Action moveAction(Area entity, boolean force) {
        List<EntityID> result = pathPlanning.setFrom(agentInfo.getPosition()).setDestination(entity.getID()).calc().getResult();
        if (result == null) {
            return null;
        }
        return moveAction(result);
    }

    protected Action moveToPoint(EntityID position, int x, int y) {
        if (agentInfo.getPosition().equals(position)) {
            List<EntityID> list = new ArrayList<EntityID>();
            list.add(position);

            return moveAction(agentInfo.getTime(), list, x, y);
        } else {
            return moveAction((Area) worldInfo.getEntity(position), false);
        }
    }


    protected Action sendClearAct(int time, int x, int y) {
        return new ActionClear(x, y);
    }

    protected Action sendClearAct(int time, EntityID blockadeId) {
        return new ActionClear(blockadeId);
    }

    protected Map<EntityID, EntityID> getEntranceRoads() {
        return world.getEntranceRoads();
    }


    protected Set<Blockade> getBlockadeSeen() {
        return world.getBlockadesSeen();
    }

    protected RoadModel getRoadModel(EntityID roadId) {
        return world.getRoadModel(roadId);
    }


    protected Set<Road> getRoadsSeen() {
        return world.getRoadsSeen();
    }

    protected void printData(String s) {
        world.printData(s);
    }

    protected List<StandardEntity> getEntities(List<EntityID> idList) {
        List<StandardEntity> entities = new ArrayList<>();
        for (EntityID id : idList) {
            entities.add(worldInfo.getEntity(id));
        }
        return entities;
    }
}
