package ApolloRescue.module.complex.firebrigade;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import ApolloRescue.ApolloConstants;
import ApolloRescue.module.algorithm.clustering.ApolloFireClustering;
import ApolloRescue.module.complex.component.Info.RoadInfo;
import ApolloRescue.module.complex.component.RoadInfoComponent;
import ApolloRescue.module.complex.firebrigade.search.HighValueBasedFireZoneSearch;
import ApolloRescue.module.complex.firebrigade.search.IFireZoneSearch;
import ApolloRescue.module.universal.ApolloWorld;
import ApolloRescue.module.universal.CommandTypes;
import ApolloRescue.module.universal.entities.BuildingModel;
import ApolloRescue.module.algorithm.clustering.FireSimulator;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.centralized.CommandFire;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import javolution.util.FastMap;
import rescuecore2.config.NoSuchConfigOptionException;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;


public class FireBrigadeWorld extends ApolloWorld {

    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(FireBrigadeWorld.class);
    protected RoadInfoComponent roadInfoComponent;
    protected FireSimulator simulator;
    //    private WaterCoolingEstimator coolingEstimator;
    private Map<EntityID, EntityID> gotoMap = new FastMap<EntityID, EntityID>();

    //----------------- connection value ---------------
    private boolean isPolyLoaded;
    private float rayRate = 0.0025f;
    //--------------------------------------------------

    private int maxWater;
    private int waterRefillRate;
    private int waterRefillRateInHydrant;
    private boolean isVisibilityAreaDataLoaded;
    private boolean isBorderEntitiesDataLoaded;
    private String fileName;
    private ApolloFireClustering fireClustering;
    /** The fire zone search manager which uses to update fire zone info*/
    protected IFireZoneSearch fireZoneSearch;

    /** The last command */
    private int lastCommand;


    public FireBrigadeWorld(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, DevelopData developData) {
        super(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);

//        createClusterManager();
        fileName = getMapName() + ".rays";
        fireZoneSearch = new HighValueBasedFireZoneSearch(this);
    }

    private void prepareFirebrigadeWorld(ScenarioInfo scenarioInfo) {
        isVisibilityAreaDataLoaded = false;
        isBorderEntitiesDataLoaded = false;
        roadInfoComponent = getComponent(RoadInfoComponent.class);
        fireZoneSearch = new HighValueBasedFireZoneSearch(this);



        //----------------- connection value ---------------
        initSimulator(this);


//        coolingEstimator = new WaterCoolingEstimator();

        setMaxWater(scenarioInfo.getFireTankMaximum());
        int refugeRefillRateTemp = ApolloConstants.WATER_REFILL_RATE;
        try {
            refugeRefillRateTemp = scenarioInfo.getFireTankRefillRate();
            isWaterRefillRateInRefugeSet = true;
        } catch (NoSuchConfigOptionException ignored) {
            isWaterRefillRateInRefugeSet = false;
        }
        setWaterRefillRate(refugeRefillRateTemp);//It can not be reached from config.getIntValue(WATER_REFILL_RATE_KEY);

        int hydrantRefillRateTemp = ApolloConstants.WATER_REFILL_RATE_IN_HYDRANT;
        try {
            hydrantRefillRateTemp = scenarioInfo.getFireTankRefillHydrantRate();
            isWaterRefillRateInHydrantSet = true;
        } catch (NoSuchConfigOptionException ignored) {
            isWaterRefillRateInHydrantSet = false;
        }
        setWaterRefillRateInHydrant(hydrantRefillRateTemp);
        //setBorderBuildings();


//        MrlPersonalData.VIEWER_DATA.setExtinguishRange(getMaxExtinguishDistance());
        //call process area visibility
//   ProcessAreaVisibility.process(this, config);
    }


    @Override
    public ApolloWorld precompute(PrecomputeData precomputeData) {
        super.precompute(precomputeData);
        if (this.getCountPrecompute() >= 2) {
            return this;
        }

        fireClustering = new ApolloFireClustering(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
//        if(this.fireClustering == null){
//            System.out.println("fireclustering made lose");
//        }else{
//            System.out.println("fireclustering made success");
//        }

        shouldPrecompute = true;
        try {
            createCND(PrecomputeData.PRECOMP_DATA_DIR.getAbsolutePath() + File.separator + fileName);
            /*if (MrlPersonalData.DEBUG_MODE) {
                System.out.println("CND is created.");
            }*/
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }

    @Override
    public ApolloWorld resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        shouldPrecompute = false;
        if (this.getCountResume() >= 2) {
            return this;
        }

        fireClustering = new ApolloFireClustering(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
//        if(this.fireClustering == null){
//            System.out.println("fireclustering made lose");
//        }else{
//            System.out.println("fireclustering made success");
//        }
        try {
            System.out.println("FB world preparate");
            readCND(PrecomputeData.PRECOMP_DATA_DIR.getAbsolutePath() + File.separator + fileName);

            prepareFirebrigadeWorld(scenarioInfo);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }

    @Override
    public ApolloWorld preparate() {
        super.preparate();
        shouldPrecompute = false;
//        if (this.getCountPreparate() >= 2) {
//            return this;
//        }
        fireClustering = new ApolloFireClustering(agentInfo, worldInfo, scenarioInfo, moduleManager, developData);
//        if(this.fireClustering == null){
//            System.out.println("fireclustering made lose");
//        }else{
//            System.out.println("fireclustering made success");
//        }
        try {
            System.out.println("FB world preparate");
            readCND(PrecomputeData.PRECOMP_DATA_DIR.getAbsolutePath() + File.separator + fileName);

            prepareFirebrigadeWorld(scenarioInfo);


        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }


    private void initSimulator(final FireBrigadeWorld world) {
        Thread loader = new Thread() {
            @Override
            public void run() {
//                initConnectionValues();
                simulator = new FireSimulator(world);
            }
        };
        loader.run();
    }

    private int lastUpdateTime = 0;

    @Override
    public ApolloWorld updateInfo(MessageManager messageManager) {
        if (lastUpdateTime >= getTime()) {
            return this;
        }
        updateBeforeSense();
        super.updateInfo(messageManager);
        updateAfterSense();
        lastUpdateTime = getTime();
        if (messageManager.getReceivedMessageList(CommandFire.class).size()>0){
            CommunicationMessage message = messageManager.getReceivedMessageList(CommandFire.class).get(0);
            CommandFire command = (CommandFire)message;
            lastCommand = command.getAction();
        }else{
            lastCommand = 1;
        }


        if(fireClustering == null){
            fireClustering = new ApolloFireClustering(agentInfo,worldInfo,scenarioInfo,moduleManager,developData);
//	    System.out.println("I have a new cireclustering");
	    fireClustering.updateInfo(messageManager);
        }else{
	   fireClustering.updateInfo(messageManager);
//           System.out.println("update successfully!!!!!!!!!!!!!");
	}

//	    System.out.println("fireclustering!!!!!!");
        return this;
    }

    public void updateBeforeSense() {
        if (simulator != null) {
            simulator.update();
        }
    }

    public void updateAfterSense() {


//    updates in clustering module.
//        try {
//            fireClusterManager.updateClusters();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        estimatedBurningBuildings.clear();
        for (BuildingModel building : getBuildingsModel()) {
            if (building.getEstimatedFieryness() >= 1 && building.getEstimatedFieryness() <= 3) {
                estimatedBurningBuildings.add(building);
            }
        }

        //TODO @MRL @MAHDI updateAvailableHydrants();
//        MrlPersonalData.setCivilianClusterManager(civilianClusterManager.updateConvexHullsForViewer());
    }

//    private void createClusterManager() {
//        fireClusterManager = new mrl.common.clustering.FireClusterManager(this);
////        setCivilianClusterManager(new CivilianClusterManager(this));
////        policeTargetClusterManager=new PoliceTargetClusterManager(this);
//
//    }


    //----------------- connection value ---------------
    private void initConnectionValues() {
        String fileName = ApolloConstants.PRECOMPUTE_DIRECTORY + getMapName() + ".rays";
        try {
            readCND(fileName);
        } catch (Exception e) {
            if (ApolloConstants.DEBUG_FIRE_BRIGADE) {
                System.err.println("Unable to load CND files");
                Logger.debug("Unable to Load CND files");
            }

            try {
//                if (LaunchMRL.shouldPrecompute) {
                createCND(fileName);
//                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void readCND(String fileName) throws IOException {
        File f = new File(fileName);
        BufferedReader br = new BufferedReader(new FileReader(f));
        float rayDens = Float.parseFloat(br.readLine());
        String nl;
        while (null != (nl = br.readLine())) {
            int x = Integer.parseInt(nl);
            int y = Integer.parseInt(br.readLine());
            int quantity = Integer.parseInt(br.readLine());
            double hitRate = Double.parseDouble(br.readLine());
            List<BuildingModel> bl = new ArrayList<BuildingModel>();
            List<EntityID> bIDs = new ArrayList<EntityID>();
            List<Float> weight = new ArrayList<Float>();
            for (int c = 0; c < quantity; c++) {
                int ox = Integer.parseInt(br.readLine());
                int oy = Integer.parseInt(br.readLine());
                Building building = getBuildingInPoint(ox, oy);
                if (building == null) {
                    System.err.println("building not found: " + ox + "," + oy);
                    br.readLine();
                } else {
                    bl.add(getBuildingsModel(building.getID()));
                    bIDs.add(building.getID());
                    weight.add(Float.parseFloat(br.readLine()));
                }

            }
            Building b = getBuildingInPoint(x, y);
            BuildingModel building = getBuildingsModel(b.getID());
//            buildingHelper.setConnectedBuildings(b.getID(), bl);
//            buildingHelper.setConnectedValue(b.getID(), weight);
            building.setConnectedBuilding(bl);
            building.setConnectedValues(weight);
            building.setHitRate(hitRate);
        }
        br.close();
        if (ApolloConstants.DEBUG_FIRE_BRIGADE) {
            System.out.println("Read from file:" + fileName);
        }
    }

    private void createCND(String fileName) throws IOException {
        if (ApolloConstants.DEBUG_FIRE_BRIGADE) {
//            System.out.println("  Creating CND Files .... ");
        }

        int n = 1;
        long t1 = System.currentTimeMillis();
        long timeStart = System.currentTimeMillis();

//        System.out.println("init walls time = "+(System.currentTimeMillis()-timeStart));

        //int size = getBuildingsModel().size();

        File f = new File(fileName);
//        noinspection ResultOfMethodCallIgnored
        BufferedWriter bw = null;
        if (this.shouldPrecompute) {
            f.createNewFile();
            bw = new BufferedWriter(new FileWriter(f));
            bw.write(rayRate + "\n");
        }

        for (BuildingModel apolloBuilding : getBuildingsModel()) {

            apolloBuilding.initWallValues(this);
            if (bw != null) {
                bw.write(apolloBuilding.getSelfBuilding().getX() + "\n");
                bw.write(apolloBuilding.getSelfBuilding().getY() + "\n");
                bw.write(apolloBuilding.getConnectedBuilding().size() + "\n");
                bw.write(apolloBuilding.getHitRate() + "\n");
            }

            for (int c = 0; c < apolloBuilding.getConnectedBuilding().size(); c++) {
                BuildingModel building = apolloBuilding.getConnectedBuilding().get(c);
                Float val = apolloBuilding.getConnectedValues().get(c);
                if (bw != null) {
                    bw.write(building.getSelfBuilding().getX() + "\n");
                    bw.write(building.getSelfBuilding().getY() + "\n");
                    bw.write(val + "\n");
                }
            }
//            if (MRLConstants.DEBUG_FIRE_BRIGADE) {
//
//                long dt = System.currentTimeMillis() - t1;
//                dt = dt / n;
//                dt = dt * (size - n);
//                long sec = dt / (1000);
//                long min = (sec / 60) % 60;
//                long hour = sec / (60 * 60);
//                sec = sec % 60;
//
////                if (n % 100 == 0)
////                    System.out.println(" Time Left: " + hour + ":" + min + ":" + sec+" rayrate:"+rayRate);
//            }
            apolloBuilding.cleanup();
        }
        if (bw != null) {
            bw.close();
        }
        if (ApolloConstants.DEBUG_FIRE_BRIGADE) {
//            System.out.println("wrote CND file \"" + fileName + "\"");
            printTookTime("creating CND files", timeStart);
        }
    }

//    private void writeCND(String fileName) throws IOException {
//        File f = new File(fileName);
//        //noinspection ResultOfMethodCallIgnored
//        f.createNewFile();
//        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
//        bw.write(rayRate + "\n");
//        for (StandardEntity standardEntity : getEntities()) {
//            Building b = (Building) standardEntity;
//            bw.write(b.getX() + "\n");
//            bw.write(b.getY() + "\n");
//            bw.write(buildingHelper.getConnectedBuildings(b.getID()).size() + "\n");
//            for (int c = 0; c < buildingHelper.getConnectedBuildings(b.getID()).size(); c++) {
//                EntityID id = buildingHelper.getConnectedBuildings(b.getID()).get(c);
//                Float val = buildingHelper.getConnectedValue(b.getID()).get(c);
//                Building building = (Building) getEntity(id);
//                bw.write(building.getX() + "\n");
//                bw.write(building.getY() + "\n");
//                bw.write(val + "\n");
//            }
//        }
//        bw.close();
//        System.out.println("wrote CND file \"" + fileName + "\"");
//    }

    public static void printTookTime(String title, long start) {
        long dtTotal = System.currentTimeMillis() - start;
        long hour = dtTotal / (1000 * 60 * 60);
        dtTotal = dtTotal % (1000 * 60 * 60);
        long min = dtTotal / (1000 * 60);
        dtTotal = dtTotal % (1000 * 60);
        long sec = dtTotal / (1000);

//        System.out.println(title + " took  " + (hour < 10 ? "0" + hour : hour) + ":" + (min < 10 ? "0" + min : min) + ":" + (sec < 10 ? "0" + sec : sec));
    }

//    public Point getSecureCenterOfMap() {
//        HashSet<Building> unBurnedBuildings = new HashSet<Building>();
//        for (Building b : this.getEntities()) {
//            if (b.isUnburned())
//                unBurnedBuildings.add(b);
//        }
//        int sumX = 0;
//        int sumY = 0;
//        for (Building bd : unBurnedBuildings) {
//            sumX += bd.getX();
//            sumY += bd.getY();
//        }
//        if (!unBurnedBuildings.isEmpty()) {
//            sumX /= unBurnedBuildings.size();
//            sumY /= unBurnedBuildings.size();
//        }
//        return new Point(sumX, sumY);
//    }

    public boolean isPolyLoaded() {
        return isPolyLoaded;
    }

    public void setPolyLoaded(boolean polyLoaded) {
        isPolyLoaded = polyLoaded;
    }

    public float getRayRate() {
        return rayRate;
    }

    /*  public List<StandardEntity> getFreeFireBrigades() {
          HumanHelper humanHelper = getHelper(HumanHelper.class);
          MrlPlatoonAgent mrlPlatoonAgent;
          List<StandardEntity> freeAgents = new ArrayList<StandardEntity>();
          freeAgents.addAll(getFireBrigades());
          freeAgents.removeAll(humanHelper.getBlockedAgents());
          freeAgents.removeAll(getBuriedAgents());
          List<StandardEntity> atRefuges = new ArrayList<StandardEntity>();
          for (StandardEntity entity : freeAgents) {
              FireBrigade fireBrigade = (FireBrigade) entity;
              if (!fireBrigade.isPositionDefined() || (getEntity(fireBrigade.getPosition()) instanceof Refuge)) {
                  atRefuges.add(fireBrigade);
              }
          }
          freeAgents.removeAll(atRefuges);
          return freeAgents;
      }
  */
    public RoadInfoComponent getRoadInfoComponent() {
        return roadInfoComponent;
    }

//    public List<MrlBuilding> getMrlBuildings() {
//        return mrlBuildings;
//    }
//
//    public MrlBuilding getMrlBuilding(EntityID id) {
//        return tempBuildingsMap.get(id);
//    }


    public FireSimulator getSimulator() {
        return simulator;
    }

  /*  public FireClusters getFireClusters() {
//        return fireClusters;         //TODO commented by sajjad, uncomment if needed, but it should be unnessesary
        return null;
    }*/

//    public WaterCoolingEstimator getCoolingEstimator() {
//        return coolingEstimator;
//    }

    public Map getGotoMap() {
        return gotoMap;
    }

    public void addGotoMap(Map<EntityID, EntityID> FireBrigadeGotoMAp) {
        for (EntityID id : FireBrigadeGotoMAp.keySet())
            gotoMap.put(id, FireBrigadeGotoMAp.get(id));
    }

    public void clearGoToMap() {
        gotoMap.clear();
    }

    public void setBorderBuildings() {
        Thread loader = new Thread() {
            @Override
            public void run() {
                //long tm1 = System.currentTimeMillis();
                borderBuildings = borderFinder.getBordersOf(0.9);
                //long tm2 = System.currentTimeMillis();
                //long tm = tm2 - tm1;
                //int number = getBuildingIDs().size();
//                MrlPersonalData.VIEWER_DATA.setBorderMapBuildings(getSelf().getID(), borderBuildings);
                //System.out.println("done on " + tm + "Miliseconds for " + number + "Buildings.");
                setBorderEntitiesDataLoaded(true);
            }
        };
        loader.start();

    }


    public int getMaxWater() {
        return maxWater;
    }

    public void setMaxWater(int maxWater) {
        this.maxWater = maxWater;
    }

    public int getWaterRefillRate() {
        return waterRefillRate;
    }

    public int getWaterRefillRateInHydrant() {
        return waterRefillRateInHydrant;
    }

    public void setWaterRefillRate(int waterRefillRate) {
        this.waterRefillRate = waterRefillRate;
    }

    public void setWaterRefillRateInHydrant(int waterRefillRate) {
        this.waterRefillRateInHydrant = waterRefillRate;
    }

    public boolean isPrecomputedDataLoaded() {
        return isVisibilityAreaDataLoaded && isBorderEntitiesDataLoaded;
    }

    public boolean isVisibilityAreaDataLoaded() {
        return isVisibilityAreaDataLoaded;
    }

    public void setProcessVisibilityDataLoaded(boolean isPrecomputedDataLoaded) {
        this.isVisibilityAreaDataLoaded = isPrecomputedDataLoaded;
    }

    public boolean isBorderEntitiesDataLoaded() {
        return isBorderEntitiesDataLoaded;
    }

    public void setBorderEntitiesDataLoaded(boolean isBorderEntitesDataLoaded) {
        this.isBorderEntitiesDataLoaded = isBorderEntitesDataLoaded;
    }

    public int getMaxPower() {
        return scenarioInfo.getFireExtinguishMaxSum();
    }


    public ApolloFireClustering getFireClustering() {
        return fireClustering;
    }

    public void setFireClustering(ApolloFireClustering fireClustering) {
        this.fireClustering = fireClustering;
    }

    /** To get fire zone search manager.*/
    public IFireZoneSearch getFireZoneSearch() {
        return this.fireZoneSearch;
    }

    public int getLastCommand() {
        return lastCommand;
    }

}
