/*******************************************************************************
 * Copyright (C) 2005, 2015, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/*
 *  based on this files:
 *  net.tourbook.device.gpx.PrefPageImportGPX.java
 *  net.tourbook.preferences.PrefPageMap2Appearance.java
 *  net.tourbook.preferences.PrefPageMap25OfflineMap.java
*/

public class PrefPageMap25Appearance extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   public static final String   PHOTO_TITLE_TYPE_NONE    = "none";                       //$NON-NLS-1$
   public static final String   PHOTO_TITLE_TYPE_TIME    = "time";                       //$NON-NLS-1$
   public static final String   PHOTO_TITLE_TYPE_RATING  = "rating";                     //$NON-NLS-1$
   public static final String   DEFAULT_PHOTO_TITLE_TYPE      = PHOTO_TITLE_TYPE_NONE;
   private IPreferenceStore      _prefStore   = TourbookPlugin.getPrefStore();

   private boolean            _isModified;
   private SelectionAdapter _defaultSelectionListener;


   /*
	 * UI controls
	 */
   private Button _rdoPhotoTitleNone;
	private Button _rdoPhotoTitleTime;
   private Button _rdoPhotoTitleRating;


	@Override
   protected void createFieldEditors() {

      createUI(getFieldEditorParent());

      // content is set in initialize() !!!;
   }

   private void createUI(final Composite parent) {
      initUI(parent);

      // VERY IMPORTANT, otherwise nothing is displayed
      GridLayoutFactory.fillDefaults().applyTo(parent);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
		{
         createUI_10_PhotoTitle(container);
		}
	}

   private void createUI_10_PhotoTitle(final Composite parent) {

		final Group groupContainer = new Group(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(groupContainer);
      groupContainer.setText(Messages.Pref_Map25_Appearance_Group_PhotoLayer);
      //GridDataFactory.fillDefaults().grab(true, false).applyTo(groupContainer);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(groupContainer);
		{
			// label
			{
				final Label label = new Label(groupContainer, SWT.NONE);
            label.setText(Messages.Pref_Map25_Appearance_Label_PhotoTitle);
            //GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);

            final Composite radioContainer = new Composite(groupContainer, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(radioContainer);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(radioContainer);

				{
               _rdoPhotoTitleNone = new Button(radioContainer, SWT.RADIO);
               _rdoPhotoTitleNone.setText(Messages.Pref_Map25_Appearance_Label_PhotoTitleNone);
               _rdoPhotoTitleNone.addSelectionListener(_defaultSelectionListener);
               //_rdoPhotoTitleNone.setToolTipText("Pref_Map25_Appearance_Label_PhotoTitleNone_Tooltip");

               _rdoPhotoTitleTime = new Button(radioContainer, SWT.RADIO);
               _rdoPhotoTitleTime.setText(Messages.Pref_Map25_Appearance_Label_PhotoTitleTime);
               _rdoPhotoTitleTime.addSelectionListener(_defaultSelectionListener);
               //_rdoPhotoTitleTime.setToolTipText("Pref_Map25_Appearance_Label_PhotoTitleTime_Tooltip");

               _rdoPhotoTitleRating = new Button(radioContainer, SWT.RADIO);
               _rdoPhotoTitleRating.setText(Messages.Pref_Map25_Appearance_Label_PhotoTitleStars);
               _rdoPhotoTitleRating.addSelectionListener(_defaultSelectionListener);
               //_rdoPhotoTitleStars.setToolTipText("Pref_Map25_Appearance_Label_PhotoTitleStars_Tooltip");
				}
			}
		}
	}

   private void enableControls() {
      //final boolean isTrackOpacity = _chkTrackOpacity.getSelection();
   }

	private String getPhotoTitleType() {

      final String photoTitleType;

      if (_rdoPhotoTitleNone.getSelection()) {
         photoTitleType = PHOTO_TITLE_TYPE_NONE;
      } else if (_rdoPhotoTitleTime.getSelection()) {
         photoTitleType = PHOTO_TITLE_TYPE_TIME;
      } else if (_rdoPhotoTitleRating.getSelection()) {
         photoTitleType = PHOTO_TITLE_TYPE_RATING;
      } else {
         photoTitleType = DEFAULT_PHOTO_TITLE_TYPE;
      }

      return photoTitleType;
   }

   @Override
   public void init(final IWorkbench workbench) {
      setPreferenceStore(_prefStore);
   }

   @Override
   protected void initialize() {

      super.initialize();

      // #####################################################################################
      //
      // must be done after the initialize() method because this will overwrite prop listener
      //
      // it took me several hours to figure out this problem
      //
      // #####################################################################################

      final IPropertyChangeListener propListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {
            enableControls();
         }
      };

      restoreState();

   }

   private void initUI(final Control parent) {

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            //onChangeProperty();
         }
      };

   }


   @Override
   protected void performDefaults() {
      _isModified = true;
      updateUI_SetPhotoTitleType(_prefStore.getDefaultString(ITourbookPreferences.MAP25_PHOTO_TITLE_TYPE));

      super.performDefaults();

      enableControls();
   }

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		if (isOK) {
			saveState();
		}

		return isOK;
	}

	private void restoreState() {
      updateUI_SetPhotoTitleType(_prefStore.getString(ITourbookPreferences.MAP25_PHOTO_TITLE_TYPE));
      enableControls();
	}

	private void saveState() {
      // photo title type
      _prefStore.setValue(ITourbookPreferences.MAP25_PHOTO_TITLE_TYPE, getPhotoTitleType());

	}

   private void updateUI_SetPhotoTitleType(String photoTitleType) {

      if      (photoTitleType.equals(PHOTO_TITLE_TYPE_NONE)   == false
            && photoTitleType.equals(PHOTO_TITLE_TYPE_TIME)   == false
            && photoTitleType.equals(PHOTO_TITLE_TYPE_RATING) == false) {

         photoTitleType = DEFAULT_PHOTO_TITLE_TYPE;
      }

      _rdoPhotoTitleNone.setSelection(photoTitleType.equals(PHOTO_TITLE_TYPE_NONE));
      _rdoPhotoTitleTime.setSelection(photoTitleType.equals(PHOTO_TITLE_TYPE_TIME));
      _rdoPhotoTitleRating.setSelection(photoTitleType.equals(PHOTO_TITLE_TYPE_RATING));

   }

}
