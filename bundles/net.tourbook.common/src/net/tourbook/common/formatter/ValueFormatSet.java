/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.common.formatter;

public class ValueFormatSet {

	public static ValueFormat[]	Number	= new ValueFormat[] {
										//
			ValueFormat.NUMBER_1_0,
			ValueFormat.NUMBER_1_1,
			ValueFormat.NUMBER_1_2,
			ValueFormat.NUMBER_1_3		};

	public static ValueFormat[]	Time	= new ValueFormat[] {
										//
			ValueFormat.TIME_HH,
			ValueFormat.TIME_HH_MM,
			ValueFormat.TIME_HH_MM_SS	};

}
