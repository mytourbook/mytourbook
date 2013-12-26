/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.map2.action;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.color.IGradientColors;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.Map2ColorProfile;
import net.tourbook.common.color.Map2GradientColorProvider;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.map.MapColorProvider;
import net.tourbook.map2.view.DialogMap2ColorEditor;
import net.tourbook.map2.view.IMap2ColorUpdater;
import net.tourbook.map3.Messages;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceColors;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;

public class ActionMap2Color extends Action implements IMap2ColorUpdater {

	private MapGraphId			_mapColorId;
	private DialogMap2ColorEditor	_dialogMappingColor;
	private ColorDefinition		_colorDefinition;

	public ActionMap2Color() {

		super(null, AS_PUSH_BUTTON);

		setText(Messages.Map3_Action_TrackColor);
	}

	@Override
	public void applyMapColors(final Map2ColorProfile newMapColor) {

		// update color definition with new color from the color dialog
		_colorDefinition.setNewMapColor(newMapColor);

		GraphColorManager.saveNewColors();

		// force to change the status
		TourbookPlugin.getDefault().getPreferenceStore()//
				.setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());
	}

	/**
	 * Create tour color action, this is done here to separate map2 Messages from map3 Messages.
	 * 
	 * @param map3View
	 * @param colorId
	 * @return
	 */

	@Override
	public void run() {

		final IMapColorProvider mapColorProvider = MapColorProvider.getMap2ColorProvider(_mapColorId);

		if (mapColorProvider instanceof IGradientColors) {

			final Map2GradientColorProvider mapLegendColorProvider = PrefPageAppearanceColors
					.createLegendImageColorProvider();

			_dialogMappingColor = new DialogMap2ColorEditor(
					Display.getCurrent().getActiveShell(),
					mapLegendColorProvider,
					this);

			_colorDefinition = null;

			final GraphColorManager colorManager = GraphColorManager.getInstance();

			switch (_mapColorId) {
			case Altitude:
				_colorDefinition = colorManager.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_ALTITUDE);
				break;
			case Gradient:
				_colorDefinition = colorManager.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_GRADIENT);
				break;
			case Pace:
				_colorDefinition = colorManager.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_PACE);
				break;
			case Pulse:
				_colorDefinition = colorManager.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_HEARTBEAT);
				break;
			case Speed:
				_colorDefinition = colorManager.getGraphColorDefinition(GraphColorManager.PREF_GRAPH_SPEED);
				break;

			default:
				break;
			}

			if (_colorDefinition == null) {
				StatusUtil.logError("Colordefinition is null"); //$NON-NLS-1$
				return;
			}

			// set the color which should be modified in the dialog
			_dialogMappingColor.setLegendColor(_colorDefinition);

			// new colors will be set with applyMapColors
			_dialogMappingColor.open();
		}
	}

	public void setColorId(final MapGraphId mapColorId) {

		_mapColorId = mapColorId;

		setImageDescriptor(UI.getGraphImageDescriptor(mapColorId));
	}

}
