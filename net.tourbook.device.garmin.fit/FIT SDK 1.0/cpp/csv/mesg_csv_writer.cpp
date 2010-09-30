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

#include <sstream>
#include "mesg_csv_writer.hpp"

using namespace fit;

MesgCSVWriter::MesgCSVWriter(string fileName)
: csv(fileName)
{
}

void MesgCSVWriter::Close(void)
{
   csv.Close();
}

void MesgCSVWriter::OnMesg(Mesg& mesg)
{
   csv.Clear();
   csv.Set("Message", mesg.GetName());
   for (int i = 0; i < mesg.GetNumFields(); i++)
   {
      Field* field = mesg.GetFieldByIndex(i);
      ostringstream headerNum;

      headerNum << i + 1;      
      csv.Set("Field " + headerNum.str(), field->GetName());

      string value = field->GetSTRINGValue(0);
      for (int fieldElement = 1; fieldElement < field->GetNumValues(); fieldElement++)
      {
         value += "|" + field->GetSTRINGValue(fieldElement);
      }
      csv.Set("Value " + headerNum.str(), value);

      csv.Set("Units " + headerNum.str(), field->GetUnits());
   }

   csv.Writeln();
}

