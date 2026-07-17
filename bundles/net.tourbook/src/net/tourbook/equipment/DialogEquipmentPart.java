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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.autocomplete.AutoComplete_ComboInputMT;
import net.tourbook.common.measurement_system.MeasurementSystem;
import net.tourbook.common.measurement_system.MeasurementSystem_Manager;
import net.tourbook.common.measurement_system.Unit_Weight;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.ImageUtils;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.data.EquipmentPart;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.web.WEB;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Dialog to modify an {@link EquipmentPart}
 */
public class DialogEquipmentPart extends TitleAreaDialog {

   private static final String          ID                                            = "net.tourbook.equipment.DialogEquipmentPart";    //$NON-NLS-1$

   private static final IDialogSettings _state                                        = TourbookPlugin.getState(ID);

   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND         = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND";         //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_COLLATE_ID    = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_COLLATE_ID";    //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_INITIAL_VALUE = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_INITIAL_VALUE"; //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL         = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL";         //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT    = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT";    //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_SIZE          = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_SIZE";          //$NON-NLS-1$
   private static final String          STATE_IMAGE_LAST_SELECTED_PATH                = "STATE_IMAGE_LAST_SELECTED_PATH";                //$NON-NLS-1$
   private static final String          STATE_PRICE_UNIT_DEFAULT                      = "STATE_PRICE_UNIT_DEFAULT";                      //$NON-NLS-1$
   private static final String          STATE_SYNC_DATES                              = "STATE_SYNC_DATES";                              //$NON-NLS-1$

   /**
    * New or cloned instance
    */
   private EquipmentPart                _part;
   private Equipment                    _partEquipment;

   private boolean                      _isDuplicatePart;
   private boolean                      _isInUIUpdate;
   private boolean                      _isNewPart;

   private ModifyListener               _defaultModifyListener;
   private SelectionListener            _defaultSelectionListener;
   private MouseWheelListener           _defaultMouseWheelListener;
   private ITourEventListener           _tourEventListener;

   private boolean                      _isModified;

   private String                       _imageFilePath;

   private int                          _defaultDescriptionWidth;
   private int                          _defaultEquipmentWidth;
   private int                          _currencyWidth;
   private int                          _decoratorDistance;
   private GridDataFactory              _gdVertCenter;
   private String                       _tooltip1;
   private String                       _tooltip2;

   private ToolBarManager               _toolbarManager_ImageSize;

   private ActionSlideout_SetImageSize  _actionSlideout_SetImageSize;

   /**
    * Contains the controls which are displayed in the first column, these controls are used to get
    * the maximum width and set the first column within the different section to the same width
    */
   private final ArrayList<Control>     _firstColumnControls                          = new ArrayList<>();

   /*
    * UI resources
    */
   private Image _decorationImageInfo;

   private Image _imageCamera;
   private Image _imageNow;
   private Image _imageTrash;

   // must be created eraly
   private Image _imageDialog = TourbookPlugin.getImageDescriptor(Images.Equipment_Part).createImage();

   /*
    * UI controls
    */
   private Composite                 _uiContainer;
   private Composite                 _parent;

   private Button                    _btnDateRetiredNow;
   private Button                    _btnDeleteImage;
   private Button                    _btnSelectImage;
   private Button                    _chkCollate;
   private Button                    _chkRetired;
   private Button                    _chkSyncDates;
   private Button                    _rdoCollateWith_Previous;
   private Button                    _rdoCollateWith_Next;

   private Combo                     _comboBrand;
   private Combo                     _comboCollateID;
   private Combo                     _comboInitialValueUnit;
   private Combo                     _comboModel;
   private Combo                     _comboPriceUnit;
   private Combo                     _comboSize;
   private Combo                     _comboWeightUnit;

   private DateTime                  _dateBuilt;
   private DateTime                  _datePurchased;
   private DateTime                  _dateRetired;
   private DateTime                  _dateUsed;

   private Label                     _canvasEquipmentImage;

   private Label                     _lblCollate;
   private Label                     _lblCollateID;
   private Label                     _lblCollateWith;
   private Label                     _lblImage;

   private Link                      _linkWebsite;

   private Spinner                   _spinDistance;
   private Spinner                   _spinInitialValue;
   private Spinner                   _spinPrice;
   private Spinner                   _spinWeight;

   private Text                      _txtDescription;
   private Text                      _txtImageFilePath;
   private Text                      _txtPurchaseLocation;
   private Text                      _txtUrlAddress;

   private AutoComplete_ComboInputMT _autocomplete_Brand;
   private AutoComplete_ComboInputMT _autocomplete_CollateID;
   private AutoComplete_ComboInputMT _autocomplete_InitialValueUnit;
   private AutoComplete_ComboInputMT _autocomplete_Model;
   private AutoComplete_ComboInputMT _autocomplete_PriceUnit;
   private AutoComplete_ComboInputMT _autocomplete_Size;

   private ControlDecoration         _comboDecorator_Collate;
   private ControlDecoration         _comboDecorator_DateFrom;
   private ControlDecoration         _comboDecorator_CollateID;

   private class ActionSlideout_SetImageSize extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutEquipment_SetImageSize(_parent, toolbar);
      }
   }

   public DialogEquipmentPart(final Shell parentShell,
                              final Equipment equipment,
                              final EquipmentPart part,
                              final boolean isDuplicatePart) {

      super(parentShell);

      _partEquipment = equipment;

      _isNewPart = part == null;
      _isDuplicatePart = isDuplicatePart;

      if (_isNewPart) {

         _part = new EquipmentPart(EquipmentPart.ITEM_TYPE_PART);

         _part.setCollateBetween(EquipmentPart.COLLATED_WITH_NEXT);

      } else {

         _part = part.clone();

         _part.setName_Service(null);
         _part.resetPartName();

         if (isDuplicatePart) {

            // adjust date to today

            final long today = TimeTools.nowInMilliseconds();

            _part.setDateUsed(today);
            _part.setDateBuilt(today);
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
      shell.setText(Messages.Dialog_EquipmentPart_Dialog_Title);
   }

   @Override
   public void create() {

      super.create();

      final String messageTitle = _isDuplicatePart

            ? Messages.Dialog_EquipmentPart_Dialog_Message_Part_Duplicate
            : _isNewPart

                  ? Messages.Dialog_EquipmentPart_Dialog_Message_Part_New
                  : Messages.Dialog_EquipmentPart_Dialog_Message_Part_Edit;

      setTitle(messageTitle.formatted(_partEquipment.getName()));
      setMessage(_part.getName_Combined());
   }

   private void createActions() {

      _actionSlideout_SetImageSize = new ActionSlideout_SetImageSize();
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // OK -> Create/Save
      getButton(IDialogConstants.OK_ID).setText(_isNewPart || _isDuplicatePart
            ? Messages.App_Action_Create
            : Messages.App_Action_Save);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      _parent = parent;

      setupImageListener(parent);

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      initUI();

      createActions();

      createUI(dlgContainer);

      fillUI();

      updateUI_FromModel();

      resoreState();

      // compute width for all controls and equalize column width for the first column
      UI.setEqualizeColumWidths(_firstColumnControls);
      _uiContainer.layout(true, true);

// FOR DEBUGGING
//    _date.setFocus();

      _comboBrand.setFocus();

      // ensure the UI is created
      _parent.getDisplay().asyncExec(() -> {

         // !!! MUST BE DONE VERY LATE, OTHERWISE THERE ARE ISSUES !!!  ?????????? need to be checked
         _toolbarManager_ImageSize.update(true);

         enableControls();
      });

      return dlgContainer;
   }

   private void createUI(final Composite parent) {

      _uiContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_uiContainer);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(_uiContainer);
//      _container.setBackground(UI.SYS_COLOR_GREEN);

      createUI_100_Top(_uiContainer);

      UI.createSpacer_Vertical(_uiContainer, 8, 3);

      createUI_200_Col1(_uiContainer);
      UI.createSpacer_Horizontal(_uiContainer, 10, 1);
      createUI_300_Col2(_uiContainer);

      createUI_900_Bottom(_uiContainer);
   }

   private void createUI_100_Top(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_GREEN);
      {
         {
            /*
             * Brand/name
             */

            final Label label = UI.createLabel(container, Messages.Dialog_Equipment_Label_Brand, Messages.Dialog_Equipment_Label_Brand_Tooltip);
            _gdVertCenter.applyTo(label);
            _firstColumnControls.add(label);

            // autocomplete combo
            _comboBrand = new Combo(container, SWT.BORDER | SWT.FLAT);
            _comboBrand.setText(UI.EMPTY_STRING);
            _comboBrand.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  // this will force that both columns have the same default width
                  .hint(_defaultEquipmentWidth, SWT.DEFAULT)
                  .applyTo(_comboBrand);

            _autocomplete_Brand = new AutoComplete_ComboInputMT(_comboBrand);
         }
         {
            /*
             * Model/subname
             */
            final Label label = UI.createLabel(container, Messages.Dialog_Equipment_Label_Model);
            _gdVertCenter.applyTo(label);
            _firstColumnControls.add(label);

            // autocomplete combo
            _comboModel = new Combo(container, SWT.BORDER | SWT.FLAT);
            _comboModel.setText(UI.EMPTY_STRING);
            _comboModel.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  // this will force that both columns have the same default width
                  .hint(_defaultEquipmentWidth, SWT.DEFAULT)
                  .applyTo(_comboModel);

            _autocomplete_Model = new AutoComplete_ComboInputMT(_comboModel);
         }
         {
            /*
             * Collate
             */
            _lblCollate = UI.createLabel(container, Messages.Dialog_Equipment_Label_Collate);
            _lblCollate.setToolTipText(_tooltip1);
            _gdVertCenter.applyTo(_lblCollate);

            _chkCollate = new Button(container, SWT.CHECK);
            _chkCollate.setText(Messages.Dialog_Equipment_Checkbox_Collate);
            _chkCollate.setToolTipText(_tooltip1);

            _chkCollate.addSelectionListener(_defaultSelectionListener);

            GridDataFactory.fillDefaults()
                  // align to the beginning, otherwise the decoration is partly hidden !!!
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_chkCollate);

            /*
             * Add a decoration for this important field
             */
            _comboDecorator_Collate = new ControlDecoration(_lblCollate, SWT.CENTER | SWT.RIGHT);
            _comboDecorator_Collate.setDescriptionText(_tooltip1);
            _comboDecorator_Collate.setImage(_decorationImageInfo);
            _comboDecorator_Collate.setMarginWidth(_decoratorDistance);
         }
         {
            /*
             * Collate ID
             */
            UI.createSpacer_Horizontal(container, 1);

            final Composite idContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(idContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(idContainer);
            {
               _lblCollateID = UI.createLabel(idContainer, Messages.Dialog_Equipment_Label_CollateID);
               _lblCollateID.setToolTipText(_tooltip2);
               GridDataFactory.fillDefaults()
                     .align(SWT.FILL, SWT.CENTER)
                     .indent(16, 0)
                     .applyTo(_lblCollateID);

               // autocomplete combo
               _comboCollateID = new Combo(idContainer, SWT.BORDER | SWT.FLAT);
               _comboCollateID.setText(UI.EMPTY_STRING);
               _comboCollateID.addModifyListener(_defaultModifyListener);

               GridDataFactory.fillDefaults()
                     .grab(true, false)
                     .indent(10, 0)
                     .applyTo(_comboCollateID);

               _autocomplete_CollateID = new AutoComplete_ComboInputMT(_comboCollateID);

               /*
                * Add a decoration for this important field
                */
               _comboDecorator_CollateID = new ControlDecoration(_lblCollateID, SWT.CENTER | SWT.RIGHT);
               _comboDecorator_CollateID.setDescriptionText(_tooltip2);
               _comboDecorator_CollateID.setImage(_decorationImageInfo);
               _comboDecorator_CollateID.setMarginWidth(_decoratorDistance);
            }
         }
         {
            /*
             * Collate between
             */

            UI.createSpacer_Horizontal(container, 1);

            final String collateWithTooltip = Messages.Dialog_EquipmentPart_Collate_Tooltip;

            final Composite collateContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(collateContainer);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(collateContainer);
//            collateContainer.setBackground(UI.SYS_COLOR_CYAN);
            {
               _lblCollateWith = UI.createLabel(collateContainer, Messages.Dialog_EquipmentPart_Label_CollateBetween);
               _lblCollateWith.setToolTipText(collateWithTooltip);
               GridDataFactory.fillDefaults()
                     .indent(16, 0)
                     .applyTo(_lblCollateWith);

               _rdoCollateWith_Previous = new Button(collateContainer, SWT.RADIO);
               _rdoCollateWith_Previous.setText(Messages.Dialog_EquipmentPart_Radio_CollatePart_Previous);
               _rdoCollateWith_Previous.setToolTipText(collateWithTooltip);
               _rdoCollateWith_Previous.addSelectionListener(_defaultSelectionListener);

               _rdoCollateWith_Next = new Button(collateContainer, SWT.RADIO);
               _rdoCollateWith_Next.setText(Messages.Dialog_EquipmentPart_Radio_CollatePart_Next);
               _rdoCollateWith_Next.setToolTipText(collateWithTooltip);
               _rdoCollateWith_Next.addSelectionListener(_defaultSelectionListener);
            }
         }
      }
   }

   private void createUI_200_Col1(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_CYAN);
      {
         {
            /*
             * Date used
             */
            final Label label = UI.createLabel(container, Messages.Dialog_Equipment_Label_Date);

            _dateUsed = new DateTime(container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
            _dateUsed.addSelectionListener(_defaultSelectionListener);

            /*
             * Add a decoration for this important field
             */
            _comboDecorator_DateFrom = new ControlDecoration(label, SWT.CENTER | SWT.RIGHT);
            _comboDecorator_DateFrom.setDescriptionText(_tooltip2);
            _comboDecorator_DateFrom.setImage(_decorationImageInfo);
            _comboDecorator_DateFrom.setMarginWidth(_decoratorDistance);

            UI.createSpacer_Horizontal(container, 1);
         }
         {
            /*
             * Built date
             */
            final Label label = UI.createLabel(container, Messages.Dialog_Equipment_Label_DateBuilt);
            _gdVertCenter.applyTo(label);
            _firstColumnControls.add(label);

            _dateBuilt = new DateTime(container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
            _dateBuilt.addSelectionListener(_defaultSelectionListener);

            _chkSyncDates = new Button(container, SWT.CHECK);
            _chkSyncDates.setText(Messages.Dialog_Equipment_Checkbox_Sync);
            _chkSyncDates.setToolTipText(Messages.Dialog_Equipment_Checkbox_Sync_Tooltip);
            _chkSyncDates.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Purchased date
             */
            final Label label = UI.createLabel(container, "Purc&hased");
            _gdVertCenter.applyTo(label);
            _firstColumnControls.add(label);

            _datePurchased = new DateTime(container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
            _datePurchased.addSelectionListener(_defaultSelectionListener);

            UI.createSpacer_Horizontal(container, 1);
         }
         {
            /*
             * Retired: Checkbox
             */
            final Label label = UI.createLabel(container, Messages.Dialog_Equipment_Label_DateRetired);
            _gdVertCenter.applyTo(label);
            _firstColumnControls.add(label);

            _chkRetired = new Button(container, SWT.CHECK);
            _chkRetired.setText(Messages.Dialog_Equipment_Checkbox_IsRetired);

            _chkRetired.addSelectionListener(_defaultSelectionListener);

            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_chkRetired);
         }
         {
            /*
             * Retired: Date
             */

            UI.createSpacer_Horizontal(container, 1);

            _dateRetired = new DateTime(container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
            _dateRetired.addSelectionListener(_defaultSelectionListener);

            _btnDateRetiredNow = new Button(container, SWT.PUSH);
            _btnDateRetiredNow.setImage(_imageNow);
            _btnDateRetiredNow.setToolTipText(Messages.Dialog_Equipment_Button_SetDateToToday_Tooltip);
            _btnDateRetiredNow.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelect_RetireNow()));
         }
      }
   }

   private void createUI_300_Col2(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_MAGENTA);
      {
         {
            /*
             * Size
             */
            final Label label = UI.createLabel(container, Messages.Dialog_Equipment_Label_Size);
            _gdVertCenter.applyTo(label);
            _firstColumnControls.add(label);

            // autocomplete combo
            _comboSize = new Combo(container, SWT.BORDER | SWT.FLAT);
            _comboSize.setText(UI.EMPTY_STRING);
            _comboSize.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_comboSize);

            _autocomplete_Size = new AutoComplete_ComboInputMT(_comboSize);
         }
         {
            /*
             * Price
             */

            final Label label = UI.createLabel(container, Messages.Dialog_Equipment_Label_Price);
            _firstColumnControls.add(label);

            // spinner
            _spinPrice = new Spinner(container, SWT.BORDER);

            _spinPrice.setDigits(2);
            _spinPrice.setMinimum(-1_000_000_000);
            _spinPrice.setMaximum(1_000_000_000);

            _spinPrice.addMouseWheelListener(_defaultMouseWheelListener);
            _spinPrice.addSelectionListener(_defaultSelectionListener);

            // autocomplete combo
            _comboPriceUnit = new Combo(container, SWT.BORDER | SWT.FLAT);
            _comboPriceUnit.setText(UI.EMPTY_STRING);
            _comboPriceUnit.setToolTipText(Messages.Dialog_Equipment_Combo_PriceUnit_Tooltip);
            _comboPriceUnit.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .hint(_currencyWidth, SWT.DEFAULT)
                  .applyTo(_comboPriceUnit);

            _autocomplete_PriceUnit = new AutoComplete_ComboInputMT(_comboPriceUnit);
         }
         {
            /*
             * Weight
             */

            final Label label = UI.createLabel(container, Messages.Dialog_Equipment_Label_Weight);
            _firstColumnControls.add(label);

            // spinner
            _spinWeight = new Spinner(container, SWT.BORDER);

            _spinWeight.setDigits(3);
            _spinWeight.setMinimum(0);
            _spinWeight.setMaximum(1_000_000_000);

            _spinWeight.addMouseWheelListener(_defaultMouseWheelListener);
            _spinWeight.addSelectionListener(_defaultSelectionListener);

            // combo: weight kg/g - lbs/oz
            _comboWeightUnit = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
            _comboWeightUnit.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelect_WeightUnit()));

            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .hint(_currencyWidth, SWT.DEFAULT)
                  .applyTo(_comboWeightUnit);
         }
         {
            /*
             * Distance first use
             */

            final Label label = UI.createLabel(container, Messages.Dialog_Equipment_Label_InitialDistance);
            label.setToolTipText(Messages.Dialog_Equipment_Label_InitialDistance_Tooltip);
            _firstColumnControls.add(label);

            // spinner
            _spinDistance = new Spinner(container, SWT.BORDER);

            _spinDistance.setDigits(0);
            _spinDistance.setMinimum(0);
            _spinDistance.setMaximum(1_000_000_000);

            _spinDistance.addMouseWheelListener(_defaultMouseWheelListener);
            _spinDistance.addSelectionListener(_defaultSelectionListener);

            // label: km/mi
            UI.createLabel(container, UI.UNIT_LABEL_DISTANCE);
         }
         {
            /*
             * Initial value
             */

            final Label label = UI.createLabel(container, Messages.Dialog_Equipment_Label_InitialValue);
            _firstColumnControls.add(label);

            // spinner
            _spinInitialValue = new Spinner(container, SWT.BORDER);

            _spinInitialValue.setMinimum(-1_000_000_000);
            _spinInitialValue.setMaximum(1_000_000_000);

            _spinInitialValue.addMouseWheelListener(_defaultMouseWheelListener);
            _spinInitialValue.addSelectionListener(_defaultSelectionListener);

            // autocomplete combo
            _comboInitialValueUnit = new Combo(container, SWT.BORDER | SWT.FLAT);
            _comboInitialValueUnit.setText(UI.EMPTY_STRING);
            _comboInitialValueUnit.setToolTipText(Messages.Dialog_Equipment_Label_InitialValue_Unit);
            _comboInitialValueUnit.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .hint(_currencyWidth, SWT.DEFAULT)
                  .applyTo(_comboInitialValueUnit);

            _autocomplete_InitialValueUnit = new AutoComplete_ComboInputMT(_comboInitialValueUnit);
         }
      }
   }

   private void createUI_900_Bottom(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).span(3, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      _container.setBackground(UI.SYS_COLOR_GREEN);
      {
         {
            /*
             * Purchase location
             */
            final Label label = UI.createLabel(container, Messages.Dialog_Equipment_Label_PurchaseLocation);
            _firstColumnControls.add(label);

            _txtPurchaseLocation = new Text(container, SWT.BORDER);
            _txtPurchaseLocation.addModifyListener(e -> onModify());
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .hint(_defaultDescriptionWidth, SWT.DEFAULT)
                  .applyTo(_txtPurchaseLocation);
         }
         {
            /*
             * Website
             */
            _linkWebsite = new Link(container, SWT.NONE);
            _linkWebsite.setText(Messages.Dialog_Equipment_Link_Website);
            _linkWebsite.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onSelect_Website()));
            _firstColumnControls.add(_linkWebsite);

            _txtUrlAddress = new Text(container, SWT.BORDER);
            _txtUrlAddress.addModifyListener(e -> onModify());
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .hint(_defaultDescriptionWidth, SWT.DEFAULT)
                  .applyTo(_txtUrlAddress);
         }
         {
            /*
             * Description
             */
            final Label label = UI.createLabel(container, Messages.Dialog_Equipment_Label_Description);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);
            _firstColumnControls.add(label);

            _txtDescription = new Text(container, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            _txtDescription.addModifyListener(e -> onModify());
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .hint(_defaultDescriptionWidth, convertHeightInCharsToPixels(5))
                  .minSize(SWT.DEFAULT, convertHeightInCharsToPixels(2))
                  .applyTo(_txtDescription);
         }
         {
            /*
             * Image filename
             */

            _lblImage = UI.createLabel(container, UI.EMPTY_STRING);
            _lblImage.setText(Messages.Dialog_Equipment_Label_Image);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblImage);
            _firstColumnControls.add(_lblImage);

            final Composite imagePathContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .applyTo(imagePathContainer);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(imagePathContainer);
//            imagePathContainer.setBackground(UI.SYS_COLOR_YELLOW);
            {
               {
                  _txtImageFilePath = UI.createReadOnlyText(imagePathContainer, UI.EMPTY_STRING);
                  GridDataFactory.fillDefaults()
                        .grab(true, false)
                        .align(SWT.FILL, SWT.CENTER)
                        .applyTo(_txtImageFilePath);
               }
               {
                  _btnSelectImage = new Button(imagePathContainer, SWT.PUSH);
                  _btnSelectImage.setText(Messages.app_btn_browse);
                  _btnSelectImage.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onImage_Select()));
               }
               {
                  _btnDeleteImage = new Button(imagePathContainer, SWT.PUSH);
                  _btnDeleteImage.setImage(_imageTrash);
                  _btnDeleteImage.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onImage_Delete()));
               }
            }

            UI.createSpacer_Horizontal(container);

            final Composite imageContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(imageContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(imageContainer);
            {
               {
                  /*
                   * Part image
                   */

                  final int imageSize = TagManager.getTagContent_ImageSize();

                  _canvasEquipmentImage = new Label(imageContainer, SWT.WRAP);
                  GridDataFactory.fillDefaults()
                        .hint(imageSize, imageSize)
                        .applyTo(_canvasEquipmentImage);
               }
               {
                  /*
                   * Options slideout to resize image
                   */
                  final ToolBar toolbar = new ToolBar(imageContainer, SWT.FLAT);

                  GridDataFactory.fillDefaults()
                        .grab(true, false)
                        .align(SWT.FILL, SWT.BEGINNING)
                        .applyTo(toolbar);

                  _toolbarManager_ImageSize = new ToolBarManager(toolbar);
                  _toolbarManager_ImageSize.add(_actionSlideout_SetImageSize);
               }
            }
         }
      }
   }

   private void enableControls() {

      if (_isInUIUpdate) {
         return;
      }

// SET_FORMATTING_OFF

      final boolean isCollate          = _chkCollate.getSelection();
      final boolean isRetired          = _chkRetired.getSelection();
      final boolean isSyncDates        = _chkSyncDates.getSelection();
      final boolean canEditBuiltDate   = isSyncDates == false;
      final boolean hasImage           = StringUtils.hasContent(_imageFilePath);

      _actionSlideout_SetImageSize  .setEnabled(hasImage);

      _dateBuilt                    .setEnabled(canEditBuiltDate);
      _dateRetired                  .setEnabled(isRetired);

      _btnDateRetiredNow            .setEnabled(isRetired);
      _btnDeleteImage               .setEnabled(StringUtils.hasContent(_imageFilePath));

      _comboCollateID               .setEnabled(isCollate);
      _lblCollateID                 .setEnabled(isCollate);
      _lblCollateWith               .setEnabled(isCollate);
      _rdoCollateWith_Next          .setEnabled(isCollate);
      _rdoCollateWith_Previous      .setEnabled(isCollate);

      if (isCollate) {

         _comboDecorator_CollateID  .show();
         _comboDecorator_DateFrom   .show();

      } else {

         _comboDecorator_CollateID  .hide();
         _comboDecorator_DateFrom   .hide();
      }

      // this is VERY important, otherwise parts of the old image is visible !!!
      _comboDecorator_CollateID.getControl().getParent().redraw();
      _comboDecorator_DateFrom.getControl().getParent().redraw();

// SET_FORMATTING_ON

      // OK button
      final boolean isValid = _isNewPart && _isModified == false

            // disable OK when new and not modified but do NOT display validation message
            ? false

            : isDataValid();

      getButton(IDialogConstants.OK_ID).setEnabled(isValid);
   }

   private void fillUI() {

// SET_FORMATTING_OFF

      UI.fillUI_Combobox(_comboBrand,              EquipmentManager.getCachedFields_AllBrands());
      UI.fillUI_Combobox(_comboCollateID,          EquipmentManager.getCachedFields_AllCollateIDs());
      UI.fillUI_Combobox(_comboInitialValueUnit,   EquipmentManager.getCachedFields_AllInitialValueUnits());
      UI.fillUI_Combobox(_comboModel,              EquipmentManager.getCachedFields_AllModels());
      UI.fillUI_Combobox(_comboPriceUnit,          EquipmentManager.getCachedFields_AllPriceUnits());
      UI.fillUI_Combobox(_comboSize,               EquipmentManager.getCachedFields_AllSizes());

// SET_FORMATTING_ON

      _comboWeightUnit.add(UI.UNIT_LABEL_WEIGHT);
      _comboWeightUnit.add(UI.UNIT_LABEL_WEIGHT_SMALL);
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
   }

   /**
    * @return Returns new or cloned instance
    */
   EquipmentPart getPart() {

      _part.updateUntilDate();

      return _part;
   }

   private int getWeight_FromModel(final EquipmentPart part) {

      final float weightKG = part.getWeight();
      final short weightUnit = part.getWeightUnit();

      final MeasurementSystem activeSystem = MeasurementSystem_Manager.getActiveMeasurementSystem();

      if (Unit_Weight.POUND.equals(activeSystem.getWeight())) {

         // imperial system

         if (weightUnit == 1) {

            // oz

            final float weightPound = weightKG * UI.UNIT_VALUE_WEIGHT;
            final float weightOz = weightPound * UI.UNIT_OZ_TO_POUND;

            return (int) weightOz;

         } else {

            // lbs

            final float weightPound = weightKG * UI.UNIT_VALUE_WEIGHT;
            final float weightPoundUI = weightPound * 1000f;

            return Math.round(weightPoundUI);
         }

      } else {

         // metric system

         if (weightUnit == 1) {

            // g

            return (int) (weightKG * 1000f);

         } else {

            // kg

            return Math.round(weightKG * 1000);
         }
      }
   }

   /**
    * @return Returns the selected weight in kg
    */
   private float getWeight_FromUI() {

      final float selectedWeight_kg_g_lbs_oz = _spinWeight.getSelection();

      final int selectionUnitIndex = _comboWeightUnit.getSelectionIndex();

      final MeasurementSystem activeSystem = MeasurementSystem_Manager.getActiveMeasurementSystem();

      if (Unit_Weight.POUND.equals(activeSystem.getWeight())) {

         // imperial system

         float selectedWeightPound;

         if (selectionUnitIndex == 1) {

            // oz

            selectedWeightPound = selectedWeight_kg_g_lbs_oz / UI.UNIT_OZ_TO_POUND;

         } else {

            // lbs

            selectedWeightPound = selectedWeight_kg_g_lbs_oz / 1000;
         }

         final float selectedWeightKG = selectedWeightPound / UI.UNIT_VALUE_WEIGHT;

         return selectedWeightKG;

      } else {

         // metric system

         if (selectionUnitIndex == 1) {

            // g

            return selectedWeight_kg_g_lbs_oz / 1000f;

         } else {

            // kg

            return selectedWeight_kg_g_lbs_oz / 1000f;
         }
      }
   }

   private void initUI() {

      /*
       * Collated tours are a collection of tours to sum up values,
       * e.g. distances or durations.
       * Tours are collated by the "Collate ID" and "Date" fields.
       * When an equipment is collated,
       * then it cannot contain parts or services.
       * When an equipment contains parts or services,
       * then this option is disabled.
       */
      _tooltip1 = Messages.Dialog_Equipment_Tooltip_1b;

      /*
       * With the "Collate ID" and "Date" fields, tours are collated to display
       * e.g. all kilometers for one part or one service
       */

      _tooltip2 = Messages.Dialog_Equipment_Tooltip_2b;

   // SET_FORMATTING_OFF

      _decoratorDistance         = 3;
      _gdVertCenter              = GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER);

      _defaultEquipmentWidth     = convertWidthInCharsToPixels(20);
      _defaultDescriptionWidth   = convertWidthInCharsToPixels(40);
      _currencyWidth             = UI.IS_WIN ? convertWidthInCharsToPixels(6) : convertWidthInCharsToPixels(18);

      _imageCamera   = TourbookPlugin.getImageDescriptor(Images.Camera).createImage();
      _imageNow      = TourbookPlugin.getImageDescriptor(Images.Calendar).createImage();
      _imageTrash    = TourbookPlugin.getImageDescriptor(Images.App_Trash_Themed).createImage();

// SET_FORMATTING_ON

      _decorationImageInfo = FieldDecorationRegistry.getDefault()
            .getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
            .getImage();

      _parent.addDisposeListener(disposeEvent -> onDispose());

      _defaultModifyListener = modifyEvent -> onModify();
      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModify());
      _defaultMouseWheelListener = mouseEvent -> {

         Util.adjustSpinnerValueOnMouseScroll(mouseEvent);

         onModify();
      };

      _gdVertCenter = GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER);

   }

   private boolean isDataValid() {

      final String brand = _comboBrand.getText().trim();
      final String model = _comboModel.getText().trim();

      if (StringUtils.hasContent(brand) == false && StringUtils.hasContent(model) == false) {

         setErrorMessage(Messages.Dialog_Equipment_Error_BrandModelIsEmpty);

         return false;
      }

      setErrorMessage(null);

      return true;
   }

   /**
    * @param imageFilePath
    *
    * @return Returns <code>true</code> when the image could be loaded, otherwise <code>false</code>
    */
   private boolean loadEquipmentImage(final String imageFilePath) {

      setErrorMessage(null);

      final Image[] loadedImage = new Image[] { null };

      if (StringUtils.hasContent(imageFilePath)) {

         if (Files.exists(Paths.get(imageFilePath)) == false) {

            setErrorMessage(NLS.bind(Messages.Dialog_TourTag_Label_ImageNotFound, imageFilePath));

         } else {

            _imageFilePath = imageFilePath;

            BusyIndicator.showWhile(Display.getCurrent(), () -> {

               try {

                  loadedImage[0] = EquipmentManager.getEquipmentImage(_imageFilePath, ImageSize.CONTENT);

               } catch (final IOException ioException) {

                  /**
                   * It is possible that an image cannot be loaded, e.g.
                   * <p>
                   * javax.imageio.IIOException: 16-bit samples are not supported for Horizontal
                   * differencing Predictor
                   */
                  final String errorMessage = Messages.Dialog_Equipment_Error_CannotLoadImage.formatted(
                        imageFilePath,
                        ioException.getMessage());

                  setErrorMessage(errorMessage);
               }
            });
         }
      }

      final Image equipmentImage = loadedImage[0];
      final boolean isImageLoaded = equipmentImage != null;

      // update UI
      updateUI_EquipmentImage(equipmentImage, getErrorMessage());

      return isImageLoaded;
   }

   @Override
   protected void okPressed() {

      updateModelFromUI();

      if (_part.isValidForSave() == false) {

         // data are not valid to be saved which is done in the action which opened this dialog

         return;
      }

      super.okPressed();
   }

   private void onDispose() {

      saveState();

      _autocomplete_Brand.saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND);
      _autocomplete_Model.saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL);
      _autocomplete_PriceUnit.saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT);

      _state.put(STATE_PRICE_UNIT_DEFAULT, _comboPriceUnit.getText().trim());

      UI.disposeResource(_imageCamera);
      UI.disposeResource(_imageDialog);
      UI.disposeResource(_imageNow);
      UI.disposeResource(_imageTrash);
   }

   private void onImage_Delete() {

      _imageFilePath = null;

      _txtImageFilePath.setText(UI.EMPTY_STRING);
      _canvasEquipmentImage.setImage(_imageCamera);

      // update UI
      updateUI_EquipmentImage(null, getErrorMessage());

      enableControls();

      _btnSelectImage.setFocus();
   }

   private void onImage_Select() {

      String lastSelectedPath = null;

      if (StringUtils.hasContent(_imageFilePath)) {

         if (Files.exists(Paths.get(_imageFilePath))) {

            final Path pathParent = Paths.get(_imageFilePath).getParent();

            lastSelectedPath = pathParent.toString();
         }

      } else {

         lastSelectedPath = Util.getStateString(_state, STATE_IMAGE_LAST_SELECTED_PATH, null);
      }

      final FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);

      fileDialog.setText(Messages.Dialog_EquipmentPart_FileDialog_Text);
      fileDialog.setFilterPath(lastSelectedPath);

      final String[] imageExtensions = { ImageUtils.getImageExtensions() };

      fileDialog.setFilterNames(imageExtensions);
      fileDialog.setFilterExtensions(imageExtensions);

      // open file dialog
      final String imageFilePath = fileDialog.open();

      // check if user canceled the dialog
      if (imageFilePath == null) {
         return;
      }

      // keep last selected path
      final String filePathFolder = Paths.get(imageFilePath).getParent().toString();
      _state.put(STATE_IMAGE_LAST_SELECTED_PATH, filePathFolder);

      final boolean isImageLoaded = loadEquipmentImage(imageFilePath);
      if (isImageLoaded) {

         _txtImageFilePath.setText(imageFilePath);

         enableControls();
      }
   }

   private void onModify() {

      if (_isInUIUpdate) {
         return;
      }

      _linkWebsite.setToolTipText(_txtUrlAddress.getText());

      final boolean isSyncDates = _chkSyncDates.getSelection();

      if (isSyncDates) {

         _dateBuilt.setDate(_dateUsed.getYear(), _dateUsed.getMonth(), _dateUsed.getDay());
      }

      _isModified = true;

      enableControls();
   }

   private void onSelect_RetireNow() {

      _isModified = true;

      final ZonedDateTime now = TimeTools.now();

      _dateRetired.setDate(now.getYear(), now.getMonthValue() - 1, now.getDayOfMonth());

      enableControls();
   }

   private void onSelect_Website() {

      final String url = _txtUrlAddress.getText().trim();

      if (url.length() > 0) {

         WEB.openUrl(url);
      }
   }

   private void onSelect_WeightUnit() {

      final float selectedWeight = _spinWeight.getSelection() / 1000f;
      final int selectionUnitIndex = _comboWeightUnit.getSelectionIndex();

      final MeasurementSystem activeSystem = MeasurementSystem_Manager.getActiveMeasurementSystem();

      if (Unit_Weight.POUND.equals(activeSystem.getWeight())) {

         // imperial system

         if (selectionUnitIndex == 1) {

            // lbs ->  oz

            _spinWeight.setSelection((int) (selectedWeight * UI.UNIT_OZ_TO_POUND * 1000f));

         } else {

            // oz -> lbs

            _spinWeight.setSelection(Math.round(selectedWeight / UI.UNIT_OZ_TO_POUND * 1000f));
         }

      } else {

         // metric system

         if (selectionUnitIndex == 1) {

            // kg -> g

            _spinWeight.setSelection((int) (selectedWeight * 1000 * 1000));

         } else {

            // g -> kg

            _spinWeight.setSelection(Math.round(selectedWeight));
         }
      }

      updateUI_WeightUnits();
   }

   private void resoreState() {

      _isInUIUpdate = true;

// SET_FORMATTING_OFF

      _autocomplete_Brand              .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND);
      _autocomplete_CollateID          .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_COLLATE_ID);
      _autocomplete_InitialValueUnit   .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_INITIAL_VALUE);
      _autocomplete_Model              .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL);
      _autocomplete_PriceUnit          .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT);
      _autocomplete_Size               .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_SIZE);

      _chkSyncDates                    .setSelection(Util.getStateBoolean(_state, STATE_SYNC_DATES, true));

      _comboPriceUnit                  .setText(Util.getStateString(_state, STATE_PRICE_UNIT_DEFAULT, UI.EMPTY_STRING));

// SET_FORMATTING_ON

      _imageFilePath = _part.getImageFilePath();

      _txtImageFilePath.setText(_imageFilePath == null ? UI.EMPTY_STRING : _imageFilePath);

      loadEquipmentImage(_imageFilePath);

      _isInUIUpdate = false;
   }

   private void saveState() {

// SET_FORMATTING_OFF

      _autocomplete_Brand              .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND);
      _autocomplete_CollateID          .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_COLLATE_ID);
      _autocomplete_InitialValueUnit   .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_INITIAL_VALUE);
      _autocomplete_Model              .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL);
      _autocomplete_PriceUnit          .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT);
      _autocomplete_Size               .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_SIZE);

      _state.put(STATE_PRICE_UNIT_DEFAULT,   _comboPriceUnit.getText().trim());
      _state.put(STATE_SYNC_DATES,           _chkSyncDates.getSelection());

// SET_FORMATTING_ON
   }

   private void setupImageListener(final Composite parent) {

      _tourEventListener = (workbenchPart, tourEventId, eventData) -> {

         if (tourEventId == TourEventId.CONTENT_LAYOUT_CHANGED) {

            // the image size is modified

            Image eqImage = _canvasEquipmentImage.getImage();

            // image is very likely disposed
            if (eqImage != null && eqImage.isDisposed()) {

               // reload image
               if (loadEquipmentImage(_imageFilePath)) {

                  eqImage = _canvasEquipmentImage.getImage();

                  // force width that the image is not truncated
                  final GridData gd = (GridData) _canvasEquipmentImage.getLayoutData();
                  final Rectangle imageBounds = eqImage.getBounds();
                  gd.widthHint = imageBounds.width;

                  _uiContainer.layout(true, true);
               }
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);

      parent.addDisposeListener(e -> {

         TourManager.getInstance().removeTourEventListener(_tourEventListener);
      });
   }

   private void updateModelFromUI() {

      String collateID = _comboCollateID.getText().trim();

      if (collateID.length() == 0) {
         collateID = EquipmentManager.createEmptyEquipmentType();
      }

// SET_FORMATTING_OFF

      final float distance          = _spinDistance.getSelection() * UI.UNIT_VALUE_DISTANCE;

      final LocalDate dateBuilt     = LocalDate.of(_dateBuilt.getYear(),      _dateBuilt.getMonth() + 1,       _dateBuilt.getDay());
      final LocalDate datePurchased = LocalDate.of(_datePurchased.getYear(),  _datePurchased.getMonth() + 1,   _datePurchased.getDay());
      final LocalDate dateRetired   = LocalDate.of(_dateRetired.getYear(),    _dateRetired.getMonth() + 1,     _dateRetired.getDay());
      final LocalDate dateUsed      = LocalDate.of(_dateUsed.getYear(),       _dateUsed.getMonth() + 1,        _dateUsed.getDay());

      _part.setEquipment(        _partEquipment);

      _part.setBrand(            _comboBrand.getText().trim());
      _part.setModel(            _comboModel.getText().trim());
      _part.setPartCollateID(    collateID);
      _part.setPurchaseLocation( _txtPurchaseLocation    .getText().trim());
      _part.setDescription(      _txtDescription         .getText().trim());
      _part.setUrlAddress(       _txtUrlAddress          .getText().trim());
      _part.setImageFilePath(    _txtImageFilePath       .getText().trim());

      _part.setIsCollate(        _chkCollate.getSelection());
      _part.setIsRetired(        _chkRetired.getSelection());
      _part.setCollateBetween(   _rdoCollateWith_Next.getSelection()
                                       ? EquipmentPart.COLLATED_WITH_NEXT
                                       : EquipmentPart.COLLATED_WITH_PREVIOUS);

      _part.setDistanceFirstUse( distance);
      _part.setInitialValue(     _spinInitialValue       .getSelection());
      _part.setInitialValueUnit( _comboInitialValueUnit  .getText());
      _part.setPrice(            _spinPrice              .getSelection() / 100f);
      _part.setPriceUnit(        _comboPriceUnit         .getText());
      _part.setSize(             _comboSize              .getText().trim());
      _part.setWeight(           getWeight_FromUI());
      _part.setWeightUnit(       (short) _comboWeightUnit.getSelectionIndex());

      _part.setDateBuilt(        TimeTools.toEpochMilli(dateBuilt));
      _part.setDatePurchased(    TimeTools.toEpochMilli(datePurchased));
      _part.setDateRetired(      TimeTools.toEpochMilli(dateRetired));
      _part.setDateUsed(         TimeTools.toEpochMilli(dateUsed));

// SET_FORMATTING_ON
   }

   /**
    * @param equipmentImage
    *           Can be <code>null</code>
    * @param errorMessage
    *           Can be <code>null</code>
    */
   private void updateUI_EquipmentImage(final Image equipmentImage, final String errorMessage) {

      int gdWidth;
      int gdHeight;

      // this must be set BEFORE an image is set, otherwise the image is not displayed
      _canvasEquipmentImage.setText(UI.EMPTY_STRING);

      if (equipmentImage != null) {

         // image is available

         _canvasEquipmentImage.setImage(equipmentImage);

         final Rectangle imageBounds = equipmentImage.getBounds();
         gdWidth = imageBounds.width;
         gdHeight = imageBounds.height;

      } else {

         // image is not available

         final boolean isError = errorMessage != null;

         if (isError) {

            // display error

            _canvasEquipmentImage.setImage(null);
            _canvasEquipmentImage.setText(errorMessage);

            gdWidth = SWT.DEFAULT;
            gdHeight = SWT.DEFAULT;

         } else {

            // image is not yet set -> display default image

            _canvasEquipmentImage.setImage(_imageCamera);

            final Rectangle cameraImageBounds = _imageCamera.getBounds();
            gdWidth = cameraImageBounds.width;
            gdHeight = cameraImageBounds.height;
         }
      }

      // update layout height
      final GridData gd = (GridData) _canvasEquipmentImage.getLayoutData();
      gd.widthHint = gdWidth;
      gd.heightHint = gdHeight;

      _uiContainer.layout(true, true);
   }

   private void updateUI_FromModel() {

      _isInUIUpdate = true;

      String collateID = _part.getPartCollateID();

      if (EquipmentManager.isEmptyEquipmentType(collateID)) {
         collateID = UI.EMPTY_STRING;
      }

// SET_FORMATTING_OFF

      /*
       * Set date default values
       */
      LocalDateTime dateBuilt       = _part.getDateBuilt_Local();
      LocalDateTime datePurchased   = _part.getDatePurchased_Local();
      LocalDateTime dateRetired     = _part.getDateRetired_Local();
      LocalDateTime dateUsed        = _part.getDateUsed_Local();

      final long dateBuiltMS        = TimeTools.toEpochMilli(dateBuilt);
      final long datePurchasedMS    = TimeTools.toEpochMilli(datePurchased);
      final long dateRetiredMS      = TimeTools.toEpochMilli(dateRetired);
      final long dateUsedMS         = TimeTools.toEpochMilli(dateUsed);

      if (dateBuiltMS      == 0) {  dateBuilt      = LocalDateTime.now();}
      if (datePurchasedMS  == 0) {  datePurchased  = dateBuilt;}
      if (dateRetiredMS    == 0) {  dateRetired    = LocalDateTime.of(2099, 1, 1, 0, 0);}
      if (dateUsedMS       == 0) {  dateUsed       = LocalDateTime.now();}


      final int collateWith      = _part.getCollateBetween();
      final float distance       = _part.getDistanceFirstUse() / UI.UNIT_VALUE_DISTANCE;
      final String urlAddress    = _part.getUrlAddress();

      _chkCollate                .setSelection(_part.isCollate());
      _chkRetired                .setSelection(_part.isRetired());

      _comboBrand                .setText(_part.getBrand());
      _comboCollateID            .setText(collateID);
      _comboInitialValueUnit     .setText(_part.getInitialValueUnit());
      _comboModel                .setText(_part.getModel());
      _comboSize                 .setText(_part.getSize());

      _comboWeightUnit           .select(_part.getWeightUnit());

      _dateBuilt                 .setDate(dateBuilt      .getYear(), dateBuilt      .getMonthValue() - 1,   dateBuilt      .getDayOfMonth());
      _datePurchased             .setDate(datePurchased  .getYear(), datePurchased  .getMonthValue() - 1,   datePurchased  .getDayOfMonth());
      _dateRetired               .setDate(dateRetired    .getYear(), dateRetired    .getMonthValue() - 1,   dateRetired    .getDayOfMonth());
      _dateUsed                  .setDate(dateUsed       .getYear(), dateUsed       .getMonthValue() - 1,   dateUsed       .getDayOfMonth());

      _rdoCollateWith_Next       .setSelection(collateWith == EquipmentPart.COLLATED_WITH_NEXT);
      _rdoCollateWith_Previous   .setSelection(collateWith == EquipmentPart.COLLATED_WITH_PREVIOUS);

      _spinDistance              .setSelection((int) distance);
      _spinInitialValue          .setSelection((int) _part.getInitialValue());
      _spinPrice                 .setSelection((int) (_part.getPrice()  * 100));
      _spinWeight                .setSelection(getWeight_FromModel(_part));

      _txtDescription            .setText(_part.getDescription());
      _txtPurchaseLocation       .setText(_part.getPurchaseLocation());
      _txtUrlAddress             .setText(urlAddress);

      _linkWebsite               .setToolTipText(urlAddress);

// SET_FORMATTING_ON

      final String priceUnit = _part.getPriceUnit();
      if (StringUtils.hasContent(priceUnit)) {

         // overwrite default

         _comboPriceUnit.setText(priceUnit);
      }

      updateUI_WeightUnits();

      _isInUIUpdate = false;
   }

   private void updateUI_WeightUnits() {

      final int selectedUnitIndex = _comboWeightUnit.getSelectionIndex();

      final MeasurementSystem activeSystem = MeasurementSystem_Manager.getActiveMeasurementSystem();

      if (Unit_Weight.POUND.equals(activeSystem.getWeight())) {

         // imperial system

         if (selectedUnitIndex == 1) {

            // lbs ->  oz

            _spinWeight.setDigits(0);

            _spinWeight.setIncrement(10);
            _spinWeight.setPageIncrement(100);

         } else {

            // oz -> lbs

            _spinWeight.setDigits(3);

            _spinWeight.setIncrement(100);
            _spinWeight.setPageIncrement(1000);

         }

      } else {

         // metric system

         if (selectedUnitIndex == 1) {

            // kg -> g

            _spinWeight.setDigits(0);

            _spinWeight.setIncrement(10);
            _spinWeight.setPageIncrement(100);

         } else {

            // g -> kg

            _spinWeight.setDigits(3);

            _spinWeight.setIncrement(100);
            _spinWeight.setPageIncrement(1000);
         }
      }
   }
}
