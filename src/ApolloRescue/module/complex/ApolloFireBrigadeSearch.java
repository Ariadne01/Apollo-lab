package ApolloRescue.module.complex;

import ApolloRescue.module.complex.component.VisibilityInfoComponent;
import ApolloRescue.module.universal.ApolloWorld;
//import ApolloRescue.module.complex.search.SearchJudge;
import ApolloRescue.module.universal.entities.BuildingModel;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.Search;
import adf.launcher.ConsoleOutput;
//import com.mrl.debugger.remote.VDClient;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.io.Serializable;
import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;
import static rescuecore2.standard.entities.StandardEntityURN.FIRE_STATION;
import static rescuecore2.standard.entities.StandardEntityURN.POLICE_OFFICE;

public class ApolloFireBrigadeSearch extends Search{
    private PathPlanning pathPlanning;
    private Clustering clustering;

    private EntityID result;
    private Set<EntityID> unsearchedBuildingIDs;
    private Set<EntityID> unsearchedClusterBuildingIDs;
    private List<EntityID> unsearchedBuildingIDList;
    private List<EntityID> unsearchedClusterBuildingList;

    private Map<EntityID, Integer> unreachableTargets;
    private RandomGenerator randomGenerator;
    private ApolloWorld world;
    private EntityID toSearchTarget;

    private int lastExecuteTime;
    private int thisCycleTryTime;
    private VisibilityInfoComponent visibilityComponent;


    //cluster为分区；
    public ApolloFireBrigadeSearch(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.unsearchedBuildingIDs = new HashSet<>();
        this.unsearchedClusterBuildingIDs = new HashSet<>();

        this.unreachableTargets = new HashMap<>();
        this.unsearchedBuildingIDList = new ArrayList<>();
        this.unsearchedClusterBuildingList=new ArrayList<>();
        this.randomGenerator = new MersenneTwister(System.currentTimeMillis());

        StandardEntityURN agentURN = ai.me().getStandardURN();
        switch (si.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("Search.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("Search.Clustering.Fire", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("Search.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("Search.Clustering.Fire", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("Search.PathPlanning.Fire", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("Search.Clustering.Fire", "adf.sample.module.algorithm.SampleKMeans");
                break;
        }

        this.world = ApolloWorld.load(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
        visibilityComponent = world.getComponent(VisibilityInfoComponent.class);
    }


    @Override
    public Search updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.world.updateInfo(messageManager);
        this.pathPlanning.updateInfo(messageManager);
        this.clustering.updateInfo(messageManager);

        updateUnreachableTargets();
        this.world.getPossibleBurningBuildings().removeAll(this.worldInfo.getChanged().getChangedEntities());
        this.world.getPossibleBurningBuildings().removeAll(unreachableTargets.keySet());
        this.world.getPossibleBurningBuildings().removeAll(this.world.getVisitedBuildings());
        this.unsearchedBuildingIDs.removeAll(unreachableTargets.keySet());
        this.unsearchedBuildingIDs.removeAll(this.world.getVisitedBuildings());
        this.unsearchedClusterBuildingIDs.removeAll(unreachableTargets.keySet());
        this.unsearchedClusterBuildingIDs.removeAll(this.worldInfo.getChanged().getChangedEntities());
        if (this.unsearchedBuildingIDs.isEmpty()) {
            this.reset();
            this.unsearchedBuildingIDs.removeAll(this.worldInfo.getChanged().getChangedEntities());
        }

        return this;
    }

    private void reset() {
        this.unsearchedClusterBuildingIDs.clear();
        this.unsearchedBuildingIDs.clear();
        Collection<StandardEntity> clusterEntities = null;
        if (this.clustering != null) {
            int clusterIndex = this.clustering.getClusterIndex(this.agentInfo.getID());
            clusterEntities = this.clustering.getClusterEntities(clusterIndex);
        }
        if (clusterEntities != null && clusterEntities.size() > 0) {
            for (StandardEntity entity : clusterEntities) {
                if (entity instanceof Building && entity.getStandardURN() != REFUGE) {
                    this.unsearchedClusterBuildingIDs.add(entity.getID());
                }
            }
        }
        this.unsearchedBuildingIDs.addAll(this.world.getUnvisitedBuildings());
        if(unsearchedBuildingIDs.isEmpty()){
            this.unsearchedBuildingIDs.addAll(this.worldInfo.getEntityIDsOfType(
                    BUILDING,
                    GAS_STATION,
                    AMBULANCE_CENTRE,
                    FIRE_STATION,
                    POLICE_OFFICE
            ));
        }
    }
    @Override
    public Search calc() {
//        if (MrlPersonalData.DEBUG_MODE) {
//
//            int clusterIndex = clustering.getClusterIndex(this.agentInfo.getID());
//            Collection<StandardEntity> elements = clustering.getClusterEntities(clusterIndex);
//
//            if (elements != null) {
//                List<Integer> elementList = Util.fetchIdValueFormElements(elements);
//                VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlFireBrigadeBuildingsLayer", (Serializable) elementList);
//            }
//
//
//            if (world.getPossibleBurningBuildings() != null) {
//                List<Integer> elementList = Util.fetchIdValueFormElementIds(world.getPossibleBurningBuildings());
//                VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlPossibleBurningBuildingsLayer", (Serializable) elementList);
//            }
//
//        }

        //try to search for possible burning buildings based on last target
        //System.out.println("excute this search method");
           // changeTargetFlag=false;
            toSearchTarget = search(result,true);
            if (toSearchTarget == null) {
                //try to search for possible burning buildings in the partition
                System.out.println("开始搜索未着火建筑");
                toSearchTarget = search(result ,false);
                if(toSearchTarget ==null) {
                    System.out.println("没找到目标");
                    return this;
                }
                else{
                    this.result = toSearchTarget;
                }

            }
            else {
                this.result = toSearchTarget;
            }

        return this;
    }
    private boolean shouldChangeTarget(){
        if (result != null &&  unsearchedBuildingIDs.contains(result)) {
            if (hasPath(result)) {
                System.out.println("选择了上次的目标");
                return false;
            } else {
                addToUnreachableTargets(result);
            }
        }
        return true;
    }
    private boolean judgeBuilding(EntityID lastTarget){
        Building building=(Building) worldInfo.getEntity(lastTarget);
        BuildingModel buildingModel = world.getBuildingModel(lastTarget);
        if(visibilityComponent.canISeeInside(buildingModel)||world.getSelfPosition().equals(building)||!buildingModel.isVisitable()){
            this.unsearchedBuildingIDs.remove(lastTarget);
            this.world.setBuildingVisited(lastTarget,true);
            return false;
        }
        return true;
    }
    private EntityID search(EntityID lastTarget, boolean burn) {
    //    System.out.println("运行search");
        EntityID toSearchTarget = null;
        if(burn){
               return searchBurnBuild(lastTarget);
        }
        else {
            if (unsearchedBuildingIDs!= null && ! unsearchedBuildingIDs.isEmpty()) {

                if (lastTarget != null &&  unsearchedBuildingIDs.contains(lastTarget)) {
                    if (hasPath(lastTarget)) {
            //            System.out.println("选择了上次的目标");
                        if(judgeBuilding(lastTarget)){
                            return lastTarget;
                        }
                    } else {
                        addToUnreachableTargets(lastTarget);
                    }
                }

                List<StandardEntity> unsearchedBuildings = new ArrayList<>();
                unsearchedClusterBuildingIDs.forEach(entityID -> {
                    unsearchedBuildings.add(worldInfo.getEntity(entityID));
                });
                unsearchedBuildings.sort((o1, o2) -> {
                    int l1 = worldInfo.getDistance(agentInfo.getID(), o1.getID());
                    int l2 = worldInfo.getDistance(agentInfo.getID(), o2.getID());
                    if (l1 > l2)       //increase
                        return 1;
                    if (l1 == l2)
                        return 0;

                    return -1;
                });
                this.unsearchedClusterBuildingList.clear();
                unsearchedBuildings.forEach(standardEntity -> {
                    unsearchedClusterBuildingList.add(standardEntity.getID());
                });
//              this.result = null;
            //    System.out.println("怎么就不动了" );
                for (int i = 0; i < 10; i++) {
                    if (!this.unsearchedBuildingIDList.isEmpty()||!this.unsearchedClusterBuildingList.isEmpty()) {
                        EntityID next = null;
                        if (world.isGather()) {
                      //      System.out.println("发生Agent聚集  ID:" ); //TODO
                            next = this.unsearchedClusterBuildingList.get(randomGenerator.nextInt(unsearchedClusterBuildingList.size()));
                        } else {
                            if(i<unsearchedClusterBuildingList.size()) {
                                next = unsearchedClusterBuildingList.get(i);
                                //System.out.println("找到目标在分区中");
                            }
                            if (next == null) {
                                if(i==0){
                                   prepareGetTargetInMap();
                                }
                                if(i<unsearchedClusterBuildingList.size()) {
                                    unsearchedClusterBuildingList.get(i);
                                }
                            }
                        }
                        if(next == null){
                           // System.out.println("在地图中没有找到目标");
                        }
                        else if (hasPath(next)){
                            toSearchTarget = next;
                            //System.out.println("此目标无法到达");
                            if(judgeBuilding(next)){
                                result=toSearchTarget;
                                return toSearchTarget;
                            }
                            else{
                                addToUnreachableTargets(next);
                            }
                        } else {
                            addToUnreachableTargets(next);
                        }
                    }
                }
            }
        }
        return null;
    }

    private EntityID searchBurnBuild(EntityID lastTarget){
        Set<EntityID> searchableEntityIds=world.getPossibleBurningBuildings();
        if (searchableEntityIds != null && !searchableEntityIds.isEmpty()) {
            if (lastTarget != null && searchableEntityIds.contains(lastTarget)) {
                if (hasPath(lastTarget)) {
                    return lastTarget;
                } else {
                    addToUnreachableTargets(lastTarget);
                }
            }

            List<StandardEntity> unsearchedBuildings = new ArrayList<>();
            searchableEntityIds.forEach(entityID -> {
                unsearchedBuildings.add(worldInfo.getEntity(entityID));
            });
            unsearchedBuildings.sort((o1, o2) -> {
                int l1 = worldInfo.getDistance(agentInfo.getID(), o1.getID());
                int l2 = worldInfo.getDistance(agentInfo.getID(), o2.getID());
                if (l1 > l2)       //increase
                    return 1;
                if (l1 == l2)
                    return 0;

                    return -1;
            });

            this.unsearchedBuildingIDList.clear();
            unsearchedBuildings.forEach(standardEntity -> {
                unsearchedBuildingIDList.add(standardEntity.getID());
            });
        }
//            this.result = null;
            for (int i = 0; i < 10; i++) {
                if (!this.unsearchedBuildingIDList.isEmpty()) {
                    EntityID next = this.unsearchedBuildingIDList.get(randomGenerator.nextInt(unsearchedBuildingIDList.size()));
                    if (hasPath(next)) {
                        toSearchTarget = next;
                        return toSearchTarget;
                    } else {
                        addToUnreachableTargets(next);
                    }
                }
            }

        return null;
    }
    private void prepareGetTargetInMap() {
        this.unsearchedBuildingIDList.clear();
        List<StandardEntity> unsearchedBuildings = new ArrayList<>();
        unsearchedBuildingIDs.forEach(entityID -> {
            unsearchedBuildings.add(worldInfo.getEntity(entityID));
        });
        unsearchedBuildings.sort((o1, o2) -> {
            int l1 = worldInfo.getDistance(agentInfo.getID(), o1.getID());
            int l2 = worldInfo.getDistance(agentInfo.getID(), o2.getID());
            if (l1 > l2)       //increase
                return 1;
            if (l1 == l2)
                return 0;

            return -1;
        });
        this.unsearchedBuildingIDList.clear();
        unsearchedBuildings.forEach(standardEntity -> {
            unsearchedBuildingIDList.add(standardEntity.getID());
        });
    }
    private boolean shouldCheckInside(BuildingModel building) {
        BuildingModel buildingModel = world.getBuildingModel(building.getID());
        if (buildingModel.getCenterVisitRoadShapes().isEmpty()) {
            if (building.getSelfBuilding().isFierynessDefined()) {
                int fieryness = building.getSelfBuilding().getFieryness();
                if (fieryness == 0 || fieryness == 4) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }
    protected void printDebugMessage(String msg) {
        ConsoleOutput.error("Agent:" + agentInfo.getID() + " Time:" + agentInfo.getTime() + " " + msg);
    }

    private void updateUnreachableTargets() {
        ArrayList<EntityID> toRemove = new ArrayList<EntityID>();
        int postponeTime;
        for (EntityID standardEntity : unreachableTargets.keySet()) {
            postponeTime = unreachableTargets.get(standardEntity);
            postponeTime--;
            if (postponeTime <= 0) {
                toRemove.add(standardEntity);
            } else {
                unreachableTargets.put(standardEntity, postponeTime);
            }

        }
        unreachableTargets.keySet().removeAll(toRemove);
    }

    private void addToUnreachableTargets(EntityID result) {
        int postponeTime = randomGenerator.nextInt(6) + 5;
        unreachableTargets.put(worldInfo.getEntity(result).getID(), postponeTime);
    }

    private boolean hasPath(EntityID result) {
        List<EntityID> idList = this.pathPlanning.setFrom(agentInfo.getPosition()).setDestination(result).calc().getResult();
        return idList != null && !idList.isEmpty();
    }

    @Override
    public EntityID getTarget() {
        return this.result;
    }

    @Override
    public Search precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        this.world.precompute(precomputeData);
        this.pathPlanning.precompute(precomputeData);
        this.clustering.precompute(precomputeData);
        return this;
    }

    @Override
    public Search resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        this.world.resume(precomputeData);
        this.worldInfo.requestRollback();
        this.pathPlanning.resume(precomputeData);
        this.clustering.resume(precomputeData);

        return this;
    }

    @Override
    public Search preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        this.world.preparate();
        this.worldInfo.requestRollback();
        this.pathPlanning.preparate();
        this.clustering.preparate();
        return this;
    }
}
