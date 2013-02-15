/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.view;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;

import java.awt.BorderLayout;

import net.tourbook.common.UI;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

public class Map3View extends ViewPart {

	public static final String					ID			= "net.tourbook.map3.Map3View"; //$NON-NLS-1$

	private static final WorldWindowGLCanvas	_wwCanvas	= Map3Manager.getWWCanvas();

	private ActionOpenMap3Properties			_actionOpenMap3Properties;

	private static int							_renderCounter;

	public Map3View() {

	}

	private void addMap3Listener() {

		// Register a rendering listener that's notified when exceptions occur during rendering.
		_wwCanvas.addRenderingListener(new RenderingListener() {

			@Override
			public void stageChanged(final RenderingEvent event) {

				Display.getDefault().asyncExec(new Runnable() {
					public void run() {

						System.out.println(UI.timeStampNano() + " is rendered: " + _renderCounter++);
						// TODO remove SYSTEM.OUT.PRINTLN

					}
				});
			}
		});
	}

	private void createActions() {

		_actionOpenMap3Properties = new ActionOpenMap3Properties();

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionOpenMap3Properties);
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

//		addMap3Listener();
		createActions();

		Map3Manager.setMap3View(this);
	}

	private void createUI(final Composite parent) {

		// set parent griddata, this must be done AFTER the content is created, otherwise it fails !!!
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);

		// build GUI: container(SWT) -> Frame(AWT) -> Panel(AWT) -> WorldWindowGLCanvas(AWT)
		final Composite container = new Composite(parent, SWT.EMBEDDED);
		GridDataFactory.fillDefaults().applyTo(container);
		{
			final java.awt.Frame awtFrame = SWT_AWT.new_Frame(container);
			final java.awt.Panel awtPanel = new java.awt.Panel(new java.awt.BorderLayout());

			awtFrame.add(awtPanel);
			awtPanel.add(_wwCanvas, BorderLayout.CENTER);
		}

		parent.layout();
	}

	@Override
	public void dispose() {

		Map3Manager.setMap3View(null);

		super.dispose();
	}

	@Override
	public void setFocus() {

	}

}
