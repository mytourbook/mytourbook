/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.equipment;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.tag.TagManager;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
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
 * Slideout to set the content image size
 */
public class SlideoutEquipment_SetImageSize extends ToolbarSlideout {

   private static final IDialogSettings _state = TourbookPlugin.getState(TourDataEditorView.ID);

   private FocusListener                _keepOpenListener;
   private PixelConverter               _pc;

   /*
    * UI controls
    */
   private Composite _shellContainer;

   private Combo     _comboImageSize;
   private Spinner   _spinnerImageSize;

   public SlideoutEquipment_SetImageSize(final Control ownerControl,
                                         final ToolBar toolBar) {

      super(ownerControl, toolBar);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

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
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
         {
            /*
             * Content image size
             */
            {
               // label
               final Label label = new Label(container, SWT.NONE);
               label.setText(Messages.Pref_Appearance_Label_ImageSize);
               label.setToolTipText(Messages.Pref_Appearance_Label_ImageSize_Tooltip);
               GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
            }
            {
               // spinner
               _spinnerImageSize = new Spinner(container, SWT.BORDER);
               _spinnerImageSize.setMinimum(TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_MIN);
               _spinnerImageSize.setMaximum(TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_MAX);
               _spinnerImageSize.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelect_ImageSize_Spinner()));
               _spinnerImageSize.addMouseWheelListener(mouseEvent -> {
                  UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10);
                  onSelect_ImageSize_Spinner();
               });
               GridDataFactory.fillDefaults()
                     .hint(_pc.convertWidthInCharsToPixels(5), SWT.DEFAULT)
                     .align(SWT.BEGINNING, SWT.FILL)
                     .applyTo(_spinnerImageSize);
            }
            {
               // combo
               _comboImageSize = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
               _comboImageSize.setVisibleItemCount(10);
               _comboImageSize.setToolTipText(Messages.Pref_Appearance_Combo_ImageSize_Tooltip);
               _comboImageSize.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelect_ImageSize_Combo()));
               _comboImageSize.addFocusListener(_keepOpenListener);

               GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_comboImageSize);
            }
         }
      }

      return _shellContainer;
   }

   private void fillUI() {

      if (_comboImageSize != null && _comboImageSize.isDisposed() == false) {

         _comboImageSize.add(OtherMessages.APP_SIZE_SMALL_SHORTCUT);
         _comboImageSize.add(OtherMessages.APP_SIZE_MEDIUM_SHORTCUT);
         _comboImageSize.add(OtherMessages.APP_SIZE_LARGE_SHORTCUT);
      }
   }

   private int getSelectedImageSizeIndex() {

      final int selectionIndex = _comboImageSize.getSelectionIndex();

      return selectionIndex < 0
            ? 0
            : selectionIndex;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

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
   protected boolean isAlignRight() {

      return true;
   }

   private void onSelect_ImageSize_Combo() {

      final int selectedImageSizeIndex = getSelectedImageSizeIndex();

      // save selected size
      _state.put(TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_INDEX, selectedImageSizeIndex);

      int imageSize;

      // set size from state
      switch (selectedImageSizeIndex) {
      case 1  -> imageSize = Util.getStateInt(_state,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_MEDIUM,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_MEDIUM_DEFAULT);

      case 2  -> imageSize = Util.getStateInt(_state,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_LARGE,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_LARGE_DEFAULT);

      default -> imageSize = Util.getStateInt(_state,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_SMALL,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_SMALL_DEFAULT);
      }

      // update UI
      _spinnerImageSize.setSelection(imageSize);

      saveState();
   }

   private void onSelect_ImageSize_Spinner() {

      // get width
      final int imageSize = _spinnerImageSize.getSelection();

      // save state for the selected size
      switch (getSelectedImageSizeIndex()) {
      case 1  -> _state.put(TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_MEDIUM, imageSize);
      case 2  -> _state.put(TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_LARGE, imageSize);
      default -> _state.put(TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_SMALL, imageSize);
      }

      saveState();
   }

   private void restoreState() {

      /*
       * Content image
       */
      _spinnerImageSize.setSelection(Util.getStateInt(_state,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_DEFAULT,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_MIN,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_MAX));

      _comboImageSize.select(Util.getStateInt(_state, TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_INDEX, 0));
   }

   private void saveState() {

      _state.put(TourDataEditorView.STATE_CONTENT_IMAGE_SIZE, _spinnerImageSize.getSelection());

      // run async because it can take time to reload the tag images
      _shellContainer.getDisplay().asyncExec(() -> TagManager.updateContentLayout());
   }

}
