/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.mapping;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class ActionDimMap extends Action implements IMenuCreator {

	private static final int		DIM_LEVELS			= 11;	// 0...100%

	private static final String		PERCENT				= "%"; //$NON-NLS-1$

	private Menu					fMenu;

	private int						fSelectedDimLevel	= 0;

	private MappingView				fMapView;

	private ArrayList<ActionDim>	fDimActions;

	private class ActionDim extends Action {

		private static final String	OSX_SPACER_STRING	= " ";	//$NON-NLS-1$

		private int					fActionDimLevel;

		public ActionDim(final int dimLevel, final String label) {

			// add space before the label otherwise OSX will not display the menu item,
			super(OSX_SPACER_STRING + NLS.bind(label, Integer.toString(dimLevel)), AS_RADIO_BUTTON);

			fActionDimLevel = dimLevel;
		}

		@Override
		public void run() {

			setDimLevel(fActionDimLevel);

			fMapView.actionDimMap(fSelectedDimLevel);
		}
	}

	public ActionDimMap(final MappingView mapView) {

		super(Messages.map_action_dim_map, AS_DROP_DOWN_MENU);

		setMenuCreator(this);

		fMapView = mapView;

		ActionDim dimAction;
		fDimActions = new ArrayList<ActionDim>();
		for (int actionIndex = 0; actionIndex < DIM_LEVELS; actionIndex++) {

			if (actionIndex == 0) {

				// action: disable dimming

				dimAction = new ActionDim(0xFF, Messages.map_action_dim_map_disabled);
				dimAction.setChecked(true);

			} else {

				/*
				 * convert dim level from 100% to 255
				 */
				float dimLevel = ((float) 0xFF / (DIM_LEVELS - 1)) * actionIndex;
				dimLevel = Math.abs(dimLevel - 0xFF);

				final int dimLevelText = (int) (Math.abs(dimLevel - 0xFF) / 0xFF * 100);

				dimAction = new ActionDim((int) dimLevel, dimLevelText + PERCENT);
			}

			fDimActions.add(dimAction);
		}
	}

	private void addActionToMenu(final Action action) {
		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(fMenu, -1);
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	public int getDimLevel() {
		return fSelectedDimLevel;
	}

	public Menu getMenu(final Control parent) {
		return null;
	}

	public Menu getMenu(final Menu parent) {

		fMenu = new Menu(parent);

		for (final ActionDim dimAction : fDimActions) {
			addActionToMenu(dimAction);
		}

		return fMenu;
	}

	/**
	 * Sets the dim level and selects the corresponding dim action
	 * 
	 * @param dimLevel
	 * @return Return the dim level which was set
	 */
	public int setDimLevel(final int dimLevel) {

		fSelectedDimLevel = dimLevel;
		boolean isDimAvail = false;

		/*
		 * check selected dim level and uncheck others
		 */
		ActionDim dimLevelRoughly = null;

		for (final ActionDim dimAction : fDimActions) {
			final int actionDimLevel = dimAction.fActionDimLevel;
			if (actionDimLevel == fSelectedDimLevel) {
				dimAction.setChecked(true);
				isDimAvail = true;
			} else {
				if (dimAction.isChecked()) {
					dimAction.setChecked(false);
				}

				/*
				 * when the dim level is not exactly like the action dim level, find the nearest dim
				 * level action
				 */
				if (fSelectedDimLevel < actionDimLevel) {
					dimLevelRoughly = dimAction;
				}
			}
		}

		if (isDimAvail == false) {

			if (dimLevelRoughly != null) {
				fSelectedDimLevel = dimLevelRoughly.fActionDimLevel;
				dimLevelRoughly.setChecked(true);
			} else {
				// set default value
				fSelectedDimLevel = 0;
				fDimActions.get(0).setChecked(true);
			}
		}

		return fSelectedDimLevel;
	}

}
