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

#include "fit_decode.hpp"
#include "fit_mesg_broadcaster.hpp"

class Listener : public fit::FileIdMesgListener, public fit::UserProfileMesgListener 
{
   public :
      void OnMesg(fit::FileIdMesg& mesg)
      {
         printf("File ID:\n");
         if (mesg.GetType() != FIT_FILE_INVALID)
            printf("   Type: %d\n", mesg.GetType());
         if (mesg.GetManufacturer() != FIT_MANUFACTURER_INVALID)
            printf("   Manufacturer: %d\n", mesg.GetManufacturer());
         if (mesg.GetProduct() != FIT_UINT16_INVALID)
            printf("   Product: %d\n", mesg.GetProduct());
         if (mesg.GetSerialNumber() != FIT_UINT32Z_INVALID)
            printf("   Serial Number: %d\n", mesg.GetSerialNumber());
         if (mesg.GetNumber() != FIT_UINT16_INVALID)
            printf("   Number: %d\n", mesg.GetNumber());
      }

      void OnMesg(fit::UserProfileMesg& mesg)
      {
         printf("User profile:\n");
         if (mesg.GetFriendlyName() != "")
            printf("   Friendly Name: %s\n", mesg.GetFriendlyName().c_str());
         if (mesg.GetGender() == FIT_GENDER_MALE)
            printf("   Gender: Male\n");
         if (mesg.GetGender() == FIT_GENDER_FEMALE)
            printf("   Gender: Female\n");
         if (mesg.GetAge() != FIT_UINT8_INVALID)
            printf("   Age [years]: %d\n", mesg.GetAge());
         if (mesg.GetWeight() != FIT_FLOAT32_INVALID)
            printf("   Weight [kg]: %0.2f\n", mesg.GetWeight());
      }
};

int main(int argc, char* argv[])
{
   fit::Decode decode;
   fit::MesgBroadcaster mesgBroadcaster;
   Listener listener;
   fstream file;

   printf("FIT Decode Example Application\n");

   if (argc != 2)
   {
      printf("Usage: decode.exe <filename>\n");
      return -1;
   }

   file.open(argv[1], ios::in|ios::binary);

   if (!file.is_open())
   {
      printf("Error opening file %s\n", argv[1]);
      return -1;
   }

   if (!decode.CheckIntegrity(file))
   {
      printf("FIT file integrity failed.\n");
      return -1;
   }

   mesgBroadcaster.AddListener((fit::FileIdMesgListener &)listener);
   mesgBroadcaster.AddListener((fit::UserProfileMesgListener &)listener);
   
   try
   {
      mesgBroadcaster.Run(file);
   }
   catch (const fit::RuntimeException& e)
   {
      printf("Exception decoding file: %s\n", e.what());
      return -1;
   }

   printf("Decoded FIT file %s.\n", argv[1]);

   return 0;
}

