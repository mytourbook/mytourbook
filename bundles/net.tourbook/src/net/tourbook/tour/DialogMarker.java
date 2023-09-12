/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import static org.eclipse.swt.events.FocusListener.focusGainedAdapter;
import static org.eclipse.swt.events.KeyListener.keyPressedAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import net.sf.swtaddons.autocomplete.combo.AutocompleteComboInput;
import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.form.SashBottomFixedForm;
import net.tourbook.common.form.SashLeftFixedForm;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.tourChart.ChartLabelMarker;
import net.tourbook.ui.tourChart.ITourMarkerSelectionListener;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

public class DialogMarker extends TitleAreaDialog implements ITourMarkerSelectionListener, ITourMarkerModifyListener {

   private static final String      DIALOG_SETTINGS_POSITION = "marker_position";                       //$NON-NLS-1$
   private static final String      STATE_INNER_SASH_HEIGHT  = "STATE_INNER_SASH_HEIGHT";               //$NON-NLS-1$
   private static final String      STATE_OUTER_SASH_WIDTH   = "STATE_OUTER_SASH_WIDTH";                //$NON-NLS-1$

   private static final int         OFFSET_PAGE_INCREMENT    = 20;
   private static final int         OFFSET_MAX               = 200;

   private final IDialogSettings    _state                   = TourbookPlugin.getState("DialogMarker"); //$NON-NLS-1$

   private TourChart                _tourChart;
   private TourData                 _tourData;

   /**
    * marker which is currently selected
    */
   private TourMarker               _selectedTourMarker;

   /**
    * backup for the selected tour marker
    */
   private TourMarker               _backupMarker            = new TourMarker();

   private Set<TourMarker>          _originalTourMarkers;
   private HashSet<TourMarker>      _dialogTourMarkers;

   /**
    * initial tour marker
    */
   private TourMarker               _initialTourMarker;

   private ModifyListener           _defaultModifyListener;
   private MouseWheelListener       _defaultMouseWheelListener;
   private SelectionListener        _defaultSelectionListener;

   private boolean                  _isOkPressed;
   private boolean                  _isInCreateUI;
   private boolean                  _isUpdateUI;
   private boolean                  _isSetXSlider            = true;

   private NumberFormat             _nf3                     = NumberFormat.getNumberInstance();

   private int                      _contentWidthHint;

   /**
    * Contains the controls which are displayed in the first column, these controls are used to get
    * the maximum width and set the first column within the different section to the same width.
    */
   private final ArrayList<Control> _firstColumnControls     = new ArrayList<>();

   /*
    * none UI
    */
   private PixelConverter _pc;

   /*
    * UI controls
    */
   private Sash                _sashInner;
   private SashLeftFixedForm   _sashOuterForm;
   private SashBottomFixedForm _sashInnerForm;
   private Composite           _sashOuterFixedPart;
   private Composite           _sashInnerFixedPart;

   private TableViewer         _markerViewer;

   private Button              _btnDelete;
   private Button              _btnHideAll;
   private Button              _btnPasteText;
   private Button              _btnPasteUrl;
   private Button              _btnShowAll;
   private Button              _btnUndo;
   private Button              _chkVisibility;

   private Combo               _comboLabelPosition;
   private Combo               _comboMarkerName;

   private Group               _groupText;
   private Group               _groupUrl;

   private Image               _imagePaste;

   private Label               _lblDescription;
   private Label               _lblLabel;
   private Label               _lblLabelOffsetX;
   private Label               _lblLabelOffsetY;
   private Label               _lblLabelPosition;
   private Label               _lblLinkText;
   private Label               _lblLinkUrl;

   private Spinner             _spinLabelOffsetX;
   private Spinner             _spinLabelOffsetY;

   private Text                _txtDescription;
   private Text                _txtUrlAddress;
   private Text                _txtUrlText;

   {
      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeMarkerUI());

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeMarkerUI();
      };

      _defaultModifyListener = modifyEvent -> {
         if (_isUpdateUI) {
            return;
         }
         onChangeMarkerUI();
      };
   }

   private final class MarkerEditingSupport extends EditingSupport {

      private final CheckboxCellEditor _cellEditor;

      private MarkerEditingSupport(final TableViewer tableViewer) {

         super(tableViewer);

         _cellEditor = new CheckboxCellEditor(tableViewer.getTable());
      }

      @Override
      protected boolean canEdit(final Object element) {
         return true;
      }

      @Override
      protected CellEditor getCellEditor(final Object element) {
         return _cellEditor;
      }

      @Override
      protected Object getValue(final Object element) {

         if (element instanceof TourMarker) {

            final TourMarker tourMarker = (TourMarker) element;

            return tourMarker.isMarkerVisible() ? Boolean.TRUE : Boolean.FALSE;
         }

         return Boolean.FALSE;
      }

      @Override
      protected void setValue(final Object element, final Object value) {

         if (element instanceof TourMarker) {

            final TourMarker tourMarker = (TourMarker) element;
            final Boolean isVisible = (Boolean) value;

            updateUI_TourMarker(tourMarker, isVisible);
         }
      }
   }

   /**
    * Sort the markers by time
    */
   private static class MarkerViewerComparator extends ViewerComparator {
      @Override
      public int compare(final Viewer viewer, final Object obj1, final Object obj2) {
//			return ((TourMarker) (obj1)).getTime() - ((TourMarker) (obj2)).getTime();
// time is disabled because it's not always available in gpx files
         return ((TourMarker) (obj1)).getSerieIndex() - ((TourMarker) (obj2)).getSerieIndex();
      }
   }

   private class MarkerViewerContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         if (_tourData == null) {
            return new Object[0];
         } else {
            return _dialogTourMarkers.toArray();
         }
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   /**
    * @param parentShell
    * @param tourData
    * @param initialTourMarker
    *           TourMarker which is selected when the dialog is opened
    */
   public DialogMarker(final Shell parentShell, final TourData tourData, final TourMarker initialTourMarker) {

      super(parentShell);

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

      // set icon for the window
      setDefaultImage(TourbookPlugin.getImageDescriptor(Images.TourMarker).createImage());

      _tourData = tourData;

      // create a shallow copy
      _originalTourMarkers = new HashSet<>();
      _originalTourMarkers.addAll(_tourData.getTourMarkers());

      /*
       * make a backup copy of the tour markers, modify the original data so that the tour chart
       * displays the modifications
       */
      _dialogTourMarkers = new HashSet<>();

      for (final TourMarker tourMarker : _originalTourMarkers) {
         _dialogTourMarkers.add(tourMarker.clone());
      }

      _tourData.setTourMarkers(_dialogTourMarkers);

      _initialTourMarker = initialTourMarker;
   }

   /**
    * remove selected markers from the view and update dependent structures
    */
   private void actionDeleteMarker() {

      final IStructuredSelection markerSelection = (IStructuredSelection) _markerViewer.getSelection();
      final TourMarker selectedMarker = (TourMarker) markerSelection.getFirstElement();

      deleteTourMarker(selectedMarker);
   }

   private void actionPastText(final Text textControl) {

      final Clipboard cb = new Clipboard(_groupUrl.getDisplay());
      try {

         final TextTransfer transfer = TextTransfer.getInstance();

         final String transferText = (String) cb.getContents(transfer);
         if (transferText != null) {
            try {

               /*
                * !!! It needs 2 times to be converted to get the correct text string !!!
                */
               final URI uri = new URI(transferText);
               final URI decodedURI = new URI(
                     uri.getScheme(),
                     uri.getUserInfo(),
                     uri.getHost(),
                     uri.getPort(),
                     uri.getPath(),
                     uri.getQuery(),
                     uri.getFragment());

               textControl.setText(decodedURI.toString());

            } catch (final Exception e) {

               MessageDialog.openInformation(
                     getShell(),
                     Messages.Dlg_TourMarker_MsgBox_WrongFormat_Title,
                     NLS.bind(Messages.Dlg_TourMarker_MsgBox_WrongFormat_Message, transferText));
            }
         }
      } finally {
         cb.dispose();
      }
   }

   private void actionShowHideAll(final boolean isVisible) {

      for (final TourMarker tourMarker : _dialogTourMarkers) {
         tourMarker.setMarkerVisible(isVisible);
      }

      /*
       * Update UI
       */
      // controls
      updateUI_FromModel();

      // viewer+chart
      final TourMarker[] allTourMarker = _dialogTourMarkers.toArray(new TourMarker[_dialogTourMarkers.size()]);
      _markerViewer.update(allTourMarker, null);
      _tourChart.updateUI_MarkerLayer(true);

      enableControls();
   }

   public void addTourMarker(final TourMarker newTourMarker) {

      if (newTourMarker == null) {
         return;
      }

      // update data model, add new marker to the marker list
      _dialogTourMarkers.add(newTourMarker);

      // update the viewer and select the new marker
      _markerViewer.refresh();
      _markerViewer.setSelection(new StructuredSelection(newTourMarker), true);

      _comboMarkerName.setFocus();

      // update chart
      _tourChart.updateUI_MarkerLayer(true);
   }

   @Override
   public boolean close() {

      if (_isOkPressed) {

         /*
          * the markers are already set into the tour data because the original values are
          * modified
          */

         restoreState_VisibleType();

      } else {

         /*
          * when OK is not pressed, revert tour markers, this happens when the Cancel button is
          * pressed or when the window is closed
          */
         _tourData.setTourMarkers(_originalTourMarkers);
      }

      saveState();

      return super.close();
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.Dlg_TourMarker_Dlg_title);

      shell.addDisposeListener(disposeEvent -> dispose());
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      final String okText = net.tourbook.ui.UI.convertOKtoSaveUpdateButton(_tourData);

      getButton(IDialogConstants.OK_ID).setText(okText);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      _isInCreateUI = true;

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      initUI(parent);

      createUI(dlgContainer);

      setTitle(Messages.Dlg_TourMarker_Dlg_title);
      setMessage(Messages.Dlg_TourMarker_Dlg_Message);

      fillUI();

      // update marker viewer
      _markerViewer.setInput(this);

      if (_initialTourMarker == null) {
         // select first marker if any are available
         final Object firstElement = _markerViewer.getElementAt(0);
         if (firstElement != null) {
            _markerViewer.setSelection(new StructuredSelection(firstElement), true);
         }
      } else {
         // select initial tour marker
         _markerViewer.setSelection(new StructuredSelection(_initialTourMarker), true);
      }

      enableControls();

      _isInCreateUI = false;

      Display.getCurrent().asyncExec(() -> {

         if (_comboMarkerName.isDisposed()) {
            return;
         }

         _comboMarkerName.setFocus();
      });

      return dlgContainer;
   }

   private void createUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(_pc.convertWidthInCharsToPixels(180), _pc.convertHeightInCharsToPixels(40))
            .applyTo(shellContainer);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite sashContainer = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .applyTo(sashContainer);
         GridLayoutFactory.swtDefaults().applyTo(sashContainer);
         {
            // left part
            _sashOuterFixedPart = createUI_10_LeftPart(sashContainer);

            // sash
            final Sash sash = new Sash(sashContainer, SWT.VERTICAL);
            UI.addSashColorHandler(sash);

            // right part
            final Composite chartContainer = createUI_20_RightPart(sashContainer);

            _sashOuterForm = new SashLeftFixedForm(
                  sashContainer,
                  _sashOuterFixedPart,
                  sash,
                  chartContainer,
                  50);
         }

         createUI_90_MarkerActions(shellContainer);

         /*
          * !!! UI must be restored and column image size must be set before columns are
          * equalized, otherwise the column height is wrong !!!
          */
         restoreState();

         // compute width for all controls and equalize column width for the different sections
         sashContainer.layout(true, true);
         UI.setEqualizeColumWidths(_firstColumnControls, _pc.convertWidthInCharsToPixels(2));
      }
   }

   private Composite createUI_10_LeftPart(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .extendedMargins(5, 5, 0, 0)
            .applyTo(container);
      {
         final Composite sashContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(sashContainer);

         {
            // top part
            final Composite flexiblePart = createUI_40_MarkerList(sashContainer);

            // sash
            _sashInner = new Sash(sashContainer, SWT.HORIZONTAL);
            UI.addSashColorHandler(_sashInner);

            // bottom part
            _sashInnerFixedPart = createUI_50_MarkerDetails(sashContainer);

            _sashInnerForm = new SashBottomFixedForm(
                  sashContainer,
                  flexiblePart,
                  _sashInner,
                  _sashInnerFixedPart);
         }
      }

      return container;
   }

   private Composite createUI_20_RightPart(final Composite dlgContainer) {

      final Composite chartContainer = new Composite(dlgContainer, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(chartContainer);
      GridLayoutFactory.fillDefaults()
            .extendedMargins(5, 5, 0, 0)
            .applyTo(chartContainer);
      {
         createUI_80_TourChart(chartContainer);
      }

      return chartContainer;
   }

   /**
    * container: marker list
    *
    * @return
    */
   private Composite createUI_40_MarkerList(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .applyTo(container);
      GridLayoutFactory.fillDefaults()
            .extendedMargins(0, 0, 0, 5)
            .applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         // label: markers
         final Label label = new Label(container, SWT.NONE);
         label.setText(Messages.Dlg_TourMarker_Label_markers);

         createUI_42_MarkerViewer(container);
      }

      return container;
   }

   private Composite createUI_42_MarkerViewer(final Composite parent) {

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(layoutContainer);

      final TableColumnLayout tableLayout = new TableColumnLayout();
      layoutContainer.setLayout(tableLayout);

      /*
       * create table
       */
      final Table table = new Table(layoutContainer, SWT.FULL_SELECTION | SWT.CHECK);

      table.setLayout(new TableLayout());
      table.setHeaderVisible(true);

      table.addKeyListener(keyPressedAdapter(keyEvent -> {

         if (keyEvent.character == ' ') {
            toggleMarkerVisibility();
         }
      }));

      _markerViewer = new TableViewer(table);

      /*
       * create columns
       */
      defineColumn_1stHidden(tableLayout);//				// 0
      defineColumn_Distance(tableLayout);//				// 1
      defineColumn_IsVisible(tableLayout);//				// 2
      defineColumn_Marker(tableLayout);//					// 4
      defineColumn_Description(tableLayout);//			// 5
      defineColumn_Url(tableLayout);//					// 6
      defineColumn_OffsetX(tableLayout);//				// 7
      defineColumn_OffsetY(tableLayout);//				// 8

      /*
       * create table viewer
       */
      _markerViewer.setContentProvider(new MarkerViewerContentProvider());
      _markerViewer.setComparator(new MarkerViewerComparator());

      _markerViewer.addSelectionChangedListener(selectionChangedEvent -> {
         final StructuredSelection selection = (StructuredSelection) selectionChangedEvent.getSelection();
         if (selection != null) {
            onSelectMarker((TourMarker) selection.getFirstElement());
         }
      });

      _markerViewer.addDoubleClickListener(doubleClickEvent -> _comboMarkerName.setFocus());

      return layoutContainer;
   }

   private Composite createUI_50_MarkerDetails(final Composite parent) {

      /*
       * container: marker details
       */
      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         /*
          * Text
          */
         _groupText = new Group(container, SWT.NONE);
         GridDataFactory.fillDefaults()
               .grab(true, true)
               /*
                * !!! This min size ensures that the upper part (description) is NOT hidden
                * before the other parts (url, image) when the vertical splitter is moved. It
                * took a while to find this solution :-(
                */
               .minSize(SWT.DEFAULT, _pc.convertVerticalDLUsToPixels(65))
               .applyTo(_groupText);
         _groupText.setText(Messages.Dlg_TourMarker_Group_Label);
         GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_groupText);
         {
            createUI_52_Label(_groupText);
            createUI_54_Description(_groupText);
            createUI_56_Label_Position(_groupText);
         }

         /*
          * Url
          */
         _groupUrl = new Group(container, SWT.NONE);
         _groupUrl.setText(Messages.Dlg_TourMarker_Group_Url);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_groupUrl);
         GridLayoutFactory.swtDefaults().numColumns(3).applyTo(_groupUrl);
         {
            createUI_60_Url(_groupUrl);
         }

         createUI_70_Visibility(container);
      }

      return container;
   }

   private void createUI_52_Label(final Composite parent) {

      /*
       * Marker name
       */
      {
         // Label
         _lblLabel = new Label(parent, SWT.NONE);
         _firstColumnControls.add(_lblLabel);
         _lblLabel.setText(Messages.Dlg_TourMarker_Label_Label);

         // Combo
         _comboMarkerName = new Combo(parent, SWT.BORDER | SWT.FLAT);
         _comboMarkerName.addModifyListener(_defaultModifyListener);
         GridDataFactory.fillDefaults()
               .grab(true, false)

               // !!! hint must be set otherwise the control could be too large, because the width is adjusted to the content
               .hint(_contentWidthHint, SWT.DEFAULT)
               .applyTo(_comboMarkerName);
      }
   }

   private void createUI_54_Description(final Composite parent) {

      {
         /*
          * Description
          */

         // label
         _lblDescription = new Label(parent, SWT.NONE);
         _lblDescription.setText(Messages.Dlg_TourMarker_Label_Description);
         GridDataFactory.fillDefaults()
               .align(SWT.BEGINNING, SWT.BEGINNING)
               .applyTo(_lblDescription);

         // text
         _txtDescription = new Text(parent,
               SWT.BORDER
                     | SWT.WRAP
                     | SWT.V_SCROLL
                     | SWT.H_SCROLL);
         _txtDescription.addModifyListener(_defaultModifyListener);
         GridDataFactory.fillDefaults()
               .grab(true, true)

               // !!! hint must be set otherwise the control could be too large, because the width is adjusted to the content
               .hint(_contentWidthHint, SWT.DEFAULT)
               .applyTo(_txtDescription);
      }
   }

   private void createUI_56_Label_Position(final Composite parent) {

      /*
       * Position
       */
      {
         // label
         _lblLabelPosition = new Label(parent, SWT.NONE);
         _firstColumnControls.add(_lblLabelPosition);
         _lblLabelPosition.setText(Messages.Dlg_TourMarker_Label_position);
         _lblLabelPosition.setToolTipText(Messages.Dlg_TourMarker_Label_Position_Tooltip);

         final Composite container = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            /*
             * Combo
             */
            {
               _comboLabelPosition = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
               _comboLabelPosition.setVisibleItemCount(20);
               _comboLabelPosition.addSelectionListener(_defaultSelectionListener);
            }

            /*
             * Offset
             */
            final Composite valueContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.FILL)
                  .applyTo(valueContainer);
            GridLayoutFactory.fillDefaults().numColumns(4).applyTo(valueContainer);
//				valueContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
            {
               /*
                * Horizontal offset
                */
               {
                  // Label
                  _lblLabelOffsetX = new Label(valueContainer, SWT.NONE);
                  _lblLabelOffsetX.setText(Messages.Dlg_TourMarker_Label_OffsetHorizontal);
                  _lblLabelOffsetX.setToolTipText(Messages.Tour_Marker_Column_horizontal_offset_tooltip);
                  GridDataFactory.fillDefaults()
                        .align(SWT.FILL, SWT.CENTER)
//								.indent(_pc.convertWidthInCharsToPixels(2), 0)
                        .applyTo(_lblLabelOffsetX);

                  // Spinner
                  _spinLabelOffsetX = new Spinner(valueContainer, SWT.BORDER);
                  _spinLabelOffsetX.setMinimum(-OFFSET_MAX);
                  _spinLabelOffsetX.setMaximum(OFFSET_MAX);
                  _spinLabelOffsetX.setPageIncrement(OFFSET_PAGE_INCREMENT);
                  _spinLabelOffsetX.addSelectionListener(_defaultSelectionListener);
                  _spinLabelOffsetX.addMouseWheelListener(_defaultMouseWheelListener);
               }

               /*
                * Vertical offset
                */
               {
                  // Label
                  _lblLabelOffsetY = new Label(valueContainer, SWT.NONE);
                  _lblLabelOffsetY.setText(Messages.Dlg_TourMarker_Label_OffsetVertical);
                  _lblLabelOffsetY.setToolTipText(Messages.Tour_Marker_Column_vertical_offset_tooltip);
                  GridDataFactory.fillDefaults()
                        .align(SWT.FILL, SWT.CENTER)
                        .applyTo(_lblLabelOffsetY);

                  // Spinner
                  _spinLabelOffsetY = new Spinner(valueContainer, SWT.BORDER);
                  _spinLabelOffsetY.setMinimum(-OFFSET_MAX);
                  _spinLabelOffsetY.setMaximum(OFFSET_MAX);
                  _spinLabelOffsetY.setPageIncrement(OFFSET_PAGE_INCREMENT);
                  _spinLabelOffsetY.addSelectionListener(_defaultSelectionListener);
                  _spinLabelOffsetY.addMouseWheelListener(_defaultMouseWheelListener);
               }
            }
         }
      }
   }

   private void createUI_60_Url(final Composite parent) {

      final FocusListener focusListenerSelectAllText = focusGainedAdapter(focusEvent -> {

         /*
          * !!! This feature is not working for all cases !!!
          */
         ((Text) focusEvent.widget).selectAll();
      });

      /*
       * Link Text
       */
      {
         // label
         _lblLinkText = new Label(parent, SWT.NONE);
         _lblLinkText.setText(Messages.Dlg_TourMarker_Label_LinkText);
         _lblLinkText.setToolTipText(Messages.Dlg_TourMarker_Label_LinkText_Tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_lblLinkText);

         // text
         _txtUrlText = new Text(parent, SWT.BORDER);
         _txtUrlText.addModifyListener(_defaultModifyListener);
         _txtUrlText.addFocusListener(focusListenerSelectAllText);
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .applyTo(_txtUrlText);

         // paste
         _btnPasteText = new Button(parent, SWT.NONE);
         _btnPasteText.setImage(_imagePaste);
         _btnPasteText.setToolTipText(Messages.Dlg_TourMarker_Button_PasteFromClipboard_Tooltip);
         _btnPasteText.addSelectionListener(widgetSelectedAdapter(selectionEvent -> actionPastText(_txtUrlText)));
      }

      /*
       * Link Url
       */
      {
         // label
         _lblLinkUrl = new Label(parent, SWT.NONE);
         _lblLinkUrl.setText(Messages.Dlg_TourMarker_Label_LinkUrl);
         _lblLinkUrl.setToolTipText(Messages.Dlg_TourMarker_Label_LinkUrl_Tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_lblLinkUrl);

         // text
         _txtUrlAddress = new Text(parent, SWT.BORDER);
         _txtUrlAddress.addFocusListener(focusListenerSelectAllText);
         _txtUrlAddress.addModifyListener(_defaultModifyListener);
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .applyTo(_txtUrlAddress);

         // paste
         _btnPasteUrl = new Button(parent, SWT.NONE);
         _btnPasteUrl.setImage(_imagePaste);
         _btnPasteUrl.setToolTipText(Messages.Dlg_TourMarker_Button_PasteFromClipboard_Tooltip);
         _btnPasteUrl.addSelectionListener(widgetSelectedAdapter(selectionEvent -> actionPastText(_txtUrlText)));
         GridDataFactory.fillDefaults()
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_btnPasteUrl);
      }
   }

   private void createUI_70_Visibility(final Composite parent) {

      {
         /*
          * Visibility
          */
         _chkVisibility = new Button(parent, SWT.CHECK);
         _chkVisibility.setText(Messages.Dlg_TourMarker_Checkbox_MarkerVisibility);
         _chkVisibility.setToolTipText(OtherMessages.TOUR_MARKER_COLUMN_IS_VISIBLE_TOOLTIP);
         _chkVisibility.addSelectionListener(widgetSelectedAdapter(selectionEvent -> toggleMarkerVisibility()));
         GridDataFactory.fillDefaults()
//					.span(2, 1)
//					.align(SWT.END, SWT.FILL)
               .applyTo(_chkVisibility);
      }
   }

   /**
    * create tour chart with new marker
    */
   private void createUI_80_TourChart(final Composite parent) {

      _tourChart = new TourChart(parent, SWT.FLAT, null, _state);
      _tourChart.setShowZoomActions(true);
      _tourChart.setShowSlider(true);
      _tourChart.setContextProvider(new DialogMarkerTourChartContextProvider(this), true);

      _tourChart.setIsDisplayedInDialog(true);

      GridDataFactory.fillDefaults()
            .grab(true, true)
//            .hint(_pc.convertWidthInCharsToPixels(320), _pc.convertHeightInCharsToPixels(40))
            .applyTo(_tourChart);

      _tourChart.addTourMarkerSelectionListener(this);
      _tourChart.addTourMarkerModifyListener(this);
      _tourChart.addSliderMoveListener(selectionChartInfo -> {

         if (_isInCreateUI) {
            return;
         }

         TourManager.fireEventWithCustomData(TourEventId.SLIDER_POSITION_CHANGED, selectionChartInfo, null);
      });

      // set title
      _tourChart.addDataModelListener(chartDataModel -> chartDataModel.setTitle(TourManager.getTourTitleDetailed(_tourData)));

      final TourChartConfiguration chartConfig = TourManager.createDefaultTourChartConfig(_state);
      _tourChart.updateTourChart(_tourData, chartConfig, false);
   }

   private void createUI_90_MarkerActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults()
            .extendedMargins(5, 0, 10, 0)
            .numColumns(4)
            .applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         /*
          * button: delete
          */
         _btnDelete = new Button(container, SWT.NONE);
         _btnDelete.setText(Messages.Dlg_TourMarker_Button_delete);
         _btnDelete.setToolTipText(Messages.Dlg_TourMarker_Button_delete_tooltip);
         _btnDelete.addSelectionListener(widgetSelectedAdapter(selectionEvent -> actionDeleteMarker()));
         setButtonLayoutData(_btnDelete);

         /*
          * button: undo
          */
         _btnUndo = new Button(container, SWT.NONE);
         _btnUndo.getLayoutData();
         _btnUndo.setText(Messages.Dlg_TourMarker_Button_undo);
         _btnUndo.setToolTipText(Messages.Dlg_TourMarker_Button_undo_tooltip);
         _btnUndo.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
            _selectedTourMarker.restoreMarkerFromBackup(_backupMarker);
            updateUI_FromModel();
            onChangeMarkerUI();
         }));
         setButtonLayoutData(_btnUndo);

         /*
          * button: show all
          */
         _btnShowAll = new Button(container, SWT.NONE);
         _btnShowAll.getLayoutData();
         _btnShowAll.setText(Messages.Dlg_TourMarker_Button_ShowAllMarker);
         _btnShowAll.setToolTipText(Messages.Dlg_TourMarker_Button_ShowAllMarker_Tooltip);
         _btnShowAll.addSelectionListener(widgetSelectedAdapter(selectionEvent -> actionShowHideAll(true)));
         setButtonLayoutData(_btnShowAll);

         /*
          * button: hide all
          */
         _btnHideAll = new Button(container, SWT.NONE);
         _btnHideAll.getLayoutData();
         _btnHideAll.setText(Messages.Dlg_TourMarker_Button_HideAllMarker);
         _btnHideAll.setToolTipText(Messages.Dlg_TourMarker_Button_HideAllMarker_Tooltip);
         _btnHideAll.addSelectionListener(widgetSelectedAdapter(selectionEvent -> actionShowHideAll(false)));
         setButtonLayoutData(_btnHideAll);
      }
   }

   /**
    * column: hidden column to show first visible column with right alignment
    */
   private void defineColumn_1stHidden(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.TRAIL);
      final TableColumn tc = tvc.getColumn();

      tvc.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {
            cell.setText(UI.EMPTY_STRING);
         }
      });
      tableLayout.setColumnData(tc, new ColumnPixelData(0, false));
   }

   /**
    * Column: Description
    */
   private void defineColumn_Description(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.CENTER);
      final TableColumn tc = tvc.getColumn();

      tc.setText(Messages.Tour_Marker_Column_Description_ShortCut);
      tc.setToolTipText(Messages.Tour_Marker_Column_Description_Tooltip);
      tvc.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final TourMarker tourMarker = (TourMarker) cell.getElement();
            final String description = tourMarker.getDescription();

            cell.setText(description.length() == 0 ? UI.EMPTY_STRING : UI.SYMBOL_STAR);
         }
      });

      tableLayout.setColumnData(tc, net.tourbook.ui.UI.getColumnPixelWidth(_pc, 4));
   }

   /**
    * column: distance km/mi
    */
   private void defineColumn_Distance(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.TRAIL);
      final TableColumn tc = tvc.getColumn();

      tc.setText(UI.UNIT_LABEL_DISTANCE);
      tc.setToolTipText(Messages.Tour_Marker_Column_km_tooltip);
      tvc.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final TourMarker tourMarker = (TourMarker) cell.getElement();
            final float markerDistance = tourMarker.getDistance();

            if (markerDistance == -1) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(_nf3.format(markerDistance / (1000 * UI.UNIT_VALUE_DISTANCE)));
            }

            if (tourMarker.getType() == ChartLabelMarker.MARKER_TYPE_DEVICE) {
               cell.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
            }
         }
      });
      tableLayout.setColumnData(tc, net.tourbook.ui.UI.getColumnPixelWidth(_pc, 11));
   }

   /**
    * column: marker
    */
   private void defineColumn_IsVisible(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.LEAD);
      final TableColumn tc = tvc.getColumn();

      tc.setText(OtherMessages.TOUR_MARKER_COLUMN_IS_VISIBLE);
      tc.setToolTipText(OtherMessages.TOUR_MARKER_COLUMN_IS_VISIBLE_TOOLTIP);

      tvc.setEditingSupport(new MarkerEditingSupport(_markerViewer));

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourMarker tourMarker = (TourMarker) cell.getElement();
            cell.setText(tourMarker.isMarkerVisible()
                  ? Messages.App_Label_BooleanYes
                  : Messages.App_Label_BooleanNo);
         }
      });
      tableLayout.setColumnData(tc, net.tourbook.ui.UI.getColumnPixelWidth(_pc, 8));
   }

   /**
    * column: marker
    */
   private void defineColumn_Marker(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.LEAD);
      final TableColumn tc = tvc.getColumn();

      tc.setText(Messages.Tour_Marker_Column_remark);

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourMarker tourMarker = (TourMarker) cell.getElement();
            cell.setText(tourMarker.getLabel());
         }
      });
      tableLayout.setColumnData(tc, new ColumnWeightData(1, true));
   }

   /**
    * column: horizontal offset
    */
   private void defineColumn_OffsetX(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.TRAIL);
      final TableColumn tc = tvc.getColumn();

      tc.setText(Messages.Tour_Marker_Column_horizontal_offset);
      tc.setToolTipText(Messages.Tour_Marker_Column_horizontal_offset_tooltip);
      tvc.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final TourMarker tourMarker = (TourMarker) cell.getElement();
            cell.setText(Integer.toString(tourMarker.getLabelXOffset()));
         }
      });
      tableLayout.setColumnData(tc, net.tourbook.ui.UI.getColumnPixelWidth(_pc, 6));
   }

   /**
    * column: vertical offset
    */
   private void defineColumn_OffsetY(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.TRAIL);
      final TableColumn tc = tvc.getColumn();

      tc.setText(Messages.Tour_Marker_Column_vertical_offset);
      tc.setToolTipText(Messages.Tour_Marker_Column_vertical_offset_tooltip);
      tvc.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final TourMarker tourMarker = (TourMarker) cell.getElement();
            cell.setText(Integer.toString(tourMarker.getLabelYOffset()));
         }
      });
      tableLayout.setColumnData(tc, net.tourbook.ui.UI.getColumnPixelWidth(_pc, 6));
   }

   /**
    * Column: Url
    */
   private void defineColumn_Url(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_markerViewer, SWT.CENTER);
      final TableColumn tc = tvc.getColumn();

      tc.setText(Messages.Tour_Marker_Column_Url_ShortCut);
      tc.setToolTipText(Messages.Tour_Marker_Column_Url_Tooltip);
      tvc.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final TourMarker tourMarker = (TourMarker) cell.getElement();
            final String urlAddress = tourMarker.getUrlAddress();
            final String urlText = tourMarker.getUrlText();

            cell.setText(urlAddress.length() > 0 || urlText.length() > 0 ? //
                  UI.SYMBOL_STAR
                  : UI.EMPTY_STRING);
         }
      });
      tableLayout.setColumnData(tc, net.tourbook.ui.UI.getColumnPixelWidth(_pc, 4));
   }

   private void deleteTourMarker(final TourMarker tourMarker) {

      if (MessageDialog.openQuestion(
            getShell(),
            Messages.Dlg_TourMarker_MsgBox_delete_marker_title,
            NLS.bind(Messages.Dlg_TourMarker_MsgBox_delete_marker_message, (tourMarker).getLabel())) == false) {
         return;
      }

      _selectedTourMarker = null;

      // get index for selected marker
      final int lastMarkerIndex = _markerViewer.getTable().getSelectionIndex();

      // update data model
      _dialogTourMarkers.remove(tourMarker);

      // update the viewer
      _markerViewer.remove(tourMarker);

      // update chart
      _tourChart.updateUI_MarkerLayer(true);

      updateUI_FromModel();

      // select next marker
      TourMarker nextMarker = (TourMarker) _markerViewer.getElementAt(lastMarkerIndex);
      if (nextMarker == null) {
         nextMarker = (TourMarker) _markerViewer.getElementAt(lastMarkerIndex - 1);
      }

      if (nextMarker == null) {
         // disable controls when no marker is available
         enableControls();
      } else {
         _markerViewer.setSelection(new StructuredSelection(nextMarker), true);
      }

      _markerViewer.getTable().setFocus();
   }

   private void dispose() {

      _firstColumnControls.clear();

      UI.disposeResource(_imagePaste);
   }

   private void enableControls() {

      final boolean isMarkerSelected = _selectedTourMarker != null;
      final boolean isMarkerEnabled = _markerViewer.getTable().getItemCount() != 0;

      if (isMarkerSelected) {
         _btnUndo.setEnabled(_selectedTourMarker.isEqual(_backupMarker, true) == false);
      } else {
         _btnUndo.setEnabled(false);
      }

      _chkVisibility.setEnabled(isMarkerSelected);

      _btnDelete.setEnabled(isMarkerSelected);
      _btnShowAll.setEnabled(isMarkerEnabled);
      _btnHideAll.setEnabled(isMarkerEnabled);
      _btnPasteText.setEnabled(isMarkerEnabled);
      _btnPasteUrl.setEnabled(isMarkerEnabled);

      _comboLabelPosition.setEnabled(isMarkerEnabled);
      _comboMarkerName.setEnabled(isMarkerEnabled);

      // this do not work on win
      _groupText.setEnabled(isMarkerEnabled);
      _groupUrl.setEnabled(isMarkerEnabled);

      _lblDescription.setEnabled(isMarkerEnabled);
      _lblLabel.setEnabled(isMarkerEnabled);
      _lblLabelOffsetX.setEnabled(isMarkerEnabled);
      _lblLabelOffsetY.setEnabled(isMarkerEnabled);
      _lblLabelPosition.setEnabled(isMarkerEnabled);
      _lblLinkText.setEnabled(isMarkerEnabled);
      _lblLinkUrl.setEnabled(isMarkerEnabled);

      _spinLabelOffsetX.setEnabled(isMarkerEnabled);
      _spinLabelOffsetY.setEnabled(isMarkerEnabled);

      _txtDescription.setEnabled(isMarkerEnabled);
      _txtUrlAddress.setEnabled(isMarkerEnabled);
      _txtUrlText.setEnabled(isMarkerEnabled);
   }

   private void fillUI() {

      /*
       * Fill position combos
       */
      for (final String position : TourMarker.LABEL_POSITIONS) {
         _comboLabelPosition.add(position);
      }

      /*
       * Marker names combo
       */
      final ConcurrentSkipListSet<String> dbTitles = TourDatabase.getCachedFields_AllTourMarkerNames();
      for (final String title : dbTitles) {
         _comboMarkerName.add(title);
      }

      new AutocompleteComboInput(_comboMarkerName);
   }

   /**
    * Fires a selection to all opened views.
    *
    * @param selection
    */
   private void fireGlobalSelection(final ISelection selection) {

      PostSelectionProvider.fireSelection(selection);
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      return _state;
//      return null;
   }

   TourChart getTourChart() {
      return _tourChart;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _contentWidthHint = _pc.convertWidthInCharsToPixels(20);

      _imagePaste = CommonActivator.getThemedImageDescriptor(CommonImages.App_Copy).createImage();

      restoreState_Viewer();
   }

   @Override
   protected void okPressed() {

      if (_selectedTourMarker != null &&
            _selectedTourMarker.isValidForSave() == false) {
         return;
      }

      _isOkPressed = true;

      super.okPressed();
   }

   /**
    * save marker modifications and update chart and viewer
    */
   private void onChangeMarkerUI() {

      updateModel_FromUI(_selectedTourMarker);

      _tourChart.updateUI_MarkerLayer(true);

      _markerViewer.update(_selectedTourMarker, null);

      enableControls();
   }

   private void onSelectMarker(final TourMarker newSelectedMarker) {

      if (newSelectedMarker == null) {
         return;
      }

      // save values for previous marker
      if (_selectedTourMarker != null && newSelectedMarker != _selectedTourMarker) {
         updateModel_FromUI(_selectedTourMarker);
         restoreState_VisibleType();
      }

      // set new selected marker
      _selectedTourMarker = newSelectedMarker;

      // make a backup of the marker to undo modifications
      _selectedTourMarker.setMarkerBackup(_backupMarker);

      updateUI_FromModel();
      onChangeMarkerUI();

      if (_isSetXSlider) {

         // set slider position
         final SelectionChartXSliderPosition sliderSelection = new SelectionChartXSliderPosition(
               _tourChart,
               newSelectedMarker.getSerieIndex(),
               SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION);

         // set x-slider in the tour chart but do not fire a default event
         _tourChart.setXSliderPosition(sliderSelection, false);

         /*
          * Fire a tour marker selection
          */
         final ArrayList<TourMarker> selectedTourMarker = new ArrayList<>();
         selectedTourMarker.add(newSelectedMarker);

         final SelectionTourMarker tourMarkerSelection = new SelectionTourMarker(_tourData, selectedTourMarker);

         fireGlobalSelection(tourMarkerSelection);
      }
   }

   private void restoreState() {

      // restore width for the marker list
      final int leftPartWidth = Util.getStateInt(_state, STATE_OUTER_SASH_WIDTH, _pc.convertWidthInCharsToPixels(80));
      _sashOuterForm.setViewerWidth(leftPartWidth);

      final int bottomPartHeight = Util.getStateInt(_state, STATE_INNER_SASH_HEIGHT, _pc.convertWidthInCharsToPixels(10));
      _sashInnerForm.setFixedHeight(bottomPartHeight);
   }

   private void restoreState_Viewer() {

//		_imageColumnWidth = Util.getStateInt(_state, STATE_IMAGE_COLUMN_WIDTH, IMAGE_DEFAULT_WIDTH);
   }

   /**
    * restore type from the backup for the currently selected tour marker
    */
   private void restoreState_VisibleType() {

      if (_selectedTourMarker == null) {
         return;
      }

      _selectedTourMarker.setVisibleType(ChartLabelMarker.VISIBLE_TYPE_DEFAULT);
   }

   private void saveState() {

      final int sashInnerFixedHeight = _sashInnerFixedPart.getSize().y;
      final int sashInnerHeight = _sashInner.getSize().y;

      _state.put(DIALOG_SETTINGS_POSITION, _comboLabelPosition.getSelectionIndex());
      _state.put(STATE_OUTER_SASH_WIDTH, _sashOuterFixedPart.getSize().x);
      _state.put(STATE_INNER_SASH_HEIGHT, sashInnerFixedHeight - sashInnerHeight);
   }

   @Override
   public void selectionChanged(final SelectionTourMarker tourMarkerSelection) {

      final ArrayList<TourMarker> selectedTourMarker = tourMarkerSelection.getSelectedTourMarker();

      // prevent that the x-slider is positioned in the tour chart
      _isSetXSlider = false;
      {
         _markerViewer.setSelection(new StructuredSelection(selectedTourMarker), true);
      }
      _isSetXSlider = true;

      _comboMarkerName.setFocus();

      fireGlobalSelection(tourMarkerSelection);
   }

   private void toggleMarkerVisibility() {

      final ISelection selection = _markerViewer.getSelection();
      if (selection instanceof StructuredSelection) {

         final Object firstElement = ((StructuredSelection) selection).getFirstElement();
         if (firstElement instanceof TourMarker) {

            final TourMarker tourMarker = (TourMarker) firstElement;
            final boolean isMarkerVisible = !tourMarker.isMarkerVisible();

            updateUI_TourMarker(tourMarker, isMarkerVisible);
            enableControls();
         }
      }
   }

   @Override
   public void tourMarkerIsModified(final TourMarker tourMarker, final boolean isDeleted) {

      if (isDeleted) {

         deleteTourMarker(tourMarker);

      } else {

         // a tour marker is modified in the tour chart which is located in this dialog, update UI

         // controls
         updateUI_FromModel();

         // viewer
         _markerViewer.update(tourMarker, null);

         // chart
         _tourChart.updateUI_MarkerLayer(true);

         enableControls();
      }
   }

   private void updateModel_FromUI(final TourMarker tourMarker) {

      if (tourMarker == null) {
         return;
      }

      tourMarker.setMarkerVisible(_chkVisibility.getSelection());

      tourMarker.setLabel(_comboMarkerName.getText());
      tourMarker.setLabelPosition(_comboLabelPosition.getSelectionIndex());

      tourMarker.setLabelXOffset(_spinLabelOffsetX.getSelection());
      tourMarker.setLabelYOffset(_spinLabelOffsetY.getSelection());

      tourMarker.setDescription(_txtDescription.getText());
      tourMarker.setUrlAddress(_txtUrlAddress.getText());
      tourMarker.setUrlText(_txtUrlText.getText());
   }

   /**
    * update marker ui from the selected marker
    */
   private void updateUI_FromModel() {

      _isUpdateUI = true;
      {
         final boolean isTourMarker = _selectedTourMarker != null;

         if (isTourMarker) {

            // make the marker more visible by setting another type
            _selectedTourMarker.setVisibleType(ChartLabelMarker.VISIBLE_TYPE_TYPE_EDIT);
         }

         _chkVisibility.setSelection(isTourMarker ? _selectedTourMarker.isMarkerVisible() : false);

         _comboMarkerName.setText(isTourMarker ? _selectedTourMarker.getLabel() : UI.EMPTY_STRING);
         _comboLabelPosition.select(isTourMarker ? _selectedTourMarker.getLabelPosition() : 0);

         _spinLabelOffsetX.setSelection(isTourMarker ? _selectedTourMarker.getLabelXOffset() : 0);
         _spinLabelOffsetY.setSelection(isTourMarker ? _selectedTourMarker.getLabelYOffset() : 0);

         _txtDescription.setText(isTourMarker ? _selectedTourMarker.getDescription() : UI.EMPTY_STRING);
         _txtUrlAddress.setText(isTourMarker ? _selectedTourMarker.getUrlAddress() : UI.EMPTY_STRING);
         _txtUrlText.setText(isTourMarker ? _selectedTourMarker.getUrlText() : UI.EMPTY_STRING);
      }
      _isUpdateUI = false;
   }

   private void updateUI_TourMarker(final TourMarker tourMarker, final Boolean isVisible) {

      tourMarker.setMarkerVisible(isVisible);

      // update UI
      _markerViewer.update(tourMarker, null);
      _tourChart.updateUI_MarkerLayer(true);
   }

}
