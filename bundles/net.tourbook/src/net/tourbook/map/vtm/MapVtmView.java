/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map.vtm;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Frame;

import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class MapVtmView extends ViewPart {

	public static final String				ID		= "net.tourbook.map.vtm.MapVtmView";	//$NON-NLS-1$

	private static final IDialogSettings	_state	= TourbookPlugin.getState(ID);

	private VtmMap							_vtmMap;

	private IPartListener2					_partListener;

	protected boolean						_isPartVisible;

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == MapVtmView.this) {
//					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == MapVtmView.this) {
					_isPartVisible = false;
				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == MapVtmView.this) {

					_isPartVisible = true;

//					if (_lastHiddenSelection != null) {
//
//						onSelectionChanged(_lastHiddenSelection);
//
//						_lastHiddenSelection = null;
//					}
				}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		addPartListener();
	}

	private void createUI(final Composite parent) {

		final Composite swtContainer = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		final Frame awtContainer = SWT_AWT.new_Frame(swtContainer);

		final Canvas awtCanvas = new Canvas();
		awtContainer.setLayout(new BorderLayout());
		awtCanvas.setIgnoreRepaint(true);

		awtContainer.add(awtCanvas);
		awtCanvas.setFocusable(true);
		awtCanvas.requestFocus();

		_vtmMap = new VtmMap(_state);
		_vtmMap.run(awtCanvas);
	}

	@Override
	public void dispose() {

		getViewSite().getPage().removePartListener(_partListener);

		_vtmMap.stop();

		super.dispose();
	}


	@Override
	public void setFocus() {}

}
