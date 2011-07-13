/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.training;

import java.util.HashSet;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.data.HrZoneContext;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;

import org.eclipse.swt.graphics.RGB;

public class TrainingManager {

//	private static final RGB		RGB_ZONE_1				= new RGB(0x95, 0xe0, 0xff);
//	private static final RGB		RGB_ZONE_2				= new RGB(0xD3, 0xFF, 0x95);
//	private static final RGB		RGB_ZONE_3				= new RGB(0xff, 0xfa, 0x95);
//	private static final RGB		RGB_ZONE_4				= new RGB(0xFF, 0xc9, 0x95);
//	private static final RGB		RGB_ZONE_5				= new RGB(0xff, 0x95, 0x95);
//
//	private static final RGB		RGB_ZONE_1				= new RGB(0x3e, 0xb9, 0xff);
//	private static final RGB		RGB_ZONE_2				= new RGB(0x96, 0xe0, 0x26);
//	private static final RGB		RGB_ZONE_3				= new RGB(0xff, 0xe2, 0x00);
//	private static final RGB		RGB_ZONE_4				= new RGB(0xff, 0x9e, 0x20);
//	private static final RGB		RGB_ZONE_5				= new RGB(0xff, 0x40, 0x20);

	private static final RGB		RGB_ZONE_1				= new RGB(0xd4, 0xff, 0x91);
	private static final RGB		RGB_ZONE_2				= new RGB(0x96, 0xe0, 0x26);
	private static final RGB		RGB_ZONE_3				= new RGB(0xff, 0xcd, 0x66);
	private static final RGB		RGB_ZONE_4				= new RGB(0xff, 0x90, 0x01);
	private static final RGB		RGB_ZONE_5				= new RGB(0xff, 0x40, 0x20);

	// keys to identify the hr max formula
	public static final int			HR_MAX_FORMULA_220_AGE	= 0;							//	HRmax = 220 - age              // this is the default formula
	public static final int			HR_MAX_FORMULA_205_8	= 1;							//	HRmax = 205.8 - (0.685 x age)
	public static final int			HR_MAX_FORMULA_206_9	= 2;							//  HRmax = 206.9 - (0.67 x age)
	public static final int			HR_MAX_FORMULA_191_5	= 3;							//  HRmax = 191.5 - (0.007 x age2)
	public static final int			HR_MAX_NOT_COMPUTED		= 999;

	/**
	 * The sequence of the template text must correspond to {@link #HR_ZONE_TEMPLATE_00}...
	 */
	public static final String[]	HR_ZONE_TEMPLATES		= {
															//
			Messages.HR_Zone_Template_Select,
			Messages.HR_Zone_Template_01_Moderate60Max100,
			Messages.HR_Zone_Template_02_GA1GA2,
															//
															};

	public static final int			HR_ZONE_TEMPLATE_00		= 0;
	public static final int			HR_ZONE_TEMPLATE_01		= 1;
	public static final int			HR_ZONE_TEMPLATE_02		= 2;

	/**
	 * This sequence is used when selecting the hr max formula for a person in the people pref page.
	 */
	public static String[]			HRMaxFormulaNames		= {
															//
			Messages.HRMaxFormula_Name_HRmax_220_age, //	HRmax = 220 - age
			Messages.HRMaxFormula_Name_HRmax_205_8, // 		HRmax = 205.8 - (0.685 x age)
			Messages.HRMaxFormula_Name_HRmax_206_9, //		HRmax = 206.9 - (0.67 x age)
			Messages.HRMaxFormula_Name_HRmax_191_5, //		HRmax = 191.5 - (0.007 x age2)
			Messages.HRMaxFormula_Name_Manual,
															//
															};
	/**
	 * These keys must be in the same sequence as {@link #HRMaxFormulaNames}
	 */
	public static int[]				HRMaxFormulaKeys		= {
															//
			HR_MAX_FORMULA_220_AGE,
			HR_MAX_FORMULA_205_8,
			HR_MAX_FORMULA_206_9,
			HR_MAX_FORMULA_191_5,
			HR_MAX_NOT_COMPUTED, // this key must be the last, it's also the last item in a combo box
									//
															};

	private static void createHrZone(	final TourPerson person,
										final Set<TourPersonHRZone> hrZones,
										final int minValue,
										final int maxValue,
										final String zoneName,
										final String nameShortcut,
										final RGB rgb) {

		final TourPersonHRZone hrZone = new TourPersonHRZone(person);

		hrZone.setZoneMinValue(minValue);
		hrZone.setZoneMaxValue(maxValue);
		hrZone.setZoneName(zoneName);
		hrZone.setNameShortcut(nameShortcut);
		hrZone.setColor(rgb);

		hrZones.add(hrZone);
	}

	/**
	 * These two values in a set cannot be deleted, they will occure always.
	 * 
	 * @param person
	 * @param selectedTemplate
	 * @return
	 */
	public static Set<TourPersonHRZone> createHrZones(final TourPerson person, final int selectedTemplate) {

		final Set<TourPersonHRZone> hrZones = new HashSet<TourPersonHRZone>();

		if (selectedTemplate == HR_ZONE_TEMPLATE_01) {

			createHrZone(person, hrZones, 0, 59, //
					Messages.HR_Zone_01_060_Moderate,
					Messages.HR_Zone_01_060_Moderate_Shortcut,
					RGB_ZONE_1);

			createHrZone(person, hrZones, 60, 69, //
					Messages.HR_Zone_01_070_FatBurning,
					Messages.HR_Zone_01_070_FatBurning_Shortcut,
					RGB_ZONE_2);

			createHrZone(person, hrZones, 70, 79, //
					Messages.HR_Zone_01_080_Aerobic,
					Messages.HR_Zone_01_080_Aerobic_Shortcut,
					RGB_ZONE_3);

			createHrZone(person, hrZones, 80, 89, //
					Messages.HR_Zone_01_090_Anaerobic,
					Messages.HR_Zone_01_090_Anaerobic_Shortcut,
					RGB_ZONE_4);

			createHrZone(person, hrZones, 90, Integer.MAX_VALUE, //
					Messages.HR_Zone_01_100_Maximum,
					Messages.HR_Zone_01_100_Maximum_Shortcut,
					RGB_ZONE_5);

		} else if (selectedTemplate == HR_ZONE_TEMPLATE_02) {

			createHrZone(person, hrZones, 0, 64, //
					Messages.HR_Zone_02_065_KB,
					Messages.HR_Zone_02_065_KB_Shortcut,
					RGB_ZONE_1);

			createHrZone(person, hrZones, 65, 74, //
					Messages.HR_Zone_02_075_GA1,
					Messages.HR_Zone_02_075_GA1_Shortcut,
					RGB_ZONE_2);

			createHrZone(person, hrZones, 75, 84, //
					Messages.HR_Zone_02_085_GA2,
					Messages.HR_Zone_02_085_GA2_Shortcut,
					RGB_ZONE_3);

			createHrZone(person, hrZones, 85, 94, //
					Messages.HR_Zone_02_095_EB,
					Messages.HR_Zone_02_095_EB_Shortcut,
					RGB_ZONE_4);

			createHrZone(person, hrZones, 95, Integer.MAX_VALUE, //
					Messages.HR_Zone_02_095_SB,
					Messages.HR_Zone_02_095_SB_Shortcut,
					RGB_ZONE_5);

		}

		return hrZones;
	}

	public static int getZoneIndex(final HrZoneContext hrZoneContext, final int pulse) {

		final int[] zoneMinBpm = hrZoneContext.zoneMinBpm;
		int zoneIndex = 0;

		for (int checkedIndex = 0; checkedIndex < zoneMinBpm.length; checkedIndex++) {

			if (zoneMinBpm[checkedIndex] > pulse) {
				return zoneIndex;
			}

			zoneIndex = checkedIndex;
		}

		return zoneIndex;
	}

	/**
	 * Checks if all necessary data are available which are needed to draw the HR zones.
	 * 
	 * @param tourData
	 * @param currentPerson
	 * @return Returns <code>true</code> when HR zones are available for the
	 */
	public static boolean isRequiredHrZoneDataAvailable(final TourData tourData) {

		// check tour
		if (tourData != null) {

			// check pulse
			final int[] pulseSerie = tourData.pulseSerie;
			final boolean isPulse = pulseSerie != null && pulseSerie.length > 0;
			if (isPulse) {

				// check person
				final TourPerson tourPerson = tourData.getTourPerson();
				if (tourPerson != null) {

					// check hr zones
					final Set<TourPersonHRZone> personHrZones = tourPerson.getHrZones();
					final boolean isHrZones = personHrZones != null && personHrZones.size() > 0;

					return isHrZones;
				}
			}
		}

		return false;
	}
}
