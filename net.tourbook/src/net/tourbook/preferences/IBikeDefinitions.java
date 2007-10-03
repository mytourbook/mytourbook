/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
 *  
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation version 2 of the License.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA    
 *******************************************************************************/
package net.tourbook.preferences;

public interface IBikeDefinitions {

	/**
	 * <code>
	 
	 Hollandrad													"roadster"
	 Mountain Bike ungefedert									"mtb"
	 Tandem mit Rennlenkern										"tandem"
	 Rennvelo Obenlenker-Haltung								"racetops"
	 Rennvelo Untenlenker-Haltung								"racedrops"
	 Triathlon													"tria"
	 Superman-Haltung (Rennrad-Stundenweltrekord)				"superman"
	 Langliegerad mit Untenlenker Alltagsausstattung			"lwbuss"
	 Kurzliegerad mit Untenlenker Alltagsausstattung			"swbuss"
	 Kurzliegerad mit Obenlenker Rennausstattung				"swbass"
	 Tiefliegerad mit Obenlenker Kreuzotter race				"ko4"
	 Tieflieger+Heckflosse Kreuzotter race						"ko4tailbox"
	 Tieflieger vollverschalt White Hawk (Stundenweltrekord)	"whitehawk"
	 Velomobil verschalt										"questclosed"
	 Handbike 3 Räder, Kurbeln mit Kettenschaltung				"handtrike"
	 
	 * </code>
	 */

	public static String[]	bikeType	= new String[] {
			"Hollandrad", //$NON-NLS-1$
			"Mountain Bike (MTB) ungefedert", //$NON-NLS-1$
			"Tandem mit Rennlenkern", //$NON-NLS-1$
			"Rennrad Obenlenker-Haltung", //$NON-NLS-1$
			"Rennrad Untenlenker-Haltung", //$NON-NLS-1$
			"Rennrad Triathlon", //$NON-NLS-1$
			"Rennrad Superman-Haltung (Stundenweltrekord)", //$NON-NLS-1$
			"Langliegerad mit Untenlenker Alltagsausstattung", //$NON-NLS-1$
			"Kurzliegerad mit Untenlenker Alltagsausstattung", //$NON-NLS-1$
			"Kurzliegerad mit Obenlenker Rennausstattung", //$NON-NLS-1$
			"Tiefliegerad mit Obenlenker Kreuzotter race", //$NON-NLS-1$
			"Tieflieger + Heckflosse Kreuzotter race", //$NON-NLS-1$
			"Tieflieger vollverschalt White Hawk (Stundenweltrekord)", //$NON-NLS-1$
			"Velomobil verschalt", //$NON-NLS-1$
			"Handbike 3 Räder, Kurbeln mit Kettenschaltung" }; //$NON-NLS-1$

	public static String[]	bikeArr		= new String[] {
			"roadster", //$NON-NLS-1$
			"mtb", //$NON-NLS-1$
			"tandem", //$NON-NLS-1$
			"racetops", //$NON-NLS-1$
			"racedrops", //$NON-NLS-1$
			"tria", //$NON-NLS-1$
			"superman", //$NON-NLS-1$
			"lwbuss", //$NON-NLS-1$
			"swbuss", //$NON-NLS-1$
			"swbass", //$NON-NLS-1$
			"ko4", //$NON-NLS-1$
			"ko4tailbox", //$NON-NLS-1$
			"whitehawk", //$NON-NLS-1$
			"questclosed", //$NON-NLS-1$
			"handtrike"				}; //$NON-NLS-1$

	public static float[]	Cw			= new float[] {
			0.9f,
			.75f,
			.33f,
			.78f,
			.57f,
			.505f,
			.45f,
			.79f,
			.63f,
			.55f,
			.48f,
			.405f,
			0,
			0,
			.59f						};

	public static float[]	sin			= new float[] {
			.95f,
			.85f,
			.7f,
			.89f,
			.67f,
			.64f,
			.55f,
			.64f,
			.51f,
			.44f,
			.37f,
			.37f,
			0f,
			0f,
			.55f						};

	public static float[]	CwBike		= new float[] {
			1.6f,
			1.23f,
			1.4f,
			1.2f,
			1.2f,
			1f,
			.75f,
			1.4f,
			1.3f,
			1f,
			.8f,
			.7f,
			.03f,
			.066f,
			1.2f						};

	public static float[]	aFrame		= new float[] {
			.06f,
			.052f,
			.06f,
			.048f,
			.048f,
			.048f,
			.044f,
			.039f,
			.036f,
			.031f,
			.023f,
			.026f,
			1f,
			1f,
			.046f						};

	public static float[]	CATireV		= new float[] {
			1.1f,
			1.1f,
			1.1f,
			1.1f,
			1.1f,
			1.1f,
			.9f,
			.66f,
			.8f,
			.85f,
			.77f,
			.77f,
			.1f,
			.26f,
			.9f						};

	public static float[]	CATireH		= new float[] {
			.9f,
			.9f,
			.9f,
			.9f,
			.9f,
			.7f,
			.7f,
			.9f,
			.80f,
			.84f,
			.49f,
			.3f,
			.13f,
			.16f,
			2f							};

	public static float[]	FV			= new float[] {
			.33f,
			.45f,
			.5f,
			.37f,
			.45f,
			.47f,
			.48f,
			.34f,
			.65f,
			.65f,
			.78f,
			.78f,
			.715f,
			.88f,
			.5f						};

	public static float[]	FH			= new float[] {
			.67f,
			.55f,
			.5f,
			.63f,
			.55f,
			.53f,
			.52f,
			.72f,
			.5f,
			.5f,
			.4f,
			.4f,
			.45f,
			.4f,
			.55f						};

	public static float[]	ks			= new float[] {
			1.04f,
			1.035f,
			1.06f,
			1.03f,
			1.03f,
			1.03f,
			1.03f,
			1.05f,
			1.05f,
			1.05f,
			1.06f,
			1.06f,
			1.07f,
			1.09f,
			1.04f						};

	public static String[]	tyreType	= new String[] {
			"schmaler Rennreifen", //$NON-NLS-1$
			"mittelbreiter Hochdruck-Slick", //$NON-NLS-1$
			"breiter Hochdruck-Slick", //$NON-NLS-1$
			"breiter Tourenreifen (Profil)", //$NON-NLS-1$
			"Rinkowski-Gürtelreifen (breit)", //$NON-NLS-1$
			"Stollenreifen 1.75'"		}; //$NON-NLS-1$

	public static float[]	Cr			= new float[] {
			.006f,
			.0055f,
			.005f,
			.0075f,
			.003f,
			.007f						};

	public static float[]	ATire		= new float[] {
			.021f,
			.031f,
			.042f,
			.048f,
			.042f,
			.055f						};

	/**
	 * weight default values
	 */
	public static float[]	def_mr		= new float[] {
			18,
			12,
			17.8f,
			9.5f,
			9.5f,
			9.5f,
			8,
			18,
			15.5f,
			11.5f,
			11.8f,
			13.5f,
			18,
			32,
			18							};

	/**
	 * front tyre default index into tyreType 
	 */
	public static int[]		i_tireF		= new int[] {
			3,
			5,
			1,
			0,
			0,
			0,
			0,
			1,
			2,
			0,
			0,
			0,
			0,
			0,
			0							};

	/**
	 * rear tyre default index into tyreType 
	 */
	public static int[]		i_tireR		= new int[] {
			3,
			5,
			1,
			0,
			0,
			0,
			0,
			3,
			3,
			0,
			0,
			0,
			0,
			0,
			0							};

}
