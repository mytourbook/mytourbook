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

#include "fit_convert.h"

int main(int argc, char* argv[])
{
   FILE *file;
   FIT_UINT8 buf[8];
   FIT_CONVERT_RETURN convert_return = FIT_CONVERT_CONTINUE;
   FIT_UINT32 buf_size;
   FIT_UINT32 mesg_index = 0;
   #if defined(FIT_CONVERT_MULTI_THREAD)
      FIT_CONVERT_STATE state;
   #endif

   printf("Testing file conversion using %s file...\n", argv[1]); 

   #if defined(FIT_CONVERT_MULTI_THREAD)
      FitConvert_Init(&state, FIT_TRUE);
   #else
      FitConvert_Init(FIT_TRUE);
   #endif
   
   if((file = fopen(argv[1], "rb")) == NULL)
   {
      printf("Error opening file %s.\n", argv[1]);
      return FIT_FALSE;
   }

   while(!feof(file) && (convert_return == FIT_CONVERT_CONTINUE))
   {
      for(buf_size=0;(buf_size < sizeof(buf)) && !feof(file); buf_size++)
      {
         buf[buf_size] = getc(file);
      }

      do
      {
         #if defined(FIT_CONVERT_MULTI_THREAD)
            convert_return = FitConvert_Read(&state, buf, buf_size);
         #else
            convert_return = FitConvert_Read(buf, buf_size);
         #endif

         switch (convert_return)
         {
            case FIT_CONVERT_MESSAGE_AVAILABLE:
            {
               #if defined(FIT_CONVERT_MULTI_THREAD)
                  const FIT_UINT8 *mesg = FitConvert_GetMessageData(&state);
                  FIT_UINT16 mesg_num = FitConvert_GetMessageNumber(&state);
               #else
                  const FIT_UINT8 *mesg = FitConvert_GetMessageData();
                  FIT_UINT16 mesg_num = FitConvert_GetMessageNumber();
               #endif
               
               printf("Mesg %d (%d) - ", mesg_index++, mesg_num); 
               
               switch(mesg_num)
               {
                  case FIT_MESG_NUM_FILE_ID:
                  {
                     const FIT_FILE_ID_MESG *id = (FIT_FILE_ID_MESG *) mesg;
                     printf("File ID: type=%u, number=%u\n", id->type, id->number); 
                     break;
                  }

                  case FIT_MESG_NUM_USER_PROFILE:
                  {
                     const FIT_USER_PROFILE_MESG *user_profile = (FIT_USER_PROFILE_MESG *) mesg;
                     printf("User Profile: weight=%0.1fkg\n", user_profile->weight / 10.0f); 
                     break;
                  }

                  case FIT_MESG_NUM_ACTIVITY:
                  {
                     const FIT_ACTIVITY_MESG *activity = (FIT_ACTIVITY_MESG *) mesg;
                     printf("Activity: timestamp=%u, type=%u, event=%u, event_type=%u, num_sessions=%u\n", activity->timestamp, activity->type, activity->event, activity->event_type, activity->num_sessions); 
                     {
                        FIT_ACTIVITY_MESG old_mesg;
                        old_mesg.num_sessions = 1;
                        #if defined(FIT_CONVERT_MULTI_THREAD)
                           FitConvert_RestoreFields(&state, &old_mesg);
                        #else
                           FitConvert_RestoreFields(&old_mesg);
                        #endif
                        printf("Restored num_sessions=1 - Activity: timestamp=%u, type=%u, event=%u, event_type=%u, num_sessions=%u\n", activity->timestamp, activity->type, activity->event, activity->event_type, activity->num_sessions); 
                     }
                     break;
                  }

                  case FIT_MESG_NUM_SESSION:
                  {
                     const FIT_SESSION_MESG *session = (FIT_SESSION_MESG *) mesg;
                     printf("Session: timestamp=%u\n", session->timestamp); 
                     break;
                  }

                  case FIT_MESG_NUM_LAP:
                  {
                     const FIT_LAP_MESG *lap = (FIT_LAP_MESG *) mesg;
                     printf("Lap: timestamp=%u\n", lap->timestamp); 
                     break;
                  }

                  case FIT_MESG_NUM_RECORD:
                  {
                     const FIT_RECORD_MESG *record = (FIT_RECORD_MESG *) mesg;
                     
                     printf("Record: timestamp=%u", record->timestamp);
                     
                     if (
                           (record->compressed_speed_distance[0] != FIT_BYTE_INVALID) ||
                           (record->compressed_speed_distance[1] != FIT_BYTE_INVALID) ||
                           (record->compressed_speed_distance[2] != FIT_BYTE_INVALID)
                        )
                     {
                        static FIT_UINT32 accumulated_distance16 = 0;
                        static FIT_UINT32 last_distance16 = 0;
                        FIT_UINT16 speed100;
                        FIT_UINT32 distance16;
                        
                        speed100 = record->compressed_speed_distance[0] | ((record->compressed_speed_distance[1] & 0x0F) << 8);
                        printf(", speed = %0.2fm/s", speed100/100.0f);
                     
                        distance16 = (record->compressed_speed_distance[1] >> 4) | (record->compressed_speed_distance[2] << 4);
                        accumulated_distance16 += (distance16 - last_distance16) & 0x0FFF;
                        last_distance16 = distance16;

                        printf(", distance = %0.3fm", accumulated_distance16/16.0f);
                     }

                     printf("\n");
                     break;
                  }
                  
                  case FIT_MESG_NUM_EVENT:
                  {
                     const FIT_EVENT_MESG *event = (FIT_EVENT_MESG *) mesg;
                     printf("Event: timestamp=%u\n", event->timestamp); 
                     break;
                  }

                  case FIT_MESG_NUM_DEVICE_INFO:
                  {
                     const FIT_DEVICE_INFO_MESG *device_info = (FIT_DEVICE_INFO_MESG *) mesg;
                     printf("Device Info: timestamp=%u\n", device_info->timestamp); 
                     break;
                  }
                  
                  default:
                    printf("Unknown\n");
                    break;
               }
               break;
            }

            default:
               break;
         }
      } while (convert_return == FIT_CONVERT_MESSAGE_AVAILABLE);
   }

   if (convert_return == FIT_CONVERT_ERROR)
   {
      printf("Error decoding file.\n");
      fclose(file);
      return 1;
   }

   if (convert_return == FIT_CONVERT_CONTINUE)
   {
      printf("Unexpected end of file.\n");
      fclose(file);
      return 1;
   }

   if (convert_return == FIT_CONVERT_PROTOCOL_VERSION_NOT_SUPPORTED)
   {
      printf("Protocol version not supported.\n");
      fclose(file);
      return 1;
   }

   if (convert_return == FIT_CONVERT_END_OF_FILE)
      printf("File converted successfully.\n");

   fclose(file);
   
   return 0;
}
