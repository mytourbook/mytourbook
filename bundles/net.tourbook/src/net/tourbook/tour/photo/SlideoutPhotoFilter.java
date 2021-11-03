/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.PhotoRatingStarOperator;
import net.tourbook.photo.RatingStars;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Photo filter slideout
 */
public class SlideoutPhotoFilter extends AdvancedSlideout {

   /**
    * <b>THEY MUST BE IN SYNC WITH </b> {@link #_allRatingStar_Labels} and
    * {@link #_allRatingStar_Tooltip}
    */
   private static final PhotoRatingStarOperator[] _allRatingStar_Operators    = {

         PhotoRatingStarOperator.HAS_ANY,
         PhotoRatingStarOperator.IS_EQUAL,
         PhotoRatingStarOperator.IS_MORE_OR_EQUAL,
         PhotoRatingStarOperator.IS_LESS_OR_EQUAL,

   };

   /**
    * <b>THEY MUST BE IN SYNC WITH </b> {@link #_allRatingStar_Operators}
    */
   private static final String[]                  _allRatingStar_Labels       = {

         Messages.Photo_Filter_Operator_HasAny,
         Messages.Photo_Filter_Operator_IsEqual,
         Messages.Photo_Filter_Operator_IsMore,
         Messages.Photo_Filter_Operator_IsLess,

   };

   /**
    * <b>THEY MUST BE IN SYNC WITH </b> {@link #_allRatingStar_Operators}
    */
   private static final String[]                  _allRatingStar_Tooltip      = {

         Messages.Photo_Filter_Operator_HasAny_Tooltip,
         Messages.Photo_Filter_Operator_IsEqual_Tooltip,
         Messages.Photo_Filter_Operator_IsMore_Tooltip,
         Messages.Photo_Filter_Operator_IsLess_Tooltip,

   };

   /**
    * Filter operator
    */
   private int                                    _selectedRatingStars        = 0;
   private PhotoRatingStarOperator                _selectedRatingStarOperator = PhotoRatingStarOperator.IS_MORE_OR_EQUAL;

   private PixelConverter                         _pc;

   private IMapWithPhotos                         _mapWithPhotos;
   private ToolItem                               _toolItem;

   /*
    * UI controls
    */
   private Composite   _shellContainer;
   private Composite   _containerNumbers;

   private Label       _lblAllPhotos;
   private Label       _lblFilteredPhotos;
   private Combo       _comboRatingStarOperators;

   private RatingStars _ratingStars;

   public SlideoutPhotoFilter(final ToolItem toolItem,
                              final IMapWithPhotos mapWithPhotos,
                              final IDialogSettings state) {

      super(toolItem.getParent(), state, new int[] { 220, 100, 220, 100 });

      _toolItem = toolItem;
      _mapWithPhotos = mapWithPhotos;

      setTitleText(Messages.Photo_Filter_Title_Map2PhotoFilter);

      // prevent that the opened slideout is partly hidden
      setIsForceBoundsToBeInsideOfViewport(true);
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

      enableActions();
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
                  onSelect_RatingStarOperands();
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
                  onSelect_RatingStars();
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
   protected void onFocus() {

      _comboRatingStarOperators.setFocus();
   }

   private void onSelect_RatingStarOperands() {

      final int ratingStarOperatorIndex = _comboRatingStarOperators.getSelectionIndex();

      _selectedRatingStarOperator = _allRatingStar_Operators[ratingStarOperatorIndex];
      updateUI_OperatorTooltip(ratingStarOperatorIndex);

      updateMapPhotoFilter();
   }

   private void onSelect_RatingStars() {

      final int selectedStars = _ratingStars.getSelection();

      _selectedRatingStars = selectedStars;

      final int ratingStarOperatorIndex = _comboRatingStarOperators.getSelectionIndex();
      updateUI_OperatorTooltip(ratingStarOperatorIndex);

      enableActions();

      updateMapPhotoFilter();
   }

   public void restoreState(final int photoFilter_RatingStars, final Enum<PhotoRatingStarOperator> photoFilter_RatingStar_Operator) {

      // keep values, when this method is called, then the slideout UI was not yet created
      _selectedRatingStars = photoFilter_RatingStars;
      _selectedRatingStarOperator = (PhotoRatingStarOperator) photoFilter_RatingStar_Operator;
   }

   @Override
   public void saveState() {

      // save slideout position/size
      super.saveState();
   }

   private void updateMapPhotoFilter() {

      _mapWithPhotos.updatePhotoFilter(_selectedRatingStars, _selectedRatingStarOperator);
   }

   private void updateUI() {

      // select rating star
      _ratingStars.setSelection(_selectedRatingStars);

      for (final String operatorLabel : _allRatingStar_Labels) {
         _comboRatingStarOperators.add(operatorLabel);
      }

      // select operator
      int ratingStarOperatorIndex = 0;
      for (int operatorIndex = 0; operatorIndex < _allRatingStar_Operators.length; operatorIndex++) {
         final PhotoRatingStarOperator photoRatingStarOperator = _allRatingStar_Operators[operatorIndex];

         if (photoRatingStarOperator.equals(_selectedRatingStarOperator)) {
            ratingStarOperatorIndex = operatorIndex;
            break;
         }
      }

      _comboRatingStarOperators.select(ratingStarOperatorIndex);
      updateUI_OperatorTooltip(ratingStarOperatorIndex);

      updateUI_NumberOfPhotos();
   }

   /**
    * This is called after the filter is run to update depending UI controls, e.g. number of
    * filtered photos
    *
    * @param numFilteredPhotos
    * @param numAllPhotos
    * @param data
    */
   public void updateUI_NumberOfPhotos() {

      if (_lblAllPhotos == null || _lblAllPhotos.isDisposed()) {

         // UI is not initialized

         return;
      }

      _lblAllPhotos.setText(Integer.toString(_mapWithPhotos.getPhotos().size()));
      _lblFilteredPhotos.setText(Integer.toString(_mapWithPhotos.getFilteredPhotos().size()));

      _containerNumbers.layout();
   }

   private void updateUI_OperatorTooltip(final int ratingStarOperatorIndex) {

      if (_selectedRatingStarOperator == PhotoRatingStarOperator.HAS_ANY) {

         // there is no number of rating stars

         _comboRatingStarOperators.setToolTipText(_allRatingStar_Tooltip[ratingStarOperatorIndex]);

      } else {

         _comboRatingStarOperators.setToolTipText(NLS.bind(_allRatingStar_Tooltip[ratingStarOperatorIndex], _selectedRatingStars));
      }
   }

}
