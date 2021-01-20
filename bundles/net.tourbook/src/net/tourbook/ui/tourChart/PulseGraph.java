/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

/**
 * Pulse graph parts which are displayed in the pulse graph
 */
public enum PulseGraph {

   /**
    * Device bpm values are displayed as graph, pulse times are displayed as line
    */
   DEVICE_BPM__2ND__RR_INTERVALS,

   /**
    * Only device bpm values are displayed, however when not available then the pulse time values
    * are displayed
    */
   DEVICE_BPM_ONLY,

   /**
    * Pulse times are displayed as line, device bpm values are displayed as graph
    */
   RR_INTERVALS__2ND_DEVICE_BPM,

   /**
    * Only pulse time values are displayed, however when not available then the device bpm values
    * are displayed
    */
   RR_INTERVALS_ONLY

}
