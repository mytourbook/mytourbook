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
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;

public class TrainingManager {

	// keys to identify the hr max formula
	public static final int			HR_MAX_FORMULA_220_AGE	= 0;	//	HRmax = 220 - age              // this is the default formula
	public static final int			HR_MAX_FORMULA_205_8	= 1;	//	HRmax = 205.8 - (0.685 x age)
	public static final int			HR_MAX_FORMULA_206_9	= 2;	//  HRmax = 206.9 - (0.67 x age)
	public static final int			HR_MAX_FORMULA_191_5	= 3;	//  HRmax = 191.5 - (0.007 x age2)
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
										final String nameShortcut) {

		final TourPersonHRZone hrZone = new TourPersonHRZone(person);

		hrZone.setZoneMinValue(minValue);
		hrZone.setZoneMaxValue(maxValue);
		hrZone.setZoneName(zoneName);
		hrZone.setNameShortcut(nameShortcut);

		hrZones.add(hrZone);
	}

	public static Set<TourPersonHRZone> createHrZones(final TourPerson person, final int selectedTemplate) {

		final Set<TourPersonHRZone> hrZones = new HashSet<TourPersonHRZone>();

		if (selectedTemplate == HR_ZONE_TEMPLATE_01) {

			createHrZone(person, hrZones, Integer.MIN_VALUE, 60, //
					Messages.HR_Zone_01_060_Moderate,
					Messages.HR_Zone_01_060_Moderate_Shortcut);

			createHrZone(person, hrZones, 60, 70, //
					Messages.HR_Zone_01_070_FatBurning,
					Messages.HR_Zone_01_070_FatBurning_Shortcut);

			createHrZone(person, hrZones, 70, 80, //
					Messages.HR_Zone_01_080_Aerobic,
					Messages.HR_Zone_01_080_Aerobic_Shortcut);

			createHrZone(person, hrZones, 80, 90, //
					Messages.HR_Zone_01_090_Anaerobic,
					Messages.HR_Zone_01_090_Anaerobic_Shortcut);

			createHrZone(person, hrZones, 90, Integer.MAX_VALUE, //
					Messages.HR_Zone_01_100_Maximum,
					Messages.HR_Zone_01_100_Maximum_Shortcut);

		} else if (selectedTemplate == HR_ZONE_TEMPLATE_02) {

			createHrZone(person, hrZones, Integer.MIN_VALUE, 65, //
					Messages.HR_Zone_02_065_KB,
					Messages.HR_Zone_02_065_KB_Shortcut);

			createHrZone(person, hrZones, 65, 75, //
					Messages.HR_Zone_02_075_GA1,
					Messages.HR_Zone_02_075_GA1_Shortcut);

			createHrZone(person, hrZones, 75, 85, //
					Messages.HR_Zone_02_085_GA2,
					Messages.HR_Zone_02_085_GA2_Shortcut);

			createHrZone(person, hrZones, 85, 95, //
					Messages.HR_Zone_02_095_EB,
					Messages.HR_Zone_02_095_EB_Shortcut);

			createHrZone(person, hrZones, 95, Integer.MAX_VALUE, //
					Messages.HR_Zone_02_095_SB,
					Messages.HR_Zone_02_095_SB_Shortcut);

		}

// #######################################################################################################
//
//		a) Kompensationsbereich (KB): weniger als 65% der Maximal-Leistung
//		- Zur Erholung nach anstrengendem Rennen/Training
//		- ausschließlich Fettverbrennung
//		- Dauer 1-2 Stunden
//
//		b) Grundlagen-Ausdauer 1 (GA1): 65–75% der Maximalleistung
//		- Zum Aufbau der Grundlagenausdauer (besonders wichtig im Winter und Frühjahr)
//		- fast ausschließlich Fettverbrennung
//		- Dauer 5 Stunden und länger
//
//		c) Grundlagen-Ausdauer 2 (GA2): 75–85% der Maximalleistung
//		– Zur Stabilisierung und zum Ausbau der Grundlagenausdauer
//		- Fett- und Kohlehydrate
//		- Dauer 1-2 Stunden
//
//		d) Entwicklungs-Bereich (EB): 85–95% der Maximalleistung
//		– Zur Erhöhung der IANS
//		- Wichtig z.B. für längere Ausreißversuche oder am Berg
//		- Vorwiegend Kohlehydrate
//		- Dauer 5-20 min, aber mehrmals hintereinander
//
//		e) Spitzenbereich (SB): mehr als 95% der Maximalleistung
//		- Sprints, kurze Ausreißversuche. Nur für Leistungssportler!
//		- Fast ausschließlich Kohlehydrate
//		- Dauer: wenige Sekunden bis maximal 5 min

//
// #######################################################################################################
//
//	    Gesundheitszone: 50 bis 60 % der maximalen Herzfrequenz (HFmax) (Faktor 0,5 bis 0,6)
//	    Fettverbrennungszone: 60 bis 70 % der HFmax (Faktor 0,6 bis 0,7)
//	    Aerobe Zone: 70 bis 80 % der HFmax (Faktor 0,7 bis 0,8)
//	    Anaerobe Zone: 80 bis 90 % der HFmax (Faktor 0,8 bis 0,9)
//	    Warnzone: 90 bis 100 % der HFmax (Faktor 0,9 bis 1,0)
//
//
// #######################################################################################################
//
//		Folgende Trainingsbereiche sind die gebräuchlichsten:
//			REKOM 	Regenerations- u. Kompensationsbereich 	< 70% HFmax
//			GA1 	Grundlagenausdauerbereich 1 	70 – 85% HFmax
//			GA2 	Grundlagenausdauerbereich 2 	85 – 95% HFmax
//			WSA 	Wettkampfspezifische Ausdauer 	95 – 100% HFmax
//
//			Weitere Trainingsbereiche:
//			GA1/2 	Zwischenbereich von GA1 und GA2
//			KA 	Kraftausdauerbereich (überschneidet sich mit GA2, anderer Fokus)
//			SA 	Schnelligkeitsausdauerbereich (überschneidet sich mit WSA, anderer Fokus)
//			EB 	Entwicklungsbereich (v.a. Radsport, Inlineskating)
//			SB 	Spitzenbereich (v.a. Radsport)
//			GB 	Grenzbereich (v.a. Skilanglauf)
		return hrZones;
	}
}
