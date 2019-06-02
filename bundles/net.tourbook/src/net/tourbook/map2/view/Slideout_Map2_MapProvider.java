/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import de.byteholder.geoclipse.preferences.IMappingPreferences;
import de.byteholder.geoclipse.preferences.PrefPage_Map2_Providers;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * 2D Map provider slideout
 */
public class Slideout_Map2_MapProvider extends ToolbarSlideout implements IColorSelectorListener {

   private static final String   MAP_ACTION_MANAGE_MAP_PROVIDERS = net.tourbook.map2.Messages.Map_Action_ManageMapProviders;

   final static IPreferenceStore _prefStore                      = TourbookPlugin.getPrefStore();
   final private IDialogSettings _state;

   private SelectionAdapter      _defaultState_SelectionListener;

   private ActionOpenPrefDialog  _actionManageMapProviders;
//   private Action                _actionRestoreDefaults;

   private Map2View              _map2View;

   /*
    * UI controls
    */
   private Composite _parent;

   /**
    * @param ownerControl
    * @param toolBar
    * @param map2View
    * @param map2State
    */
   public Slideout_Map2_MapProvider(final Control ownerControl,
                                    final ToolBar toolBar,
                                    final Map2View map2View,
                                    final IDialogSettings map2State) {

      super(ownerControl, toolBar);

      _map2View = map2View;
      _state = map2State;
   }

   @Override
   public void colorDialogOpened(final boolean isAnotherDialogOpened) {
      setIsAnotherDialogOpened(isAnotherDialogOpened);
   }

   private void createActions() {

      {
         _actionManageMapProviders = new ActionOpenPrefDialog(
               MAP_ACTION_MANAGE_MAP_PROVIDERS,
               PrefPage_Map2_Providers.ID);

         _actionManageMapProviders.closeThisTooltip(this);
         _actionManageMapProviders.setShell(_map2View.getMap().getShell());

         // set the currently displayed map provider so that this mp will be selected in the pref page
         _prefStore.setValue(IMappingPreferences.MAP_FACTORY_LAST_SELECTED_MAP_PROVIDER, _map2View.getMap().getMapProvider().getId());
      }

//      {
//         _actionRestoreDefaults = new Action() {
//            @Override
//            public void run() {
//               resetToDefaults();
//            }
//         };
//
//         _actionRestoreDefaults.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_RestoreDefault));
//         _actionRestoreDefaults.setToolTipText(Messages.App_Action_RestoreDefault_Tooltip);
//      }
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      createActions();

      final Composite ui = createUI(parent);

      restoreState();
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         createUI_10_Header(shellContainer);
         createUI_20_MapOptions(shellContainer);
      }

      return shellContainer;
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//         container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * Slideout title
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_Map_Provider_Label_Title);
            MTFont.setBannerFont(label);
            GridDataFactory
                  .fillDefaults()//
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(label);
         }
         {
            /*
             * Actionbar
             */
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
            GridDataFactory.fillDefaults()//
                  .grab(true, false)
                  .align(SWT.END, SWT.BEGINNING)
                  .applyTo(toolbar);

            final ToolBarManager tbm = new ToolBarManager(toolbar);

//            tbm.add(_actionRestoreDefaults);
            tbm.add(_actionManageMapProviders);

            tbm.update(true);
         }
      }
   }

   private void createUI_20_MapOptions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {}
   }

   private void enableControls() {

   }

   private void initUI(final Composite parent) {

      _parent = parent;

      _defaultState_SelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };
   }

   private void onChangeUI() {

      saveState();

      enableControls();

//      _map2View.restoreState_Map2_Options();
   }

   private void resetToDefaults() {

// SET_FORMATTING_OFF

//      _chkIsShowHoveredTour.setSelection(          Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT);
//      _chkIsZoomWithMousePosition.setSelection(    Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION_DEFAULT);

// SET_FORMATTING_ON

      onChangeUI();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

//      _chkIsShowHoveredTour.setSelection(       Util.getStateBoolean(_state,    Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR, Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT));
//      _chkIsZoomWithMousePosition.setSelection( Util.getStateBoolean(_state,    Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION,   Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION_DEFAULT));

// SET_FORMATTING_ON
   }

   private void saveState() {

// SET_FORMATTING_OFF

//      _state.put(Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR, _chkIsShowHoveredTour.getSelection());
//      _state.put(Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION,   _chkIsZoomWithMousePosition.getSelection());

// SET_FORMATTING_ON
   }

}
