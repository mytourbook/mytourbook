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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

// author:  wolfgang
// create:	2007-08-10

public class HandlerUtil {

	/**
	 * Gets the triggered item from the {@link ExecutionEvent} and returns the status
	 * 
	 * @param execEvent
	 * @return Returns <code>true</code> when the item is checked or <code>null</code> when the
	 *         item is not from type {@link MenuItem} or {@link ToolItem}
	 */
	public static Boolean isItemChecked(ExecutionEvent execEvent) {

		Object trigger = execEvent.getTrigger();

		if (trigger instanceof Event) {

			Event event = (Event) trigger;
			Widget widget = event.widget;

			if (widget instanceof MenuItem) {
				return ((MenuItem) widget).getSelection();
			}

			if (widget instanceof ToolItem) {
				return ((ToolItem) widget).getSelection();
			}
		}

		return null;
	}

}
