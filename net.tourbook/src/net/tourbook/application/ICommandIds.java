/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.application;

/**
 * Interface defining the application's command IDs. Key bindings can be defined
 * for specific commands. To associate an action with a command, use
 * IAction.setActionDefinitionId(commandId).
 * 
 * @see org.eclipse.jface.action.IAction#setActionDefinitionId(String)
 */
public interface ICommandIds {

	public static final String	CMD_OPENVIEW_IMPORTEDDATA	= "Tourbook.openViewImportedData";
	public static final String	CMD_OPENVIEW_TOURLIST		= "Tourbook.openViewTourList";
	public static final String	CMD_OPENVIEW_YEARMAP		= "Tourbook.openViewYearMap";
	public static final String	CMD_OPENVIEW_TOURMAP		= "Tourbook.openViewTourMap";
	public static final String	CMD_OPENVIEW_TOURCOMPARER	= "Tourbook.openViewTourCompare";
	public static final String	CMD_OPENVIEW_TOURFINDER		= "Tourbook.openViewTourviewer";

}
