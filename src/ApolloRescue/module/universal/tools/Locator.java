package ApolloRescue.module.universal.tools;

import java.awt.Point;
import java.awt.geom.Line2D;

import ApolloRescue.module.universal.ApolloWorld;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

public class Locator {
    protected ApolloWorld world;
    public static Point getCenter(Edge edge){
        Point p;
        int x, y;

        x = (edge.getStartX() + edge.getEndX()) / 2;
        y = (edge.getStartY() + edge.getEndY()) / 2;
        p = new Point(x, y);
        return p;
    }


    public static Point getCenter(Area area){
        Point p;
        int x, y;

        x = area.getX();
        y = area.getY();
        p = new Point(x, y);
        return p;
    }

    public static Line2D getLine(Edge edge){
        Point p1, p2;
        int x, y;
        Line2D line;

        x = edge.getStartX();
        y = edge.getStartY();
        p1 = new Point(x, y);
        x = edge.getEndX();
        y = edge.getEndY();
        p2 = new Point(x, y);
        line = new Line2D.Double(p1, p2);
        return line;
    }

    public static Point getExtensionPoint(Point reference, Point target, int extension){
        int d, x, y, dx, dy;
        double r;
        Point p;

        d = Ruler.getDistance(reference, target);
        r = (d+extension)/(double)d;
        dx = target.x - reference.x;
        dy = target.y - reference.y;
        x = (int) (reference.x + r * dx);
        y = (int) (reference.y + r * dy);
        p = new Point(x, y);
        return p;
    }

    //根据已给实体entity获得坐标
    public static Point getPosition(StandardEntity entity, ApolloWorld  world) {
        Point p;
        Pair<Integer, Integer> pair;

        pair = world.getWorldInfo().getLocation(entity); //返回coordinates
        if (pair == null) {
            return null;
        } else {
            p = new Point(pair.first(), pair.second());
            return p;
        }
    }

    //根据EntityID id获得实体entity，再用上一方法根据实体entity获得坐标
    public static Point getPosition(EntityID id, ApolloWorld model) {
        Point p;
        StandardEntity entity;

        entity = model.getEntity(id);
        p = getPosition(entity, model);
        return p;
    }

    /*public static Rectangle getBounds(Human human, ApolloWorld model) {
        Point p;
        double s;
        Rectangle2D rect;

        s = ApolloConstants.AGENT_RADIUS * 2;
        p = getPosition(human, model);
        rect = new Rectangle2D.Double(p.x-s, p.y-s, s*2, s*2);

        return rect.getBounds();
    }*/
}
