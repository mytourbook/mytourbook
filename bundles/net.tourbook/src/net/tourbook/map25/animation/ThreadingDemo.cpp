
// Note that the following includes must be defined in order:
#include "GL\glew.h"
#include "GLFW\glfw3.h"
#include "ThreadingDemo.h"

// Note the the following Includes do not need to be defined in order:
#include <cstdio>
#include <map>
#include <list>
#include <thread>
#include <future>
#include <atomic>
#include "glm\glm.hpp"
#include "glm\ext.hpp"
#include <iostream>

// info: http://www.baptiste-wicht.com/2012/04/c11-concurrency-tutorial-advanced-locking-and-condition-variables/
//////////////////////// global Vars //////////////////////////////
unsigned int	g_uiWindowCounter = 0;							// used to set window IDs

std::list<WindowHandle>					g_lWindows;
std::map<unsigned int, unsigned int>	g_mVAOs;
std::map<std::thread::id, WindowHandle> g_mCurrentContextMap;	// store current contex per thread!

WindowHandle g_hPrimaryWindow = nullptr;
WindowHandle g_hSecondaryWindow = nullptr;

unsigned int g_VBO = 0;
unsigned int g_IBO = 0;
unsigned int g_Texture = 0;
unsigned int g_Shader = 0;
glm::mat4	g_ModelMatrix;

std::thread *g_tpWin2 = nullptr;
std::mutex g_RenderLock;
GLsync g_MainThreadFenceSync;
GLsync g_SecondThreadFenceSync;
std::atomic_bool g_bShouldClose;
std::atomic_bool g_bDoWork;

std::map<unsigned int, FPSData*> m_mFPSData;

//////////////////////// Function Declerations //////////////////////////////
Quad CreateQuad();

int Init();
int MainLoop();
int MainLoopBAD();
int MainLoopTHREADED();
void ChildLoop(WindowHandle a_toWindow);
void Render(WindowHandle a_toWindow);
int ShutDown();

void GLFWErrorCallback(int a_iError, const char* a_szDiscription);
void GLFWWindowSizeCallback(GLFWwindow* a_pWindow, int a_iWidth, int a_iHeight);
void APIENTRY GLErrorCallback(GLenum source, GLenum type, GLuint id, GLenum severity, GLsizei length, const GLchar* message, void* userParam);
GLEWContext* glewGetContext();   // This needs to be defined for GLEW MX to work, along with the GLEW_MX define in the perprocessor!
void CalcFPS(WindowHandle a_hWindowHandle);

WindowHandle  CreateWindow(int a_iWidth, int a_iHeight, const std::string& a_szTitle, GLFWmonitor* a_pMonitor, WindowHandle a_hShare);
void MakeContextCurrent(WindowHandle a_hWindowHandle);
bool ShouldClose();


//////////////////////// Function Definitions //////////////////////////////
int main()
{
	int iReturnCode =EC_NO_ERROR;

	iReturnCode = Init();
	if (iReturnCode != EC_NO_ERROR)
		return iReturnCode;

	// use this to simulate 3ms of work per window.
	g_bDoWork = true;

	/* Use the following loop to have this demo run like the
	original Multi-Window tutorial code. its here for comparison. */
	//iReturnCode = MainLoop();

	/* My initial naive attempt at multithreading on a per window basis.
	This loop will try to render from both threads simultaneously.
	WARNING: RUN AT YOUR OWN RISK. I have had this loop crash on several occasions,
	it is not stable and is only here as an example on how not to do it. */
	//iReturnCode = MainLoopBAD();

	/* This loop is a working/stable example of how to render from multipul threads. 
	Notice that this does NOT render from both threads at the same time. 
	*/
	iReturnCode = MainLoopTHREADED();


	if (iReturnCode != EC_NO_ERROR)
		return iReturnCode;

	iReturnCode = ShutDown();
	if (iReturnCode != EC_NO_ERROR)
		return iReturnCode;

	return iReturnCode;
}


int Init()
{
	// Setup Our GLFW error callback, we do this before Init so we know what goes wrong with init if it fails:
	glfwSetErrorCallback(GLFWErrorCallback);

	// Init GLFW:
	if (!glfwInit())
		return EC_GLFW_INIT_FAIL;

	// create our first window:
	g_hPrimaryWindow = CreateWindow(c_iDefaultScreenWidth, c_iDefaultScreenHeight, c_szDefaultPrimaryWindowTitle, nullptr, nullptr);
	
	if (g_hPrimaryWindow == nullptr)
	{
		glfwTerminate();
		return EC_GLFW_FIRST_WINDOW_CREATION_FAIL;
	}

	// Print out GLFW, OpenGL version and GLEW Version:
	int iOpenGLMajor = glfwGetWindowAttrib(g_hPrimaryWindow->m_pWindow, GLFW_CONTEXT_VERSION_MAJOR);
	int iOpenGLMinor = glfwGetWindowAttrib(g_hPrimaryWindow->m_pWindow, GLFW_CONTEXT_VERSION_MINOR);
	int iOpenGLRevision = glfwGetWindowAttrib(g_hPrimaryWindow->m_pWindow, GLFW_CONTEXT_REVISION);
	printf("Status: Using GLFW Version %s\n", glfwGetVersionString());
	printf("Status: Using OpenGL Version: %i.%i, Revision: %i\n", iOpenGLMajor, iOpenGLMinor, iOpenGLRevision);
	printf("Status: Using GLEW %s\n", glewGetString(GLEW_VERSION));

	// create our second window:
	g_hSecondaryWindow = CreateWindow(c_iDefaultScreenWidth, c_iDefaultScreenHeight, c_szDefaultSecondaryWindowTitle, nullptr, g_hPrimaryWindow);
	
	MakeContextCurrent(g_hPrimaryWindow);

	// start creating our quad data for later use:
	std::future<Quad> fQuad = std::async(std::launch::async, CreateQuad);
	glm::vec4 *ptexData = new glm::vec4[256 * 256];
	std::future<glm::vec4*> ftexData = std::async(std::launch::async, [ptexData] () -> glm::vec4*
	{
		for (int i = 0; i < 256 * 256; i += 256)
		{
			for (int j = 0; j < 256; ++j)
			{
				if (j % 2 == 0)
					ptexData[i + j] = glm::vec4(0, 0, 0, 1);
				else
					ptexData[i + j] = glm::vec4(1, 1, 1, 1);
			}
		}

		return ptexData;
	} );

	// create shader:
	GLint iSuccess = 0;
	GLchar acLog[256];
	GLuint vsHandle = glCreateShader(GL_VERTEX_SHADER);
	GLuint fsHandle = glCreateShader(GL_FRAGMENT_SHADER);

	glShaderSource(vsHandle, 1, (const char**)&c_szVertexShader, 0);
	glCompileShader(vsHandle);
	glGetShaderiv(vsHandle, GL_COMPILE_STATUS, &iSuccess);
	glGetShaderInfoLog(vsHandle, sizeof(acLog), 0, acLog);
	if (iSuccess == GL_FALSE)
	{
		printf("Error: Failed to compile vertex shader!\n");
		printf(acLog);
		printf("\n");
	}

	glShaderSource(fsHandle, 1, (const char**)&c_szPixelShader, 0);
	glCompileShader(fsHandle);
	glGetShaderiv(fsHandle, GL_COMPILE_STATUS, &iSuccess);
	glGetShaderInfoLog(fsHandle, sizeof(acLog), 0, acLog);
	if (iSuccess == GL_FALSE)
	{
		printf("Error: Failed to compile fragment shader!\n");
		printf(acLog);
		printf("\n");
	}

	g_Shader = glCreateProgram();
	glAttachShader(g_Shader, vsHandle);
	glAttachShader(g_Shader, fsHandle);
	glDeleteShader(vsHandle);
	glDeleteShader(fsHandle);

	// specify Vertex Attribs:
	glBindAttribLocation(g_Shader, 0, "Position");
	glBindAttribLocation(g_Shader, 1, "UV");
	glBindAttribLocation(g_Shader, 2, "Colour");
	glBindFragDataLocation(g_Shader, 0, "outColour");

	glLinkProgram(g_Shader);
	glGetProgramiv(g_Shader, GL_LINK_STATUS, &iSuccess);
	glGetProgramInfoLog(g_Shader, sizeof(acLog), 0, acLog);
	if (iSuccess == GL_FALSE)
	{
		printf("Error: failed to link Shader Program!\n");
		printf(acLog);
		printf("\n");
	}

	glUseProgram(g_Shader);

	auto* texData = ftexData.get();

	glGenTextures( 1, &g_Texture );
	glBindTexture( GL_TEXTURE_2D, g_Texture );
	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, 256, 256, 0, GL_RGBA, GL_FLOAT, texData);

	delete ptexData;

	// specify default filtering and wrapping
	glTexParameterf( GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameterf( GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	glTexParameterf( GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT );
	glTexParameterf( GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT );

	// set the texture to use slot 0 in the shader
	GLuint texUniformID = glGetUniformLocation(g_Shader,"diffuseTexture");
	glUniform1i(texUniformID,0);

	// Create VBO/IBO
	glGenBuffers(1, &g_VBO);
	glGenBuffers(1, &g_IBO);
	glBindBuffer(GL_ARRAY_BUFFER, g_VBO);
	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, g_IBO);

	// get the quad from the future:
	Quad temp = fQuad.get();

	glBufferData(GL_ARRAY_BUFFER, temp.c_uiNoOfVerticies * sizeof(Vertex), temp.m_Verticies, GL_STATIC_DRAW);
	glBufferData(GL_ELEMENT_ARRAY_BUFFER, temp.c_uiNoOfIndicies * sizeof(unsigned int), temp.m_uiIndicies, GL_STATIC_DRAW);

	// Now do window specific stuff, including:
	// --> Creating a VAO with the VBO/IBO created above!
	// --> Setting Up Projection and View Matricies!
	// --> Specifing OpenGL Options for the window!
	for (auto window : g_lWindows)
	{
		MakeContextCurrent(window);
		
		// Setup VAO:
		g_mVAOs[window->m_uiID] = 0;
		glGenVertexArrays(1, &(g_mVAOs[window->m_uiID]));
		glBindVertexArray(g_mVAOs[window->m_uiID]);
		glBindBuffer(GL_ARRAY_BUFFER, g_VBO);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, g_IBO);

		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);
		glEnableVertexAttribArray(2);
		glVertexAttribPointer(0, 4, GL_FLOAT, GL_FALSE, sizeof(Vertex), 0);
		glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, sizeof(Vertex), ((char*)0) + 16);
		glVertexAttribPointer(2, 4, GL_FLOAT, GL_FALSE, sizeof(Vertex), ((char*)0) + 24);

		// Setup Matrix:
		window->m_m4Projection = glm::perspective(45.0f, float(window->m_uiWidth)/float(window->m_uiHeight), 0.1f, 1000.0f);
		window->m_m4ViewMatrix = glm::lookAt(glm::vec3(window->m_uiID * 8,8,8), glm::vec3(0,0,0), glm::vec3(0,1,0));

		// set OpenGL Options:
		glViewport(0, 0, window->m_uiWidth, window->m_uiHeight);
		glClearColor(0.25f,0.25f,0.25f,1);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);

		// setup FPS Data
		FPSData* fpsData = new FPSData();
		fpsData->m_fFPS = 0;
		fpsData->m_fTimeBetweenChecks = 3.0f;	// calc fps every 3 seconds!!
		fpsData->m_fFrameCount = 0;
		fpsData->m_fTimeElapsed = 0.0f;
		fpsData->m_fCurrnetRunTime = (float)glfwGetTime();
		m_mFPSData[window->m_uiID] = fpsData;
	}

	std::cout << "Init completed on thread ID: " << std::this_thread::get_id() << std::endl;

	return EC_NO_ERROR;
}


int MainLoop()
{
	std::cout << "Entering main loop on thread ID: " << std::this_thread::get_id() << std::endl;

	while (!ShouldClose())
	{
		float fTime = (float)glfwGetTime();   // get time for this iteration

		glm::mat4 identity;
		g_ModelMatrix = glm::rotate(identity, fTime * 10.0f, glm::vec3(0.0f, 1.0f, 0.0f));

		// simulate work:
		if (g_bDoWork)
		{
			std::chrono::milliseconds dura( 6 );
			std::this_thread::sleep_for( dura );
		}

		// draw each window in sequence:
		for (const auto& window : g_lWindows)
		{
			MakeContextCurrent(window);
		
			// clear the backbuffer to our clear colour and clear the depth buffer
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glUseProgram(g_Shader);

			GLuint ProjectionID = glGetUniformLocation(g_Shader,"Projection");
			GLuint ViewID = glGetUniformLocation(g_Shader,"View");
			GLuint ModelID = glGetUniformLocation(g_Shader,"Model");

			glUniformMatrix4fv(ProjectionID, 1, false, glm::value_ptr(window->m_m4Projection));
			glUniformMatrix4fv(ViewID, 1, false, glm::value_ptr(window->m_m4ViewMatrix));
			glUniformMatrix4fv(ModelID, 1, false, glm::value_ptr(g_ModelMatrix));

			glActiveTexture(GL_TEXTURE0);
			glBindTexture( GL_TEXTURE_2D, g_Texture );
			glBindVertexArray(g_mVAOs[window->m_uiID]);
			glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

			glfwSwapBuffers(window->m_pWindow);  // make this loop through all current windows??

			// calc FPS:
			CalcFPS(window);
		}

		glfwPollEvents(); // process events!
	}

	std::cout << "Exiting main loop on thread ID: " << std::this_thread::get_id() << std::endl;

	return EC_NO_ERROR;
}


int MainLoopBAD()
{
	std::cout << "Entering main loop on thread ID: " << std::this_thread::get_id() << std::endl;

	MakeContextCurrent(g_hPrimaryWindow);

	while (!ShouldClose())
	{
		// Keep Running!
		// get delta time for this iteration:
		float fDeltaTime = (float)glfwGetTime();

		glm::mat4 identity;
		g_ModelMatrix = glm::rotate(identity, fDeltaTime * 10.0f, glm::vec3(0.0f, 1.0f, 0.0f));

		// render threaded.
		std::thread renderWindow2(&Render, g_hSecondaryWindow);
		Render(g_hPrimaryWindow);

		// calc FPS:
		CalcFPS(g_hSecondaryWindow);
		CalcFPS(g_hPrimaryWindow);

		// join second render thread
		renderWindow2.join();

		glfwPollEvents(); // process events!
	}

	std::cout << "Exiting main loop on thread ID: " << std::this_thread::get_id() << std::endl;

	return EC_NO_ERROR;
}


int MainLoopTHREADED()
{
	std::cout << "Entering main loop on thread ID: " << std::this_thread::get_id() << std::endl;

	// init the sync fece to something so the initial render pass for the main thread wuill work:
	g_SecondThreadFenceSync = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);

	g_bShouldClose = ShouldClose();
	while (!g_bShouldClose)
	{
		// Keep Running!
		// get delta time for this iteration:
		float fDeltaTime = (float)glfwGetTime();

		glm::mat4 identity;
		g_ModelMatrix = glm::rotate(identity, fDeltaTime * 10.0f, glm::vec3(0.0f, 1.0f, 0.0f));

		// simulate work:
		if (g_bDoWork)
		{
			std::chrono::milliseconds dura( 3 );
			std::this_thread::sleep_for( dura );
		}

		g_RenderLock.lock();
		glWaitSync(g_SecondThreadFenceSync, 0, GL_TIMEOUT_IGNORED);				// tell the GPU to make sure that the second threads calls are in the pipline before adding ours!
		glDeleteSync(g_SecondThreadFenceSync);
		Render(g_hPrimaryWindow);
		g_MainThreadFenceSync = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);	// setup our fence sync for the other thread to wait on it.
		g_RenderLock.unlock();

		// calc FPS:
		CalcFPS(g_hPrimaryWindow);

		glfwPollEvents(); // process events!
		g_bShouldClose = ShouldClose();  // check if we should close:

		// spin off the thread for window 2 thread if it hasn't alread been done:
		if (g_tpWin2 == nullptr)
		{
			g_tpWin2 = new std::thread(&ChildLoop, g_hSecondaryWindow);
		}
	}

	std::cout << "Exiting main loop on thread ID: " << std::this_thread::get_id() << std::endl;

	return EC_NO_ERROR;
}


void ChildLoop(WindowHandle a_toWindow)
{
	std::cout << "Starting Secondary Render Thread: " << std::this_thread::get_id() << std::endl;
	MakeContextCurrent(g_hSecondaryWindow);

	while(!g_bShouldClose)
	{
		if (g_MainThreadFenceSync == 0)
		{
			continue; // dont start rendering until the main thread has started rendering for the first time.
		}

		// simulate work:
		if (g_bDoWork)
		{
			std::chrono::milliseconds dura( 3 );
			std::this_thread::sleep_for( dura );
		}

		g_RenderLock.lock();
		glWaitSync(g_MainThreadFenceSync, 0, GL_TIMEOUT_IGNORED);		// tell the GPU to make sure that the second threads calls are in the pipline before adding ours!
		glDeleteSync(g_MainThreadFenceSync);
		Render(a_toWindow);
		g_SecondThreadFenceSync = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);	// setup our fence sync for the other thread to wait on it.
		g_RenderLock.unlock();

		// calc FPS:
		CalcFPS(g_hSecondaryWindow);
	}
}


void Render(WindowHandle a_toWindow)
{
	MakeContextCurrent(a_toWindow);
		
	// clear the backbuffer to our clear colour and clear the depth buffer
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

	glUseProgram(g_Shader);

	GLuint ProjectionID = glGetUniformLocation(g_Shader,"Projection");
	GLuint ViewID = glGetUniformLocation(g_Shader,"View");
	GLuint ModelID = glGetUniformLocation(g_Shader,"Model");

	glUniformMatrix4fv(ProjectionID, 1, false, glm::value_ptr(a_toWindow->m_m4Projection));
	glUniformMatrix4fv(ViewID, 1, false, glm::value_ptr(a_toWindow->m_m4ViewMatrix));
	glUniformMatrix4fv(ModelID, 1, false, glm::value_ptr(g_ModelMatrix));

	glActiveTexture(GL_TEXTURE0);
	glBindTexture( GL_TEXTURE_2D, g_Texture );
	glBindVertexArray(g_mVAOs[a_toWindow->m_uiID]);
	glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

	glfwSwapBuffers(a_toWindow->m_pWindow);  // make this loop through all current windows??

	//CheckForGLErrors("Render Error");
}


int ShutDown()
{
	// join the window2 thread and delete it:
	if (g_tpWin2 != nullptr)
	{
		g_tpWin2->join();
		delete g_tpWin2;
	}
	
	// delete the FPS data:
	for (auto itr : m_mFPSData)
	{
		delete itr.second;
	}

	// cleanup any remaining windows:
	for (auto& window :g_lWindows)
	{
		delete window->m_pGLEWContext;
		glfwDestroyWindow(window->m_pWindow);

		delete window;
	}

	// terminate GLFW:
	glfwTerminate();

	return EC_NO_ERROR;
}


GLEWContext* glewGetContext()
{
	//return g_hCurrentContext->m_pGLEWContext;
	std::thread::id thread = std::this_thread::get_id();

	return g_mCurrentContextMap[thread]->m_pGLEWContext;
}


void MakeContextCurrent(WindowHandle a_hWindowHandle)
{
	if (a_hWindowHandle != nullptr)
	{
		std::thread::id thread = std::this_thread::get_id();

		glfwMakeContextCurrent(a_hWindowHandle->m_pWindow);
		g_mCurrentContextMap[thread] = a_hWindowHandle;
	}
}


WindowHandle CreateWindow(int a_iWidth, int a_iHeight, const std::string& a_szTitle, GLFWmonitor* a_pMonitor, WindowHandle a_hShare)
{
	// save current active context info so we can restore it later!
	std::thread::id thread = std::this_thread::get_id();
	WindowHandle hPreviousContext = g_mCurrentContextMap[thread];

	// create new window data:
	WindowHandle newWindow = new Window();
	if (newWindow == nullptr)
		return nullptr;

	newWindow->m_pGLEWContext = nullptr;
	newWindow->m_pWindow = nullptr;
	newWindow->m_uiID = g_uiWindowCounter++;		// set ID and Increment Counter!
	newWindow->m_uiWidth = a_iWidth;
	newWindow->m_uiHeight = a_iHeight;

	// if compiling in debug ask for debug context:
#ifdef _DEBUG
	glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLU_TRUE);
#endif

	std::cout << "Creating window called " << a_szTitle.c_str() << " with ID " << newWindow->m_uiID << std::endl;

	// Create Window:
	if (a_hShare != nullptr) // Check that the Window Handle passed in is valid.
	{
		newWindow->m_pWindow = glfwCreateWindow(a_iWidth, a_iHeight, a_szTitle.c_str(), a_pMonitor, a_hShare->m_pWindow);  // Window handle is valid, Share its GL Context Data!
	}
	else
	{
		newWindow->m_pWindow = glfwCreateWindow(a_iWidth, a_iHeight, a_szTitle.c_str(), a_pMonitor, nullptr); // Window handle is invlad, do not share!
	}
	
	// Confirm window was created successfully:
	if (newWindow->m_pWindow == nullptr)
	{
		printf("Error: Could not Create GLFW Window!\n");
		delete newWindow;
		return nullptr;
	}

	// create GLEW Context:
	newWindow->m_pGLEWContext = new GLEWContext();
	if (newWindow->m_pGLEWContext == nullptr)
	{
		printf("Error: Could not create GLEW Context!\n");
		delete newWindow;
		return nullptr;
	}

	glfwMakeContextCurrent(newWindow->m_pWindow);   // Must be done before init of GLEW for this new windows Context!
	MakeContextCurrent(newWindow);					// and must be made current too :)
	
	// Init GLEW for this context:
	GLenum err = glewInit();
	if (err != GLEW_OK)
	{
		// a problem occured when trying to init glew, report it:
		printf("GLEW Error occured, Description: %s\n", glewGetErrorString(err));
		glfwDestroyWindow(newWindow->m_pWindow);
		delete newWindow;
		return nullptr;
	}
	
	// setup callbacks:
	// setup callback for window size changes:
	glfwSetWindowSizeCallback(newWindow->m_pWindow, GLFWWindowSizeCallback);

	 // setup openGL Error callback:
    if (GLEW_ARB_debug_output) // test to make sure we can use the new callbacks, they wer added as an extgension in 4.1 and as a core feture in 4.3
    {
            #ifdef _DEBUG
            glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);                        // this allows us to set a break point in the callback function, no point to it if in release mode.
            #endif
            glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, 0, NULL, GL_TRUE);        // tell openGl what errors we want (all).
            glDebugMessageCallback(GLErrorCallback, NULL);                        // define the callback function.
    }

	// add new window to the map and increment handle counter:
	g_lWindows.push_back(newWindow);

	// now restore previous context:
	MakeContextCurrent(hPreviousContext);

	return newWindow;
}


void CalcFPS(WindowHandle a_hWindowHandle)
{
	FPSData* data = m_mFPSData[a_hWindowHandle->m_uiID];
	if (data != nullptr)
	{
		data->m_fFrameCount++;
		data->m_fPreviousRunTime = data->m_fCurrnetRunTime;
		data->m_fCurrnetRunTime = (float)glfwGetTime();
		data->m_fTimeElapsed += data->m_fCurrnetRunTime - data->m_fPreviousRunTime;
		if (data->m_fTimeElapsed >= data->m_fTimeBetweenChecks)
		{
			data->m_fFPS = data->m_fFrameCount / data->m_fTimeElapsed;
			data->m_fTimeElapsed = 0.0f;
			data->m_fFrameCount = 0;
			std::cout << "Thread id: " << std::this_thread::get_id() << "  Window: " <<  a_hWindowHandle->m_uiID << " FPS = " << (int)data->m_fFPS << std::endl;
		}
	}
}


bool ShouldClose()
{
	for (const auto& window : g_lWindows)
	{
		if (glfwWindowShouldClose(window->m_pWindow))
		{
			return true;
		}
	}

	return false;
}


Quad CreateQuad()
{
	Quad geom;

	geom.m_Verticies[0].m_v4Position = glm::vec4(-2,0,-2,1);
	geom.m_Verticies[0].m_v2UV = glm::vec2(0,0);
	geom.m_Verticies[0].m_v4Colour = glm::vec4(0,1,0,1);
	geom.m_Verticies[1].m_v4Position = glm::vec4(2,0,-2,1);
	geom.m_Verticies[1].m_v2UV = glm::vec2(1,0);
	geom.m_Verticies[1].m_v4Colour = glm::vec4(1,0,0,1);
	geom.m_Verticies[2].m_v4Position = glm::vec4(2,0,2,1);
	geom.m_Verticies[2].m_v2UV = glm::vec2(1,1);
	geom.m_Verticies[2].m_v4Colour = glm::vec4(0,1,0,1);
	geom.m_Verticies[3].m_v4Position = glm::vec4(-2,0,2,1);
	geom.m_Verticies[3].m_v2UV = glm::vec2(0,1);
	geom.m_Verticies[3].m_v4Colour = glm::vec4(0,0,1,1);

	geom.m_uiIndicies[0] = 3;
	geom.m_uiIndicies[1] = 1;
	geom.m_uiIndicies[2] = 0;
	geom.m_uiIndicies[3] = 3;
	geom.m_uiIndicies[4] = 2;
	geom.m_uiIndicies[5] = 1;

	printf("Created quad on thread ID: %i\n", std::this_thread::get_id());

	return geom;
}


void GLFWErrorCallback(int a_iError, const char* a_szDiscription)
{
	printf("GLFW Error occured, Error ID: %i, Description: %s\n", a_iError, a_szDiscription);
}


void APIENTRY GLErrorCallback(GLenum /* source */, GLenum type, GLuint id, GLenum severity, GLsizei /* length */, const GLchar* message, void* /* userParam */)
{
	std::cout << "---------------------opengl-callback-start------------" << std::endl;
	std::cout << "Message: " << message << std::endl;
	std::cout << "Type: "; 
    switch (type) 
        {
    case GL_DEBUG_TYPE_ERROR:
		std::cout << "ERROR" << std::endl; 
        break;
    case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR:
		std::cout << "DEPRECATED_BEHAVIOR" << std::endl; 
        break;
    case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR:
		std::cout << "UNDEFINED_BEHAVIOR" << std::endl; 
        break;
    case GL_DEBUG_TYPE_PORTABILITY:
		std::cout << "PORTABILITY" << std::endl; 
        break;
    case GL_DEBUG_TYPE_PERFORMANCE:
		std::cout << "PERFORMANCE" << std::endl; 
        break;
    case GL_DEBUG_TYPE_OTHER:
		std::cout << "OTHER" << std::endl; 
        break;
    }

	std::cout << "ID: " << id << ", Severity: ";
    switch (severity)
        {
    case GL_DEBUG_SEVERITY_LOW:
		std::cout << "LOW" << std::endl; 
        break;
    case GL_DEBUG_SEVERITY_MEDIUM:
		std::cout << "MEDIUM" << std::endl; 
        break;
    case GL_DEBUG_SEVERITY_HIGH:
		std::cout << "HIGH" << std::endl; 
        break;
    }

	std::cout << "---------------------opengl-callback-end--------------" << std::endl;
}


void GLFWWindowSizeCallback(GLFWwindow* a_pWindow, int a_iWidth, int a_iHeight)
{
	// find the window data corrosponding to a_pWindow;
	WindowHandle window = nullptr;
	for (auto& itr : g_lWindows)
	{
		if (itr->m_pWindow == a_pWindow)
		{
			window = itr;
			window->m_uiWidth = a_iWidth;
			window->m_uiHeight = a_iHeight;
			window->m_m4Projection = glm::perspective(45.0f, float(a_iWidth)/float(a_iHeight), 0.1f, 1000.0f);
		}
	}

	std::thread::id thread = std::this_thread::get_id();
	WindowHandle previousContext = g_mCurrentContextMap[thread];
	MakeContextCurrent(window);
	glViewport(0, 0, a_iWidth, a_iHeight);
	MakeContextCurrent(previousContext);
}
