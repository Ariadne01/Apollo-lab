package ApolloRescue.extaction.move;

import ApolloRescue.module.complex.component.Info.LineInfo;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.tools.RayModel;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;

import java.util.*;

public class LineOfSightForMovePerception {

    private int rayCount;
    private int rayLength;
    private int errorThreshold;

    private WorldInfo worldInfo;
    private AgentInfo agentInfo;
    private ScenarioInfo scenarioInfo;

    /**
     * Create a LineOfSightPerception object.
     */
    public LineOfSightForMovePerception(WorldInfo worldInfo, ScenarioInfo scenarioInfo, AgentInfo agentInfo) {
        this.worldInfo = worldInfo;
//        ScenarioInfo scenarioInfo1 = scenarioInfo;
        this.scenarioInfo = scenarioInfo;
        this.agentInfo = agentInfo;
        this.rayCount = 36;
        this.rayLength = scenarioInfo.getPerceptionLosMaxDistance();
        this.errorThreshold = 500;//(ApolloConstants.AGENT_SIZE/2);
    }

    //obstacles => blockade & Road & buildings
    public Map<Line2D, List<Point2D>> findEscapePoints(java.awt.Polygon scaledConvexHull, List<StandardEntity> obstacles, Point2D targetPoint) {
        Pair<Integer, Integer> location = worldInfo.getLocation(agentInfo.getID());
        Map<Line2D, List<Point2D>> targetRaysHitPoint = new HashMap<>();
        if (location != null) {
            Point2D locationpPoint = new Point2D(location.first(), location.second());
            Set<RayModel> freeRays = findRaysNotHit(locationpPoint, obstacles); //与障碍物不相交的视线
            targetRaysHitPoint = raysFromTargetPoint(targetPoint, freeRays, obstacles);
        }

        return targetRaysHitPoint;
    }

    public Map<Line2D, List<Point2D>> findEscapePoints(List<StandardEntity> obstacles, Point2D targetPoint) {
        Pair<Integer, Integer> location = worldInfo.getLocation(agentInfo.getID());
        Map<Line2D, List<Point2D>> targetRaysHitPoint = new HashMap<>(); 
        if (location != null) {
            Point2D locationpPoint = new Point2D(location.first(), location.second());
            Set<RayModel> freeRays = findRaysNotHit(locationpPoint, obstacles);
            targetRaysHitPoint = raysFromTargetPoint(targetPoint, freeRays, obstacles);
        }

        return targetRaysHitPoint;
    }


    private Set<RayModel> findRaysNotHit(Point2D location, Collection<StandardEntity> obstacles) {
        Set<LineInfo> obstaclesLines = Util.getAllLines(obstacles);
        // Cast rays
        // CHECKSTYLE:OFF:MagicNumber
        double dAngle = Math.PI * 2 / rayCount; // 单位角度
        // CHECKSTYLE:ON:MagicNumber
        Set<RayModel> result = new HashSet<>();
        for (int i = 0; i < rayCount; ++i) {
            double angle = i * dAngle; // 每条射线的角度
            Vector2D vector = new Vector2D(Math.sin(angle), Math.cos(angle)).scale(rayLength);
            Point2D distanceLocation = new Point2D(
                    location.getX() + errorThreshold * Math.sin(angle),
                    location.getY() + errorThreshold * Math.cos(angle)
            );
            RayModel RayModel = new RayModel(new Line2D(distanceLocation, vector), obstaclesLines);
            if (RayModel.getLinesHit().isEmpty()) { //若当前射线RayModel与obstaclesLines中的线段不相交
                result.add(RayModel);
            }
        }
        return result;
    }

    /**
     *
     * @param location 目标点
     * @param freeRays 与障碍物不相交的视线
     * @param obstacles 障碍物集合
     * @return 与障碍物不相交的，指向坐标点location的目标视线 & 目标视线上的点集
     */
    private Map<Line2D, List<Point2D>> raysFromTargetPoint(Point2D location, Collection<RayModel> freeRays, Collection<StandardEntity> obstacles) {
        Set<LineInfo> lines = new HashSet<>(Util.getAllLines(obstacles));
        for (RayModel rayModel : freeRays) {
            lines.add(new LineInfo(rayModel.getRayLine(), null, false));
        }
        // Cast rays
        // CHECKSTYLE:OFF:MagicNumber
        double dAngle = Math.PI * 2 / rayCount;
        // CHECKSTYLE:ON:MagicNumber
        Map<Line2D, List<Point2D>> targetRaysHitPoint = new HashMap<>();
        for (int i = 0; i < rayCount; ++i) {
            double angle = i * dAngle;
            Vector2D vector = new Vector2D(Math.sin(angle), Math.cos(angle)).scale(rayLength);
            RayModel rayModel = new RayModel(new Line2D(new Point2D((int) location.getX(), (int) location.getY()), vector), lines);

            List<LineInfo> linesHit = rayModel.getLinesHit();   //从目标点发出的视线与其他线(障碍物线 & freeRays)相交的线
            if (!linesHit.isEmpty()) {
                List<Point2D> intersections = new ArrayList<>();
                for (LineInfo lineInfo : linesHit) {
                    if (lineInfo.isBlocking()) {
                        continue;
                    }
                    Point2D intersection = Util.getIntersection(lineInfo.getLine(), rayModel.getRayLine());
                    if (Util.contains(lineInfo.getLine(), intersection, 10)) {
                        intersections.add(intersection);
                    }
                }
                targetRaysHitPoint.put(rayModel.getRayLine(), intersections);
            }
        }
        return targetRaysHitPoint;
    }

    public void setRayCount(int rayCount) {
        this.rayCount = rayCount;
    }


}
