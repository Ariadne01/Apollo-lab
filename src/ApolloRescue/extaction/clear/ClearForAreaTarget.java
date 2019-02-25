package ApolloRescue.extaction.clear;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.complex.component.RoadInfoComponent;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.Util;
import adf.agent.action.Action;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.component.module.algorithm.PathPlanning;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClearForAreaTarget extends AbstractClearMethod {
//    private double clearDistance ;
//    private GuideLine lastGuideline;
//    private EntityID lastTarget;
//    private RoadInfoComponent roadInfoComponent;
//    private boolean clearBlockadesOnWay = true;
    protected ClearHelper clearHelper;
//    protected ApolloWorld world;
//    protected Area area;
//    protected Area areanext;

    private double clearRange;
    private static final int SECURE_RANGE = /*300*/ 1000;
    private Line2D lastClearLine;
    private boolean wasOnBlockade = false;
    //    private List<EntityID> lastPath;

    public ClearForAreaTarget(ApolloWorld world, WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo, PathPlanning pathPlanning) {
        super(world, worldInfo, agentInfo, scenarioInfo,pathPlanning);

        clearRange = scenarioInfo.getClearRepairDistance();
//        clearDistance = scenarioInfo.getClearRepairDistance();
        clearHelper = new ClearHelper(world, worldInfo, agentInfo, scenarioInfo);
    }

    @Override
    public Action clearWay(List<EntityID> path, EntityID targetID) {
        Line2D clearLine;
        //Debug PF loop when reach to building entrance and no path was returned.
        StandardEntity targetEntity = worldInfo.getEntity(targetID);
        if ((targetEntity instanceof Area) && path.size() <= 1 && !path.contains(targetID)) {
            //                               未生成前进道路 且 当前节点非targetID(路径不含targetID)
            List<EntityID> newPath = pathPlanning.setFrom(getSelfPosition().getID()).setDestination(targetEntity.getID()).calc().getResult();

            if (path.size() < newPath.size()) {
                path = newPath;
            }
        }

        GuideLine guideLine = getTargetGuideline(path, clearRange, targetID);
//        // MrlPersonalData.VIEWER_DATA.setPFGuideline(agentInfo.getID(), guideLine);

        List<Area> areasSeenInPath = clearHelper.getAreasSeenInPath(path);
        if (guideLine != null) {
            Action action = moveToGuideLine(areasSeenInPath, guideLine);
            if (action != null) {
                return action;
            }
//            if (positionActState.equals(PositionActState.KEEP_MOVING)) {
//                return ActResult.SUCCESSFULLY_COMPLETE;
//            }
        }

        //guideLine == null || (guideLine != null && action == null)
        clearLine = clearHelper.getTargetClearLine(areasSeenInPath, clearRange, guideLine);

        Pair<Line2D, Line2D> clearSecureLines = null;
        Point2D agentPosition = Util.getPoint(getSelfLocation());
        if (clearLine != null) {
            Pair<Line2D, Line2D> clearLengthLines = Util.clearLengthLines(Util.clearAreaRectangle(agentPosition, clearLine.getEndPoint(), clearHelper.getClearRadius()), clearHelper.getClearRadius());

            double distance = (clearHelper.getClearRadius() - ApolloConstants.AGENT_SIZE * 0.5) / 2;
            clearSecureLines = clearHelper.getClearSecureLines(clearLengthLines.first(), clearLengthLines.second(), clearHelper.getClearRadius(), distance);
            // MrlPersonalData.VIEWER_DATA.setPFClearAreaLines(agentInfo.getID(), clearLine, clearSecureLines.first(), clearSecureLines.second());
        }

        List<EntityID> thisRoadPath = new ArrayList<EntityID>();
        thisRoadPath.add(getSelfPosition().getID());

        if (clearLine != null &&
                (clearHelper.anyBlockadeIntersection(areasSeenInPath, clearLine, true) || clearHelper.anyBlockadeIntersection(areasSeenInPath, clearSecureLines.first(), true) || clearHelper.anyBlockadeIntersection(areasSeenInPath, clearSecureLines.second(), true))) {
            lastClearLine = clearLine;
            wasOnBlockade = clearHelper.isOnBlockade(clearLine.getEndPoint(), areasSeenInPath);
            return sendClearAct(agentInfo.getTime(), (int) clearLine.getEndPoint().getX(), (int) clearLine.getEndPoint().getY());
        } else {
            boolean clearBlockadesOnWay = false;
            if (wasOnBlockade && lastClearLine != null) {//move to point to the end of lastClearLine if was on the blockade!
                int x = (int) lastClearLine.getEndPoint().getX();
                int y = (int) lastClearLine.getEndPoint().getY();
                lastClearLine = null;
                return moveAction(agentInfo.getTime(), thisRoadPath, x, y);
            } else if (clearBlockadesOnWay && guideLine != null) {
                //looking for blockades on way....
                Point2D nearestIntersect = clearHelper.anyBlockadeIntersection(guideLine);
                if (nearestIntersect != null) {
                    int dist = Util.distance(agentPosition, nearestIntersect);
                    clearLine = new Line2D(agentPosition, nearestIntersect);
                    clearLine = Util.clipLine(clearLine, clearRange - clearHelper.getClearRadius() - SECURE_RANGE);
                    lastClearLine = clearLine;
                    wasOnBlockade = clearHelper.isOnBlockade(clearLine.getEndPoint(), areasSeenInPath);
                    if (dist < clearRange - clearHelper.getClearRadius() - SECURE_RANGE) {
//                        world.printData("I found blockades which intersects with guideline and near me!!!!!");
                        return sendClearAct(agentInfo.getTime(), (int) clearLine.getEndPoint().getX(), (int) clearLine.getEndPoint().getY());
                    } else {
//                        world.printData("Move to point to clear blockades in way....");
                        return moveAction(agentInfo.getTime(), thisRoadPath, (int) nearestIntersect.getX(), (int) nearestIntersect.getY());
                    }
                }
            }

            lastClearLine = null;
        }

        return null;
    }

    @Override
    public Action clearAroundTarget(Pair<Integer, Integer> targetLocation) {
        throw new UnsupportedOperationException();
    }

    private Action moveToGuideLine(List<Area> areasSeenInPath, GuideLine guideLine) {

        int distanceThreshold = clearHelper.getClearRadius() / 3;
        Point2D betterPosition = clearHelper.getBetterPosition(guideLine, distanceThreshold);
        Point2D agentPosition = Util.getPoint(getSelfLocation());
        if (betterPosition == null) {
            return null;
        }


        Line2D line2D = new Line2D(agentPosition, betterPosition);

        Pair<Line2D, Line2D> clearLengthLines = Util.clearLengthLines(Util.clearAreaRectangle(agentPosition, line2D.getEndPoint(), clearHelper.getClearRadius()), clearHelper.getClearRadius());
        double distance = (clearHelper.getClearRadius() - ApolloConstants.AGENT_SIZE * 0.5);
        Pair<Line2D, Line2D> clearSecureLines = clearHelper.getClearSecureLines(clearLengthLines.first(), clearLengthLines.second(), clearHelper.getClearRadius(), distance);
        boolean shouldClear = clearHelper.anyBlockadeIntersection(areasSeenInPath, line2D, false) ||
                clearHelper.anyBlockadeIntersection(areasSeenInPath, clearSecureLines.first(), false) ||
                clearHelper.anyBlockadeIntersection(areasSeenInPath, clearSecureLines.second(), false);

        if (Util.lineLength(line2D) > clearRange) {
           // System.out.println("i have too much distance to guideline......");

            //todo should implement
            return clearToPoint(agentPosition, line2D.getEndPoint());
        }
        if (shouldClear) {
            Line2D clearLine = Util.improveLine(line2D, clearHelper.getClearRadius());
            return sendClearAct(agentInfo.getTime(), (int) clearLine.getEndPoint().getX(), (int) clearLine.getEndPoint().getY());
        }

        if (!clearHelper.isNeedToClear(agentPosition, areasSeenInPath, clearHelper.getClearLine(agentPosition, guideLine, clearRange))) {
            return null;
        }


        List<EntityID> path = new ArrayList<EntityID>(1);
        path.add(getSelfPosition().getID());
        return moveAction(agentInfo.getTime(), path, (int) line2D.getEndPoint().getX(), (int) line2D.getEndPoint().getY());
//        return PositionActState.MOVE_TO_GUIDELINE;
    }

    private Action clearToPoint(Point2D agentPosition, Point2D point) {
        double distance = Util.distance(agentPosition, point);
        Line2D clearLine = null, targetClearLine;

        distance = Math.min(distance, clearRange);
        distance -= SECURE_RANGE;
        clearLine = Util.clipLine(new Line2D(agentPosition, point), distance);
        targetClearLine = Util.clipLine(clearLine, distance - clearHelper.getClearRadius());

        List<Area> areasSeenInPath = new ArrayList<>();

        Area selfPosition = getSelfPosition();
        if (selfPosition.getShape().contains(point.getX(), point.getY())) {
            areasSeenInPath.add(selfPosition);
        } else {
            areasSeenInPath.addAll(getRoadsSeen());
        }
        if (clearHelper.anyBlockadeIntersection(areasSeenInPath, targetClearLine, true)) {
            Point2D endPoint = targetClearLine.getEndPoint();
            return sendClearAct(agentInfo.getTime(), (int) endPoint.getX(), (int) endPoint.getY());
        } else {
            ArrayList<EntityID> path = new ArrayList<>();
            path.add(getSelfPosition().getID());
            return moveAction(agentInfo.getTime(), path, (int) point.getX(), (int) point.getY());
        }

    }

    private GuideLineHelper guideLineHelper =  new GuideLineHelper(world, worldInfo, agentInfo, scenarioInfo, clearRange);

    public GuideLine getTargetGuideline(List<EntityID> path, double range, EntityID targetID) {
        return guideLineHelper.findTargetGuideline(path, range, targetID);
    }

}
