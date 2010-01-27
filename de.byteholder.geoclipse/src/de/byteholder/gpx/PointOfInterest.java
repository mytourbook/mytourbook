package de.byteholder.gpx;

import java.util.List;

import org.eclipse.jface.viewers.ISelection;


/**
 * A Point of interest is essentially a {@link Waypoint} that has additional information like a
 * type, category and information, what's near it. It can also contain a value, which zoom level is
 * recommended to show it in the map.
 * 
 * @author Michael Kanis
 */
public class PointOfInterest extends Waypoint implements ISelection {

//	public enum Type {Area, Way, Node};

//	private Type type;

	private String						category;

	private String						info;

	private int							recommendedZoom;

	private List<? extends Waypoint>	nearestPlaces;

	public String getCategory() {
		return category;
	}

	public String getInfo() {
		return info;
	}

	public List<? extends Waypoint> getNearestPlaces() {
		return nearestPlaces;
	}

	public int getRecommendedZoom() {
		return recommendedZoom;
	}

	public boolean isEmpty() {
		return false;
	}

	public void setCategory(final String category) {
		this.category = category;
	}

//	public void setType(Type type) {
//		this.type = type;
//	}
//	
//	public void setType(final String type) {
//		
//		if (Type.Way.toString().toLowerCase().equals(type.toLowerCase())) {
//			this.type = Type.Way;
//		}
//		else if (Type.Node.toString().toLowerCase().equals(type.toLowerCase())) {
//			this.type = Type.Node;
//		}
//		else {
//			throw new IllegalArgumentException("Not a valid type: " + type);
//		}
//	}

	public void setInfo(final String info) {
		this.info = info;
	}

	public void setNearestPlaces(final List<? extends Waypoint> nearestPlaces) {
		this.nearestPlaces = nearestPlaces;
	}

	public void setRecommendedZoom(final int recommendedZoom) {
		this.recommendedZoom = recommendedZoom;
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();
		buf.append(this.getName()).append(" (").append(this.getCategory()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$

		if (nearestPlaces != null && nearestPlaces.size() > 0) {
			buf.append(" near ").append(nearestPlaces.get(0).toString()); //$NON-NLS-1$
		}

		return buf.toString();
	}
}
