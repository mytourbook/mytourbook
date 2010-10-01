// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: Touch.c $
// First created:   $Created: Dec 11, 2009 by mirko $
// Last modified:   $Date$ by $Author$
// Version:         $Revision$
//
// Copyright (c) 2009 Mirko Raner.
// All rights reserved. This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available
// at http://www.eclipse.org/legal/epl-v10.html

/**
* The file Touch.c contains JNI code for interfacing with the Mac OS X low-level Multi-Touch API.
*
* This class is partially based on reverse engineering and previous development work by Erling Alf
* Ellingsen and Wayne Keenan.
*
* @author Mirko Raner
* @version $Revision: $
**/

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>

// Class and method names, signatures, and other constants:
//
#define OK 1
#define NOT_OK -1
#define TOUCH "net/sf/eclipsemultitouch/api/Touch"
#define CONSTRUCTOR "<init>"
#define CALLBACK "callback"
#define CALLBACK_SIGNATURE "(D[L"TOUCH";)V"
#define NEW_TOUCH_SIGNATURE "(DIIIFFFFFFFF)V"
#define THREAD_NAME "Touch Dispatcher"

// Macros for JNI and system error handling:
//
#define ASSERT_NOT_NULL(call) if ((call) == 0) {perror(#call" returned zero\n"); return NOT_OK;}
#define JNI_ASSERT(call) if ((call) == 0) {perror(#call" returned zero\n"); return JNI_ERR;}
#define JNI_ASSERT_NULL(call) if (call) {perror(#call" returned non-zero\n"); return JNI_ERR;}

// Inferred layout of a Multi-Touch contact event:
//
struct Touch
{
	int frame;
	double timestamp;
	int identifier, state;
	int unknown1, unknown2;
	float positionX, positionY, velocityX, velocityY, size;
	int unknown3;
	float angle, majorAxis, minorAxis;
	float unknown4, unknown5, unknown6, unknown7;
	int unknown8, unknown9;
	float unknown10;
};

// Typedefs used by private Multi-Touch API (currently, no header files are available for this API):
//
typedef long MTDeviceRef;
typedef int (*MTContactCallbackFunction)(int, struct Touch*, int, double, int);

// Function prototypes for Multi-Touch API (currently, no header files are available for this API):
//
MTDeviceRef MTDeviceCreateDefault();
void MTDeviceStart(MTDeviceRef, int);
void MTDeviceStop(MTDeviceRef);
void MTRegisterContactFrameCallback(MTDeviceRef, MTContactCallbackFunction);

// Statically stored data and cached JNI structures:
//
static JavaVM* JVM;
static jclass Touch;
static jmethodID callback;
static jmethodID newTouch;
static MTDeviceRef multitouch;

/**
* Receives Multi-Touch data from the native driver and passes it to the Java callback function.
*
* @param device - the Multi-Touch device ID
* @param touchData - the Multi-Touch data
* @param touchCount - the number of contacts/fingers
* @param time - a time stamp
* @param frame - the frame sequence number of the received Multi-Touch data packet
* @return JNI_OK or a JNI error code
**/
int receiveTouchData(int device, struct Touch* touchData, int touchCount, double time, int frame)
{
	JNIEnv* JNI;
	jarray array;
	jweak arrayReference;
	int touch;
	struct {jint version; char* name; jobject group;} attach = {JNI_VERSION_1_2, THREAD_NAME, NULL};
	JNI_ASSERT_NULL((*JVM)->AttachCurrentThread(JVM, (void**)&JNI, &attach));
	ASSERT_NOT_NULL(Touch);
	ASSERT_NOT_NULL(newTouch);
	ASSERT_NOT_NULL(callback);
	JNI_ASSERT(array = (*JNI)->NewObjectArray(JNI, (jsize)touchCount, Touch, NULL));
	for (touch = 0; touch < touchCount; touch++)
	{
		jobject element;
		jweak reference;
		struct Touch data = touchData[touch];
		element = (*JNI)->NewObject(JNI, Touch, newTouch, time, data.frame, data.identifier,
			data.state, data.positionX, data.positionY, data.velocityX, data.velocityY,
			data.size, data.angle, data.majorAxis, data.minorAxis);
		JNI_ASSERT(reference = (*JNI)->NewWeakGlobalRef(JNI, element));
		(*JNI)->SetObjectArrayElement(JNI, array, touch, reference);
		(*JNI)->DeleteWeakGlobalRef(JNI, reference);
		(*JNI)->DeleteLocalRef(JNI, element);
	}
	JNI_ASSERT(arrayReference = (*JNI)->NewWeakGlobalRef(JNI, array));
	(*JNI)->CallStaticVoidMethod(JNI, Touch, callback, time, arrayReference);
	(*JNI)->DeleteWeakGlobalRef(JNI, arrayReference);
	(*JNI)->DeleteLocalRef(JNI, array);
	return OK;
}

/**
* Initializes the native code when the native library is loaded.
*
* @param jvm - a pointer to the JavaVM structure
* @param reserved - a reserved parameter
* @return the JNI version number or a JNI error code
**/
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved)
{
	JNIEnv* JNI;
	jclass classTouch;
	JVM = jvm;
	JNI_ASSERT_NULL((*JVM)->GetEnv(JVM, (void**)&JNI, JNI_VERSION_1_2));
	JNI_ASSERT(classTouch = (*JNI)->FindClass(JNI, TOUCH));
	JNI_ASSERT(Touch = (*JNI)->NewWeakGlobalRef(JNI, classTouch));
	JNI_ASSERT(callback = (*JNI)->GetStaticMethodID(JNI, Touch, CALLBACK, CALLBACK_SIGNATURE));
	JNI_ASSERT(newTouch = (*JNI)->GetMethodID(JNI, Touch, CONSTRUCTOR, NEW_TOUCH_SIGNATURE));
	ASSERT_NOT_NULL(multitouch = MTDeviceCreateDefault());
	MTRegisterContactFrameCallback(multitouch, receiveTouchData);
	MTDeviceStart(multitouch, 0);
	return JNI_VERSION_1_2;
}

/**
* Frees static data when the library is unloaded.
*
* TODO: this method should also call DetachCurrentThread, but unfortunately the thread that needs to
*       be detached is the thread that calls receiveTouchData(...) not the thread that calls
*       JNI_OnUnload(...); implementing this may not be worth the effort because it would incur only
*       a very minor memory leak and only if the native library is unloaded but the hosting VM keeps
*       running (which is a rather rare situation)
*
* @param jvm - a pointer to the JavaVM structure
* @param reserved - a reserved parameter
**/
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* jvm, void* reserved)
{
	JNIEnv* JNI;
	(*JVM)->GetEnv(JVM, (void**)&JNI, JNI_VERSION_1_2);
	(*JNI)->DeleteWeakGlobalRef(JNI, Touch);
}
