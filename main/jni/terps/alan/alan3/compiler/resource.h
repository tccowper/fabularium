#ifndef _RESOURCE_H_
#define _RESOURCE_H_
/*----------------------------------------------------------------------*\

			      RESOURCE.H
		    Multimedia Resource Structure

\*----------------------------------------------------------------------*/

/* USE: */
#include "srcp.h"
#include "id.h"


/* TYPES: */

typedef enum {
  NULL_CHUNK,
  PNG_CHUNK,
  JPEG_CHUNK,
  FORM_CHUNK,
  MOD_CHUNK,
  SONG_CHUNK
} ChunkKind;

typedef enum {
  NULL_RESOURCE,
  PICT_RESOURCE,
  SND_RESOURCE
} ResourceKind;

typedef struct Resource {
  Srcp srcp;
  ResourceKind kind;
  ChunkKind chunk;
  Id *fileName;
} Resource;


#endif
