/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import java.io.File;

import net.tourbook.Messages;
import net.tourbook.application.ApplicationTools;
import net.tourbook.application.ApplicationTools.FixState;
import net.tourbook.application.ApplicationWorkbenchAdvisor;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class PrefPageWorkspace extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String     ID                   = "net.tourbook.preferences.PrefPageWorkspace";        //$NON-NLS-1$

   private static File            _workbenchFolderPath = ApplicationWorkbenchAdvisor.getWorkbenchFolderPath();

   private final IPreferenceStore _prefStore           = TourbookPlugin.getPrefStore();

   private PixelConverter         _pc;

   private SelectionListener      _defaultSelectionListener;

   private FixState               _stateCloseButtons;
   private FixState               _stateIconURIs;

   /*
    * UI controls
    */
   private Button _btnApply;

   private Button _chkFixViewCloseButton;
   private Button _chkFixViewIconURI;

   private Label  _lblFixCloseButton;
   private Label  _lblFixCloseButton_State;
   private Label  _lblFixIconURI;
   private Label  _lblFixIconURI_State;

   public PrefPageWorkspace() {

      noDefaultAndApplyButton();
   }

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite container = createUI(parent);

      updateUI();

      enableControls();

      return container;
   }

   private Composite createUI(final Composite parent) {

      final int textDefaultWidth = _pc.convertWidthInCharsToPixels(40);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
//            .spacing(5, 15)
//            .numColumns(2)
            .applyTo(container);
//      container.setBackground(UI.SYS_COLOR_YELLOW);
      {
         {
            /*
             * Info
             */
            final File fileWorkbenchXMI = new File(
                  _workbenchFolderPath,
                  ApplicationTools.WORKBENCH_XMI);

            final Text txtInfo = new Text(container, SWT.READ_ONLY | SWT.WRAP);
            txtInfo.setText(Messages.Pref_Workspace_Label_Info.formatted(fileWorkbenchXMI));
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .hint(textDefaultWidth, SWT.DEFAULT)
                  .applyTo(txtInfo);
         }
         {
            /*
             * Fix view close button
             */
            _lblFixCloseButton_State = new Label(container, SWT.WRAP);
            _lblFixCloseButton_State.setText(UI.EMPTY_STRING);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .indent(0, 20)
                  .hint(textDefaultWidth, SWT.DEFAULT)
                  .applyTo(_lblFixCloseButton_State);

            _chkFixViewCloseButton = new Button(container, SWT.CHECK);
            _chkFixViewCloseButton.setText(Messages.Pref_Workspace_Check_FixViewCloseButton);
            _chkFixViewCloseButton.addSelectionListener(_defaultSelectionListener);

            _lblFixCloseButton = new Label(container, SWT.WRAP);
            _lblFixCloseButton.setText(Messages.Pref_Workspace_Label_FixViewCloseButton);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .hint(textDefaultWidth, SWT.DEFAULT)
                  .indent(12, 0)
                  .applyTo(_lblFixCloseButton);
         }
         {
            /*
             * Fix icon uri
             */
            _lblFixIconURI_State = new Label(container, SWT.WRAP);
            _lblFixIconURI_State.setText(UI.EMPTY_STRING);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .indent(0, 20)
                  .hint(textDefaultWidth, SWT.DEFAULT)
                  .applyTo(_lblFixIconURI_State);

            _chkFixViewIconURI = new Button(container, SWT.CHECK);
            _chkFixViewIconURI.setText(Messages.Pref_Workspace_Check_FixViewIconImage);
            _chkFixViewIconURI.addSelectionListener(_defaultSelectionListener);

            _lblFixIconURI = new Label(container, SWT.WRAP);
            _lblFixIconURI.setText(Messages.Pref_Workspace_Label_FixViewIconImage);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .hint(textDefaultWidth, SWT.DEFAULT)
                  .indent(12, 0)
                  .applyTo(_lblFixIconURI);
         }
         {
            /*
             * Button: Apply & Restart
             */
            _btnApply = new Button(container, SWT.PUSH);
            _btnApply.setText(Messages.Pref_Workspace_Button_ApplyAndRestart);
            _btnApply.setToolTipText(Messages.Pref_Workspace_Button_ApplyAndRestart_Tooltip);
            _btnApply.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onApplyAndRestart()));
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .indent(0, 20)
                  .applyTo(_btnApply);
         }
      }

      return container;
   }

   private void enableControls() {

      final boolean isCloseButtonInvalid = _stateCloseButtons.numIssues > 0;
      final boolean isIconImageInvalid = _stateIconURIs.numIssues > 0;

      final boolean isFixCloseButton = _chkFixViewCloseButton.getSelection() && isCloseButtonInvalid;
      final boolean isFixIconURI = _chkFixViewIconURI.getSelection() && isIconImageInvalid;

      final boolean isFixSelected = isFixCloseButton || isFixIconURI;

      _btnApply.setEnabled(isFixSelected);

      _chkFixViewCloseButton.setEnabled(isCloseButtonInvalid);
      _chkFixViewIconURI.setEnabled(isIconImageInvalid);

      _lblFixCloseButton.setEnabled(isFixCloseButton);
      _lblFixIconURI.setEnabled(isFixIconURI);
   }

   @Override
   public void init(final IWorkbench workbench) {

      setPreferenceStore(_prefStore);
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> enableControls());

   }

   private void onApplyAndRestart() {

      // check if the tour editor contains a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      ApplicationWorkbenchAdvisor.isFixViewCloseButton = _chkFixViewCloseButton.getSelection();
      ApplicationWorkbenchAdvisor.isFixViewIconImage = _chkFixViewIconURI.getSelection();

      Display.getCurrent().asyncExec(() -> PlatformUI.getWorkbench().restart());
   }

   @Override
   protected void performApply() {

      saveState();

      super.performApply();
   }

   @Override
   public boolean performOk() {

      saveState();

      return true;
   }

   private void saveState() {

   }

   private void updateUI() {

      final Color colorOK = UI.IS_DARK_THEME ? UI.SYS_COLOR_GREEN : UI.SYS_COLOR_DARK_GREEN;
      final Color colorInvalid = UI.IS_DARK_THEME ? new Color(0xff, 0x40, 0x40) : UI.SYS_COLOR_RED;

      _stateCloseButtons = ApplicationTools.fixViewCloseButtons(_workbenchFolderPath, false);
      _stateIconURIs = ApplicationTools.fixViewIcons(_workbenchFolderPath, false);

      final int numIssuesViewIcons = _stateCloseButtons.numIssues;
      final int numIssuesViewButtons = _stateIconURIs.numIssues;

      final String stateViewButton_Text = "Number of hidden close buttons: %d".formatted(numIssuesViewIcons);
      final String stateViewIcon_Text = "Number of invalid icon images: %d".formatted(numIssuesViewButtons);

      final String stateViewButton_Tooltip = _stateCloseButtons.stateText;
      final String stateViewIcon_Tooltip = _stateIconURIs.stateText;

      final boolean isViewIconIssue = numIssuesViewIcons > 0;
      final boolean isViewButtonIssue = numIssuesViewButtons > 0;

      _btnApply.getDisplay().asyncExec(() -> {

         // must be run async otherwise the color is not set in dark theme

         _lblFixCloseButton_State.setText(stateViewButton_Text);
         _lblFixCloseButton_State.setToolTipText(stateViewButton_Tooltip);
         _lblFixCloseButton_State.setForeground(isViewButtonIssue ? colorInvalid : colorOK);

         _lblFixIconURI_State.setText(stateViewIcon_Text);
         _lblFixIconURI_State.setToolTipText(stateViewIcon_Tooltip);
         _lblFixIconURI_State.setForeground(isViewIconIssue ? colorInvalid : colorOK);
      });
   }
}
