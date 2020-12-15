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
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.map2.view.Map2View;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IViewPart;

/**
 * Photo properties dialog.
 */
public class Slideout_Map2_PhotoFilter extends AdvancedSlideout implements IPhotoEventListener {

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

   private final ListenerList<IPhotoFilterListener> _photoFilterListeners                   = new ListenerList<>(ListenerList.IDENTITY);

   /**
    * Filter operator
    */
   private int                                      _filterRatingStarOperatorIndex;
   private int                                      _filterRatingStars                      = RatingStars.MAX_RATING_STARS;

   private MapFilterData                            _oldMapFilterData;

   private PixelConverter                           _pc;

   private ToolItem                                 _toolItem;

   /*
    * UI controls
    */
   private Composite   _shellContainer;
   private Composite   _containerNumbers;

   private Label       _lblAllPhotos;
   private Label       _lblFilteredPhotos;
   private Combo       _comboRatingStarOperators;

   private RatingStars _ratingStars;

   public Slideout_Map2_PhotoFilter(final ToolItem toolItem, final Map2View map2View, final IDialogSettings state) {

      super(toolItem.getParent(), state, new int[] { 220, 100, 220, 100 });

      _toolItem = toolItem;
      _state = state;

      setTitleText(Messages.Photo_Filter_Label_PhotoFilter);

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);

      PhotoManager.addPhotoEventListener(this);
   }

   public void addPropertiesListener(final IPhotoFilterListener listener) {
      _photoFilterListeners.add(listener);
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      initUI(parent);

      createUI(parent);

      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
      UI.setChildColors(parent,
            colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND),
            colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND));

      updateUI();

      if (_oldMapFilterData != null) {

         /*
          * _oldMapFilterData can be set before the UI is created
          */

         updateFilterUI(_oldMapFilterData);
      }

      enableActions();

      PhotoManager.addPhotoEventListener(this);
   }

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults()
            .margins(0, 0)
            .applyTo(_shellContainer);
      {
         createUI_10_Filter(_shellContainer);
      }

      return _shellContainer;
   }

   private void createUI_10_Filter(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NO_FOCUS);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
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
         {
            /*
             * Number of filtered photos
             */
            _containerNumbers = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .hint(_pc.convertWidthInCharsToPixels(12), SWT.DEFAULT)
//                  .indent(10, 0)
                  .align(SWT.END, SWT.CENTER)
                  .applyTo(_containerNumbers);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(_containerNumbers);
            {
               /*
                * value: number of all photos
                */
               _lblAllPhotos = new Label(_containerNumbers, SWT.NO_FOCUS);
               _lblAllPhotos.setText(UI.EMPTY_STRING);
               _lblAllPhotos.setToolTipText(Messages.Photo_Filter_Label_NumberOfAllPhotos_Tooltip);
               GridDataFactory.fillDefaults()
                     .grab(true, false)
                     .align(SWT.END, SWT.FILL).applyTo(_lblAllPhotos);

               /*
                * label: number of filtered photos
                */
               final Label label = new Label(_containerNumbers, SWT.NO_FOCUS);
               label.setText(UI.DASH);

               /*
                * value: number of filtered photos
                */
               _lblFilteredPhotos = new Label(_containerNumbers, SWT.NO_FOCUS);
               _lblFilteredPhotos.setText(UI.EMPTY_STRING);
               _lblFilteredPhotos.setToolTipText(Messages.Photo_Filter_Label_NumberOfFilteredPhotos_Tooltip);
            }
         }
      }
   }

   private void enableActions() {

   }

   private void fireFilterEvent() {

      final PhotoFilterEvent filterEvent = new PhotoFilterEvent();

      filterEvent.filterRatingStars = _filterRatingStars;
      filterEvent.fiterRatingStarOperator = _ratingStarOperatorsValues[_filterRatingStarOperatorIndex];

      final Object[] listeners = _photoFilterListeners.getListeners();
      for (final Object listener : listeners) {
         ((IPhotoFilterListener) listener).photoFilterEvent(filterEvent);
      }
   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _toolItem.getBounds();
      final Point itemDisplayPosition = _toolItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   @Override
   protected void onDispose() {

      PhotoManager.removePhotoEventListener(this);
   }

   @Override
   protected void onFocus() {

      _comboRatingStarOperators.setFocus();
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

   @Override
   public void saveState() {

      _state.put(STATE_PHOTO_FILTER_RATING_STARS, _filterRatingStars);
      _state.put(STATE_PHOTO_FILTER_RATING_STAR_OPERATOR, _filterRatingStarOperatorIndex);

      super.saveState();
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

      _containerNumbers.layout();

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
