package ApolloRescue.module.complex.center;

enum TargetWeight {

	BLOCKED_FIRE_BRIGADE(250),
	REFUGE_ENTRANCE(179),
	FIERY_BUILDING_1(190),
	BLOCKED_AMBULANCE_TEAM(187),
	BUILDING_WITH_HEALTHY_HUMAN(186),
	FIERY_BUILDING_2(185),
	FIERY_BUILDING_3(180),
	BURIED_FIRE_BRIGADE(177),
	BURIED_POLICE_FORCE(175),
	BURIED_AMBULANCE_TEAM(140),
	BUILDING_WITH_DAMAGED_CIVILIAN(110),

	SEEN_AGENT(1000), DEFAULT(1), ;

	private int weight;

    TargetWeight(int weight) {
        this.weight = weight;
    }

	public int getWeight() {
		return weight;
	}
}
