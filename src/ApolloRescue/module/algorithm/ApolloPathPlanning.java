package ApolloRescue.module.algorithm;

import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.newsearch.graph.A_Star;
import ApolloRescue.module.universal.newsearch.graph.GraphModule;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

public class ApolloPathPlanning extends PathPlanning {
    private List<EntityID> lastMovePlan = new ArrayList<>();
    private List<EntityID> previousPath = new ArrayList<>();
    private Set<EntityID> passableAreas = new HashSet<>();
    private Queue<EntityID> previousPositionsQueue = new LinkedList<>();
//    RoadInfoComponent roadInfoComponent;

    private Area previousTarget = null;
    private int continueMovingTime = 0;


    private EntityID prevTarget;
    private int illegalPlanCount;

//    private Map<EntityID, Set<EntityID>> graph;

    private EntityID from;
    //    private Collection<EntityID> targets;
    private List<EntityID> targets;
    private List<EntityID> result;
    private A_Star a_star;
    private ApolloWorld world;
    //    RoadHelper roadHelper;


    public ApolloPathPlanning(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);

        world = ApolloWorld.load(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);

        GraphModule graph = world.getGraphModule();
        a_star = new A_Star(ai, wi, si, moduleManager, developData, graph);
//        this.init();
    }

//    private void init() {
//        Map<EntityID, Set<EntityID>> neighbours = new LazyMap<EntityID, Set<EntityID>>() {
//            @Override
//            public Set<EntityID> createValue() {
//                return new HashSet<>();
//            }
//        };
//        for (Entity next : this.worldInfo) {
//            if (next instanceof Area) {
//                Collection<EntityID> areaNeighbours = ((Area) next).getNeighbours();
//                neighbours.get(next.getID()).addAll(areaNeighbours);
//            }
//        }
//        this.graph = neighbours;
//    }


    @Override
    public PathPlanning precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        world.precompute(precomputeData);
        return this;
    }

    @Override
    public PathPlanning resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
       world.resume(precomputeData);
        return this;
    }

    @Override
    public PathPlanning preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        world.preparate();
        return this;
    }

    @Override
    public PathPlanning updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        world.updateInfo(messageManager);
        return this;
    }

    @Override
    public List<EntityID> getResult() {
        return this.result;
    }

    @Override
    public PathPlanning setFrom(EntityID id) {
        this.from = id;
        return this;
    }

    @Override
    public PathPlanning setDestination(Collection<EntityID> targets) {
        this.targets = new ArrayList<>(targets);
        List<EntityID> toRemoves = new ArrayList<>();
        List<EntityID> toAdd = new ArrayList<>();
        for (EntityID id : targets) {
            StandardEntity entity = worldInfo.getEntity(id);
            if (!(entity instanceof Area)) {
                if (entity instanceof Human) {
                    toAdd.add(((Human) entity).getPosition());
                    toRemoves.add(entity.getID());
                } else {    //若id处无Human，则从targets中移除
                    toRemoves.add(entity.getID());
                }
            }
        }
        this.targets.removeAll(toRemoves);
        this.targets.addAll(toAdd);
        return this;
    }

    public static class DistanceComparator implements Comparator<EntityID> {
        private WorldInfo worldInfo;
        private AgentInfo agentInfo;

        public DistanceComparator(WorldInfo worldInfo, AgentInfo agentInfo) {
            this.worldInfo = worldInfo;
            this.agentInfo = agentInfo;
        }

        @Override
        public int compare(EntityID o1, EntityID o2) {
            int d1 = Util.distance(worldInfo.getLocation(o1), worldInfo.getLocation(agentInfo.getID()));
            int d2 = Util.distance(worldInfo.getLocation(o2), worldInfo.getLocation(agentInfo.getID()));
//            return d1 > d2 ? 1 : d1 < d2 ? -1 : 0;
            return Integer.compare(d1, d2);
        }
    }

    private EntityID nearestTarget = null;

    @Override
    public PathPlanning calc() {

        Area sourceArea = (Area) worldInfo.getEntity(from);

        result = null;
        List<EntityID> planMove = null;
        if (nearestTarget != null && targets.contains(nearestTarget)) {

            Area target = (Area) worldInfo.getEntity(nearestTarget);
            planMove = new ArrayList<>(planMove(sourceArea, target, 0, false));
            result = planMove;
        }

        if (result == null || result.isEmpty()) {
            targets.sort(new DistanceComparator(worldInfo, agentInfo));
            for (EntityID target1 : targets) {
                Area target = (Area) worldInfo.getEntity(target1);
                nearestTarget = target1;

                planMove = new ArrayList<>(planMove(sourceArea, target, 0, false));
                if (!planMove.isEmpty()) {
                    result = planMove;
                    break;
                }
            }
        }

        if (result != null && result.isEmpty()) {
            result = null;
        }

//        Area target = (Area) worldInfo.getEntity(targets.get(0));
//
//        result = planMove(sourceArea, target, 0, false);

        return this;

        //        List<EntityID> open = new LinkedList<>();
//        Map<EntityID, EntityID> ancestors = new HashMap<>();
//        open.add(this.from);
//        EntityID next;
//        boolean found = false;
//        ancestors.put(this.from, this.from);
//        do {
//            next = open.remove(0);
//            if (isGoal(next, targets)) {
//                found = true;
//                break;
//            }
//            Collection<EntityID> neighbours = graph.get(next);
//            if (neighbours.isEmpty()) {
//                continue;
//            }
//            for (EntityID neighbour : neighbours) {
//                if (isGoal(neighbour, targets)) {
//                    ancestors.put(neighbour, next);
//                    next = neighbour;
//                    found = true;
//                    break;
//                }
//                else {
//                    if (!ancestors.containsKey(neighbour)) {
//                        open.add(neighbour);
//                        ancestors.put(neighbour, next);
//                    }
//                }
//            }
//        } while (!found && !open.isEmpty());
//        if (!found) {
//            // No path
//            this.result = null;
//        }
//        // Walk back from goal to this.from
//        EntityID current = next;
//        LinkedList<EntityID> path = new LinkedList<>();
//        do {
//            path.add(0, current);
//            current = ancestors.get(current);
//            if (current == null) {
//                throw new RuntimeException("Found a node with no ancestor! Something is broken.");
//            }
//        } while (current != this.from);
//        this.result = path;
//        return this;
    }

    public List<EntityID> planMove(Area sourceArea, Area target, int maxDistance, boolean force) {  //A*

        List<EntityID> finalAreaPath = new ArrayList<>();

        if (target == null) {
            return finalAreaPath;
        }
        if (!target.getID().equals(prevTarget)) {
            prevTarget = target.getID();
        }

        if (sourceArea.equals(target)) {
//            System.out.println("Time:" + agentInfo.getTime() + " Already on target move =" + target);
            return finalAreaPath;
        }
        boolean repeatPlanning = repeatPlanning(target);
        boolean repeatAStar = !isPositionOnPreviousPath(sourceArea.getID());// 若原地点不在先前的路径上，则需要重新规划路径
        if (repeatAStar || repeatPlanning) {
            // tanha zamani ke niaz bashe dobare a* mizane ta masir peida kone.
            // yani vaghti yeki az sharayete bala true bashe.
            Area nearestTarget = null;
            previousPath.clear();

            if (maxDistance != 0) {
                // 重新规划路径，在maxDistance范围内 获取nearestTarget
                // agar gharar nabood hatman dar mahale target gharar begirad.
                // ba estefade az maxDistance makan haei ke mitavanad dar aanha gharar begirad ra bedast miavarim.
                nearestTarget = getNearestArea(worldInfo.getObjectsInRange(target, maxDistance), target);
            }
            if (nearestTarget == null) {
                // 目标地较远
                nearestTarget = target;
            }

            // A* be ma te'dadi entityId mide ke marboot be area haei ke bahas agent azashoon oboor kone.

            finalAreaPath = a_star.getShortestPath(sourceArea, nearestTarget, force);

            previousTarget = nearestTarget;
            previousPath = finalAreaPath;

        } else if (previousTarget.equals(target)) {
            // baraye zamani ke meghdari az masir ra amade ast.
            // az pathe ghabli ta position alan ra hazf mikonim va pathe jadid bedast miayad.
            ArrayList<EntityID> temp = new ArrayList<EntityID>();

            for (EntityID aPreviousPath : previousPath) {
                if (!sourceArea.getID().equals(aPreviousPath)) {
                    temp.add(aPreviousPath);
                } else {
                    break;
                }
            }

            previousPath.removeAll(temp);
            finalAreaPath = previousPath;
        }
        return finalAreaPath;
    }

    protected Area getNearestArea(Collection entities, Area to) {
        // entekhabe yek area ke az hame nazdiktar ast baraye zamani ke be yek collection move mizanad.

        int distance = Integer.MAX_VALUE;
        Area nearestArea = null;

        for (Object o : entities) {
            StandardEntity next = (StandardEntity) o;
            if (!(next instanceof Road)) {
                continue;
            }
            int dis = worldInfo.getDistance(to, next);
            if (dis < distance) {
                distance = dis;
                nearestArea = (Area) next;
            }
        }
        return nearestArea;
    }

    private boolean repeatPlanning(Area target) {
        if (previousTarget == null || !previousTarget.equals(target) || continueMovingTime > 1) {
            continueMovingTime = 0;
            return true;
        } else {
            continueMovingTime++;
            return false;
        }
    }

    private boolean isPositionOnPreviousPath(EntityID position) {
        return previousPath.contains(position);
    }


    private boolean isGoal(EntityID e, Collection<EntityID> test) {
        return test.contains(e);
    }
}
