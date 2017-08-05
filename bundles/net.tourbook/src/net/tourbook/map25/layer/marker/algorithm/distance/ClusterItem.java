/*
 * Original: com.google.maps.android.clustering.ClusterItem
 */
package net.tourbook.map25.layer.marker.algorithm.distance;

import com.google.android.gms.maps.model.LatLng;

/**
 * ClusterItem represents a marker on the map.
 */
public interface ClusterItem {

    /**
     * The position of this marker. This must always return the same value.
     */
    LatLng getPosition();

    /**
     * The description of this marker.
     */
    String getSnippet();

    /**
     * The title of this marker.
     */
    String getTitle();
}