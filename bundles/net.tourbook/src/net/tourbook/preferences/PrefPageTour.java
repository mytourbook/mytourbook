/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class PrefPageTour extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String ID = "net.tourbook.preferences.PrefPageTour"; //$NON-NLS-1$

// private static final String    VERSION_14_10 = "14.10";                 //$NON-NLS-1$

   private final boolean          _isOSX     = UI.IS_OSX;
   private final boolean          _isLinux   = UI.IS_LINUX;

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private MouseWheelListener     _defaultMouseWheelListener;

// private Font                   _boldFont     = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

   private int            _defaultSpinnerWidth;
   private int            _defaultInfoWidth;

   private PixelConverter _pc;

   /*
    * UI controls
    */
   private Spinner _spinnerTourCacheSize;

   private Button  _rdoDbSystemEmbedded;
   private Button  _rdoDbSystemServer;

   public PrefPageTour() {
//      noDefaultAndApplyButton();
   }

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite container = createUI(parent);

      restoreState();

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().spacing(5, 15).applyTo(container);
      {
         createUI_10_TourDB(container);
         createUI_20_TourCache(container);
//       createUI_30_PostUpdate(container);
      }

      return container;
   }

   private void createUI_10_TourDB(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Pref_TourDb_Group_TourDB);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         _rdoDbSystemEmbedded = new Button(group, SWT.RADIO);
         _rdoDbSystemEmbedded.setText(Messages.Pref_TourDb_Radio_DbSystem_Embedded);
         _rdoDbSystemEmbedded.setToolTipText(Messages.Pref_TourDb_Radio_DbSystem_Embedded_Tooltip);

         _rdoDbSystemServer = new Button(group, SWT.RADIO);
         _rdoDbSystemServer.setText(Messages.Pref_TourDb_Radio_DbSystem_Server);
         _rdoDbSystemServer.setToolTipText(Messages.Pref_TourDb_Radio_DbSystem_Server_Tooltip);
      }
   }

   private void createUI_20_TourCache(final Composite parent) {

      final int verticalIndent = 20;

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Pref_Tour_Group_TourCache);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         /*
          * label: info
          */
         Label label = new Label(group, SWT.WRAP);
         label.setText(Messages.Pref_Tour_Label_TourCacheSize_Info);
         GridDataFactory.fillDefaults()
               .hint(_defaultInfoWidth, SWT.DEFAULT)
               .grab(true, false)
               .span(2, 1)
               .applyTo(label);

         /*
          * label: cache size
          */
         label = new Label(group, NONE);
         label.setText(Messages.Pref_Tour_Label_TourCacheSize);
         GridDataFactory.fillDefaults()
               .indent(0, verticalIndent)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(label);

         // spinner: cache size
         _spinnerTourCacheSize = new Spinner(group, SWT.BORDER);

         // cache size ==1 causes "java.lang.IllegalStateException: Queue full"
         _spinnerTourCacheSize.setMinimum(2);
         _spinnerTourCacheSize.setMaximum(100_000);
         _spinnerTourCacheSize.addMouseWheelListener(_defaultMouseWheelListener);
         GridDataFactory.fillDefaults()
               .indent(0, verticalIndent)
               .hint(_defaultSpinnerWidth, SWT.DEFAULT)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_spinnerTourCacheSize);
      }
   }

//   private void createUI_30_PostUpdate(final Composite parent) {
//
//      final Group group = new Group(parent, SWT.NONE);
//      group.setText(Messages.Pref_Tour_Group_FailedUpdates);
//      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
//      GridLayoutFactory.swtDefaults().applyTo(group);
//      {
//         /*
//          * label: info
//          */
//         Label label = new Label(group, SWT.WRAP);
//         label.setText(Messages.Pref_Tour_Label_FailedUpdateInfo);
//         GridDataFactory.fillDefaults()
//               .grab(true, false)
//               .hint(_defaultInfoWidth, SWT.DEFAULT)
//               .applyTo(label);
//
//         /*
//          * label: info bold
//          */
//         label = new Label(group, SWT.WRAP);
//         label.setText(Messages.Pref_Tour_Label_FailedUpdateInfo_BOLD);
//         label.setFont(_boldFont);
//         GridDataFactory.fillDefaults()
//               .grab(true, false)
//               .hint(_defaultInfoWidth, SWT.DEFAULT)
//               .indent(0, _pc.convertVerticalDLUsToPixels(8))
//               .applyTo(label);
//
//         // spacer
//         new Label(group, SWT.WRAP);
//
//         /*
//          * Button: Post update 14.10
//          */
//         final Button button = new Button(group, SWT.NONE);
//         button.setText(NLS.bind(Messages.Pref_Tour_Button_FailedUpdate, VERSION_14_10));
//         button.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectFailedUpdate_14_10()));
//      }
//   }

   @Override
   public void init(final IWorkbench workbench) {

      setPreferenceStore(_prefStore);
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSpinnerWidth = _isLinux ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(_isOSX ? 14 : 7);
      _defaultInfoWidth = _pc.convertWidthInCharsToPixels(50);

      _defaultMouseWheelListener = mouseEvent -> UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
   }

//   private void onSelectFailedUpdate_14_10() {
//
//      if (MessageDialog.openConfirm(
//            getShell(),
//            Messages.Pref_Tour_Dialog_ConfirmDatabaseUpdate_Title,
//            NLS.bind(Messages.Pref_Tour_Dialog_ConfirmDatabaseUpdate_Message, VERSION_14_10)) == false) {
//         return;
//      }
//
//      final IRunnableWithProgress runnable = new IRunnableWithProgress() {
//
//         @Override
//         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//            try {

//
// !!! "updateDb_024_To_025_DataUpdate(null <-- " fail when run, this was probably never tested
//

//               TourDatabase.getInstance().updateDb_024_To_025_DataUpdate(null, monitor);
//            } catch (final SQLException e) {
//               net.tourbook.ui.UI.showSQLException(e);
//            }
//         }
//      };
//      try {
//         new ProgressMonitorDialog(getShell()).run(true, true, runnable);
//      } catch (final Exception e) {
//         StatusUtil.showStatus(e);
//      }
//   }

   @Override
   protected void performApply() {

      saveState();

      super.performApply();
   }

   @Override
   protected void performDefaults() {

      _spinnerTourCacheSize.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.TOUR_CACHE_SIZE));

      final boolean isEmbedded = _prefStore.getDefaultBoolean(ITourbookPreferences.TOUR_DATABASE_IS_DERBY_EMBEDDED);
      _rdoDbSystemEmbedded.setSelection(isEmbedded);
      _rdoDbSystemServer.setSelection(isEmbedded == false);

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final int oldCacheSize = _prefStore.getInt(ITourbookPreferences.TOUR_CACHE_SIZE);
      final boolean oldIsEmbedded = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_DERBY_EMBEDDED);

      saveState();

      boolean isRestart = false;

      final int newCacheSize = _prefStore.getInt(ITourbookPreferences.TOUR_CACHE_SIZE);
      final boolean newIsEmbedded = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_DERBY_EMBEDDED);

      if (newCacheSize != oldCacheSize) {

         // tour cache size is modified

         if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
               Messages.Pref_Tour_Dialog_TourCacheIsModified_Title,
               Messages.Pref_Tour_Dialog_TourCacheIsModified_Message)) {

            isRestart = true;
         }
      }

      if (isRestart == false && oldIsEmbedded != newIsEmbedded) {

         // db system is modified

         if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
               Messages.Pref_TourDb_Dialog_TourDbSystemIsModified_Title,
               Messages.Pref_TourDb_Dialog_TourDbSystemIsModified_Message)) {

            isRestart = true;
         }
      }

      if (isRestart) {

         Display.getCurrent().asyncExec(() -> PlatformUI.getWorkbench().restart());
      }

      return true;
   }

   private void restoreState() {

      _spinnerTourCacheSize.setSelection(_prefStore.getInt(ITourbookPreferences.TOUR_CACHE_SIZE));

      // tour db system
      final boolean isEmbedded = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_DERBY_EMBEDDED);
      _rdoDbSystemEmbedded.setSelection(isEmbedded);
      _rdoDbSystemServer.setSelection(isEmbedded == false);

   }

   private void saveState() {

      _prefStore.setValue(ITourbookPreferences.TOUR_CACHE_SIZE, _spinnerTourCacheSize.getSelection());
      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_IS_DERBY_EMBEDDED, _rdoDbSystemEmbedded.getSelection());
   }
}
