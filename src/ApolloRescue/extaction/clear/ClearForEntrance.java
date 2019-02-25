package ApolloRescue.extaction.clear;

import ApolloRescue.module.universal.ApolloWorld;
import adf.agent.action.Action;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.PathPlanning;
import javolution.util.FastMap;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

public class ClearForEntrance extends AbstractClearMethod{

    protected ClearHelper clearHelper;

    public ClearForEntrance(ApolloWorld world, WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo, PathPlanning pathPlanning){
        super(world, worldInfo, agentInfo, scenarioInfo,pathPlanning);

        clearHelper = new ClearHelper(world, worldInfo, agentInfo, scenarioInfo);
    }
    @Override
    public Action clearWay(List<EntityID> path, EntityID target) {
        return null;
    }

    @Override
    public Action clearAroundTarget(Pair<Integer, Integer> targetLocation) {
        return null;
    }

//    private ApolloPoliceForce me;
//    private DefaultSearchManager defaultSearchManager;
//    protected Pair<StandardEntity, StandardEntity> globalTargetPair = null;
//    protected Pair<StandardEntity, StandardEntity> targetPair = null;
//    private List<Entrance> entranceToRemove = new ArrayList<Entrance>();
//
//    public ClearForEntranceJob(PoliceForceWorld world) {
//        super(world);
//        this.clearHelper = new ClearHelper(world);
//        this.me = (ApolloPoliceForce) commonAgent;
//        this.defaultSearchManager = new DefaultSearchManager(world);
//    }
//
//    @Override
//    public String getJobName() {
//        return getClass().getName();
//    }
//
//    @Override
//    public void taskDone() {
//        target = null;
//        this.jobState = JobState.Inactive;
//    }
//
//    @Override
//    public boolean shouldExecute() throws ActionException {
//        Pair<StandardEntity, StandardEntity> finalPair = null;
//        finalPair = world.getStuckedCiviliansInView(); // 自己视野内的
//        if (finalPair == null) {
//            finalPair = chooseSeenTarget();
//        }
//        if (finalPair == null) {
//            finalPair = chooseOuchAndGlobalTarget();
//        }
//
//        if (finalPair != null) { // 如果不为空，说明有目标，要进行相应的行为判断！
//            clearMethod = new ClearForEntranceMethodV2(world);
//            jobState = clearMethod.getExpectedAct(finalPair);
//            if (jobState == JobState.Inactive) {
//                return false;
//            } else {
//                return true;
//            }
//        } else { // 为空就不执行 job !
//            return false;
//        }
//    }
//
//    private Pair<StandardEntity, StandardEntity> chooseSeenTarget()
//            throws ActionException {
//        if (targetPair == null || shouldChangeTarget(targetPair)) {
//            if (targetPair != null) { // 主要是针对第一次
//                Entrance entrance = world.getPaths().getEntrance(
//                        (Road) targetPair.second());
//                entranceToRemove.add(entrance);// 想办法在这里加入当前没有堵塞的门
//            }
//            targetPair = getSeenTarget();
//            return targetPair;
//        }
//        return targetPair;
//    }
//
//    public Pair<StandardEntity, StandardEntity> getSeenTarget()
//            throws ActionException { // 找市民！ 只返回门！
//        // 视野内没有就进行全局搜索！
//        List<Area> buildingsForCivilianListen = new ArrayList<Area>();
//        Map<EntityID, EntityID> civilianPositionMap = new FastMap<EntityID, EntityID>();
//        civilianPositionMap.putAll(world.getCivilianPositionMap()); // 有听到的和看到的！
//        Iterator<Map.Entry<EntityID, EntityID>> iterator = civilianPositionMap
//                .entrySet().iterator();
//        while (iterator.hasNext()) {
//            Map.Entry<EntityID, EntityID> entry = iterator.next();
//            Civilian civilian = (Civilian) world.getEntity(entry.getKey());
//            if (world.getCivilianSeenInMap().contains(civilian)) {
//                iterator.remove(); // 把自己从一开始到现在看到的删掉，保留自己从一开始听到的！
//                continue;
//            }
//            if ((civilian.getPosition(world) instanceof Building) && // 全局看到的且在建筑物当中！
//                    (civilian.isHPDefined() && civilian.getHP() > 1000)) {  //&&  !(civilian.getPosition(this) instanceof Refuge)
//                buildingsForCivilianListen.add((Area) civilian
//                        .getPosition(world)); // 全图中,是听到别人看见的
//            }
//        }
//        Pair<StandardEntity, StandardEntity> targetForListen = getCivilianLocationInMap(buildingsForCivilianListen);
//        if (targetForListen != null) {
//            return targetForListen;
//        } else {
//            return null;
//        }
//    }
//
//    private Pair<StandardEntity, StandardEntity> chooseOuchAndGlobalTarget()
//            throws ActionException {
//        if (globalTargetPair == null || shouldChangeTarget(globalTargetPair)) {
//            if (globalTargetPair != null) { // 主要是针对第一次
//                me.getPartitionSearchManager().setWorthless(
//                        (Building) globalTargetPair.first());
//                defaultSearchManager.setWorthless((Building) globalTargetPair
//                        .first());
//            }
//            globalTargetPair = getOuchAndGlobalTarget();
//            return globalTargetPair;
//        }
//        return globalTargetPair;
//    }
//
//    private boolean shouldChangeTarget(Pair<StandardEntity, StandardEntity> pair) {     //pair 是传进来的目标！
//        if (isBuildingBurnt((Building) pair.first())
//                || world.shouldILeave(0, 3)) { // 如果建筑物燃烧就换目标！
//            return true;
//        }
//        if (((Road) pair.second()).isBlockadesDefined()) {
//            BuildingModel buildingModel = world
//                    .getBuildingModel(pair.first().getID());
//            Edge connectedEdge = buildingModel.getEntranceEdge().get(
//                    pair.second().getID());
//            if (!clearHelper.isBuildingDoorBlocked(connectedEdge)) { // 如果门没有堵就换目标！
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    public Pair<StandardEntity, StandardEntity> getOuchAndGlobalTarget()
//            throws ActionException {
//        Area targetToGo = null; // targetToGo 一定是building!
//
//        targetToGo = me.getPartitionSearchManager().getNextTarget();
//
//        if (targetToGo == null) {
//            targetToGo = defaultSearchManager.getNextTarget();
//        }
//
//        if (targetToGo != null) {
//            BuildingModel targetModel = world.getBuildingModel(targetToGo
//                    .getID());
//            List<Entrance> ouchAndGlobalEntrances = new ArrayList<Entrance>(
//                    targetModel.getEntrances());
//            Collections.sort(ouchAndGlobalEntrances,
//                    new EntranceDistanceSorter(world.getSelfHuman(), world));
//            Entrance closestEntrance = ouchAndGlobalEntrances.get(0);
//            return new Pair<StandardEntity, StandardEntity>(
//                    (Building) targetToGo, closestEntrance.getNeighbour());
//        } else {
//            return null;
//        }
//    }
//
//    /**
//     * 在全局中选择一个市民目标 ， 是自己听到的！ 暂定选一个离自己最近的！
//     */
//    private Pair<StandardEntity, StandardEntity> getCivilianLocationInMap(
//            List<Area> buildingsForCivilianListen) {
//        RoadInfoComponent roadInfoComponent = world
//                .getComponent(RoadInfoComponent.class);
//        Map<Entrance, Building> entranceAndItsBuilding = new HashMap<Entrance, Building>();
//        if (buildingsForCivilianListen.isEmpty()) {
//            return null;
//        }
//        Iterator<Area> it = buildingsForCivilianListen.iterator();
//        while (it.hasNext()) {
//            Area area = it.next();
//            Building building = (Building) world.getEntity(area.getID());
//            BuildingModel buildingModel = world.getBuildingModel(area.getID());
//            if (isBuildingBurnt(building)) { // if building is on fire,then
//                // next; //FIXME
//                it.remove();
//                continue;
//            }
//            if (buildingModel != null) {
//                List<Entrance> entrancesOfBuilding = new ArrayList<Entrance>(
//                        buildingModel.getEntrances());
//                for (Entrance entrance : entrancesOfBuilding) {
//                    if (entrance.isBlockedOrNotSeen(roadInfoComponent)) { // 如果门看不到或者是被障碍物堵住。
//                        entranceAndItsBuilding.put(entrance, building);// 不一定是连在一起的！
//                    }
//                }
//            }
//        }
//        List<Entrance> blockedEntrances = new ArrayList<Entrance>(
//                entranceAndItsBuilding.keySet());
//        if (!blockedEntrances.isEmpty()) { // 在自己所在的分区内进行排序并找个离自己最近的门！
//            Partition myPartition = world.getComponent(
//                    PartitionAssignComponent.class).getPartition(
//                    commonAgent.getID());
//            Iterator<Entrance> iter = blockedEntrances.iterator();
//            while (iter.hasNext()) {
//                Entrance entrance = iter.next();
//                if (!(myPartition.getAreaIDSet().contains(entrance.getID()))) {
//                    iter.remove(); // 如果门不在我自己的分区内就删除！
//                    continue;
//                }
//            }
//            if (blockedEntrances.isEmpty()) {
//                return null;
//            } else {
//                blockedEntrances.removeAll(entranceToRemove);
//                if (blockedEntrances.isEmpty()) {
//                    return null;
//                } else {
//                    Collections.sort(blockedEntrances,
//                            new EntranceDistanceSorter(world.getSelfHuman(),
//                                    world));
//                    Entrance closestEntrance = blockedEntrances.get(0);
//                    return new Pair<StandardEntity, StandardEntity>(
//                            entranceAndItsBuilding.get(closestEntrance),
//                            closestEntrance.getNeighbour());
//                }
//            }
//        } else {
//            return null;
//        }
//    }

    private boolean isBuildingBurnt(Building building) {
        if (building == null || !building.isFierynessDefined()) {
            return false;
        }
        int fieriness = building.getFieryness();
        return fieriness != 0 && fieriness != 4 && fieriness != 5;
    }
}
