////////////////////////////////////////////////////////////////////////////////
// The following FIT Protocol software provided may be used with FIT protocol
// devices only and remains the copyrighted property of Dynastream Innovations Inc.
// The software is being provided on an "as-is" basis and as an accommodation,
// and therefore all warranties, representations, or guarantees of any kind
// (whether express, implied or statutory) including, without limitation,
// warranties of merchantability, non-infringement, or fitness for a particular
// purpose, are specifically disclaimed.
//
// Copyright 2008 Dynastream Innovations Inc.
////////////////////////////////////////////////////////////////////////////////

#include "lap_csv_writer.hpp"

using namespace fit;

LapCSVWriter::LapCSVWriter(string fileName)
: csv(fileName)
{
}

void LapCSVWriter::Close()
{
   csv.Close();
}

void LapCSVWriter::OnMesg(LapMesg& mesg)
{
   if (mesg.GetTimestamp() != FIT_DATE_TIME_INVALID)
      csv.Set("Timestamp [s]", mesg.GetTimestamp());
   if (mesg.GetMessageIndex() != FIT_MESSAGE_INDEX_INVALID)
      csv.Set("Lap #", mesg.GetMessageIndex() + 1);
   if (mesg.GetLapTrigger() != FIT_LAP_TRIGGER_INVALID)
      csv.Set("Lap Trigger", mesg.GetLapTrigger());
   if (mesg.GetSport() != FIT_SPORT_INVALID)
      csv.Set("Sport", mesg.GetSport());
   if (mesg.GetStartTime() != FIT_DATE_TIME_INVALID)
      csv.Set("Start Time [s]", mesg.GetStartTime());
   if (mesg.GetTotalTimerTime() != FIT_FLOAT32_INVALID)
      csv.Set("Total Timer Time [s]", mesg.GetTotalTimerTime());
   if (mesg.GetTotalElapsedTime() != FIT_FLOAT32_INVALID)
      csv.Set("Total Elapsed Time [s]", mesg.GetTotalElapsedTime());
   if (mesg.GetStartPositionLat() != FIT_SINT32_INVALID)
      csv.Set("Start Latitude [semicircles]", mesg.GetStartPositionLat());
   if (mesg.GetStartPositionLong() != FIT_SINT32_INVALID)
      csv.Set("Start Longitude [semicircles]", mesg.GetStartPositionLong());
   if (mesg.GetEndPositionLat() != FIT_SINT32_INVALID)
      csv.Set("End Latitude [semicircles]", mesg.GetEndPositionLat());
   if (mesg.GetEndPositionLong() != FIT_SINT32_INVALID)
      csv.Set("End Longitude [semicircles]", mesg.GetEndPositionLong());
   if (mesg.GetTotalDistance() != FIT_FLOAT32_INVALID)
      csv.Set("Total Distance [m]", mesg.GetTotalDistance());
   if (mesg.GetTotalCycles() != FIT_UINT32_INVALID)
      csv.Set("Total Cycles [cycles]", mesg.GetTotalCycles());
   if (mesg.GetTotalCalories() != FIT_UINT16_INVALID)
      csv.Set("Total Calories [calories]", mesg.GetTotalCalories());
   if (mesg.GetTotalAscent() != FIT_UINT16_INVALID)
      csv.Set("Total Ascent [m]", mesg.GetTotalAscent());
   if (mesg.GetTotalDescent() != FIT_UINT16_INVALID)
      csv.Set("Total Descent [m]", mesg.GetTotalDescent());
   if (mesg.GetAvgSpeed() != FIT_FLOAT32_INVALID)
      csv.Set("Avg Speed [m/s]", mesg.GetAvgSpeed());
   if (mesg.GetMaxSpeed() != FIT_FLOAT32_INVALID)
      csv.Set("Max Speed [m/s]", mesg.GetMaxSpeed());
   if (mesg.GetAvgHeartRate() != FIT_UINT8_INVALID)
      csv.Set("Avg Heart Rate [bpm]", mesg.GetAvgHeartRate());
   if (mesg.GetMaxHeartRate() != FIT_UINT8_INVALID)
      csv.Set("Max Heart Rate [bpm]", mesg.GetMaxHeartRate());
   if (mesg.GetAvgCadence() != FIT_UINT8_INVALID)
      csv.Set("Avg Cadence [rpm]", mesg.GetAvgCadence());
   if (mesg.GetMaxCadence() != FIT_UINT8_INVALID)
      csv.Set("Max Cadence [rpm]", mesg.GetMaxCadence());
   if (mesg.GetAvgPower() != FIT_UINT16_INVALID)
      csv.Set("Avg Power [watts]", mesg.GetAvgPower());
   if (mesg.GetMaxPower() != FIT_UINT16_INVALID)
      csv.Set("Max Power [watts]", mesg.GetMaxPower());

   csv.Writeln();
}

