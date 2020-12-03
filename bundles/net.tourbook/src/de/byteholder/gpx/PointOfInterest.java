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

   private String category;

   private String info;

//	private int							recommendedZoom	= -1;

   private List<? extends Waypoint> nearestPlaces;

   private String                   _boundingBox;

   /**
    * @return Returns bounding box of this POI or <code>null</code>, when not available.
    *         <p>
    *         e.g.
    *         boundingbox="48.4838981628418,48.5500030517578,9.02030849456787,9.09173774719238"
    */
   public String getBoundingBox() {
      return _boundingBox;
   }

   public String getCategory() {
      return category;
   }

   public String getInfo() {
      return info;
   }

//	/**
//	 * @return Return the recommended zoom level or -1 when the zoom level should not be changed
//	 */
//	public int getRecommendedZoom() {
//		return recommendedZoom;
//	}

   public List<? extends Waypoint> getNearestPlaces() {
      return nearestPlaces;
   }

   @Override
   public boolean isEmpty() {
      return false;
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

   public void setBoundingBox(final String boundingBox) {
      _boundingBox = boundingBox;
   }

   public void setCategory(final String category) {
      this.category = category;
   }

   public void setInfo(final String info) {
      this.info = info;
   }

//	public void setRecommendedZoom(final int recommendedZoom) {
//		this.recommendedZoom = recommendedZoom;
//	}

   public void setNearestPlaces(final List<? extends Waypoint> nearestPlaces) {
      this.nearestPlaces = nearestPlaces;
   }

   @Override
   public String toString() {

      final StringBuilder buf = new StringBuilder();
      buf.append(this.getName()).append(" (").append(this.getCategory()).append(")"); //$NON-NLS-1$ //$NON-NLS-2$

      if ((nearestPlaces != null) && (nearestPlaces.size() > 0)) {
         buf.append(" near ").append(nearestPlaces.get(0).toString()); //$NON-NLS-1$
      }

      return buf.toString();
   }
}
