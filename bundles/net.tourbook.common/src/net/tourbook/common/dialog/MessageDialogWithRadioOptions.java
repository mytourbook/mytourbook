/*******************************************************************************
 * Copyright (C) 2021, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.common.dialog;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * This {@link MessageDialog} shows radio buttons
 */
public class MessageDialogWithRadioOptions extends MessageDialog {

   private String[] _allOptionLabels;

   private int      _defaultValue;

   /*
    * UI Controls
    */
   private Button[] _allRadioOptions;

   private int      _selectedOption;

   /**
    * Create a message dialog. Note that the dialog will have no visual
    * representation (no widgets) until it is told to open.
    * <p>
    * The labels of the buttons to appear in the button bar are supplied in this
    * constructor as a varargs of Strings. The <code>open</code> method will return
    * the index of the label in this array corresponding to the button that was
    * pressed to close the dialog.
    * </p>
    * <p>
    * <strong>Note:</strong> If the dialog was dismissed without pressing a button
    * (ESC key, close box, etc.) then {@link SWT#DEFAULT} is returned. Note that
    * the <code>open</code> method blocks.
    * </p>
    *
    * @param parentShell
    *           the parent shell, or <code>null</code> to create a
    *           top-level shell
    * @param dialogTitle
    *           the dialog title, or <code>null</code> if none
    * @param dialogTitleImage
    *           the dialog title image, or <code>null</code> if
    *           none
    * @param dialogMessage
    *           the dialog message
    * @param dialogImageType
    *           one of the following values:
    *           <ul>
    *           <li><code>MessageDialog.NONE</code> for a dialog
    *           with no image</li>
    *           <li><code>MessageDialog.ERROR</code> for a dialog
    *           with an error image</li>
    *           <li><code>MessageDialog.INFORMATION</code> for a
    *           dialog with an information image</li>
    *           <li><code>MessageDialog.QUESTION </code> for a
    *           dialog with a question image</li>
    *           <li><code>MessageDialog.WARNING</code> for a dialog
    *           with a warning image</li>
    *           </ul>
    * @param defaultIndex
    *           the index in the button label array of the default
    *           button
    * @param dialogButtonLabels
    *           varargs of Strings for the button labels in the
    *           button bar
    * @since 3.12
    */
   public MessageDialogWithRadioOptions(final Shell parentShell,
                                        final String dialogTitle,
                                        final Image dialogTitleImage,
                                        final String dialogMessage,
                                        final int dialogImageType,
                                        final int defaultIndex,
                                        final String... dialogButtonLabels) {

      super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, defaultIndex, dialogButtonLabels);
   }

   @Override
   protected Control createCustomArea(final Composite parent) {

      if (_allOptionLabels == null) {
         return null;
      }

      final int iconWidth = imageLabel == null
            ? 0
            : imageLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .indent(iconWidth + 12, 16)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         {
            final int numOptions = _allOptionLabels.length;
            _allRadioOptions = new Button[numOptions];

            for (int optionIndex = 0; optionIndex < numOptions; optionIndex++) {

               final Button optionControl = new Button(container, SWT.RADIO);

               optionControl.setText(_allOptionLabels[optionIndex]);
               optionControl.setData(Integer.valueOf(optionIndex));
               optionControl.addSelectionListener(widgetSelectedAdapter(this::onSelectOption));

               _allRadioOptions[optionIndex] = optionControl;
            }
         }
      }

      // select default value
      _allRadioOptions[_defaultValue].setSelection(true);

      return container;
   }

   /**
    * @return Returns the index of the selected radio button
    */
   public int getSelectedOption() {

      return _selectedOption;
   }

   private void onSelectOption(final SelectionEvent selectionEvent) {

      final Object data = selectionEvent.widget.getData();

      _selectedOption = (int) data;
   }

   /**
    * Set labels for the radio buttons
    *
    * @param optionMessage
    * @param options
    * @param defaultValue
    */
   public void setRadioOptions(final String[] options, final int defaultValue) {

      _allOptionLabels = options;

      // ensure array bounds
      _defaultValue = Math.max(0, Math.min(_allOptionLabels.length - 1, defaultValue));

      _selectedOption = _defaultValue;
   }

}
