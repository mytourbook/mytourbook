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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
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

   private PixelConverter               _pc;

   /*
    * UI controls
    */
   private Composite _shellContainer;

   private Spinner   _spinnerContentImageSize;

   public SlideoutEquipment_SetImageSize(final Control ownerControl,
                                         final ToolBar toolBar) {

      super(ownerControl, toolBar);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      final Composite ui = createUI(parent);

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
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
         {
            /*
             * Content image size
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_Appearance_Label_ImageSize);
            label.setToolTipText(Messages.Pref_Appearance_Label_ImageSize_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

            // spinner
            _spinnerContentImageSize = new Spinner(container, SWT.BORDER);
            _spinnerContentImageSize.setMinimum(TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_MIN);
            _spinnerContentImageSize.setMaximum(TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_MAX);
            _spinnerContentImageSize.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_ContentImageLayout()));
            _spinnerContentImageSize.addMouseWheelListener(mouseEvent -> {
               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10);
               onSelect_ContentImageLayout();
            });
            GridDataFactory.fillDefaults()
                  .hint(_pc.convertWidthInCharsToPixels(5), SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_spinnerContentImageSize);
         }
      }

      return _shellContainer;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   @Override
   protected boolean isAlignRight() {

      return true;
   }

   private void onSelect_ContentImageLayout() {

      _state.put(TourDataEditorView.STATE_CONTENT_IMAGE_SIZE, _spinnerContentImageSize.getSelection());

      // run async because it can take time to reload the tag images
      _spinnerContentImageSize.getDisplay().asyncExec(() -> TagManager.updateContentLayout());
   }

   private void restoreState() {

      /*
       * Content image
       */
      _spinnerContentImageSize.setSelection(Util.getStateInt(_state,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_DEFAULT,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_MIN,
            TourDataEditorView.STATE_CONTENT_IMAGE_SIZE_MAX));
   }

}
