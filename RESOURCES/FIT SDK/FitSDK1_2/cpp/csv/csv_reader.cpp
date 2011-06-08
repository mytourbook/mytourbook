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
   Mesg mesg;
   vector<string> cells;
   int lineNum = 1;
  
   ReadRow(cells, in); // Skip header.

   while (!in.eof())
   {
      int cellIndex = 1;

      ReadRow(cells, in);
      lineNum++;

      if ((cells.size() < 1) || (cells[0].compare("") == 0))
         continue;

      mesg = Mesg(cells[0]);

      if (mesg.GetNum() == FIT_MESG_NUM_INVALID) {
         printf("CSVReader.read(): Error on line %d - Unknown message \"%s\".\n", lineNum, mesg.GetName());
         return FIT_FALSE;
      }

      while ((cellIndex + 2) <= (int)cells.size()) {
         string fieldName = cells[cellIndex++];
         Field field(mesg.GetName(), fieldName);
         vector<string> values;
         
         ReadValues(values, cells[cellIndex++]);
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
            field.SetSTRINGValue(values[i], i);
         }

         mesg.AddField(field);
      }

      mesgListener.OnMesg(mesg);
   }

   return FIT_TRUE;
}

void CSVReader::ReadRow(vector<string> &cells, fstream &in)
{
   string cell = "";
   bool inQuotes = false;
   char c;

   cells.clear();

   while ((c = in.get()) != EOF)
   {
      switch (c)
      {
         case '\r':
            break;
         
         case '\n':
            cells.push_back(cell);
            return;            

         case '"':
            inQuotes = !inQuotes;
            break;

         case ',':
            if (inQuotes)
            {
               cell += c;
            }
            else
            {
               cells.push_back(cell);
               cell = "";
            }
            break;

         default:
            cell += c;
            break;
      }
   }

   cells.push_back(cell);
}

void CSVReader::ReadValues(vector<string> &values, string cell)
{
   string value = "";
   int i = 0;

   values.clear();

   while (i < (int)cell.length())
   {
      if (cell[i] == '|')
      {
         if (value.length() > 0)
            values.push_back(value);
         value = "";
      }
      else
      {
         value += cell[i];
      }
      
      i++;
   }
   
   if (value.length() > 0)
      values.push_back(value);
}