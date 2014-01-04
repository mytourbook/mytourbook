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
package net.tourbook.map3.action;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.Map3ColorManager;
import net.tourbook.common.color.Map3GradientColorProvider;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.map.MapColorProvider;
import net.tourbook.map3.Messages;
import net.tourbook.map3.ui.DialogMap3ColorEditor;
import net.tourbook.map3.ui.IMap3ColorUpdater;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;

public class ActionMap3Color extends Action implements IMap3ColorUpdater {

	private MapGraphId	_mapColorId;

	public ActionMap3Color() {

		super(null, AS_PUSH_BUTTON);

		setText(Messages.Map3_Action_TrackColor);
	}

	@Override
	public void applyMapColors(	final Map3GradientColorProvider originalColorProvider,
								final Map3GradientColorProvider modifiedColorProvider,
								final boolean isNewColorProvider) {

		// ignore isNewColorProvider because the current profile is edited

		final MapGraphId modifiedGraphId = modifiedColorProvider.getGraphId();

		// apply colors only to gradient color provider
		final IMapColorProvider activeColorProvider = MapColorProvider.getActiveMap3ColorProvider(modifiedGraphId);

		if (activeColorProvider instanceof Map3GradientColorProvider) {

			// update active color provider
			final Map3GradientColorProvider activeMap3ColorProvider = (Map3GradientColorProvider) activeColorProvider;
			activeMap3ColorProvider.setColorProfile(modifiedColorProvider.getColorProfile());

			// update model
			Map3ColorManager.replaceColorProvider(originalColorProvider, modifiedColorProvider);

			Map3ColorManager.saveColors();

			// fire event that color has changed
			TourbookPlugin.getPrefStore().setValue(ITourbookPreferences.MAP3_COLOR_IS_MODIFIED, Math.random());
		}
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

		final IMapColorProvider colorProvider = MapColorProvider.getActiveMap3ColorProvider(_mapColorId);

		if (colorProvider instanceof Map3GradientColorProvider) {

			final Map3GradientColorProvider map3ColorProvider = (Map3GradientColorProvider) colorProvider;

			new DialogMap3ColorEditor(//
					Display.getCurrent().getActiveShell(),
					map3ColorProvider,
					this,
					false).open();
		}
	}

	public void setColorId(final MapGraphId mapColorId) {

		_mapColorId = mapColorId;

		setImageDescriptor(UI.getGraphImageDescriptor(mapColorId));
	}

}
