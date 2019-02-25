package ApolloRescue.extaction.clear;

import ApolloRescue.module.algorithm.convexhull.JarvisMarch;
import ApolloRescue.module.complex.component.Info.AreaInfo;
import ApolloRescue.module.complex.component.Info.AreaModel;
import ApolloRescue.module.complex.component.Info.PathInfo;
import ApolloRescue.module.complex.component.RoadInfoComponent;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.entities.BuildingModel;
import ApolloRescue.module.universal.entities.Entrance;
import ApolloRescue.module.universal.tools.comparator.EntityIDComparator;
import ApolloRescue.module.universal.tools.comparator.EntranceDistanceSorter;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

// Jingyi Lu
public class PoliceForceWorld extends ApolloWorld {
    private List<EntityID> travelMemory;
    private Polygon mapPolygon;
    private Polygon scaleMapPolygon;
    private Map<EntityID, Integer> PFNearby;
    private ClearHelper newClearHelper;
    private List<Road> crossedRoads = new ArrayList<Road>();
    private ApolloWorld apolloWorld;
    private int lastUpdateTime = 0;


    @SuppressWarnings("rawtypes")
    public PoliceForceWorld(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo,
                            ModuleManager moduleManager, DevelopData developData,
                            StandardAgent self) {
        super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
        this.newClearHelper = new ClearHelper(apolloWorld, worldInfo, agentInfo, scenarioInfo);
        travelMemory = new ArrayList<EntityID>();
        PFNearby = new HashMap<EntityID, Integer>();
        // fireZoneEstimator = new FireZoneEstimator(this);
        // initMainRoad();
        initCrossedRoad();

        // 设置边界建筑
        setMapSideBuildings();
        // wzp 为所有的建筑物产生自己的，门和边列表！
        for (BuildingModel buildingModel : buildingModels) {
            buildingModel.createEntranceEdge();
        }

    }


//     @Override
    public ApolloWorld updateInfo(MessageManager messageManager) {
        if (lastUpdateTime >= getTime()) {
            return this;
        }
        super.updateInfo(messageManager);
        lastUpdateTime = getTime();
//        super.updateInfo(messageManager);

        // fireZoneEstimator.update();
        setEstimatedBurningBuildings();

        if (getSelfPosition().getID() != null) {
            travelMemory.add(super.getSelfPosition().getID());
        }

        for (PoliceForce pf : this.getPoliceForceList()) {
            if (pf.getID().equals(this.getSelf().getID())) {// 就是自己
                continue;
            }
            if (PFNearby.containsKey(pf.getID())) {// 列表中已经有这个PF
                if (this.canSee(pf) && this.isInSameArea(pf)) {// 发现可以看见他，并且在同一地点上
                    PFNearby.put(pf.getID(), PFNearby.get(pf.getID()) + 1);
                } else {// 看不见他，或者不在同一个地点
                    PFNearby.put(pf.getID(), 0);
                }

            } else {// 列表中没有这个PF
                PFNearby.put(pf.getID(), 0);
            }
        }
        return this;
    }
    private void initCrossedRoad() {
        for(StandardEntity entity : this.getRoads()) {
            int count = 0;
            Road road = (Road)entity;
            if(road.getNeighbours().size()<=2) {
                continue;
            }
            for(EntityID id : road.getNeighbours()) {
                StandardEntity entity1 = this.getEntity(id);
                if(entity1 instanceof Road && RoadInfoComponent.getConnectedBuildings(this, (Road)entity1).size() == 0) {
                    //  是路不是门！
                    count++;
                }
            }
            if(count>=3) {
                crossedRoads.add(road);
            }
        }
    }

//    // 初始化主干道
//    private void initMainRoad() {
//        Set<PathInfo> mainRoads = new HashSet<PathInfo>();
//        mainRoads
//                .addAll(getComponent(PartitionAssignComponent.class)
//                        .getPartition(self.getID())
//                        .setPathInfoSet(
//                                getComponent(PathInfoComponent.class)
//                                        .getPathInfoSet()));
//        getComponent(PathInfoComponent.class).setPathInfoInPartition(mainRoads);
//        KmeansLayer.PATH_INFOS.put(self.getID(), mainRoads);
//    }

    public EntityID getLastTimePlace() {// NEW Method 只能记录上一个停留的位置
        if (travelMemory == null || travelMemory.size() == 0) {
            return null;
        } else {
            return travelMemory.get(travelMemory.size() - 1);
        }
    }

//    /**
//     * Wzp 获取视野内的被堵塞在建筑物中的市民
//     *
//     * @return 目标建筑物和相对应的门
//     *
//     */
//    public Pair<StandardEntity, StandardEntity> getStuckedCiviliansInView() {
//        List<Area> buildingsForCivilianSeen = new ArrayList<Area>();
//        Map<Entrance, Building> entranceAndBuildingMap = new HashMap<Entrance, Building>();
//        for (EntityID id : this.getChanges()) {
//            StandardEntity entity = this.getEntity(id);
//            if (entity instanceof Civilian) {
//                Civilian civilian = (Civilian) entity;
//                if (civilian.getPosition(this) instanceof Building  ) {   //&&  !(civilian.getPosition(this) instanceof Refuge)
//                    if (civilian.isHPDefined() && civilian.getHP() > 1000) { // 市民在建筑物中，同时市民是活着的！
//                        buildingsForCivilianSeen.add((Area) civilian
//                                .getPosition(this)); // 此集合是视野内看到的所有在建筑物当中的市民！
//                    }
//                }
//            }
//        }
//
//        Iterator<Area> it = buildingsForCivilianSeen.iterator();
//        while (it.hasNext()) {
//            List<Entrance> blockedEntrance = new ArrayList<Entrance>();
//            Area area = it.next();
//            BuildingModel buildingModel = this.getBuildingModel(area.getID());
//            Building building = (Building) this.getEntity(area.getID());
//            for (Entrance entrance : buildingModel.getEntrances()) {
//                if (this.getChanges().contains(entrance.getID())) { // 门要是看见的门！
//                    Edge connectedEdge = buildingModel.getEntranceEdge().get(
//                            entrance.getID());
//                    if (!newClearHelper.isBuildingDoorBlocked(connectedEdge)) { // 如果有一个门没有被堵住！
//                        it.remove();
//                        for (Entrance entrance1 : blockedEntrance) { // 包括把之前加的也删掉！
//                            entranceAndBuildingMap.remove(entrance1);
//                        }
//                        break;
//                    } else { // 如果这一个看到的门被堵死了
//                        blockedEntrance.add(entrance);
//                        entranceAndBuildingMap.put(entrance, building);
//                    }
//                }
//            }
//        }
//        // 集合当中只有一个元素也可以传进比较器中进行比较，但只返回这个元素！
//        List<Entrance> blockedEntrances = new ArrayList<Entrance>(
//                entranceAndBuildingMap.keySet());
//        if (!blockedEntrances.isEmpty()) { // 如果被阻碍的门个数 > 1 ，找个最近的
//            Collections.sort(blockedEntrances,
//                    new EntranceDistanceSorter(this.getSelfHuman(), this));
//            Entrance closestEntrance = blockedEntrances.get(0);
//            return new Pair<StandardEntity, StandardEntity>(
//                    entranceAndBuildingMap.get(closestEntrance),
//                    closestEntrance.getNeighbour());
//        } else {
//            return null;
//        }
//    }
//    /**
//     * 获取视野内被堵塞在建筑中的智能体
//     * @return
//     */
//    public Pair<StandardEntity, StandardEntity> getStuckedAgentsInView() {
//        List<Area> buildingsForAgentSeen = new ArrayList<Area>();
//        Map<Entrance, Building> entranceAndBuildingMap = new HashMap<Entrance, Building>();
//        for (EntityID id : this.getChanges()) {
//            StandardEntity entity = this.getEntity(id);
//            if (entity instanceof PoliceForce || entity instanceof AmbulanceTeam || entity instanceof FireBrigade) {
//                Human agent = (Human) entity;
//                if (agent.getPosition(this) instanceof Building  ) {   //&&  !(civilian.getPosition(this) instanceof Refuge)
//                    if (agent.isHPDefined() && agent.getHP() > 1000) { // 智能体在建筑物中，同时智能体是活着的！
//                        buildingsForAgentSeen.add((Area) agent
//                                .getPosition(this)); // 此集合是视野内看到的所有在建筑物当中的智能体！
//                    }
//                }
//            }
//        }
//
//        Iterator<Area> it = buildingsForAgentSeen.iterator();
//        while (it.hasNext()) {
//            List<Entrance> blockedEntrance = new ArrayList<Entrance>();
//            Area area = it.next();
//            BuildingModel buildingModel = this.getBuildingModel(area.getID());
//            Building building = (Building) this.getEntity(area.getID());
//            for (Entrance entrance : buildingModel.getEntrances()) {
//                if (this.getChanges().contains(entrance.getID())) { // 门要是看见的门！
//                    Edge connectedEdge = buildingModel.getEntranceEdge().get(
//                            entrance.getID());
//                    if (!newClearHelper.isBuildingDoorBlocked(connectedEdge)) { // 如果有一个门没有被堵住！
//                        it.remove();
//                        for (Entrance entrance1 : blockedEntrance) { // 包括把之前加的也删掉！
//                            entranceAndBuildingMap.remove(entrance1);
//                        }
//                        break;
//                    } else { // 如果这一个看到的门被堵死了
//                        blockedEntrance.add(entrance);
//                        entranceAndBuildingMap.put(entrance, building);
//                    }
//                }
//            }
//        }
//        // 集合当中只有一个元素也可以传进比较器中进行比较，但只返回这个元素！
//        List<Entrance> blockedEntrances = new ArrayList<Entrance>(
//                entranceAndBuildingMap.keySet());
//        if (!blockedEntrances.isEmpty()) { // 如果被阻碍的门个数 > 1 ，找个最近的
//            Collections.sort(blockedEntrances,
//                    new EntranceDistanceSorter(this.getSelfHuman(), this));
//            Entrance closestEntrance = blockedEntrances.get(0);
//            return new Pair<StandardEntity, StandardEntity>(
//                    entranceAndBuildingMap.get(closestEntrance),
//                    closestEntrance.getNeighbour());
//        } else {
//            return null;
//        }
//    }
//    /**
//     * 获取分区内的PathInfo
//     *
//     * @return
//     */
//    public Set<PathInfo> getPathInfoSet() {
//        return getComponent(PathInfoComponent.class).getPathInfoInPartition();
//    }

    public ClearHelper getNewClearHelper() {
        return newClearHelper;
    }

    /**
     * 设置地图边界建筑物
     */
    public void setMapSideBuildings() {
        scaleMapPolygon = null;
        if (mapPolygon == null) {
            try {
                setMapPolygon();
            } catch (Exception e) {
            }
        }
        scaleMapPolygon = getScaleConvexHull(mapPolygon, 0.9f);
        for (BuildingModel b : getBuildingsModel()) {
            if (!scaleMapPolygon.contains(b.x(), b.y())) {
                b.setMapSide(true);
                mapSideBuildings.add(b.getID());
            }
        }

    }

    /**
     * 建立整个地图的凸包
     *
     * @throws Exception
     */
    private void setMapPolygon() throws Exception {
        Polygon map = new Polygon();
        List<AreaModel> points = new ArrayList<AreaModel>();
        for (StandardEntity b : getAreas()) {
            if (b instanceof Area) {
                points.add(new AreaModel((Area) b));
            }
        }
        JarvisMarch<AreaModel> maker = new JarvisMarch<AreaModel>(points);
        for (AreaModel a : maker.getHull()) {
            map.addPoint(a.x(), a.y());
        }

        this.mapPolygon = map;
    }

    /**
     * 获取地图凸包
     *
     * @param convex
     * @param scale
     * @return
     */
    public Polygon getScaleConvexHull(Polygon convex, float scale) {
        Polygon p = new Polygon();
        int size = convex.npoints;
        int[][] convexArray = new int[2][size];
        int[] xpoints = new int[size];
        int[] ypoints = new int[size];
        for (int i = 0; i < size; i++) {
            xpoints[i] = convex.xpoints[i];
            ypoints[i] = convex.ypoints[i];
        }
        convexArray[0] = xpoints;
        convexArray[1] = ypoints;
        scaleConvex(convexArray, scale);
        p.xpoints = convexArray[0];
        p.ypoints = convexArray[1];
        p.npoints = size;
        return p;
    }

    /**
     * 对凸包进行缩放
     *
     * @param convex
     * @param scale
     */
    private void scaleConvex(int[][] convex, float scale) {
        int size = convex[0].length;
        double Cx, Cy;
        Cx = Cy = 0d;
        for (int i = 0; i < size; i++) {
            Cx += convex[0][i];
            Cy += convex[1][i];
        }
        Cx /= size;
        Cy /= size;

        for (int i = 0; i < size; i++) {
            convex[0][i] = (int) ((convex[0][i] - Cx) * scale + Cx);
            convex[1][i] = (int) ((convex[1][i] - Cy) * scale + Cy);
        }
    }

    /**
     * 判断是否扎堆
     *
     * @return
     */
    public boolean isTooMuchPeople() {// 判断是否扎堆
        for (Map.Entry<EntityID, Integer> pf : PFNearby.entrySet()) {
            if (pf.getValue() >= 3) {// 有一个PF见的次数超过3次，判断扎堆
                return true;
            }
        }
        return false;
    }

    /**
     * 判断我是否还需要维持上一个任务
     *
     * @return 返回true，继续工作，返回false任务切换
     */
    public boolean shouldIContinueTask() {
        List<EntityID> possiblePF = new ArrayList<EntityID>();
        for (Map.Entry<EntityID, Integer> pf : PFNearby.entrySet()) {
            if (pf.getValue() >= 3) {// 有一个PF见的次数超过3次，判断扎堆
                possiblePF.add(pf.getKey());// 加入列表
            }
        }
        possiblePF.add(this.getSelf().getID());// 加入自身
        if (possiblePF.size() <= 1) {// 本身就我一个人
            return true;
        }
        Collections.sort(possiblePF, new EntityIDComparator());
        if (!this.getSelf().getID().equals(possiblePF.get(0))) {// 自己不是最小编号的
            return true;
        } else {
            return false;
        }
    }

    /**
     * 将里面所有PF跟随次数清0
     */
    public void resetPFNearby() {
        for (Map.Entry<EntityID, Integer> pf : PFNearby.entrySet()) {
            pf.setValue(0);
        }
    }

    public boolean shouldILeave(int threshold, int times) {//避免PF扎堆
        List<EntityID> humanIDs = new ArrayList<EntityID>();
        int nearNum = 0;
        for (Map.Entry<Human, Integer> entry : sameAgentNear.entrySet()) {//Entry<Human, Integer> --- <身边的同类Agent,Agent连续出现次数>
            int i = entry.getValue();
            if (i > times) {
                nearNum++;
                humanIDs.add(entry.getKey().getID());
            }
        }
        if (nearNum > threshold) {// 超过阀值
            Collections.sort(humanIDs, new EntityIDComparator());
            if (humanIDs.get(0).getValue() == this.getSelf().getID().getValue()) {// 自己该留下
                return false;
            } else {// 自己该走
                // System.out.println("我该走了");
                sameAgentNear.clear();// 清空列表，避免重复删除任务
                return true;
            }
        }
        return false;
    }

    public List<Road> getCrossedRoads() {
        return crossedRoads;
    }
}