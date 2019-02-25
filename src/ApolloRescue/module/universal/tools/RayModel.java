package ApolloRescue.module.universal.tools;

import java.io.Serializable;
import java.util.*;

import ApolloRescue.module.complex.component.Info.LineInfo;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;


public class RayModel implements Serializable {
    static final long serialVersionUID = -198713768239652370L;
    private  PairSerialized< PairSerialized<Double,Double>, PairSerialized<Double,Double>> ray = new  PairSerialized<>();

    public RayModel(Line2D ray) {
        this.ray = new  PairSerialized< PairSerialized<Double, Double>,  PairSerialized<Double, Double>>(new  PairSerialized<Double, Double>(ray.getOrigin().getX(), ray.getOrigin().getY()), new  PairSerialized<Double, Double>(ray.getEndPoint().getX(), ray.getEndPoint().getY()));
    }

    public Line2D getRay() {
        return new Line2D(ray.first().first(),ray.first().second(),ray.second().first(),ray.second().second());
    }

    public void setRay(Line2D ray) {
        this.ray = new  PairSerialized< PairSerialized<Double, Double>,  PairSerialized<Double, Double>>(new  PairSerialized<Double, Double>(ray.getOrigin().getX(), ray.getOrigin().getY()), new PairSerialized<Double, Double>(ray.getEndPoint().getX(), ray.getEndPoint().getY()));
    }

    ///////////////////////////////////////////////////////
    /**
     * The rayLine itself.
     */
    private Line2D rayLine = null;
    /**
     * The visible length of the rayLine.
     */
    private double length = 0.0D;
    /**
     * List of lines hit in order.
     */
    private List<LineInfo> hit = new ArrayList<>();


    public RayModel(Line2D rayLine, Collection<LineInfo> otherLines) {

        this.rayLine = rayLine;
        List<Pair<LineInfo, Double>> intersections = new ArrayList<Pair<LineInfo, Double>>();
        // Find intersections with other lines
        for (LineInfo other : otherLines) {
            double d1 = rayLine.getIntersection(other.getLine());
            double d2 = other.getLine().getIntersection(rayLine);
            if (d2 >= 0 && d2 <= 1 && d1 > 0 && d1 <= 1) {  //d ∈ [0,1],说明已知线段rayLine与otherLines中遍历的当前线段相交。
                intersections.add(new Pair<LineInfo, Double>(other, d1));
            }
        }
        IntersectionSorter intersectionSorter = new  IntersectionSorter();

        Collections.sort(intersections, new  IntersectionSorter());
        hit = new ArrayList<LineInfo>();    //相交线集合
        length = 1;
        for (Pair<LineInfo, Double> next : intersections) {
            LineInfo l = next.first();
            hit.add(l);
            if (l.isBlocking()) {
                length = next.second();
                break;  //获得length(可见长度)后，直接退出循环。
            }
        }
    }

    public Line2D getRayLine() {
        return rayLine;
    }

    public double getVisibleLength() {
        return length;
    }

    public List<LineInfo> getLinesHit() {
        return Collections.unmodifiableList(hit);
    }


}

class IntersectionSorter implements Comparator<Pair<LineInfo, Double>>, java.io.Serializable {
    @Override
    public int compare(Pair<LineInfo, Double> a, Pair<LineInfo, Double> b) {
        double d1 = a.second();
        double d2 = b.second();
        if (d1 < d2) {
            return -1;
        }
        if (d1 > d2) {
            return 1;
        }
        return 0;
    }
}