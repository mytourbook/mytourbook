/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views;

import java.util.ArrayList;

import net.tourbook.data.TourData;
import net.tourbook.tour.TourInfoUI;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.util.ITourToolTipProvider;
import net.tourbook.util.TourToolTip;

import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

/**
 * Implemented custom tooltip similar like
 * {@link org.eclipse.ui.internal.dialogs.CustomizePerspectiveDialog} and
 * {@link org.eclipse.jface.viewers.ColumnViewerToolTipSupport}
 */
@SuppressWarnings("restriction")
public abstract class ColumnViewerTourInfoToolTip extends ToolTip implements ITourProvider, ITourToolTipProvider {

	private final TourInfoUI	_tourInfoUI	= new TourInfoUI();

	private Long				_tourId;
	private TourData			_tourData;

	public ColumnViewerTourInfoToolTip(final Control control, final int style) {

		super(control, style, false);

		// disable shifting
//		setShift(new Point(0, 0));

		setHideOnMouseDown(false);

//		setPopupDelay(1000);
	}

	@Override
	public void afterHideToolTip() {
		// not used
	}

	@Override
	protected void afterHideToolTip(final Event event) {
		super.afterHideToolTip(event);
	}

	@Override
	public Composite createToolTipContentArea(final Event event, final Composite parent) {

		Composite container;

		if (_tourId != -1) {
			// first get data from the tour id when it is set
			_tourData = TourManager.getInstance().getTourData(_tourId);
		}

		if (_tourData == null) {

			// there are no data available

			container = _tourInfoUI.createUINoData(parent);

			// allow the actions to be selected
			setHideOnMouseDown(true);

		} else {

			// tour data is available

			container = _tourInfoUI.createContentArea(parent, _tourData, this, this);

			_tourInfoUI.setActionsEnabled(true);

			// allow the actions to be selected
			setHideOnMouseDown(false);
		}

		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				_tourInfoUI.dispose();
			}
		});

		return container;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		if (_tourData == null) {
			return null;
		}

		final ArrayList<TourData> list = new ArrayList<TourData>();
		list.add(_tourData);

		return list;
	}

	@Override
	public void paint(final GC gc, final Rectangle clientArea) {
		// not used
	}

	@Override
	public boolean setHoveredLocation(final int x, final int y) {
		// not used
		return false;
	}

	protected void setTourId(final Long tourId) {
		_tourId = tourId;
	}

//	@Override
//	protected Composite createToolTipContentArea(final Event event, final Composite parent) {
//
//		final Composite composite = new Composite(parent, SWT.NONE);
//		composite.setLayout(new RowLayout(SWT.VERTICAL));
//		{
//
//			final DateTime calendar = new DateTime(composite, SWT.CALENDAR);
//			calendar.setEnabled(false);
//			calendar.setSize(100, 100);
//			composite.pack();
//
//			if (_tourId != null) {
//
//				final Text text = new Text(composite, SWT.SINGLE);
//				text.setText(Long.toString(_tourId));
//				text.setSize(100, 60);
//			}
//		}
//
//		return composite;
//	}

	@Override
	public void setTourToolTip(final TourToolTip tourToolTip) {
		// not used
	}
}
