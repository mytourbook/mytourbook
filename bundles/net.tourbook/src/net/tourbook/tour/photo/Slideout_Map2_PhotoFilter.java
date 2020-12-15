/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.photo;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.map2.view.MapFilterData;
import net.tourbook.photo.IPhotoEventListener;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.RatingStars;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IViewPart;

/**
 * Photo properties dialog.
 */
public class Slideout_Map2_PhotoFilter extends ToolbarSlideout implements IPhotoEventListener {

   private static final int                         SHELL_MARGIN                            = 5;

   private static final String                      STATE_PHOTO_FILTER_RATING_STARS         = "STATE_PHOTO_FILTER_RATING_STARS";         //$NON-NLS-1$
   private static final String                      STATE_PHOTO_FILTER_RATING_STAR_OPERATOR = "STATE_PHOTO_FILTER_RATING_STAR_OPERATOR"; //$NON-NLS-1$

   public static final int                          OPERATOR_IS_LESS_OR_EQUAL               = 0;
   public static final int                          OPERATOR_IS_EQUAL                       = 1;
   public static final int                          OPERATOR_IS_MORE_OR_EQUAL               = 2;

   private static final String[]                    _ratingStarOperatorsText                = {

         Messages.Photo_Filter_Operator_IsLess,
         Messages.Photo_Filter_Operator_IsEqual,
         Messages.Photo_Filter_Operator_IsMore,

   };

   /**
    * <b>THEY MUST BE IN SYNC WITH </b> {@link #_filterRatingStarOperatorsText}
    */
   private static final int[]                       _ratingStarOperatorsValues              = {

         OPERATOR_IS_LESS_OR_EQUAL,
         OPERATOR_IS_EQUAL,
         OPERATOR_IS_MORE_OR_EQUAL,

   };

   private IDialogSettings                          _state;

   private int                                      _filterRatingStars                      = RatingStars.MAX_RATING_STARS;

   private final ListenerList<IPhotoFilterListener> _propertiesListeners                    = new ListenerList<>(ListenerList.IDENTITY);

   /**
    * Filter operator
    */
   private int                                      _filterRatingStarOperatorIndex;

   private MapFilterData                            _oldMapFilterData;

   private PixelConverter                           _pc;

   /*
    * UI controls
    */
   private Composite   _shellContainer;
   private Composite   _containerFilterHeader;

   private Label       _lblAllPhotos;
   private Label       _lblFilteredPhotos;
   private Combo       _comboRatingStarOperators;

   private RatingStars _ratingStars;

   public Slideout_Map2_PhotoFilter(final Control ownerControl, final ToolBar toolBar, final IDialogSettings state) {

      super(ownerControl, toolBar);

      _state = state;

      PhotoManager.addPhotoEventListener(this);
   }

   public void addPropertiesListener(final IPhotoFilterListener listener) {
      _propertiesListeners.add(listener);
   }

   @Override
   protected boolean canShowToolTip() {
      return true;
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      final Composite container = createUI(parent);

      updateUI();

      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
      UI.setChildColors(parent,
            colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND),
            colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND));

      if (_oldMapFilterData != null) {

         /*
          * _oldMapFilterData can be set before the UI is created
          */

         updateFilterUI(_oldMapFilterData);
      }

      enableActions();

      PhotoManager.addPhotoEventListener(this);

      return container;
   }

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults()
            .margins(SHELL_MARGIN, SHELL_MARGIN)
//            .numColumns(3)
            .applyTo(_shellContainer);
      {
         createUI_10_Title(_shellContainer);
         createUI_20_Filter(_shellContainer);
      }

      return _shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      final GridDataFactory gridData_NumPhotos = GridDataFactory.fillDefaults()
            .align(SWT.CENTER, SWT.END)
//            .hint(_pc.convertWidthInCharsToPixels(3), SWT.DEFAULT)

      ;

      final Composite container = new Composite(parent, SWT.NO_FOCUS);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * Label: photo filter
             */
            final Label label = new Label(container, SWT.NO_FOCUS);
            GridDataFactory.fillDefaults().applyTo(label);

            label.setText(Messages.Photo_Filter_Label_PhotoFilter);
            label.setToolTipText(Messages.Photo_Filter_Label_RatingStars_Tooltip);
            MTFont.setBannerFont(label);
         }
         {
            _containerFilterHeader = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, true)
//                  .indent(5, 0)
                  .hint(_pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
                  .applyTo(_containerFilterHeader);
            GridLayoutFactory.fillDefaults().applyTo(_containerFilterHeader);
//            _containerFilterHeader.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
            {
               final Composite innerContainer = new Composite(_containerFilterHeader, SWT.NONE);
               GridDataFactory.fillDefaults()
                     .grab(true, true)
                     .align(SWT.CENTER, SWT.END)
                     .applyTo(innerContainer);
               GridLayoutFactory.fillDefaults().numColumns(3).applyTo(innerContainer);
//               innerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
               {
                  /*
                   * value: number of all photos
                   */
                  _lblAllPhotos = new Label(innerContainer, SWT.NO_FOCUS);
                  _lblAllPhotos.setText(UI.EMPTY_STRING);
                  _lblAllPhotos.setToolTipText(Messages.Photo_Filter_Label_NumberOfAllPhotos_Tooltip);
                  gridData_NumPhotos.applyTo(_lblAllPhotos);

                  /*
                   * label: number of filtered photos
                   */
                  final Label label = new Label(innerContainer, SWT.NO_FOCUS);
                  label.setText(UI.DASH);
                  GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.END).applyTo(label);

                  /*
                   * value: number of filtered photos
                   */
                  _lblFilteredPhotos = new Label(innerContainer, SWT.NO_FOCUS);
                  _lblFilteredPhotos.setText(UI.EMPTY_STRING);
                  _lblFilteredPhotos.setToolTipText(Messages.Photo_Filter_Label_NumberOfFilteredPhotos_Tooltip);
                  gridData_NumPhotos.applyTo(_lblFilteredPhotos);
               }
            }
         }
      }
   }

   private void createUI_20_Filter(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NO_FOCUS);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * Combo: > = <
             */
            _comboRatingStarOperators = new Combo(container, SWT.READ_ONLY);
            _comboRatingStarOperators.setVisibleItemCount(10);
            _comboRatingStarOperators.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelectRatingStarOperands();
               }
            });
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_comboRatingStarOperators);
         }
         {
            /*
             * Rating stars
             */
            _ratingStars = new RatingStars(container);
            _ratingStars.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelectRatingStars();
               }
            });
         }
      }
   }

   private void enableActions() {

   }

   private void fireFilterEvent() {

      final PhotoFilterEvent filterEvent = new PhotoFilterEvent();

      filterEvent.filterRatingStars = _filterRatingStars;
      filterEvent.fiterRatingStarOperator = _ratingStarOperatorsValues[_filterRatingStarOperatorIndex];

      final Object[] listeners = _propertiesListeners.getListeners();
      for (final Object listener : listeners) {
         ((IPhotoFilterListener) listener).photoFilterEvent(filterEvent);
      }
   }

   private void initUI(final Composite parent) {
      // TODO Auto-generated method stub

      _pc = new PixelConverter(parent);
   }

   @Override
   protected void onDispose() {

      PhotoManager.removePhotoEventListener(this);
   }

   private void onSelectRatingStarOperands() {

      _filterRatingStarOperatorIndex = _comboRatingStarOperators.getSelectionIndex();

      fireFilterEvent();
   }

   private void onSelectRatingStars() {

      final int selectedStars = _ratingStars.getSelection();

      _filterRatingStars = selectedStars;

      enableActions();

      fireFilterEvent();
   }

   @Override
   public void photoEvent(final IViewPart viewPart, final PhotoEventId photoEventId, final Object data) {

      if (photoEventId == PhotoEventId.PHOTO_FILTER) {

         if (data instanceof MapFilterData) {

            updateFilterUI((MapFilterData) data);
         }
      }
   }

   public void restoreState() {

      _filterRatingStars = Util.getStateInt(_state, STATE_PHOTO_FILTER_RATING_STARS, RatingStars.MAX_RATING_STARS);
      _filterRatingStarOperatorIndex = Util.getStateInt(_state, STATE_PHOTO_FILTER_RATING_STAR_OPERATOR, OPERATOR_IS_EQUAL);

      // set photo filter into the map
      fireFilterEvent();
   }

   public void saveState() {

      _state.put(STATE_PHOTO_FILTER_RATING_STARS, _filterRatingStars);
      _state.put(STATE_PHOTO_FILTER_RATING_STAR_OPERATOR, _filterRatingStarOperatorIndex);
   }

   /**
    * This is called when the filter is run and filter statistics are available.
    *
    * @param data
    */
   protected void updateFilterActionUI(final MapFilterData data) {
      // do nothing
   }

   private void updateFilterUI(final MapFilterData data) {

      if (_lblAllPhotos == null || _lblAllPhotos.isDisposed()) {

         // UI is not initialized

         _oldMapFilterData = data;

         return;
      }

      _lblAllPhotos.setText(Integer.toString(data.allPhotos));
      _lblFilteredPhotos.setText(Integer.toString(data.filteredPhotos));

      _containerFilterHeader.layout();

      // update action button
      updateFilterActionUI(data);
   }

   private void updateUI() {

      // select rating star
      _ratingStars.setSelection(_filterRatingStars);

      for (final String operator : _ratingStarOperatorsText) {
         _comboRatingStarOperators.add(operator);
      }

      // ensure array bounds
      if (_filterRatingStarOperatorIndex >= _ratingStarOperatorsText.length) {
         _filterRatingStarOperatorIndex = 0;
      }

      // select operator
      _comboRatingStarOperators.select(_filterRatingStarOperatorIndex);
   }

}
