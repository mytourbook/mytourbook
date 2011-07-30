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

package net.tourbook.ui.views.calendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class CalendarComponents extends Composite {

	private CalendarGraph	_calGraph;

	CalendarComponents(final Composite parent, final int style) {

		// create composite with vertical scrollbars
		super(parent, SWT.NO_BACKGROUND | SWT.V_SCROLL);

		GridData gd;

		// set layout for this composite
		final GridLayout gl = new GridLayout(1, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		setLayout(gl);

		// set layout for the components

//		final CLabel label = new CLabel(this, SWT.FLAT | SWT.CENTER);
//		label.setText("Calendar");
//		label.setBackground(_display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
//		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
//		label.setLayoutData(gd);

		_calGraph = new CalendarGraph(this, SWT.NO_BACKGROUND);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		_calGraph.setLayoutData(gd);

	}

	public CalendarGraph getGraph() {
		return _calGraph;
	}

}
