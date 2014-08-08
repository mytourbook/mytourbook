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
package net.tourbook.ui.tourChart.action;


public class ActionSetMarkerImageMenu_DISABLED /* extends Action implements IMenuCreator, ITourSignSetter */{

//	private Menu				_menu;
//
//	private SignMenuManager		_signMenuManager	= new SignMenuManager(this);
//
//	private ITourMarkerUpdater	_tourMarkerUpdater;
//	private TourMarker			_tourMarker;
//
//	public ActionSetMarkerImageMenu(final ITourMarkerUpdater tourMarkerUpdater) {
//
//		super(Messages.Tour_Action_Marker_SetMarkerImage, AS_DROP_DOWN_MENU);
//
//		setMenuCreator(this);
//
//		_tourMarkerUpdater = tourMarkerUpdater;
//
//		createActions();
//	}
//
//	private void createActions() {
//
//	}
//
//	public void dispose() {
//
//		if (_menu != null) {
//			_menu.dispose();
//			_menu = null;
//		}
//	}
//
//	public Menu getMenu(final Control parent) {
//		return null;
//	}
//
//	public Menu getMenu(final Menu parent) {
//
//		dispose();
//
//		_menu = new Menu(parent);
//
//		// Add listener to repopulate the menu each time
//		_menu.addMenuListener(new MenuAdapter() {
//			@Override
//			public void menuShown(final MenuEvent e) {
//
//				// dispose old menu items
//				for (final MenuItem menuItem : ((Menu) e.widget).getItems()) {
//					menuItem.dispose();
//				}
//
//				_signMenuManager.fillSignMenu(_menu);
//			}
//		});
//
//		return _menu;
//	}
//
//	@Override
//	public void removeTourSign() {
//
//		_tourMarker.setTourSign(null);
//		_tourMarkerUpdater.updateModifiedTourMarker(_tourMarker);
//	}
//
//	public void setTourMarker(final TourMarker tourMarker) {
//
//		_tourMarker = tourMarker;
//
//		_signMenuManager.setTourSign(tourMarker.getTourSign());
//	}
//
//	@Override
//	public void setTourSign(final TourSign tourSign) {
//
//		_tourMarker.setTourSign(tourSign);
//		_tourMarkerUpdater.updateModifiedTourMarker(_tourMarker);
//	}

}
