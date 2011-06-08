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

#include "fit_encode.hpp"
#include "fit_mesg_broadcaster.hpp"
#include "fit_file_id_mesg.hpp"

int main(int argc, char* argv[])
{
   fit::Encode encode;
   fstream file;

   printf("FIT Encode Example Application\n");

   file.open("test.fit", ios::in|ios::out|ios::binary|ios::trunc);

   if (!file.is_open())
   {
      printf("Error opening file test.fit\n");
      return -1;
   }

   fit::FileIdMesg fileIdMesg;
   fileIdMesg.SetManufacturer(FIT_MANUFACTURER_DYNASTREAM);
   fileIdMesg.SetProduct(0);
   fileIdMesg.SetSerialNumber(12345);
   
   encode.Open(file);
   encode.Write(fileIdMesg);
   if (!encode.Close())
   {
      printf("Error closing encode.\n");
      return -1;
   }
   file.close();

   printf("Encoded FIT file test.fit.\n");

   return 0;
}

