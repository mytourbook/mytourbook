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
 * Dialog to modify a {@link EquipmentService}
 */
public class DialogEquipmentService extends TitleAreaDialog {

   private static final String          ID                                         = "net.tourbook.equipment.DialogEquipmentService"; //$NON-NLS-1$

   private static final IDialogSettings _state                                     = TourbookPlugin.getState(ID);

   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_COMPANY    = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_COMPANY";       //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_NAME       = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_NAME";          //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT";    //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE       = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE";          //$NON-NLS-1$
   private static final String          STATE_IMAGE_LAST_SELECTED_PATH             = "STATE_IMAGE_LAST_SELECTED_PATH";                //$NON-NLS-1$
   private static final String          STATE_PRICE_UNIT_DEFAULT                   = "STATE_PRICE_UNIT_DEFAULT";                      //$NON-NLS-1$

   /**
    * New or cloned instance
    */
   private EquipmentPart                _service;
   private Equipment                    _serviceEquipment;

   private boolean                      _isDuplicateService;
   private boolean                      _isInUIUpdate;
   private boolean                      _isModified;
   private boolean                      _isNewService;

   private ModifyListener               _defaultModifyListener;
   private SelectionListener            _defaultSelectionListener;
   private MouseWheelListener           _defaultMouseWheelListener;
   private ITourEventListener           _tourEventListener;

   private String                       _tooltip1;
   private String                       _tooltip2;

   private GridDataFactory              _gdVertCenter;
   private int                          _decoratorDistance;
   private String                       _imageFilePath;

   private int                          _currencyWidth;
   private int                          _defaultDescriptionWidth;

   private ToolBarManager               _toolbarManager_ImageSize;

   private ActionSlideout_SetImageSize  _actionSlideout_SetImageSize;

   /**
    * Contains the controls which are displayed in the first column, these controls are used to get
    * the maximum width and set the first column within the different section to the same width
    */
   private final ArrayList<Control>     _firstColumnControls                       = new ArrayList<>();

   /*
    * UI resources
    */
   private Image _decorationImage;
   private Image _imageCamera;
   private Image _imageNow;
   private Image _imageTrash;

   // must be created eraly
   private Image _imageDialog = TourbookPlugin.getImageDescriptor(Images.Equipment_Service).createImage();

   /*
    * UI controls
    */
   private Composite                 _parent;
   private Composite                 _uiContainer;

   private Button                    _btnDateRetiredNow;
   private Button                    _btnDeleteImage;
   private Button                    _btnSelectImage;

   private Button                    _chkCollate;
   private Button                    _chkRetired;

   private Button                    _rdoCollateWith_Next;
   private Button                    _rdoCollateWith_Previous;

   private Combo                     _comboCompany;
   private Combo                     _comboName;
   private Combo                     _comboPriceUnit;
   private Combo                     _comboCollateID;

   private DateTime                  _datePurchased;
   private DateTime                  _dateRetired;
   private DateTime                  _dateUsed;

   private Label                     _canvasEquipmentImage;
   private Label                     _lblCollateID;
   private Label                     _lblCollateWith;
   private Label                     _lblImage;

   private Link                      _linkWebsite;

   private Spinner                   _spinPrice;

   private Text                      _txtDescription;
   private Text                      _txtImageFilePath;
   private Text                      _txtPurchaseLocation;
   private Text                      _txtUrlAddress;

   private AutoComplete_ComboInputMT _autocomplete_Company;
   private AutoComplete_ComboInputMT _autocomplete_Name;
   private AutoComplete_ComboInputMT _autocomplete_PriceUnit;
   private AutoComplete_ComboInputMT _autocomplete_CollateID;

   private ControlDecoration         _comboDecorator_Collate;
   private ControlDecoration         _comboDecorator_DateFrom;
   private ControlDecoration         _comboDecorator_CollateID;

   private class ActionSlideout_SetImageSize extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutEquipment_SetImageSize(_parent, toolbar);
      }
   }

   public DialogEquipmentService(final Shell parentShell,
                                 final Equipment equipment,
                                 final EquipmentPart service,
                                 final boolean isDuplicate) {

      super(parentShell);

      _serviceEquipment = equipment;
      _isDuplicateService = isDuplicate;

      _isNewService = service == null;

      if (_isNewService) {

         _service = new EquipmentPart(EquipmentPart.ITEM_TYPE_SERVICE);

         _service.setCollateBetween(EquipmentPart.COLLATED_WITH_PREVIOUS);

      } else {

         _service = service.clone();

         if (isDuplicate) {

            // adjust date to today

            _service.setDateUsed(TimeTools.nowInMilliseconds());
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
      shell.setText(Messages.Dialog_EquipmentService_Dialog_Title);
   }

   @Override
   public void create() {

      super.create();

      final String messageTitle =

            _isDuplicateService

                  ? Messages.Dialog_EquipmentService_Dialog_Message_Service_Duplicate
                  : _isNewService

                        ? Messages.Dialog_EquipmentService_Dialog_Message_Service_New
                        : Messages.Dialog_EquipmentService_Dialog_Message_Service_Edit;

      setTitle(messageTitle.formatted(_serviceEquipment.getName()));
      setMessage(_service.getName_Service());
   }

   private void createActions() {

      _actionSlideout_SetImageSize = new ActionSlideout_SetImageSize();
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // OK -> Create/Save
      getButton(IDialogConstants.OK_ID).setText(_isNewService || _isDuplicateService
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

      resoreState();

      updateUIFromModel();

      // compute width for all controls and equalize column width for the first column
      UI.setEqualizeColumWidths(_firstColumnControls);
      _uiContainer.layout(true, true);

      // ensure the UI is created
      _parent.getDisplay().asyncExec(() -> {

         // !!! MUST BE DONE VERY LATE, OTHERWISE THERE ARE ISSUES !!!  ?????????? need to be checked
         _toolbarManager_ImageSize.update(true);

         enableControls();

         _comboName.setFocus();
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
      UI.createSpacer_Horizontal(_uiContainer, 2, 1);
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
             * Name
             */

            final Label label = UI.createLabel(container, Messages.Dialog_EquipmentService_Label_Name);
            _gdVertCenter.applyTo(label);
            _firstColumnControls.add(label);

            // autocomplete combo
            _comboName = new Combo(container, SWT.BORDER | SWT.FLAT);
            _comboName.setText(UI.EMPTY_STRING);
            _comboName.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboName);

            _autocomplete_Name = new AutoComplete_ComboInputMT(_comboName);
         }
         {
            /*
             * Company
             */

            final Label label = UI.createLabel(container, Messages.Dialog_EquipmentService_Label_Company);
            _gdVertCenter.applyTo(label);
            _firstColumnControls.add(label);

            // autocomplete combo
            _comboCompany = new Combo(container, SWT.BORDER | SWT.FLAT);
            _comboCompany.setText(UI.EMPTY_STRING);
            _comboCompany.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboCompany);

            _autocomplete_Company = new AutoComplete_ComboInputMT(_comboCompany);
         }
         {
            /*
             * Is collate tours
             */
            final Label label = UI.createLabel(container, Messages.Dialog_Equipment_Label_Collate);
            _gdVertCenter.applyTo(label);

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
            _comboDecorator_Collate = new ControlDecoration(label, SWT.CENTER | SWT.RIGHT);
            _comboDecorator_Collate.setDescriptionText(_tooltip1);
            _comboDecorator_Collate.setImage(_decorationImage);
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
               GridDataFactory.fillDefaults()
                     .align(SWT.FILL, SWT.CENTER)
                     .indent(16, 0)
                     .applyTo(_lblCollateID);

               // autocomplete combo
               _comboCollateID = new Combo(idContainer, SWT.BORDER | SWT.FLAT);
               _comboCollateID.setText(UI.EMPTY_STRING);
               _comboCollateID.setToolTipText(_tooltip2);
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

               // a restart is required for the theme change to take full effect
               _comboDecorator_CollateID.setDescriptionText(_tooltip2);
               _comboDecorator_CollateID.setImage(_decorationImage);
               _comboDecorator_CollateID.setMarginWidth(_decoratorDistance);
            }
         }
         {
            /*
             * Collate with
             */

            UI.createSpacer_Horizontal(container, 1);

            final String collateWithTooltip = Messages.Dialog_EquipmentService_Collate_Tooltip;

            final Composite collateContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(collateContainer);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(collateContainer);
//            collateContainer.setBackground(UI.SYS_COLOR_CYAN);
            {
               _lblCollateWith = UI.createLabel(collateContainer, Messages.Dialog_EquipmentService_Label_CollateBetween);
               _lblCollateWith.setToolTipText(collateWithTooltip);
               GridDataFactory.fillDefaults()
                     .indent(16, 0)
                     .applyTo(_lblCollateWith);

               _rdoCollateWith_Previous = new Button(collateContainer, SWT.RADIO);
               _rdoCollateWith_Previous.setText(Messages.Dialog_EquipmentService_Radio_CollateService_Previous);
               _rdoCollateWith_Previous.setToolTipText(collateWithTooltip);
               _rdoCollateWith_Previous.addSelectionListener(_defaultSelectionListener);

               _rdoCollateWith_Next = new Button(collateContainer, SWT.RADIO);
               _rdoCollateWith_Next.setText(Messages.Dialog_EquipmentService_Radio_CollateService_Next);
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
             * Date
             */
            final Label label = UI.createLabel(container, Messages.Dialog_Equipment_Label_Date);
            _gdVertCenter.applyTo(label);

            _dateUsed = new DateTime(container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
            _dateUsed.setToolTipText(_tooltip2);
            _dateUsed.addSelectionListener(_defaultSelectionListener);

            /*
             * Add a decoration for this important field
             */
            _comboDecorator_DateFrom = new ControlDecoration(label, SWT.CENTER | SWT.RIGHT);
            _comboDecorator_DateFrom.setDescriptionText(_tooltip2);
            _comboDecorator_DateFrom.setImage(_decorationImage);
            _comboDecorator_DateFrom.setMarginWidth(_decoratorDistance);
         }
         UI.createSpacer_Horizontal(container, 1);
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
         }
         UI.createSpacer_Horizontal(container, 1);
         UI.createSpacer_Horizontal(container, 1);
         {
            /*
             * Retired: Date
             */
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
             * Price
             */

            UI.createLabel(container, Messages.Dialog_Equipment_Label_Price);

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
             * Purchased
             */
            final Label label = UI.createLabel(container, Messages.Dialog_Equipment_Label_Purchased);
            _firstColumnControls.add(label);

            final Composite purchaseContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(purchaseContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(purchaseContainer);
            {
               _datePurchased = new DateTime(purchaseContainer, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
               _datePurchased.addSelectionListener(_defaultSelectionListener);

               _txtPurchaseLocation = new Text(purchaseContainer, SWT.BORDER);
               _txtPurchaseLocation.setToolTipText(Messages.Dialog_Equipment_Label_Purchased_Tooltip);
               _txtPurchaseLocation.addModifyListener(e -> onModify());
               GridDataFactory.fillDefaults()
                     .grab(true, false)
                     .hint(_defaultDescriptionWidth, SWT.DEFAULT)
                     .applyTo(_txtPurchaseLocation);
            }
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
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
            _firstColumnControls.add(label);

            _txtDescription = new Text(container, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            _txtDescription.addModifyListener(e -> onModify());
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .hint(_defaultDescriptionWidth, convertHeightInCharsToPixels(20))
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
//            imageContainer.setBackground(UI.SYS_COLOR_YELLOW);
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
            {
               /*
                * Equipment image
                */

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
   }

   private void enableControls() {

      if (_isInUIUpdate) {
         return;
      }

// SET_FORMATTING_OFF

      final boolean isCollate    = _chkCollate.getSelection();
      final boolean isRetired    = _chkRetired.getSelection();
      final boolean hasImage     = StringUtils.hasContent(_imageFilePath);

      _actionSlideout_SetImageSize  .setEnabled(hasImage);


      _btnDeleteImage            .setEnabled(StringUtils.hasContent(_imageFilePath));

      _btnDateRetiredNow         .setEnabled(isRetired);
      _dateRetired               .setEnabled(isRetired);

      _comboCollateID            .setEnabled(isCollate);
      _lblCollateWith            .setEnabled(isCollate);
      _lblCollateID              .setEnabled(isCollate);
      _rdoCollateWith_Next       .setEnabled(isCollate);
      _rdoCollateWith_Previous   .setEnabled(isCollate);

      if (isCollate) {

         _comboDecorator_DateFrom   .show();
         _comboDecorator_CollateID  .show();

      } else {

         _comboDecorator_DateFrom   .hide();
         _comboDecorator_CollateID  .hide();
      }


      // this is VERY important, otherwise parts of the old image is visible !!!
      _comboDecorator_CollateID.getControl().getParent().redraw();
      _comboDecorator_DateFrom.getControl().getParent().redraw();

// SET_FORMATTING_ON

      // OK button
      final boolean isValid = _isNewService && _isModified == false

            // disable OK when new and not modified but do NOT display validation message
            ? false

            : isDataValid();
      getButton(IDialogConstants.OK_ID).setEnabled(isValid);
   }

   private void fillUI() {

// SET_FORMATTING_OFF

      UI.fillUI_Combobox(_comboCompany,   EquipmentManager.getCachedFields_AllCompanies());
      UI.fillUI_Combobox(_comboName,      EquipmentManager.getCachedFields_AllServiceNames());
      UI.fillUI_Combobox(_comboPriceUnit, EquipmentManager.getCachedFields_AllPriceUnits());
      UI.fillUI_Combobox(_comboCollateID, EquipmentManager.getCachedFields_AllCollateIDs());

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
   EquipmentPart getService() {

      _service.updateUntilDate();

      return _service;
   }

   private void initUI() {

      _tooltip1 = Messages.Dialog_Equipment_Tooltip_1b;
      _tooltip2 = Messages.Dialog_Equipment_Tooltip_2b;

      _decorationImage = FieldDecorationRegistry.getDefault()
            .getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
            .getImage();

      _gdVertCenter = GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER);

      _defaultDescriptionWidth = convertWidthInCharsToPixels(40);
      _currencyWidth = UI.IS_WIN ? convertWidthInCharsToPixels(4) : convertWidthInCharsToPixels(12);
      // > 0 will hide the decorator
      _decoratorDistance = 3;

// SET_FORMATTING_OFF

      _imageCamera   = TourbookPlugin.getImageDescriptor(Images.Camera).createImage();
      _imageTrash    = TourbookPlugin.getImageDescriptor(Images.App_Trash_Themed).createImage();
      _imageNow      = TourbookPlugin.getImageDescriptor(Images.Calendar).createImage();

// SET_FORMATTING_ON

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

         setErrorMessage(Messages.Dialog_EquipmentService_Error_NameIsEmpty);

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

      if (_service.isValidForSave() == false) {

         // data are not valid to be saved which is done in the action which opened this dialog

         return;
      }

      super.okPressed();
   }

   private void onDispose() {

      UI.disposeResource(_imageCamera);
      UI.disposeResource(_imageDialog);
      UI.disposeResource(_imageNow);
      UI.disposeResource(_imageTrash);

// SET_FORMATTING_OFF

      _autocomplete_Company   .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_COMPANY);
      _autocomplete_Name      .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_NAME);
      _autocomplete_PriceUnit .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT);
      _autocomplete_CollateID .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE);

// SET_FORMATTING_ON

      _state.put(STATE_PRICE_UNIT_DEFAULT, _comboPriceUnit.getText().trim());
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

      fileDialog.setText(Messages.Dialog_EquipmentService_FileDialog_Title);
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

      _isModified = true;

      _linkWebsite.setToolTipText(_txtUrlAddress.getText());

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

   private void resoreState() {

      _isInUIUpdate = true;

// SET_FORMATTING_OFF
      _autocomplete_Company   .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_COMPANY);
      _autocomplete_Name      .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_NAME);
      _autocomplete_PriceUnit .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT);
      _autocomplete_CollateID .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE);

// SET_FORMATTING_ON

      // set default unit
      _comboPriceUnit.setText(Util.getStateString(_state, STATE_PRICE_UNIT_DEFAULT, UI.EMPTY_STRING));

      _imageFilePath = _service.getImageFilePath();

      _txtImageFilePath.setText(_imageFilePath == null ? UI.EMPTY_STRING : _imageFilePath);

      loadEquipmentImage(_imageFilePath);

      _isInUIUpdate = false;
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

// SET_FORMATTING_OFF

      final LocalDate dateUsed      = LocalDate.of(_dateUsed      .getYear(), _dateUsed      .getMonth() + 1,  _dateUsed      .getDay());
      final LocalDate datePurchased = LocalDate.of(_datePurchased .getYear(), _datePurchased .getMonth() + 1,  _datePurchased .getDay());

      _service.setEquipment(        _serviceEquipment);

      _service.setCompany(          _comboCompany        .getText().trim());
      _service.setName_Service(     _comboName           .getText().trim());
      _service.setPartCollateID(    _comboCollateID      .getText().trim());
      _service.setPurchaseLocation( _txtPurchaseLocation .getText().trim());
      _service.setDescription(      _txtDescription      .getText().trim());
      _service.setUrlAddress(       _txtUrlAddress       .getText().trim());

      _service.setImageFilePath(    _txtImageFilePath    .getText().trim());

      _service.setIsCollate(        _chkCollate          .getSelection());
      _service.setIsRetired(        _chkRetired          .getSelection());
      _service.setCollateBetween(   _rdoCollateWith_Next .getSelection()
                                          ? EquipmentPart.COLLATED_WITH_NEXT
                                          : EquipmentPart.COLLATED_WITH_PREVIOUS);

      _service.setPrice(            _spinPrice           .getSelection() / 100f);
      _service.setPriceUnit(        _comboPriceUnit      .getText());

      _service.setDatePurchased(    TimeTools.toEpochMilli(datePurchased));
      _service.setDateUsed(         TimeTools.toEpochMilli(dateUsed));

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

   private void updateUIFromModel() {

      _isInUIUpdate = true;

// SET_FORMATTING_OFF

      final int collateWith      = _service.getCollateBetween();
      final String urlAddress    = _service.getUrlAddress();

      LocalDateTime datePurchased   = _service.getDatePurchased_Local();
      LocalDateTime dateUsed        = _service.getDateUsed_Local();

      final long datePurchasedMS    = TimeTools.toEpochMilli(datePurchased);
      final long dateUsedMS         = TimeTools.toEpochMilli(dateUsed);

      if (datePurchasedMS  == 0) {  datePurchased  = LocalDateTime.now();}
      if (dateUsedMS       == 0) {  dateUsed       = LocalDateTime.now();}

      _chkCollate                .setSelection(_service.isCollate());
      _chkRetired                .setSelection(_service.isRetired());

      _comboCompany              .setText(_service.getCompany());
      _comboName                 .setText(_service.getName_Service());
      _comboCollateID            .setText(_service.getPartCollateID());

      _datePurchased             .setDate(datePurchased  .getYear(), datePurchased  .getMonthValue() - 1,   datePurchased  .getDayOfMonth());
      _dateUsed                  .setDate(dateUsed       .getYear(), dateUsed       .getMonthValue() - 1,   dateUsed       .getDayOfMonth());

      _rdoCollateWith_Next       .setSelection(collateWith == EquipmentPart.COLLATED_WITH_NEXT);
      _rdoCollateWith_Previous   .setSelection(collateWith == EquipmentPart.COLLATED_WITH_PREVIOUS);

      _spinPrice                 .setSelection((int) (_service.getPrice()  * 100));

      _txtDescription            .setText(_service.getDescription());
      _txtPurchaseLocation       .setText(_service.getPurchaseLocation());
      _txtUrlAddress             .setText(urlAddress);

      _linkWebsite               .setToolTipText(urlAddress);

// SET_FORMATTING_ON

      final String priceUnit = _service.getPriceUnit();
      if (StringUtils.hasContent(priceUnit)) {

         // overwrite default

         _comboPriceUnit.setText(priceUnit);
      }

      _isInUIUpdate = false;
   }

}
