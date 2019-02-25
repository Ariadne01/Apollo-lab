package ApolloRescue.module.universal;

import adf.agent.info.AgentInfo;
import adf.agent.info.WorldInfo;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.geom.Area;
import java.util.List;

public class Utils_AT {
    public static Point2D getAgentPosition(AgentInfo agentInfo) {
        return new Point2D(agentInfo.getX(), agentInfo.getY());
    }

    public static boolean isPathEqual(List<EntityID> path1, List<EntityID> path2) {
        if (path1 == null || path2 == null)
            return false;

        if (path1.size() != path2.size())
            return false;

        for (int i = 0; i < path1.size(); i++) {
            EntityID id1 = path1.get(i);
            EntityID id2 = path2.get(i);
            if (!id1.equals(id2))
                return false;
        }

        return true;
    }

    public static boolean isEqual(Point2D point1, Point2D point2, double thresh) {
        double dist = GeometryTools2D.getDistance(point1, point2);
        if (dist < thresh) {
            return true;
        }
        return false;
    }

    public static List<Line2D> getAllBlockadeLines(Blockade b, WorldInfo wi) {
        return GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(b.getApexes()), true);
    }
    public static rescuecore2.standard.entities.Area getArea(WorldInfo wi, EntityID id) {
        StandardEntity se = wi.getEntity(id);
        if (se instanceof rescuecore2.standard.entities.Area) {
            return (rescuecore2.standard.entities.Area) se;
        }
        return null;
    }

    public static boolean isStuckInBlockade(int x, int y, EntityID areaID, WorldInfo wi) {
        int stuckThresh = 5;
        rescuecore2.standard.entities.Area area = getArea(wi, areaID);
        if (area != null) {
            if (area.isBlockadesDefined()) {
                List<EntityID> blockadeIDs = area.getBlockades();
                for (EntityID blockadeID : blockadeIDs) {
                    Blockade blockade = (Blockade) wi.getEntity(blockadeID);
                    if (new Area(blockade.getShape()).contains(x, y)) {
                        return true;
                    } else {
                        List<Line2D> bLines = getAllBlockadeLines(blockade ,wi);
                        Point2D humanPoint = new Point2D(x, y);
                        for (Line2D line : bLines) {
                            Point2D closest = GeometryTools2D.getClosestPointOnSegment(line, humanPoint);
                            if (closest != null) {
                                double dist = GeometryTools2D.getDistance(closest, humanPoint);
                                if (dist < stuckThresh) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean isInsideApex(double pX, double pY, int[] apex) {
        Point2D p = new Point2D(pX, pY);
        Vector2D v1 = (new Point2D(apex[apex.length - 2], apex[apex.length - 1])).minus(p);
        Vector2D v2 = (new Point2D(apex[0], apex[1])).minus(p);
        double theta = getAngle(v1, v2);

        for(int i = 0; i < apex.length - 2; i += 2) {
            v1 = (new Point2D(apex[i], apex[i + 1])).minus(p);
            v2 = (new Point2D(apex[i + 2], apex[i + 3])).minus(p);
            theta += getAngle(v1, v2);
        }
        return Math.round(Math.abs((theta / 2) / Math.PI)) >= 1;
    }

    private static double getAngle(Vector2D v1, Vector2D v2) {
        double flag = (v1.getX() * v2.getY()) - (v1.getY() * v2.getX());
        double angle = Math.acos(((v1.getX() * v2.getX()) + (v1.getY() * v2.getY())) / (v1.getLength() * v2.getLength()));
        if(flag > 0) {
            return angle;
        }
        if(flag < 0) {
            return -1 * angle;
        }
        return 0.0D;
    }

}
