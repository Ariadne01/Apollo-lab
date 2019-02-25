package ApolloRescue.module.complex;

import ApolloRescue.module.complex.firebrigade.BuildingProperty;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.knd.ApolloCommunication;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.communication.standard.bundle.information.MessageBuilding;
import adf.agent.communication.standard.bundle.information.MessageCivilian;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.HumanDetector;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import adf.component.communication.CommunicationMessage;
import adf.agent.communication.standard.bundle.MessageUtil;

import java.awt.geom.Rectangle2D;
import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;
import static rescuecore2.standard.entities.StandardEntityURN.CIVILIAN;
import static rescuecore2.standard.entities.StandardEntityURN.REFUGE;

public class ApolloHumanDetector extends HumanDetector {
    private Clustering clustering;
    private PathPlanning pathPlanning;
    private List<EntityID> activeCivilian;
    private EntityID result;
    private List<Human> rescueTargets;
    private ApolloCommunication communication;
    private int sendTime;
    private int sendingAvoidTimeClearRequest;

    private Collection<EntityID> agentPositions;
    private Map<EntityID, Integer> sentTimeMap;
    private int sendingAvoidTimeReceived;
    private int sendingAvoidTimeSent;
    private int moveDistance;
    private EntityID lastPosition;
    private int positionCount;
    private List<EntityID> vistedcivilian;
    private Map<EntityID, BuildingProperty> sentBuildingMap;
    private EntityID lastResult;
    private ApolloWorld apolloWorld;

    public ApolloHumanDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);


        this.result = null;
        this.sendTime = 0;
        this.vistedcivilian = new LinkedList<>();
        this.sendingAvoidTimeClearRequest = developData.getInteger("SampleHumanDetector.sendingAvoidTimeClearRequest", 5);
        this.rescueTargets = new ArrayList<>();
        this.agentPositions = new HashSet<>();
        this.sentTimeMap = new HashMap<>();
        this.sendingAvoidTimeReceived = developData.getInteger("SampleHumanDetector.sendingAvoidTimeReceived", 3);
        this.sendingAvoidTimeSent = developData.getInteger("SampleHumanDetector.sendingAvoidTimeSent", 5);
        this.moveDistance = developData.getInteger("SampleHumanDetector.moveDistance", 40000);
        this.activeCivilian = new LinkedList<>();
        this.communication = new ApolloCommunication(ai, wi, si, developData);
        this.sentTimeMap = new HashMap<>();
        this.sentBuildingMap = new HashMap<>();
        this.sendingAvoidTimeReceived = developData.getInteger("ApolloBuildingDetector.sendingAvoidTimeReceived", 3);
        this.sendingAvoidTimeSent = developData.getInteger("ApolloBuildingDetector.sendingAvoidTimeSent", 5);
        switch (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("HumanDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("HumanDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("HumanDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("HumanDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("HumanDetector.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                this.clustering = moduleManager.getModule("HumanDetector.Clustering", "adf.sample.module.algorithm.SampleKMeans");
                break;
        }
        this.apolloWorld=ApolloWorld.load(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);

    }

    @Override
    public HumanDetector updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2) {
            return this;
        }
        this.communication.updateInfo(messageManager);
        this.clustering.updateInfo(messageManager);
        this.pathPlanning.updateInfo(messageManager);

        this.reflectMessage(messageManager);

        Set<StandardEntity> inBuildingAmbulances = new HashSet<>();
        worldInfo.getChanged().getChangedEntities().forEach(id -> {
            StandardEntity entity = worldInfo.getEntity(id);
            if (entity instanceof AmbulanceTeam) {
                StandardEntity position = worldInfo.getPosition(entity.getID());
                if (!entity.getID().equals(agentInfo.getID()) && position != null
                        && position instanceof Building
                        && position.getID().equals(agentInfo.getPosition())) {
                    inBuildingAmbulances.add(entity);
                }
            }
        });
       worldInfo.getChanged().getChangedEntities().forEach(id -> {
            StandardEntity entity = worldInfo.getEntity(id);
            if (entity instanceof Building) {
                Building building = (Building) worldInfo.getEntity(id);
                if (building.isFierynessDefined() && building.getFieryness() > 0 /*|| building.isTemperatureDefined() && building.getTemperature() > 0*/) {
                    BuildingProperty buildingProperty = sentBuildingMap.get(id);
                    if (buildingProperty == null || buildingProperty.getFieryness() != building.getFieryness() || buildingProperty.getFieryness() == 1) {
//                        printDebugMessage("burningBuilding: " + building.getID());
                        messageManager.addMessage(new MessageBuilding(true, building));
                        messageManager.addMessage(new MessageBuilding(false, building));
                        System.out.println("yes find Building");
                        sentBuildingMap.put(id, new BuildingProperty(building));
                    }
                }
            } else if (entity instanceof Civilian) {
                Civilian civilian = (Civilian) entity;
                if ((civilian.isHPDefined() && civilian.getHP() > 1000 && civilian.isDamageDefined() && civilian.getDamage() > 0)
                        || ((civilian.isPositionDefined() && !(worldInfo.getEntity(civilian.getPosition()) instanceof Refuge))
                        && (worldInfo.getEntity(civilian.getPosition()) instanceof Building))) {
                    messageManager.addMessage(new MessageCivilian(true, civilian));
                    messageManager.addMessage(new MessageCivilian(false, civilian));
//                    System.out.println(" CIVILIAN_MESSAGE: " + agentInfo.getTime() + " " + agentInfo.getID() + " --> " + civilian.getID());
                }

            }
        });
/*
        int currentTime = this.agentInfo.getTime();
        Human agent = (Human)this.agentInfo.me();
        int agentX = agent.getX();
        int agentY = agent.getY();
        StandardEntity positionEntity = this.worldInfo.getPosition(agent);
        if(positionEntity instanceof Road) {
            Road road = (Road)positionEntity;
            if(road.isBlockadesDefined() && road.getBlockades().size() > 0) {
                for(Blockade blockade : this.worldInfo.getBlockades(road)) {
                    if(blockade == null || !blockade.isApexesDefined()) {
                        continue;
                    }
                    if(this.isInside(agentX, agentY, blockade.getApexes())) {
                        if ((this.sendTime + this.sendingAvoidTimeClearRequest) <= currentTime) {
                            this.sendTime = currentTime;
                            messageManager.addMessage(
                                    new CommandPolice(
                                            true,
                                            null,
                                            agent.getPosition(),
                                            CommandPolice.ACTION_CLEAR
                                    )
                            );
                            break;
                        }
                    }
                }
            }
            if(this.lastPosition != null && this.lastPosition.getValue() == road.getID().getValue()) {
                this.positionCount++;
                if(this.positionCount > this.getMaxTravelTime(road)) {
                    if ((this.sendTime + this.sendingAvoidTimeClearRequest) <= currentTime) {
                        this.sendTime = currentTime;
                        messageManager.addMessage(
                                new CommandPolice(
                                        true,
                                        null,
                                        agent.getPosition(),
                                        CommandPolice.ACTION_CLEAR
                                )
                        );
                    }
                }
            } else {
                this.lastPosition = road.getID();
                this.positionCount = 0;
            }
        }
        */
        return this;
    }

    @Override
    public HumanDetector calc() {
        Human transportHuman = this.agentInfo.someoneOnBoard();
        if (transportHuman != null) {
            this.result = transportHuman.getID();
            return this;
        }
        if (this.nullResult()) {
            this.result = null;
        }
        if (this.result == null) {
            this.result = this.calcTarget();
        }
        if(this. result == null){
           System.out.println("AT 目标没有啊！！！！！！" + this.agentInfo.getID()+ " 目标ＩＤ是"+this.result+" "+this.agentInfo.getTime());
        }
        else{
            System.out.println("AT 有目标啊！！！！！！"+this.agentInfo.getID()+ "目标ＩＤ是 "+ this.result+ this.agentInfo.getTime());
        }
        return this;
    }

    private boolean nullResult() {
        if (this.result != null) {
            Human target = (Human) this.worldInfo.getEntity(this.result);
            if (target != null) {
                if (!target.isHPDefined() || target.getHP() == 0) {
                    return true;
                } else if (!target.isPositionDefined()) {
                    return true;
                } else if (this.civilianDoesNotNeedRescue(target)) {
                    return true;
                } else if (this.changeCivilianView()) {
                    return true;
                } else if (target.getPosition().equals(agentInfo.getPosition())
                        && !agentInfo.getChanged().getChangedEntities().contains(target.getID())) {
                    this.activeCivilian.add(target.getID());
                    return true;
                } else {
                    StandardEntity position = this.worldInfo.getPosition(target);
                    if (position != null) {
                        StandardEntityURN positionURN = position.getStandardURN();
                        if (positionURN.equals(REFUGE) || positionURN.equals(AMBULANCE_TEAM)) {
                            return true;
                        }
                    }
                }
                if ((target.isHPDefined() && target.getHP() < 600)
                        || (target.isBuriednessDefined() && target.getBuriedness() >= 5)) {
                    StandardEntity targetPosition = worldInfo.getEntity(target.getPosition());
                    if (targetPosition != null
                            & targetPosition instanceof Building) {
                        if (((Building) targetPosition).isOnFire()) {
                            return true;
                        }
                    }
                }

            }
            if (target instanceof AmbulanceTeam
                    || target instanceof FireBrigade
                    || target instanceof PoliceForce) {
                if (target.isBuriednessDefined() && target.getBuriedness() == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private EntityID calcTarget() {
        Rectangle2D band = worldInfo.getBounds();
        final double distansTORescureAgent = ((band.getMaxX() - band.getMinX()) + (band.getMaxY() - band.getMinY())) / 2 / 3;
        final double distanseAzCivilian = scenarioInfo.getPerceptionLosMaxDistance() * 3;
        this.rescueTargets.clear();
        List<Human> loadTargets = new ArrayList<>();
        List<EntityID> rescueTargetPosision = new ArrayList<>();
        List<EntityID> loadTargetPosision = new ArrayList<>();
        List<Human> rescueAgent = new ArrayList<>();
        List<EntityID> rescueAgentPosision = new ArrayList<>();


        for (StandardEntity next : this.worldInfo.getEntitiesOfType(AMBULANCE_TEAM, FIRE_BRIGADE, POLICE_FORCE)) {
            Human h = (Human) next;
            StandardEntity positionEntity = this.worldInfo.getPosition(h);
            if (this.agentInfo.getID().equals(h.getID())) {
                continue;
            }
            if (positionEntity instanceof Area) {
                Area area = (Area) positionEntity;
                if (h.isPositionDefined() && this.getDistance(agentInfo.getX(), agentInfo.getY(), area.getX(), area.getY()) > distansTORescureAgent) {
                    continue;
                }
            }

            if (positionEntity != null) {
                if (h.isHPDefined() && h.isBuriednessDefined() && h.getHP() > 0 && h.getBuriedness() > 0) {
                    if (activeTime(h) > h.getBuriedness()) {
                        rescueAgent.add(h);
                        rescueAgentPosision.add(h.getPosition());
                    }
                }
            }
        }
        this.pathPlanning.setFrom(agentInfo.getPosition());
        if (rescueAgentPosision.size() > 0) {
            this.pathPlanning.setDestination(rescueAgentPosision);
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                EntityID targetPosision = path.get(path.size() - 1);
                for (Human h : rescueAgent) {
                    if (h.getPosition().equals(targetPosision)) {
                        return h.getID();
                    }
                }
            }
        }
        if(lastResult != null){
            if(((Human)worldInfo.getEntity(lastResult)).getBuriedness()!=0){
                return lastResult;
            }

        }
        Collection<EntityID> range = worldInfo.getObjectIDsInRange(agentInfo.me(),this.apolloWorld.getViewDistance());
        for(EntityID next : range){
            if(this.worldInfo.getEntity(next).getStandardURN().equals(StandardEntityURN.CIVILIAN)){
                Human thisHuman=  (Human)(this.worldInfo.getEntity(next)) ;
                if(thisHuman.getBuriedness()>0){
                    return next;
                }

            }
        }
        for (StandardEntity next : this.worldInfo.getEntitiesOfType(CIVILIAN)) {
            Human h = (Human) next;
            StandardEntity positionEntity = this.worldInfo.getPosition(h);

            if (this.activeCivilian.contains(next.getID()))
                continue;
            if (positionEntity instanceof Area) {
                Area area = (Area) positionEntity;
                if (h.isPositionDefined() && this.getDistance(agentInfo.getX(), agentInfo.getY(), area.getX(), area.getY()) > distanseAzCivilian) {
                    continue;
                }
            }
            this.vistedcivilian.add(next.getID());

            if (positionEntity != null && positionEntity instanceof Area && positionEntity.getStandardURN() != REFUGE) {
                if (this.needRescueFromCollapse(h)) {
                    rescueTargets.add(h);
                    rescueTargetPosision.add(h.getPosition());
                } else if (this.needRescueFromRoad(h)) {
                    loadTargets.add(h);
                    loadTargetPosision.add(h.getPosition());
                }
            }
        }


        this.removeDangerPos(rescueTargetPosision);
        EntityID thisTimeTarget;
        List<EntityID> rescueTargeWithLowBuriednessPosision = new ArrayList<>();
        for (Human human : this.rescueTargets) {
            if (human.isBuriednessDefined() && human.getBuriedness() < 25) {
                rescueTargeWithLowBuriednessPosision.add(human.getPosition());
            }
        }
        if (rescueTargeWithLowBuriednessPosision.size() > 0) {
            this.pathPlanning.setDestination(rescueTargeWithLowBuriednessPosision);
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                EntityID targetPosision = path.get(path.size() - 1);

                   thisTimeTarget  =this.getWeakerCivilian(targetPosision);
                   lastResult=thisTimeTarget;
                return thisTimeTarget;
            }
        }


        if (rescueTargets.size() > 0) {
            this.pathPlanning.setDestination(rescueTargetPosision);
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                EntityID targetPosision = path.get(path.size() - 1);

                thisTimeTarget  =this.getWeakerCivilian(targetPosision);
                lastResult=thisTimeTarget;
                return thisTimeTarget;
            }
        }

        if (loadTargets.size() > 0) {
            this.pathPlanning.setDestination(loadTargetPosision);
            List<EntityID> path = this.pathPlanning.calc().getResult();
            if (path != null && path.size() > 0) {
                EntityID targetPosision = path.get(path.size() - 1);
                thisTimeTarget  =this.getWeakerCivilian(targetPosision);
                lastResult=thisTimeTarget;
                return thisTimeTarget;
            }
        }
        lastResult = null;
        return null;
    }

    private List<EntityID> removeDangerPos(List<EntityID> targetPos) {
        final int removeRangeforEachBurdness = 1100;
        List<Human> removeCivilian = new LinkedList<>();
        List<EntityID> removePos = new LinkedList<>();

        for (Human civilian : this.rescueTargets) {
            Collection<EntityID> range = worldInfo.getObjectIDsInRange(civilian, (civilian.getBuriedness() + 30) * removeRangeforEachBurdness);
            for (EntityID id : range) {
                StandardEntity entity = worldInfo.getEntity(id);
                if (entity instanceof Building) {
                    Building building = (Building) entity;
                    if (building.isFierynessDefined()
                            && building.getFieryness() > 0
                            && building.getFieryness() < 4) {
                        removePos.add(civilian.getPosition());
                        removeCivilian.add(civilian);
                    }
                }
            }
        }
        this.rescueTargets.removeAll(removeCivilian);
        targetPos.removeAll(removePos);
        return targetPos;
    }


    private EntityID getWeakerCivilian(EntityID targetPosision) {
        List<Human> civilian = new LinkedList<>();
        for (Human h : this.rescueTargets) {
            if (h.getPosition().equals(targetPosision)) {
                civilian.add(h);
            }
        }
        if (civilian == null || civilian.size() == 0)
            return null;
        Human temp = null;
        if (civilian.size() > 0)
            temp = civilian.get(0);
        if (temp != null) {
            for (Human h : civilian) {
                if (h.isDamageDefined() && h.getDamage() > temp.getDamage()) {
                    temp = h;
                }
            }
            return temp.getID();
        }
        return null;
    }

    //TODO fill it
    private boolean needRescueFromCollapse(Human human) {
        if (human.getPosition() != null) {
            StandardEntity targetPosition = worldInfo.getEntity(human.getPosition());
            if (targetPosition != null
                    && targetPosition instanceof Building
                    && ((human.isHPDefined() && human.getHP() < 600)
                    || (human.isBuriednessDefined() && human.getBuriedness() >= 5))) {
                if (((Building) targetPosition).isOnFire()) {
                    return false;
                }
            }
            if (human.isDamageDefined() && human.getDamage() > 0)
                if (human.isBuriednessDefined() && human.getBuriedness() == 0
                        && targetPosition instanceof Building) {
                    return true;
                }
        }
        if (human.isHPDefined() && human.getHP() > 0) {
            if (human.isBuriednessDefined() && human.getBuriedness() > 0
                    && this.activeTime(human) >= human.getBuriedness()) {
                return true;
            }
        }

        return false;
    }

    //TODO fill it
    private boolean needRescueFromRoad(Human human) {
        if (human.isPositionDefined()
                && worldInfo.getEntity(human.getPosition()) instanceof Road) {
            if (human.isDamageDefined() && human.getDamage() > 0) {
                if (human.isBuriednessDefined() && human.getBuriedness() == 0) {
                    if (agentInfo.getTime() + this.activeTime(human) <= 300
                            ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private boolean civilianDoesNotNeedRescue(Human human) {
        if (!human.isPositionDefined()
                || worldInfo.getEntity(human.getPosition()) instanceof Building) {
            return false;
        }
        if (human.isBuriednessDefined() && human.getBuriedness() == 0) {
            if (agentInfo.getTime() + this.activeTime(human) >= 300
                    ) {
                return true;
            }
        } else if (human.isBuriednessDefined()) {
            if (this.activeTime(human) < human.getBuriedness()) {
                return true;
            }
        }
        return false;
    }

    private boolean changeCivilianView() {
        Set<EntityID> changeSet_Civilian = worldInfo.getChanged().getChangedEntities();

        changeSet_Civilian.removeAll(this.vistedcivilian);
        for (EntityID id : changeSet_Civilian) {
            StandardEntity entity = worldInfo.getEntity(id);
            if (entity != null && entity instanceof Civilian) {
                return true;
            }
        }
        return false;
    }

    private int activeTime(Human human) {
        if (human.isHPDefined() && human.isDamageDefined()) {
            int hp = human.getHP();
            int damage = human.getDamage();
            if (hp == 0) {
                return 0;
            }
            if (human.isBuriednessDefined() && human.getBuriedness() > 0) {
                int damageIdea = 45;
                damage += human.getBuriedness() - damageIdea > 0 ? human.getBuriedness() - damageIdea : damageIdea;
                return (hp / damage) + ((hp % damage) != 0 ? 1 : 0) - 20;
            } else {
                damage += 45;
                return (hp / damage) + ((hp % damage) != 0 ? 1 : 0) - 25;
            }
        }
        return -1;
    }

    @Override
    public EntityID getTarget() {
        return this.result;
    }

    @Override
    public HumanDetector precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }
        this.clustering.precompute(precomputeData);
        return this;
    }

    @Override
    public HumanDetector resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if (this.getCountResume() >= 2) {
            return this;
        }
        this.clustering.resume(precomputeData);
        return this;
    }

    @Override
    public HumanDetector preparate() {
        super.preparate();
        if (this.getCountPreparate() >= 2) {
            return this;
        }
        this.clustering.preparate();
        return this;
    }


    private boolean isInside(double pX, double pY, int[] apex) {
        Point2D p = new Point2D(pX, pY);
        Vector2D v1 = (new Point2D(apex[apex.length - 2], apex[apex.length - 1])).minus(p);
        Vector2D v2 = (new Point2D(apex[0], apex[1])).minus(p);
        double theta = this.getAngle(v1, v2);

        for (int i = 0; i < apex.length - 2; i += 2) {
            v1 = (new Point2D(apex[i], apex[i + 1])).minus(p);
            v2 = (new Point2D(apex[i + 2], apex[i + 3])).minus(p);
            theta += this.getAngle(v1, v2);
        }
        return Math.round(Math.abs((theta / 2) / Math.PI)) >= 1;
    }

    private double getAngle(Vector2D v1, Vector2D v2) {
        double flag = (v1.getX() * v2.getY()) - (v1.getY() * v2.getX());
        double angle = Math.acos(((v1.getX() * v2.getX()) + (v1.getY() * v2.getY())) / (v1.getLength() * v2.getLength()));
        if (flag > 0) {
            return angle;
        }
        if (flag < 0) {
            return -1 * angle;
        }
        return 0.0D;
    }


    private int getDistance(double fromX, double fromY, double toX, double toY) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        return (int) Math.hypot(dx, dy);
    }
    private int getMaxTravelTime(Area area) {
        int distance = 0;
        List<Edge> edges = new ArrayList<>();
        for(Edge edge : area.getEdges()) {
            if(edge.isPassable()) {
                edges.add(edge);
            }
        }
        if(edges.size() <= 1) {
            return Integer.MAX_VALUE;
        }
        for(int i = 0; i < edges.size(); i++) {
            for(int j = 0; j < edges.size(); j++) {
                if(i != j) {
                    Edge edge1 = edges.get(i);
                    double midX1 = (edge1.getStartX() + edge1.getEndX()) / 2;
                    double midY1 = (edge1.getStartY() + edge1.getEndY()) / 2;
                    Edge edge2 = edges.get(j);
                    double midX2 = (edge2.getStartX() + edge2.getEndX()) / 2;
                    double midY2 = (edge2.getStartY() + edge2.getEndY()) / 2;
                    int d = this.getDistance(midX1, midY1, midX2, midY2);
                    if(distance < d) {
                        distance = d;
                    }
                }
            }
        }
        if(distance > 0) {
            return (distance / this.moveDistance) + ((distance % this.moveDistance) > 0 ? 1 : 0) + 1;
        }
        return Integer.MAX_VALUE;
    }


    private void reflectMessage(MessageManager messageManager) {
        Set<EntityID> changedEntities = this.worldInfo.getChanged().getChangedEntities();
        changedEntities.add(this.agentInfo.getID());
        int time = this.agentInfo.getTime();
        for (CommunicationMessage message : messageManager.getReceivedMessageList()) {
            Class<? extends CommunicationMessage> messageClass = message.getClass();
            if (messageClass == MessageCivilian.class) {
                MessageCivilian mc = (MessageCivilian) message;
                if (!changedEntities.contains(mc.getAgentID())) {
                    MessageUtil.reflectMessage(this.worldInfo, mc);
                }
//                this.sentTimeMap.put(mc.getAgentID(), time + this.sendingAvoidTimeReceived);
            }
        }
    }

}

