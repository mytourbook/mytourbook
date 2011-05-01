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

public class TrainingManager {

	// keys to identify the hr max formula
	public static final int	HR_MAX_FORMULA_220_AGE	= 0;	//	HRmax = 220 - age              // this is the default formula
	public static final int	HR_MAX_FORMULA_205_8	= 1;	//	HRmax = 205.8 - (0.685 x age)
	public static final int	HR_MAX_FORMULA_206_9	= 2;	//  HRmax = 206.9 - (0.67 x age)
	public static final int	HR_MAX_FORMULA_191_5	= 3;	//  HRmax = 191.5 - (0.007 x age2)
	public static final int	HR_MAX_FORMULA_MANUAL	= 999;

	/**
	 * This sequence is used when selecting the hr max formula for a person in the people pref page.
	 */
	public static String[]	HRMaxFormulaNames		= {
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
	public static int[]		HRMaxFormulaKeys		= {
													//
			HR_MAX_FORMULA_220_AGE,
			HR_MAX_FORMULA_205_8,
			HR_MAX_FORMULA_206_9,
			HR_MAX_FORMULA_191_5,
			HR_MAX_FORMULA_MANUAL,
													//
													};
}
