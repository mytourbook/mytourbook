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

#if !defined(MESG_CSV_WRITER_HPP)
#define MESG_CSV_WRITER_HPP

#include "fit_mesg_listener.hpp"
#include "csv_writer.hpp"

using namespace fit;

class MesgCSVWriter : public MesgListener
{
   public:
      MesgCSVWriter(string fileName);
      void Close(void);
      void OnMesg(Mesg& mesg);

   private:
      CSVWriter csv;
};

#endif // !defined(FIT_MESG_OBSERVER_HPP)
