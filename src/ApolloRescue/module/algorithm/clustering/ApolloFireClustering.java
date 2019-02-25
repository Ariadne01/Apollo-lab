package ApolloRescue.module.algorithm.clustering;

import ApolloRescue.module.algorithm.ApolloPathPlanning;

import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.entities.BuildingModel;
import ApolloRescue.module.universal.Util;
import adf.agent.communication.MessageManager;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.module.algorithm.Clustering;
import adf.component.module.algorithm.DynamicClustering;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import math.geom2d.polygon.SimplePolygon2D;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static rescuecore2.standard.entities.StandardEntityURN.*;

//import com.sun.jmx.mbeanserver.Util;

public class ApolloFireClustering extends DynamicClustering {
    private int groupingDistance;
    private int idCounter = 1;
    private static int CLUSTER_RANGE_THRESHOLD;
    ApolloFireZone firezone;
    List<List<StandardEntity>> clusterList = new LinkedList<>();
    private List<Polygon> clusterConvexPolygons;
    private List<Cluster> clusters;
    private int myClusterIndex = -1;

    private ApolloPathPlanning pathPlanning;
    private WorldInfo worldInfo;
    private ScenarioInfo scenarioInfo;
    private AgentInfo agentInfo;
    private Map<EntityID, Cluster> entityClusterMap;
    private ApolloWorld world;
    private List<BuildingModel> buildings;

    public ApolloFireClustering(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.groupingDistance = developData.getInteger("ApolloRescue.module.algorithm.clustering.ApolloFireClustering.groupingDistance", 30);
        worldInfo = wi;
        scenarioInfo = si;
        agentInfo=ai;
        entityClusterMap = new HashMap<>();
        clusters = new ArrayList<>();
        clusterConvexPolygons = new ArrayList<>();
        switch (si.getMode()) {
            case PRECOMPUTATION_PHASE:
                this.pathPlanning = moduleManager.getModule("Clustering.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case PRECOMPUTED:
                this.pathPlanning = moduleManager.getModule("Clustering.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
            case NON_PRECOMPUTE:
                this.pathPlanning = moduleManager.getModule("Clustering.PathPlanning", "adf.sample.module.algorithm.SamplePathPlanning");
                break;
        }

        CLUSTER_RANGE_THRESHOLD = scenarioInfo.getPerceptionLosMaxDistance();
        this.world = ApolloWorld.load(ai,wi,si,moduleManager,developData);
    }

    /**
     * calculation phase; update cluster
     *
     * @return own instance for method chaining
     */

    public Clustering calc() {
        Cluster cluster;
        Cluster tempCluster;
        Set<Cluster> adjacentClusters = new HashSet<>();
        this.clusterConvexPolygons = new ArrayList<>();

        for (StandardEntity entity : worldInfo.getEntitiesOfType(BUILDING, AMBULANCE_CENTRE, POLICE_OFFICE, FIRE_STATION, GAS_STATION)) {
            Building building = (Building) entity;
            if (building.isFierynessDefined() && building.getFieryness() != 8
                    && building.isTemperatureDefined() && building.getTemperature() > 25) {

                cluster = getCluster(building.getID());
                if (cluster == null) {
//                    cluster = new FireCluster(world, fireClusterMembershipChecker); //old
                    cluster = new ApolloFireZone(world, buildings);
                    cluster.add(building);

                    //checking neighbour clusters
                    for (StandardEntity neighbourEntity : worldInfo.getObjectsInRange(building.getID(), CLUSTER_RANGE_THRESHOLD)) {
                        if (!(neighbourEntity instanceof Building)) {
                            continue;
                        }

                        tempCluster = getCluster(neighbourEntity.getID());
                        if (tempCluster == null) {
                            //TODO: isEligible_Estimated ok or it should be isEligible
                            /*if (fireClusterMembershipChecker.isEligible_Estimated(world.getMrlBuilding(entity.getID()))) {
                                cluster.add(entity);
                                entityClusterMap.put(entity.getID(), cluster);
                            }*/
                        } else {
                            adjacentClusters.add(tempCluster);
                        }
                    }

                    if (adjacentClusters.isEmpty()) {
                        cluster.setId(idCounter++);
                        addToClusterSet(cluster, building.getID());
                    } else {
                        merge(adjacentClusters, cluster, building.getID());
                    }


                } else {
                    //do noting
                }


            } else { // remove this building if it was in a cluster
                // Was it previously in any cluster?
                cluster = getCluster(building.getID());
                if (cluster == null) {
                    //do nothing
                } else {
                    cluster.remove(building);
                    entityClusterMap.remove(building.getID());//edited by sajjad, 2 lines shifted up
                    if (cluster.entities.isEmpty()) {
                        clusters.remove(cluster);
                    }
                }
            }
            adjacentClusters.clear();

        }
        if(clusters == null){
            System.out.println("clusters is null!!!");
        }
        for (Cluster c : clusters) {
            c.updateConvexHull();
            c.setAllEntities(world.getBuildingsInShape(c.getConvexHullObject().getConvexPolygon()));//Mostafa
        }

        if (getClusterNumber() > 0) {
            for (int i = 0; i < getClusterNumber(); i++) {
                clusterConvexPolygons.add(i, createConvexHull(getClusterEntities(i)));
            }


            double minDistance = Double.MAX_VALUE;
            int nearestClusterIndex = 0;
            for (int i = 0; i < this.clusterConvexPolygons.size(); i++) {
                double distance = Util.distance(this.clusterConvexPolygons.get(i), worldInfo.getLocation(agentInfo.getID()), false);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestClusterIndex = i;
                }
            }

            myClusterIndex = nearestClusterIndex;


            Map<ApolloFireZone, Set<ApolloFireZone>> eat = new HashMap<>();
            List<ApolloFireZone> eatenFireClusters = new ArrayList<>();
            for (Cluster cluster1 : clusters) {
                if (eatenFireClusters.contains((ApolloFireZone) cluster1)) continue;
                Set<ApolloFireZone> feed = new HashSet<>();
                for (Cluster cluster2 : clusters) {
                    if (eatenFireClusters.contains((ApolloFireZone) cluster2)) continue;
                    if (cluster1.equals(cluster2)) continue;
                    if (canEat((ApolloFireZone) cluster1, (ApolloFireZone) cluster2)) {
                        feed.add((ApolloFireZone) cluster2);
                        eatenFireClusters.add((ApolloFireZone) cluster2);
                    }
                }
                eat.put((ApolloFireZone) cluster1, feed);
            }
            for (ApolloFireZone nextCluster : eat.keySet()) {
                for (ApolloFireZone c : eat.get(nextCluster)) {
                    nextCluster.eat(c);
                    // refreshing EntityClusterMap
                    for (StandardEntity entity : c.entities) {
                        entityClusterMap.remove(entity.getID());
                        entityClusterMap.put(entity.getID(), nextCluster);
                    }
                    clusters.remove(c);
                }
            }

            List<StandardEntity> ignoredBorderBuildings = new ArrayList<StandardEntity>();
            for (int i = 0; i < clusters.size() - 1; i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    findMutualEntities((ApolloFireZone) clusters.get(i), (ApolloFireZone) clusters.get(j), ignoredBorderBuildings);
                }
            }


//                System.out.println(agentInfo.getTime() + " " + agentInfo.getID() + " clusterIndex: " +
//                        myClusterIndex + " clusterSize: " + clustering.getClusterEntities(myClusterIndex).size());

//            for (int i = 0; i < this.clustering.getClusterNumber(); i++) {
//                System.out.println(agentInfo.getID() + " first cluster : " + this.clustering.getClusterEntities(i).size());
            /*if (MrlPersonalData.DEBUG_MODE) {
                try {
                    Collection<StandardEntity> clusterEntities = getClusterEntities(nearestClusterIndex);
                    if (clusterEntities != null) {
                        List<Integer> elementList = Util.fetchIdValueFormElements(clusterEntities);
                        VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "MrlSampleBuildingsLayer", (Serializable) elementList);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                }
                if (MrlPersonalData.DEBUG_MODE) {
                try {
                    ArrayList<Polygon> data = new ArrayList<>();
                    data.add(clusterConvexPolygons.get(nearestClusterIndex));
                    VDClient.getInstance().drawAsync(agentInfo.getID().getValue(), "ClusterConvexPolygon", data);
                    } catch (Exception e) {
                    e.printStackTrace();
                    }
                }
//            }*/
//            this.result = this.calcTargetInCluster(myClusterIndex);
            }


//        Set<StandardEntity> borderBuildings = findBorderElements(elements, clustering.getClusterConvexPolygons().get(myClusterIndex));

        return this;
    }





    private void findMutualEntities(ApolloFireZone primaryFireCluster, ApolloFireZone secondaryFireCluster, List<StandardEntity> ignoredBorderBuildings) {
        for (BuildingModel apolloBuilding : getBuildingsInConvexPolygon(primaryFireCluster.getConvexHullObject().getConvexPolygon())) {
            if (getBuildingsInConvexPolygon(secondaryFireCluster.getConvexHullObject().getConvexPolygon()).contains(apolloBuilding)) {
                primaryFireCluster.getIgnoredBorderEntities().add(apolloBuilding.getSelfBuilding());
                primaryFireCluster.getBorderEntities().remove(apolloBuilding.getSelfBuilding());
                secondaryFireCluster.getIgnoredBorderEntities().add(apolloBuilding.getSelfBuilding());
                secondaryFireCluster.getBorderEntities().remove(apolloBuilding.getSelfBuilding());
                ignoredBorderBuildings.add(apolloBuilding.getSelfBuilding());
            }
        }
    }

    public List<BuildingModel> getBuildingsInConvexPolygon(Polygon polygon) {
        List<BuildingModel> result = new ArrayList<BuildingModel>();
        for (BuildingModel mrlBuilding : world.getEstimatedBurningBuildings()) {
            Pair<Integer, Integer> location = worldInfo.getLocation(mrlBuilding.getSelfBuilding().getID());
            if (polygon.contains(location.first(), location.second()))
                result.add(mrlBuilding);
        }
        return result;
    }
    /**
     * merge new cluster to others and replace the result with all others
     *
     * @param adjacentClusters adjacent clusters to the new cluster
     * @param cluster          new constructed cluster
     * @param entityID
     */
    protected void merge(Set<Cluster> adjacentClusters, Cluster cluster, EntityID entityID) {
        int maxCId = 0;
        for (Cluster c : adjacentClusters) {
            if (maxCId < c.getId()) {
                maxCId = c.getId();
            }
            cluster.eat(c);

            // refreshing EntityClusterMap
            for (StandardEntity entity : c.entities) {
                entityClusterMap.remove(entity.getID()); //added 25 khordad! by sajjad & peyman
                entityClusterMap.put(entity.getID(), cluster);
            }
            clusters.remove(c);
            break;//todo: remove this line to merge all possible clusters
        }
        cluster.setId(maxCId);
        addToClusterSet(cluster, entityID);
    }

    protected void addToClusterSet(Cluster cluster, EntityID entityID) {
//        cluster.updateConvexHull()
        entityClusterMap.put(entityID, cluster);
        clusters.add(cluster);
    }

    private Cluster getCluster(EntityID id) {
        return entityClusterMap.get(id);
    }

    @Override
    public Clustering updateInfo(MessageManager messageManager)
    {
        super.updateInfo(messageManager);
        if(this.getCountUpdateInfo() > 1) { return this; }

        this.calc(); // invoke calc()

        System.out.println("Cluster : " + clusters.size());

        return this;
    }

    @Override
    public Clustering precompute(PrecomputeData precomputeData)
    {
        super.precompute(precomputeData);
        if(this.getCountPrecompute() > 1) { return this; }
        return this;
    }

    @Override
    public Clustering resume(PrecomputeData precomputeData)
    {
        super.resume(precomputeData);
        if(this.getCountResume() > 1) { return this; }
        return this;
    }

    @Override
    public Clustering preparate()
    {
        super.preparate();
        if(this.getCountPreparate() > 1) { return this; }
        return this;
    }

    @Override
    public int getClusterNumber()
    {
        return clusters.size();
    }

    @Override
    public int getClusterIndex(StandardEntity standardEntity)
    {
        for (int index = 0; index < clusters.size(); index++)
        {
            if (clusters.get(index).getEntities().contains(standardEntity))
            { return index; }
        }
        return -1;
    }

    @Override
    public int getClusterIndex(EntityID entityID)
    {
        return getClusterIndex(worldInfo.getEntity(entityID));
    }

    @Override
    public Collection<StandardEntity> getClusterEntities(int i)
    {
        if (i < clusters.size()) {
            return clusters.get(i).getEntities();
        } else {
            return null;
        }
    }

    @Override
    public Collection<EntityID> getClusterEntityIDs(int i)
    {
        ArrayList<EntityID> list = new ArrayList<>();
        for (StandardEntity entity : getClusterEntities(i))
        { list.add(entity.getID()); }
        return list;
    }


    /**
     * classify burning building
     * @param building target building
     * @return is building burning
     */
    private boolean isBurning(Building building)
    {
        if (building.isFierynessDefined())
        {
            switch (building.getFieryness())
            {
                case 1: case 2: case 3:
                return true;
                default:
                    return false;
            }
        }
        return false;
    }

    /**
     * output text with class name to STDOUT when debug-mode.
     * @param text output text
     */
    private void debugStdOut(String text)
    {
        if (scenarioInfo.isDebugMode())
        { System.out.println("[" + this.getClass().getSimpleName() + "] " + text); }
    }

    private Polygon createConvexHull(Collection<StandardEntity> clusterEntities) {
        ConvexHull convexHull = new ConvexHull();


        for (StandardEntity entity : clusterEntities) {

            if (entity instanceof Building) {
                Building building = (Building) entity;
                for (int i = 0; i < building.getApexList().length; i += 2) {
                    convexHull.addPoint(building.getApexList()[i],
                            building.getApexList()[i + 1]);
                }
            }
        }
        return convexHull.convex();
    }

    private boolean canEat(ApolloFireZone zone1, ApolloFireZone zone2) {
        int nPointsCluster1 = zone1.getConvexHullObject().getConvexPolygon().npoints;
        int nPointsCluster2 = zone2.getConvexHullObject().getConvexPolygon().npoints;

        double[] xPointsCluster2 = new double[nPointsCluster2];
        double[] yPointsCluster2 = new double[nPointsCluster2];

        for (int i = 0; i < nPointsCluster2; i++) {
            xPointsCluster2[i] = zone2.getConvexHullObject().getConvexPolygon().xpoints[i];
            yPointsCluster2[i] = zone2.getConvexHullObject().getConvexPolygon().ypoints[i];
        }

        SimplePolygon2D cluster2Polygon = new SimplePolygon2D(xPointsCluster2, yPointsCluster2);

        double mapArea = (world.getMapWidth() / 1000) * (world.getMapHeight() / 1000);
        if ((cluster2Polygon.getArea() / 1000000) > mapArea * 0.1) return false;

        if (zone1.getConvexHullObject().getConvexPolygon().contains(zone2.getCenter())) return true;

        rescuecore2.misc.geometry.Point2D clusterCenter = new rescuecore2.misc.geometry.Point2D(zone2.getCenter().getX(), zone2.getCenter().getY());
        Polygon convexPolygon = zone1.getConvexHullObject().getConvexPolygon();
        for (int i = 0; i < nPointsCluster1; i++) {
            rescuecore2.misc.geometry.Point2D point1 = new rescuecore2.misc.geometry.Point2D(convexPolygon.xpoints[i], convexPolygon.ypoints[i]);
            rescuecore2.misc.geometry.Point2D point2 = new rescuecore2.misc.geometry.Point2D(convexPolygon.xpoints[(i + 1) % nPointsCluster1], convexPolygon.ypoints[(i + 1) % nPointsCluster1]);
            if (Util.distance(new rescuecore2.misc.geometry.Line2D(point1, point2), clusterCenter) < 30000) {
                return true;
            }
        }
        return false;
    }

    public List<Polygon> getClusterConvexPolygons() {
        return this.clusterConvexPolygons;
    }

    public Cluster findNearestCluster(Pair<Integer, Integer> location) {
        if (clusters == null || clusters.isEmpty()) {
            return null;
        }
        Cluster resultFireCluster = null;
        double minDistance = Double.MAX_VALUE;
        Set<Cluster> dyingAndNoExpandableClusters = new HashSet<>();
        for (Cluster cluster : clusters) {
            if (cluster.isDying() || (cluster instanceof ApolloFireZone && !((ApolloFireZone) cluster).isExpandableToCenterOfMap())) {
                dyingAndNoExpandableClusters.add(cluster);
                continue;
            }
            double distance = Util.distance(cluster.getConvexHullObject().getConvexPolygon(), location);
            if (distance < minDistance) {
                minDistance = distance;
                resultFireCluster = cluster;
            }
        }
        minDistance = Double.MAX_VALUE;
        if (resultFireCluster == null) {
            for (Cluster cluster : dyingAndNoExpandableClusters) {
                double distance = Util.distance(cluster.getConvexHullObject().getConvexPolygon(), location);
                if (distance < minDistance) {
                    minDistance = distance;
                    resultFireCluster = cluster;
                }
            }
        }
        return resultFireCluster;
    }
    public int getMyClusterIndex(){
	return myClusterIndex;
    }

}
