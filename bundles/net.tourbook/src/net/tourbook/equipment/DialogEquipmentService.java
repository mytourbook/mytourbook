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
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.autocomplete.AutoComplete_ComboInputMT;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.ImageUtils;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.data.EquipmentPart;
import net.tourbook.tag.TagManager;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
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
   private static final String          STATE_IMAGE_LAST_SELECTED_PATH             = "STATE_IMAGE_LAST_SELECTED_PATH";                //$NON-NLS-1$
   private static final String          STATE_PRICE_UNIT_DEFAULT                   = "STATE_PRICE_UNIT_DEFAULT";                      //$NON-NLS-1$

   /**
    * New or cloned instance
    */
   private EquipmentPart                _service;
   private Equipment                    _serviceEquipment;

   private boolean                      _isDuplicateService;
   private boolean                      _isInUIUpdate;
   private boolean                      _isNewService;

   private ModifyListener               _defaultModifyListener;
   private SelectionListener            _defaultSelectionListener;
   private MouseWheelListener           _defaultMouseWheelListener;

   private boolean                      _isModified;

   private String                       _imageFilePath;

   private PixelConverter               _pc;

   /*
    * UI resources
    */
   private Image _imageTrash;
   private Image _imageCamera;

   // must be created eraly
   private Image _imageDialog = TourbookPlugin.getImageDescriptor(Images.Equipment_Part).createImage();

   /*
    * UI controls
    */
   private Composite                 _container;
   private Composite                 _parent;

   private Button                    _btnDeleteImage;

   private Button                    _chkCollate;

   private Button                    _rdoCollateWith_Next;
   private Button                    _rdoCollateWith_Previous;

   private Combo                     _comboCompany;
   private Combo                     _comboName;
   private Combo                     _comboPriceUnit;
   private Combo                     _comboType;

   private DateTime                  _dateUsed;

   private Label                     _canvasEquipmentImage;

   private Label                     _lblCollateWith;
   private Label                     _lblImage;
   private Label                     _lblImageFilePath;

   private Spinner                   _spinPrice;

   private Text                      _txtDescription;
   private Text                      _txtUrlAddress;

   private AutoComplete_ComboInputMT _autocomplete_Company;
   private AutoComplete_ComboInputMT _autocomplete_Name;
   private AutoComplete_ComboInputMT _autocomplete_PriceUnit;
   private AutoComplete_ComboInputMT _autocomplete_Type;

   private ControlDecoration         _comboDecorator_Collate;
   private ControlDecoration         _comboDecorator_DateFrom;
   private ControlDecoration         _comboDecorator_Type;

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

            _isDuplicateService ? Messages.Dialog_EquipmentService_Dialog_Message_Service_Duplicate
                  : _isNewService ? Messages.Dialog_EquipmentService_Dialog_Message_Service_New
                        : Messages.Dialog_EquipmentService_Dialog_Message_Service_Edit;

      setTitle(messageTitle);
      setMessage(_service.getName());
   }

   private void createActions() {

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

      final String tooltip = Messages.Dialog_Equipment_Tooltip_1;
      final String tooltip2 = Messages.Dialog_Equipment_Tooltip_2;

      final Image decorationImage = FieldDecorationRegistry.getDefault()
            .getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
            .getImage();

      final GridDataFactory gdVertCenter = GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER);

      final int defaultWidth = convertWidthInCharsToPixels(100);

      // > 0 will hide the decorator
      final int decoratorDistance = 3;

      _container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_container);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(_container);
//      _container.setBackground(UI.SYS_COLOR_MAGENTA);
      {
         {
            /*
             * Name
             */

            final Label label = UI.createLabel(_container, Messages.Dialog_EquipmentService_Label_Name);
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

            final Label label = UI.createLabel(_container, Messages.Dialog_EquipmentService_Label_Company);
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
            final Label label = UI.createLabel(_container, Messages.Dialog_Equipment_Label_Type);
            gdVertCenter.applyTo(label);

            // autocomplete combo
            _comboType = new Combo(_container, SWT.BORDER | SWT.FLAT);
            _comboType.setText(UI.EMPTY_STRING);
            _comboType.setToolTipText(tooltip2);
            _comboType.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_comboType);

            _autocomplete_Type = new AutoComplete_ComboInputMT(_comboType);

            /*
             * Add a decoration for this important field
             */
            _comboDecorator_Type = new ControlDecoration(_comboType, SWT.CENTER | SWT.LEFT);

            // a restart is required for the theme change to take full effect
            _comboDecorator_Type.setDescriptionText(tooltip2);
            _comboDecorator_Type.setImage(decorationImage);
            _comboDecorator_Type.setMarginWidth(decoratorDistance);
         }
         {
            /*
             * Date
             */
            final Label label = UI.createLabel(_container, Messages.Dialog_Equipment_Label_Date);
            gdVertCenter.applyTo(label);

            _dateUsed = new DateTime(_container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
            _dateUsed.setToolTipText(tooltip2);
            _dateUsed.addSelectionListener(_defaultSelectionListener);

            /*
             * Add a decoration for this important field
             */
            _comboDecorator_DateFrom = new ControlDecoration(_dateUsed, SWT.CENTER | SWT.LEFT);
            _comboDecorator_DateFrom.setDescriptionText(tooltip2);
            _comboDecorator_DateFrom.setImage(decorationImage);
            _comboDecorator_DateFrom.setMarginWidth(decoratorDistance);
         }
         UI.createSpacer_Horizontal(_container, 1);
         {
            /*
             * Price
             */

            UI.createLabel(_container, Messages.Dialog_Equipment_Label_Price);

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
            _comboPriceUnit.setToolTipText(Messages.Dialog_Equipment_Combo_PriceUnit_Tooltip);
            _comboPriceUnit.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .hint(_pc.convertWidthInCharsToPixels(4), SWT.DEFAULT)
                  .applyTo(_comboPriceUnit);

            _autocomplete_PriceUnit = new AutoComplete_ComboInputMT(_comboPriceUnit);
         }
         {
            /*
             * Collate tours
             */
            final Label label = UI.createLabel(_container, Messages.Dialog_Equipment_Label_Collate);
            gdVertCenter.applyTo(label);

            _chkCollate = new Button(_container, SWT.CHECK);
            _chkCollate.setText(Messages.Dialog_Equipment_Checkbox_Collate);
            _chkCollate.setToolTipText(tooltip);
            _chkCollate.addSelectionListener(_defaultSelectionListener);

            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  // align to the beginning, otherwise the decoration is partly hidden !!!
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_chkCollate);

            /*
             * Add a decoration for this important field
             */
            _comboDecorator_Collate = new ControlDecoration(_chkCollate, SWT.CENTER | SWT.RIGHT);
            _comboDecorator_Collate.setDescriptionText(tooltip);
            _comboDecorator_Collate.setImage(decorationImage);
            _comboDecorator_Collate.setMarginWidth(decoratorDistance);
         }
         {
            /*
             * Collate with
             */

            UI.createSpacer_Horizontal(_container, 1);

            final String collateWithTooltip =
                  "All services of the same \"Type\" can be collated only\neither with the next or the previous service,\nthere can be no mix and match";

            final Composite collateContainer = new Composite(_container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(collateContainer);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(collateContainer);
//            collateContainer.setBackground(UI.SYS_COLOR_CYAN);
            {
               _lblCollateWith = UI.createLabel(collateContainer, "Collate between this service and");
               _lblCollateWith.setToolTipText(collateWithTooltip);
               GridDataFactory.fillDefaults()
                     .indent(12, 0)
                     .applyTo(_lblCollateWith);

               _rdoCollateWith_Previous = new Button(collateContainer, SWT.RADIO);
               _rdoCollateWith_Previous.setText("Pre&vious service");
               _rdoCollateWith_Previous.setToolTipText(collateWithTooltip);
               _rdoCollateWith_Previous.addSelectionListener(_defaultSelectionListener);

               _rdoCollateWith_Next = new Button(collateContainer, SWT.RADIO);
               _rdoCollateWith_Next.setText("Ne&xt service");
               _rdoCollateWith_Next.setToolTipText(collateWithTooltip);
               _rdoCollateWith_Next.addSelectionListener(_defaultSelectionListener);
            }
         }
         {
            /*
             * Website
             */
            final Label label = UI.createLabel(_container, Messages.Dialog_Equipment_Label_Website);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

            _txtUrlAddress = new Text(_container, SWT.BORDER);
            _txtUrlAddress.addModifyListener(e -> onModify());
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .hint(defaultWidth, SWT.DEFAULT)
                  .span(2, 1)
                  .applyTo(_txtUrlAddress);
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
                  .hint(defaultWidth, convertHeightInCharsToPixels(20))
                  .span(2, 1)
                  .applyTo(_txtDescription);
         }
         {
            /*
             * Image filename
             */

            _lblImage = UI.createLabel(_container, UI.EMPTY_STRING);
            _lblImage.setText(Messages.Dialog_Equipment_Label_Image);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblImage);

            final Composite imageContainer = new Composite(_container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(2, 1)
                  .applyTo(imageContainer);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(imageContainer);
//            imageContainer.setBackground(UI.SYS_COLOR_YELLOW);
            {
               {
                  _lblImageFilePath = UI.createLabel(imageContainer, UI.EMPTY_STRING);
                  GridDataFactory.fillDefaults()
                        .grab(true, false)
                        .align(SWT.FILL, SWT.CENTER)
                        .applyTo(_lblImageFilePath);
               }
               {
                  final Button btnSelectImage = new Button(imageContainer, SWT.PUSH);
                  btnSelectImage.setText(Messages.app_btn_browse);
                  btnSelectImage.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onImage_Select()));
               }
               {
                  _btnDeleteImage = new Button(imageContainer, SWT.PUSH);
                  _btnDeleteImage.setImage(_imageTrash);
                  _btnDeleteImage.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onImage_Delete()));
               }
            }
            {
               /*
                * Equipment image
                */

               UI.createSpacer_Horizontal(_container);

               final int imageSize = TagManager.getTagContent_ImageSize();

               _canvasEquipmentImage = new Label(_container, SWT.WRAP);
               GridDataFactory.fillDefaults()
                     .span(2, 1)
                     .hint(imageSize, imageSize)
                     .applyTo(_canvasEquipmentImage);
            }
         }
      }
   }

   private void enableControls() {

      if (_isInUIUpdate) {
         return;
      }

      final boolean isCollate = _chkCollate.getSelection();

// SET_FORMATTING_OFF

      _btnDeleteImage            .setEnabled(StringUtils.hasContent(_imageFilePath));

      _lblCollateWith            .setEnabled(isCollate);
      _rdoCollateWith_Next       .setEnabled(isCollate);
      _rdoCollateWith_Previous   .setEnabled(isCollate);

      if (isCollate) {

         _comboDecorator_DateFrom   .show();
         _comboDecorator_Type       .show();

      } else {

         _comboDecorator_DateFrom   .hide();
         _comboDecorator_Type       .hide();
      }

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
   EquipmentPart getService() {

      _service.updateUntilDate();

      return _service;
   }

   private void initUI() {

      _pc = new PixelConverter(_parent);

      _imageCamera = TourbookPlugin.getImageDescriptor(Images.Camera).createImage();
      _imageTrash = TourbookPlugin.getImageDescriptor(Images.App_Trash_Themed).createImage();

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
      UI.disposeResource(_imageTrash);

// SET_FORMATTING_OFF

      _autocomplete_Company   .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_COMPANY);
      _autocomplete_Name      .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_NAME);
      _autocomplete_PriceUnit .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT);
      _autocomplete_Type      .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE);

// SET_FORMATTING_ON

      _state.put(STATE_PRICE_UNIT_DEFAULT, _comboPriceUnit.getText().trim());
   }

   private void onImage_Delete() {

      _imageFilePath = null;

      _lblImageFilePath.setText(UI.EMPTY_STRING);
      _canvasEquipmentImage.setImage(_imageCamera);

      enableControls();
   }

   private void onImage_Select() {

      final String lastSelectedPath = Util.getStateString(_state, STATE_IMAGE_LAST_SELECTED_PATH, null);

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

         _lblImageFilePath.setText(imageFilePath);

         enableControls();
      }
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

      _imageFilePath = _service.getImageFilePath();

      _lblImageFilePath.setText(_imageFilePath == null ? UI.EMPTY_STRING : _imageFilePath);

      loadEquipmentImage(_imageFilePath);

      _isInUIUpdate = false;
   }

   private void updateModelFromUI() {

// SET_FORMATTING_OFF

      final LocalDate dateUsed      = LocalDate.of(_dateUsed.getYear(), _dateUsed.getMonth() + 1, _dateUsed.getDay());

      _service.setEquipment(        _serviceEquipment);

      _service.setCompany(          _comboCompany.getText().trim());
      _service.setName(             _comboName.getText().trim());
      _service.setPartType(         _comboType.getText().trim());
      _service.setDescription(      _txtDescription.getText().trim());
      _service.setUrlAddress(       _txtUrlAddress.getText().trim());

      _service.setImageFilePath(    _lblImageFilePath.getText().trim());

      _service.setIsCollate(        _chkCollate.getSelection());
      _service.setCollateBetween(   _rdoCollateWith_Next.getSelection()
                                          ? EquipmentPart.COLLATED_WITH_NEXT
                                          : EquipmentPart.COLLATED_WITH_PREVIOUS);

      _service.setPrice(            _spinPrice.getSelection() / 100f);
      _service.setPriceUnit(        _comboPriceUnit.getText());

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

      int gdHeight;

      if (equipmentImage != null) {

         // image is available

         _canvasEquipmentImage.setImage(equipmentImage);

         gdHeight = equipmentImage.getBounds().height;

      } else {

         // image is not available

         final boolean isError = errorMessage != null;

         if (isError) {

            // display error

            _canvasEquipmentImage.setImage(null);
            _canvasEquipmentImage.setText(errorMessage);

            gdHeight = SWT.DEFAULT;

         } else {

            // image is not yet set -> display default image

            _canvasEquipmentImage.setImage(_imageCamera);
            _canvasEquipmentImage.setText(UI.EMPTY_STRING);

            gdHeight = _imageCamera.getBounds().height;
         }
      }

      // update layout height
      final GridData gd = (GridData) _canvasEquipmentImage.getLayoutData();
      gd.heightHint = gdHeight;
      _container.layout(true, true);
   }

   private void updateUIFromModel() {

      _isInUIUpdate = true;

      LocalDateTime dateUsed = _service.getDateUsed_Local();

      final long dateUsedMS = TimeTools.toEpochMilli(dateUsed);

      if (dateUsedMS == 0) {
         dateUsed = LocalDateTime.now();
      }

// SET_FORMATTING_OFF

      final int collateWith      = _service.getCollateBetween();

      _chkCollate                .setSelection(_service.isCollate());

      _comboCompany              .setText(_service.getCompany());
      _comboName                 .setText(_service.getName());
      _comboType                 .setText(_service.getPartType());

      _dateUsed                  .setDate(dateUsed.getYear(), dateUsed.getMonthValue() - 1, dateUsed.getDayOfMonth());

      _rdoCollateWith_Next       .setSelection(collateWith == EquipmentPart.COLLATED_WITH_NEXT);
      _rdoCollateWith_Previous   .setSelection(collateWith == EquipmentPart.COLLATED_WITH_PREVIOUS);

      _spinPrice                 .setSelection((int) (_service.getPrice()  * 100));

      _txtDescription            .setText(_service.getDescription());
      _txtUrlAddress             .setText(_service.getUrlAddress());

// SET_FORMATTING_ON

      final String priceUnit = _service.getPriceUnit();
      if (StringUtils.hasContent(priceUnit)) {

         // overwrite default

         _comboPriceUnit.setText(priceUnit);
      }

      _isInUIUpdate = false;
   }

}
