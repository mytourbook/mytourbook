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

#include "csv_reader.hpp"


FIT_BOOL CSVReader::Read(fstream &in, MesgListener &mesgListener)
{
   #define MAX_LINE_SIZE   255
   char line[MAX_LINE_SIZE];
   int lineNum = 1;

   in.getline(line, MAX_LINE_SIZE); // Skip header.

   while (in.eof())
   {
      Mesg mesg;
      vector<string> cells;
      int cellIndex = 1;

      in.getline(line, MAX_LINE_SIZE);
      cells = ReadCells(line);
      lineNum++;

      if (cells.size() < 1)
         break;

      mesg = Mesg(cells[0]);

      if (mesg.GetNum() == FIT_MESG_NUM_INVALID) {
         printf("CSVReader.read(): Error on line %d - Unknown message \"%s\".\n", lineNum, mesg.GetName());
         return FIT_FALSE;
      }

      while ((cellIndex + 2) <= (int)cells.size()) {
         string fieldName = cells[cellIndex++];
         Field field(mesg.GetName(), fieldName);
         vector<string> values = ReadValues(cells[cellIndex++]);
         cellIndex++; // ignore units

         if (fieldName == "")
            break; // Blank cell.

         if (field.GetNum() == FIT_FIELD_NUM_INVALID) {
            printf("CSVReader.read(): Error on line %d - Unknown field \"%s\" in message \"%s\".\n", lineNum, fieldName, mesg.GetName());
            return FIT_FALSE;
         }
         
         if (values.size() == 0)
            break;

         for (int i = 0; i < (int)values.size(); i++)
         {
            //!!field.SetValue(field.GetNumValues(), value);
         }

         mesg.AddField(field);
      }

      mesgListener.OnMesg(mesg);
   }

   return FIT_TRUE;
}

vector<string> CSVReader::ReadCells(string line)
{
   vector<string> list;

   /*//!!
   m = csvPattern.matcher(line);

   while (m.find()) {
      String match = m.group();

      if (match == null)
         break;

      if (match.endsWith(",")) { // trim trailing ,
         match = match.substring(0, match.length() - 1);
      }

      if (match.startsWith("\"")) { // assume also ends with
         match = match.substring(1, match.length() - 1);
      }

      if (match.length() == 0)
         match = null;

      list.add(match);
   }
   */
   return list;
}

vector<string> CSVReader::ReadValues(string cell)
{
   vector<string> list;
   string value;
   int i = 0;

   /*//!!
   value = "";
   while (i < cell.length()) {
      if (cell.charAt(i) == '|') {
         if (value.length() > 0)
            list.add(value);
         value = "";
      } else {
         value += cell.charAt(i);
      }
      i++;
   }
   
   if (value.length() > 0)
      list.add(value);
   */  
   return list;
}