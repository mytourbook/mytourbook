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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog to modify a {@link Equipment}
 */
public class DialogEquipment extends TitleAreaDialog {

   private static final String          ID                                         = "net.tourbook.equipment.DialogEquipment";     //$NON-NLS-1$

   private static final IDialogSettings _state                                     = TourbookPlugin.getState(ID);

   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND      = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND";      //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL      = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL";      //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_SIZE       = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_SIZE";       //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE       = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE";       //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT"; //$NON-NLS-1$
   private static final String          STATE_PRICE_UNIT_DEFAULT                   = "STATE_PRICE_UNIT_DEFAULT";                   //$NON-NLS-1$
   private static final String          STATE_SYNC_DATES                           = "STATE_SYNC_DATES";                           //$NON-NLS-1$

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

   private PixelConverter               _pc;

   /*
    * UI resources
    */
   private Image _imageDialog = TourbookPlugin.getImageDescriptor(Images.Equipment_Only).createImage();

   /*
    * UI controls
    */
   private Composite                 _container;
   private Composite                 _parent;

   private Button                    _chkSyncDates;

   private Combo                     _comboBrand;
   private Combo                     _comboModel;
   private Combo                     _comboPriceUnit;
   private Combo                     _comboSize;
   private Combo                     _comboType;

   private DateTime                  _dateBuilt;
   private DateTime                  _dateFirstUse;
   private DateTime                  _dateRetired;

   private Spinner                   _spinDistance;
   private Spinner                   _spinPrice;
   private Spinner                   _spinWeight;

   private Text                      _txtDescription;

   private AutoComplete_ComboInputMT _autocomplete_Brand;
   private AutoComplete_ComboInputMT _autocomplete_Model;
   private AutoComplete_ComboInputMT _autocomplete_PriceUnit;
   private AutoComplete_ComboInputMT _autocomplete_Size;
   private AutoComplete_ComboInputMT _autocomplete_Type;

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
      shell.setText("Equipment");
   }

   @Override
   public void create() {

      super.create();

      final String messageTitle = _isNewEquipment
            ? "New Equipment"
            : "Edit Equipment";

      setTitle(messageTitle);
      setMessage(_equipment.getName());
   }

   private void createActions() {

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

      createActions();

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
      GridLayoutFactory.swtDefaults().numColumns(7).applyTo(_container);
//      _container.setBackground(UI.SYS_COLOR_GREEN);
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

            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_comboBrand);

            _autocomplete_Brand = new AutoComplete_ComboInputMT(_comboBrand);
         }
         // force more space between the 2 data columns
         UI.createSpacer_Horizontal(_container, _pc.convertWidthInCharsToPixels(3), 1);
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

            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_comboModel);

            _autocomplete_Model = new AutoComplete_ComboInputMT(_comboModel);
         }
         {
            /*
             * Type
             */
            final Label label = UI.createLabel(_container, "T&ype");
            gdVertCenter.applyTo(label);

            // autocomplete combo
            _comboType = new Combo(_container, SWT.BORDER | SWT.FLAT);
            _comboType.setText(UI.EMPTY_STRING);
            _comboType.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_comboType);

            _autocomplete_Type = new AutoComplete_ComboInputMT(_comboType);
         }
         UI.createSpacer_Horizontal(_container, 1);
         {
            /*
             * Size
             */
            final Label label = UI.createLabel(_container, "Si&ze");
            gdVertCenter.applyTo(label);

            // autocomplete combo
            _comboSize = new Combo(_container, SWT.BORDER | SWT.FLAT);
            _comboSize.setText(UI.EMPTY_STRING);
            _comboSize.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_comboSize);

            _autocomplete_Size = new AutoComplete_ComboInputMT(_comboSize);
         }
         {
            /*
             * Price
             */

            UI.createLabel(_container, "&Price");

            // spinner
            _spinPrice = new Spinner(_container, SWT.BORDER);

            _spinPrice.setDigits(2);
            _spinPrice.setMinimum(-1_000_000_000);
            _spinPrice.setMaximum(1_000_000_000);

            _spinPrice.addMouseWheelListener(_defaultMouseWheelListener);
            _spinPrice.addSelectionListener(_defaultSelectionListener);

            // autocomplete combo
            _comboPriceUnit = new Combo(_container, SWT.BORDER | SWT.FLAT);
            _comboPriceUnit.setText(UI.EMPTY_STRING);
            _comboPriceUnit.setToolTipText("Currency");
            _comboPriceUnit.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .hint(_pc.convertWidthInCharsToPixels(4), SWT.DEFAULT)
                  .applyTo(_comboPriceUnit);

            _autocomplete_PriceUnit = new AutoComplete_ComboInputMT(_comboPriceUnit);
         }
         UI.createSpacer_Horizontal(_container, 1);
         {
            /*
             * First use date
             */
            final Label label = UI.createLabel(_container, Messages.Dialog_Equipment_Label_DateFirstUse);
            gdVertCenter.applyTo(label);

            _dateFirstUse = new DateTime(_container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
            _dateFirstUse.addSelectionListener(_defaultSelectionListener);
         }
         UI.createSpacer_Horizontal(_container, 1);
         {
            /*
             * Weight
             */

            UI.createLabel(_container, "&Weight");

            // spinner
            _spinWeight = new Spinner(_container, SWT.BORDER);

            _spinWeight.setDigits(3);
            _spinWeight.setMinimum(0);
            _spinWeight.setMaximum(1_000_000_000);

            _spinWeight.addMouseWheelListener(_defaultMouseWheelListener);
            _spinWeight.addSelectionListener(_defaultSelectionListener);

            // label: kg
            UI.createLabel(_container, UI.UNIT_LABEL_WEIGHT);
         }
         UI.createSpacer_Horizontal(_container, 1);
         {
            /*
             * Built date
             */
            final Label label = UI.createLabel(_container, Messages.Dialog_Equipment_Label_DateBuilt);
            gdVertCenter.applyTo(label);

            _dateBuilt = new DateTime(_container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
            _dateBuilt.addSelectionListener(_defaultSelectionListener);

            _chkSyncDates = new Button(_container, SWT.CHECK);
            _chkSyncDates.setText("Sy&nc");
            _chkSyncDates.setToolTipText("Sync built date with first use date");
            _chkSyncDates.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Distance first use
             */

            final Label label = UI.createLabel(_container, "Initial d&istance");
            label.setToolTipText("Distance by first use");

            // spinner
            _spinDistance = new Spinner(_container, SWT.BORDER);

            _spinDistance.setDigits(0);
            _spinDistance.setMinimum(0);
            _spinDistance.setMaximum(1_000_000_000);

            _spinDistance.addMouseWheelListener(_defaultMouseWheelListener);
            _spinDistance.addSelectionListener(_defaultSelectionListener);

            // label: km/mi
            UI.createLabel(_container, UI.UNIT_LABEL_DISTANCE);
         }
         UI.createSpacer_Horizontal(_container, 1);
         {
            /*
             * Retired date
             */
            final Label label = UI.createLabel(_container, Messages.Dialog_Equipment_Label_DateRetired);
            gdVertCenter.applyTo(label);

            _dateRetired = new DateTime(_container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
            _dateRetired.addSelectionListener(_defaultSelectionListener);
         }
         UI.createSpacer_Horizontal(_container, 1);
//         UI.createSpacer_Horizontal(_container, 1);
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
                  .hint(convertWidthInCharsToPixels(40), convertHeightInCharsToPixels(10))
                  .span(6, 1)
                  .applyTo(_txtDescription);
         }
      }

      // set tab ordering, cool feature but all controls MUST have the same parent !!!
      _container.setTabList(new Control[] {

            _comboBrand,
            _comboModel,
            _comboType,
            _comboSize,

            _spinPrice,
            _comboPriceUnit,
            _spinWeight,
            _spinDistance,

            _dateFirstUse,
            _dateBuilt,
            _chkSyncDates,
            _dateRetired,

            _txtDescription
      });
   }

   private void enableControls() {

      if (_isInUIUpdate) {
         return;
      }

      final boolean isSyncDates = _chkSyncDates.getSelection();
      final boolean canEditBuiltDate = isSyncDates == false;

      _dateBuilt.setEnabled(canEditBuiltDate);

      final boolean isValid = _isNewEquipment && _isModified == false

            // disable OK when new and not modified but do NOT display validation message
            ? false

            : isDataValid();

      // OK button
      getButton(IDialogConstants.OK_ID).setEnabled(isValid);
   }

   private void fillUI() {

      // fill brand combobox
      final ConcurrentSkipListSet<String> allBrands = EquipmentManager.getCachedFields_AllBrands();

      for (final String brand : allBrands) {
         if (brand != null) {
            _comboBrand.add(brand);
         }
      }

      // fill model combobox
      final ConcurrentSkipListSet<String> allModels = EquipmentManager.getCachedFields_AllModels();

      for (final String model : allModels) {
         if (model != null) {
            _comboModel.add(model);
         }
      }

      // fill price unit combobox
      final ConcurrentSkipListSet<String> allPriceUnits = EquipmentManager.getCachedFields_AllPriceUnits();

      for (final String model : allPriceUnits) {
         if (model != null) {
            _comboPriceUnit.add(model);
         }
      }

      // fill size combobox
      final ConcurrentSkipListSet<String> allSizes = EquipmentManager.getCachedFields_AllSizes();

      for (final String size : allSizes) {
         if (size != null) {
            _comboSize.add(size);
         }
      }

      // fill type combobox
      final ConcurrentSkipListSet<String> allTypes = EquipmentManager.getCachedFields_AllTypes();

      for (final String type : allTypes) {
         if (type != null) {
            _comboType.add(type);
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

      _pc = new PixelConverter(_parent);

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

      saveState();

      UI.disposeResource(_imageDialog);
   }

   private void onModify() {

      if (_isInUIUpdate) {
         return;
      }

      _isModified = true;

      final boolean isSyncDates = _chkSyncDates.getSelection();

      if (isSyncDates) {

         _dateBuilt.setDate(_dateFirstUse.getYear(), _dateFirstUse.getMonth(), _dateFirstUse.getDay());
      }

      enableControls();
   }

   private void resoreState() {

      _isInUIUpdate = true;

      _autocomplete_Brand.restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND);
      _autocomplete_Model.restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL);
      _autocomplete_PriceUnit.restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT);
      _autocomplete_Size.restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_SIZE);
      _autocomplete_Type.restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE);

      // set also default unit
      _comboPriceUnit.setText(Util.getStateString(_state, STATE_PRICE_UNIT_DEFAULT, UI.EMPTY_STRING));

      _chkSyncDates.setSelection(Util.getStateBoolean(_state, STATE_SYNC_DATES, true));

      _isInUIUpdate = false;
   }

   private void saveState() {

      _autocomplete_Brand.saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND);
      _autocomplete_Model.saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL);
      _autocomplete_PriceUnit.saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT);
      _autocomplete_Size.saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_SIZE);
      _autocomplete_Type.saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE);

      _state.put(STATE_PRICE_UNIT_DEFAULT, _comboPriceUnit.getText().trim());
      _state.put(STATE_SYNC_DATES, _chkSyncDates.getSelection());
   }

   private void updateModelFromUI() {

// SET_FORMATTING_OFF

      final LocalDate dateBuilt     = LocalDate.of(_dateBuilt.getYear(),      _dateBuilt.getMonth() + 1,    _dateBuilt.getDay());
      final LocalDate dateFirstUse  = LocalDate.of(_dateFirstUse.getYear(),   _dateFirstUse.getMonth() + 1, _dateFirstUse.getDay());
      final LocalDate dateRetired   = LocalDate.of(_dateRetired.getYear(),    _dateRetired.getMonth() + 1,  _dateRetired.getDay());

      _equipment.setDateBuilt(   dateBuilt.toEpochDay());
      _equipment.setDateFirstUse(dateFirstUse.toEpochDay());
      _equipment.setDateRetired( dateRetired.toEpochDay());

      _equipment.setBrand(             _comboBrand.getText().trim());
      _equipment.setModel(             _comboModel.getText().trim());
      _equipment.setType(              _comboType.getText().trim());
      _equipment.setDescription(       _txtDescription.getText().trim());

      _equipment.setDistanceFirstUse(  _spinDistance.getSelection());
      _equipment.setPrice(             _spinPrice.getSelection() / 100f);
      _equipment.setPriceUnit(         _comboPriceUnit.getText());
      _equipment.setSize(              _comboSize.getText().trim());
      _equipment.setWeight(            _spinWeight.getSelection() / 1000f);

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

      _comboBrand       .setText(_equipment.getBrand());
      _comboModel       .setText(_equipment.getModel());
      _comboSize        .setText(_equipment.getSize());
      _comboType        .setText(_equipment.getType());

      _dateBuilt        .setDate(dateBuilt.getYear(),    dateBuilt.getMonthValue() - 1,      dateBuilt.getDayOfMonth());
      _dateFirstUse     .setDate(dateFirstUse.getYear(), dateFirstUse.getMonthValue() - 1,   dateFirstUse.getDayOfMonth());
      _dateRetired      .setDate(dateRetired.getYear(),  dateRetired.getMonthValue() - 1,    dateRetired.getDayOfMonth());

      _spinDistance     .setSelection((int) (_equipment.getDistanceFirstUse()));
      _spinPrice        .setSelection((int) (_equipment.getPrice()  * 100));
      _spinWeight       .setSelection((int) (_equipment.getWeight() * 1000));

      _txtDescription   .setText(_equipment.getDescription());

// SET_FORMATTING_ON

      final String priceUnit = _equipment.getPriceUnit();
      if (StringUtils.hasContent(priceUnit)) {

         // overwrite default

         _comboPriceUnit.setText(priceUnit);
      }

      _isInUIUpdate = false;
   }

}
