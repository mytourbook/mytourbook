/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.search;

import net.tourbook.common.util.Util;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

public class ActionHandler_OpenSearchView extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent execEvent) throws ExecutionException {

		boolean isCtrlKey = false;

		final Object event = execEvent.getTrigger();
		if (event instanceof Event) {

			final Event widgetEvent = (Event) event;
			final int stateMask = widgetEvent.stateMask;

			isCtrlKey = (stateMask & SWT.SHIFT) > 0;
		}


		SearchView.setIsForceLinuxView(isCtrlKey);

		Util.showView(SearchView.ID, true);

		return null;
	}

}
