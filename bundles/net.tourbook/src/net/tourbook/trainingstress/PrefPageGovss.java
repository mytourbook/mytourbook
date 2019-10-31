/*******************************************************************************
 * Copyright (C) 2019 Frédéric Bard
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
package net.tourbook.trainingstress;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.TableLayoutComposite;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tourType.TourTypeImage;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class PrefPageGovss extends PrefPageTrainingStressModel {

   private static TableViewer          _tourTypesViewer;
   private static Label                _labelThresholdPower_Value;
   private static Label                _labelThresholdVelocity_Value;

   private static Spinner              _spinnerThresholdPower_Distance;

   private static Spinner              _spinnerThresholdPower_AverageSlope;

   private static ActionOpenPrefDialog _actionOpenTourTypePrefs;
   private SelectionListener           _defaultSelectionListener;
   private MouseWheelListener          _defaultMouseWheelListener;

   private Group                       _govssGroup;
   private TourPerson                  _tourPerson;

   private Spinner                     _textThresholdPower_Duration_Hours;
   private Spinner                     _textThresholdPower_Duration_Minutes;
   private Spinner                     _textThresholdPower_Duration_Seconds;
   private int                         _hintDefaultSpinnerWidth;

   private ActionTourType_Add          _action_TourType_Add;
   private ActionTourType_Remove       _action_TourType_Remove;
   private Font                        _boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

   private PixelConverter              _pc;

   private class Action_TourType extends Action {

      private TourType _tourType;

      /**
       * @param tourType
       */
      public Action_TourType(final TourType tourType, final boolean isChecked) {

         super(tourType.getName(), AS_CHECK_BOX);

         if (isChecked == false) {

            final Image tourTypeImage = TourTypeImage.getTourTypeImage(tourType.getTypeId());
            setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));
         }

         setChecked(isChecked);
         setEnabled(isChecked == false);

         _tourType = tourType;

      }

      @Override
      public void run() {
         _tourTypesViewer.add(_tourType);
         _personModifiedListener.onPersonModifiedListener();
         enableControls();
      }
   }

   private class ActionTourType_Add extends Action implements IMenuCreator {

      private Menu _menu;

      public ActionTourType_Add() {

         super(null, AS_PUSH_BUTTON);

         setToolTipText(Messages.Pref_TrainingStress_Govss_AddTourTypes);
         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Add));

         setMenuCreator(this);
      }

      @Override
      public void dispose() {
         if (_menu != null) {
            _menu.dispose();
            _menu = null;
         }
      }

      @Override
      public Menu getMenu(final Control parent) {
         if (_menu != null) {
            _menu.dispose();
         }

         //Getting the current selected tour types
         final TableItem[] selectedTours = _tourTypesViewer.getTable().getItems();

         // add the tour types that have not been added already to the menu
         final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
         final MenuManager menuManager = new MenuManager();
         for (final TourType tourType : tourTypes) {

            boolean isChecked = false;
            for (final TableItem currentItem : selectedTours) {
               if (currentItem.getText().equals(tourType.getName())) {

                  isChecked = true;
                  break;
               }
            }

            final Action_TourType action = new Action_TourType(tourType, isChecked);

            menuManager.add(action);
         }

         menuManager.add(new Separator());
         menuManager.add(_actionOpenTourTypePrefs);

         _menu = menuManager.createContextMenu(parent);
         return _menu;
      }

      @Override
      public Menu getMenu(final Menu arg0) {
         // TODO Auto-generated method stub
         return null;
      }

      @Override
      public void runWithEvent(final Event event) {
         if (event.widget instanceof ToolItem) {
            final ToolItem toolItem = (ToolItem) event.widget;
            final Control control = toolItem.getParent();
            final Menu menu = getMenu(control);

            final Rectangle bounds = toolItem.getBounds();
            final Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
            menu.setLocation(control.toDisplay(topLeft));
            menu.setVisible(true);
         }
      }
   }

   private class ActionTourType_Remove extends Action {

      public ActionTourType_Remove() {

         super(null, AS_PUSH_BUTTON);

         setToolTipText(Messages.Pref_TrainingStress_Govss_RemoveTourType);
         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Trash));
      }

      @Override
      public void run() {

         final int selectedIndex = _tourTypesViewer.getTable().getSelectionIndex();

         final TourType _selectedTourType = (TourType) _tourTypesViewer.getElementAt(selectedIndex);
         if (_selectedTourType == null) {
            return;
         }

         _tourTypesViewer.remove(_selectedTourType);

         final int listSize = _tourTypesViewer.getTable().getItemCount();
         final int newSelectedIndex = selectedIndex >= listSize ? listSize - 1 : selectedIndex;
         _tourTypesViewer.getTable().setSelection(newSelectedIndex);

         _personModifiedListener.onPersonModifiedListener();

         enableControls();
      }

   }

   /**
    * Compute the velocity of the time trial in m/s
    *
    * @param testDistance
    * @param testDuration
    * @return
    */
   private float computeThresholdVelocity(final float testDistance, final int testDuration) {

      final float thresholdVelocity = (testDistance / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE)
            / testDuration;

      return thresholdVelocity;
   }

   private void createActions() {

      _action_TourType_Add = new ActionTourType_Add();
      _action_TourType_Remove = new ActionTourType_Remove();

      _actionOpenTourTypePrefs = new ActionOpenPrefDialog(
            Messages.action_tourType_modify_tourTypes,
            ITourbookPreferences.PREF_PAGE_TOUR_TYPE);
   }

   /**
    * UI for the threshold power group
    */
   private void createUI_110_ThresholdPower(final Composite parent) {

      final Group container = new Group(parent, SWT.WRAP);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      container.setText(Messages.Pref_TrainingStress_Group_ThresholdPower);
      GridLayoutFactory.swtDefaults().numColumns(8).applyTo(container);
      {
         {
            // label : Time
            Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_ThresholdPower_Label_Duration);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

            _textThresholdPower_Duration_Hours = new Spinner(container, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
            _textThresholdPower_Duration_Hours.setToolTipText("TODO");//Messages.Pref_Weather_Label_ApiKey_Tooltip);
            _textThresholdPower_Duration_Hours.setMaximum(23);
            _textThresholdPower_Duration_Hours.addSelectionListener(_defaultSelectionListener);
            _textThresholdPower_Duration_Hours.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults().hint(_hintDefaultSpinnerWidth, SWT.DEFAULT).applyTo(_textThresholdPower_Duration_Hours);

            _textThresholdPower_Duration_Minutes = new Spinner(container, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
            _textThresholdPower_Duration_Minutes.setToolTipText("TODO");//Messages.Pref_Weather_Label_ApiKey_Tooltip);
            _textThresholdPower_Duration_Minutes.setMaximum(59);
            _textThresholdPower_Duration_Minutes.addSelectionListener(_defaultSelectionListener);
            _textThresholdPower_Duration_Minutes.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults().hint(_hintDefaultSpinnerWidth, SWT.DEFAULT).applyTo(_textThresholdPower_Duration_Minutes);

            _textThresholdPower_Duration_Seconds = new Spinner(container, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
            _textThresholdPower_Duration_Seconds.setToolTipText("TODO");//Messages.Pref_Weather_Label_ApiKey_Tooltip);
            _textThresholdPower_Duration_Seconds.setMaximum(59);
            _textThresholdPower_Duration_Seconds.addSelectionListener(_defaultSelectionListener);
            _textThresholdPower_Duration_Seconds.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults().hint(_hintDefaultSpinnerWidth, SWT.DEFAULT).span(5, 1).align(SWT.BEGINNING, SWT.CENTER).applyTo(
                  _textThresholdPower_Duration_Seconds);

            // ROW #2

            // label : Distance
            label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_ThresholdPower_Label_Distance);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

            // Text entry for the distance
            _spinnerThresholdPower_Distance = new Spinner(container, SWT.BORDER);
            _spinnerThresholdPower_Distance.setToolTipText("");//Messages.Pref_Weather_Label_ApiKey_Tooltip);
            _spinnerThresholdPower_Distance.setDigits(1);
            _spinnerThresholdPower_Distance.setMaximum(500);
            _spinnerThresholdPower_Distance.addSelectionListener(_defaultSelectionListener);
            _spinnerThresholdPower_Distance.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.LEFT, SWT.CENTER)
                  .applyTo(_spinnerThresholdPower_Distance);

            // label: km or mi
            label = new Label(container, SWT.NONE);
            label.setText(UI.UNIT_LABEL_DISTANCE);
            GridDataFactory.fillDefaults()
                  .align(SWT.LEFT, SWT.CENTER)
                  .applyTo(label);

            // label : Time
            label = new Label(container, SWT.NONE);
            label.setText("Threshold velocity");//Messages.Pref_ThresholdPower_Label_Duration);
            label.setFont(_boldFont);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

            // text
            _labelThresholdVelocity_Value = new Label(container, SWT.NONE);
            _labelThresholdVelocity_Value.setFont(_boldFont);
            final GridData gd = new GridData();
            gd.widthHint = _hintDefaultSpinnerWidth;
            _labelThresholdVelocity_Value.setLayoutData(gd);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_labelThresholdVelocity_Value);

            // label:
            label = new Label(container, SWT.NONE);
            label.setText(UI.UNIT_LABEL_PACE);
            label.setFont(_boldFont);
            GridDataFactory.fillDefaults().grab(true, false).align(SWT.BEGINNING, SWT.CENTER).span(3, 1).applyTo(label);

            // ROW #3

            // label : average slope
            label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_ThresholdPower_Label_AverageSlope);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

            // text
            _spinnerThresholdPower_AverageSlope = new Spinner(container, SWT.BORDER);
            _spinnerThresholdPower_AverageSlope.setToolTipText("");//Messages.Pref_Weather_Label_ApiKey_Tooltip);
            _spinnerThresholdPower_AverageSlope.setDigits(2);
            _spinnerThresholdPower_AverageSlope.setMinimum(-50);
            _spinnerThresholdPower_AverageSlope.setMaximum(50);
            _spinnerThresholdPower_AverageSlope.addSelectionListener(_defaultSelectionListener);
            _spinnerThresholdPower_AverageSlope.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.LEFT, SWT.CENTER)
                  .applyTo(_spinnerThresholdPower_AverageSlope);

            // label : Time
            label = new Label(container, SWT.NONE);
            label.setText("Threshold Power");//Messages.Pref_ThresholdPower_Label_Duration);
            label.setFont(_boldFont);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(label);

            // text
            _labelThresholdPower_Value = new Label(container, SWT.NONE);
            _labelThresholdPower_Value.setFont(_boldFont);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_labelThresholdPower_Value);

            // label:
            label = new Label(container, SWT.NONE);
            label.setText(UI.UNIT_POWER);
            label.setFont(_boldFont);
            GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(label);
         }
      }

   }

   /**
    * UI for the list of tour types
    */
   private void createUI_120_TourTypesList(final Composite parent) {

      final Group container = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      container.setText("Tour types");//Messages.Pref_Appearance_Group_Tagging);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
      {
         {
            // Toolbar
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);

            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_action_TourType_Add);
            tbm.add(_action_TourType_Remove);

            tbm.update(true);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(toolbar);

            // Table
            final TableLayoutComposite layouter = new TableLayoutComposite(container, SWT.NONE);
            GridDataFactory.fillDefaults().hint(100, 100).applyTo(layouter);

            final Table table = new Table(layouter, (SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION));
            table.setHeaderVisible(false);
            table.setLinesVisible(false);
            table.addFocusListener(new FocusListener() {

               @Override
               public void focusGained(final FocusEvent arg0) {
                  _action_TourType_Remove.setEnabled(true);
               }

               @Override
               public void focusLost(final FocusEvent arg0) {
                  _action_TourType_Remove.setEnabled(false);

               }
            });
            _tourTypesViewer = new TableViewer(table);

            TableViewerColumn tvc;

            // column: image + name
            tvc = new TableViewerColumn(_tourTypesViewer, SWT.NONE);
            tvc.setLabelProvider(new CellLabelProvider() {
               @Override
               public void update(final ViewerCell cell) {

                  final TourType tourType = ((TourType) cell.getElement());

                  final String filterName = tourType.getName();
                  final Image filterImage = TourTypeImage.getTourTypeImage(tourType.getTypeId());

                  cell.setText(filterName);
                  cell.setImage(filterImage);
               }
            });
            layouter.addColumnData(new ColumnWeightData(1));

            _tourTypesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
               @Override
               public void selectionChanged(final SelectionChangedEvent event) {
                  enableControls();
               }
            });
         }
      }

   }

   @Override
   public void dispose() {
      _govssGroup = null;

   }

   private void enableControls() {

      final StructuredSelection currentTourTypeViewerSelection = (StructuredSelection) _tourTypesViewer.getSelection();
      final boolean isTourTypeSelected = currentTourTypeViewerSelection.isEmpty() ? false : true;
      _action_TourType_Remove.setEnabled(isTourTypeSelected);
   }

   /**
    * UI for the GOVSS preferences
    */
   @Override
   public Group getGroupUI(final Composite parent, final TourPerson tourPerson) {

      _tourPerson = tourPerson;

      initUI(parent);

      createActions();

      if (_govssGroup == null) {
         _govssGroup = new Group(parent, SWT.NONE);
         GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_govssGroup);
         {
            createUI_110_ThresholdPower(_govssGroup);
            createUI_120_TourTypesList(_govssGroup);
         }

         restoreState();
      }

      enableControls();

      return _govssGroup;

   }

   @Override
   public String getId() {

      return "GOVSS";
   }

   /**
    * Prints a velocity in min/mile or min/km
    *
    * @param thresholdVelocity
    * @return
    */
   private String getThresholdVelocityString(float thresholdVelocity) {

      //Converting speed from m/s to min/km or min/mile
      // m/s -> s/km
      thresholdVelocity = 1000 / thresholdVelocity;
      // s/km -> min/km
      thresholdVelocity /= 60f;
      // min/km -> min/km or min/mile
      thresholdVelocity /= net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

      int thresholdVelocity_Minutes = (int) Math.floor(thresholdVelocity);
      int seconds = Math.round(60 * (thresholdVelocity - thresholdVelocity_Minutes));
      if (seconds == 60) {
         thresholdVelocity_Minutes += 1;
         seconds = 0;
      }
      final StringBuilder criticalPace = new StringBuilder();
      criticalPace.append(String.valueOf(thresholdVelocity_Minutes));
      if (seconds > 0) {
         criticalPace.append("'" + String.valueOf(seconds));
      }
      return criticalPace.toString();
   }

   private int getTimeTrialDuration() {
      return _textThresholdPower_Duration_Hours.getSelection() * 3600 + _textThresholdPower_Duration_Minutes
            .getSelection() * 60 +
            _textThresholdPower_Duration_Seconds.getSelection();
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _hintDefaultSpinnerWidth = UI.IS_LINUX ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(UI.IS_OSX ? 10 : 5);

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onComputeThresholdPower();
         }
      };

      _defaultMouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            UI.adjustSpinnerValueOnMouseScroll(event);
            onComputeThresholdPower();
         }
      };

   }

   private void onComputeThresholdPower() {

      //Total duration in seconds
      final int thresholdPowerDuration = getTimeTrialDuration();
      float thresholdPowerDistance = (_spinnerThresholdPower_Distance.getSelection() / 10f) * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
      if (_tourPerson == null || thresholdPowerDuration <= 0 || thresholdPowerDistance <= 0) {
         _labelThresholdPower_Value.setText("0");
         _labelThresholdVelocity_Value.setText(UI.EMPTY_STRING);
         return;
      }

      _personModifiedListener.onPersonModifiedListener();

      // Distance in meters
      thresholdPowerDistance *= 1000f;
      // Speed in m/s
      final float thresholdVelocity = computeThresholdVelocity(thresholdPowerDistance, thresholdPowerDuration);
      final float averageSlope = _spinnerThresholdPower_AverageSlope.getSelection() / 100f;
      final Running_Govss _running_Govss = new Running_Govss(_tourPerson);
      final double thresholdPower = _running_Govss.ComputePower(thresholdPowerDistance,
            averageSlope,
            0f,
            thresholdVelocity);

      _labelThresholdPower_Value.setText(String.valueOf(Math.round(thresholdPower)));
      _labelThresholdPower_Value.requestLayout();

      final String criticalPace = getThresholdVelocityString(thresholdVelocity);

      _labelThresholdVelocity_Value.setText(criticalPace.toString());
      _labelThresholdVelocity_Value.requestLayout();
   }

   @Override
   public void restoreState() {

      if (_tourPerson == null || _govssGroup == null) {
         return;
      }

      final int testDuration = _tourPerson.getGovssTimeTrialDuration();
      _textThresholdPower_Duration_Seconds.setSelection(testDuration % 60);
      _textThresholdPower_Duration_Minutes.setSelection((testDuration / 60) % 60);
      _textThresholdPower_Duration_Hours.setSelection(testDuration / 3600);

      _spinnerThresholdPower_Distance.setSelection(_tourPerson.getGovssTimeTrialDistance() / 100);
      _spinnerThresholdPower_AverageSlope.setSelection((int) _tourPerson.getGovssTimeTrialAverageSlope());

      final float thresholdVelocity = computeThresholdVelocity(_tourPerson.getGovssTimeTrialDistance(), testDuration);
      _labelThresholdVelocity_Value.setText(String.valueOf(getThresholdVelocityString(thresholdVelocity)));
      _labelThresholdPower_Value.setText(String.valueOf(_tourPerson.getGovssThresholdPower()));

      final String govssAssociatedTourTypes = _tourPerson.getGovssAssociatedTourTypes();

      if (govssAssociatedTourTypes == null || govssAssociatedTourTypes.equals(UI.EMPTY_STRING)) {
         return;
      }

      _tourTypesViewer.getTable().removeAll();

      final String[] associatedTourTypes = govssAssociatedTourTypes.split(";");

      // add the tour types that have not been added already to the menu
      final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
      for (final TourType tourType : tourTypes) {

         for (final String currentItem : associatedTourTypes) {
            if (currentItem.equals(String.valueOf(tourType.getTypeId()))) {

               _tourTypesViewer.add(tourType);
               break;
            }
         }
      }

   }

   @Override
   public void saveState() {

      if (_tourPerson == null) {
         return;
      }

      _tourPerson.setGovssThresholdPower(Integer.valueOf(_labelThresholdPower_Value.getText()));
      _tourPerson.setGovssTimeTrialDuration(getTimeTrialDuration());
      _tourPerson.setGovssTimeTrialDistance(_spinnerThresholdPower_Distance.getSelection() * 100);
      _tourPerson.setGovssTimeTrialAverageSlope(_spinnerThresholdPower_AverageSlope.getSelection());

      final StringBuilder associatedTourTypes = new StringBuilder();

      for (int index = 0; index < _tourTypesViewer.getTable().getItemCount(); ++index) {

         final TourType tourType = (TourType) _tourTypesViewer.getElementAt(index);
         associatedTourTypes.append(tourType.getTypeId() + ";");
      }

      _tourPerson.setGovssAssociatedTourTypes(associatedTourTypes.toString());
   }
}
