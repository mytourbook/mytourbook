/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import net.tourbook.OtherMessages;
import net.tourbook.common.UI;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for the tour data editor options.
 */
public class SlideoutTourInfoOptions extends ToolbarSlideout implements IColorSelectorListener, IActionResetToDefault {

   static final String            STATE_UI_WIDTH_SIZE_INDEX     = "STATE_UI_WIDTH_SIZE_INDEX"; //$NON-NLS-1$

   static final String            STATE_UI_WIDTH_SMALL          = "STATE_UI_WIDTH_SMALL";      //$NON-NLS-1$
   static final String            STATE_UI_WIDTH_MEDIUM         = "STATE_UI_WIDTH_MEDIUM";     //$NON-NLS-1$
   static final String            STATE_UI_WIDTH_LARGE          = "STATE_UI_WIDTH_LARGE";      //$NON-NLS-1$
   static final int               STATE_UI_WIDTH_SMALL_DEFAULT  = 600;
   static final int               STATE_UI_WIDTH_MEDIUM_DEFAULT = 800;
   static final int               STATE_UI_WIDTH_LARGE_DEFAULT  = 1000;

   static final int               STATE_UI_WIDTH_MIN            = 100;
   static final int               STATE_UI_WIDTH_MAX            = 3000;

   private static IDialogSettings _state;

   private TourInfoUI             _tourInfoUI;

   private FocusListener          _keepOpenListener;

   /*
    * UI controls
    */
   private Composite _shellContainer;

   private Combo     _comboUIWidth_Size;

   private Spinner   _spinnerUIWidth_Pixel;

   public SlideoutTourInfoOptions(final Control ownerControl,
                                  final ToolBar toolBar,
                                  final TourInfoUI tourInfoUI,
                                  final IDialogSettings state) {

      super(ownerControl, toolBar);

      _tourInfoUI = tourInfoUI;
      _state = state;
   }

   @Override
   public void colorDialogOpened(final boolean isDialogOpened) {

      setIsAnotherDialogOpened(isDialogOpened);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI();

      final Composite ui = createUI(parent);

      fillUI();

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
      {
         final Composite container = new Composite(_shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            createUI_10_Title(container);
            createUI_20_Options(container);
         }
      }

      return _shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      label.setText("Tour Info Options");
//      label.setFont(JFaceResources.getBannerFont());
      GridDataFactory.fillDefaults().span(2, 1).applyTo(label);

      MTFont.setBannerFont(label);
   }

   private void createUI_20_Options(final Composite parent) {

      {
         /*
          * Tooltip UI width
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText("Tooltip &width");
         GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
      }
      {
         final Composite widthContainer = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(widthContainer);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(widthContainer);
         {
            {
               // Combo: Mouse wheel incrementer
               _comboUIWidth_Size = new Combo(widthContainer, SWT.READ_ONLY | SWT.BORDER);
               _comboUIWidth_Size.setVisibleItemCount(10);
               _comboUIWidth_Size.setToolTipText(net.tourbook.ui.Messages.Tour_Tooltip_Combo_UIWidthSize_Tooltip);
               _comboUIWidth_Size.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelect_UIWidth_1_Size()));
               _comboUIWidth_Size.addFocusListener(_keepOpenListener);

               GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_comboUIWidth_Size);
            }
            {
               /*
                * Text width in pixel
                */
               _spinnerUIWidth_Pixel = new Spinner(widthContainer, SWT.BORDER);
               _spinnerUIWidth_Pixel.setMinimum(STATE_UI_WIDTH_MIN);
               _spinnerUIWidth_Pixel.setMaximum(STATE_UI_WIDTH_MAX);
               _spinnerUIWidth_Pixel.setIncrement(10);
               _spinnerUIWidth_Pixel.setPageIncrement(50);
               _spinnerUIWidth_Pixel.setToolTipText(net.tourbook.ui.Messages.Tour_Tooltip_Spinner_TextWidth_Tooltip);

               _spinnerUIWidth_Pixel.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                     selectionEvent -> onSelect_UIWidth_2_Value()));

               _spinnerUIWidth_Pixel.addMouseWheelListener(mouseEvent -> {

                  UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10);
                  onSelect_UIWidth_2_Value();
               });

               GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_spinnerUIWidth_Pixel);
            }
         }
      }
   }

   private void fillUI() {

      if (_comboUIWidth_Size != null && _comboUIWidth_Size.isDisposed() == false) {

         _comboUIWidth_Size.add(OtherMessages.APP_SIZE_SMALL_SHORTCUT);
         _comboUIWidth_Size.add(OtherMessages.APP_SIZE_MEDIUM_SHORTCUT);
         _comboUIWidth_Size.add(OtherMessages.APP_SIZE_LARGE_SHORTCUT);
      }
   }

   private int getSelectedUIWidthSizeIndex() {

      final int selectionIndex = _comboUIWidth_Size.getSelectionIndex();

      return selectionIndex < 0
            ? 0
            : selectionIndex;
   }

   private void initUI() {

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {

            setIsAnotherDialogOpened(false);
         }
      };
   }

   @Override
   protected boolean isCenterHorizontal() {

      return true;
   }

   private void onSelect_UIWidth_1_Size() {

      final int selectedUIWidthSizeIndex = getSelectedUIWidthSizeIndex();

      // save selected size
      _state.put(STATE_UI_WIDTH_SIZE_INDEX, selectedUIWidthSizeIndex);

      int uiWidth_Pixel;

      // set size from state
      switch (getSelectedUIWidthSizeIndex()) {
      case 1  -> uiWidth_Pixel = Util.getStateInt(_state, STATE_UI_WIDTH_MEDIUM, STATE_UI_WIDTH_MEDIUM_DEFAULT);
      case 2  -> uiWidth_Pixel = Util.getStateInt(_state, STATE_UI_WIDTH_LARGE, STATE_UI_WIDTH_LARGE_DEFAULT);
      default -> uiWidth_Pixel = Util.getStateInt(_state, STATE_UI_WIDTH_SMALL, STATE_UI_WIDTH_SMALL_DEFAULT);
      }

      // update model
      _tourInfoUI.setUIWidth_Pixel(uiWidth_Pixel);

      // update UI
      _spinnerUIWidth_Pixel.setSelection(uiWidth_Pixel);

      _tourInfoUI.updateUI_UIWidth();
   }

   private void onSelect_UIWidth_2_Value() {

      // get width
      final int uiWidth_Pixel = _spinnerUIWidth_Pixel.getSelection();

      // save state for the selected size
      switch (getSelectedUIWidthSizeIndex()) {
      case 1  -> _state.put(STATE_UI_WIDTH_MEDIUM, uiWidth_Pixel);
      case 2  -> _state.put(STATE_UI_WIDTH_LARGE, uiWidth_Pixel);
      default -> _state.put(STATE_UI_WIDTH_SMALL, uiWidth_Pixel);
      }

      // update model
      _tourInfoUI.setUIWidth_Pixel(uiWidth_Pixel);

      // update UI
      _tourInfoUI.updateUI_UIWidth();
   }

   @Override
   public void resetToDefaults() {

   }

   private void restoreState() {

      final int uiWidth_SizeIndex = Util.getStateInt(_state, SlideoutTourInfoOptions.STATE_UI_WIDTH_SIZE_INDEX, 0);

      if (_spinnerUIWidth_Pixel != null && _spinnerUIWidth_Pixel.isDisposed() == false) {

         _spinnerUIWidth_Pixel.setSelection(_tourInfoUI.getUIWidth_Pixel());

         _comboUIWidth_Size.select(uiWidth_SizeIndex);
      }
   }

}
