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
package net.tourbook.device.garmin;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import net.tourbook.common.UI;
import net.tourbook.common.util.MtMath;
import net.tourbook.ext.velocity.VelocityService;
import net.tourbook.importdata.DataTransferWizard;
import net.tourbook.importdata.ExternalDevice;
import net.tourbook.importdata.RawDataManager;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.dinopolis.gpstool.gpsinput.GPSException;
import org.dinopolis.gpstool.gpsinput.GPSRoute;
import org.dinopolis.gpstool.gpsinput.GPSSerialDevice;
import org.dinopolis.gpstool.gpsinput.GPSTrack;
import org.dinopolis.gpstool.gpsinput.GPSTrackpoint;
import org.dinopolis.gpstool.gpsinput.GPSWaypoint;
import org.dinopolis.gpstool.gpsinput.garmin.FixedGPSGarminDataProcessor;
import org.dinopolis.gpstool.gpsinput.garmin.GarminProduct;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrack;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrackpointAdapter;
import org.dinopolis.util.ProgressListener;
import org.dinopolis.util.text.OneArgumentMessageFormat;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Version;

public class GarminExternalDevice extends ExternalDevice {

   private List<File> _receivedFiles;
   private boolean    _isCancelImport;

   public GarminExternalDevice() {
      buildNewFileNames = false;
      VelocityService.init();
   }

   @Override
   public IRunnableWithProgress createImportRunnable(final String portName, final List<File> receivedFiles) {
      _receivedFiles = receivedFiles;

      return new IRunnableWithProgress() {

         // ----------------------------------------------------------------------
         /**
          * Adds some important values to the velocity context (e.g. date, ...).
          *
          * @param context
          *           the velocity context holding all the data
          * @param productInfo
          *           infos about the Garmin device
          */
         private void addValuesToContext(final VelocityContext context, final GarminProduct productInfo) {

            final DecimalFormat double6formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
            double6formatter.applyPattern("0.0000000"); //$NON-NLS-1$
            final DecimalFormat int_formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
            int_formatter.applyPattern("000000"); //$NON-NLS-1$
            final DecimalFormat double2formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
            double2formatter.applyPattern("0.00"); //$NON-NLS-1$
            final OneArgumentMessageFormat string_formatter = new OneArgumentMessageFormat("{0}", Locale.US); //$NON-NLS-1$
            final SimpleDateFormat dateFormat = new SimpleDateFormat();
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
            context.put("dateformatter", dateFormat); //$NON-NLS-1$
            context.put("double6formatter", double6formatter); //$NON-NLS-1$
            context.put("intformatter", int_formatter); //$NON-NLS-1$
            context.put("stringformatter", string_formatter); //$NON-NLS-1$
            context.put("double2formatter", double2formatter); //$NON-NLS-1$

            // current time, date
            final Calendar now = Calendar.getInstance();
            final Date creationDate = now.getTime();
            context.put("creation_date", creationDate); //$NON-NLS-1$

            // author
            context.put("author", System.getProperty(DataTransferWizard.SYSPROPERTY_IMPORT_PERSON, "MyTourbook")); //$NON-NLS-1$ //$NON-NLS-2$

            // device infos
            final String productName = productInfo.getProductName();
            context.put("devicename", productName.substring(0, productName.indexOf(' '))); //$NON-NLS-1$
            context.put("productid", UI.EMPTY_STRING + productInfo.getProductId()); //$NON-NLS-1$
            context.put("devicemajorversion", UI.EMPTY_STRING + (productInfo.getProductSoftware() / 100)); //$NON-NLS-1$
            context.put("deviceminorversion", UI.EMPTY_STRING + (productInfo.getProductSoftware() % 100)); //$NON-NLS-1$

            // Version
            String pluginmajorversion = "0"; //$NON-NLS-1$
            String pluginminorversion = "0"; //$NON-NLS-1$
            final Version version = Activator.getDefault().getVersion();
            if (version != null) {
               pluginmajorversion = UI.EMPTY_STRING + version.getMajor();
               pluginminorversion = UI.EMPTY_STRING + version.getMinor();
            }
            context.put("pluginmajorversion", pluginmajorversion); //$NON-NLS-1$
            context.put("pluginminorversion", pluginminorversion); //$NON-NLS-1$

            // extent of waypoint, routes and tracks:
            double min_latitude = 90.0;
            double min_longitude = 180.0;
            double max_latitude = -90.0;
            double max_longitude = -180.0;

            final List<?> routes = (List<?>) context.get("routes"); //$NON-NLS-1$
            if (routes != null) {
               final Iterator<?> route_iterator = routes.iterator();
               while (route_iterator.hasNext()) {
                  final GPSRoute route = (GPSRoute) route_iterator.next();
                  min_longitude = route.getMinLongitude();
                  max_longitude = route.getMaxLongitude();
                  min_latitude = route.getMinLatitude();
                  max_latitude = route.getMaxLatitude();
               }
            }

            final List<?> tracks = (List<?>) context.get("tracks"); //$NON-NLS-1$
            if (tracks != null) {
               final Iterator<?> track_iterator = tracks.iterator();
               while (track_iterator.hasNext()) {
                  final GPSTrack track = (GPSTrack) track_iterator.next();
                  min_longitude = Math.min(min_longitude, track.getMinLongitude());
                  max_longitude = Math.max(max_longitude, track.getMaxLongitude());
                  min_latitude = Math.min(min_latitude, track.getMinLatitude());
                  max_latitude = Math.max(max_latitude, track.getMaxLatitude());
               }
            }
            final List<?> waypoints = (List<?>) context.get("waypoints"); //$NON-NLS-1$
            if (waypoints != null) {
               final Iterator<?> waypoint_iterator = waypoints.iterator();
               while (waypoint_iterator.hasNext()) {
                  final GPSWaypoint waypoint = (GPSWaypoint) waypoint_iterator.next();
                  min_longitude = Math.min(min_longitude, waypoint.getLongitude());
                  max_longitude = Math.max(max_longitude, waypoint.getLongitude());
                  min_latitude = Math.min(min_latitude, waypoint.getLatitude());
                  max_latitude = Math.max(max_latitude, waypoint.getLatitude());
               }
            }
            context.put("min_latitude", new Double(min_latitude)); //$NON-NLS-1$
            context.put("min_longitude", new Double(min_longitude)); //$NON-NLS-1$
            context.put("max_latitude", new Double(max_latitude)); //$NON-NLS-1$
            context.put("max_longitude", new Double(max_longitude)); //$NON-NLS-1$

            Date starttime = null;
            Date endtime = null;
            int heartNum = 0;
            long heartSum = 0;
            int cadNum = 0;
            long cadSum = 0;
            short maximumheartrate = 0;
            double totaldistance = 0;

            for (final Object name2 : tracks) {
               final GPSTrack track = (GPSTrack) name2;
               for (final Iterator<?> wpIter = track.getWaypoints().iterator(); wpIter.hasNext();) {
                  final GPSTrackpoint wp = (GPSTrackpoint) wpIter.next();

                  // starttime, totaltime
                  if (wp.getDate() != null) {
                     if (starttime == null) {
                        starttime = wp.getDate();
                     }
                     endtime = wp.getDate();
                  }
                  if (wp instanceof GarminTrackpointAdapter) {
                     final GarminTrackpointAdapter gta = (GarminTrackpointAdapter) wp;

                     // averageheartrate, maximumheartrate
                     if (gta.hasValidHeartrate()) {
                        heartSum += gta.getHeartrate();
                        heartNum++;
                        if (gta.getHeartrate() > maximumheartrate) {
                           maximumheartrate = gta.getHeartrate();
                        }
                     }

                     // averagecadence
                     if (gta.hasValidCadence()) {
                        cadSum += gta.getCadence();
                        cadNum++;
                     }

                     // totaldistance
                     if (gta.hasValidDistance()) {
                        totaldistance = gta.getDistance();
                     }
                  }
               }
            }

            if (starttime != null) {
               context.put("starttime", starttime); //$NON-NLS-1$
            } else {
               context.put("starttime", creationDate); //$NON-NLS-1$
            }

            if (starttime != null && endtime != null) {
               context.put("totaltime", ((double) endtime.getTime() - starttime.getTime()) / 1000); //$NON-NLS-1$
            } else {
               context.put("totaltime", (double) 0); //$NON-NLS-1$
            }

            context.put("totaldistance", totaldistance); //$NON-NLS-1$

            if (maximumheartrate != 0) {
               context.put("maximumheartrate", maximumheartrate); //$NON-NLS-1$
            }
            if (heartNum != 0) {
               context.put("averageheartrate", heartSum / heartNum); //$NON-NLS-1$
            }
            if (cadNum != 0) {
               context.put("averagecadence", cadSum / cadNum); //$NON-NLS-1$
            }
         }

         /**
          * If in the tracks date or altitude values is missing, these are copied from activeLog.
          *
          * @param monitor
          * @param activeLog
          * @param tracks
          */
         @SuppressWarnings("unchecked")
         private void mergeActiveLog(final GarminTrack activeLog, final List<GarminTrack> tracks) {

            final Map<ListIterator<GPSTrackpoint>, GPSTrackpoint> destinationTracks = new HashMap<>();
            for (final GarminTrack track : tracks) {
               final ListIterator<GPSTrackpoint> lIter = ((List<GPSTrackpoint>) track.getWaypoints()).listIterator();
               while (lIter.hasNext()) {
                  final GPSTrackpoint tp = lIter.next();
                  if (tp.getDate() == null || !tp.hasValidAltitude()) {
                     destinationTracks.put(lIter, tp);
                     break;
                  }
               }
            }

            if (destinationTracks.size() > 0) {

               for (final Object obj : activeLog.getWaypoints()) {
                  if (obj instanceof GPSTrackpoint) {
                     final GPSTrackpoint srcTp = (GPSTrackpoint) obj;

                     for (final Entry<ListIterator<GPSTrackpoint>, GPSTrackpoint> entry : destinationTracks.entrySet()) {
                        final GPSTrackpoint destTP = entry.getValue();

                        if (srcTp.getLongitude() == destTP.getLongitude()
                              && srcTp.getLatitude() == destTP.getLatitude()) {
                           final ListIterator<GPSTrackpoint> destIter = entry.getKey();
                           if ((!destTP.hasValidAltitude()) && srcTp.hasValidAltitude()) {
                              destTP.setAltitude(srcTp.getAltitude());
                           }
                           if (destTP.getDate() == null && srcTp.getDate() != null) {
                              destTP.setDate(srcTp.getDate());
                           }
                           while (destIter.hasNext()) {
                              final GPSTrackpoint tp = destIter.next();
                              if (tp.getDate() == null || !tp.hasValidAltitude()) {
                                 entry.setValue(tp);
                                 break;
                              }
                           }
                        }
                     }

                  }
               }
            }
         }

         @Override
         @SuppressWarnings("unchecked")
         public void run(final IProgressMonitor monitor) {
            _isCancelImport = false;
            final Thread currentThread = Thread.currentThread();

            final Hashtable<String, Comparable<?>> environment = new Hashtable<>();
            environment.put(GPSSerialDevice.PORT_NAME_KEY, portName);
            environment.put(GPSSerialDevice.PORT_SPEED_KEY, 9600);

            // handling of monitor.isCanceled()
            final Thread cancelObserver = new Thread(new Runnable() {

//					@Override
               @Override
               public void run() {
                  while (!monitor.isCanceled()) {
                     try {
                        Thread.sleep(100);
                     } catch (final InterruptedException ex) {
                        return;
                     }
                  }

                  _isCancelImport = true;

                  // we have to be a little bit insistent because
                  // InterruptedException is catched at GPSGarminDataProcessor.putPacket(GarminPacket, long)
                  // and request is repeated 5 times (MAX_TRIES)
                  while (currentThread.isAlive()) {
                     currentThread.interrupt();
                     try {
                        Thread.sleep(10);
                     } catch (final InterruptedException ex) {}
                  }
               }

            }, "GarminCancelObserver"); //$NON-NLS-1$
            cancelObserver.start();

            try {

               // 1. read data from garmin device
               final FixedGPSGarminDataProcessor garminDataProcessor = new FixedGPSGarminDataProcessor();
               final GPSSerialDevice serialDevice = new GPSSerialDevice();
               serialDevice.init(environment);
               garminDataProcessor.setGPSDevice(serialDevice);
               garminDataProcessor.open();

               final String monitorDevInfo;
               final String[] gpsInfo = garminDataProcessor.getGPSInfo();
               if (gpsInfo != null && gpsInfo.length > 0) {
                  monitorDevInfo = Messages.Garmin_Transfer_msg + gpsInfo[0];
               } else {
                  monitorDevInfo = Messages.Garmin_unknown_device;
               }

               final GarminProduct productInfo = garminDataProcessor.getGarminProductInfo(2000L);

               garminDataProcessor.addProgressListener(new ProgressListener() {
                  private int done;

//						@Override
                  @Override
                  public void actionEnd(final String action_id) {}

//						@Override
                  @Override
                  public void actionProgress(final String action_id, final int current_value) {
                     monitor.worked(current_value - done);
                     done = current_value;
                  }

//						@Override
                  @Override
                  public void actionStart(final String action_id, final int min_value, final int max_value) {
                     done = 0;
                     monitor.beginTask(monitorDevInfo, max_value);
                  }

               });

               final List<GarminTrack> tracks = garminDataProcessor.getTracks();

               garminDataProcessor.close();

               // 2. merge received tracks with "ACTIVE LOG"-track
               if (tracks != null) {

                  // separate "ACTIVE LOG"
                  GarminTrack srcTrack = null;
                  final List<GarminTrack> destTracks = new ArrayList<>();
                  for (final GarminTrack track : tracks) {
                     if (track.getIdentification().equals("ACTIVE LOG")) { //$NON-NLS-1$
                        srcTrack = track;
                     } else {
                        destTracks.add(track);
                     }
                  }

                  // merge
                  if (srcTrack != null && destTracks.size() > 0) {
                     mergeActiveLog(srcTrack, destTracks);
                  }

                  // save files
                  for (final GarminTrack track : destTracks) {

                     // compute distance if not supported by device
                     GarminTrackpointAdapter prevGta = null;
                     for (final Iterator<GPSTrackpoint> wpIter = track.getWaypoints().iterator(); wpIter
                           .hasNext();) {
                        final GPSTrackpoint wp = wpIter.next();
                        if (wp instanceof GarminTrackpointAdapter) {
                           final GarminTrackpointAdapter gta = (GarminTrackpointAdapter) wp;
                           if (!gta.hasValidDistance()) {
                              if (prevGta != null) {
                                 gta.setDistance(prevGta.getDistance()
                                       + MtMath.distanceVincenty(prevGta.getLatitude(),
                                             prevGta.getLongitude(),
                                             gta.getLatitude(),
                                             gta.getLongitude()));
                              } else {
                                 gta.setDistance(0);
                              }
                           }
                           prevGta = gta;
                        }
                     }

                     // create context
                     final VelocityContext context = new VelocityContext();

                     // prepare context
                     final ArrayList<GarminTrack> tList = new ArrayList<>();
                     tList.add(track);
                     context.put("tracks", tList); //$NON-NLS-1$
                     context.put("printtracks", new Boolean(true)); //$NON-NLS-1$
                     context.put("printwaypoints", new Boolean(false)); //$NON-NLS-1$
                     context.put("printroutes", new Boolean(false)); //$NON-NLS-1$

                     final File receivedFile = new File(RawDataManager.getTempDir()
                           + File.separator
                           + track.getIdentification()
                           + ".tcx"); //$NON-NLS-1$

                     final Reader reader = new InputStreamReader(this.getClass()
                           .getResourceAsStream("/gpx-template/tcx-2.0.vm")); //$NON-NLS-1$
                     final Writer writer = new FileWriter(receivedFile);

                     addValuesToContext(context, productInfo);

                     Velocity.evaluate(context, writer, "MyTourbook", reader); //$NON-NLS-1$
                     writer.close();

                     _receivedFiles.add(receivedFile);
                  }
               }

            } catch (final Exception ex) {
               if (!_isCancelImport) {
                  final Display display = PlatformUI.getWorkbench().getDisplay();
                  Runnable runnable;

                  // "Garmin device does not respond" -> org.dinopolis.gpstool.gpsinput.garmin.GPSGarminDataProcessor.open()
                  if (ex instanceof GPSException && ex.getMessage().equals("Garmin device does not respond!")) { //$NON-NLS-1$
                     runnable = new Runnable() {
//								@Override
                        @Override
                        public void run() {
                           MessageDialog.openError(display.getActiveShell(),
                                 Messages.Garmin_data_transfer_error,
                                 Messages.Garmin_no_connection);
                        }
                     };
                  } else {
                     runnable = new Runnable() {
//								@Override
                        @Override
                        public void run() {
                           ex.printStackTrace();
                           ErrorDialog.openError(display.getActiveShell(),
                                 Messages.Garmin_data_transfer_error,
                                 Messages.Garmin_error_receiving_data,
                                 new Status(Status.ERROR,
                                       Activator.PLUGIN_ID,
                                       Messages.Garmin_commuication_error,
                                       ex));
                        }
                     };
                  }
                  display.syncExec(runnable);
               }

            } finally {
               cancelObserver.interrupt();
            }

         }
      };
   }

   @Override
   public boolean isImportCanceled() {
      return _isCancelImport;
   }

}
