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
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourLogManager.AutoOpenEvent;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class PrefPageTour extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String            ID                           = "net.tourbook.preferences.PrefPageTour"; //$NON-NLS-1$

   private static final String           STATE_PREF_TOUR_SELECTED_TAB = "STATE_PREF_TOUR_SELECTED_TAB";          //$NON-NLS-1$

   private static final IPreferenceStore _prefStore                   = TourbookPlugin.getPrefStore();

   private final boolean                 _isOSX                       = UI.IS_OSX;
   private final boolean                 _isLinux                     = UI.IS_LINUX;

   private SelectionListener             _defaultListener;
   private MouseWheelListener            _defaultMouseWheelListener;

   private int                           _defaultInfoWidth;
   private int                           _defaultSpinnerWidth;

   private PixelConverter                _pc;

   /*
    * UI controls
    */
   private Spinner    _spinnerTourCacheSize;

   private Button     _btnClearLog;
   private Button     _btnTestConnection;
   private Button     _rdoDbSystem_InternalH2;
   private Button     _rdoDbSystem_External;

   private Label      _lblDatabaseName;
   private Label      _lblConnectionUrl;
   private Label      _lblConnectionUrl_Protocol;
   private Label      _lblConnectionUrl_Parameter;
   private Label      _lblDatabaseUsername;
   private Label      _lblDatabasePassword;
   private Label      _lblHibernateDialect;
   private Label      _lblJdbcDriver;
   private Label      _lblJarDownloadUrl;

   private Text       _txtDatabaseName;
   private Text       _txtConnectionUrl;
   private Text       _txtConnectionUrl_Protocol;
   private Text       _txtConnectionUrl_Parameter;
   private Text       _txtDatabaseUsername;
   private Text       _txtDatabasePassword;
   private Text       _txtHibernateDialect;
   private Text       _txtJdbcDriver;
   private Text       _txtJarDownloadUrl;

   private Font       _fontItalic;

   private CTabFolder _tabFolder;

   public PrefPageTour() {
//      noDefaultAndApplyButton();
   }

   private void actionTestConnection() {

      saveState_ExternalDatabase();

      // force to display the log view
      TourLogManager.showLogView(AutoOpenEvent.EXCEPTION);

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

      _tabFolder = new CTabFolder(parent, SWT.TOP);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(_tabFolder);
//      _tabFolder.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         final CTabItem tabMeasurementSystem = new CTabItem(_tabFolder, SWT.NONE);
         tabMeasurementSystem.setControl(createUI_10_TourDB(_tabFolder));
         tabMeasurementSystem.setText(Messages.Pref_TourDb_Group_TourDB);

         final CTabItem tabTimeZone = new CTabItem(_tabFolder, SWT.NONE);
         tabTimeZone.setControl(createUI_20_TourCache(_tabFolder));
         tabTimeZone.setText(Messages.Pref_Tour_Group_TourCache);
      }

      return _tabFolder;
   }

   private Composite createUI_10_TourDB(final Composite parent) {

      final GridDataFactory gdGrabWidth = GridDataFactory.fillDefaults().grab(true, false);

      final GridDataFactory gdLabelConnection = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.FILL)
            .indent(12, 0);

      final GridDataFactory gdDefaultWidth = GridDataFactory.fillDefaults()
            .grab(true, false)
            .hint(_pc.convertWidthInCharsToPixels(50), SWT.DEFAULT);

      final GridDataFactory gdLabel = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.FILL);

      final ModifyListener modifyListener = e -> onModify();

      final Composite container = new Composite(parent, SWT.NONE);
      gdGrabWidth.applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_GREEN);
      {
         _rdoDbSystem_InternalH2 = new Button(container, SWT.RADIO);
         _rdoDbSystem_InternalH2.setText(Messages.Pref_TourDb_Radio_Internal);
         _rdoDbSystem_InternalH2.setToolTipText(Messages.Pref_TourDb_Radio_Internal_Tooltip);
         _rdoDbSystem_InternalH2.addSelectionListener(_defaultListener);

         _rdoDbSystem_External = new Button(container, SWT.RADIO);
         _rdoDbSystem_External.setText(Messages.Pref_TourDb_Radio_External);
         _rdoDbSystem_External.setToolTipText(Messages.Pref_TourDb_Radio_External_Tooltip);

         final Composite containerExternal = new Composite(container, SWT.NONE);
         gdGrabWidth.indent(12, 0).applyTo(containerExternal);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerExternal);
         {
            {
               _lblJarDownloadUrl = UI.createLabel(containerExternal, "Jar download &address");
               _lblJarDownloadUrl.setToolTipText(
                     "Url to download the jar file, e.g.\nhttps://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.11/mariadb-java-client-3.5.11.jar");
               gdLabel.applyTo(_lblJarDownloadUrl);

               _txtJarDownloadUrl = new Text(containerExternal, SWT.BORDER);
               gdDefaultWidth.applyTo(_txtJarDownloadUrl);

            }
            {
               _lblJdbcDriver = UI.createLabel(containerExternal, "JDBC &driver name");
               _lblJdbcDriver.setToolTipText("e.g. org.mariadb.jdbc.Driver");
               gdLabel.applyTo(_lblJdbcDriver);

               _txtJdbcDriver = new Text(containerExternal, SWT.BORDER);
               gdDefaultWidth.applyTo(_txtJdbcDriver);
            }
            {
               _lblConnectionUrl_Protocol = UI.createLabel(containerExternal, "&Connection url");
               _lblConnectionUrl_Protocol.setToolTipText("A database url of the form jdbc:subprotocol:subname\ne.g. jdbc:mariadb://localhost:3306/");
               gdLabel.applyTo(_lblConnectionUrl_Protocol);

               _txtConnectionUrl_Protocol = new Text(containerExternal, SWT.BORDER);
               _txtConnectionUrl_Protocol.addModifyListener(modifyListener);
               gdDefaultWidth.applyTo(_txtConnectionUrl_Protocol);
            }
            {
               _lblDatabaseName = UI.createLabel(containerExternal, "Database na&me");
               _lblDatabaseName.setToolTipText("Sql database name");
               gdLabelConnection.applyTo(_lblDatabaseName);

               _txtDatabaseName = new Text(containerExternal, SWT.BORDER);
               _txtDatabaseName.addModifyListener(modifyListener);
               gdDefaultWidth.applyTo(_txtDatabaseName);
            }
            {
               _lblConnectionUrl_Parameter = UI.createLabel(containerExternal, "&Parameters");
               _lblConnectionUrl_Parameter.setToolTipText("Additional connection parameters, e.g. ;AUTO_SERVER=TRUE");
               gdLabelConnection.applyTo(_lblConnectionUrl_Parameter);

               _txtConnectionUrl_Parameter = new Text(containerExternal, SWT.BORDER);
               _txtConnectionUrl_Parameter.addModifyListener(modifyListener);
               gdDefaultWidth.applyTo(_txtConnectionUrl_Parameter);
            }
            {
               _lblConnectionUrl = UI.createLabel(containerExternal, "Connection url");
               _lblConnectionUrl.setToolTipText("Concatenated connection url parts");
               _lblConnectionUrl.setFont(_fontItalic);
               gdLabelConnection.applyTo(_lblConnectionUrl);

               _txtConnectionUrl = new Text(containerExternal, SWT.READ_ONLY);
               _txtConnectionUrl.setFont(_fontItalic);
               gdDefaultWidth.applyTo(_txtConnectionUrl);
            }
            {
               _lblDatabaseUsername = UI.createLabel(containerExternal, "Database &username");
               gdLabel.applyTo(_lblDatabaseUsername);

               _txtDatabaseUsername = new Text(containerExternal, SWT.BORDER);
               gdDefaultWidth.applyTo(_txtDatabaseUsername);
            }
            {
               _lblDatabasePassword = UI.createLabel(containerExternal, "Database pass&word");
               gdLabel.applyTo(_lblDatabasePassword);

               _txtDatabasePassword = new Text(containerExternal, SWT.BORDER);
               gdDefaultWidth.applyTo(_txtDatabasePassword);
            }
            {
               _lblHibernateDialect = UI.createLabel(containerExternal, "Hibernate dialect");
               _lblHibernateDialect.setToolTipText("EJB hibernate dialect, e.g. org.hibernate.dialect.MariaDB103Dialect");
               gdLabel.applyTo(_lblHibernateDialect);

               _txtHibernateDialect = new Text(containerExternal, SWT.BORDER);
               gdDefaultWidth.applyTo(_txtHibernateDialect);
            }
         }

         final Composite containerActions = new Composite(container, SWT.NONE);
         gdGrabWidth.indent(12, 20).grab(true, false).applyTo(containerActions);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerActions);
         {
            _btnTestConnection = new Button(containerActions, SWT.PUSH);
            _btnTestConnection.setText("&Test Connection");
            _btnTestConnection.setToolTipText("Test connection with the current configuration");
            _btnTestConnection.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> actionTestConnection()));

            _btnClearLog = new Button(containerActions, SWT.PUSH);
            _btnClearLog.setText("Clear &Log");
            _btnClearLog.setToolTipText("Clear tour log view");
            _btnClearLog.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> TourLogManager.clear()));
         }
      }

      return container;
   }

   private Composite createUI_20_TourCache(final Composite parent) {

      final int verticalIndent = 20;

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
      {
         /*
          * label: info
          */
         Label label = new Label(container, SWT.WRAP);
         label.setText(Messages.Pref_Tour_Label_TourCacheSize_Info);
         GridDataFactory.fillDefaults()
               .hint(_defaultInfoWidth, SWT.DEFAULT)
               .grab(true, false)
               .span(2, 1)
               .applyTo(label);

         /*
          * label: cache size
          */
         label = new Label(container, NONE);
         label.setText(Messages.Pref_Tour_Label_TourCacheSize);
         GridDataFactory.fillDefaults()
               .indent(0, verticalIndent)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(label);

         // spinner: cache size
         _spinnerTourCacheSize = new Spinner(container, SWT.BORDER);

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

      return container;
   }

   private void enableControls() {

      final boolean isExternalDb = _rdoDbSystem_External.getSelection();

// SET_FORMATTING_OFF

      _btnTestConnection            .setEnabled(isExternalDb);

      _lblConnectionUrl             .setEnabled(isExternalDb);
      _lblConnectionUrl_Protocol    .setEnabled(isExternalDb);
      _lblConnectionUrl_Parameter   .setEnabled(isExternalDb);
      _lblDatabaseName              .setEnabled(isExternalDb);
      _lblDatabaseUsername          .setEnabled(isExternalDb);
      _lblDatabasePassword          .setEnabled(isExternalDb);
      _lblHibernateDialect          .setEnabled(isExternalDb);
      _lblJarDownloadUrl            .setEnabled(isExternalDb);
      _lblJdbcDriver                .setEnabled(isExternalDb);

      _txtConnectionUrl             .setEnabled(isExternalDb);
      _txtConnectionUrl_Protocol    .setEnabled(isExternalDb);
      _txtConnectionUrl_Parameter   .setEnabled(isExternalDb);
      _txtDatabaseName              .setEnabled(isExternalDb);
      _txtDatabaseUsername          .setEnabled(isExternalDb);
      _txtDatabasePassword          .setEnabled(isExternalDb);
      _txtHibernateDialect          .setEnabled(isExternalDb);
      _txtJarDownloadUrl            .setEnabled(isExternalDb);
      _txtJdbcDriver                .setEnabled(isExternalDb);

// SET_FORMATTING_ON
   }

   @Override
   public void init(final IWorkbench workbench) {

      setPreferenceStore(_prefStore);
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _fontItalic = JFaceResources.getFontRegistry().getItalic(JFaceResources.DIALOG_FONT);

      _defaultSpinnerWidth = _isLinux ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(_isOSX ? 14 : 7);
      _defaultInfoWidth = _pc.convertWidthInCharsToPixels(50);

      _defaultMouseWheelListener = mouseEvent -> UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
      _defaultListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> enableControls());
   }

   private void onModify() {

      final String fullUrl = _txtConnectionUrl_Protocol.getText() + _txtDatabaseName.getText() + _txtConnectionUrl_Parameter.getText();

      _txtConnectionUrl.setText(fullUrl);
   }

   @Override
   protected void performApply() {

      saveState();

      super.performApply();
   }

   @Override
   public boolean performCancel() {

      saveState_UI();

      return super.performCancel();
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

      final boolean isExternalDb = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_EXTERNAL);

      _tabFolder                    .setSelection(_prefStore.getInt(STATE_PREF_TOUR_SELECTED_TAB));

      _spinnerTourCacheSize         .setSelection(_prefStore.getInt(ITourbookPreferences.TOUR_CACHE_SIZE));

      // tour db system
      _rdoDbSystem_InternalH2       .setSelection(isExternalDb == false);
      _rdoDbSystem_External         .setSelection(isExternalDb);

      _txtConnectionUrl_Protocol    .setText(_prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_CONNECTION_URL_PROTOCOL));
      _txtConnectionUrl_Parameter   .setText(_prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_CONNECTION_URL_PARAMETERS));
      _txtDatabaseName              .setText(_prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_NAME));
      _txtDatabaseUsername          .setText(_prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_USERNAME));
      _txtDatabasePassword          .setText(_prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_PASSWORD));
      _txtHibernateDialect          .setText(_prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_HIBERNATE_DIALECT));
      _txtJarDownloadUrl            .setText(_prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_JAR_DOWNLOAD_URL));
      _txtJdbcDriver                .setText(_prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_JDBC_DRIVER));

// SET_FORMATTING_ON
   }

   private void saveState() {

// SET_FORMATTING_OFF

      _prefStore.setValue(ITourbookPreferences.TOUR_CACHE_SIZE,                                 _spinnerTourCacheSize.getSelection());
      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_IS_EXTERNAL,                       _rdoDbSystem_External.getSelection());

// SET_FORMATTING_ON

      saveState_ExternalDatabase();
      saveState_UI();
   }

   private void saveState_ExternalDatabase() {

// SET_FORMATTING_OFF

      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_CONNECTION_URL_PROTOCOL,     _txtConnectionUrl_Protocol    .getText().trim());
      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_CONNECTION_URL_PARAMETERS,   _txtConnectionUrl_Parameter   .getText().trim());
      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_NAME,               _txtDatabaseName              .getText().trim());
      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_USERNAME,           _txtDatabaseUsername          .getText().trim());
      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_PASSWORD,           _txtDatabasePassword          .getText().trim());
      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_HIBERNATE_DIALECT,           _txtHibernateDialect          .getText().trim());
      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_JAR_DOWNLOAD_URL,            _txtJarDownloadUrl            .getText().trim());
      _prefStore.setValue(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_JDBC_DRIVER,                 _txtJdbcDriver                .getText().trim());

// SET_FORMATTING_ON
   }

   private void saveState_UI() {

      if (_tabFolder == null || _tabFolder.isDisposed()) {
         return;
      }

      _prefStore.setValue(STATE_PREF_TOUR_SELECTED_TAB, _tabFolder.getSelectionIndex());
   }
}
