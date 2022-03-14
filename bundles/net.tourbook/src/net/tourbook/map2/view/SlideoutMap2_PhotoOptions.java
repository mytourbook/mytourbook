/*******************************************************************************
 * Copyright (C) 2020, 2021 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.Photo;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for 2D map track options
 */
public class SlideoutMap2_PhotoOptions extends ToolbarSlideout implements IActionResetToDefault {

   private static final String   MAP_ACTION_EDIT2D_MAP_PREFERENCES = net.tourbook.map2.Messages.Map_Action_Edit2DMapPreferences;

   private static final String   STATE_PHOTO_PROPERTIES_IMAGE_SIZE = "STATE_PHOTO_PROPERTIES_IMAGE_SIZE";                       //$NON-NLS-1$

   private static final int      MIN_IMAGE_WIDTH                   = 10;

   /**
    * This value is small because a map do not yet load large images !!!
    */
   private static final int      MAX_IMAGE_WIDTH                   = 200;

   final static IPreferenceStore _prefStore                        = TourbookPlugin.getPrefStore();
   private IDialogSettings       _state;

   private ActionResetToDefaults _actionRestoreDefaults;

   private Map2View              _map2View;

   private int                   _imageSize;

   /*
    * UI controls
    */
   private Composite _parent;

   private Spinner   _spinnerImageSize;

   /**
    * @param ownerControl
    * @param toolBar
    * @param map2View
    * @param map2State
    */
   public SlideoutMap2_PhotoOptions(final Control ownerControl,
                                    final ToolBar toolBar,
                                    final Map2View map2View,
                                    final IDialogSettings map2State) {

      super(ownerControl, toolBar);

      _map2View = map2View;
      _state = map2State;

      restoreState_BeforeUI();
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      createActions();

      final Composite ui = createUI(parent);

      enableControls();

      updateUI();

      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
      UI.setChildColors(parent,
            colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND),
            colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND));

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         createUI_10_Header(shellContainer);
         createUI_20_ImageSize(shellContainer);
      }

      return shellContainer;
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * Slideout title
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_Map_PhotoOptions_Label_Title);
            MTFont.setBannerFont(label);
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(label);
         }
         {
            /*
             * Actionbar
             */
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.BEGINNING)
                  .applyTo(toolbar);

            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_actionRestoreDefaults);

            tbm.update(true);
         }
      }
   }

   private void createUI_20_ImageSize(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * label: displayed photos
             */
            final Label label = new Label(container, SWT.NO_FOCUS);
            label.setText(Messages.Photo_Properties_Label_Size);
            label.setToolTipText(Messages.Photo_Properties_Label_ThumbnailSize_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);
         }
         {
            /*
             * spinner: size
             */
            _spinnerImageSize = new Spinner(container, SWT.BORDER);
            _spinnerImageSize.setMinimum(MIN_IMAGE_WIDTH);
            _spinnerImageSize.setMaximum(MAX_IMAGE_WIDTH);
            _spinnerImageSize.setIncrement(1);
            _spinnerImageSize.setPageIncrement(10);
            _spinnerImageSize.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onChangeUI();
               }
            });
            _spinnerImageSize.addMouseWheelListener(new MouseWheelListener() {
               @Override
               public void mouseScrolled(final MouseEvent event) {
                  Util.adjustSpinnerValueOnMouseScroll(event);
                  onChangeUI();
               }
            });

            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_spinnerImageSize);
         }
      }
   }

   private void enableControls() {

   }

   private void initUI(final Composite parent) {

      _parent = parent;
   }

   @Override
   protected boolean isCenterHorizontal() {
      return true;
   }

   private void onChangeUI() {

      final int oldImageSize = _imageSize;

      _imageSize = _spinnerImageSize.getSelection();

      saveState();

      // optimize fire event
      if (oldImageSize != _imageSize) {

         updateUI_Map();
      }
   }

   @Override
   public void resetToDefaults() {

      _imageSize = Photo.MAP_IMAGE_DEFAULT_WIDTH_HEIGHT;

      updateUI();
      updateUI_Map();
   }

   private void restoreState_BeforeUI() {

      _imageSize = Util.getStateInt(_state, STATE_PHOTO_PROPERTIES_IMAGE_SIZE, Photo.MAP_IMAGE_DEFAULT_WIDTH_HEIGHT);

      // ensure that an image is displayed, it happend that image size was 0
      if (_imageSize < 10) {
         _imageSize = Photo.MAP_IMAGE_DEFAULT_WIDTH_HEIGHT;
      }

      Photo.setPaintedMapImageWidth(_imageSize);
   }

   private void saveState() {

      _state.put(STATE_PHOTO_PROPERTIES_IMAGE_SIZE, _imageSize);
   }

   private void updateUI() {

      // image size
      _spinnerImageSize.setSelection(_imageSize);
   }

   private void updateUI_Map() {

      Photo.setPaintedMapImageWidth(_imageSize);

      _map2View.updateUI_Photos();
   }

}
