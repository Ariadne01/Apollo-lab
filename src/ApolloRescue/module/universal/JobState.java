package ApolloRescue.module.universal;

public enum JobState {

    //common state
    Exploring,
    //fb
    ExtinguishWhenStuck,
    GoingToRefugeToRefillWater,
    TakingCivilianToRefuge,
    GoingToBlockedRoad,
    MovingToFire,
    MovingToFireArea,
    MovingToRefuge,
    CheckFireZone,
    //at
    MovingToHuman,
    GoingToGoalToClearPath,
    GoingToStartToClearPath,
    MovingToPartition,
    MovingToMainRoad,
    MovingToBlockAtCurrentLocation,
    MovingToBlockedRoad,
    GoingToFirePartition,
    MovingToClearPath,
    MovingToGoal,
    MoveOutOfThePlace,
    MoveToBuildingCenter,
    MoveAwayFromGasStation,

    MoveBack,//
    MoveToNextRefuge,//
    RandomMove,

    WillClearPath, ClearingBlock, ExploringRandom,
    LoadingHuman, UnloadingHuman, RescuingHuman,

    WillSaveHuman, Extinguishing,

    FillingWater, Waiting,  Inactive,
    SavingMyself,

    ExtinguishInRefuge,
    //pf
    SlowMovingInPath,
    ClearingPath, SavedByOtherAgent, HumanIsHelpless,
    GettingCloserToBlock,
    ClearingBlocksToPath, ClearingAllBlocksAtLocation,
    ClearingCurrentRoad, BuriedResting, StuckResting,
    ClearingRefugeEntrance,
    ClearingAnnoyingBlock,
    MovingToClearPoint,
    Rest,
    Ouch,   //civilian
    MovingToHydrant,
    MoveToNextArea, //wzp
    MoveToPoint,	//wzp

    //search condition
    SearchCancel,
    SearchFinish,
    Searching,

}
