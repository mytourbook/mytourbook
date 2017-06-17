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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.oscim.core.MapPosition;
import org.oscim.map.Map;

import okhttp3.Cache;

public class MapVtmDebugView extends ViewPart {

	public static final String				ID		= "net.tourbook.map.vtm.MapVtmDebugView";	//$NON-NLS-1$

	private static final IDialogSettings	_state	= TourbookPlugin.getState(ID);

	private IPartListener2					_partListener;

	private Label							_lblCacheHits;
	private Label							_lblRequestedTiles;
	private Label							_lblNetworkRequests;

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == MapVtmDebugView.this) {
					MapVtmManager.setDebugView(null);
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == MapVtmDebugView.this) {
					MapVtmManager.setDebugViewVisible(false);
				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {
				MapVtmManager.setDebugView(MapVtmDebugView.this);
			}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == MapVtmDebugView.this) {
					MapVtmManager.setDebugViewVisible(true);
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

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			{
				/*
				 * Http Requests
				 */

				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(label);
				label.setText("Requested Tiles");

				_lblRequestedTiles = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblRequestedTiles);
				_lblRequestedTiles.setText(UI.EMPTY_STRING);
			}
			{
				/*
				 * Cache Hits
				 */

				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(label);
				label.setText("Cache Hits");

				_lblCacheHits = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblCacheHits);
				_lblCacheHits.setText(UI.EMPTY_STRING);
			}
			{
				/*
				 * Network Requests
				 */

				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(label);
				label.setText("Network requests");

				_lblNetworkRequests = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNetworkRequests);
				_lblNetworkRequests.setText(UI.EMPTY_STRING);
			}
		}
	}

	@Override
	public void dispose() {

		getViewSite().getPage().removePartListener(_partListener);

		super.dispose();
	}

	@Override
	public void setFocus() {}

	void updateUI(final Map map, final Cache httpCache) {

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				_lblRequestedTiles.setText(Integer.toString(httpCache.requestCount()));
				_lblCacheHits.setText(Integer.toString(httpCache.hitCount()));
				_lblNetworkRequests.setText(Integer.toString(httpCache.networkCount()));
			}
		});

		final MapPosition mapPos = map.getMapPosition();

		System.out.println(
				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\trender()") //
						+ ("\ttilt: " + mapPos.tilt)
						+ ("\tbearing: " + mapPos.bearing)
						+ ("\tx: " + mapPos.x)
						+ ("\ty: " + mapPos.y)
						+ ("\tscale: " + mapPos.scale)
						+ ("\t")
						+ ("\t")
		//
		);
		// TODO remove SYSTEM.OUT.PRINTLN

	}

}
