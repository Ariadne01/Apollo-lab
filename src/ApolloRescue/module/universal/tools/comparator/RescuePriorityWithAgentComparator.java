//package ApolloRescue.module.universal.tools.comparator;
//
//import apollo.agent.at.selector.RescueTarget;
//import rescuecore2.standard.entities.*;
//
//import java.util.Comparator;
//
///**
// * Agent优先 & 距离
// *
// * @author Langley
// *
// */
//public class RescuePriorityWithAgentComparator implements Comparator<RescueTarget> {
//
//	private StandardEntity reference;
//	private StandardWorldModel world;
//
//	/**
//	 * Create a DeathTimeSorter.
//	 *
//	 * @param reference
//	 *            The reference point to measure deathTime from.
//	 * @param world
//	 *            The world model.
//	 */
//	public RescuePriorityWithAgentComparator(StandardEntity reference, StandardWorldModel world) {
//		this.reference = reference;
//		this.world = world;
//	}
//
//	@Override
//	public int compare(RescueTarget ta1, RescueTarget ta2) {
//		double time1 = ta1.getDeathTimeBasedWeight();
//		double time2 = ta2.getDeathTimeBasedWeight();
//		if (time1 > time2) {
//			return 1;
//		}
//		if (time1 < time2) {
//			return -1;
//		} else {
//			return 0;
//		}
//	}
//
//	private boolean isAgent(Human human) {
//		if (human instanceof PoliceForce) {
//			return true;
//		}
//		if (human instanceof FireBrigade) {
//			return true;
//		}
//		if (human instanceof AmbulanceTeam) {
//			return true;
//		}
//		return false;
//	}
//
//}
