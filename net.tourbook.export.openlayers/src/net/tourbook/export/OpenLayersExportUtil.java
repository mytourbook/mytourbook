package net.tourbook.export;

import java.util.ArrayList;

import net.tourbook.data.TourData;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.extension.export.ExportTourExtension;
import net.tourbook.ui.ITourProvider;

/**
 * TODO NR: join with ExportUtil
 */
public class OpenLayersExportUtil {

	/**
	 * Get the tour export format for the given file extension.
	 * 
	 * @param fileExtension
	 *            file extension as defined in the <code>fileextension</code> attribute of the
	 *            <code>export</code> plugin extension, e.g. 'gpx'
	 * @return export format instance or null when no export format found for the file extension
	 */
	public static ExportTourExtension getExportTour(String fileExtension) {

		// create dummy instance to obtain export extensions from
		ActionExport actionExport = new ActionExport(new ITourProvider() {

			@Override
			public ArrayList<TourData> getSelectedTours() {
				throw new AssertionError("This instance is not supposed to be run, only for getting extensions"); //$NON-NLS-1$
			}
		});
		ArrayList<ExportTourExtension> extensionPoints = actionExport.getExtensionPoints();

		ExportTourExtension result = null;
		for (final ExportTourExtension exportTourExtension : extensionPoints) {
			if (fileExtension.toLowerCase().equals(exportTourExtension.getFileExtension().toLowerCase())) {
				result = exportTourExtension;
			}
		}
		return result;
	}
}
