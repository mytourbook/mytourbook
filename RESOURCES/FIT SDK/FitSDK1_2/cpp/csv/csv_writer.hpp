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

#if !defined(CSV_WRITER_HPP)
#define CSV_WRITER_HPP

#include <vector>
#include <fstream>

using namespace std;

class CSVWriter
{
   public:
      CSVWriter(string fileName);
      void Open(void);
      void Close(void);
      void Clear();
      void Set(string header, string value); 
      void Set(string header, double value); 
      void Writeln(void); 

   private:
      string fileName;
      fstream file;
      vector<string> headers;
      vector<string> values;
};

#endif // !defined(CSV_WRITER_HPP)

