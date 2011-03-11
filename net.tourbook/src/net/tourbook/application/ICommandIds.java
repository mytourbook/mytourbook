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
package net.tourbook.application;

/**
 * Interface defining the application's command IDs. Key bindings can be defined for specific
 * commands. To associate an action with a command, use IAction.setActionDefinitionId(commandId).
 * 
 * @see org.eclipse.jface.action.IAction#setActionDefinitionId(String)
 */
public interface ICommandIds {

	public static final String	CMD_OPENVIEW_IMPORTEDDATA	= "Tourbook.openViewImportedData";	//$NON-NLS-1$
	public static final String	CMD_OPENVIEW_TOURLIST		= "Tourbook.openViewTourList";		//$NON-NLS-1$
	public static final String	CMD_OPENVIEW_YEARMAP		= "Tourbook.openViewYearMap";		//$NON-NLS-1$
	public static final String	CMD_OPENVIEW_TOURCATALOG	= "Tourbook.openViewTourCatalog";	//$NON-NLS-1$
	public static final String	CMD_OPENVIEW_TOURCOMPARER	= "Tourbook.openViewTourCompare";	//$NON-NLS-1$
	public static final String	CMD_OPENVIEW_TOURFINDER		= "Tourbook.openViewTourviewer";	//$NON-NLS-1$
	public static final String	CMD_OPENVIEW_TOURCHART		= "Tourbook.openViewTourChart";	//$NON-NLS-1$
	public static final String	CMD_OPENVIEW_STATISTICS		= "Tourbook.openViewStatistics";	//$NON-NLS-1$

	public static final String	ACTION_ADD_TAG				= "ACTION_ADD_TAG";				//$NON-NLS-1$
	public static final String	ACTION_REMOVE_TAG			= "ACTION_REMOVE_TAG";				//$NON-NLS-1$

}
