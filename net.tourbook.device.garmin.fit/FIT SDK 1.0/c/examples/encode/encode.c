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

#define _CRT_SECURE_NO_WARNINGS

#include "stdio.h"
#include "string.h"

#include "fit_product.h"
#include "fit_crc.h"

///////////////////////////////////////////////////////////////////////
// Private Definitions 
///////////////////////////////////////////////////////////////////////
#define LOCAL_MESSAGE_NUMBER_RECORD    0

///////////////////////////////////////////////////////////////////////
// Private Function Prototypes 
///////////////////////////////////////////////////////////////////////

void CreateFITFile(FILE *fp);
///////////////////////////////////////////////////////////////////////
// Creates a FIT file. Puts a place-holder for the file header on top of the file. 
// Not provided in SDK. 
///////////////////////////////////////////////////////////////////////

void WriteMessageDefinition(FIT_UINT8 local_mesg_number, void *mesg_def_pointer, FIT_UINT8 mesg_def_size, FILE *fp);
///////////////////////////////////////////////////////////////////////
// Appends a FIT message definition (including the definition header) to the end of a file. 
// Not provided in SDK. 
///////////////////////////////////////////////////////////////////////

void WriteMessage(FIT_UINT8 local_mesg_number, void *mesg_pointer, FIT_UINT8 mesg_size, FILE *fp);
///////////////////////////////////////////////////////////////////////
// Appends a FIT message (including the message header) to the end of a file. 
// Not provided in SDK.  
///////////////////////////////////////////////////////////////////////

void CloseFITFile(FILE *fp);
///////////////////////////////////////////////////////////////////////
// Calls a function to calculate the file size and back-fills the 
// file header information at top of the file.
// Calculates the 16-bit file CRC and appends it to the end of the file
// Not provided in SDK. 
///////////////////////////////////////////////////////////////////////

FIT_UINT16 FileCalculateCRC16(FILE *fp);
///////////////////////////////////////////////////////////////////////
// Calculates CRC16 for the file (including header and data)
// Returns the 16-bit CRC value.
// Not provided in SDK. 
///////////////////////////////////////////////////////////////////////

void UpdateSportsData(FIT_RECORD_MESG *SportsDataStruct);
///////////////////////////////////////////////////////////////////////
// Function to update the user speed, distance, cadence, and heart rate
// based on data received from sports sensors.
// Not provided in SDK. 
///////////////////////////////////////////////////////////////////////

void UpdateFileHeader(FILE *fp);
///////////////////////////////////////////////////////////////////////
// Function to update the file header
// Not provided in SDK. 
///////////////////////////////////////////////////////////////////////

void WaitMiliSeconds(FIT_UINT16 delay_miliseconds);


///////////////////////////////////////////////////////////////////////
// Private Variables 
///////////////////////////////////////////////////////////////////////
static FIT_FILE_HDR file_header;
static FIT_UINT32 distance = 10;

int main(void)
{   
   FIT_RECORD_MESG sports_data;   
   FIT_UINT8 num_messages;
   FILE *fp; 

   fp = fopen("FITFile.txt", "w+b");

   CreateFITFile(fp); 
   
   WriteMessageDefinition(LOCAL_MESSAGE_NUMBER_RECORD, (void *)fit_mesg_defs[FIT_MESG_RECORD], FIT_RECORD_MESG_DEF_SIZE, fp);  

   for (num_messages = 0; num_messages < 10; num_messages++)
   {
      // Initialize the fields of the FIT message struct to invalid
      Fit_InitMesg((FIT_MESG_DEF *)fit_mesg_defs[FIT_MESG_RECORD], (void*)&sports_data);

      // The function below updates the contents of the RECORD message struct based on the data received from sports sensors
      UpdateSportsData(&sports_data);	
      WriteMessage(LOCAL_MESSAGE_NUMBER_RECORD, &sports_data, FIT_RECORD_MESG_SIZE, fp);

      WaitMiliSeconds(1000);  // 1-second delay to receive new data
   }  
   CloseFITFile(fp);    
   return 0;
}

void CreateFITFile(FILE *fp)
{   
   file_header.header_size = FIT_FILE_HDR_SIZE;
   file_header.profile_version = FIT_PROFILE_VERSION;
   file_header.protocol_version = FIT_PROTOCOL_VERSION;
   strcpy_s((FIT_UINT8 *)&file_header.data_type, sizeof(".FIT"), ".FIT");
   file_header.data_size = 0;                                     // The header size will be updated before closing the file

   fwrite((void *)&file_header, 1, FIT_FILE_HDR_SIZE, fp);			// Write the temporary file header
}
  
void WriteMessageDefinition(FIT_UINT8 local_mesg_number, void *mesg_def_pointer, FIT_UINT8 mesg_def_size, FILE *fp)
{
   FIT_UINT8 header;

   header = local_mesg_number | 0x40;                             // To mark the header as a "definition header"
   
   // Insert the definition header
   fwrite(&header, 1, FIT_HDR_SIZE, fp);

   // Insert the message definition
   fwrite(mesg_def_pointer, 1, mesg_def_size, fp);  
}

void WriteMessage(FIT_UINT8 local_mesg_number, void *mesg_pointer, FIT_UINT8 mesg_size, FILE *fp)
{
   // Insert the message header
   fwrite(&local_mesg_number, 1, FIT_HDR_SIZE, fp);

   // Insert the message
   fwrite(mesg_pointer, 1, mesg_size, fp);   
}

void CloseFITFile(FILE *fp)
{
   FIT_UINT16 crc16;
               
   // Function to calculate the file size (does not include CRC16)
   UpdateFileHeader(fp);                        
   
   fseek (fp , 0 , SEEK_SET);
   fwrite((void *)&file_header, 1, FIT_FILE_HDR_SIZE, fp);

   // CRC 16 must include the header and data
   crc16 = FileCalculateCRC16(fp);           
   fseek(fp, 0, SEEK_END); 
   fwrite(&crc16, 1, sizeof(crc16), fp);

   fclose(fp);
}

void WaitMiliSeconds(FIT_UINT16 delay_miliseconds)
{   
}


void UpdateSportsData(FIT_RECORD_MESG *SportsDataStruct)
{
   SportsDataStruct->distance = distance;
   distance += 10;   
}

void UpdateFileHeader(FILE *fp)
{
   FIT_UINT32 data_size = 0;
   
   fseek (fp , 0 , SEEK_END);
   file_header.data_size = ftell(fp) - FIT_FILE_HDR_SIZE;
}

FIT_UINT16 FileCalculateCRC16(FILE *fp)
{
   FIT_UINT32 counter = 0;
   FIT_UINT16 crc16 = 0;

   fseek (fp , 0, SEEK_SET); 
   
   while (counter < (file_header.data_size + FIT_FILE_HDR_SIZE))
   {
      crc16 = FitCRC_Get16(crc16, getc(fp));
      counter++;
   }
   return crc16;
} 