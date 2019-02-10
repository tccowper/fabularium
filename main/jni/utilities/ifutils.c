/*
 * Copyright (C) 2017 Tim Cadogan-Cowper.
 *
 * This file is part of Fabularium.
 *
 * Fabularium is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Fabularium; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <setjmp.h>
#include <android/log.h>

/* Need dlfcn.h for the routines to
   dynamically load libraries */
#include <dlfcn.h>

#include "ifutils.h"

jmp_buf term_buf;

//jobject GameUtils_obj;

void cleanup(int argc, const char** argv, int sout, int serr) {
  for (int i = 0; i < argc; i++) {
    free(argv[i]);
  }
  free(argv);

  /* restore stdout and stderr */
  fflush(NULL);
  if (sout != -1) {
    dup2(sout, fileno(stdout));
    close(sout);
    sout = -1;
  }
  if (serr != -1) {
    dup2(serr, fileno(stderr));
    close(serr);
    serr = -1;
  }
}

JNIEXPORT jint JNICALL Java_com_luxlunae_fabularium_RunProgramService_runProgram(JNIEnv *e, jobject jc,
                                                              jstring progLibName, jobject gameUtils, jobjectArray args,
                                                              jstring outFilePath) {
    /* This is the main entry point for running a third-party utility binary
     * It will return zero upon success and non-zero upon failure. */
    int argc = (*e)->GetArrayLength(e, args);
    const char *argv[argc];
    const char *outFile = (*e)->GetStringUTFChars(e, outFilePath, 0);
    int i, ret;
    int sout, serr;
    const char *error;
    void *prog;
    p_main prog_main;

    /* redirect stdout and stderr to the logging file */
    sout = dup(fileno(stdout));
    serr = dup(fileno(stderr));
    if (freopen(outFile, "w", stdout) == NULL) {
      __android_log_write(ANDROID_LOG_ERROR, "ifutils.c", "RunProgramService: could not redirect stdout");
    }
    if (freopen(outFile, "a", stderr) == NULL) {
      __android_log_write(ANDROID_LOG_ERROR, "ifutils.c", "RunProgramService: could not redirect stderr.");
    }

    const char *libName = (*e)->GetStringUTFChars(e, progLibName, 0);

    /* Load dynamically loaded library */
    prog = dlopen(libName, RTLD_LAZY);

    (*e)->ReleaseStringUTFChars(e, progLibName, libName);

    if (!prog) {
     fprintf(stderr, "Couldn't open utils plugin: %s\n", dlerror());
     return 1;
    }
 
    /* Get symbols */
    dlerror();
    prog_main = dlsym(prog, "main");
    if ((error = dlerror())) {
     fprintf(stderr, "Couldn't find main function: %s\n", error);
     return 1;
    }

    /* Ensure the global environment variable is pointing to the terp thread */
  //  GameUtils_obj = (*e)->NewGlobalRef(e, gameUtils);
    
    /* Get the arguments */
    for (i = 0; i < argc; i++) {
        jstring arg = (jstring) ((*e)->GetObjectArrayElement(e, args, i));
        argv[i] = (*e)->GetStringUTFChars(e, arg, 0);
    }
    
    /* N.B. it's unsafe to assign the return value of setjmp - according to C99 spec
     * the below should work.
     * Thanks https://stackoverflow.com/questions/22893178/how-to-safely-get-the-return-value-of-setjmp
     * Also need to take care re any local variables that have changed between the setjmp call and the
     * longjmp call, in our case we only call cleanup() after the longjmp, which uses global variables
     * and hence should be safe */
    switch (setjmp(term_buf)) {
        case 0:
            /* Call the relevant terp - we assume it has already been loaded in Java
               through a call to LoadLibrary */
           ret = prog_main(argc, argv);
           break;
        case 1:
        default:
           ret = 1;
           break;
    }

    dlclose(prog);

    (*e)->ReleaseStringUTFChars(e, outFilePath, outFile);
    //(*e)->DeleteGlobalRef(e, GameUtils_obj);
    for (i = 0; i < argc; i++) {
        jstring arg = (jstring) ((*e)->GetObjectArrayElement(e, args, i));
        (*e)->ReleaseStringUTFChars(e, arg, argv[i]);
    }

    /* restore stdout and stderr */
    fflush(NULL);
    dup2(sout, fileno(stdout));
    dup2(serr, fileno(stderr));
    close(sout);
    close(serr);

    return (jint) ret;
}

void __wrap_exit(int status) {
    longjmp(term_buf, status ? 0 : 1);
}

