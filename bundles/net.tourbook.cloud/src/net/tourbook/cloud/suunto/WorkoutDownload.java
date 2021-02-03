/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package net.tourbook.cloud.suunto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WorkoutDownload {

   @JsonProperty("workoutKey")
   private String  _workoutKey;
   private boolean _successfullyDownloaded;
   private String  _absoluteFilePath;
   private String  _error;

   @JsonIgnoreProperties(ignoreUnknown = true)
   public WorkoutDownload(final String workoutKey) {

      _workoutKey = workoutKey;
      setSuccessfullyDownloaded(false);
   }

   public String getAbsoluteFilePath() {
      return _absoluteFilePath;
   }

   public String getError() {
      return _error;
   }

   public String getWorkoutKey() {
      return _workoutKey;
   }

   public boolean isSuccessfullyDownloaded() {
      return _successfullyDownloaded;
   }

   public void setAbsoluteFilePath(final String absoluteFilePath) {
      _absoluteFilePath = absoluteFilePath;
   }

   public void setError(final String error) {
      _error = error;
   }

   public void setSuccessfullyDownloaded(final boolean downloaded) {
      _successfullyDownloaded = downloaded;
   }

}
