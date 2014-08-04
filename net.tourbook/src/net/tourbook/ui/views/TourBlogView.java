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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.DialogMarker;
import net.tourbook.tour.DialogQuickEdit;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChart;
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
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TourBlogView extends ViewPart {

	public static final String		ID					= "net.tourbook.ui.views.TourBlogView";		//$NON-NLS-1$

	private static final String		HREF_TOKEN			= "#";											//$NON-NLS-1$

	private static final String		ACTION_EDIT_MARKER	= "EditMarker";								//$NON-NLS-1$
	private static final String		ACTION_EDIT_TOUR	= "EditTour";									//$NON-NLS-1$
	private static final String		ACTION_OPEN_MARKER	= "OpenMarker";								//$NON-NLS-1$

	private static final String		HREF_EDIT_MARKER	= HREF_TOKEN + ACTION_EDIT_MARKER + HREF_TOKEN;
	private static final String		HREF_EDIT_TOUR		= HREF_TOKEN + ACTION_EDIT_TOUR;
	private static final String		HREF_OPEN_MARKER	= HREF_TOKEN + ACTION_OPEN_MARKER + HREF_TOKEN;

	private final IPreferenceStore	_prefStore			= TourbookPlugin.getPrefStore();

	private PostSelectionProvider	_postSelectionProvider;

	private ISelectionListener		_postSelectionListener;
	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourEventListener;
	private IPartListener2			_partListener;

	private TourData				_tourData;
	private String					_htmlCss;
	private String					_actionEditImageUrl;

	private boolean					_isShowHiddenMarker;

	private final DateTimeFormatter	_dateFormatter		= DateTimeFormat.fullDate();
	private final DateTimeFormatter	_timeFormatter		= DateTimeFormat.mediumTime();

	/*
	 * UI controls
	 */
	private PageBook				_pageBook;
	private Label					_pageNoTour;
	private Composite				_pageNoBrowser;
	private Composite				_pageContent;

	private Browser					_browser;
	private TourChart				_tourChart;
	private Text					_txtNoBrowser;

	private void addPartListener() {

		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourBlogView.this) {
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
				if (part == TourBlogView.this) {
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

				if ((_tourData == null) || (part == TourBlogView.this)) {
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

								// removed old tour data from the selection provider
								_postSelectionProvider.clearSelection();

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

		// removed old tour data from the selection provider
		_postSelectionProvider.clearSelection();

		showInvalidPage();
	}

	private String convertLineBreaks(final String text) {

		return text.replaceAll("\\r\\n|\\r|\\n", "<br>");
	}

	private String createHead() {

		final String html = ""//
				+ "	<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />\n"
				+ "	<meta http-equiv='X-UA-Compatible' content='IE=edge' />\n"
				+ _htmlCss
				+ "\n";

		return html;
	}

	private String createHtml() {

		final StringBuilder sb = new StringBuilder();

		final Set<TourMarker> tourMarkers = _tourData.getTourMarkers();
		final ArrayList<TourMarker> allMarker = new ArrayList<TourMarker>(tourMarkers);
		Collections.sort(allMarker);

		sb.append("<div class='action-hover-container'>\n");
		{
			createHtml_TourTitle(sb);
			createHtml_TourDescription(sb);
		}
		sb.append("</div>\n");

		for (final TourMarker tourMarker : allMarker) {

			// check if marker is hidden and should not be displayed
			if (tourMarker.isMarkerVisible() == false && _isShowHiddenMarker == false) {
				continue;
			}

			sb.append("<div class='action-hover-container'>\n");
			{
				createHtml_MarkerLabel(sb, tourMarker);
				createHtml_MarkerDescription(sb, tourMarker);
				createHtml_MarkerUrl(sb, tourMarker);
			}
			sb.append("</div>\n");
		}

		return sb.toString();
	}

	/**
	 * Description
	 */
	private void createHtml_MarkerDescription(final StringBuilder sb, final TourMarker tourMarker) {

		final String description = tourMarker.getDescription();
		if (description.length() > 0) {
			sb.append("<p class='description'>" + convertLineBreaks(description) + "</p>\n");
		}
	}

	/**
	 * Label
	 */
	private void createHtml_MarkerLabel(final StringBuilder sb, final TourMarker tourMarker) {

		final String hrefOpen = HREF_OPEN_MARKER + tourMarker.getMarkerId();
		final String hrefEdit = HREF_EDIT_MARKER + tourMarker.getMarkerId();

		final String hoverEdit = "Edit marker: \"" + tourMarker.getLabel() + "\"";
		final String hoverOpen = "Navigate to the marker \""
				+ tourMarker.getLabel()
				+ "\" in other views, e.g. Tour Chart, 2D/3D Map";

		final String label = tourMarker.getLabel();
		String textOpen;
		if (label.length() == 0) {
			textOpen = hrefOpen;
		} else {
			textOpen = label;
		}

		sb.append("<div class='marker-title'>\n"

				+ ("<div class='action-container'>"
						+ ("<a class='action' style='background: url("
								+ _actionEditImageUrl
								+ ") no-repeat;'"
								+ (" href='" + hrefEdit + "'")
								+ (" title='" + hoverEdit + "'")
								+ ">" //
						+ "</a>") //
				+ "	</div>\n")

				+ ("<a class='label-text' href='" + hrefOpen + "' title='" + hoverOpen + "'>" + textOpen + "</a>\n")
				+ "</div>\n");
	}

	/**
	 * Url
	 */
	private void createHtml_MarkerUrl(final StringBuilder sb, final TourMarker tourMarker) {

		final String urlText = tourMarker.getUrlText();
		final String urlAddress = tourMarker.getUrlAddress();
		final boolean isText = urlText.length() > 0;
		final boolean isAddress = urlAddress.length() > 0;

		if (isText || isAddress) {

			String linkText;

			if (isAddress == false) {

				// only text is in the link -> this is not a internet address but create a link of it

				final String title = urlText;
				linkText = "<a href='" + urlText + "' title='" + title + "'>" + urlText + "</a>\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			} else if (isText == false) {

				final String title = urlAddress;
				linkText = "<a href='" + urlAddress + "' title='" + title + "'>" + urlAddress + "</a>\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			} else {

				final String title = urlAddress;
				linkText = "<a href='" + urlAddress + "' title='" + title + "'>" + urlText + "</a>\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

			sb.append(linkText);
		}
	}

	private void createHtml_TourDescription(final StringBuilder sb) {

		final String tourDescription = _tourData.getTourDescription();

		if (tourDescription.length() == 0) {
			return;
		}

		sb.append("<div class='tour-description'>" + convertLineBreaks(tourDescription) + "</div>\n");
	}

	private void createHtml_TourTitle(final StringBuilder sb) {

		/*
		 * Date/Time header
		 */
		final long recordingTime = _tourData.getTourRecordingTime();
		final DateTime dtTourStart = _tourData.getTourStartTime();
		final DateTime dtTourEnd = dtTourStart.plus(recordingTime * 1000);

		final String date = String.format(_dateFormatter.print(dtTourStart.getMillis()));

		final String time = String.format(//
				"%s - %s",
				_timeFormatter.print(dtTourStart.getMillis()),
				_timeFormatter.print(dtTourEnd.getMillis()));

		sb.append("<div class='date'>" + date + "</div>\n");
		sb.append("<div class='time'>" + time + "</div>\n");

		sb.append("<div class='top-margin'>&nbsp;</div>\n");

		/*
		 * Tour title
		 */
		String tourTitle = _tourData.getTourTitle();
		if (tourTitle.length() == 0) {
			tourTitle = "&nbsp;";
		}

		final String hoverEdit = "Quick edit: \"" + tourTitle + "\"";

		sb.append("<div class='action-hover-container'>\n"

				+ ("<div class='action-container'>"
						+ ("<a class='action' style='background: url("
								+ _actionEditImageUrl
								+ ") no-repeat;'"
								+ (" href='" + HREF_EDIT_TOUR + "'")
								+ (" title='" + hoverEdit + "'")
								+ ">" //
						+ "</a>") //
				+ "	</div>\n")

				+ ("<span class='log-title'>" + tourTitle + "</span>\n")
				+ "</div>\n");
	}

	@Override
	public void createPartControl(final Composite parent) {

		loadResources();
		createUI(parent);

		addSelectionListener();
		addTourEventListener();
		addPrefListener();
		addPartListener();

		showInvalidPage();

		// this part is a selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

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

		_pageNoBrowser = new Composite(_pageBook, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageNoBrowser);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_pageNoBrowser);
		_pageNoBrowser.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		{
			_txtNoBrowser = new Text(_pageNoBrowser, SWT.WRAP | SWT.READ_ONLY);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.align(SWT.FILL, SWT.BEGINNING)
					.applyTo(_txtNoBrowser);
			_txtNoBrowser.setText(Messages.UI_Label_BrowserCannotBeCreated);
		}

		_pageContent = new Composite(_pageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_pageContent);
		{
			createUI_10_Browser(_pageContent);
		}
	}

	private void createUI_10_Browser(final Composite parent) {

		try {

			_browser = new Browser(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);

			_browser.addLocationListener(new LocationAdapter() {
				@Override
				public void changing(final LocationEvent event) {
					onLocationChanging(event);
				}

			});

		} catch (final SWTError e) {

			_txtNoBrowser.setText(NLS.bind(Messages.UI_Label_BrowserCannotBeCreated_Error, e.getMessage()));
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

	private void fireMarkerPosition(final StructuredSelection selection) {

		final Object[] selectedMarker = selection.toArray();

		if (selectedMarker.length > 0) {

			final ArrayList<TourMarker> allTourMarker = new ArrayList<TourMarker>();

			for (final Object object : selectedMarker) {
				allTourMarker.add((TourMarker) object);
			}

			_postSelectionProvider.setSelection(new SelectionTourMarker(_tourData, allTourMarker));
		}
	}

	private void hrefActionEditMarker(final TourMarker selectedTourMarker) {

		if (_tourData.isManualTour()) {
			// a manually created tour do not have time slices -> no markers
			return;
		}

		final DialogMarker markerDialog = new DialogMarker(
				Display.getCurrent().getActiveShell(),
				_tourData,
				selectedTourMarker);

		if (markerDialog.open() == Window.OK) {
			TourManager.saveModifiedTour(_tourData);
		}
	}

	private void hrefActionEditTour() {

		if (new DialogQuickEdit(//
				Display.getCurrent().getActiveShell(),
				_tourData).open() == Window.OK) {

			TourManager.saveModifiedTour(_tourData);
		}
	}

	/**
	 * Fire a selection for the selected marker(s).
	 */
	private void hrefActionOpenMarker(final StructuredSelection selection) {

		// a chart must be available
		if (_tourChart == null) {

			final TourChart tourChart = TourManager.getInstance().getActiveTourChart();

			if ((tourChart == null) || tourChart.isDisposed()) {

				fireMarkerPosition(selection);

				return;

			} else {
				_tourChart = tourChart;
			}
		}

		final Object[] selectedMarker = selection.toArray();

		if (selectedMarker.length > 1) {

			// two or more markers are selected

			_postSelectionProvider.setSelection(new SelectionChartXSliderPosition(
					_tourChart,
					((TourMarker) selectedMarker[0]).getSerieIndex(),
					((TourMarker) selectedMarker[selectedMarker.length - 1]).getSerieIndex()));

		} else if (selectedMarker.length > 0) {

			// one marker is selected

			_postSelectionProvider.setSelection(new SelectionChartXSliderPosition(
					_tourChart,
					((TourMarker) selectedMarker[0]).getSerieIndex(),
					SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION));
		}
	}

	private void loadResources() {

		try {

			/*
			 * load css from file
			 */
			URL bundleUrl = TourbookPlugin.getDefault().getBundle().getEntry("/html/tour-blog.css"); //$NON-NLS-1$
			URL fileUrl = FileLocator.toFileURL(bundleUrl);
			final URI fileUri = fileUrl.toURI();
			final File file = new File(fileUri);

			final String cssContent = Util.readContentFromFile(file.getAbsolutePath());

			_htmlCss = "<style>" + cssContent + "</style>";

			/*
			 * set edit image url
			 */
			bundleUrl = TourbookPlugin.getDefault().getBundle().getEntry("/icons/quick-edit.gif"); //$NON-NLS-1$
			fileUrl = FileLocator.toFileURL(bundleUrl);
			_actionEditImageUrl = fileUrl.toExternalForm();

		} catch (final IOException | URISyntaxException e) {
			StatusUtil.showStatus(e);
		}

	}

	private void onLocationChanging(final LocationEvent event) {

		final String location = event.location;
		final String[] locationParts = location.split(HREF_TOKEN);

		if (locationParts.length == 3) {

			// a tour marker id is selected, fire tour marker selection

			try {

				/**
				 * Split location<br>
				 * Part 1: location, e.g. "about"<br>
				 * Part 2: action<br>
				 * Part 3: markerID
				 */
				final String markerIdText = locationParts[2];
				final long markerId = Long.parseLong(markerIdText);

				// get tour marker by id
				TourMarker hrefTourMarker = null;
				for (final TourMarker tourMarker : _tourData.getTourMarkers()) {
					if (tourMarker.getMarkerId() == markerId) {
						hrefTourMarker = tourMarker;
						break;
					}
				}

				final String action = locationParts[1];

				if (hrefTourMarker != null) {

					switch (action) {
					case ACTION_EDIT_MARKER:
						hrefActionEditMarker(hrefTourMarker);
						break;

					case ACTION_OPEN_MARKER:
						hrefActionOpenMarker(new StructuredSelection(hrefTourMarker));
						break;
					}
				}

			} catch (final Exception e) {
				// ignore
			}

		} else if (locationParts.length == 2) {

			final String action = locationParts[1];

			switch (action) {
			case ACTION_EDIT_TOUR:
				hrefActionEditTour();
				break;
			}

		} else if (location.startsWith("http")) {

			// open link in the external browser

			// check if this is a valid web url and not any other protocol
			Util.openLink(_browser.getShell(), location);
		}

		if (location.equals("about:blank") == false) {

			// about:blank is the initial page

			event.doit = false;
		}
	}

	private void onSelectionChanged(final ISelection selection) {

		long tourId = TourDatabase.ENTITY_IS_NOT_SAVED;

		if (selection instanceof SelectionTourData) {

			// a tour was selected, get the chart and update the marker viewer

			final SelectionTourData tourDataSelection = (SelectionTourData) selection;
			_tourData = tourDataSelection.getTourData();

			if (_tourData == null) {
				_tourChart = null;
			} else {
				_tourChart = tourDataSelection.getTourChart();
				tourId = _tourData.getTourId();
			}

		} else if (selection instanceof SelectionTourId) {

			_tourChart = null;
			tourId = ((SelectionTourId) selection).getTourId();

		} else if (selection instanceof SelectionTourIds) {

			final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
			if ((tourIds != null) && (tourIds.size() > 0)) {
				_tourChart = null;
				tourId = tourIds.get(0);
			}

		} else if (selection instanceof SelectionTourCatalogView) {

			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;

			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
			if (refItem != null) {
				_tourChart = null;
				tourId = refItem.getTourId();
			}

		} else if (selection instanceof StructuredSelection) {

			_tourChart = null;
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

		final boolean isTourAvailable = (tourId >= 0) && (_tourData != null);
		if (isTourAvailable && _browser != null) {
			_pageBook.showPage(_pageContent);
			updateUI();
		}
	}

	private void saveState() {

	}

	@Override
	public void setFocus() {

	}

	private void showInvalidPage() {

		_pageBook.showPage(_browser == null ? _pageNoBrowser : _pageNoTour);
	}

	private void showTourFromTourProvider() {

		showInvalidPage();

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

		if (_tourData == null || _browser == null) {
			return;
		}

		_isShowHiddenMarker = _prefStore.getBoolean(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_HIDDEN_MARKER);

//		Force Internet Explorer to not use compatibility mode. Internet Explorer believes that websites under
//		several domains (including "ibm.com") require compatibility mode. You may see your web application run
//		normally under "localhost", but then fail when hosted under another domain (e.g.: "ibm.com").
//		Setting "IE=Edge" will force the latest standards mode for the version of Internet Explorer being used.
//		This is supported for Internet Explorer 8 and later. You can also ease your testing efforts by forcing
//		specific versions of Internet Explorer to render using the standards mode of previous versions. This
//		prevents you from exploiting the latest features, but may offer you compatibility and stability. Lookup
//		the online documentation for the "X-UA-Compatible" META tag to find which value is right for you.

		final String html = "" //
				+ "<!DOCTYPE html>\n" // ensure that IE is using the newest version and not the quirk mode
				+ "<html style='height: 100%; width: 100%; margin: 0px; padding: 0px;'>\n"
				+ ("<head>\n" + createHead() + "\n</head>\n")
				+ ("<body>\n" + createHtml() + "\n</body>\n")
				+ "</html>";

		_browser.setText(html);
	}

}
