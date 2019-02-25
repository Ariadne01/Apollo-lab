package ApolloRescue.module.universal.entities;

public enum BlockadeValue {
    //means these blockades blocked road or blocked passable edge..
    VERY_IMPORTANT,
    //means these blockades with another blockades blocked road
    IMPORTANT_WITH_HIGH_REPAIR_COST,
    IMPORTANT_WITH_LOW_REPAIR_COST,
    //these blockades do not blocked any route but will slow down agents which want to pass road
    ORNERY,
    //these blockade do not create any problem for passing road
    WORTHLESS,
}