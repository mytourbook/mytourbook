/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo.manager;

import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.tourbook.photo.Messages;
import net.tourbook.photo.gallery.GalleryMTItem;

import org.eclipse.swt.widgets.Display;
import org.imgscalr.Scalr;

public class PhotoManager {

	public static final int										THUMBNAIL_DEFAULT_SIZE	= 160;

// SET_FORMATTING_OFF

	/**
	 * Contains image sizes for different image qualities. 1000er are slow when painted therefor a
	 * medium size is choosen.
	 */
	public static int[]											IMAGE_SIZES = { THUMBNAIL_DEFAULT_SIZE, 600, Integer.MAX_VALUE };

// SET_FORMATTING_ON

	/*
	 * image quality is the index in IMAGE_SIZE
	 */
	public static int											IMAGE_QUALITY_THUMB_160	= 0;
	public static int											IMAGE_QUALITY_HQ_1000	= 1;
	public static int											IMAGE_QUALITY_ORIGINAL	= 2;

	/**
	 * Used to indicate that the scaling implementation should decide which method to use in order
	 * to get the best looking scaled image in the least amount of time.
	 * <p/>
	 * The scaling algorithm will use the {@link Scalr#THRESHOLD_QUALITY_BALANCED} or
	 * {@link Scalr#THRESHOLD_BALANCED_SPEED} thresholds as cut-offs to decide between selecting the
	 * <code>QUALITY</code>, <code>BALANCED</code> or <code>SPEED</code> scaling algorithms.
	 * <p/>
	 * <b>AUTOMATIC</b><br>
	 * By default the thresholds chosen will give nearly the best looking result in the fastest
	 * amount of time. We intend this method to work for 80% of people looking to scale an image
	 * quickly and get a good looking result.
	 * <p>
	 * <b> SPEED </b><br>
	 * Used to indicate that the scaling implementation should scale as fast as possible and return
	 * a result. For smaller images (800px in size) this can result in noticeable aliasing but it
	 * can be a few magnitudes times faster than using the QUALITY method.
	 * <p>
	 * <b> BALANCED </b><br>
	 * Used to indicate that the scaling implementation should use a scaling operation balanced
	 * between SPEED and QUALITY. Sometimes SPEED looks too low quality to be useful (e.g. text can
	 * become unreadable when scaled using SPEED) but using QUALITY mode will increase the
	 * processing time too much. This mode provides a "better than SPEED" quality in a
	 * "less than QUALITY" amount of time.
	 * <p>
	 * <b> QUALITY </b><br>
	 * Used to indicate that the scaling implementation should do everything it can to create as
	 * nice of a result as possible. This approach is most important for smaller pictures (800px or
	 * smaller) and less important for larger pictures as the difference between this method and the
	 * SPEED method become less and less noticeable as the source-image size increases. Using the
	 * AUTOMATIC method will automatically prefer the QUALITY method when scaling an image down
	 * below 800px in size.
	 * <p>
	 * <b> ULTRA_QUALITY </b><br>
	 * Used to indicate that the scaling implementation should go above and beyond the work done by
	 * {@link Method#QUALITY} to make the image look exceptionally good at the cost of more
	 * processing time. This is especially evident when generating thumbnails of images that look
	 * jagged with some of the other {@link Method}s (even {@link Method#QUALITY}).
	 * <p>
	 */
	public static String[]										SCALING_QUALITY_TEXT	= {
			Messages.Scaling_Quality_Automatic,
			Messages.Scaling_Quality_Speed,
			Messages.Scaling_Quality_Balanced,
			Messages.Scaling_Quality_Quality,
			Messages.Scaling_Quality_UltraQuality										};

	public static Scalr.Method[]								SCALING_QUALITY_ID		= {
			Scalr.Method.AUTOMATIC,
			Scalr.Method.SPEED,
			Scalr.Method.BALANCED,
			Scalr.Method.QUALITY,
			Scalr.Method.ULTRA_QUALITY													};

	private static Display										_display;

	private static ThreadPoolExecutor							_executorService;

	/**
	 * (H)igh(Q)ality executor is running only in one thread because multiple threads are slowing
	 * down the process loading of fullsize images.
	 */
	private static ThreadPoolExecutor							_executorServiceHQ;

	private static org.imgscalr.Scalr.Method					_resizeQuality;

	private static final LinkedBlockingDeque<PhotoImageLoader>	_waitingQueue			= new LinkedBlockingDeque<PhotoImageLoader>();
	private static final LinkedBlockingDeque<PhotoImageLoader>	_waitingQueueHQ			= new LinkedBlockingDeque<PhotoImageLoader>();

	static {

		_display = Display.getDefault();

		int processors = Runtime.getRuntime().availableProcessors() - 1;
		processors = Math.max(processors, 1);

//		processors = 1;

		System.out.println("Number of processors: " + processors); //$NON-NLS-1$

		final ThreadFactory threadFactory = new ThreadFactory() {

			private int	_threadNumber	= 0;

			public Thread newThread(final Runnable r) {

				final String threadName = "Photo-Image-Loader-" + _threadNumber++; //$NON-NLS-1$

				final Thread thread = new Thread(r, threadName);

				thread.setPriority(Thread.MIN_PRIORITY);
				thread.setDaemon(true);

				return thread;
			}
		};

		_executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(processors, threadFactory);

		_executorServiceHQ = new ThreadPoolExecutor(
				1,
				1,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());

	}

	private static void clearQueue(	final LinkedBlockingDeque<PhotoImageLoader> waitingQueue,
									final ThreadPoolExecutor executorService) {

		final Object[] queuedPhotoImageLoaderItems = waitingQueue.toArray();

		/*
		 * terminate all submitted tasks, the executor shutdownNow() creates
		 * RejectedExecutionException when reusing the executor, I found no other way how to stop
		 * the submitted tasks
		 */
		final BlockingQueue<Runnable> taskQueue = executorService.getQueue();
		for (final Runnable runnable : taskQueue) {
			final FutureTask<?> task = (FutureTask<?>) runnable;
			task.cancel(false);
		}

		waitingQueue.clear();

		// reset loading state for not loaded images
		for (final Object object : queuedPhotoImageLoaderItems) {

			if (object == null) {
				// it's possible that a queue item has already been removed
				continue;
			}

			final PhotoImageLoader photoImageLoaderItem = (PhotoImageLoader) object;

			photoImageLoaderItem.photo.setLoadingState(PhotoLoadingState.UNDEFINED, photoImageLoaderItem.imageQuality);
		}
	}

	public static Scalr.Method getResizeQuality() {
		return _resizeQuality;
	}

	public static void putImageInHQLoadingQueue(final GalleryMTItem galleryItem,
												final Photo photo,
												final int imageQuality,
												final ILoadCallBack loadCallBack) {
		// set state
		photo.setLoadingState(PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE, imageQuality);

		// set HQ image loading item into the waiting queue
		_waitingQueueHQ.add(new PhotoImageLoader(_display, galleryItem, photo, imageQuality, loadCallBack));

		final Runnable executorTask = new Runnable() {
			public void run() {

				// get last added loader itme
				final PhotoImageLoader loadingItem = _waitingQueueHQ.pollLast();

				if (loadingItem != null) {
					loadingItem.loadImageHQ();
				}
			}
		};
		_executorServiceHQ.submit(executorTask);
	}

	public static void putImageInLoadingQueue(	final GalleryMTItem galleryItem,
												final Photo photo,
												final int imageQuality,
												final ILoadCallBack imageLoadCallback) {

		// set state
		photo.setLoadingState(PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE, imageQuality);

		// put image loading item into the waiting queue
		_waitingQueue.add(new PhotoImageLoader(_display, galleryItem, photo, imageQuality, imageLoadCallback));

		final Runnable executorTask = new Runnable() {
			public void run() {

				// get last added loader itme
				final PhotoImageLoader loadingItem = _waitingQueue.pollLast();

				if (loadingItem != null) {
					loadingItem.loadImage();
				}
			}
		};
		_executorService.submit(executorTask);
	}

	public static void setResizeQuality(final String requestedResizeQualityId) {

		// set default value
		_resizeQuality = Scalr.Method.SPEED;

		for (final Scalr.Method quality : SCALING_QUALITY_ID) {
			if (quality.name().equals(requestedResizeQualityId)) {
				_resizeQuality = quality;
				break;
			}
		}
	}

	/**
	 * Remove all items in the image loading queue.
	 */
	public synchronized static void stopImageLoading() {

		clearQueue(_waitingQueue, _executorService);
		clearQueue(_waitingQueueHQ, _executorServiceHQ);
	}
}
