/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.mapping;

public class LegendColor {

	public static final int	COLOR_RED	= 1;
	public static final int	COLOR_GREEN	= 2;
	public static final int	COLOR_BLUE	= 3;

	public int				maxColor1	= 255;
	public int				maxColor2	= 255;
	public int				maxColor3	= 255;

	/*
	 * set default values to prevent a division by 0
	 */
	public int				minValue	= 0;
	public int				lowValue	= 1;
	public int				midValue	= 2;
	public int				highValue	= 3;
	public int				maxValue	= 4;

	/**
	 * min and max value is painted black when {@link #dimmFactor}==1, a value below 1 will dimm
	 * the color
	 */
	public float			dimmFactor	= 1.0F;

	/*
	 * defines which color (red/green/blue) should be used for color1/2/3
	 */
	public int				color1		= COLOR_RED;
	public int				color2		= COLOR_GREEN;
	public int				color3		= COLOR_BLUE;

}
