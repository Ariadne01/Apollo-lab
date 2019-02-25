package ApolloRescue.module.algorithm.samplesearch;

import javolution.util.FastMap;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

public class BreadthFirstSearch implements SearchAlgorithm {


    /**
     * Do a samplesearch from one location to the closest of a set of goals.
     *
     * @param start          The location we start at.
     * @param goal           The goals we want to reach.
     * @param graph          a connectivity graph of all the places in the world
     * @param distanceMatrix A matrix containing the pre-computed distances between each
     *                       two entities in the world.
     * @return The path from start to one of the goals, or null if no path can be found.
     */
    @Override
    public List<EntityID> search(EntityID start, EntityID goal, Graph graph, DistanceInterface distanceMatrix) {
        HashSet<EntityID> goals = new HashSet<EntityID>();
        goals.add(goal);
        return search(start, goals, graph, distanceMatrix);
    }


    /**
     * Do a samplesearch from one location to the closest of a set of goals.
     *
     * @param start          The location we start at.
     * @param goals           The goals we want to reach.
     * @param graph          a connectivity graph of all the places in the world
     * @param distanceMatrix A matrix containing the pre-computed distances between each
     *                       two entities in the world.
     * @return The path from start to one of the goals, or null if no path can be found.
     */
    @Override
    public List<EntityID> search(EntityID start, Collection<EntityID> goals, Graph graph, DistanceInterface distanceMatrix) {
        List<EntityID> open = new LinkedList<>();
        Map<EntityID, EntityID> ancestors = new FastMap<EntityID, EntityID>();
        open.add(start);
        EntityID next;
        boolean found = false;
        ancestors.put(start, start);
        do{
            next = open.remove(0);
            if(isGoal(next, goals)){
                found = true;
                break;
            }
            Collection<EntityID> neighbours = graph.getNeighbors(next);
            if (neighbours.isEmpty()) {
                continue;
            }
            for(EntityID neighbour : neighbours){
                if(isGoal(neighbour, goals)){
                    ancestors.put(neighbour, next);
                    next = neighbour;
                    found = true;
                    break;
                }else {
                    if(!ancestors.containsKey(neighbour)){
                        open.add(neighbour);
                        ancestors.put(neighbour, next);
                    }
                }
            }
        }while (!found && !open.isEmpty());

        if (!found) { // No path
            return null;
        }

        // Walk back from goal to start
        EntityID current = next;
        List<EntityID> path = new LinkedList<EntityID>();
        do {
            path.add(0, current);
            current = ancestors.get(current);//更新current为邻居
            if (current == null) {
                throw new RuntimeException("Found a node with no ancestor! Something is broken.");
            }
        } while (current != start);

        return path;
    }


    private boolean isGoal(EntityID id, Collection<EntityID> collection) {
        return collection.contains(id);
    }
}
