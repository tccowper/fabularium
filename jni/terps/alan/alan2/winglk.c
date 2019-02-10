/*
 * Windows MFC GLK Libraries
 * Startup code for Glk applications
 */

#include <windows.h>
#include "Glk.h"
#include "WinGlk.h"

int InitGlk(unsigned int iVersion);

/* Entry point for all Glk applications */
int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow)
{
  /* Attempt to initialise Glk */
  if (InitGlk(0x00000601) == 0)
    exit(0);

  /* Call the Windows specific initialization routine */
  if (winglk_startup_code(lpCmdLine) != 0)
  {
    /* Run the application */
    glk_main();

    /* There is no return from this routine */
    glk_exit();
  }

  return 0;
}


