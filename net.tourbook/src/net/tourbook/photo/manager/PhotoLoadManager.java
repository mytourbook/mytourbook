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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.photo.gallery.MT20.GalleryMT20Item;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.util.UI;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;

public class PhotoLoadManager {

	private static final IPreferenceStore						_prefStore;

//	public static final int										IMAGE_SIZE_THUMBNAIL		= 20;
	public static final int										IMAGE_SIZE_THUMBNAIL		= 160;
	public static final int										IMAGE_SIZE_LARGE_DEFAULT	= 600;

//	/**
//	 * Default image ratio between image width/height. It is the average between 4000x3000 (1.3333)
//	 * and 5184x3456 (1.5)
//	 */
//	public static final double									IMAGE_RATIO					= 15.0 / 10;									//1.41;

// SET_FORMATTING_OFF

	
	public static final int[]									HQ_IMAGE_SIZES					=
			{ 200, IMAGE_SIZE_LARGE_DEFAULT, 1000, 2000 };

// SET_FORMATTING_ON

//	/*
//	 * image quality is the index in HQ_IMAGE_SIZES
//	 */
//	public static int											IMAGE_QUALITY_EXIF_THUMB	= 0;
//	public static int											IMAGE_QUALITY_LARGE_IMAGE	= 1;
//	public static int											IMAGE_QUALITY_ORIGINAL		= 2;

	private static Display										_display;

	private static ThreadPoolExecutor							_executorExif;
	private static ThreadPoolExecutor							_executorThumb;

	/**
	 * (H)igh(Q)ality executor is running only in one thread because multiple threads are slowing
	 * down the process loading of fullsize images.
	 */
	private static ThreadPoolExecutor							_executorHQ;

	private static final LinkedBlockingDeque<PhotoExifLoader>	_waitingQueueExif			= new LinkedBlockingDeque<PhotoExifLoader>();
	private static final LinkedBlockingDeque<PhotoImageLoader>	_waitingQueueThumb			= new LinkedBlockingDeque<PhotoImageLoader>();
	private static final LinkedBlockingDeque<PhotoImageLoader>	_waitingQueueHQ				= new LinkedBlockingDeque<PhotoImageLoader>();

	public static final String									IMAGE_FRAMEWORK_SWT			= "swt";										//$NON-NLS-1$
	public static final String									IMAGE_FRAMEWORK_AWT			= "awt";										//$NON-NLS-1$

	private static String										_imageFramework;
	private static int											_hqImageSize;

	static {

		_display = Display.getDefault();

		_prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		_imageFramework = _prefStore.getString(ITourbookPreferences.PHOTO_VIEWER_IMAGE_FRAMEWORK);
		_hqImageSize = _prefStore.getInt(ITourbookPreferences.PHOTO_VIEWER_HQ_IMAGE_SIZE);

		final int availableProcessors = Runtime.getRuntime().availableProcessors();

		System.out.println(UI.timeStamp() + "Number of processors: " + availableProcessors); //$NON-NLS-1$

		int processors = availableProcessors - 0; // 1 processor for HQ loading
		processors = Math.max(processors, 1);

		final ThreadFactory threadFactoryExif = new ThreadFactory() {

			private int	_threadNumberExif	= 0;

			public Thread newThread(final Runnable r) {

				final String threadName = "ImageLoader-Exif-" + _threadNumberExif++; //$NON-NLS-1$

				final Thread thread = new Thread(r, threadName);

				thread.setPriority(Thread.MIN_PRIORITY);
				thread.setDaemon(true);

				return thread;
			}
		};

		final ThreadFactory threadFactoryThumb = new ThreadFactory() {

			private int	_threadNumber	= 0;

			public Thread newThread(final Runnable r) {

				final String threadName = "ImageLoader-Thumb-" + _threadNumber++; //$NON-NLS-1$

				final Thread thread = new Thread(r, threadName);

				thread.setPriority(Thread.MIN_PRIORITY);
				thread.setDaemon(true);

				return thread;
			}
		};

		final ThreadFactory threadFactoryHQ = new ThreadFactory() {

			private int	_threadNumber	= 0;

			public Thread newThread(final Runnable r) {

				final String threadName = "ImageLoader-HQ-" + _threadNumber++; //$NON-NLS-1$

				final Thread thread = new Thread(r, threadName);

				thread.setPriority(Thread.MIN_PRIORITY);
				thread.setDaemon(true);

				return thread;
			}
		};

		_executorExif = (ThreadPoolExecutor) Executors.newFixedThreadPool(processors, threadFactoryExif);
		_executorThumb = (ThreadPoolExecutor) Executors.newFixedThreadPool(processors, threadFactoryThumb);
		_executorHQ = (ThreadPoolExecutor) Executors.newFixedThreadPool(1, threadFactoryHQ);
	}

	public static void clearExifLoadingQueue() {
		clearWaitingQueue(_waitingQueueExif, _executorExif);
	}

	private static Object[] clearWaitingQueue(	final LinkedBlockingDeque<?> waitingQueue,
												final ThreadPoolExecutor executorService) {

		final Object[] waitingQueueItems = waitingQueue.toArray();

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

		return waitingQueueItems;
	}

	public static int getExifQueueSize() {
		return _waitingQueueExif.size();
	}

	/**
	 * @param hqImageSize
	 * @return Returns the index in the HQ image size array for the requested image size, default is
	 *         used when requested size is not available
	 */
	public static int getHQImageSizeIndex(final int hqImageSize) {

		int hqImageSizeIndex = -1;

		// try to get stored image size index
		for (int imageIndex = 0; imageIndex < HQ_IMAGE_SIZES.length; imageIndex++) {
			if (HQ_IMAGE_SIZES[imageIndex] == hqImageSize) {
				hqImageSizeIndex = imageIndex;
				break;
			}
		}
		if (hqImageSizeIndex == -1) {
			// get default size
			for (int imageIndex = 0; imageIndex < HQ_IMAGE_SIZES.length; imageIndex++) {
				if (HQ_IMAGE_SIZES[imageIndex] == IMAGE_SIZE_LARGE_DEFAULT) {
					hqImageSizeIndex = imageIndex;
					break;
				}
			}
		}

		return hqImageSizeIndex;
	}

	public static int getImageHQQueueSize() {
		return _waitingQueueHQ.size();
	}

	public static int getImageQueueSize() {
		return _waitingQueueThumb.size();
	}

	public static void putImageInExifLoadingQueue(final Photo photo, final ILoadCallBack imageLoadCallback) {

		// put image loading item into the waiting queue
		_waitingQueueExif.add(new PhotoExifLoader(photo, imageLoadCallback));

		final Runnable executorTask = new Runnable() {
			public void run() {

				// get last added loader itme
				final PhotoExifLoader loadingItem = _waitingQueueExif.pollLast();

				if (loadingItem != null) {
					loadingItem.loadExif();
				}
			}
		};
		_executorExif.submit(executorTask);
	}

	public static void putImageInHQLoadingQueue(final GalleryMT20Item galleryItem,
												final Photo photo,
												final ImageQuality imageQuality,
												final ILoadCallBack loadCallBack) {
		// set state
		photo.setLoadingState(PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE, imageQuality);

		// set HQ image loading item into the waiting queue
		_waitingQueueHQ.add(new PhotoImageLoader(
				_display,
				galleryItem,
				photo,
				imageQuality,
				_imageFramework,
				_hqImageSize,
				loadCallBack));

		final Runnable executorTask = new Runnable() {
			public void run() {

				// get last added loader itme
				final PhotoImageLoader loadingItem = _waitingQueueHQ.pollFirst();

				if (loadingItem != null) {
					loadingItem.loadImageHQ(_waitingQueueThumb, _waitingQueueExif);
				}
			}
		};
		_executorHQ.submit(executorTask);
	}

	public static void putImageInThumbLoadingQueue(	final GalleryMT20Item galleryItem,
													final Photo photo,
													final ImageQuality imageQuality,
													final ILoadCallBack imageLoadCallback) {

		// set state
		photo.setLoadingState(PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE, imageQuality);

		// put image loading item into the waiting queue
		_waitingQueueThumb.add(new PhotoImageLoader(
				_display,
				galleryItem,
				photo,
				imageQuality,
				_imageFramework,
				_hqImageSize,
				imageLoadCallback));

		final Runnable executorTask = new Runnable() {
			public void run() {

				// get last added loader itme
				final PhotoImageLoader loadingItem = _waitingQueueThumb.pollFirst();

				if (loadingItem != null) {
					loadingItem.loadImage(_waitingQueueExif);
				}
			}
		};
		_executorThumb.submit(executorTask);
	}

	private static void resetLoadingState(final Object[] waitingQueueItems) {

		// reset loading state for not loaded images
		for (final Object waitingQueueItem : waitingQueueItems) {

			if (waitingQueueItem == null) {
				// it's possible that a queue item has already been removed
				continue;
			}

			final PhotoImageLoader photoImageLoaderItem = (PhotoImageLoader) waitingQueueItem;

			photoImageLoaderItem._photo.setLoadingState(
					PhotoLoadingState.UNDEFINED,
					photoImageLoaderItem._requestedImageQuality);
		}
	}

	public static void setFromPrefStore(final String imageFramework, final int hqImageSize) {
		_imageFramework = imageFramework;
		_hqImageSize = hqImageSize;
	}

	/**
	 * Remove all items in the image loading queue.
	 * 
	 * @param isClearExifQueue
	 */
	public synchronized static void stopImageLoading(final boolean isClearExifQueue) {

		Object[] waitingQueueItems = clearWaitingQueue(_waitingQueueThumb, _executorThumb);
		resetLoadingState(waitingQueueItems);

		waitingQueueItems = clearWaitingQueue(_waitingQueueHQ, _executorHQ);
		resetLoadingState(waitingQueueItems);

		if (isClearExifQueue) {
			clearExifLoadingQueue();
		}
	}

}
