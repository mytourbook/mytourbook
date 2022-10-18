/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.renderer;

/**
 * Original class: org.oscim.renderer.bucket.RenderBuckets
 */
public class TourTrack_BucketManager {

   private TourTrack_Bucket _trackBucket_Painter;
   private TourTrack_Bucket _trackBucket_Worker;

   public TourTrack_BucketManager() {

   }

   /**
    * Cleanup only when buckets are not used by tile or bucket anymore!
    */
   public void clear() {

      setBucket_Painter(null);
      _trackBucket_Worker = null;
   }

   TourTrack_Bucket getBucket_Painter() {

      return _trackBucket_Painter;
   }

   TourTrack_Bucket getBucket_Worker() {

      TourTrack_Bucket trackBucket = null;

      if (_trackBucket_Worker != null) {

         trackBucket = _trackBucket_Worker;

         return trackBucket;
      }

      TourTrack_Bucket chainedBucked = _trackBucket_Painter;

      if (chainedBucked == null) {

         // insert new bucket at start
         chainedBucked = null;

      } else {

         if (_trackBucket_Worker != null) {
            chainedBucked = _trackBucket_Worker;
         }
      }

      if (trackBucket == null) {

         trackBucket = new TourTrack_Bucket();

         if (chainedBucked == null) {

            // insert at start

            _trackBucket_Painter = trackBucket;
         }
      }

      _trackBucket_Worker = trackBucket;

      return trackBucket;
   }

   /**
    * Set new bucket and clear previous
    */
   public void setBucket_Painter(final TourTrack_Bucket newBucket) {

      final TourTrack_Bucket previousPainterBucket = _trackBucket_Painter;

      if (previousPainterBucket != null) {
         previousPainterBucket.clear();
      }

      _trackBucket_Painter = newBucket;
   }

}
