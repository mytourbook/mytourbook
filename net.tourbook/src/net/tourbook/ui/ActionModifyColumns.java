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
package net.tourbook.ui;

import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

public class ActionModifyColumns extends Action {

	private ColumnManager	fColumnManager;

	public ActionModifyColumns(ColumnManager columnManager) {

		fColumnManager = columnManager;

		setText("Customize columns");
		setToolTipText("Set the columns which are displayed in the table");

		setImageDescriptor(TourbookPlugin.getImageDescriptor("configure_columns.gif"));
	}

	public void run() {
		fColumnManager.openColumnDialog();
	}

}
