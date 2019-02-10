// binfile - class library for files/streams - windows nt/95 sound playback
// Copyright (c) 1997-2000 Niklas Beisert
// See COPYING (GNU General Public License 2) for license

#ifndef WIN32
#error must compile for WIN32
#endif
#include <string.h>
#include "binfplnt.h"

#ifdef _MSC_VER
#pragma comment(lib, "winmm.lib") // automatic linking to winmm.dll for visualc
#endif

int ntplaybinfile::open(int rate, int stereo, int bit16, int blen, int nb)
{
  close();

  blklen=blen*(stereo?2:1)*(bit16?2:1);
  nblk=nb;

  hdrs=new WAVEHDR[nblk];
  if (!hdrs)
    return -1;

  playbuf=new char [blklen*nblk];
  if (!playbuf)
    return -1;

  WAVEFORMATEX form;
  form.wFormatTag=WAVE_FORMAT_PCM;
  form.nChannels=stereo?2:1;
  form.nSamplesPerSec=rate;
  form.wBitsPerSample=bit16?16:8;
  form.nBlockAlign=form.nChannels*form.wBitsPerSample/8;
  form.nAvgBytesPerSec=form.nSamplesPerSec*form.nBlockAlign;
  form.cbSize=0;

  if (waveOutOpen(&wavehnd, WAVE_MAPPER, &form, 0, 0, 0))
    return -1;

  int i;
  for (i=0; i<nblk; i++)
    hdrs[i].dwFlags=0;
  curbuflen=0;

  blocking=1;
  linger=-1;

  openmode(modewrite, 0, 0);
  return 0;
}

errstat ntplaybinfile::rawclose()
{
  closemode();

  int i;
  while (1)
  {
    if (linger>=0)
      break;
    for (i=0; i<nblk; i++)
      if (!(hdrs[i].dwFlags&WHDR_DONE)&&(hdrs[i].dwFlags&WHDR_PREPARED))
        break;
    if (i==nblk)
      break;
    Sleep(50);
  }
  waveOutReset(wavehnd);

  for (i=0; i<nblk; i++)
    if (hdrs[i].dwFlags&WHDR_PREPARED)
      waveOutUnprepareHeader(wavehnd, &hdrs[i], sizeof(*hdrs));
  waveOutClose(wavehnd);

  delete hdrs;
  delete playbuf;
  return 0;
}

void ntplaybinfile::abort(void)
{
  closemode();
  reset();

  waveOutReset(wavehnd);
  waveOutClose(wavehnd);

  delete hdrs;
  delete playbuf;
}

binfilepos ntplaybinfile::rawwrite(const void *buf, binfilepos len)
{
  int l0=0;
  while (len)
  {
    if (!curbuflen)
    {
      int i;
      for (i=0; i<nblk; i++)
        if (hdrs[i].dwFlags&WHDR_DONE)
          waveOutUnprepareHeader(wavehnd, &hdrs[i], sizeof(*hdrs));
      for (i=0; i<nblk; i++)
        if (!(hdrs[i].dwFlags&WHDR_PREPARED))
          break;
      if (i==nblk)
      {
        if (!blocking)
          return l0;
        Sleep(20);
        continue;
      }
      curbuf=i;
    }
    int l=blklen-curbuflen;
    if (l>len)
      l=len;
    memcpy(playbuf+curbuf*blklen+curbuflen, buf, l);
    *(char**)&buf+=l;
    len-=l;
    l0+=l;
    curbuflen+=l;
    if (curbuflen==blklen)
    {
      hdrs[curbuf].lpData=playbuf+curbuf*blklen;
      hdrs[curbuf].dwBufferLength=blklen;
      hdrs[curbuf].dwFlags=0;
      waveOutPrepareHeader(wavehnd, &hdrs[curbuf], sizeof(*hdrs));
      waveOutWrite(wavehnd, &hdrs[curbuf], sizeof(*hdrs));
      curbuflen=0;
    }
  }
  return l0;
}

ntplaybinfile::ntplaybinfile()
{
}

ntplaybinfile::~ntplaybinfile()
{
  close();
}

binfilepos ntplaybinfile::rawioctl(intm code, void *buf, binfilepos len)
{
  binfilepos ret;
  switch (code)
  {
  case ioctlblocking:
    ret=blocking;
    blocking=len;
    return ret;
  case ioctlblockingget:
    return blocking;
  case ioctllingerget:
    return linger;
  case ioctllinger:
    ret=linger;
    linger=len;
    return ret;
  default:
    return binfile::rawioctl(code, buf, len);
  }
}

