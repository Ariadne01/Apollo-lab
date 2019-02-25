package ApolloRescue.extaction.clear;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.algorithm.samplesearch.BreadthFirstSearch;
import ApolloRescue.module.algorithm.samplesearch.DistanceInterface;
import ApolloRescue.module.algorithm.samplesearch.Graph;
import ApolloRescue.module.complex.component.RoadInfoComponent;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.Util;
import ApolloRescue.module.universal.entities.EdgeModel;
import ApolloRescue.module.universal.entities.Path;
import ApolloRescue.module.universal.entities.RoadModel;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.io.Console;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GuideLineHelper {

    protected WorldInfo worldInfo;
    protected ApolloWorld world;
    protected AgentInfo agentInfo;
    protected ScenarioInfo scenarioInfo;
    private ClearHelper clearHelper;
    private double clearRange;
    private GuideLine lastGuideline;
    private EntityID lastTarget;
    private List<EntityID> lastPlan;
    protected List<Path> paths = new ArrayList<>();

    public GuideLineHelper(ApolloWorld world, WorldInfo worldInfo, AgentInfo agentInfo, ScenarioInfo scenarioInfo, double clearRange) {
        this.world = world;
        this.worldInfo = worldInfo;
        this.agentInfo = agentInfo;
        this.scenarioInfo = scenarioInfo;
        this.clearHelper = new ClearHelper(world, worldInfo, agentInfo, scenarioInfo);
        this.clearRange = clearRange;
        this.lastPlan = new ArrayList<>();
        this.paths = new ArrayList<>();
//        generateGuidelines();//ljy: TODO: TEST
    }

    public GuideLine findTargetGuideline(List<EntityID> movePlan, double range, EntityID targetID) {
        if (movePlan == null || range <= 0 || targetID == null) {
            // 若传入的参数之一无效，则全部置为lastXxx。 重新寻找TargetGuideline。
            lastGuideline = null;
            lastPlan = movePlan;
            lastTarget = targetID;
            return null;
        }

        GuideLine guideLine;
        //new guideline method
//        if(lastGuideline!=null && lastGuideline.isMinor() && lastGuideline.getAreas().contains(world.getSelfPosition().getID())){
//            //for minor guidelines which connects Major guideline to entrance
//            guideLine = lastGuideline;
//        }else {
        guideLine = findByPath(movePlan);
//        }

//        if (guideLine != null) world.printData("GUIDELINE : new = " + guideLine);

        if (guideLine != null && isNeedToInvert(guideLine, movePlan)) {
//            guideLine = new GuideLine(guideLine.getEndPoint(), guideLine.getOrigin());
            Point2D origin = guideLine.getOrigin();
            Point2D end = guideLine.getEndPoint();
            guideLine.setEnd(origin);
            guideLine.setOrigin(end);
        }

        if (guideLine == null) {
            guideLine = findByMovePlan(movePlan, range, targetID);
        }


        lastGuideline = guideLine;
        lastPlan = movePlan;
        lastTarget = targetID;
        return guideLine;
    }

    /**
     * 当guideLine和curToNextLine角度在[-90°,90°]之外，则需要Invert(翻转guideLine)
     * @param guideLine
     * @param movePlan
     * @return
     */
    private boolean isNeedToInvert(GuideLine guideLine, List<EntityID> movePlan) {
        if (guideLine == null || movePlan.size() <= 1 || guideLine.isMinor()) {
            return false;
        }
        Area currentPosition = (Area) worldInfo.getEntity(movePlan.get(0));
        Area nextPosition = (Area) worldInfo.getEntity(movePlan.get(1));
//        Util.getMiddle(currentPosition.getEdgeTo(nextPosition.getID()).getLine());
        Line2D curToNextLine = new Line2D(
//                Util.getPoint(currentPosition.getLocation(world)),
                Util.getPoint(worldInfo.getLocation(currentPosition)),
                Util.getMiddle(currentPosition.getEdgeTo(nextPosition.getID()).getLine())
        );
        double angle = Util.angleBetween2Lines(curToNextLine, guideLine);

//        if(angle>60 && angle <120){
//            world.printData("#######Suspicious GUIDELINE ANGLE is " + angle);
//        }

        if (angle <= 90 &&   angle >= -90) {
            return false;
        } else {
            return true;
        }


    }

    /**
     * ( new method )
     * Find the GuideLine by movePlan(Path)
     * @param movePlan
     * @return guideLine
     */
    private GuideLine findByPath(List<EntityID> movePlan) {
        GuideLine guideLine = null;

        if (movePlan.size() >= 2) {
            EntityID currentPosition = movePlan.get(0);
            EntityID nextPosition = movePlan.get(1);

            Path path = null;
            RoadModel road = world.getRoadModel(currentPosition);
            if (road != null) {
                //new guideline method

                StandardEntity nextEntity = worldInfo.getEntity(nextPosition);
                if (nextEntity instanceof Road) {//获取一条(从当前位置)通往nextEntity的路径path
                    for (Path p : road.getPaths()) {
                        if (p.contains(nextEntity)) {
                            path = p;
                            break;
                        }
                    }
                }

                if (path != null) {

                    //ljy: TODO: test what if there's no "generateGuidelines()" in the constructor...
                    if(path.getGuideLines().isEmpty() || path.getGuideLines() == null){
                        world.printData("Empty Guidelines!!!【GuideLineHelper】 Agent:" + agentInfo.toString());
                    }

                    for (GuideLine gl : path.getGuideLines()) {
                        if (gl.getAreas().contains(road.getID())) {
                            guideLine = gl;
                            break;
                        }
                    }
                    // 若整个路径都不包含nextPosition, 重新设置guideLine --> GUIDELINE TO ENTRANCE....
                    if (guideLine != null && !path.getHeadToEndRoads().contains(nextPosition)) {
                        Edge edge = road.getParent().getEdgeTo(nextPosition);//当前位置道路与nextPosition的边
                        Point2D middle = Util.getMiddle(edge.getLine());//当前位置道路与nextPosition边的中点
                        Point2D closestPoint = Util.closestPoint(guideLine, middle);//原guideLine处，距离middle最近的一点
                        guideLine = new GuideLine(closestPoint, middle);
                        List<EntityID> areas = new ArrayList<>();
                        areas.add(currentPosition);
//                        areas.add(nextPosition);
                        guideLine.setAreas(areas);
                        guideLine.setMinor(true);
                        printData("GUIDELINE TO ENTRANCE....");
                    }
                }
            }
        }

        return guideLine;
    }


    /**
     * Old method for find guideline by move plan
     *
     * @param path     move plan
     * @param range    clear range
     * @param targetID target
     * @return guideline
     */
    private GuideLine findByMovePlan(List<EntityID> path, double range, EntityID targetID) {
        if (path == null || range <= 0 || targetID == null) {
            return null;
        }

        Point2D agentPosition = Util.getPoint(getSelfLocation());
        StandardEntity target = worldInfo.getEntity(targetID);

        GuideLine guideLine = null;
        if (path.size() == 1) {
            if (target instanceof Building) {
                Set<Edge> edges = RoadInfoComponent.getEdgesBetween((Area) worldInfo.getEntity(path.get(0)), (Area) target);
                if (edges != null && !edges.isEmpty()) {
                    Edge edge = edges.iterator().next();
                    int middleX = (edge.getStartX() + edge.getEndX()) / 2;
                    int middleY = (edge.getStartY() + edge.getEndY()) / 2;

                    Point2D targetPoint = new Point2D(middleX, middleY);
                    List<GuideLine> pathGuidelines = clearHelper.getPathGuidelines(path, targetPoint);
                    if (pathGuidelines.isEmpty()) {
                        guideLine = new GuideLine(agentPosition, targetPoint);
                    } else {
                        guideLine = clearHelper.getTargetGuideLine(pathGuidelines, path, targetID, clearRange);
                    }
                }
            }
        } else if (!targetID.equals(lastTarget) ||
                lastGuideline == null ||
                !lastGuideline.getAreas().contains(getSelfPosition().getID()) ||
                (lastPlan == null || !lastPlan.containsAll(path))) {
            Point2D targetPoint = Util.getPoint(worldInfo.getLocation(target));
            List<GuideLine> pathGuidelines = clearHelper.getPathGuidelines(path, targetPoint);
            if (pathGuidelines.isEmpty()) {
                guideLine = new GuideLine(agentPosition, targetPoint);
            } else {
                guideLine = clearHelper.getTargetGuideLine(pathGuidelines, path, targetID, clearRange);

            }
        } else {
            guideLine = lastGuideline;
        }


        return guideLine;
    }


    public void generateGuidelines() {
        Long before = System.currentTimeMillis();
        //prepare file
//        if (fileReadWrite) {
//            prepareFile();
//        }

//        if (!fileReadWrite) {//should read from file
//            if (readFromFile()) {
//                //System.out.println("Generate guidelines took " + (System.currentTimeMillis() - before) + "ms");
//                return;//successful
//            }
//        }

        //merge inline paths
        mergePaths(paths);

        for (Path path : paths) {
            Set<GuideLine> guidelines = findGuidelines(path);
            path.setGuideLines(guidelines);
        }

        //write into file
//        if (fileReadWrite) {
//            putIntoFile(paths);
//        }

        //System.out.println("Generate guidelines took " + (System.currentTimeMillis() - before) + "ms");
    }

    private Set<GuideLine> findGuidelines(Path path) {
        Road absSource = path.getHeadOfPath();
        Road absDestination = path.getEndOfPath();
        if (absSource.equals(absDestination)) {
            System.out.println("HEAD AND END OF PATH ARE EQUAL<----------------");
            return new HashSet<>();
        }


        List<EntityID> wholePath = new ArrayList<>(planMove(absSource, absDestination, new ArrayList<>(path)));
        path.setHeadToEndRoads(wholePath);

        Set<GuideLine> guideLines = new HashSet<>();


        int srcIndex = 0;
        int dstIndex = wholePath.size() - 1;
        Road dst;


        Edge edgeTo1 = absSource.getEdgeTo(wholePath.get(srcIndex + 1));
        if (edgeTo1 == null) {
            System.out.println("EDGE-TO IS NULL <------------------");
            return new HashSet<>();
        }
        Point2D mid1 = Util.getMiddle(edgeTo1.getLine());
        Edge edgeTo = absDestination.getEdgeTo(wholePath.get(dstIndex - 1));
        if (edgeTo == null) {
            System.out.println("EDGE-TO IS NULL <------------------");
            return new HashSet<>();
        }
        Point2D mid2 = Util.getMiddle(edgeTo.getLine());
        GuideLine semiGuideline = new GuideLine(mid1.getX(), mid1.getY(), mid2.getX(), mid2.getY());

        do {
            dst = absDestination;
            dstIndex = wholePath.size() - 1;

            for (int i = dstIndex; i > srcIndex; i--) {
                RoadModel road = world.getRoadModel(wholePath.get(i));
                if (road == null) {
                    System.out.println("THIS IS NOT ROAD! <----------------");
                    continue;
                }
                List<EdgeModel> mrlEdgesTo = road.getEdgeModelsTo(wholePath.get(i - 1));
                boolean closeEnough = false;
                Point2D dstPoint = new Point2D(dst.getX(), dst.getY());

                for (EdgeModel mrlEdge : mrlEdgesTo) {
                    dstPoint = mrlEdge.getMiddle();
                    double distance = Util.distance(semiGuideline, dstPoint);

                    double distanceThreshold = ApolloConstants.AGENT_SIZE / 2;
                    if (distance < distanceThreshold) {
                        closeEnough = true;
                        break;
                    }

                }

                if (!closeEnough && srcIndex < i - 1) {//todo 2nd condition should reviewed
                    dstIndex = i - 1;
                    dst = (Road) worldInfo.getEntity(wholePath.get(dstIndex));
                    semiGuideline = new GuideLine(semiGuideline.getOrigin().getX(), semiGuideline.getOrigin().getY(), dstPoint.getX(), dstPoint.getY());
                }

            }
            if (Util.lineLength(semiGuideline) < 1) {
//                System.out.println("Unexpected guideline. src:" + srcIndex + " \tdst:" + dstIndex);
            } else {
                semiGuideline.setAreas(wholePath.subList(srcIndex + 1, dstIndex + 1));
                semiGuideline.setMinor(false);
                guideLines.add(semiGuideline);
            }
            if (!dst.getID().equals(absDestination.getID())) {
                srcIndex = dstIndex;
                semiGuideline = new GuideLine(semiGuideline.getEndPoint().getX(), semiGuideline.getEndPoint().getY(), mid2.getX(), mid2.getY());
            }
        } while (!dst.getID().equals(absDestination.getID()));
        return guideLines;
    }


    /**
     * BFS plan move instead of A*
     *
     * @param src  source road
     * @param dst  destination road
     * @param path list of Road that should we should find path in it.
     * @return path
     */
    private List<EntityID> planMove(Road src, Road dst, List<Area> path) {
        List<EntityID> plan = new ArrayList<>();
        Graph graph = new Graph(path);
        BreadthFirstSearch bfs = new BreadthFirstSearch();
        plan.add(src.getID());
        plan.addAll(bfs.search(src.getID(), dst.getID(), graph, new DistanceInterface(worldInfo)));
        return plan;
    }


    private void mergePaths(List<Path> paths) {
        //do nothing
    }


//    private void putIntoFile(List<Path> paths) {
//        boolean couldWrite = false;
////        File file = null;
////        file = new File(ApolloConstants.PRECOMPUTE_DIRECTORY + world.getUniqueMapNumber() + ".gdln");
//        FileOutputStream fileOutputStream = null;
//        try {
//            fileOutputStream = new FileOutputStream(file);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;
//        }
//
//        StringBuilder strBuilder = new StringBuilder();
//        for (Path path : paths) {
//            strBuilder.append(path.getId());
//            strBuilder.append("\n");
//
//            strBuilder.append(path.getGuideLines().size());
//            strBuilder.append("\n");
//
//            for (GuideLine guideLine : path.getGuideLines()) {
//                strBuilder.append(guideLine.getOrigin());
//                strBuilder.append("\n");
//                strBuilder.append(guideLine.getEndPoint());
//                strBuilder.append("\n");
//
//                strBuilder.append(guideLine.getAreas());
//                strBuilder.append("\n");
//
//            }
//        }
//        OutputStreamWriter osr = new OutputStreamWriter(fileOutputStream);
//        try {
//            osr.write(strBuilder.toString());
//            osr.flush();
//            osr.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//    }


//    private boolean readFromFile() {
//
//        if (!file.exists() || !file.isFile() || !file.canRead()) {
//            return false;
//        }
//
//
//        String line;
//        try {
////            FileReader fileReader = new FileReader(file);
//
////            BufferedReader br = new BufferedReader(fileReader);
//
////            Scanner scan = new Scanner(file);
//            String s = new Scanner(file).useDelimiter("\\Z").next();
//            Scanner scan = new Scanner(s);
//
////            InputStream fis = new FileInputStream(file);
////            InputStreamReader isr = new InputStreamReader(fis);
////            BufferedReader br = new BufferedReader(isr);
//
//            //path id
//            Path path;
//            while (scan.hasNextLine()) {
//                line = scan.nextLine();
//                if (line == null) {
//                    return false;
//                }
//                path = world.getPath(new EntityID(Integer.parseInt(line)));
//
//                //////guidelines
//                //guideline count
//                line = scan.nextLine();
//
//                int count = Integer.parseInt(line);
//                Set<GuideLine> guideLines = new HashSet<>();
//                for (int i = 0; i < count; i++) {
//                    line = scan.nextLine();
//                    Point2D head = convertToPoint(line);
//
//                    line = scan.nextLine();
//                    Point2D end = convertToPoint(line);
//                    GuideLine guideLine = new GuideLine(head, end);
//
//                    //guideline areas contains
//                    line = scan.nextLine();
//                    guideLine.setAreas(convertToEntityIDList(line));
//
//                    guideLine.setMinor(false);
//                    guideLines.add(guideLine);
//                }
//
//
//                path.setGuideLines(guideLines);
////            while ((line = scan.nextLine()) != null) {
////               line
////            }
//            }
//            scan.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        } catch (NoSuchElementException ex) {
//            return false;
//        }
//        return true;
//    }

    private Point2D convertToPoint(String str) {
        int index = str.indexOf(',');
        double x = Double.parseDouble(str.substring(0, index - 1));
        double y = Double.parseDouble(str.substring(index + 2));
        return new Point2D(x, y);
    }

    private List<EntityID> convertToEntityIDList(String str) {

        int index, begin = 1;
        List<EntityID> list = new ArrayList<>();
        while ((index = str.indexOf(',', begin)) > 0) {
            EntityID id = new EntityID(Integer.parseInt(str.substring(begin, index)));

            list.add(id);
            begin = index + 2;
        }
        if (begin > 1) {
            index = str.indexOf(']', begin);
            EntityID id = new EntityID(Integer.parseInt(str.substring(begin, index)));
            list.add(id);
        }
        return list;
    }


//    private void prepareFile() {
//        boolean couldCreate = false;
//        try {
////            File file = new File(ApolloConstants.PRECOMPUTE_DIRECTORY + world.getUniqueMapNumber() + ".gdln");
//            if (!file.exists() || !file.isFile()) {
//                couldCreate = file.createNewFile();
//
//            } else {
////                file.delete();
////                file.createNewFile();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (!couldCreate) {
////                fileReadWrite = false;
//            }
//        }
//    }

    private Pair<Integer, Integer> getSelfLocation() {
        return worldInfo.getLocation(agentInfo.getID());
    }

    private Area getSelfPosition() {
        return agentInfo.getPositionArea();
    }

    private void printData(String s) {
        System.out.println(s);
    }


}