/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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
import net.tourbook.database.ExternalDatabase;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class PrefPageTour extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String     ID         = "net.tourbook.preferences.PrefPageTour"; //$NON-NLS-1$

   private final boolean          _isOSX     = UI.IS_OSX;
   private final boolean          _isLinux   = UI.IS_LINUX;

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private SelectionListener      _defaultListener;
   private MouseWheelListener     _defaultMouseWheelListener;

   private int                    _defaultInfoWidth;
   private int                    _defaultSpinnerWidth;

   private PixelConverter         _pc;

   /*
    * UI controls
    */
   private Spinner _spinnerTourCacheSize;

   private Button  _btnTestConnection;
   private Button  _rdoDbSystem_InternalH2;
   private Button  _rdoDbSystem_External;

   private Label   _lblDatabaseUrl;
   private Label   _lblDatabaseUrlParameter;
   private Label   _lblDatabaseUsername;
   private Label   _lblDatabasePassword;
   private Label   _lblJdbcDriver;
   private Label   _lblJdbcJarUrl;

   private Text    _txtDatabaseUrl;
   private Text    _txtDatabaseUrlParameter;
   private Text    _txtDatabaseUsername;
   private Text    _txtDatabasePassword;
   private Text    _txtJdbcDriver;
   private Text    _txtJdbcJarUrl;

   public PrefPageTour() {
//      noDefaultAndApplyButton();
   }

   private void actionTestConnection() {

      saveState_Database();

      ExternalDatabase.testConnection();
   }

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite container = createUI(parent);

      restoreState();

      enableControls();

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().spacing(5, 15).applyTo(container);
      {
         createUI_10_TourDB(container);
         createUI_20_TourCache(container);
      }

      return container;
   }

   private void createUI_10_TourDB(final Composite parent) {

      final GridDataFactory gdGrabWidth = GridDataFactory.fillDefaults().grab(true, false);
      final GridDataFactory gdDefaultWidth = GridDataFactory.fillDefaults()
            .grab(true, false)
            .hint(_pc.convertWidthInCharsToPixels(50), SWT.DEFAULT);

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Pref_TourDb_Group_TourDB);
      gdGrabWidth.applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
      {
         _rdoDbSystem_InternalH2 = new Button(group, SWT.RADIO);
         _rdoDbSystem_InternalH2.setText(Messages.Pref_TourDb_Radio_Internal);
         _rdoDbSystem_InternalH2.setToolTipText(Messages.Pref_TourDb_Radio_Internal_Tooltip);
         _rdoDbSystem_InternalH2.addSelectionListener(_defaultListener);

         _rdoDbSystem_External = new Button(group, SWT.RADIO);
         _rdoDbSystem_External.setText(Messages.Pref_TourDb_Radio_External);
         _rdoDbSystem_External.setToolTipText(Messages.Pref_TourDb_Radio_External_Tooltip);

         final Composite container = new Composite(group, SWT.NONE);
         gdGrabWidth.indent(12, 0).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            {
               _lblJdbcJarUrl = UI.createLabel(container, "Jar download &address");
               _lblJdbcJarUrl.setToolTipText(
                     "Url to download the jar file, e.g.\nhttps://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.11/mariadb-java-client-3.5.11.jar");

               _txtJdbcJarUrl = new Text(container, SWT.BORDER);
               gdDefaultWidth.applyTo(_txtJdbcJarUrl);

            }
            {
               _lblJdbcDriver = UI.createLabel(container, "JDBC &driver name");
               _lblJdbcDriver.setToolTipText("e.g. org.mariadb.jdbc.Driver");

               _txtJdbcDriver = new Text(container, SWT.BORDER);
               gdDefaultWidth.applyTo(_txtJdbcDriver);
            }
            {
               _lblDatabaseUrl = UI.createLabel(container, "&Connection url");
               _lblDatabaseUrl.setToolTipText("A database url of the form jdbc:subprotocol:subname\ne.g. jdbc:mariadb://localhost:3306/");

               _txtDatabaseUrl = new Text(container, SWT.BORDER);
               gdDefaultWidth.applyTo(_txtDatabaseUrl);
            }
            {
               _lblDatabaseUrlParameter = UI.createLabel(container, "Additional url &parameters");
               _lblDatabaseUrlParameter.setToolTipText("Additional url parameters, e.g. ;AUTO_SERVER=TRUE");

               _txtDatabaseUrlParameter = new Text(container, SWT.BORDER);
               gdDefaultWidth.applyTo(_txtDatabaseUrlParameter);
            }
            {
               _lblDatabaseUsername = UI.createLabel(container, "Database &username");

               _txtDatabaseUsername = new Text(container, SWT.BORDER);
               gdDefaultWidth.applyTo(_txtDatabaseUsername);
            }
            {
               _lblDatabasePassword = UI.createLabel(container, "Database pass&word");

               _txtDatabasePassword = new Text(container, SWT.BORDER);
               gdDefaultWidth.applyTo(_txtDatabasePassword);
            }
         }

         final Composite containerActions = new Composite(group, SWT.NONE);
         gdGrabWidth.indent(12, 0).applyTo(containerActions);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(containerActions);
         {
            _btnTestConnection = new Button(containerActions, SWT.PUSH);
            _btnTestConnection.setText("Test Connection");
            _btnTestConnection.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> actionTestConnection()));
         }
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

   private void enableControls() {

      final boolean isExternalDb = _rdoDbSystem_External.getSelection();

// SET_FORMATTING_OFF

      _btnTestConnection         .setEnabled(isExternalDb);

      _lblDatabaseUrl            .setEnabled(isExternalDb);
      _lblDatabaseUrlParameter   .setEnabled(isExternalDb);
      _lblDatabaseUsername       .setEnabled(isExternalDb);
      _lblDatabasePassword       .setEnabled(isExternalDb);
      _lblJdbcDriver             .setEnabled(isExternalDb);
      _lblJdbcJarUrl             .setEnabled(isExternalDb);

      _txtDatabaseUrl            .setEnabled(isExternalDb);
      _txtDatabaseUrlParameter   .setEnabled(isExternalDb);
      _txtDatabaseUsername       .setEnabled(isExternalDb);
      _txtDatabasePassword       .setEnabled(isExternalDb);
      _txtJdbcDriver             .setEnabled(isExternalDb);
      _txtJdbcJarUrl             .setEnabled(isExternalDb);

// SET_FORMATTING_ON
   }

   @Override
   public void init(final IWorkbench workbench) {

      setPreferenceStore(_prefStore);
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSpinnerWidth = _isLinux ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(_isOSX ? 14 : 7);
      _defaultInfoWidth = _pc.convertWidthInCharsToPixels(50);

      _defaultMouseWheelListener = mouseEvent -> UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
      _defaultListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> enableControls());
   }

   @Override
   protected void performApply() {

      saveState();

      super.performApply();
   }

   @Override
   protected void performDefaults() {

      _spinnerTourCacheSize.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.TOUR_CACHE_SIZE));

      final boolean isExternal = _prefStore.getDefaultBoolean(ITourbookPreferences.TOUR_DATABASE_IS_EXTERNAL);
      _rdoDbSystem_InternalH2.setSelection(isExternal);
      _rdoDbSystem_External.setSelection(isExternal == false);

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final int oldCacheSize = _prefStore.getInt(ITourbookPreferences.TOUR_CACHE_SIZE);
      final boolean oldIsExternal = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_EXTERNAL);

      saveState();

      boolean isRestart = false;

      final int newCacheSize = _prefStore.getInt(ITourbookPreferences.TOUR_CACHE_SIZE);
      final boolean newIsExternal = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_EXTERNAL);

      if (newCacheSize != oldCacheSize) {

         // tour cache size is modified

         if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
               Messages.Pref_Tour_Dialog_TourCacheIsModified_Title,
               Messages.Pref_Tour_Dialog_TourCacheIsModified_Message)) {

            isRestart = true;
         }
      }

      if (isRestart == false && oldIsExternal != newIsExternal) {

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

// SET_FORMATTING_OFF

      _spinnerTourCacheSize   .setSelection(_prefStore.getInt(ITourbookPreferences.TOUR_CACHE_SIZE));

      // tour db system
      final boolean isExternal = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_EXTERNAL);
      _rdoDbSystem_InternalH2 .setSelection(isExternal == false);
      _rdoDbSystem_External   .setSelection(isExternal);

      _txtJdbcJarUrl          .setText(_prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_JAR_URL));
      _txtJdbcDriver          .setText(_prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_JDBC_DRIVER));
      _txtDatabaseUrl         .setText(_prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_URL));
      _txtDatabaseUrlParameter.setText(_prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_URL_PARAMETER));
      _txtDatabaseUsername    .setText(_prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_USERNAME));
      _txtDatabasePassword    .setText(_prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_PASSWORD));

// SET_FORMATTING_ON
   }

   private void saveState() {

// SET_FORMATTING_OFF

      _prefStore.setValue(ITourbookPreferences.TOUR_CACHE_SIZE,                                 _spinnerTourCacheSize.getSelection());
      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_IS_EXTERNAL,                       _rdoDbSystem_External.getSelection());

// SET_FORMATTING_ON

      saveState_Database();
   }

   private void saveState_Database() {

// SET_FORMATTING_OFF

      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_JAR_URL,                  _txtJdbcJarUrl.getText());
      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_JDBC_DRIVER,              _txtJdbcDriver.getText());
      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_URL,             _txtDatabaseUrl.getText());
      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_URL_PARAMETER,   _txtDatabaseUrlParameter.getText());
      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_USERNAME,        _txtDatabaseUsername.getText());
      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_PASSWORD,        _txtDatabasePassword.getText());

// SET_FORMATTING_ON
   }
}
