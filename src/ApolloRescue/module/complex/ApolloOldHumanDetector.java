package ApolloRescue.module.complex;

import ApolloRescue.module.complex.firebrigade.BuildingProperty;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.information.*;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.PathPlanning;
import adf.component.module.complex.HumanDetector;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

/*public class ApolloHumanDetector extends HumanDetector {

    private Clustering clustering;
    private PathPlanning pathPlanning;
    private EntityID result;

    private int sendTime;
    private int sendingAvoidTimeClearRequest;

    private Collection<EntityID> agentPositions;
    private Map<EntityID, Integer> sentTimeMap;
    private Map<EntityID, BuildingProperty> sentBuildingMap;
    private int sendingAvoidTimeReceived;
    private int sendingAvoidTimeSent;

    private int moveDistance;
    private EntityID lastPosition;
    private int positionCount;

    public ApolloHumanDetector(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);

        this.result = null;
        this.sendTime = 0;
        this.sendingAvoidTimeClearRequest = developData.getInteger("ApolloHumanDetector.sendingAvoidTimeClearRequest", 5);

        this.agentPositions = new HashSet<>();
        this.sentTimeMap = new HashMap<>();
        this.sentBuildingMap = new HashMap<>();
        this.sendingAvoidTimeReceived = developData.getInteger("ApolloHumanDetector.sendingAvoidTimeReceived", 3);
        this.sendingAvoidTimeSent = developData.getInteger("ApolloHumanDetector.sendingAvoidTimeSent", 5);

        this.moveDistance = developData.getInteger("ApolloHumanDetector.moveDistance", 40000);

        switch (scenarioInfo.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("HumanDetector.PathPlanning", "adf.component.module.algorithm.PathPlanning");
                this.clustering = moduleManager.getModule("HumanDetector.Clustering", "adf.component.module.algorithm.StaticClustering");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("HumanDetector.PathPlanning", "adf.component.module.algorithm.PathPlanning");
                this.clustering = moduleManager.getModule("HumanDetector.Clustering", "adf.component.module.algorithm.StaticClustering");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("HumanDetector.PathPlanning", "adf.component.module.algorithm.PathPlanning");
                this.clustering = moduleManager.getModule("HumanDetector.Clustering", "adf.component.module.algorithm.StaticClustering");
                break;
        }
    }

    @Override
    public HumanDetector updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if (this.getCountUpdateInfo() >= 2)
            return this;
        this.clustering.updateInfo(messageManager);
        this.pathPlanning.updateInfo(messageManager);
        this.reflectMessage(messageManager);
        this.sendEntityInfo(messageManager);

        Set<StandardEntity> atsInBuilding = new HashSet<>();
        worldInfo.getChanged().getChangedEntities().forEach(id -> {
            StandardEntity entity = worldInfo.getEntity(id);
            if (entity instanceof AmbulanceTeam) {
                StandardEntity position = worldInfo.getPosition(entity.getID());
                if (!entity.getID().equals(agentInfo.getID()) && position != null
                        && position instanceof Building
                        && position.getID().equals(agentInfo.getPosition())) {
                    atsInBuilding.add(entity);
                }
            }
        });

        worldInfo.getChanged().getChangedEntities().forEach(id -> {
            StandardEntity entity = worldInfo.getEntity(id);
            if (entity instanceof Building) {
                Building building = (Building) worldInfo.getEntity(id);
                if (building.isFierynessDefined() && building.getFieryness() > 0) {
                    BuildingProperty buildingProperty = sentBuildingMap.get(id);
                    if (buildingProperty == null || buildingProperty.getFieryness() != building.getFieryness()) {
                        messageManager.addMessage(new MessageBuilding(true, building));
                        messageManager.addMessage(new MessageBuilding(false, building));
                        sentBuildingMap.put(id, new BuildingProperty(building));
                    }
                }
            } else if (entity instanceof Civilian) {
                Civilian civilian = (Civilian) entity;
                if ((civilian.isHPDefined() && civilian.getHP() > 1000 && civilian.isDamageDefined() && civilian.getDamage() > 0)
                        || ((civilian.isPositionDefined() && !(worldInfo.getEntity(civilian.getPosition()) instanceof Refuge))
                        && (worldInfo.getEntity(civilian.getPosition()) instanceof Building))) {

                    if (atsInBuilding.size() < 3) {
                        messageManager.addMessage(new MessageCivilian(true, civilian));
                        messageManager.addMessage(new MessageCivilian(false, civilian));
                    }
                }

            }
        });

        return this;
    }

    @Override
    public HumanDetector calc() {
        Human transportHuman = this.agentInfo.someoneOnBoard();
        if (transportHuman != null) {
            this.result = transportHuman.getID();
            return this;
        }
        if (!targetShouldBeAbandoned())
            this.result = null;

        if (this.result == null) {
            if (clustering == null) {
                this.result = this.calcTargetInWorld();
                return this;
            }
            this.result = this.calcTargetInCluster(clustering);
            if (this.result == null) {
                this.result = this.calcTargetInWorld();
                return this;
            }
        }
        return this;
    }

    private boolean targetShouldBeAbandoned() {
        if (this.result != null) {
            Human target = (Human) this.worldInfo.getEntity(this.result);
            if (target != null) {
                if (!target.isHPDefined() || target.getHP() == 0)
                    return true;
                else if (!target.isPositionDefined())
                    return true;
                else if (this.targetDoesNotNeedRescue(target))
                    return true;
                else if (target.getPosition().equals(agentInfo.getPosition())
                        && !agentInfo.getChanged().getChangedEntities().contains(target.getID()))
                    return true;
                else {
                    StandardEntity position = this.worldInfo.getPosition(target);
                    if (position != null) {
                        StandardEntityURN positionURN = position.getStandardURN();
                        if (positionURN == REFUGE || positionURN == AMBULANCE_TEAM) {
                            return true;
                        }
                    }
                }
                if (target.isHPDefined() && target.getHP() < 600
                        || (target.isBuriednessDefined() && target.getBuriedness() >= 10)) {
                    StandardEntity targetPosition = worldInfo.getEntity(target.getPosition());
                    if (targetPosition != null && targetPosition instanceof Building) {
                        if (((Building) targetPosition).isOnFire())
                            return true;
                    }
                }
            }
            if (target instanceof AmbulanceTeam || target instanceof FireBrigade || target instanceof PoliceForce) {
                if (target.isBuriednessDefined() && target.getBuriedness() == 0)
                    return true;
            }
        }
        return false;
    }

    private boolean targetDoesNotNeedRescue(Human human) {
        if (!human.isPositionDefined() || worldInfo.getEntity(human.getPosition()) instanceof Building)
            return false;
        if (human.isBuriednessDefined()) {
            if (human.getBuriedness() == 0 && agentInfo.getTime() + activeTime(human) > 300)
                return true;
            if (human.getBuriedness() > 0 && activeTime(human) < human.getBuriedness())
                return true;
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

    private EntityID calcTargetInCluster(Clustering clustering) {
        int clusterIndex = clustering.getClusterIndex(this.agentInfo.getID());
        Collection<StandardEntity> elements = clustering.getClusterEntities(clusterIndex);
        if (elements == null || elements.isEmpty()) {
            return null;
        }

        List<Human> rescueTargets = new ArrayList<>();
        List<Human> loadTargets = new ArrayList<>();
        for (StandardEntity next : this.worldInfo.getEntitiesOfType(AMBULANCE_TEAM, FIRE_BRIGADE, POLICE_FORCE)) {
            Human h = (Human) next;
            if (this.agentInfo.getID().getValue() == h.getID().getValue()) {
                continue;
            }
            StandardEntity positionEntity = this.worldInfo.getPosition(h);
            if (positionEntity != null && elements.contains(positionEntity) || elements.contains(h)) {
                if (h.isHPDefined() && h.isBuriednessDefined() && h.getHP() > 0 && h.getBuriedness() > 0) {
                    rescueTargets.add(h);
                }
            }
        }
        for (StandardEntity next : this.worldInfo.getEntitiesOfType(CIVILIAN)) {
            Human h = (Human) next;
            StandardEntity positionEntity = this.worldInfo.getPosition(h);
            if (positionEntity != null && positionEntity instanceof Area) {
                if (elements.contains(positionEntity)) {
                    if (h.isHPDefined() && h.getHP() > 0) {
                        if (h.isBuriednessDefined() && h.getBuriedness() > 0) {
                            rescueTargets.add(h);
                        } else {
                            if (h.isDamageDefined() && h.getDamage() > 0 && positionEntity.getStandardURN() != REFUGE) {
                                loadTargets.add(h);
                            }
                        }
                    }
                }
            }
        }
        if (rescueTargets.size() > 0) {
            rescueTargets.sort(new DistanceSorter(this.worldInfo, this.agentInfo.me()));
            return rescueTargets.get(0).getID();
        }
        if (loadTargets.size() > 0) {
            loadTargets.sort(new DistanceSorter(this.worldInfo, this.agentInfo.me()));
            return loadTargets.get(0).getID();
        }
        return null;
    }

    private EntityID calcTargetInWorld() {
        List<Human> rescueTargets = new ArrayList<>();
        List<Human> loadTargets = new ArrayList<>();
        for (StandardEntity next : this.worldInfo.getEntitiesOfType(AMBULANCE_TEAM, FIRE_BRIGADE, POLICE_FORCE)) {
            Human h = (Human) next;
            if (this.agentInfo.getID().getValue() != h.getID().getValue()) {
                StandardEntity positionEntity = this.worldInfo.getPosition(h);
                if (positionEntity != null && h.isHPDefined() && h.isBuriednessDefined()) {
                    if (h.getHP() > 0 && h.getBuriedness() > 0) {
                        rescueTargets.add(h);
                    }
                }
            }
        }
        for (StandardEntity next : this.worldInfo.getEntitiesOfType(CIVILIAN)) {
            Human h = (Human) next;
            StandardEntity positionEntity = this.worldInfo.getPosition(h);
            if (positionEntity != null && positionEntity instanceof Area) {
                if (h.isHPDefined() && h.getHP() > 0) {
                    if (h.isBuriednessDefined() && h.getBuriedness() > 0) {
                        rescueTargets.add(h);
                    } else {
                        if (h.isDamageDefined() && h.getDamage() > 0 && positionEntity.getStandardURN() != REFUGE) {
                            loadTargets.add(h);
                        }
                    }
                }
            }
        }
        if (rescueTargets.size() > 0) {
            rescueTargets.sort(new DistanceSorter(this.worldInfo, this.agentInfo.me()));
            return rescueTargets.get(0).getID();
        }
        if (loadTargets.size() > 0) {
            loadTargets.sort(new DistanceSorter(this.worldInfo, this.agentInfo.me()));
            return loadTargets.get(0).getID();
        }
        return null;
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
                this.sentTimeMap.put(mc.getAgentID(), time + this.sendingAvoidTimeReceived);
            } else if (messageClass == MessageAmbulanceTeam.class) {
                MessageAmbulanceTeam mat = (MessageAmbulanceTeam) message;
                if (!changedEntities.contains(mat.getAgentID())) {
                    MessageUtil.reflectMessage(this.worldInfo, mat);
                }
                this.sentTimeMap.put(mat.getAgentID(), time + this.sendingAvoidTimeReceived);
            } else if (messageClass == MessageFireBrigade.class) {
                MessageFireBrigade mfb = (MessageFireBrigade) message;
                if (!changedEntities.contains(mfb.getAgentID())) {
                    MessageUtil.reflectMessage(this.worldInfo, mfb);
                }
                this.sentTimeMap.put(mfb.getAgentID(), time + this.sendingAvoidTimeReceived);
            } else if (messageClass == MessagePoliceForce.class) {
                MessagePoliceForce mpf = (MessagePoliceForce) message;
                if (!changedEntities.contains(mpf.getAgentID())) {
                    MessageUtil.reflectMessage(this.worldInfo, mpf);
                }
                this.sentTimeMap.put(mpf.getAgentID(), time + this.sendingAvoidTimeReceived);
            }
        }
    }

    private void sendEntityInfo(MessageManager messageManager) {
        if (this.checkSendFlags()) {
            Civilian civilian = null;
            int currentTime = this.agentInfo.getTime();
            Human agent = (Human) this.agentInfo.me();
            for (EntityID id : this.worldInfo.getChanged().getChangedEntities()) {
                StandardEntity entity = Objects.requireNonNull(this.worldInfo.getEntity(id));
                if (entity.getStandardURN() == CIVILIAN) {
                    Integer time = this.sentTimeMap.get(id);
                    if (time != null && time > currentTime) {
                        continue;
                    }
                    Civilian target = (Civilian) entity;
                    if (!this.agentPositions.contains(target.getPosition())) {
                        civilian = this.selectCivilian(civilian, target);
                    } else if (target.getPosition().getValue() == agent.getPosition().getValue()) {
                        civilian = this.selectCivilian(civilian, target);
                    }
                }
            }
            if (civilian != null) {
                messageManager.addMessage(new MessageCivilian(true, civilian));
                this.sentTimeMap.put(civilian.getID(), currentTime + this.sendingAvoidTimeSent);
            }
        }
    }

    private boolean checkSendFlags() {
        boolean isSendCivilianMessage = true;

        StandardEntity me = this.agentInfo.me();
        if (!(me instanceof Human)) {
            return false;
        }
        Human agent = (Human) me;
        EntityID agentID = agent.getID();
        EntityID position = agent.getPosition();
        StandardEntityURN agentURN = agent.getStandardURN();
        EnumSet<StandardEntityURN> agentTypes = EnumSet.of(AMBULANCE_TEAM, FIRE_BRIGADE, POLICE_FORCE);
        agentTypes.remove(agentURN);

        this.agentPositions.clear();
        for (StandardEntity entity : this.worldInfo.getEntitiesOfType(agentURN)) {
            Human other = (Human) entity;
            if (isSendCivilianMessage) {
                if (other.getPosition().getValue() == position.getValue()) {
                    if (other.getID().getValue() > agentID.getValue()) {
                        isSendCivilianMessage = false;
                    }
                }
            }
            this.agentPositions.add(other.getPosition());
        }

        for (StandardEntityURN urn : agentTypes) {
            for (StandardEntity entity : this.worldInfo.getEntitiesOfType(urn)) {
                Human other = (Human) entity;
                if (isSendCivilianMessage) {
                    if (other.getPosition().getValue() == position.getValue()) {
                        if (urn == AMBULANCE_TEAM) {
                            isSendCivilianMessage = false;
                        } else if (agentURN != AMBULANCE_TEAM && other.getID().getValue() > agentID.getValue()) {
                            isSendCivilianMessage = false;
                        }
                    }
                }
                this.agentPositions.add(other.getPosition());
            }
        }
        return isSendCivilianMessage;
    }

    private Civilian selectCivilian(Civilian civilian1, Civilian civilian2) {
        if (this.checkCivilian(civilian1, true)) {
            if (this.checkCivilian(civilian2, true)) {
                if (civilian1.getHP() > civilian2.getHP()) {
                    return civilian1;
                } else if (civilian1.getHP() < civilian2.getHP()) {
                    return civilian2;
                } else {
                    if (civilian1.isBuriednessDefined() && civilian2.isBuriednessDefined()) {
                        if (civilian1.getBuriedness() > 0 && civilian2.getBuriedness() == 0) {
                            return civilian1;
                        } else if (civilian1.getBuriedness() == 0 && civilian2.getBuriedness() > 0) {
                            return civilian2;
                        } else {
                            if (civilian1.getBuriedness() < civilian2.getBuriedness()) {
                                return civilian1;
                            } else if (civilian1.getBuriedness() > civilian2.getBuriedness()) {
                                return civilian2;
                            }
                        }
                    }
                    if (civilian1.isDamageDefined() && civilian2.isDamageDefined()) {
                        if (civilian1.getDamage() < civilian2.getDamage()) {
                            return civilian1;
                        } else if (civilian1.getDamage() > civilian2.getDamage()) {
                            return civilian2;
                        }
                    }
                }
            }
            return civilian1;
        }
        if (this.checkCivilian(civilian2, true)) {
            return civilian2;
        } else if (this.checkCivilian(civilian1, false)) {
            return civilian1;
        } else if (this.checkCivilian(civilian2, false)) {
            return civilian2;
        }
        return null;
    }

    private boolean checkCivilian(Civilian c, boolean checkOtherValues) {
        if (c != null && c.isHPDefined() && c.isPositionDefined()) {
            return !checkOtherValues || c.isDamageDefined() || c.isBuriednessDefined();
        }
        return false;
    }


    private class DistanceSorter implements Comparator<StandardEntity> {
        private StandardEntity reference;
        private WorldInfo worldInfo;

        DistanceSorter(WorldInfo wi, StandardEntity reference) {
            this.reference = reference;
            this.worldInfo = wi;
        }

        public int compare(StandardEntity a, StandardEntity b) {
            int d1 = this.worldInfo.getDistance(this.reference, a);
            int d2 = this.worldInfo.getDistance(this.reference, b);
            return d1 - d2;
        }
    }

    private int getMaxTravelTime(Area area) {
        int distance = 0;
        List<Edge> edges = new ArrayList<>();
        for (Edge edge : area.getEdges()) {
            if (edge.isPassable()) {
                edges.add(edge);
            }
        }
        if (edges.size() <= 1) {
            return Integer.MAX_VALUE;
        }
        for (int i = 0; i < edges.size(); i++) {
            for (int j = 0; j < edges.size(); j++) {
                if (i != j) {
                    Edge edge1 = edges.get(i);
                    double midX1 = (edge1.getStartX() + edge1.getEndX()) / 2;
                    double midY1 = (edge1.getStartY() + edge1.getEndY()) / 2;
                    Edge edge2 = edges.get(j);
                    double midX2 = (edge2.getStartX() + edge2.getEndX()) / 2;
                    double midY2 = (edge2.getStartY() + edge2.getEndY()) / 2;
                    int d = this.getDistance(midX1, midY1, midX2, midY2);
                    if (distance < d) {
                        distance = d;
                    }
                }
            }
        }
        if (distance > 0) {
            return (distance / this.moveDistance) + ((distance % this.moveDistance) > 0 ? 1 : 0) + 1;
        }
        return Integer.MAX_VALUE;
    }

    private int getDistance(double fromX, double fromY, double toX, double toY) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        return (int) Math.hypot(dx, dy);
    }
}*/

