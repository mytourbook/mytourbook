/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.sensors;

import de.byteholder.geoclipse.map.UI;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.DeviceSensor;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog to modify a {@link DeviceSensor}
 */
public class DialogSensor extends TitleAreaDialog {

   private static final String   ID     = "net.tourbook.ui.views.sensors.DialogSensor.ID"; //$NON-NLS-1$

   private final IDialogSettings _state = TourbookPlugin.getState(ID);

   private DeviceSensor          _sensor_Original;
   private DeviceSensor          _sensor_Clone;

   /*
    * UI controls
    */
   private Text _txtDescription;
   private Text _txtName;

   public DialogSensor(final Shell parentShell, final DeviceSensor sensor) {

      super(parentShell);

      _sensor_Original = sensor;
      _sensor_Clone = sensor.clone();

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      // set window title
      shell.setText(Messages.Dialog_Sensor_EditSensor_Title);
   }

   @Override
   public void create() {

      super.create();

      final StringBuilder sb = new StringBuilder();

      final String sensorName = _sensor_Original.getSensorName();
      if (sensorName != null && sensorName.length() > 0) {
         sb.append(sensorName);
      } else {
         sb.append(_sensor_Original.getProductName() + UI.DASH_WITH_SPACE + _sensor_Original.getManufacturerName());
      }

      setTitle(sb.toString());
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // OK -> Save
      getButton(IDialogConstants.OK_ID).setText(Messages.App_Action_Save);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(dlgContainer);

      restoreState();

      _txtName.selectAll();
      _txtName.setFocus();

      return dlgContainer;
   }

   /**
    * create the drop down menus, this must be created after the parent control is created
    */

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
      {
         {
            // Text: Name

            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Dialog_Label_Name);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

            _txtName = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtName);
         }
         {
            // Text: Notes

            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Dialog_Label_Description);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

            _txtDescription = new Text(container, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .hint(convertWidthInCharsToPixels(100), convertHeightInCharsToPixels(20))
                  .applyTo(_txtDescription);
         }
      }
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
   }

   @Override
   protected void okPressed() {

      // set model from UI
      saveState();

      if (_sensor_Clone.isValidForSave() == false) {
         return;
      }

      // update original model
      _sensor_Original.updateFromModified(_sensor_Clone);

      super.okPressed();
   }

   private void restoreState() {

      _txtName.setText(_sensor_Clone.getSensorName());
      _txtDescription.setText(_sensor_Clone.getDescription());
   }

   private void saveState() {

      _sensor_Clone.setDescription(_txtDescription.getText());
      _sensor_Clone.setSensorName(_txtName.getText());
   }
}
