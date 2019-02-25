package ApolloRescue.extaction.fireFighting;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.algorithm.clustering.ApolloFireClustering;
import ApolloRescue.module.algorithm.clustering.ApolloFireZone;
import ApolloRescue.module.algorithm.clustering.Cluster;
import ApolloRescue.module.complex.firebrigade.*;
import ApolloRescue.module.complex.firebrigade.search.FireZoneSearchMethod;
import ApolloRescue.module.complex.firebrigade.search.HighValueBasedFireZoneSearch;
import ApolloRescue.module.complex.firebrigade.search.IFireZoneSearch;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.tools.ActionException;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.view.HumanLayer;
import rescuecore2.worldmodel.EntityID;
import ApolloRescue.module.universal.JobState;


import java.util.ArrayList;
import java.util.List;

public class AdvancedExtinguishFire {

    private FireBrigadeWorld world;
    private JobState jobState;
    private ExtinguishTarget extinguishTarget;
    private List<EntityID> checkPath;
    private FireBrigadeTargetSelectorType targetSelectorType = FireBrigadeTargetSelectorType.FULLY_GREEDY;
    private IFireZoneSearch fireZoneSearch;
    private ApolloFireClustering fireClustering;
    private AbstractFBTargetSelector targetSelector;
    private FireZoneSearchMethod fireZoneSearchMethod;
    private ActionException finishFireZoneSearchFlag = null;
    protected ScenarioInfo scenarioInfo;
    protected AgentInfo agentInfo;
    protected WorldInfo worldInfo;
    protected ModuleManager moduleManager;

    public AdvancedExtinguishFire(FireBrigadeWorld world,AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager) {
        this.world = world;
        this.worldInfo = wi;
        this.agentInfo = ai;
        this.scenarioInfo = si;
        this.moduleManager = moduleManager;
        this.fireZoneSearch = world.getFireZoneSearch();
        fireZoneSearchMethod = new FireZoneSearchMethod(
                world);
//        this.extinguishMethod = new DirectionBasedExtinguishMethod(world);
        setTargetSelectorMethod();
    }

    public boolean shouldExecute() {
        System.out.println("Time: " + world.getTime() + " Agent Id:" + world.getAgentInfo().getID() +"\tusing AdvancedExtingushFireFBJob.shouldExecute()");
//        Cluster targetZone = fireClustering
//                .findNearestCluster(world.getWorldInfo().getLocation(world.getAgentInfo().getID()));
        ApolloFireZone targetZone = (ApolloFireZone) world.getFireClustering().findNearestCluster((worldInfo.getLocation(agentInfo.getID())));
        if (targetZone != null) {
            if (!targetZone.isControllable() && targetSelectorType.equals(FireBrigadeTargetSelectorType.FULLY_GREEDY)) {
                targetSelectorType = FireBrigadeTargetSelectorType.DIRECTION_BASED;
                setTargetSelectorMethod();;

            } else if (targetZone.isControllable() && targetSelectorType.equals(FireBrigadeTargetSelectorType.DIRECTION_BASED)) {
                System.out.println("Time: " + world.getTime() + " Agent Id: " +  world.getAgentInfo().getID() + "\tinto targetZone.isControllable()");
                targetSelectorType = FireBrigadeTargetSelectorType.FULLY_GREEDY;
                setTargetSelectorMethod();;
            }
        }
        System.out.println("Time: " + world.getTime() + " Agent Id:" + world.getAgentInfo().getID() +"\ttarget selector type is " + targetSelectorType);
//		if(targetSelector.shouldGetTarget(targetZone)) {

        extinguishTarget = targetSelector.getTarget(targetZone);
        int judgeTime = 0;
        boolean judgedNotExtinguishTarget = false;
        boolean shouldExtinguishTarget = true;

//		System.out.println("Time:" + world.getTime() + " AgentId:" + world.getSelfHuman().getID() + "\t is companion decrease : " + world.isCompanionDecrease());
//		System.out.println("Time:" + world.getTime() + " AgentId:" + world.getSelfHuman().getID() + "\tshouldExtinguish it : " + shouldExtinguishIt(extinguishTarget));
        //2016 09 15 14 46
        if (world.isInitTimeGather() && world.getTime() < 61) {
            shouldExtinguishTarget = shouldExtinguishIt(extinguishTarget);
            if (shouldExtinguishTarget == false)
                judgedNotExtinguishTarget = true;
            if (world.getTime() > world.getJudgeTime() + 5) {
                world.setJudgeTime(world.getTime());
            }
        }
        if (!shouldExtinguishTarget || judgedNotExtinguishTarget && world.getTime() - world.getJudgeTime() < 5) {
            return false;
        }

        if (fireZoneSearch.shouldSearch(extinguishTarget)) {
            try {
//                System.out.println("Time: " + world.getTime() + " Agent Id: " +  + "\t try fireZoneSearchMethod.execute()");
                fireZoneSearchMethod.execute();
            } catch (ActionException e) {
                finishFireZoneSearchFlag = e;
                jobState = JobState.CheckFireZone;
                return true;
            }
        }
//		}

        if (extinguishTarget != null) {
            jobState = JobState.Extinguishing;
            return true;
        }

        return false;
    }

//    public void execute() throws ActionException {
////        updateViewer(extinguishTarget);
//        System.out.println("Time: " + world.getTime() + " Agent Id:" + world.getAgentInfo().getID() + "\tusing AdvancedExtingushFireFBJob.execute()");
//        if(jobState == JobState.CheckFireZone) {
////            if(finishFireZoneSearchFlag == null) {
////                ConsoleDebug.printFB("finishFireZoneSearchFlag is null");
////            }
////			System.out.println("Check fire job");
//            throw finishFireZoneSearchFlag;
//        } else {
//            if (extinguishTarget.getTarget() == null) {
//                System.out.println("灭火目标为空");
//            }
//            // System.out.println("灭火任务："+extinguishTarget.getTarget().getID());
//
//            // set extinguish target
//            me.setLastExtinguishTarget(extinguishTarget);
//            this.extinguish(extinguishTarget);
//        }
//
//    }

//    private void checkFire() {
//        if (extinguishTarget == null) {
//            jobState = JobState.Inactive;
//            return;
//        }
//        checkPath = me.getPathPlanner().planMove(
//                (Area) world.getSelfPosition(),
//                extinguishTarget.getTarget().getSelfBuilding(), 0, true);
//        if (checkPath != null && checkPath.size() > 1) {
//            checkPath.remove(checkPath.size() - 1);
//            jobState = JobState.CheckFireZone;
//        } else {
//            jobState = JobState.Inactive;
//        }
//    }

    private void setTargetSelectorMethod() {
        System.out.println("Time: " + world.getTime() + "\tusing AdvancedExtingushFireFBJob.setTargetSelectorMethod()");
        switch (targetSelectorType) {
            case FULLY_GREEDY:
                targetSelector = new GreedyFireBrigadeTargetSelector(world);
                break;
            case DIRECTION:
                targetSelector = new HybridFireBrigadeTargetSelector(world);
                break;
            default:
                targetSelector = new HybridFireBrigadeTargetSelector(world);
                break;
        }

    }

//    private void updateViewer(ExtinguishTarget target) {
//        if (target == null) {
//            if (ApolloConstants.LAUNCH_VIEWER) {
//                HumanLayer.FIRE_BRIGADE_TARGETS_TO_GO_MAP.put(
//                        commonAgent.getID(), null);
//            }
//        } else {
//            if (ApolloConstants.LAUNCH_VIEWER) {
//                HumanLayer.FIRE_BRIGADE_TARGETS_TO_GO_MAP.put(
//                        commonAgent.getID(), target.getTarget().getID());
//            }
//        }
//    }

    public boolean shouldExtinguishIt(ExtinguishTarget target) {
        if (world.getBiggestGatherEvent()[0] == 0 && world.getBiggestGatherEvent()[1] > 50 ) {
            return true;
        }
        if (!world.isCompanionDecrease()) {
            return true;
        }
        System.out.println();
        //0907
        if (target == null) {
            System.out.println("target is null !!!!!!!!!!");
            return false;
        }

        List<Integer> Xs = new ArrayList<Integer>();
        List<Integer> Ys = new ArrayList<Integer>();
        for (Pair<Integer, Integer> temp : world.getHistoryLocation() ) {
            Xs.add(temp.first());
            Ys.add(temp.second());
        }
        int[][] vectors = new int[Xs.size()+1][2];
        vectors[0][0] = Xs.get(0);
        vectors[0][1] = Ys.get(1);
        for (int i=1; i < Xs.size(); i++) {
            int x = Xs.get(i) - Xs.get(i-1);
            int y = Ys.get(i) - Ys.get(i-1);
            vectors[i][0] = x;
            vectors[i][1] = y;
        }
        vectors[vectors.length-1][0] = Xs.get(Xs.size()-1) - Xs.get(0);
        vectors[vectors.length-1][1] = Ys.get(Ys.size()-1) - Ys.get(0);
        // 0906
        int length = Xs.size();
        int selfX = world.getWorldInfo().getLocation(world.getAgentInfo().getID()).first();
        int selfY = world.getWorldInfo().getLocation(world.getAgentInfo().getID()).second();
        int selfVX = selfX - Xs.get(0);
        int selfVY = selfY - Ys.get(0);
        int targetVX = target.getTarget().getSelfBuilding().getX() - Xs.get(0);
        int targetVY = target.getTarget().getSelfBuilding().getY() - Ys.get(0);
        int eX = 0; //单位基向量坐标x
        int eY = 0;
        if (selfX == Xs.get(0) && selfY == Ys.get(0)) //原地
            return true;
//		if (Ys.get(0) == Ys.get(1) && Xs.get(0) != Xs.get(1)) {
//			eX = Xs.get(0);
//		} else if (Ys.get(0) == Ys.get(1) ) {
//
//		}

        int index = 0; //非0向量指针 vector[index][0||1] != 0
        for (int i=0; i < Xs.size(); i++) {
            if(Xs.get(i) != Xs.get(i+1) || Ys.get(i) != Ys.get(i+1)) {
                index = i+1;
                break;
            }
        }

        //直线两侧分布统计
        int plusSideCnt = 0; //左和上
        int minusSideCnt = 0;
        int plusSideSum = 0;
        int minusSideSum = 0;
        //l: y = (selfY - y0)/(selfX - x0) * x + y0
        if (selfX != Xs.get(0) && selfY == Ys.get(0)) {
            for (int i=1; i < length; i++)
                if (Ys.get(i) > selfY) {
                    plusSideSum += Ys.get(i) - selfY;
                    plusSideCnt++;
                } else if (Ys.get(i) < selfY) {
                    minusSideCnt++;
                    minusSideSum += selfY - Ys.get(i);
                }
        } else if (selfX == Xs.get(0) && selfY != Ys.get(0)) {
            for (int i=1; i < length; i++)
                if (Xs.get(i) < selfX) {
                    plusSideCnt++;
                    plusSideSum += selfX - Xs.get(i);
                } else if (Xs.get(i) > selfX) {
                    minusSideCnt++;
                    minusSideSum += Xs.get(i) - selfX;
                }
        } else {
            for (int i=1; i < length; i++) {
                int lineX = Xs.get(i);
                int lineY = (int)(1.0 * (selfY - Ys.get(0)) / (selfX - Xs.get(0)) * lineX + Ys.get(0));
                if (lineY > Ys.get(i)) {
                    plusSideCnt++;
                    plusSideSum += lineY - Ys.get(i);
                } else if (lineY < Ys.get(i)) {
                    minusSideCnt++;
                    minusSideSum += Ys.get(i) - lineY;
                }
            }
        }
        int signX = 1;
        int signY = 1;
        if (plusSideSum * 1.0 / plusSideCnt > minusSideSum * 1.0 / minusSideCnt) {
            signY = -1;
        } else if (plusSideSum * 1.0 / plusSideCnt < minusSideSum * 1.0 / minusSideCnt) {
            signX = -1;
        }

        //向量平行坐标轴时的坐标数值处理    unsigned
        if (Xs.get(index) != Xs.get(index-1) && Ys.get(index) == Ys.get(index-1)) { //平行x轴
//			eY = Ys.get(0) + signY * (Math.abs(Xs.get(index) - Xs.get(index-1)));
            eY = signY * (Math.abs(Xs.get(index) - Xs.get(index-1)));
            eX = 0;
        } else if (Xs.get(index) == Xs.get(index-1) && Ys.get(index) != Ys.get(index-1)) { //平行y轴
//			eX = Xs.get(0) + signX * (Math.abs(Ys.get(index) - Ys.get(index-1)));
//			eY = Ys.get(0);
            eX = signX * (Math.abs(Ys.get(index) - Ys.get(index-1)));
            eY = 0;
        } else {
//			eX = Xs.get(0) + signX * (Math.abs(Ys.get(index) - Ys.get(index-1)));
//			eY = (int)(-1.0 * (Xs.get(index) - Xs.get(0))) / (Ys.get(index) - Ys.get(0)) * (eX - Xs.get(0)) + Ys.get(0);
            eX = signX * (Math.abs(Ys.get(index) - Ys.get(index-1)));
            eY = (int)(-1.0 * (Xs.get(index) - Xs.get(0))) / (Ys.get(index) - Ys.get(0)) * (eX - Xs.get(0));
        }

        //基向量和目标向量的关系 TODO
        System.out.println("Time:" + world.getTime() + " AgentId: " + world.getSelfHuman().getID() + "\tVectors");
        System.out.println("eX :" + eX + "\teY :" + eY);
        System.out.println("selfVX :" + selfVX + "\tselfVY :" + selfVY);
        System.out.println("targetVX :" + targetVX + "\ttargetVY :" + targetVY);
        double alpha = 180.0 / Math.PI * Math.acos((selfVX * targetVX + selfVY * targetVY)/
                (Math.sqrt(Math.pow(selfVX, 2) + Math.pow(selfVY, 2)) * Math.sqrt(Math.pow(targetVX, 2) + Math.pow(targetVY, 2))));//selfVector和targetVector的夹角
        double beta = 180.0 / Math.PI * Math.acos((eX * targetVX + eY * targetVY)/
                (Math.sqrt(Math.pow(eX, 2) + Math.pow(eY, 2)) * Math.sqrt(Math.pow(targetVX, 2) + Math.pow(targetVY, 2))));//baseVector和targetVector的夹角
        System.out.println("Time:" + world.getTime() + " AgentId: " + world.getSelfHuman().getID() + "\t alpha is " + alpha);
        System.out.println("Time:" + world.getTime() + " AgentId: " + world.getSelfHuman().getID() + "\t beta  is " + beta);
//		数值不完善，待测
        if (beta > 95 || beta == 95 && alpha < 10) {
            return true;
        } else if (beta < 90 && alpha < 85) {
            if (Util.distance(targetVX, targetVY, 0, 0) > 1.5 * Util.distance(selfVX, selfVY, targetVX, targetVY)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
        //done
//		return true;
    }
}
