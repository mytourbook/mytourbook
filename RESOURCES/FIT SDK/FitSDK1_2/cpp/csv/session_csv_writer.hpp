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

#if !defined(SESSION_CSV_WRITER_HPP)
#define SESSION_CSV_WRITER_HPP

#include "fit_session_mesg_listener.hpp"
#include "csv_writer.hpp"

using namespace fit;

class SessionCSVWriter : public SessionMesgListener
{
   public:
      SessionCSVWriter(string fileName);
      void Close(void);
      void OnMesg(SessionMesg& mesg);

   private:
      CSVWriter csv;
};

#endif // !defined(CSV_WRITER_HPP)
