package ApolloRescue.module.universal.newsearch.node;

import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.tools.Ruler;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class Node {

    private int id;
    private Point position;
    private Area area;
    private java.util.List<Node> connectedNodes; // ???????
    public boolean isConnectedToEnd; // ?????????

    private double nodeWidth;
    protected double passRate = 1; // ***?????????????

    protected java.util.List<EntityID> roadBlockades;// ***Road????????

    private Edge longEdge;
    private Edge shortEdge;
    private boolean flag = false;

    public Node(Area area, ApolloWorld model) {

        roadBlockades = new ArrayList<EntityID>();

        // ?????????

        this.area = area;
        int x = area.getX();
        int y = area.getY();
        Point center = new Point(x, y);
        initialize(center);

        // ???Area????????????

        if (findTwoImpassableEdges(area, model) != null) {
            longEdge = findTwoImpassableEdges(area, model).get(0);
            shortEdge = findTwoImpassableEdges(area, model).get(1);

            // ?????????
            nodeWidth = Ruler.getDistance(longEdge, shortEdge);
            if (nodeWidth == 0) {
                nodeWidth = Ruler.getDistance(longEdge.getStart(),
                        longEdge.getEnd())
                        * Math.sin(Math.PI / 3);
            }
        }

        // ????????
        java.util.List<EntityID> blockades;
        blockades = area.getBlockades();
        if (blockades != null && (!blockades.isEmpty())) {
            this.roadBlockades.addAll(blockades);
        }

    }

    public Node(Node node) {
        this.id = node.getId();
        this.position = node.getPosition();
        this.area = node.getArea();
        this.connectedNodes = node.getConnectNodes();

        this.passRate = node.getPassRate();

        this.roadBlockades = node.getBlockades();
        isConnectedToEnd = false;
    }

    public Node(Rectangle rectangle) {
        initialize(rectangle);
    }

    private void initialize(Rectangle rectangle) {
        Point center;
        center = new Point((int) rectangle.getCenterX(),
                (int) rectangle.getCenterY());
        initialize(center);
    }

    private void initialize(Point point) {
        position = point;
        connectedNodes = new ArrayList<Node>();
        isConnectedToEnd = false;
    }

    public Node(int x, int y) {
        Point center;

        center = new Point(x, y);
        initialize(center);
    }

    // ***???passRate
    public double getPassRate() {
        return this.passRate;
    }

    // ***??????·? ?????????
    public void setPassRate(double passRate) {
        this.passRate = passRate;
    }

    // public void setPassRate(ApolloWorld model){
    // int n,i=0;
    // double blockadesWidth[]=null;
    // double rate=1,m=0,h=0,blockadeWidth,blockadeLength,width=0,roadLength;
    //
    // if(roadBlockades != null&&(!roadBlockades.isEmpty())) {
    // n=roadBlockades.size();
    // blockadesWidth=new double[n];
    //
    // for(EntityID blockadeID : roadBlockades){
    // Blockade blockade;
    // blockade=(Blockade)model.getEntity(blockadeID);
    // blockadeWidth=blockade.getShape().getBounds().getWidth();
    // blockadeLength=blockade.getShape().getBounds().getHeight();
    //
    // // if(blockadeLength>blockadeWidth&&blockadeLength<=nodeWidth){
    // // width=blockadeLength;
    // // }else if(blockadeLength>blockadeWidth&&blockadeLength>nodeWidth){
    // // width=(blockadeWidth+blockadeLength)/2;
    // // }else if(blockadeLength<blockadeWidth&&blockadeWidth<=nodeWidth){
    // // width=blockadeWidth;
    // // }else if(blockadeLength<blockadeWidth&&blockadeWidth>nodeWidth){
    // // width=(blockadeWidth+blockadeLength)/2;
    // // }
    //
    // if(blockadeLength>nodeWidth){
    // width=blockadeLength;
    // }else if(blockadeWidth>nodeWidth){
    // width=blockadeLength;
    // }else{
    // width=(blockadeWidth+blockadeLength)/2;
    // }
    //
    // blockadesWidth[i]=width;
    // i++;
    // if(width+1000>nodeWidth){
    // rate=0.1;
    // passRate= rate;
    // return ;
    // }
    // }
    // for(int j=0;j<n-1;j++)
    // for(int t=j+1;t<n;t++){m++;
    // if(blockadesWidth[j]+blockadesWidth[t]+1000<nodeWidth)h++;
    // }
    // rate=(1+h*0.75)/(m+1);
    //
    // passRate=rate;
    // System.out.println("passRate "+passRate);
    // }
    // passRate=0.8;
    //
    // }

    // public void setPassRate(ApolloWorld model) {
    //
    // if (longEdge == null && shortEdge == null) {
    // passRate = 0;
    // return;
    // }
    //
    // if (!roadBlockades.isEmpty()) {
    // // System.out.println("blockade "+roadBlockades);
    // double max1 = 0, max2 = 0, min1 = 10 ^ 8, min2 = 10 ^ 8, length1,
    // length2, Max1, Max2, Min1, Min2;
    //
    // for (EntityID blockadeID : roadBlockades) {
    //
    // Max1 = max1;
    // Max2 = max2;
    // Min1 = min1;
    // Min2 = min2;
    // int longFlage = 1;
    // int shortFlage = 1;
    //
    // Blockade blockade = (Blockade) (model.getEntity(blockadeID));
    // double width = Math.max(blockade.getShape().getBounds()
    // .getHeight(), blockade.getShape().getBounds()
    // .getWidth());
    //
    // for (int i = 0; i < blockade.getApexes().length - 1; i += 2) {
    // int j = i + 1;
    // Point point = new Point(blockade.getApexes()[i],
    // blockade.getApexes()[j]);
    //
    // length1 = Ruler.getDistance(point, longEdge); // ??????????
    // length2 = Ruler.getDistance(point, shortEdge); // ?????????
    //
    // if (length1 == 0)
    // longFlage = 0;
    // if (length2 == 0)
    // shortFlage = 0;
    //
    // if (shortFlage != 0) {
    // if (longFlage == 0) {
    // min2 = Min2;
    // max2 = Max2;
    // }
    // if (min1 > length1)
    // min1 = length1;
    // if (max1 < length1)
    // max1 = length1;
    // }
    //
    // if (longFlage != 0) {
    // if (shortFlage == 0) {
    // min1 = Min1;
    // max1 = Max1;
    // }
    // if (min2 > length2)
    // min2 = length2;
    // if (max2 < length2)
    // max2 = length2;
    //
    // }
    // if (longFlage == 0 && shortFlage == 0) {
    // if (width + 2500 > nodeWidth) {
    // passRate = 0.1;
    // return;
    // }
    // passRate = 0.3;
    // return;
    // }
    // }
    //
    // }
    // if (max1 == 0)
    // min1 = 0;
    // if (max2 == 0)
    // min2 = 0;
    // if ((min1 == 0 && min2 == 0 && ((max1 + 1500 > nodeWidth)) || (max2 +
    // 1500 > nodeWidth))) {
    // passRate = 0.1;
    // } else if (min1 == 0 && min2 == 0) {
    //
    // if (max1 + max2 + 2500 > nodeWidth) {
    // passRate = 0.25;
    // } else {
    // passRate = 1 - (max1 + max2) / nodeWidth - 0.08;
    // }
    // } else {
    // if (min1 == 0 && max1 != 0) {
    // passRate = 1 - max1 / nodeWidth - 0.05;
    // } else if (min2 == 0 && max2 != 0) {
    // passRate = 1 - max2 / nodeWidth - 0.05;
    // } else {
    // passRate = 1 - (max1 - min1 + max2 - min2)
    // / (2 * nodeWidth) - 0.08;
    // }
    // }
    // // System.out.println("passRate *** "+passRate);
    // } else {
    // passRate = 1;
    // }
    // }

	/*
	 * public void setPassRate(ApolloWorld model){ //???? double
	 * blocladeArea=0,roadArea; roadArea=nodeLength*nodeWidth; if(roadBlockades
	 * != null&&(!roadBlockades.isEmpty())) { for(EntityID blockadeID :
	 * roadBlockades){ Blockade blockade; double blockWidth,blockLength;
	 * blockade=(Blockade)model.getEntity(blockadeID);
	 * blockWidth=blockade.getShape().getBounds().getWidth();
	 * blockLength=blockade.getShape().getBounds().getHeight();
	 * if(blockWidth+1000>nodeWidth){ passRate=0.1; return; }
	 * blocladeArea+=blockWidth*blockLength; } if(roadArea<blocladeArea)
	 * passRate=0.1; else passRate=1-roadArea/blocladeArea+0.1;
	 *
	 * } }
	 */

    public void setPassRate(ApolloWorld model) {

        if (longEdge == null || shortEdge == null) {
            passRate = 0.1;
            return;
        }

        if (flag) {
            passRate = 0.9;
            return;
        }

        if (!roadBlockades.isEmpty()) {

            java.util.List<EntityID> closestLongEdgeBlockades = getBlockadesClosestTolongEdge(model);
            java.util.List<EntityID> closestShortEdgeBlockades = getBlockadesClosestToshortEdge(closestLongEdgeBlockades);
            int blockWidth = 0;

            for (EntityID blockadeID : roadBlockades) {
                if (blockadeID == null) {
                    continue;
                }

                Blockade blockade = (Blockade) (model.getEntity(blockadeID));
                Edge closestEdge = getClosestEdge(blockade);
                EntityID oppositeBlockID = null;
                if (closestEdge.equals(longEdge)) {
                    oppositeBlockID = getOppsiteBlockade(blockade,
                            closestShortEdgeBlockades, model);
                } else {
                    oppositeBlockID = getOppsiteBlockade(blockade,
                            closestLongEdgeBlockades, model);
                }

                Blockade oppositeBlockade = null;
                if (oppositeBlockID != null) {
                    oppositeBlockade = (Blockade) (model
                            .getEntity(oppositeBlockID));
                }

                int width = getBlockadeWidth(blockade, oppositeBlockade);
                // if(area.getID().getValue()==32787){
                // System.out.println("blockadeWidth "+getBlockadeWidth(blockade)+"  blockadeWidth_2 "+getBlockadeWidth(oppositeBlockade));
                // }

                if (width + 1000 > nodeWidth) {
                    passRate = 0.1;
                    return;
                }
                if (width > blockWidth) {
                    blockWidth = width;
                }
            }

            if (blockWidth + 1000 > nodeWidth) {
                passRate = 0.1;
            } else {
                passRate = 1 - blockWidth / nodeWidth + 0.05;
            }
        } else {
            passRate = 1;
        }

        // System.out.println("o(?ɡ?)o...????  ??????? " + passRate);
    }

    // ??????
    public java.util.List<EntityID> getBlockades() {
        return this.roadBlockades;
    }

    // ???·??
    public void clearBlockades(EntityID blockadeID) {
        if (blockadeID != null) {
            if (!roadBlockades.isEmpty())
                if (roadBlockades.contains(blockadeID)) {
                    roadBlockades.remove(blockadeID);
                }
        }
    }

    // ???????·??
    public void addBlockade(EntityID blockadeID) {
        if (blockadeID != null)
            if (!roadBlockades.contains(blockadeID))
                roadBlockades.add(blockadeID);
    }

    // ????Area??????·??
    public void addBlockades(java.util.List<EntityID> blockadeIDs) {
        if (blockadeIDs != null && (!blockadeIDs.isEmpty())) {
            roadBlockades.clear();
            for (EntityID blockade : blockadeIDs) {
                if (!roadBlockades.contains(blockade)) {
                    roadBlockades.add(blockade);
                }
            }
        } else {
            roadBlockades.clear();
        }
    }

    public int getX() {
        return position.x;
    }

    public int getY() {
        return position.y;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public Area getArea() {
        return this.area;
    }

    // Set Node ID
    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Point getPosition() {
        return position;
    }

    public void addConnectedNode(Node node) {

        connectedNodes.add(node);
    }

    public java.util.List<Node> getConnectNodes() {
        return connectedNodes;
    }

    public java.util.List<Edge> findTwoImpassableEdges(Area area, ApolloWorld model) {

        java.util.List<Edge> impassableEdges = new ArrayList<Edge>();
        java.util.List<Edge> edges = area.getEdges();

        if (area != null && !edges.isEmpty()) {
            for (Edge edge : edges) {
                if (edge == null) {
                    continue;
                }
                if (!isPassableEdge(edge, model)) {
                    impassableEdges.add(edge);
                }
            }

            if (impassableEdges.size() == 2) {
                return impassableEdges;

            } else if (impassableEdges.size() == 0) { // ???·??

                if (edges.size() == 3) { // ?????
                    impassableEdges.add(edges.get(0));
                    impassableEdges.add(edges.get(1));
                } else {
                    java.util.List<Edge> temp = new ArrayList<Edge>();
                    temp = getTwoLongestEdge(edges);
                    if (temp != null) {
                        impassableEdges.addAll(temp);
                    }
                }

            } else if (impassableEdges.size() == 1) { // ????·??
                if (edges.size() == 3) { // ?????
                    for (Edge edge : edges) {
                        if (!impassableEdges.contains(edge)) {
                            impassableEdges.add(edge);
                            break;
                        }
                    }
                } else {

                    impassableEdges.add(getOppositeEdge_new(
                            impassableEdges.get(0), edges));
                }
            } else { // ??????????ж??

                java.util.List<Edge> twoEdge = new ArrayList<Edge>();
                twoEdge.add(impassableEdges.get(0));
                Edge secondEdge = getOppositeEdge_new(impassableEdges.get(0),
                        impassableEdges);
                twoEdge.add(secondEdge);
                return twoEdge;

                // if(passableEdges.size()<2){
                // flag=true;
                // return null;
                // }else{

                // List<Edge> temp = new ArrayList<Edge>();
                // temp = getTwoOppositeEdge(impassableEdges,passableEdges);
                // impassableEdges.clear();
                // impassableEdges.addAll(temp);
                // }

                // if(passableEdges.size()<2){
                // flag=true;
                // return null;
                // }
                //
                // if(passableEdges.size()==2 && ){
                //
                // }
                // Edge tempEdge=impassableEdges.get(0);
                // Edge firstEdge=new
                // Edge(tempEdge.getStart(),tempEdge.getEnd());
                //
                // impassableEdges.clear();
                // impassableEdges.add(firstEdge);
                // Edge oppositeEdge=getOppositeEdge(firstEdge, edges,
                // passableEdges);
                // impassableEdges.add(oppositeEdge);
            }
            return impassableEdges;
        }
        return null;

    }

    public boolean isPassableEdge(Edge edge, ApolloWorld world) {
        Point2D start = edge.getStart();
        Point2D end = edge.getEnd();
        StandardEntity neighbor = world.getEntity(edge.getNeighbour());

        if (neighbor != null && neighbor instanceof Area) {

            if (((Area) neighbor).getEdges() != null) {
                java.util.List<Point2D> points = new ArrayList<Point2D>();
                java.util.List<Edge> neighborEdges = ((Area) neighbor).getEdges();
                for (Edge neighborEdge : neighborEdges) {
                    if (neighborEdge != null) {
                        if (!points.contains(neighborEdge.getStart())) {
                            points.add(neighborEdge.getStart());
                        }
                        if (!points.contains(neighborEdge.getEnd()))
                            points.add(neighborEdge.getEnd());
                    }
                }

                if (points.contains(start) && points.contains(end)) {
                    return true;
                }
            }
        }
        return false;
    }

    public java.util.List<Edge> getTwoLongestEdge(java.util.List<Edge> edges) { // ???·???????????????
        if (!edges.isEmpty()) {
            java.util.List<Edge> theTwoLongestEdge = new ArrayList<Edge>();
            Edge firstEdge = null;
            int max = 0;
            for (Edge edge : edges) {
                if (edge != null) {
                }
                int length = Ruler.getDistance(edge.getStart(), edge.getEnd());
                if (length > max) {
                    max = length;
                    firstEdge = edge;
                }
            }
            theTwoLongestEdge.add(firstEdge);
            Edge secondEdge = getOppositeEdge_new(firstEdge, edges);
            theTwoLongestEdge.add(secondEdge);
            return theTwoLongestEdge;
        }
        return null;
    }

    public java.util.List<Edge> getTwoOppositeEdge(java.util.List<Edge> impassableEdges,
                                                   java.util.List<Edge> passableEdges) { // ?ж???????????
        if (!impassableEdges.isEmpty()) {
            java.util.List<Edge> theTwoLongestEdge = new ArrayList<Edge>();
            Edge firstEdge = null;
            int max = 0;
            for (Edge edge : impassableEdges) {
                if (edge != null) {
                }
                int length = Ruler.getDistance(edge.getStart(), edge.getEnd());
                if (length > max) {
                    max = length;
                    firstEdge = edge;
                }
            }
            theTwoLongestEdge.add(firstEdge);
            Edge secondEdge = getOppositeEdge(firstEdge, impassableEdges,
                    passableEdges);
            theTwoLongestEdge.add(secondEdge);
            return theTwoLongestEdge;
        }
        return null;
    }

    public Edge getOppositeEdge(Edge edge, java.util.List<Edge> edges) { // ???·???????????
        if (edge != null) {
            Edge oppositeEdge = null;
            Point2D startPoint = edge.getStart();
            Point2D endPoint = edge.getEnd();

            for (Edge otherEdge : edges) {
                if (otherEdge.equals(edge)) {
                    continue;
                }
                java.util.List<Point2D> points = new ArrayList<Point2D>();
                points.add(otherEdge.getStart());
                points.add(otherEdge.getEnd());

                if (!points.contains(startPoint) && !points.contains(endPoint)
				/* && Ruler.getDistance(otherEdge, edge) > 500 */) {
                    oppositeEdge = edge;
                }
            }
            return oppositeEdge;
        }
        return null;
    }

    public Edge getOppositeEdge(Edge edge, java.util.List<Edge> impassableEdges,
                                java.util.List<Edge> passableEdges) {
        // edge??????????passEdge???????????????

        if (!passableEdges.isEmpty()) {

            java.util.List<Edge> theTwoLongestEdge = new ArrayList<Edge>();
            Edge firstEdge = null;
            int max = 0;
            for (Edge edge_ : impassableEdges) {
                if (edge_ != null) {
                }
                int length = Ruler
                        .getDistance(edge_.getStart(), edge_.getEnd());
                if (length > max) {
                    max = length;
                    firstEdge = edge_;
                }
            }

            Edge secondEdge = getOppositeEdge(firstEdge, passableEdges);

            // EdgeComparator comparator=new EdgeComparator();
            // PriorityQueue<EdgeAndLength> edgesCom=new
            // PriorityQueue<EdgeAndLength>(5,comparator);
            // List<EdgeAndLength> edge_length=new ArrayList<EdgeAndLength>();
            //
            // for(Edge edge_1 : passableEdges){
            // int
            // distance=Ruler.getDistance(edge_1.getStart(),edge_1.getEnd());
            // EdgeAndLength edge_2=new EdgeAndLength(edge_1, distance);
            // edge_length.add(edge_2);
            // }
            //
            // edgesCom.addAll(edge_length);

            Point2D start = getEdgeCenter(firstEdge); // ???????????????????е?
            Point2D end = getEdgeCenter(secondEdge); // ?????????????????е?

            Point2D vector = new Point2D(end.getX() - start.getX(), end.getY()
                    - start.getY());

            double firstCrossValue = crossMultiply(edge, vector, start);
            for (Edge otherEdge : impassableEdges) {
                if (otherEdge == null || otherEdge.equals(edge)) {
                    continue;
                }
                double secondCrossValue = crossMultiply(otherEdge, vector,
                        start);
                if (firstCrossValue * secondCrossValue < 0) {
                    return otherEdge;
                }
            }
        }
        return null;
    }

    public Edge getOppositeEdge_new(Edge edge, java.util.List<Edge> impassableEdges) {
        if (!impassableEdges.isEmpty()) {
            EdgeAngleComparator comparator = new EdgeAngleComparator(edge);
            PriorityQueue<Edge> edgesComparator = new PriorityQueue<Edge>(5,
                    comparator);
            edgesComparator.addAll(impassableEdges);
            return edgesComparator.poll();
        }
        return null;
    }

    public java.util.List<Point2D> getEdgePoints(Edge edge) {
        if (edge != null) {
            java.util.List<Point2D> points = new ArrayList<Point2D>();
            points.add(edge.getStart());
            points.add(edge.getEnd());
            return points;
        }
        return null;
    }

    public double crossMultiply(Edge edge, Point2D vector, Point2D start) {
        Point2D edgeCenter = getEdgeCenter(edge); // ???ж?????е?
        Point2D vector_1 = new Point2D(edgeCenter.getX() - start.getX(),
                edgeCenter.getY() - start.getY());
        double m = vector.getX() * vector_1.getY() - vector.getY()
                * vector_1.getX();

        return m;
    }

    public Point2D getEdgeCenter(Edge edge) {
        if (edge != null) {
            int x = (edge.getStartX() + edge.getEndX()) / 2;
            int y = (edge.getStartY() + edge.getEndY()) / 2;
            Point2D point = new Point2D(x, y);
            return point;
        }
        return null;

    }

    public boolean isContainPoints(java.util.List<Point2D> firstEdgePoints,
                                   java.util.List<Point2D> secondEdgePoints) {

        boolean flag = false;
        if (!firstEdgePoints.isEmpty() && !secondEdgePoints.isEmpty()) {
            for (Point2D point : secondEdgePoints) {
                if (firstEdgePoints.contains(point)) {
                    flag = true;
                }
            }
        }
        return flag;
    }

    // ???????????
    public int getBlockadeWidth(Blockade blockade) {
        if (blockade != null) {
            int width;
            int min = 10 ^ 8, max = 0;
            Edge closestEdge = getClosestEdge(blockade);
            for (int i = 0; i < blockade.getApexes().length - 1; i += 2) {
                int j = i + 1;
                Point point = new Point(blockade.getApexes()[i],
                        blockade.getApexes()[j]);

                int length = Ruler.getDistance(point, closestEdge);
                if (length > max) {
                    max = length;
                }
                if (length < min) {
                    min = length;
                }
            }
            width = (max - min);
            return width;
        }
        return 0;
    }

    public int getBlockadeWidth(Blockade block, Blockade oppositeBlock) {
        if (block != null) {
            if (oppositeBlock == null) {
                return getBlockadeWidth(block);
            } else {
                int d_1 = getBlockadeWidth(block);
                int d_2 = getBlockadeWidth(oppositeBlock);
                return (d_1 + d_2);
            }
        }
        return 0;
    }

    // ??????x????????????
    public java.util.List<EntityID> getBlockadesClosestTolongEdge(ApolloWorld model) {
        if (!roadBlockades.isEmpty()) {
            java.util.List<EntityID> blockades = new ArrayList<EntityID>();
            for (EntityID blockadeID : roadBlockades) {
                if (blockadeID == null) {
                    continue;
                }
                Blockade blockade = (Blockade) (model.getEntity(blockadeID));
                int d_1 = Ruler.getDistance(
                        new Point(blockade.getX(), blockade.getY()), longEdge);
                int d_2 = Ruler.getDistance(
                        new Point(blockade.getX(), blockade.getY()), shortEdge);
                if (d_1 <= d_2) {
                    blockades.add(blockadeID);
                }
            }
            return blockades;
        }
        return null;
    }

    // ????????????????????
    public java.util.List<EntityID> getBlockadesClosestToshortEdge(
            java.util.List<EntityID> closeLongEdgeBlockades) {
        if (!roadBlockades.isEmpty()) {
            java.util.List<EntityID> blockades = new ArrayList<EntityID>();
            if (closeLongEdgeBlockades == null
                    || closeLongEdgeBlockades.isEmpty()) {
                blockades = roadBlockades;
            } else {
                for (EntityID blockade : roadBlockades) {
                    if (blockade == null) {
                        continue;
                    }
                    if (!closeLongEdgeBlockades.contains(blockade)) {
                        blockades.add(blockade);
                    }
                }
            }
            return blockades;
        }
        return null;
    }

    public Edge getClosestEdge(Blockade blockade) {
        if (blockade != null) {
            int d_1 = Ruler.getDistance(
                    new Point(blockade.getX(), blockade.getY()), longEdge);
            int d_2 = Ruler.getDistance(
                    new Point(blockade.getX(), blockade.getY()), shortEdge);
            if (d_1 > d_2) {
                return shortEdge;
            } else {
                return longEdge;
            }
        }
        return null;
    }


    // ?????????????????
    public EntityID getOppsiteBlockade(Blockade blockade,
                                       java.util.List<EntityID> blockades, ApolloWorld model) {
        if (blockade != null) {
            if (blockades == null) {
                return null;
            }
            int minX = 10 ^ 8, maxX = 0, minY = 10 ^ 8, maxY = 0;
            for (int i = 0; i < blockade.getApexes().length; i++) {
                if (i % 2 == 0) {
                    int x = blockade.getApexes()[i];
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (x < minX) {
                        minX = x;
                    }
                } else {
                    int y = blockade.getApexes()[i];
                    if (y > maxY) {
                        maxY = y;
                    }
                    if (y < minY) {
                        minY = y;
                    }

                }
            }

            double maxArea = 0;
            EntityID maxBlockadeID = null;

            for (EntityID id : blockades) {
                if (id == null) {
                    continue;
                }
                Blockade block = (Blockade) model.getEntity(id);
                int[] apexes = block.getApexes();
                for (int i = 0; i < apexes.length; i++) {
                    if (i % 2 == 0) {
                        if (minX < apexes[i] && apexes[i] < maxX) {
                            double area = block.getShape().getBounds()
                                    .getHeight()
                                    * block.getShape().getBounds().getWidth();
                            if (area > maxArea) {
                                maxBlockadeID = id;
                            }
                        }
                    } else {
                        if (minY < apexes[i] && apexes[i] < maxY) {
                            double area = block.getShape().getBounds()
                                    .getHeight()
                                    * block.getShape().getBounds().getWidth();
                            if (area > maxArea) {
                                maxBlockadeID = id;
                            }
                        }
                    }
                }
            }
            return maxBlockadeID;

        }
        return null;
    }

    public List<Edge> getAllEdges() {
        return area.getEdges();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((area == null) ? 0 : area.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Node other = (Node) obj;
        if (area == null) {
            if (other.area != null)
                return false;
        } else if (!area.equals(other.area))
            return false;
        return true;
    }

    @Override
    public String toString() {
        // return "Node(" + id_ + ")" + " - " + connectedAreas_;
        return "Node(" + this.getArea().getID() + ")" + "passRate  "
                + this.passRate + "  , roadblockades " + this.roadBlockades
                + " ,  nodeWidth  " + this.nodeWidth;
    }
}