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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReentrantLock;

public class PhotoManager {

	public static final int												THUMBNAIL_SIZE			= 160;

	// SET_FORMATTING_OFF
	public int[]														IMAGE_WIDTH	 = { THUMBNAIL_SIZE, 640, 800, 1024 };
	public int[]														IMAGE_HEIGHT = { THUMBNAIL_SIZE, 480, 600, 768 };
	// SET_FORMATTING_ON

	public static int													IMAGE_QUALITY_THUMB_160	= 0;													// 160x160 max
	public static int													IMAGE_QUALITY_LOW_640	= 1;													// 640x480 max
//	public static int													IMAGE_STD		= 2;										// 800x600 max
//	public static int													IMAGE_HIGH		= 3;										// 1024x768 max
	public static int													IMAGE_QUALITY_ORIGINAL	= 4;													// Full size

	/**
	 * This must be the max image quality which is also used as an index in an array
	 */
	public static int													MAX_IMAGE_QUALITY		= 4;

	private static ExecutorService										_executorService;

	private static PhotoManager											_instance;

	private static final ThumbnailStore									_thumbnailStore			= ThumbnailStore
																										.getInstance();
	private static final PhotoImageCache								_imageCache				= PhotoImageCache
																										.getInstance();

	private static final ReentrantLock									INSTANCE_LOCK			= new ReentrantLock();

	private static final ConcurrentLinkedQueue<PhotoImageLoaderItem>	_waitingQueue			= new ConcurrentLinkedQueue<PhotoImageLoaderItem>();

	private PhotoManager() {}

	public static PhotoManager getInstance() {

		final int cpuCores = 2;

		if (_instance == null) {

			INSTANCE_LOCK.lock();
			{
				try {

					if (_instance != null) {
						return _instance;
					}

					_instance = new PhotoManager();

					final ThreadFactory threadFactory = new ThreadFactory() {

						private int	_threadNumber	= 0;

						public Thread newThread(final Runnable r) {

							final String threadName = "Photo-Loader-" + _threadNumber++; //$NON-NLS-1$

							final Thread thread = new Thread(r, threadName);

							thread.setPriority(Thread.MIN_PRIORITY);
							thread.setDaemon(true);

							return thread;
						}
					};

					_executorService = Executors.newFixedThreadPool(cpuCores, threadFactory);

				} finally {
					INSTANCE_LOCK.unlock();
				}
			}

			// setup image scaler
			System.setProperty("imgscalr.async.threadCount", Integer.toString(cpuCores));
//			AsyncScalr.getService();
		}

		return _instance;
	}

	static ConcurrentLinkedQueue<PhotoImageLoaderItem> getWaitingQueue() {
		return _waitingQueue;
	}

	public void loadImage(final Photo photo, final int[] imageQualities, final ILoadCallBack[] imageCallback) {

		for (int imageIndex = 0; imageIndex < imageCallback.length; imageIndex++) {

			final int imageQuality = imageQualities[imageIndex];
			final ILoadCallBack loadCallBack = imageCallback[imageIndex];

			if (imageQuality != -1 && loadCallBack != null) {
				loadImage_10(photo, imageQuality, loadCallBack);
			}
		}
	}

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

	private void loadImage_10(final Photo photo, final int imageQuality, final ILoadCallBack loadCallBack) {

//		final ThreadPoolExecutor exec = (ThreadPoolExecutor) _executorService;

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
		final PhotoImageLoaderItem loadingTask = new PhotoImageLoaderItem(photo, imageQuality, loadCallBack);

		_waitingQueue.add(loadingTask);

		photo.getLoadingState()[imageQuality] = PhotoLoadingState.IMAGE_IS_BEING_LOADED;

		final Future<?> taskFuture = _executorService.submit(loadingTask);

		loadingTask.future = taskFuture;
	}

	/**
	 * Remove all items in the image loading queue.
	 */
	public synchronized void stopLoadingImages() {

		final int queueSize = _waitingQueue.size();

		if (queueSize == 0) {
			return;
		}

		final PhotoImageLoaderItem[] photosInQueue = _waitingQueue.toArray(new PhotoImageLoaderItem[queueSize]);

		_waitingQueue.clear();

		// stop executer tasks
		for (final PhotoImageLoaderItem photoImageLoaderItem : photosInQueue) {

			if (photoImageLoaderItem == null) {
				// item can already be removed
				continue;
			}

			// stop downloading task in the executer
			photoImageLoaderItem.future.cancel(true);

			// reset state
			photoImageLoaderItem.photo.getLoadingState()[photoImageLoaderItem.imageQuality] = PhotoLoadingState.UNDEFINED;
		}
	}
}
