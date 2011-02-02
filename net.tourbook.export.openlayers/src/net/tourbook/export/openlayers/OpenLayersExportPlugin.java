package net.tourbook.export.openlayers;

import static org.eclipse.ui.browser.IWorkbenchBrowserSupport.AS_VIEW;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import net.tourbook.data.TourData;
import net.tourbook.export.ExportTourHTML;
import net.tourbook.export.OpenLayersExportUtil;
import net.tourbook.extension.export.ExportTourExtension;
import net.tourbook.mapping.TourMapView;
import net.tourbook.util.StatusUtil;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.util.Util;

/**
 * The main class of the OpenLayers export plugin. Offers an action which opens an external browser
 * to show the current map position and displayed tours with OpenLayers on Google or Bing Maps, ...
 */
public class OpenLayersExportPlugin {

	private static final String	OPENLAYERS_BROWSER_ID	= "net.tourbook.export.openlayers"; //$NON-NLS-1$

	private final TourMapView	_tourMapView;

	private String				_fileExtension;

	public OpenLayersExportPlugin(final TourMapView tourMapView) {
		this._tourMapView = tourMapView;
		this._fileExtension = ExportTourHTML.GPX_EXTENSION;
	}

	public void actionOpenWebMapBrowser() {

		final String targetDir = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$

		try {

			ExportTourExtension exportTour = OpenLayersExportUtil.getExportTour(_fileExtension);

			if (exportTour != null) {

				final List<TourData> tourDataList = _tourMapView.getTourDataList();

				// write tour files
				List<String> allFilePath = exportTour.exportTourHeadless(tourDataList, targetDir);

				// delete temporary file when program exits
				for (String filePath : allFilePath) {
					final File tourTmpFile = new File(filePath);
					tourTmpFile.deleteOnExit();
				}

				// write HTML
				final ExportOpenLayers exportOpenLayers = new ExportOpenLayers(_fileExtension);
				final String htmlName = exportOpenLayers.doExportHTML(tourDataList, targetDir, getMapContext());

				// open browser
				browse(targetDir, htmlName);

			} else {
				StatusUtil.log("No ExportTourExtension found for extension '" + _fileExtension + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			}

		} catch (final IOException e) {
			StatusUtil.log(e);
		}
	}

	private MapContext getMapContext() {

		Map map = _tourMapView.getMap();

		MapContext mapContext = new MapContext();
		mapContext.setLat(map.getGeoCenter().latitude);
		mapContext.setLon(map.getGeoCenter().longitude);
		mapContext.setZoom(map.getZoom());
		return mapContext;
	}

	private void browse(String tmpDir, String htmlName) {

		URI uri = new File(tmpDir, htmlName).toURI();

		final Util.IBrowserGetter browserGetter = new Util.IBrowserGetter() {
			@Override
			public IWebBrowser getBrowser(IWorkbenchBrowserSupport support) throws PartInitException {

				// eclipse bug: name is ignored with AS_VIEW (view title is always 'Internal Web Browser') 
				// fixed in 3.7, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=322000
				return support.createBrowser(AS_VIEW, OPENLAYERS_BROWSER_ID, Messages.view_name_browser_map, null);
			}
		};
		Util.openLink(browserGetter, uri.toString());
	}

	public String getFileExtension() {
		return _fileExtension;
	}

	public void setFileExtension(String fileExtension) {
		this._fileExtension = fileExtension;
	}
}
