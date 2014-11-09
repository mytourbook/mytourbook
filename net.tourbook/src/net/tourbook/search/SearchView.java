/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
package net.tourbook.search;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class SearchView extends ViewPart {

	public static final String			ID						= "net.tourbook.search.SearchView"; //$NON-NLS-1$

	private static final String			SEARCH_RESULT_CSS_FILE	= "/html/search-result.css";		//$NON-NLS-1$

	private static final String			STATE_POPUP_WIDTH		= "STATE_POPUP_WIDTH";				//$NON-NLS-1$
	private static final String			STATE_POPUP_HEIGHT		= "STATE_POPUP_HEIGHT";			//$NON-NLS-1$
	private static final String			STATE_SEARCH_TEXT		= "STATE_SEARCH_TEXT";				//$NON-NLS-1$

	private final DateTimeFormatter		_dateFormatter			= DateTimeFormat.fullDate();
	private final DateTimeFormatter		_timeFormatter			= DateTimeFormat.mediumTime();

	private final IPreferenceStore		_prefStore				= TourbookPlugin.getPrefStore();
	private final IDialogSettings		_state					= TourbookPlugin.getState(ID);
	//
	private PostSelectionProvider		_postSelectionProvider;
	private IPropertyChangeListener		_prefChangeListener;
	private ITourEventListener			_tourEventListener;
	private IPartListener2				_partListener;
	//
	private String						_htmlCss;
	private String						_tourChartIconUrl;
	private String						_tourMarkerIconUrl;

	/**
	 * Set time that an error displays the correct time.
	 */
	private long						_searchStartTime		= System.currentTimeMillis();

	private MTContentProposalAdapter	_contentProposalAdapter;
	private TextContentAdapter			_controlContentAdapter	= new TextContentAdapter();
	private MTProposalProvider			_proposalProvider		= new MTProposalProvider();

	private PixelConverter				_pc;

	/*
	 * UI controls
	 */
	private Browser						_browser;
	private Text						_txtSearch;
	private Text						_txtStatus;

	public class MTContentProposalAdapter extends ContentProposalAdapter {

		/**
		 * @param control
		 * @param controlContentAdapter
		 * @param proposalProvider
		 * @param keyStroke
		 * @param autoActivationCharacters
		 */
		public MTContentProposalAdapter(final Control control,
										final IControlContentAdapter controlContentAdapter,
										final IContentProposalProvider proposalProvider,
										final KeyStroke keyStroke,
										final char[] autoActivationCharacters) {

			super(control, controlContentAdapter, proposalProvider, keyStroke, autoActivationCharacters);
		}

		/**
		 * Open popup with {@link SWT#ARROW_DOWN} key.
		 * <p>
		 * {@inheritDoc}
		 */
		@Override
		protected void openProposalPopup() {
			super.openProposalPopup();
		}
	}

	/**
	 * Copied from {@link org.eclipse.jdt.internal.ui.dialogs.GenerateToStringDialog} and adjusted.
	 */
	private class MTProposalProvider implements IContentProposalProvider {

		private String	latestContents;
		private int		latestPosition;

		private class Proposal implements IContentProposal {

			final private String	proposal;
			private int				position;

			public Proposal(final String proposal) {
				this.proposal = proposal;
				this.position = proposal.length();
			}

			public String getContent() {

				final int overlap = stringOverlap(latestContents.substring(0, latestPosition), proposal);

				position = proposal.length() - overlap;

				return proposal.substring(overlap);
			}

			public int getCursorPosition() {
				return position;
			}

			public String getDescription() {
//				return parser.getVariableDescriptions().get(proposal);
				return null;
			}

			public String getLabel() {
				return proposal;
			}
		}

		public IContentProposal[] getProposals(final String contents, final int position) {

			final List<Proposal> primaryProposals = new ArrayList<Proposal>();
			final List<Proposal> secondaryProposals = new ArrayList<Proposal>();

			final List<LookupResult> proposals = MTSearchManager.getProposals(contents, position);

			final String contentToCursor = contents.substring(0, position);

			for (final LookupResult lookupResult : proposals) {

				final String proposalString = lookupResult.key.toString();

				if (stringOverlap(contentToCursor, proposalString) > 0) {
					primaryProposals.add(new Proposal(proposalString));
				} else {
					secondaryProposals.add(new Proposal(proposalString));
				}
			}

			this.latestContents = contents;
			this.latestPosition = position;

			primaryProposals.addAll(secondaryProposals);

			return primaryProposals.toArray(new IContentProposal[0]);
		}

		/**
		 * Checks if the end of the first string is equal to the beginning of of the second string.
		 * 
		 * @param s1
		 *            first String
		 * @param s2
		 *            second String
		 * @return length of overlapping segment (0 if strings don't overlap)
		 */
		private int stringOverlap(final String s1, final String s2) {

			final int l1 = s1.length();

			for (int l = 1; l <= Math.min(s1.length(), s2.length()); l++) {

				boolean ok = true;

				for (int i = 0; i < l; i++) {
					if (s1.charAt(l1 - l + i) != s2.charAt(i)) {
						ok = false;
						break;
					}
				}

				if (ok) {
					return l;
				}
			}

			return 0;
		}
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == SearchView.this) {
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

				}
			}
		};
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == SearchView.this) {
					return;
				}

				if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						// update modified tour

						for (final TourData tourData : modifiedTours) {

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

		// removed old tour data from the selection provider
		_postSelectionProvider.clearSelection();
	}

	private void createActions() {

		fillActionBars();
	}

	private String createHTML_10_Head() {

		final String html = ""// //$NON-NLS-1$
				+ "	<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />\n" //$NON-NLS-1$
				+ "	<meta http-equiv='X-UA-Compatible' content='IE=edge' />\n" //$NON-NLS-1$
				+ _htmlCss
				+ "\n"; //$NON-NLS-1$

		return html;
	}

	private String createHTML_20_Body(final SearchResult searchResult) {

		final StringBuilder sb = new StringBuilder();

		for (final Entry<String, SearchResultItem> entry : searchResult.items.entrySet()) {

			sb.append("<div class='result-item'>"); //$NON-NLS-1$
			{
				final SearchResultItem resultItem = entry.getValue();

				final boolean isTour = resultItem.tourId != null;
				final boolean isMarker = resultItem.markerId != null;
				final String iconUrl = isTour ? _tourChartIconUrl : _tourMarkerIconUrl;

				String itemTitle = UI.EMPTY_STRING;
				final String hrefOpenItem = null;

				if (isTour) {

					final String tourTitle = resultItem.tourTitle;

					if (tourTitle != null) {
						itemTitle = tourTitle;
					}

				} else if (isMarker) {

					final String tourMarkerLabel = resultItem.markerLabel;

					if (tourMarkerLabel != null) {
						itemTitle = tourMarkerLabel;
					}
				}

				sb.append("<div class='result-title'>"); //$NON-NLS-1$
				{
					sb.append("<a class='item-title'" //
							+ (" style='" //
									+ "background: url("
									+ iconUrl + ") no-repeat;'")
							+ (" href='" + hrefOpenItem + "'") //$NON-NLS-1$ //$NON-NLS-2$
							+ (" title='" + itemTitle + "'") //$NON-NLS-1$ //$NON-NLS-2$
							+ ">" // //$NON-NLS-1$
							+ itemTitle
							+ "</a>");
				}
				sb.append("</div>\n"); //$NON-NLS-1$

				final String description = resultItem.description;
				if (description != null) {
					sb.append("<div class='result-description'>"); //$NON-NLS-1$
					{
						sb.append(description);
					}
					sb.append("</div>\n"); //$NON-NLS-1$
				}

				final long tourStartTime = resultItem.tourStartTime;
				if (tourStartTime != 0) {

					sb.append("<div class='' style='text-align:right;'>"); //$NON-NLS-1$
					{
						final DateTime dt = new DateTime(tourStartTime);

						sb.append(String.format(//
//								"%s - %s - CW %d",
								"%s - %s",
								_dateFormatter.print(dt.getMillis()),
								_timeFormatter.print(dt.getMillis())
//								dt.getWeekOfWeekyear()
								));
					}
					sb.append("</div>\n"); //$NON-NLS-1$
				}
			}
			sb.append("</div>\n"); //$NON-NLS-1$
		}

		return sb.toString();
	}

	@Override
	public void createPartControl(final Composite parent) {

		initUI(parent);

		createUI(parent);
		createActions();

		addTourEventListener();
		addPrefListener();
		addPartListener();

		// this part is a selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		restoreState();
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(1)
				.spacing(0, 2)
				.applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		{
			createUI_10_Query(container);
			createUI_20_Status(container);
			createUI_30_Result(container);
		}
	}

	private Composite createUI_10_Query(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.extendedMargins(0, 2, 2, 2)
//				.margins(0, 0)
				.applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		{
// ALTERNATIVE TEXT CONTROL
//tasktop.TextSearchControl
			/*
			 * Text: Search field
			 */
			_txtSearch = new Text(container, SWT.NONE /* SWT.BORDER */);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtSearch);
			_txtSearch.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			_txtSearch.addKeyListener(new KeyAdapter() {

				@Override
				public void keyPressed(final KeyEvent e) {
//					onSearchKey(e);
				}
			});

			_txtSearch.addTraverseListener(new TraverseListener() {
				@Override
				public void keyTraversed(final TraverseEvent e) {
					if (e.detail == SWT.TRAVERSE_RETURN) {
						onSearchSelect();
					}
				}
			});

			_contentProposalAdapter = new MTContentProposalAdapter(//
					_txtSearch,
					_controlContentAdapter,
					_proposalProvider,
					null,
					null);

			_contentProposalAdapter.addContentProposalListener(new IContentProposalListener() {

				@Override
				public void proposalAccepted(final IContentProposal proposal) {

					System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
							+ ("\tContent: " + proposal.getContent())
							+ ("\tLabel: " + proposal.getLabel())
							+ ("\tCursorPosition: " + proposal.getCursorPosition())
					//
							);
					// TODO remove SYSTEM.OUT.PRINTLN
					onSearchSelect();
				}
			});

			_contentProposalAdapter.addContentProposalListener(new IContentProposalListener2() {

				@Override
				public void proposalPopupClosed(final ContentProposalAdapter adapter) {
					// TODO Auto-generated method stub

				}

				@Override
				public void proposalPopupOpened(final ContentProposalAdapter adapter) {
					// TODO Auto-generated method stub

				}
			});
		}

		return container;
	}

	private void createUI_20_Status(final Composite parent) {

		_txtStatus = new Text(parent, SWT.WRAP | SWT.READ_ONLY);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.BEGINNING)
				.applyTo(_txtStatus);

		_txtStatus.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
	}

	private void createUI_30_Result(final Composite parent) {

		try {

			try {

				// use default browser
				_browser = new Browser(parent, SWT.NONE);

			} catch (final Exception e) {

				/*
				 * Use mozilla browser, this is necessary for Linux when default browser fails
				 * however the XULrunner needs to be installed.
				 */
				_browser = new Browser(parent, SWT.MOZILLA);
			}

			GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);

			_browser.addLocationListener(new LocationAdapter() {
				@Override
				public void changing(final LocationEvent event) {
					onBrowserLocationChanging(event);
				}
			});

			_browser.addProgressListener(new ProgressAdapter() {
				@Override
				public void completed(final ProgressEvent event) {
					onBrowserCompleted(event);
				}
			});

		} catch (final SWTError e) {

			_txtSearch.setEnabled(false);

			final String message = e.getMessage();

			updateUI_Status(NLS.bind(Messages.UI_Label_BrowserCannotBeCreated_Error, message), false);

			StatusUtil.log(e);
		}
	}

	@Override
	public void dispose() {

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		getViewSite().getPage().removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void fillActionBars() {

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

//		tbm.add(_actionTourBlogMarker);
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		try {

			/*
			 * load css from file
			 */
			final URL bundleUrl = TourbookPlugin.getDefault().getBundle().getEntry(SEARCH_RESULT_CSS_FILE);
			final URL fileUrl = FileLocator.toFileURL(bundleUrl);
			final URI fileUri = fileUrl.toURI();
			final File file = new File(fileUri);

			final String cssContent = Util.readContentFromFile(file.getAbsolutePath());

			_htmlCss = "<style>" + cssContent + "</style>"; //$NON-NLS-1$ //$NON-NLS-2$

			/*
			 * set image urls
			 */
			_tourChartIconUrl = net.tourbook.ui.UI.getIconUrl(Messages.Image__TourChart);
			_tourMarkerIconUrl = net.tourbook.ui.UI.getIconUrl(Messages.Image__TourMarker);

		} catch (final IOException | URISyntaxException e) {
			StatusUtil.showStatus(e);
		}
	}

	private void onBrowserCompleted(final ProgressEvent event) {
		// TODO Auto-generated method stub

	}

	private void onBrowserLocationChanging(final LocationEvent event) {
		// TODO Auto-generated method stub

	}

	/**
	 * Open popup with {@link SWT#ARROW_DOWN} key.
	 * 
	 * @param e
	 */
	private void onSearchKey(final KeyEvent e) {

		if (_contentProposalAdapter.isProposalPopupOpen()) {
			return;
		}

		if (e.keyCode == SWT.ARROW_DOWN) {
//		if (e.keyCode == SWT.ARROW_UP) {
//			e.doit = false;

			e.keyCode = SWT.ARROW_UP;
			_contentProposalAdapter.openProposalPopup();
		}
	}

	private void onSearchSelect() {

		if (_contentProposalAdapter.isProposalPopupOpen()) {
			return;
		}

		_searchStartTime = System.currentTimeMillis();

		String searchText = _txtSearch.getText().trim();

		// check if search text is valid
		if (searchText.length() == 0) {
			updateUI_Status("No result", false);
			return;
		}
		if (searchText.startsWith(UI.SYMBOL_STAR) || searchText.startsWith("?")) {
			updateUI_Status("* or ? is not allowed as first character", false);
			return;
		}

		if (searchText.endsWith(UI.SYMBOL_STAR) == false) {

			// Append a * otherwise nothing is found
			searchText += UI.SYMBOL_STAR;
		}

		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
				+ ("\tsearch: " + searchText));
		// TODO remove SYSTEM.OUT.PRINTLN

		final SearchResult searchResult = MTSearchManager.search(searchText);

		if (searchResult.items.size() == 0) {
			updateUI_Status("No result", false);
			return;
		}

		updateUI_SearchResult(searchResult);
	}

	private void restoreState() {

		final int popupWidth = Util.getStateInt(_state, STATE_POPUP_WIDTH, _pc.convertWidthInCharsToPixels(40));
		final int popupHeight = Util.getStateInt(_state, STATE_POPUP_HEIGHT, _pc.convertHeightInCharsToPixels(20));
		final Point popupSize = new Point(popupWidth, popupHeight);

		_contentProposalAdapter.setPopupSize(popupSize);

		final String searchText = Util.getStateString(_state, STATE_SEARCH_TEXT, UI.EMPTY_STRING);
		_txtSearch.setText(searchText);
		_txtSearch.setSelection(searchText.length());
	}

	private void saveState() {

		final Point popupSize = _contentProposalAdapter.getPopupSize();
		_state.put(STATE_POPUP_WIDTH, popupSize.x);
		_state.put(STATE_POPUP_HEIGHT, popupSize.y);

		_state.put(STATE_SEARCH_TEXT, _txtSearch.getText());
	}

	@Override
	public void setFocus() {

		_txtSearch.setFocus();
	}

	private void updateUI_SearchResult(final SearchResult searchResult) {

		final String html = "" // //$NON-NLS-1$
				+ "<!DOCTYPE html>\n" // ensure that IE is using the newest version and not the quirk mode //$NON-NLS-1$
				+ "<html style='height: 100%; width: 100%; margin: 0px; padding: 0px;'>\n" //$NON-NLS-1$
				+ ("<head>\n" + createHTML_10_Head() + "\n</head>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("<body>\n" + createHTML_20_Body(searchResult) + "\n</body>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ "</html>"; //$NON-NLS-1$

		_browser.setRedraw(true);
		_browser.setText(html);

		updateUI_Status(String.format("Hits: %d", searchResult.totalHits), true);
	}

	/**
	 * @param statusText
	 * @param isResultAvailable
	 */
	private void updateUI_Status(final String statusText, final boolean isResultAvailable) {

		/*
		 * Update status
		 */
		final long time = System.currentTimeMillis() - _searchStartTime;

		String text;
		if (statusText == null) {
			text = String.format("%d ms", time);
		} else {
			text = String.format("%s - %d ms", statusText, time);
		}

		_txtStatus.setText(text);

		/*
		 * Clear browser when an error occured.
		 */
		if (!isResultAvailable && _browser != null) {

			_browser.setRedraw(true);
			_browser.setText(UI.EMPTY_STRING);
		}
	}
}
