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
package net.tourbook.map25;

import java.io.IOException;
import java.text.NumberFormat;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

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
import org.oscim.map.Map;

import de.byteholder.geoclipse.preferences.Messages;
import okhttp3.Cache;

public class Map25DebugView extends ViewPart {

	public static final String				ID		= "net.tourbook.map25.Map25DebugView";	//$NON-NLS-1$

	private static final IDialogSettings	_state	= TourbookPlugin.getState(ID);

	private final static NumberFormat		_nf2	= NumberFormat.getNumberInstance();
	{
		_nf2.setMinimumIntegerDigits(2);
		_nf2.setMaximumFractionDigits(2);
	}

	private IPartListener2	_partListener;

	/*
	 * UI controls
	 */
	private Label			_lblCacheHits;
	private Label			_lblRequestedTiles;
	private Label			_lblNetworkRequests;
	private Label			_lblCacheSize;

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map25DebugView.this) {
					Map25Manager.setDebugView(null);
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map25DebugView.this) {
					Map25Manager.setDebugViewVisible(false);
				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {
				Map25Manager.setDebugView(Map25DebugView.this);
			}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map25DebugView.this) {
					Map25Manager.setDebugViewVisible(true);
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
		GridLayoutFactory
				.swtDefaults()//
				.numColumns(2)
				.spacing(10, 2)
				.applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
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
				 * Network Requests
				 */

				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(label);
				label.setText("Network requests");

				_lblNetworkRequests = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNetworkRequests);
				_lblNetworkRequests.setText(UI.EMPTY_STRING);
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
				 * Cache Size
				 */

				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(label);
				label.setText("Cache Size");

				_lblCacheSize = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblCacheSize);
				_lblCacheSize.setText(UI.EMPTY_STRING);
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

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\tupdateUI"));
//		// TODO remove SYSTEM.OUT.PRINTLN

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				try {

					_lblRequestedTiles.setText(Integer.toString(httpCache.requestCount()));
					_lblNetworkRequests.setText(Integer.toString(httpCache.networkCount()));

					_lblCacheHits.setText(Integer.toString(httpCache.hitCount()));
					_lblCacheSize.setText(_nf2.format(//
							(float) httpCache.size() / 1024 / 1024) + Messages.prefPage_cache_MByte);

				} catch (final IOException e) {
					StatusUtil.log(e);
				}
			}
		});

//		final MapPosition mapPos = map.getMapPosition();
//		System.out.println(
//				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\trender()") //
//						+ ("\ttilt: " + mapPos.tilt)
//						+ ("\tbearing: " + mapPos.bearing)
//						+ ("\tx: " + mapPos.x)
//						+ ("\ty: " + mapPos.y)
//						+ ("\tscale: " + mapPos.scale)
//						+ ("\t")
//						+ ("\t")
//		//
//		);
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

}
