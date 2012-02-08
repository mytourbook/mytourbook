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

public class PhotoManager {

	public static final int											THUMBNAIL_DEFAULT_SIZE	= 160;

// SET_FORMATTING_OFF
	
	public static final int[]										THUMBNAIL_SIZES = new int[]
			{
				30, 40, 50, 60, 70, 80, 90, 100, 120, 140, THUMBNAIL_DEFAULT_SIZE, 200, 250, 300, 400, 500, 600
			};
	
	public static int[]												IMAGE_SIZE = { THUMBNAIL_DEFAULT_SIZE, 600, 999999 };
	
// SET_FORMATTING_ON

	public static int												IMAGE_QUALITY_THUMB_160	= 0;
	public static int												IMAGE_QUALITY_600		= 1;
	public static int												IMAGE_QUALITY_ORIGINAL	= 2;
	/**
	 * This must be the max image quality which is also used as array.length()
	 */
	public static int												MAX_IMAGE_QUALITY		= 2;

	private static ThreadPoolExecutor								_executorService;

	private static final LinkedBlockingDeque<PhotoImageLoaderItem>	_waitingQueue			= new LinkedBlockingDeque<PhotoImageLoaderItem>();

	static {

		final int cpuCores = 4;

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

		_executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(cpuCores, threadFactory);

		// setup image scaler, set number of max threads
//		System.setProperty("imgscalr.async.threadCount", Integer.toString(cpuCores));
//		AsyncScalr.getService();
	}

	/**
	 * Removes LIFO item from the queue and loads the image
	 */
	private static final class LoadingTask implements Runnable {

		@Override
		public void run() {

			final PhotoImageLoaderItem loadingItem = _waitingQueue.pollLast();

			if (loadingItem != null) {
				loadingItem.loadImage();
			}
		}
	}

// Original in org.sharemedia.services.impl.mediadownload.MediaDownload
//
//	synchronized public void load(final IMedia m, final int imageQuality, final ILoadCallBack callback) {
//
//		final LoadItem newItem = new LoadItem();
//		newItem.setMedia(m);
//		newItem.setDefinition(imageQuality);
//
//		if (logger.isDebugEnabled()) {
//			logger.debug("Media " + m + " def " + imageQuality);
//		}
//
//		LoadItem queuedItem = null;
//
//		if ((queuedItem = getLoadItem(loading, newItem)) != null) {
//
//			addCallBack(queuedItem, callback);
//			if (logger.isDebugEnabled()) {
//				logger.debug("Media is loading, adding callback");
//			}
//
//		} else if ((queuedItem = getLoadItem(waitList, newItem)) != null) {
//
//			addCallBack(queuedItem, callback);
//			waitList.remove(queuedItem);
//			waitList.push(queuedItem);
//			if (logger.isDebugEnabled()) {
//				logger.debug("Media is waiting, adding callback and queue on top");
//			}
//
//		} else {
//
//			newItem.addCallback(callback);
//			waitList.push(newItem);
//			if (logger.isDebugEnabled()) {
//				logger.debug("Media not queued, adding to queue");
//			}
//		}
//
//		if (nbThread < maxThreads) {
//			if (logger.isDebugEnabled()) {
//				logger.debug("Starting Thread");
//			}
//			nbThread++;
//			new LoadThead().start();
//		}
//	}

	public static void loadImage(final Photo photo, final int imageQuality, final ILoadCallBack imageLoadCallback) {

//		final Image image = null;

		/*
		 * check if image is available in the thumbnail store
		 */
//		final Image image = _thumbnailStore.getImage(photo, imageQuality);
//		if (image != null) {
//			loadCallBack.imageIsLoaded();
//			return;
//		}

		/*
		 * check if original image should be loaded
		 */
//		if (imageQuality == PhotoImageCache.IMAGE_QUALITY_ORIGINAL) {
//
//		}
		final PhotoImageLoaderItem loadingCallback = new PhotoImageLoaderItem(photo, imageQuality, imageLoadCallback);

		photo.getLoadingState()[imageQuality] = PhotoLoadingState.IMAGE_IS_BEING_LOADED;

		/*
		 * removes existing loader item if exists so that it can be queued on top
		 */
//		_waitingQueue.remove(loadingCallback);

		_waitingQueue.add(loadingCallback);

		_executorService.submit(new LoadingTask());
	}

	/**
	 * Remove all items in the image loading queue.
	 */
	public synchronized static void stopImageLoading() {

//		final int queueSize = _waitingQueue.size();
//
//		if (queueSize == 0) {
//			return;
//		}

		final Object[] photosInQueue = _waitingQueue.toArray();

		/*
		 * terminate all submitted tasks, the executor shutdownNow() creates
		 * RejectedExecutionException when reusing the executor, I found no other way how to stop
		 * the submitted tasks
		 */
		final BlockingQueue<Runnable> taskQueue = _executorService.getQueue();
		for (final Runnable runnable : taskQueue) {
			final FutureTask<?> task = (FutureTask<?>) runnable;
			task.cancel(true);
		}

		_waitingQueue.clear();

		// reset loading state for not loaded images
		for (final Object object : photosInQueue) {

			if (object == null) {
				// item can already be removed
				continue;
			}

			final PhotoImageLoaderItem photoImageLoaderItem = (PhotoImageLoaderItem) object;
			photoImageLoaderItem.photo.getLoadingState()[photoImageLoaderItem.imageQuality] = PhotoLoadingState.UNDEFINED;
		}
	}

}
