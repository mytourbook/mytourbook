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

import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;

import java.util.ArrayList;

import net.tourbook.map3.Activator;

import org.eclipse.jface.dialogs.IDialogSettings;

public class Map3Manager {

	static final int							LAYER_ID	= 1;
	static final int							CONTROLS_ID	= 2;

	private static final IDialogSettings		_state		= Activator.getDefault()//
																	.getDialogSettingsSection("PhotoDirectoryView");	//$NON-NLS-1$

	private static ArrayList<TVICategory>		_rootCategories;

	private static final WorldWindowGLCanvas	_wwCanvas	= new WorldWindowGLCanvas();

	/**
	 * Root item for the {@link Map3PropertiesView}.
	 */
	private static TVIRoot						_rootItem	= new TVIRoot();

	/**
	 * Instance of {@link Map3View} or <code>null</code> when map view is not created.
	 */
	private static Map3View						_map3View;

	static {
		initWorldWindLayerModel();
	}

	public static Map3View getMap3View() {
		return _map3View;
	}

	static ArrayList<TVICategory> getRootCategories(final TVIRoot rootItem) {

		if (_rootCategories != null) {
			return _rootCategories;
		}

		final ArrayList<TVICategory> _rootCategories = new ArrayList<TVICategory>();

		_rootCategories.add(new TVICategory(rootItem, LAYER_ID, "Layer"));
		_rootCategories.add(new TVICategory(rootItem, CONTROLS_ID, "Controls"));

		return _rootCategories;
	}

	static TVIRoot getRootItem() {
		return _rootItem;
	}

	static WorldWindowGLCanvas getWWCanvas() {
		return _wwCanvas;
	}

	/*
	 * Initialize WW model with default layers
	 */
	static void initWorldWindLayerModel() {

		// create default model
		final Model model = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);

		// restore map3 state
//		restoreState();

		model.setShowWireframeExterior(false);
		model.setShowWireframeInterior(false);
		model.setShowTessellationBoundingVolumes(false);

		_wwCanvas.setModel(model);
	}

	public static void saveState() {
		// TODO Auto-generated method stub
		
	}

	static void setMap3View(final Map3View map3View) {
		_map3View = map3View;
	}

}
