/**
 * 
 */
package de.byteholder.geoclipse.tests;


import de.byteholder.geoclipse.util.GeoUtils;
import de.byteholder.gpx.GeoPosition;

/**
 * @author Michael Kanis
 */
public class SimpleDistanceTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GeoPosition munich = new GeoPosition(48.139722, 11.574444);
		GeoPosition gera = new GeoPosition(50.880556, 12.083333);
		
		System.out.println(GeoUtils.distance(munich, gera));
	}

}
