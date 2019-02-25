package ApolloRescue.module.complex.center;

import rescuecore2.worldmodel.EntityID;

import java.util.Map;

class ClearTarget {
	private EntityID id;
	private EntityID positionID;
	private int weight;
	private TargetWeight targetWeight;
	private EntityID nearestRoadID;
	private Map<EntityID, Boolean> roadsToMove; // road and its openness
	private int distanceToIt;


	public ClearTarget(EntityID id, EntityID positionID, int weight, TargetWeight targetWeight) {
		this.id = id;
		this.positionID = positionID;
		this.weight = weight;
		this.targetWeight = targetWeight;
	}

	public ClearTarget(EntityID id, int importance) {
		this.id = id;
		this.weight = importance;
	}

	public EntityID getId() {
		return id;
	}

	public EntityID getPositionID() {
		return positionID;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int importance) {
		this.weight = importance;
	}

	public TargetWeight getImportanceType() {
		return targetWeight;
	}

	public EntityID getNearestRoadID() {
		return nearestRoadID;
	}

	public void setNearestRoadID(EntityID nearestRoadID) {
		this.nearestRoadID = nearestRoadID;
	}

	public int getDistanceToIt() {
		return distanceToIt;
	}

	public void setDistanceToIt(int distanceToIt) {
		this.distanceToIt = distanceToIt;
	}

	public Map<EntityID, Boolean> getRoadsToMove() {
		return roadsToMove;
	}

	public void setRoadsToMove(Map<EntityID, Boolean> roadsToMove) {
		this.roadsToMove = roadsToMove;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ClearTarget)) {
			return false;
		}
		ClearTarget target = (ClearTarget) obj;
        return target.getId().equals(getId()) && target.getPositionID().equals(getPositionID())
                && target.getImportanceType().equals(getImportanceType());
    }
}
