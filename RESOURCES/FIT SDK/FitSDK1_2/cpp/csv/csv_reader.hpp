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

#if !defined(CSV_READER_HPP)
#define CSV_READER_HPP

#include <vector>
#include <string>
#include <fstream>
#include "fit_mesg_listener.hpp"

using namespace std;
using namespace fit;

class CSVReader
{
   public:
      static FIT_BOOL Read(fstream &in, MesgListener &mesgListener);
   private:
      static void ReadRow(vector<string> &cells, fstream &in);
      static void ReadValues(vector<string> &values, string cell);
};

#endif // !defined(CSV_READER_HPP)

