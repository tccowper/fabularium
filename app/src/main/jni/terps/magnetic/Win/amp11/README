
                                    amp11
                                   =======

                          (c) 1997-2000 Niklas Beisert

amp11 is an Audio-MPEG decoder distributed under the
GNU General Public License Version 2 (see file COPYING).

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.


features:
 -layers 1, 2 and 3
 -version 2 LSF and Fraunhofer IIS 2.5 VLSF extensions
 -clean, fast, portable and easy to use code
 -downsampling and downmixing
 -equalizer, volume and stereo control
 -direct from http decoding
 -can decode several streams at a time
 -tested with Windows 98, 95, Linux, DOS, Sun, Alpha,
   Watcom C++ 11, GNU C++ and Visual C++

   if you want to add features, have ported amp11 to a new compiler or
   platform, or want to use amp11 in a player, please contact me.


uses code/ideas from:
  -reference code (DIST10) by MPEG Software Simulation Group
  -maplay by Jeff Tsay, Tobias Bading et al. (Mikko Tommila)
  -mpg123 by Michael Hipp, Oliver Fromme et al.
     http://www.sfs.nphil.uni-tuebingen.de/~hipp/mpg123.html


how to compile the example code (amp11):

  Watcom C++:
    set the system at the top of makewc then
      wmake -f makewc

  Linux/DJGPP/Sun/Alpha:
    set the system at the top of makeunix then
      make -f makeunix

  Visual C++:
    i don't know, find out yourself.


command line parameters for amp11:

  you must specify files and/or URLs (http://...) which are to be played

  -q: quiet operation, no screen output
  -n: no sound
  -P: use proxy for http (no space before name!)
  -l: log http stream to file
  -o: write output to specified file (xxx.wav for wav file)
  -w: write to *.wav in current directory
  -m: mono
  -s: stereo
  -S: downmixed stereo
  -2: downsampling by 2
  -4: downsampling by 4
  -v: volume 1.0: normal
  -p: channel separation: -1.0:reverse stereo, 0.0:mono, 1.0:normal
  -c: center: -1.0:left, 0.0:mid, 1.0:right
  -b: balance: -1.0:left, 0.0:mid, 1.0:right
  -i: inverse stereo aka. shitty surround
  -f: pitch: -0.3:bass, 0.0:normal, 0.5:treble.
  -F: base frequency for -f. default: 1764
  -B: buffer size for Windows in 1152 samples, default: 4
  -R: Repeat
  -O: No shuffle


keys:

  q, x:   exit
  return: finish song
  n:      next song
  l:      last song
  space:  pause/unpause
  f, F:   forward 2s, 15s
  r, R:   rewind 2s, 15s
  +, -:   volume
  b, t:   bass, treble


how to use it:

  you should take a look at amp11.cpp and makeunix.

  defines for portability:
    FASTBITS: unaligned access is allowed, might be faster
    BIGENDIAN: the machine is a big endian machine
    GNUCI486: the machine is 486 and compiler is GNU C
    FDCTBEXT: asm speedup for synthesis (watcom c)
    FDCTDEXT: asm speedup for mp3 decode (watcom c)

  the decoder is a class, let's call it dec:
    ampegdecoder dec;

  to open the decoder use:
    dec.open(instream, freq, stereo, fmt, dwn, chn);
  binfile &instream: an opened binfile with the encoded data
  int &freq: will contain the frequency of the decoded data
  int &stereo: will be true if the decoded data is stereo (interleaved l/r)
  int fmt: format, 0: float, 1:little endian 16 bit
  int dwn: downsampling: 0: none, 1: by 2, 2: by 4
  int chn: channels: -2: 2 but downmixing, 0: as in mpeg, 1: 1, 2: 2

  at the end please call:
    dec.close();

  to read some data:
    n=dec.read(buf, len);
  int &n: number of bytes read
  void *buf: buffer to receive data
  int len: number of bytes to read

  to seek to a position:
    p=dec.seek(pos);
  int &p: new position
  int pos: position to seek to
    warning: it is not always possible to calculate the exact position,
      as well as the exact length. the returned new position is just an
      estimate. for example if you seek to the current position, you might
      end up somewhere else.

  you can set an equalizer for 32 or 576 bands:
    dec.ioctl(dec.ioctlsetequal32, equal32, 0);
    dec.ioctl(dec.ioctlsetequal576, equal576, 0);
  float equal32[32]:    per band (freq/64) amplification, 1: normal
  float equal576[576]:  per band (freq/1152) amplification, 1: normal

  you can set an amplification:
    dec.ioctl(dec.ioctlsetvol, &vol, 0);
  float &vol: volume, 1:normal

  you can modify the stereo output:
    dec.ioctl(dec.ioctlsetstereo, vols, 0);
  float vols[3][3]: volume matrix [dst][src]: [0]:left, [1]:right, [2]:mono.

  you can modify the seek mode:
    dec.ioctl(dec.ioctlseekmode, dec.seekmodeexact);
    dec.ioctl(dec.ioctlseekmode, dec.seekmoderelaxed);
    dec.ioctl(dec.ioctlseekmodeget);
  relaxed is faster than exact. relaxed does not correctly initialize
  the buffers that carry data across frames but leaves them unchanged
  instead. this should make seeking sound softer, but actually it does
  the opposite, so better not use seekmoderelaxed... :(

  these are just the basic functions, the other functions are explained
  in the binfile documentation. e.g.:
    dec.length();
    dec.tell();
    dec.eof();
    getil2(dec);


comments/bugs/todos:

  -file needs to start with an audio header (FF Fx)
    see revision history for details
  -problems with downsampling and the time display under DJGPP... why?
  -seek not tested much

revision history:

  -nb000315:
    -ID3v2 support. no promise. just a try. i have no examples.
      in case of malfunction, please provide me with one < 50k.
      Thanks to Zachary Bedell for his bugfix, although i did not use
      it. i simply don't like the idea of browsing through files
      and potentially ending up emptyhanded. or stepping right into
      a dog's **** ie. an innocent 0xFFF. who knows what will happen then?
      amp11 mistakes your favorite xxx-pic for an mpeg and goes
      OOHH and AAHH. and in the case of windows it'll probably have
      some fun with it as well. you see, not good.
      better: if it doesn't look like an MPwhatever and
      it doesn't taste like one either, then it's probably none.

      then again it's just my stupid ideology.

      i could make an ideology switch.
      on (me) or off (easy to use and with many features).
      but: lazyness prevents that.

      that's all for today. bye, folks.

      PS: actually the xxx-pic thingy would give a nice feature,
      don't you think so?

    -extended comment on tiny new feature.
    -forget that bullshit i wrote above. amp11.cpp does
      look for a 0xFFF header. i just found out, so please forgive me.
      i also found out that amp11 looks for an INFO header. what the
      heck is that?
    -nevermind.

  -nb990908:
    -now works in Visual C++
      Thanks to Alen Ladavac for the changes.
    -file closing fixed
    -mono in winnt fixed
    -writing multiple files to wav

  -nb990509:
    -greetings to The Lost Souls for their support (IMS)
    -seeking in MP3 fixed a little
    -keyboard control in amp11

  -nb980614:
    -there are several changes, but i don't know them anymore...
    -next release
    -soundblaster output under djgpp
      (thanks to }{JuJu}{ for example source)

  -nb221197:
    -first release


contact:
  nbeisert@ph.tum.de

www:
  http://www.ph.tum.de/~nbeisert/amp11.html
