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
package net.tourbook.map3.ui;

import java.util.ArrayList;

import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map3.view.Map3Manager;
import net.tourbook.map3.view.TVIMap3Layer;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

public class SlideoutMap3Layer extends ToolbarSlideout {

	private Composite	_shellContainer;

	private Map3LayerUI	_layerUI;

	public SlideoutMap3Layer(final Control ownerControl, final ToolBar toolBar) {
		super(ownerControl, toolBar);
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		Map3Manager.setMap3LayerSlideout(this);

		return createUI(parent);
	}

	private Composite createUI(final Composite parent) {

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()//
				.margins(0, 0)
				.spacing(0, 0)
				.applyTo(_shellContainer);
		{
			_layerUI = new Map3LayerUI(_shellContainer);
		}

		return _shellContainer;
	}

	@Override
	protected void onDispose() {

		Map3Manager.setMap3LayerSlideout(null);
	}

	public void setLayerVisible(final TVIMap3Layer tviLayer, final boolean isVisible) {

		_layerUI.setLayerVisible(tviLayer, isVisible);
	}

	public void setLayerVisible_TourTrack(final TVIMap3Layer tviLayer, final boolean isTrackVisible) {

		_layerUI.setLayerVisible_TourTrack(tviLayer, isTrackVisible);
	}

	public void updateUI_NewLayer(final ArrayList<TVIMap3Layer> insertedLayers) {

		_layerUI.updateUI_NewLayer(insertedLayers);
	}

}
