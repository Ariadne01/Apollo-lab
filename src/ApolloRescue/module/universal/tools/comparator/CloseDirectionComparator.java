package ApolloRescue.module.universal.tools.comparator;

import rescuecore2.misc.geometry.Vector2D;

import java.util.Comparator;

public class CloseDirectionComparator implements Comparator<Vector2D> {
	private Vector2D reference;

	public CloseDirectionComparator(Vector2D referenceDirection) {
		this.reference = referenceDirection.normalised();
	}

	@Override
	public int compare(Vector2D v1, Vector2D v2) {// cos大的夹角小
		
		double cos1 = getAngleCosValue(v1, reference);
		double cos2 = getAngleCosValue(v2, reference);
		if (cos1 > cos2) {
			return -1;
		} else if (cos1 < cos2) {
			return 1;
		} else {
			return 0;
		}
	}

	private double getAngleCosValue(Vector2D v1, Vector2D reference) {
		return reference.normalised().dot(v1.normalised());
	}

}
