/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICatalogRefTourItem;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TourMarkerLogbookView extends ViewPart {

	public static final String		ID				= "net.tourbook.ui.views.TourMarkerLogbookView";	//$NON-NLS-1$

	private static final String		CSS_DESCRIPTION	= "description";
	private static final String		CSS_LABEL		= "label";

	private final IPreferenceStore	_prefStore		= TourbookPlugin.getPrefStore();

	private TourData				_tourData;

	private ISelectionListener		_postSelectionListener;

	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourEventListener;
	private IPartListener2			_partListener;

	private String					_htmlCss;

	private final DateTimeFormatter	_dateFormatter	= DateTimeFormat.fullDate();
	private final DateTimeFormatter	_timeFormatter	= DateTimeFormat.mediumTime();
	private final NumberFormat		_nf_3_3			= NumberFormat.getNumberInstance();
	{
		_nf_3_3.setMinimumFractionDigits(3);
		_nf_3_3.setMaximumFractionDigits(3);
	}

	/*
	 * UI controls
	 */
	private PageBook				_pageBook;
	private Label					_pageNoTour;

	private Composite				_viewerContainer;
	private Browser					_browser;

	private boolean					_isShowHiddenMarker;

	private void addPartListener() {

		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourMarkerLogbookView.this) {
					saveState();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

				} else if (property.equals(ITourbookPreferences.GRAPH_MARKER_IS_MODIFIED)) {

					updateUI();
				}
			}
		};
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				if (part == TourMarkerLogbookView.this) {
					return;
				}
				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if ((_tourData == null) || (part == TourMarkerLogbookView.this)) {
					return;
				}

				if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						// update modified tour

						final long viewTourId = _tourData.getTourId();

						for (final TourData tourData : modifiedTours) {
							if (tourData.getTourId() == viewTourId) {

								// get modified tour
								_tourData = tourData;

								updateUI();

								// nothing more to do, the view contains only one tour
								return;
							}
						}
					}

				} else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

					clearView();
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void clearView() {

		_tourData = null;

		_pageBook.showPage(_pageNoTour);
	}

	private String createBody() {

		final StringBuilder sb = new StringBuilder();

		final Set<TourMarker> tourMarkers = _tourData.getTourMarkers();
		final ArrayList<TourMarker> allMarker = new ArrayList<TourMarker>(tourMarkers);
		Collections.sort(allMarker);

		createBody_Title(sb);

		for (final TourMarker tourMarker : allMarker) {

			// check if marker is hidden and should not be displayed
			if (tourMarker.isMarkerVisible() == false && _isShowHiddenMarker == false) {
				continue;
			}

			sb.append("<p>");
			{
				createBody_Label(sb, tourMarker);
				createBody_Description(sb, tourMarker);
				createBody_Url(sb, tourMarker);
			}
			sb.append("</p>\n");
		}

		return sb.toString();
	}

	/**
	 * Description
	 */
	private void createBody_Description(final StringBuilder sb, final TourMarker tourMarker) {

		final String description = tourMarker.getDescription();
		if (description.length() > 0) {
			sb.append("<div class=\"" + CSS_DESCRIPTION + "\">" + description + "</div>");
		}
	}

	/**
	 * Label
	 */
	private void createBody_Label(final StringBuilder sb, final TourMarker tourMarker) {

		final String label = tourMarker.getLabel();
		if (label.length() > 0) {
			sb.append("<div class=\"" + CSS_LABEL + "\">" + label + "</div>");
		}
	}

	private void createBody_Title(final StringBuilder sb) {

		/*
		 * Date/Time
		 */
		final long recordingTime = _tourData.getTourRecordingTime();
		final DateTime dtTourStart = _tourData.getTourStartTime();
		final DateTime dtTourEnd = dtTourStart.plus(recordingTime * 1000);

		final String date = String.format(_dateFormatter.print(dtTourStart.getMillis()));

		final String time = String.format(//
				"%s - %s",
				_timeFormatter.print(dtTourStart.getMillis()),
				_timeFormatter.print(dtTourEnd.getMillis()));

		sb.append("<div class=\"date\">" + date + "</div>");
		sb.append("<div class=\"time\">" + time + "</div>");

		sb.append("<div class=\"top-margin\">&nbsp;</div>\n");

		/*
		 * Title
		 */
		final String tourTitle = _tourData.getTourTitle();
		if (tourTitle.length() > 0) {
			sb.append("<h1 class=\"title\">" + tourTitle + "</h1>\n");
		}
	}

	/**
	 * Url
	 */
	private void createBody_Url(final StringBuilder sb, final TourMarker tourMarker) {

		final String urlText = tourMarker.getUrlText();
		final String urlAddress = tourMarker.getUrlAddress();
		final boolean isText = urlText.length() > 0;
		final boolean isAddress = urlAddress.length() > 0;

		if (isText || isAddress) {

			String linkText;

			if (isAddress == false) {

				// only text is in the link -> this is not a internet address but create a link of it

				final String title = urlText;
				linkText = "<a href=\"" + urlText + "\" title=\"" + title + "\">" + urlText + "</a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			} else if (isText == false) {

				final String title = urlAddress;
				linkText = "<a href=\"" + urlAddress + "\" title=\"" + title + "\">" + urlAddress + "</a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			} else {

				final String title = urlAddress;
				linkText = "<a href=\"" + urlAddress + "\" title=\"" + title + "\">" + urlText + "</a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

			sb.append(linkText);
		}
	}

	private String createHTML_Head() {

		final String html = _htmlCss;

		return html;
	}

	@Override
	public void createPartControl(final Composite parent) {

		loadResources();
		createUI(parent);

		addSelectionListener();
		addTourEventListener();
		addPrefListener();
		addPartListener();

		// show default page
		_pageBook.showPage(_pageNoTour);

		// show markers from last selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (_tourData == null) {
			showTourFromTourProvider();
		}
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);

		_pageNoTour = new Label(_pageBook, SWT.NONE);
		_pageNoTour.setText(Messages.UI_Label_no_chart_is_selected);

		_viewerContainer = new Composite(_pageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
		{
			createUI_10_Browser(_viewerContainer);
		}
	}

	private void createUI_10_Browser(final Composite parent) {

		try {

			_browser = new Browser(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);

			_browser.addLocationListener(new LocationAdapter() {
				@Override
				public void changing(final LocationEvent event) {

					// open link in the external browser

					final String location = event.location;

					// check if this is a valid web url and not any other protocol
					if (location.startsWith("http")) {
						Util.openLink(_browser.getShell(), location);
					}

					// about:blank is the initial page
					if (location.startsWith("about:blank") == false) {
						event.doit = false;
					}
				}
			});

		} catch (final SWTError e) {

			StatusUtil.showStatus("Could not instantiate Browser: " + e.getMessage());
		}
	}

	@Override
	public void dispose() {

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getViewSite().getPage().removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void loadResources() {

		try {

			final URL bundleUrl = TourbookPlugin.getDefault().getBundle().getEntry("/html/marker-logbook.css"); //$NON-NLS-1$
			final URL fileUrl = FileLocator.toFileURL(bundleUrl);
			final URI fileUri = fileUrl.toURI();
			final File file = new File(fileUri);

			final String cssContent = Util.readContentFromFile(file.getAbsolutePath());

			_htmlCss = "<style>" + cssContent + "</style>";

		} catch (final IOException | URISyntaxException e) {
			StatusUtil.showStatus(e);
		}

	}

	private void onSelectionChanged(final ISelection selection) {

		long tourId = TourDatabase.ENTITY_IS_NOT_SAVED;

		if (selection instanceof SelectionTourData) {

			// a tour was selected, get the chart and update the marker viewer

			final SelectionTourData tourDataSelection = (SelectionTourData) selection;
			_tourData = tourDataSelection.getTourData();

			if (_tourData == null) {} else {
				tourId = _tourData.getTourId();
			}

		} else if (selection instanceof SelectionTourId) {

			tourId = ((SelectionTourId) selection).getTourId();

		} else if (selection instanceof SelectionTourIds) {

			final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
			if ((tourIds != null) && (tourIds.size() > 0)) {
				tourId = tourIds.get(0);
			}

		} else if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {
				tourId = refItem.getTourId();
			}

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if (firstElement instanceof TVICatalogComparedTour) {
				tourId = ((TVICatalogComparedTour) firstElement).getTourId();
			} else if (firstElement instanceof TVICompareResultComparedTour) {
				tourId = ((TVICompareResultComparedTour) firstElement).getComparedTourData().getTourId();
			}

		} else if (selection instanceof SelectionDeletedTours) {

			clearView();
		}

		if (tourId >= TourDatabase.ENTITY_IS_NOT_SAVED) {

			final TourData tourData = TourManager.getInstance().getTourData(tourId);
			if (tourData != null) {
				_tourData = tourData;
			}
		}

		final boolean isTour = (tourId >= 0) && (_tourData != null);

		if (isTour) {
			_pageBook.showPage(_viewerContainer);
			updateUI();
		}
	}

	private void saveState() {

	}

	@Override
	public void setFocus() {

	}

	private void showTourFromTourProvider() {

		_pageBook.showPage(_pageNoTour);

		// a tour is not displayed, find a tour provider which provides a tour
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				// validate widget
				if (_pageBook.isDisposed()) {
					return;
				}

				/*
				 * check if tour was set from a selection provider
				 */
				if (_tourData != null) {
					return;
				}

				final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();

				if ((selectedTours != null) && (selectedTours.size() > 0)) {
					onSelectionChanged(new SelectionTourData(selectedTours.get(0)));
				}
			}
		});
	}

	/**
	 * Update the UI from {@link #_tourData}.
	 */
	private void updateUI() {

		if (_tourData == null) {
			return;
		}

		_isShowHiddenMarker = _prefStore.getBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_HIDDEN_MARKER);

		final String html = "<html>\n" //
				+ ("<head>\n" + createHTML_Head() + "\n</head>\n")
				+ ("<body>\n" + createBody() + "\n</body>\n")
				+ "</html>";

		_browser.setText(html);
	}

}
