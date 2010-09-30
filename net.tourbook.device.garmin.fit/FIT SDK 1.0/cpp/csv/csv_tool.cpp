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

#include <fstream>
#include <string>

#include "fit_decode.hpp"
#include "csv_reader.hpp"
#include "fit_encode.hpp"
#include "csv_writer.hpp"
#include "mesg_csv_writer.hpp"
#include "activity_csv_writer.hpp"
#include "session_csv_writer.hpp"
#include "lap_csv_writer.hpp"
#include "record_csv_writer.hpp"
#include "fit_mesg_broadcaster.hpp"

using namespace fit;
using namespace std;

void PrintUsage(void);

int main(int numArgs, char* args[])
{
   string in = "";
   string out = "";
   FIT_BOOL fitToCsv = false;
   FIT_BOOL csvToFit = false;
   int arg = 0;

   printf("FIT CSV Tool %d.%d.%d.%d\n", FIT_PROTOCOL_VERSION_MAJOR, FIT_PROTOCOL_VERSION_MINOR, FIT_PROFILE_VERSION_MAJOR, FIT_PROFILE_VERSION_MINOR);

   while (arg < numArgs)
   {
      if (strcmp(args[arg], "-b") == 0)
      {
         if ((numArgs - arg) < 3)
         {
            PrintUsage();
            return 0;
         }

         fitToCsv = true;
         in = args[arg + 1];
         out = args[arg + 2];

         arg += 2;
      }
      else if (strcmp(args[arg], "-c") == 0)
      {
         if ((numArgs - arg) < 3)
         {
            PrintUsage();
            return 0;
         }

         csvToFit = true;
         in = args[arg + 1];
         out = args[arg + 2];
      }
      else if (strcmp(args[arg], "-d") == 0)
      {
         //!!Fit.debug = true;
      }

      arg++;
   }

   if (fitToCsv)
   {
      fstream file(in.c_str(), ios::in | ios::binary);
      Decode decode;
      MesgBroadcaster broadcaster;

      if ((out.length() >= 4) && (out.substr(out.length() - 4, out.length()) == ".csv"))
         out = out.substr(0, out.length() - 4); // Remove .csv extension.

      if (!decode.CheckIntegrity(file))
      {
         printf("FIT file integrity failure.");
         return -1;
      }

      MesgCSVWriter mesgWriter(out + ".csv");
      RecordCSVWriter recordWriter(out + "_records.csv");
      LapCSVWriter lapWriter(out + "_laps.csv");
      SessionCSVWriter sessionWriter(out + "_sessions.csv");

      broadcaster.AddListener(mesgWriter);
      broadcaster.AddListener((RecordMesgListener&) recordWriter);
      broadcaster.AddListener((MesgWithEventListener&) recordWriter);
      broadcaster.AddListener(lapWriter);
      broadcaster.AddListener(sessionWriter);

      broadcaster.Run(file);

      mesgWriter.Close();
      recordWriter.Close();
      lapWriter.Close();
      sessionWriter.Close();

      printf("FIT binary file %s decoded to %s*.csv files.\n", in.c_str(), out.c_str());
   }
   else if (csvToFit)
   {
      Encode encode;
      fstream inFile(in.c_str(), ios::in);
      fstream outFile(out.c_str(), ios::out | ios::in | ios::binary);

      encode.Open(outFile);

      if (!CSVReader::Read(inFile, encode))
         printf("FIT encoding error.\n");
      
      encode.Close();

      printf("%s encoded into FIT binary file %s.\n", in, out);
   }
   else
   {
      PrintUsage();
   }

   return 0;
}

void PrintUsage(void)
{
   printf("Usage: FitCSVTool -b|-c <INPUT FILE> <OUTPUT FILE>\n");
   printf("      -b <FIT FILE> <CSV FILE>  FIT binary to CSV.\n");
   printf("      -c <CSV FILE> <FIT FILE>  CSV to FIT binary.\n");
   printf("      -d Enable debug output (also enables file verification tests).\n");
}

