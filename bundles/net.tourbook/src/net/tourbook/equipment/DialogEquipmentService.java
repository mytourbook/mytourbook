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
import java.time.LocalDateTime;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.autocomplete.AutoComplete_ComboInputMT;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.data.EquipmentService;

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
 * Dialog to modify a {@link EquipmentService}
 */
public class DialogEquipmentService extends TitleAreaDialog {

   private static final String          ID                                         = "net.tourbook.equipment.DialogEquipmentService"; //$NON-NLS-1$

   private static final IDialogSettings _state                                     = TourbookPlugin.getState(ID);

   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_COMPANY    = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_COMPANY";       //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_NAME       = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_NAME";          //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT";    //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE       = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE";          //$NON-NLS-1$
   private static final String          STATE_PRICE_UNIT_DEFAULT                   = "STATE_PRICE_UNIT_DEFAULT";                      //$NON-NLS-1$

   /**
    * New or cloned instance
    */
   private EquipmentService             _service;
   private Equipment                    _serviceEquipment;

   private boolean                      _isDuplicateService;
   private boolean                      _isInUIUpdate;
   private boolean                      _isNewService;

   private ModifyListener               _defaultModifyListener;
   private SelectionListener            _defaultSelectionListener;
   private MouseWheelListener           _defaultMouseWheelListener;

   private boolean                      _isModified;

   private PixelConverter               _pc;

   /*
    * UI resources
    */
   private Image _imageDialog = TourbookPlugin.getImageDescriptor(Images.Equipment_Service).createImage();

   /*
    * UI controls
    */
   private Composite                 _container;
   private Composite                 _parent;

   private Button                    _chkCollate;

   private Combo                     _comboCompany;
   private Combo                     _comboName;
   private Combo                     _comboPriceUnit;
   private Combo                     _comboType;

   private DateTime                  _date;

   private Spinner                   _spinPrice;

   private Text                      _txtDescription;

   private AutoComplete_ComboInputMT _autocomplete_Company;
   private AutoComplete_ComboInputMT _autocomplete_Name;
   private AutoComplete_ComboInputMT _autocomplete_PriceUnit;
   private AutoComplete_ComboInputMT _autocomplete_Type;

   public DialogEquipmentService(final Shell parentShell,
                                 final Equipment equipment,
                                 final EquipmentService service,
                                 final boolean isDuplicate) {

      super(parentShell);

      _serviceEquipment = equipment;
      _isDuplicateService = isDuplicate;

      _isNewService = service == null;

      if (_isNewService) {

         _service = new EquipmentService();

      } else {

         _service = service.clone();

         if (isDuplicate) {

            // adjust date to today

            _service.setDate(TimeTools.nowInMilliseconds());
         }
      }

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);
      setDefaultImage(_imageDialog);
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      // set window title
      shell.setText("Equipment Service");
   }

   @Override
   public void create() {

      super.create();

      final String messageTitle =

            _isDuplicateService ? "Duplicate Service"
                  : _isNewService ? "Create Equipment Service"
                        : "Edit Equipment Service";

      setTitle(messageTitle);
      setMessage(_service.getName());
   }

   private void createActions() {

   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // OK -> Create/Save
      getButton(IDialogConstants.OK_ID).setText(_isNewService
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

      // ensure the UI is created
      _parent.getDisplay().asyncExec(() -> {

         enableControls();

         _comboName.setFocus();
      });

      return dlgContainer;
   }

   private void createUI(final Composite parent) {

      final GridDataFactory gdVertCenter = GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER);

      _container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_container);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(_container);
//      _container.setBackground(UI.SYS_COLOR_MAGENTA);
      {
         {
            /*
             * Name
             */

            final Label label = UI.createLabel(_container, "&Name");
            gdVertCenter.applyTo(label);

            // autocomplete combo
            _comboName = new Combo(_container, SWT.BORDER | SWT.FLAT);
            _comboName.setText(UI.EMPTY_STRING);
            _comboName.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_comboName);

            _autocomplete_Name = new AutoComplete_ComboInputMT(_comboName);
         }
         {
            /*
             * Company
             */

            final Label label = UI.createLabel(_container, "&Company");
            gdVertCenter.applyTo(label);

            // autocomplete combo
            _comboCompany = new Combo(_container, SWT.BORDER | SWT.FLAT);
            _comboCompany.setText(UI.EMPTY_STRING);
            _comboCompany.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_comboCompany);

            _autocomplete_Company = new AutoComplete_ComboInputMT(_comboCompany);
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
            _comboType.setToolTipText("With the type and date fields, tours are collated to display e.g. all kilometers for one part or one service");
            _comboType.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_comboType);

            _autocomplete_Type = new AutoComplete_ComboInputMT(_comboType);
         }
         {
            /*
             * Date
             */
            final Label label = UI.createLabel(_container, "D&ate");
            gdVertCenter.applyTo(label);

            _date = new DateTime(_container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
            _date.setToolTipText("With the type and date fields, tours are collated to display e.g. all kilometers for one part or one service");
            _date.addSelectionListener(_defaultSelectionListener);
         }
         UI.createSpacer_Horizontal(_container, 1);
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
//         UI.createSpacer_Horizontal(_container, 2);
         {
            /*
             * Collate tours
             */
            final Label label = UI.createLabel(_container, "Co&llate");
            gdVertCenter.applyTo(label);

            _chkCollate = new Button(_container, SWT.CHECK);
            _chkCollate.setText("Include in collated tours");
            _chkCollate.setToolTipText("Collated tours are a collection of tours to summarize,\ne.g. distance or duration values");
            _chkCollate.addSelectionListener(_defaultSelectionListener);
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
                  .span(2, 1)
                  .applyTo(_txtDescription);
         }
      }
   }

   private void enableControls() {

      if (_isInUIUpdate) {
         return;
      }

      final boolean isValid = _isNewService && _isModified == false

            // disable OK when new and not modified but do NOT display validation message
            ? false

            : isDataValid();

      // OK button
      getButton(IDialogConstants.OK_ID).setEnabled(isValid);
   }

   private void fillUI() {

// SET_FORMATTING_OFF

      UI.fillUI_Combobox(_comboCompany,   EquipmentManager.getCachedFields_AllCompanies());
      UI.fillUI_Combobox(_comboName,      EquipmentManager.getCachedFields_AllServiceNames());
      UI.fillUI_Combobox(_comboPriceUnit, EquipmentManager.getCachedFields_AllPriceUnits());
      UI.fillUI_Combobox(_comboType,      EquipmentManager.getCachedFields_AllTypes());

// SET_FORMATTING_ON
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
   }

   /**
    * @return Returns new or cloned instance
    */
   EquipmentService getService() {

      _service.updateUntilDate();

      return _service;
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

      final String name = _comboName.getText().trim();

      if (StringUtils.hasContent(name) == false) {

         setErrorMessage("Name cannot be empty");

         return false;
      }

      setErrorMessage(null);

      return true;
   }

   @Override
   protected void okPressed() {

      updateModelFromUI();

      if (_service.isValidForSave() == false) {

         // data are not valid to be saved which is done in the action which opened this dialog

         return;
      }

      super.okPressed();
   }

   private void onDispose() {

      UI.disposeResource(_imageDialog);

// SET_FORMATTING_OFF

      _autocomplete_Company   .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_COMPANY);
      _autocomplete_Name      .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_NAME);
      _autocomplete_PriceUnit .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT);
      _autocomplete_Type      .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE);

// SET_FORMATTING_ON

      _state.put(STATE_PRICE_UNIT_DEFAULT, _comboPriceUnit.getText().trim());
   }

   private void onModify() {

      if (_isInUIUpdate) {
         return;
      }

      _isModified = true;

      enableControls();
   }

   private void resoreState() {

      _isInUIUpdate = true;

// SET_FORMATTING_OFF
      _autocomplete_Company   .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_COMPANY);
      _autocomplete_Name      .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_NAME);
      _autocomplete_PriceUnit .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT);
      _autocomplete_Type      .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE);

// SET_FORMATTING_ON

      // set default unit
      _comboPriceUnit.setText(Util.getStateString(_state, STATE_PRICE_UNIT_DEFAULT, UI.EMPTY_STRING));

      _isInUIUpdate = false;
   }

   private void updateModelFromUI() {

// SET_FORMATTING_OFF

      final LocalDate date     = LocalDate.of(_date.getYear(), _date.getMonth() + 1, _date.getDay());

      _service.setEquipment(        _serviceEquipment);

      _service.setCompany(          _comboCompany.getText().trim());
      _service.setName(             _comboName.getText().trim());
      _service.setType(             _comboType.getText().trim());
      _service.setDescription(      _txtDescription.getText().trim());

      _service.setIsCollate(        _chkCollate.getSelection());
      _service.setPrice(            _spinPrice.getSelection() / 100f);
      _service.setPriceUnit(        _comboPriceUnit.getText());

      _service.setDate(             TimeTools.toEpochMilli(date));

// SET_FORMATTING_ON
   }

   private void updateUIFromModel() {

      _isInUIUpdate = true;

      LocalDateTime date = _service.getDate_Local();

      final long dateMS = TimeTools.toEpochMilli(date);

      if (dateMS == 0) {
         date = LocalDateTime.now();
      }

// SET_FORMATTING_OFF

      _comboCompany     .setText(_service.getCompany());
      _comboName        .setText(_service.getName());
      _comboType        .setText(_service.getType());

      _date             .setDate(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());

      _chkCollate       .setSelection(_service.isCollate());
      _spinPrice        .setSelection((int) (_service.getPrice()  * 100));

      _txtDescription   .setText(_service.getDescription());

// SET_FORMATTING_ON

      final String priceUnit = _service.getPriceUnit();
      if (StringUtils.hasContent(priceUnit)) {

         // overwrite default

         _comboPriceUnit.setText(priceUnit);
      }

      _isInUIUpdate = false;
   }

}
