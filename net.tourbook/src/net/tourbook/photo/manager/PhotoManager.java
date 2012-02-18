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

import net.tourbook.photo.gallery.GalleryMTItem;

public class PhotoManager {

	public static final int											THUMBNAIL_DEFAULT_SIZE	= 160;
//	public static final int											THUMBNAIL_DEFAULT_SIZE	= 50;

// SET_FORMATTING_OFF
	
	public static final int[]										THUMBNAIL_SIZES = new int[]
			{
				50, 60, 70, 80, 90, 100, 120, 140, THUMBNAIL_DEFAULT_SIZE, 200, 250, 300, 400, 500, 600
//				THUMBNAIL_DEFAULT_SIZE, 60, 70, 80, 90, 100, 120, 140, 160, 200, 250, 300, 400, 500, 600
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

		int processors = Runtime.getRuntime().availableProcessors() - 2;
		processors = Math.max(processors, 1);

		processors = 4;

		System.out.println("Number of processors: " + processors);

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

		// setup image scaler, set number of max threads
//		System.setProperty("imgscalr.async.threadCount", Integer.toString(cpuCores));
//		AsyncScalr.getService();
	}

	public static void putImageInLoadingQueue(	final GalleryMTItem galleryItem,
												final Photo photo,
												final int imageQuality,
												final ILoadCallBack imageLoadCallback) {

		// set state
		photo.setLoadingState(PhotoLoadingState.IMAGE_IS_IN_LOADING_QUEUE, imageQuality);

		// add loading item into the waiting queue
		_waitingQueue.add(new PhotoImageLoaderItem(//
				galleryItem,
				photo,
				imageQuality,
				imageLoadCallback));

		_executorService.submit(new Runnable() {
			public void run() {

				// get last added loader itme
				final PhotoImageLoaderItem loadingItem = _waitingQueue.pollLast();

				if (loadingItem != null) {
					loadingItem.loadImage();
				}
			}
		});
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

	/**
	 * Remove all items in the image loading queue.
	 */
	public synchronized static void stopImageLoading() {

		final Object[] queuedPhotoImageLoaderItems = _waitingQueue.toArray();

		/*
		 * terminate all submitted tasks, the executor shutdownNow() creates
		 * RejectedExecutionException when reusing the executor, I found no other way how to stop
		 * the submitted tasks
		 */
		final BlockingQueue<Runnable> taskQueue = _executorService.getQueue();
		for (final Runnable runnable : taskQueue) {
			final FutureTask<?> task = (FutureTask<?>) runnable;
			task.cancel(false);
		}

		_waitingQueue.clear();

		// reset loading state for not loaded images
		for (final Object object : queuedPhotoImageLoaderItems) {

			if (object == null) {
				// queue item can already be removed
				continue;
			}

			final PhotoImageLoaderItem photoImageLoaderItem = (PhotoImageLoaderItem) object;

			photoImageLoaderItem.photo.setLoadingState(PhotoLoadingState.UNDEFINED, photoImageLoaderItem.imageQuality);
		}
	}

}
