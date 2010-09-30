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

#include "activity_csv_writer.hpp"

using namespace fit;

ActivityCSVWriter::ActivityCSVWriter(string fileName)
: csv(fileName)
{
}

void ActivityCSVWriter::Close(void)
{
   csv.Close();
}

void ActivityCSVWriter::OnMesg(ActivityMesg mesg)
{
   csv.Writeln();
}
