/*******************************************************************************
 * Copyright (C) 2025, 2026 Wolfgang Schramm and Contributors
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

import java.time.LocalDate;
import java.util.concurrent.ConcurrentSkipListSet;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.autocomplete.AutoComplete_ComboInputMT;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourTag;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog to modify a {@link TourTag}
 */
public class DialogEquipment extends TitleAreaDialog {

   private static final String          ID                                    = "net.tourbook.equipment.DialogEquipment"; //$NON-NLS-1$

   private static final IDialogSettings _state                                = TourbookPlugin.getState(ID);

   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND";  //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL";  //$NON-NLS-1$

   /**
    * New or cloned instance
    */
   private Equipment                    _equipment;

   private boolean                      _isInUIUpdate;
   private boolean                      _isNewEquipment;

   private ModifyListener               _defaultModifyListener;
   private SelectionListener            _defaultSelectionListener;
   private MouseWheelListener           _defaultMouseWheelListener;

   private boolean                      _isModified;

   /*
    * UI resources
    */
   private Image _imageDialog = TourbookPlugin.getImageDescriptor(Images.Equipment).createImage();

   /*
    * UI controls
    */
   private Composite                 _container;
   private Composite                 _parent;

   private Text                      _txtDescription;

   private Combo                     _comboBrand;
   private Combo                     _comboModel;

   private DateTime                  _dateBuilt;
   private DateTime                  _dateFirstUse;
   private DateTime                  _dateRetired;

   private Spinner                   _spinDistance;
   private Spinner                   _spinWeight;

   private AutoComplete_ComboInputMT _autocomplete_Brand;
   private AutoComplete_ComboInputMT _autocomplete_Model;

   public DialogEquipment(final Shell parentShell, final Equipment equipment) {

      super(parentShell);

      _isNewEquipment = equipment == null;

      if (_isNewEquipment) {

         _equipment = new Equipment();

      } else {

         _equipment = equipment.clone();
      }

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);
      setDefaultImage(_imageDialog);
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      // set window title
      shell.setText(Messages.Dialog_Equipment_Title);
   }

   @Override
   public void create() {

      super.create();

      final String newTitle = _isNewEquipment
            ? Messages.Dialog_Equipment_Message_Equipment_New
            : Messages.Dialog_Equipment_Message_Equipment_Edit.formatted(_equipment.getName());

      setTitle(newTitle);

//      setMessage("message");
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // OK -> Create/Save
      getButton(IDialogConstants.OK_ID).setText(_isNewEquipment
            ? Messages.App_Action_Create
            : Messages.App_Action_Save);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      _parent = parent;

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      initUI();

      createUI(dlgContainer);

      fillUI();

      resoreState();

      updateUIFromModel();

      _comboBrand.setFocus();

      // ensure the UI is created
      _parent.getDisplay().asyncExec(() -> {

         enableControls();
      });

      return dlgContainer;
   }

   private void createUI(final Composite parent) {

      final GridDataFactory gdVertCenter = GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER);

      _container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_container);
      GridLayoutFactory.swtDefaults().numColumns(4).applyTo(_container);
//      _container.setBackground(UI.SYS_COLOR_CYAN);
      {
         {
            /*
             * Brand/name
             */

            final Label label = UI.createLabel(_container, Messages.Dialog_Equipment_Label_Brand, Messages.Dialog_Equipment_Label_Brand_Tooltip);
            gdVertCenter.applyTo(label);

            // autocomplete combo
            _comboBrand = new Combo(_container, SWT.BORDER | SWT.FLAT);
            _comboBrand.setText(UI.EMPTY_STRING);
            _comboBrand.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(_comboBrand);

            _autocomplete_Brand = new AutoComplete_ComboInputMT(_comboBrand);
         }
         {
            /*
             * Model/subname
             */
            final Label label = UI.createLabel(_container, Messages.Dialog_Equipment_Label_Model);
            gdVertCenter.applyTo(label);

            // autocomplete combo
            _comboModel = new Combo(_container, SWT.BORDER | SWT.FLAT);
            _comboModel.setText(UI.EMPTY_STRING);
            _comboModel.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(_comboModel);

            _autocomplete_Model = new AutoComplete_ComboInputMT(_comboModel);
         }
         {
            /*
             * Built date
             */
            final Label label = UI.createLabel(_container, Messages.Dialog_Equipment_Label_DateBuilt);
            gdVertCenter.applyTo(label);

            _dateBuilt = new DateTime(_container, SWT.DATE | SWT.MEDIUM | SWT.BORDER);
            _dateBuilt.addSelectionListener(_defaultSelectionListener);

            UI.createSpacer_Horizontal(_container, 2);
         }
         {
            /*
             * First use date
             */
            final Label label = UI.createLabel(_container, Messages.Dialog_Equipment_Label_DateFirstUse);
            gdVertCenter.applyTo(label);

            _dateFirstUse = new DateTime(_container, SWT.DATE | SWT.MEDIUM | SWT.BORDER);
            _dateFirstUse.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Retired date
             */
            final Label label = UI.createLabel(_container, Messages.Dialog_Equipment_Label_DateRetired);
            gdVertCenter.applyTo(label);

            _dateRetired = new DateTime(_container, SWT.DATE | SWT.MEDIUM);
            _dateRetired.addSelectionListener(_defaultSelectionListener);
         }
//         private float                      weight;
//         private float                      distanceFirstUse;

         {
            /*
             * Weight
             */

            UI.createLabel(_container, "&Weight");

            final Composite containerWeight = new Composite(_container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerWeight);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerWeight);
            {
               // spinner
               _spinWeight = new Spinner(containerWeight, SWT.BORDER);

               _spinWeight.setDigits(3);
               _spinWeight.setMinimum(0);
               _spinWeight.setMaximum(1_000_000_000);

               _spinWeight.addMouseWheelListener(_defaultMouseWheelListener);

               // label: kg
               UI.createLabel(containerWeight, UI.UNIT_LABEL_WEIGHT);
            }
         }
         {
            /*
             * Distance first use
             */

            final Label label = UI.createLabel(_container, "D&istance");
            label.setToolTipText("Distance by first use");

            final Composite containerWeight = new Composite(_container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerWeight);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerWeight);
            {
               // spinner
               _spinDistance = new Spinner(containerWeight, SWT.BORDER);

               _spinDistance.setDigits(0);
               _spinDistance.setMinimum(0);
               _spinDistance.setMaximum(1_000_000_000);

               _spinDistance.addMouseWheelListener(_defaultMouseWheelListener);

               // label: km
               UI.createLabel(containerWeight, UI.UNIT_LABEL_DISTANCE);
            }
         }
         {
            /*
             * Description
             */
            final Label label = UI.createLabel(_container, Messages.Dialog_Equipment_Label_Description);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

            _txtDescription = new Text(_container, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            _txtDescription.addModifyListener(e -> onModify());
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .hint(convertWidthInCharsToPixels(100), convertHeightInCharsToPixels(20))
                  .span(3, 1)
                  .applyTo(_txtDescription);
         }
      }
   }

   private void enableControls() {

      if (_isInUIUpdate) {
         return;
      }

      final boolean isValid = _isNewEquipment && _isModified == false

            // disable OK when new and not modified but do NOT display validation message
            ? false

            : isDataValid();

      // OK button
      getButton(IDialogConstants.OK_ID).setEnabled(isValid);
   }

   private void fillUI() {

      // fill brand combobox
      final ConcurrentSkipListSet<String> allBrands = EquipmentManager.getCachedFields_AllEquipment_Brands();

      for (final String brand : allBrands) {
         if (brand != null) {
            _comboBrand.add(brand);
         }
      }

      // fill model combobox
      final ConcurrentSkipListSet<String> allModels = EquipmentManager.getCachedFields_AllEquipment_Models();

      for (final String model : allModels) {
         if (model != null) {
            _comboModel.add(model);
         }
      }
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
   }

   /**
    * @return Returns new or cloned instance
    */
   Equipment getEquipment() {

      return _equipment;
   }

   private void initUI() {

      _parent.addDisposeListener(disposeEvent -> onDispose());

      _defaultModifyListener = modifyEvent -> onModify();

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModify());

      _defaultMouseWheelListener = mouseEvent -> {

         Util.adjustSpinnerValueOnMouseScroll(mouseEvent);

         onModify();
      };
   }

   private boolean isDataValid() {

      final String brand = _comboBrand.getText().trim();
      final String model = _comboModel.getText().trim();

      if (StringUtils.hasContent(brand) == false && StringUtils.hasContent(model) == false) {

         setErrorMessage("Brand and model cannot be empty");

         return false;
      }

      setErrorMessage(null);

      return true;
   }

   @Override
   protected void okPressed() {

      updateModelFromUI();

      if (_equipment.isValidForSave() == false) {

         // data are not valid to be saved which is done in the action which opened this dialog

         return;
      }

      super.okPressed();
   }

   private void onDispose() {

      UI.disposeResource(_imageDialog);

      _autocomplete_Brand.saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND);
      _autocomplete_Model.saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL);
   }

   private void onModify() {

      if (_isInUIUpdate) {
         return;
      }

      _isModified = true;

      enableControls();
   }

   private void resoreState() {

      _autocomplete_Brand.restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND);
      _autocomplete_Model.restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL);
   }

   private void updateModelFromUI() {

// SET_FORMATTING_OFF

      final LocalDate dateBuilt     = LocalDate.of(_dateBuilt.getYear(),      _dateBuilt.getMonth() + 1,    _dateBuilt.getDay());
      final LocalDate dateFirstUse  = LocalDate.of(_dateFirstUse.getYear(),   _dateFirstUse.getMonth() + 1, _dateFirstUse.getDay());
      final LocalDate dateRetired   = LocalDate.of(_dateRetired.getYear(),    _dateRetired.getMonth() + 1,  _dateRetired.getDay());

      _equipment.setBrand(       _comboBrand.getText().trim());
      _equipment.setModel(       _comboModel.getText().trim());
      _equipment.setDescription( _txtDescription.getText().trim());

      _equipment.setDateBuilt(   dateBuilt.toEpochDay());
      _equipment.setDateFirstUse(dateFirstUse.toEpochDay());
      _equipment.setDateRetired( dateRetired.toEpochDay());

// SET_FORMATTING_ON
   }

   private void updateUIFromModel() {

      _isInUIUpdate = true;

// SET_FORMATTING_OFF

      LocalDate dateBuilt     = _equipment.getDateBuilt();
      LocalDate dateFirstUse  = _equipment.getDateFirstUse();
      LocalDate dateRetired   = _equipment.getDateRetired();

      final long epochDayBuilt      = dateBuilt.toEpochDay();
      final long epochDayFirstUse   = dateFirstUse.toEpochDay();
      final long epochDayRetired    = dateRetired.toEpochDay();

      if (epochDayBuilt == 0) {
         dateBuilt = LocalDate.now();
      }

      if (epochDayFirstUse == 0) {
         dateFirstUse = LocalDate.now();
      }

      if (epochDayRetired == 0) {
         dateRetired = LocalDate.of(2099,1,1);
      }

      _dateBuilt        .setDate(dateBuilt.getYear(),    dateBuilt.getMonthValue() - 1,      dateBuilt.getDayOfMonth());
      _dateFirstUse     .setDate(dateFirstUse.getYear(), dateFirstUse.getMonthValue() - 1,   dateFirstUse.getDayOfMonth());
      _dateRetired      .setDate(dateRetired.getYear(),  dateRetired.getMonthValue() - 1,    dateRetired.getDayOfMonth());

      _comboBrand       .setText(_equipment.getBrand());
      _comboModel       .setText(_equipment.getModel());
      _txtDescription   .setText(_equipment.getDescription());

// SET_FORMATTING_ON

      _isInUIUpdate = false;
   }

}
