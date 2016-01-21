/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

public class FileCollisionBehavior {

	public static final int	ASK					= 0;
	public static final int	DIALOG_IS_CANCELED	= -1;

	public static final int	KEEP				= 10;
	public static final int	KEEP_ALL			= 20;
	public static final int	REPLACE				= 30;
	public static final int	REPLACE_ALL			= 40;

	public int				value				= ASK;
}
