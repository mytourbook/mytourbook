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

#if !defined(RECORD_CSV_WRITER_HPP)
#define RECORD_CSV_WRITER_HPP

#include "fit_mesg_with_event_listener.hpp"
#include "fit_record_mesg_listener.hpp"
#include "csv_writer.hpp"

using namespace fit;

class RecordCSVWriter: public MesgWithEventListener, public RecordMesgListener
{
   public:
      RecordCSVWriter(string fileName);
      void Close(void);
      void OnMesg(MesgWithEvent& mesg);
      void OnMesg(RecordMesg& mesg);

   private:
      CSVWriter csv;
};

#endif // !defined(RECORD_CSV_WRITER_HPP)
