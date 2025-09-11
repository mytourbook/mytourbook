/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.common.font;

import net.tourbook.common.Messages;
import net.tourbook.common.UI;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/**
 * This is a copy of {@link FontFieldEditor} with an open & close listener which is fired when
 * the dialog is opened or closes. There are also other adjustments.
 * <p>
 * This can be used to keep parent dialog opened when the font dialog is opened.
 */
public class FontFieldEditorExtended extends FieldEditor {

   private final ListenerList<IFontDialogListener> _openListeners = new ListenerList<>();

   /**
    * The text for the change font button, or <code>null</code> if missing.
    */
   private String                                  _buttonText;

   /**
    * The text for the preview, or <code>null</code> if no preview is desired
    */
   private String                                  _previewAreaText;

   /*
    * UI controls
    */

   /**
    * The change font button, or <code>null</code> if none (before creation and after disposal).
    */
   private Button           _btnChangeFont;

   /**
    * Font data for the chosen font button, or <code>null</code> if none.
    */
   private FontData[]       _chosenFont;

   /**
    * The label that displays the selected font, or <code>null</code> if none.
    */
   private Label            _lblSelectedFont;

   private Label            _lblFontSize;

   private Composite        _containerFontSize;
   private Spinner          _spinFontSize;

   private DefaultPreviewer _fontPreviewer;

   /**
    * Internal font previewer implementation.
    */
   private class DefaultPreviewer {

      private String _previewText;

      private Font   _previewFont;
      private Text   _txtPreviewText;

      /**
       * Constructor for the previewer.
       *
       * @param previewText
       * @param parent
       */
      public DefaultPreviewer(final String previewText, final Composite parent) {

         _previewText = previewText;

         _txtPreviewText = new Text(parent, SWT.READ_ONLY | SWT.BORDER | SWT.WRAP);
         _txtPreviewText.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
               if (_previewFont != null) {
                  _previewFont.dispose();
               }
            }
         });

         if (_previewText != null) {
            _txtPreviewText.setText(_previewText);
         }
      }

      /**
       * @return the control the previewer is using
       */
      public Control getControl() {
         return _txtPreviewText;
      }

      public int getPreferredHeight() {
         return convertHorizontalDLUsToPixels(_txtPreviewText, 2 * 8);
      }

      /**
       * @return the preferred size of the previewer.
       */
      public int getPreferredWidth() {
         return convertHorizontalDLUsToPixels(_txtPreviewText, 20 * 4);
      }

      public void setEnabled(final boolean isEnabled) {
         _txtPreviewText.setEnabled(isEnabled);
      }

      /**
       * Set the font to display with
       *
       * @param fontData
       */
      public void setFont(final FontData[] fontData) {

         if (_previewFont != null) {
            _previewFont.dispose();
         }

         _previewFont = new Font(_txtPreviewText.getDisplay(), fontData);
         _txtPreviewText.setFont(_previewFont);
      }
   }

   /**
    * Creates a new font field editor
    */
   protected FontFieldEditorExtended() {}

   /**
    * Creates a font field editor with an optional preview area
    *
    * @param name
    *           the name of the preference this field editor works on
    * @param labelText
    *           the label text of the field editor
    * @param previewAreaText
    *           the text used for the preview window
    * @param parent
    *           the parent of the field editor's control
    */
   public FontFieldEditorExtended(final String name,
                                  final String labelText,
                                  final String previewAreaText,
                                  final Composite parent) {
      init(name, labelText);

      _previewAreaText = previewAreaText;
      _buttonText = JFaceResources.getString("openChange"); //$NON-NLS-1$

      createControl(parent);
   }

   public void addOpenListener(final IFontDialogListener listener) {
      _openListeners.add(listener);
   }

   @Override
   protected void adjustForNumColumns(final int numColumns) {

      // ignore, there will be always 2 top columns
   }

   @Override
   protected void applyFont() {

      if (_chosenFont != null && _fontPreviewer != null) {
         _fontPreviewer.setFont(_chosenFont);
      }
   }

   private void createUI(final Composite parent) {

//      parent.setBackground(UI.SYS_COLOR_YELLOW);

      {
         /*
          * Label: Selected font
          */

         _lblSelectedFont = new Label(parent, SWT.LEFT);

         GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(_lblSelectedFont);
      }

      createUI_10_FontSize(parent);

      {
         /*
          * Font preview
          */
         _fontPreviewer = new DefaultPreviewer(_previewAreaText, parent);

         GridDataFactory.fillDefaults()
               .grab(true, true)
               .hint(_fontPreviewer.getPreferredWidth(), _fontPreviewer.getPreferredHeight())
               .applyTo(_fontPreviewer.getControl());
      }
   }

   private void createUI_10_FontSize(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_MAGENTA);
      {
         {
            /*
             * Font size
             */
            _containerFontSize = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerFontSize);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(_containerFontSize);
//            _containerFontSize.setBackground(UI.SYS_COLOR_GREEN);
            {
               // Label
               _lblFontSize = new Label(_containerFontSize, SWT.NONE);
               GridDataFactory.fillDefaults()
                     .align(SWT.FILL, SWT.CENTER)
                     .applyTo(_lblFontSize);
               _lblFontSize.setText(Messages.Font_Editor_Label_FontSize);

               // Spinner
               _spinFontSize = new Spinner(_containerFontSize, SWT.BORDER);
               _spinFontSize.setMinimum(2);
               _spinFontSize.setMaximum(100);
               _spinFontSize.setPageIncrement(5);
               _spinFontSize.addSelectionListener(new SelectionAdapter() {
                  @Override
                  public void widgetSelected(final SelectionEvent e) {
                     onChangeFontSize();
                  }
               });
               _spinFontSize.addMouseWheelListener(new MouseWheelListener() {

                  @Override
                  public void mouseScrolled(final MouseEvent event) {
                     UI.adjustSpinnerValueOnMouseScroll(event);
                     onChangeFontSize();
                  }
               });
            }
         }
         {
            /*
             * Button: Change font
             */
            _btnChangeFont = createUI_20_ChangeButton(container);

            final int widthHint = convertHorizontalDLUsToPixels(_btnChangeFont, IDialogConstants.BUTTON_WIDTH);
            final int defaultWidth = _btnChangeFont.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x;

//            final GridData gd = new GridData();
//            gd.widthHint = Math.max(widthHint, defaultWidth);
//            gd.verticalAlignment = SWT.BEGINNING;
//            gd.horizontalSpan = 2;
//            _btnChangeFont.setLayoutData(gd);

            GridDataFactory.fillDefaults()
//                  .grab(true, false)
                  .span(2, 1)
//                  .hint(Math.max(widthHint, defaultWidth), SWT.DEFAULT)
                  .applyTo(_btnChangeFont);

         }
      }
   }

   /**
    * Returns the change button for this field editor.
    *
    * @param parent
    *           The Composite to create the button in if required.
    *
    * @return the change button
    */
   private Button createUI_20_ChangeButton(final Composite parent) {

      _btnChangeFont = new Button(parent, SWT.PUSH);
      _btnChangeFont.setText(_buttonText);

      _btnChangeFont.addSelectionListener(new SelectionAdapter() {

         @Override
         public void widgetSelected(final SelectionEvent event) {

            final FontDialog fontDialog = new FontDialog(_btnChangeFont.getShell());

            if (_chosenFont != null) {
               fontDialog.setFontList(_chosenFont);
            }

            fontDialog.setEffectsVisible(false);

            fireOpenEvent(true);

            final FontData font = fontDialog.open();

            fireOpenEvent(false);

            if (font != null) {
               fireFontChanged(font);
            }

         }
      });

      setButtonLayoutData(_btnChangeFont);

      return _btnChangeFont;
   }

   @Override
   protected void doFillIntoGrid(final Composite parent, final int numColumns) {

      createUI(parent);
   }

   @Override
   protected void doLoad() {

      if (_btnChangeFont == null) {
         return;
      }

      updateFont(PreferenceConverter.getFontDataArray(getPreferenceStore(), getPreferenceName()));
   }

   @Override
   protected void doLoadDefault() {

      if (_btnChangeFont == null) {
         return;
      }

      updateFont(PreferenceConverter.getDefaultFontDataArray(getPreferenceStore(), getPreferenceName()));
   }

   @Override
   protected void doStore() {

      if (_chosenFont != null) {
         PreferenceConverter.setValue(getPreferenceStore(), getPreferenceName(), _chosenFont);
      }
   }

   private void fireFontChanged(final FontData font) {

      FontData[] oldFont = _chosenFont;
      if (oldFont == null) {
         oldFont = JFaceResources.getDefaultFont().getFontData();
      }
      setPresentsDefaultValue(false);

      final FontData[] newFontData = new FontData[1];
      newFontData[0] = font;
      updateFont(newFontData);

      fireValueChanged(VALUE, oldFont[0], font);
   }

   /**
    * Fire an open event that the dialog is opened or closes.
    */
   private void fireOpenEvent(final boolean isOpened) {

      final Object[] listeners = _openListeners.getListeners();

      for (final Object listener : listeners) {

         final IFontDialogListener dialogListener = (IFontDialogListener) listener;

         SafeRunnable.run(new SafeRunnable() {
            @Override
            public void run() {
               dialogListener.fontDialogOpened(isOpened);
            }
         });
      }
   }

   /**
    * Get the system default font data.
    *
    * @return FontData[]
    */
   private FontData[] getDefaultFontData() {

      return _lblSelectedFont.getDisplay().getSystemFont().getFontData();
   }

   @Override
   public int getNumberOfControls() {

      return 2;
   }

   private void onChangeFontSize() {

      final FontData[] selectedFont = _chosenFont;

      final FontData font = selectedFont[0];
      font.setHeight(_spinFontSize.getSelection());

      final FontData[] validFont = JFaceResources.getFontRegistry()
            .filterData(
                  selectedFont,
                  _lblSelectedFont.getDisplay());

      fireFontChanged(validFont[0]);
   }

   public void removeOpenListener(final IFontDialogListener listener) {
      _openListeners.remove(listener);
   }

   @Override
   public void setEnabled(final boolean isEnabled, final Composite parent) {

      _btnChangeFont.setEnabled(isEnabled);

      _lblSelectedFont.setEnabled(isEnabled);
      _lblFontSize.setEnabled(isEnabled);

      _fontPreviewer.setEnabled(isEnabled);

      _spinFontSize.setEnabled(isEnabled);
   }

   /**
    * Set indent for the first column fields.
    *
    * @param horizontalIndent
    * @param verticalIndent
    */
   public void setFirstColumnIndent(final int horizontalIndent, final int verticalIndent) {

      GridData gd = (GridData) _lblSelectedFont.getLayoutData();
      gd.horizontalIndent = horizontalIndent;
      gd.verticalIndent = verticalIndent;

      gd = (GridData) _btnChangeFont.getLayoutData();
      gd.horizontalIndent = horizontalIndent;
      gd.verticalIndent = verticalIndent;

      gd = (GridData) _containerFontSize.getLayoutData();
      gd.horizontalIndent = horizontalIndent;
      gd.verticalIndent = verticalIndent;
   }

   @Override
   public void setFocus() {

// this is not working
//    _btnChangeFont.setFocus();

      _spinFontSize.setFocus();
   }

   /**
    * Store the default preference for the field being edited
    */
   protected void setToDefault() {

      final FontData[] defaultFontData = PreferenceConverter.getDefaultFontDataArray(
            getPreferenceStore(),
            getPreferenceName());

      PreferenceConverter.setValue(getPreferenceStore(), getPreferenceName(), defaultFontData);
   }

   public void setTooltipText(final String tooltip) {

      _btnChangeFont.setToolTipText(tooltip);
      _lblSelectedFont.setToolTipText(tooltip);
   }

   /**
    * Updates the change font button and the previewer to reflect the newly selected font.
    *
    * @param font
    *           The FontData[] to update with.
    */
   private void updateFont(final FontData[] font) {

      FontData[] bestFont = JFaceResources.getFontRegistry().filterData(font, _lblSelectedFont.getDisplay());

      //if we have nothing valid do as best we can
      if (bestFont == null) {
         bestFont = getDefaultFontData();
      }

      //Now cache this value in the receiver
      _chosenFont = bestFont;

      if (_lblSelectedFont != null) {
         _lblSelectedFont.setText(StringConverter.asString(_chosenFont[0]));
      }
      if (_fontPreviewer != null) {
         _fontPreviewer.setFont(bestFont);
      }

      // update font size widget
      final int fontHeight = bestFont[0].getHeight();
      _spinFontSize.setSelection(fontHeight);
   }

}
