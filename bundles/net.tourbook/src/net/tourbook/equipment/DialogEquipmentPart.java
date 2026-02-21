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
 * Dialog to modify an {@link EquipmentPart}
 */
public class DialogEquipmentPart extends TitleAreaDialog {

   private static final String          ID                                         = "net.tourbook.equipment.DialogEquipmentPart"; //$NON-NLS-1$

   private static final IDialogSettings _state                                     = TourbookPlugin.getState(ID);

   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND      = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND";      //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL      = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL";      //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_SIZE       = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_SIZE";       //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE       = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE";       //$NON-NLS-1$
   private static final String          STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT = "STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT"; //$NON-NLS-1$
   private static final String          STATE_IMAGE_LAST_SELECTED_PATH             = "STATE_IMAGE_LAST_SELECTED_PATH";             //$NON-NLS-1$
   private static final String          STATE_PRICE_UNIT_DEFAULT                   = "STATE_PRICE_UNIT_DEFAULT";                   //$NON-NLS-1$
   private static final String          STATE_SYNC_DATES                           = "STATE_SYNC_DATES";                           //$NON-NLS-1$

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
   private Button                    _chkSyncDates;

   private Combo                     _comboBrand;
   private Combo                     _comboModel;
   private Combo                     _comboPriceUnit;
   private Combo                     _comboSize;
   private Combo                     _comboType;

   private DateTime                  _dateFrom;
   private DateTime                  _dateBuilt;
   private DateTime                  _dateRetired;

   private Label                     _canvasEquipmentImage;

   private Label                     _lblCollate;
   private Label                     _lblImage;
   private Label                     _lblImageFilePath;

   private Spinner                   _spinDistance;
   private Spinner                   _spinPrice;
   private Spinner                   _spinWeight;

   private Text                      _txtDescription;
   private Text                      _txtUrlAddress;

   private AutoComplete_ComboInputMT _autocomplete_Brand;
   private AutoComplete_ComboInputMT _autocomplete_Model;
   private AutoComplete_ComboInputMT _autocomplete_PriceUnit;
   private AutoComplete_ComboInputMT _autocomplete_Size;
   private AutoComplete_ComboInputMT _autocomplete_Type;

   private ControlDecoration         _comboDecorator_Collate;
   private ControlDecoration         _comboDecorator_DateFrom;
   private ControlDecoration         _comboDecorator_Type;

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

      } else {

         _part = part.clone();

         if (isDuplicatePart) {

            // adjust date to today

            final long today = TimeTools.nowInMilliseconds();

            _part.setDateFrom(today);
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

            ? Messages.Dialog_EquipmentPart_Dialog_Message_DuplicatePart
            : _isNewPart

                  ? Messages.Dialog_EquipmentPart_Dialog_Message_CreatePart
                  : Messages.Dialog_EquipmentPart_Dialog_Message_EditPart;

      setTitle(messageTitle.formatted(_partEquipment.getName()));
      setMessage(_part.getName());
   }

   private void createActions() {

   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // OK -> Create/Save
      getButton(IDialogConstants.OK_ID).setText(_isNewPart
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

      updateUIFromModel();

      resoreState();

// FOR DEBUGGING
//    _date.setFocus();

      _comboBrand.setFocus();

      // ensure the UI is created
      _parent.getDisplay().asyncExec(() -> {

         enableControls();
      });

      return dlgContainer;
   }

   private void createUI(final Composite parent) {

      final GridDataFactory gdVertCenter = GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER);

      final int defaultWidth = convertWidthInCharsToPixels(40);

      final int decoratorDistance = 5;

      final Image decorationImage = FieldDecorationRegistry.getDefault()
            .getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
            .getImage();

      final String tooltip = Messages.DialogEquipmentPart_Dialog_EquipmentPart_Label_Tooltip1;
      final String tooltip2 = Messages.DialogEquipmentPart_Dialog_EquipmentPart_Label_Tooltip2;

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
            final Label label = UI.createLabel(_container, Messages.DialogEquipmentPart_Dialog_EquipmentPart_Label_Type);
            gdVertCenter.applyTo(label);

            // autocomplete combo
            _comboType = new Combo(_container, SWT.BORDER | SWT.FLAT);
            _comboType.setText(UI.EMPTY_STRING);
            _comboType.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_comboType);

            _autocomplete_Type = new AutoComplete_ComboInputMT(_comboType);

            /*
             * Add a decoration for this important field
             */
            _comboDecorator_Type = new ControlDecoration(_comboType, SWT.CENTER | SWT.LEFT);
            _comboDecorator_Type.setDescriptionText(tooltip2);
            _comboDecorator_Type.setImage(decorationImage);
            _comboDecorator_Type.setMarginWidth(decoratorDistance);
         }
         UI.createSpacer_Horizontal(_container, 1);
         {
            /*
             * Size
             */
            final Label label = UI.createLabel(_container, Messages.DialogEquipmentPart_Dialog_EquipmentPart_Label_Size);
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
             * Date from
             */
            UI.createLabel(_container, Messages.DialogEquipmentPart_Dialog_EquipmentPart_Label_Date);

            _dateFrom = new DateTime(_container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
            _dateFrom.addSelectionListener(_defaultSelectionListener);

            /*
             * Add a decoration for this important field
             */
            _comboDecorator_DateFrom = new ControlDecoration(_dateFrom, SWT.CENTER | SWT.LEFT);
            _comboDecorator_DateFrom.setDescriptionText(tooltip2);
            _comboDecorator_DateFrom.setImage(decorationImage);
            _comboDecorator_DateFrom.setMarginWidth(decoratorDistance);
         }
         UI.createSpacer_Horizontal(_container, 1);
         UI.createSpacer_Horizontal(_container, 1);
         {
            /*
             * Price
             */

            UI.createLabel(_container, Messages.DialogEquipmentPart_Dialog_EquipmentPart_Label_Price);

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
            _comboPriceUnit.setToolTipText(Messages.DialogEquipmentPart_Dialog_EquipmentPart_Label_Currency);
            _comboPriceUnit.addModifyListener(_defaultModifyListener);

            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .hint(_pc.convertWidthInCharsToPixels(4), SWT.DEFAULT)
                  .applyTo(_comboPriceUnit);

            _autocomplete_PriceUnit = new AutoComplete_ComboInputMT(_comboPriceUnit);
         }
         {
            /*
             * Built date
             */
            final Label label = UI.createLabel(_container, Messages.Dialog_Equipment_Label_DateBuilt);
            gdVertCenter.applyTo(label);

            _dateBuilt = new DateTime(_container, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN);
            _dateBuilt.addSelectionListener(_defaultSelectionListener);

            _chkSyncDates = new Button(_container, SWT.CHECK);
            _chkSyncDates.setText(Messages.DialogEquipmentPart_Dialog_EquipmentPart_Label_Sync);
            _chkSyncDates.setToolTipText(Messages.DialogEquipmentPart_Dialog_EquipmentPart_Label_SyncTooltip);
            _chkSyncDates.addSelectionListener(_defaultSelectionListener);
         }
         UI.createSpacer_Horizontal(_container, 1);
         {
            /*
             * Weight
             */

            UI.createLabel(_container, Messages.DialogEquipmentPart_Dialog_EquipmentPart_Label_Weight);

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
         UI.createSpacer_Horizontal(_container, 1);
         {
            /*
             * Distance first use
             */

            final Label label = UI.createLabel(_container, Messages.DialogEquipmentPart_Dialog_EquipmentPart_Label_InitialDistance);
            label.setToolTipText(Messages.DialogEquipmentPart_Dialog_EquipmentPart_Label_InitialDistance_Tooltip);

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
         {
            /*
             * Collate tours
             */

            _lblCollate = UI.createLabel(_container, Messages.DialogEquipmentPart_Dialog_EquipmentPart_Label_Collate);
            _lblCollate.setToolTipText(tooltip);
            gdVertCenter.applyTo(_lblCollate);

            _chkCollate = new Button(_container, SWT.CHECK);
            _chkCollate.setText(Messages.DialogEquipmentPart_Dialog_EquipmentPart_Checkbox_IncludeInCollatedTours);
            _chkCollate.setToolTipText(tooltip);
            _chkCollate.addSelectionListener(_defaultSelectionListener);

            GridDataFactory.fillDefaults().grab(true, false).span(6, 1).applyTo(_chkCollate);

            /*
             * Add a decoration for this important field
             */
            _comboDecorator_Collate = new ControlDecoration(_chkCollate, SWT.CENTER | SWT.LEFT);
            _comboDecorator_Collate.setDescriptionText(tooltip);
            _comboDecorator_Collate.setImage(decorationImage);
            _comboDecorator_Collate.setMarginWidth(decoratorDistance);
         }
         {
            /*
             * Website
             */
            final Label label = UI.createLabel(_container, Messages.DialogEquipmentPart_Dialog_EquipmentPart_Label_Website);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

            _txtUrlAddress = new Text(_container, SWT.BORDER);
            _txtUrlAddress.addModifyListener(e -> onModify());
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .hint(defaultWidth, SWT.DEFAULT)
                  .span(6, 1)
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
                  .hint(defaultWidth, convertHeightInCharsToPixels(10))
                  .span(6, 1)
                  .applyTo(_txtDescription);
         }
         {
            /*
             * Image filename
             */

            _lblImage = UI.createLabel(_container, UI.EMPTY_STRING);
            _lblImage.setText(Messages.DialogEquipmentPart_Dialog_EquipmentPart_Label_Image);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblImage);

            final Composite imageContainer = new Composite(_container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(6, 1)
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
                     .span(6, 1)
                     .hint(imageSize, imageSize)
                     .applyTo(_canvasEquipmentImage);
            }
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

            _dateFrom,
            _dateBuilt,
            _chkSyncDates,
            _dateRetired,
            _chkCollate,

            _txtUrlAddress,
            _txtDescription,
      });
   }

   private void enableControls() {

      if (_isInUIUpdate) {
         return;
      }

      final boolean isCollate = _chkCollate.getSelection();
      final boolean isSyncDates = _chkSyncDates.getSelection();
      final boolean canEditBuiltDate = isSyncDates == false;

      _dateBuilt.setEnabled(canEditBuiltDate);
      _btnDeleteImage.setEnabled(StringUtils.hasContent(_imageFilePath));

      if (isCollate) {

         _comboDecorator_DateFrom.show();
         _comboDecorator_Type.show();

      } else {

         _comboDecorator_DateFrom.hide();
         _comboDecorator_Type.hide();
      }

      // OK button
      final boolean isValid = _isNewPart && _isModified == false

            // disable OK when new and not modified but do NOT display validation message
            ? false

            : isDataValid();
      getButton(IDialogConstants.OK_ID).setEnabled(isValid);
   }

   private void fillUI() {

// SET_FORMATTING_OFF

      UI.fillUI_Combobox(_comboBrand,     EquipmentManager.getCachedFields_AllBrands());
      UI.fillUI_Combobox(_comboModel,     EquipmentManager.getCachedFields_AllModels());
      UI.fillUI_Combobox(_comboPriceUnit, EquipmentManager.getCachedFields_AllPriceUnits());
      UI.fillUI_Combobox(_comboSize,      EquipmentManager.getCachedFields_AllSizes());
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
   EquipmentPart getPart() {

      _part.updateUntilDate();

      return _part;
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

      final String brand = _comboBrand.getText().trim();
      final String model = _comboModel.getText().trim();

      if (StringUtils.hasContent(brand) == false && StringUtils.hasContent(model) == false) {

         setErrorMessage(Messages.DialogEquipmentPart_Dialog_EquipmentPart_Error_BrandModelIsEmpty);

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
                  final String errorMessage = Messages.DialogEquipmentPart_Dialog_EquipmentPart_Error_CannotLoadImage.formatted(
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
      UI.disposeResource(_imageTrash);
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

      fileDialog.setText(Messages.DialogEquipmentPart_Dialog_EquipmentPart_FileDialog_Text);
      fileDialog.setFilterPath(lastSelectedPath);

      final String imageExtensions = ImageUtils.getImageExtensions();

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

      final boolean isSyncDates = _chkSyncDates.getSelection();

      if (isSyncDates) {

         _dateBuilt.setDate(_dateFrom.getYear(), _dateFrom.getMonth(), _dateFrom.getDay());
      }

      _isModified = true;

      enableControls();
   }

   private void resoreState() {

      _isInUIUpdate = true;

// SET_FORMATTING_OFF

      _autocomplete_Brand     .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND);
      _autocomplete_Model     .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL);
      _autocomplete_PriceUnit .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT);
      _autocomplete_Size      .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_SIZE);
      _autocomplete_Type      .restoreState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE);

      _chkSyncDates           .setSelection(Util.getStateBoolean(_state, STATE_SYNC_DATES, true));

      _comboPriceUnit         .setText(Util.getStateString(_state, STATE_PRICE_UNIT_DEFAULT, UI.EMPTY_STRING));

// SET_FORMATTING_ON

      _imageFilePath = _part.getImageFilePath();

      _lblImageFilePath.setText(_imageFilePath == null ? UI.EMPTY_STRING : _imageFilePath);

      loadEquipmentImage(_imageFilePath);

      _isInUIUpdate = false;
   }

   private void saveState() {

// SET_FORMATTING_OFF

      _autocomplete_Brand     .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_BRAND);
      _autocomplete_Model     .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_MODEL);
      _autocomplete_PriceUnit .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_PRICE_UNIT);
      _autocomplete_Size      .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_SIZE);
      _autocomplete_Type      .saveState(_state, STATE_AUTOCOMPLETE_POPUP_HEIGHT_TYPE);

      _state.put(STATE_PRICE_UNIT_DEFAULT,   _comboPriceUnit.getText().trim());
      _state.put(STATE_SYNC_DATES,           _chkSyncDates.getSelection());

// SET_FORMATTING_ON
   }

   private void updateModelFromUI() {

// SET_FORMATTING_OFF

      final LocalDate dateFrom      = LocalDate.of(_dateFrom.getYear(),    _dateFrom.getMonth() + 1,     _dateFrom.getDay());
      final LocalDate dateBuilt     = LocalDate.of(_dateBuilt.getYear(),   _dateBuilt.getMonth() + 1,    _dateBuilt.getDay());
      final LocalDate dateRetired   = LocalDate.of(_dateRetired.getYear(), _dateRetired.getMonth() + 1,  _dateRetired.getDay());

      _part.setEquipment(        _partEquipment);

      _part.setBrand(            _comboBrand.getText().trim());
      _part.setModel(            _comboModel.getText().trim());
      _part.setType(             _comboType.getText().trim());
      _part.setDescription(      _txtDescription.getText().trim());
      _part.setUrlAddress(       _txtUrlAddress.getText().trim());

      _part.setImageFilePath(    _lblImageFilePath.getText().trim());

      _part.setDistanceFirstUse( _spinDistance.getSelection());
      _part.setIsCollate(        _chkCollate.getSelection());
      _part.setPrice(            _spinPrice.getSelection() / 100f);
      _part.setPriceUnit(        _comboPriceUnit.getText());
      _part.setSize(             _comboSize.getText().trim());
      _part.setWeight(           _spinWeight.getSelection() / 1000f);

      _part.setDateFrom(         TimeTools.toEpochMilli(dateFrom));
      _part.setDateBuilt(        TimeTools.toEpochMilli(dateBuilt));
      _part.setDateRetired(      TimeTools.toEpochMilli(dateRetired));

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

// SET_FORMATTING_OFF

      /*
       * Set date default values
       */
      LocalDateTime dateFrom        = _part.getDateFrom_Local();
      LocalDateTime dateBuilt       = _part.getDateBuilt_Local();
      LocalDateTime dateRetired     = _part.getDateRetired_Local();

      final long dateMS             = TimeTools.toEpochMilli(dateFrom);
      final long dateBuiltMS        = TimeTools.toEpochMilli(dateBuilt);
      final long dateRetiredMS      = TimeTools.toEpochMilli(dateRetired);

      if (dateMS == 0) {
         dateFrom = LocalDateTime.now();
      }

      if (dateBuiltMS == 0) {
         dateBuilt = LocalDateTime.now();
      }

      if (dateRetiredMS == 0) {
         dateRetired = LocalDateTime.of(2099,1,1,0,0);
      }

      _chkCollate       .setSelection(_part.isCollate());

      _comboBrand       .setText(_part.getBrand());
      _comboModel       .setText(_part.getModel());
      _comboSize        .setText(_part.getSize());
      _comboType        .setText(_part.getType());

      _dateFrom         .setDate(dateFrom.getYear(),     dateFrom.getMonthValue() - 1,    dateFrom.getDayOfMonth());
      _dateBuilt        .setDate(dateBuilt.getYear(),    dateBuilt.getMonthValue() - 1,   dateBuilt.getDayOfMonth());
      _dateRetired      .setDate(dateRetired.getYear(),  dateRetired.getMonthValue() - 1, dateRetired.getDayOfMonth());

      _spinDistance     .setSelection((int) (_part.getDistanceFirstUse()));
      _spinPrice        .setSelection((int) (_part.getPrice()  * 100));
      _spinWeight       .setSelection((int) (_part.getWeight() * 1000));

      _txtDescription   .setText(_part.getDescription());
      _txtUrlAddress    .setText(_part.getUrlAddress());

// SET_FORMATTING_ON

      final String priceUnit = _part.getPriceUnit();
      if (StringUtils.hasContent(priceUnit)) {

         // overwrite default

         _comboPriceUnit.setText(priceUnit);
      }

      _isInUIUpdate = false;
   }

}
