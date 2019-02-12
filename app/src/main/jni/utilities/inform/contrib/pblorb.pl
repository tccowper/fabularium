#!/usr/bin/perl
# ---------------------------------------------------------------------------
#  perlBlorb: a perl script for creating Blorb files
#  (c) Graham Nelson 1998
#
#  Distributed under the terms of the Artistic License 2.0
#
#  (c) David Griffith 2012-2015
#
#  Latest version is at https://github.com/DavidGriffith/blorbtools
# ---------------------------------------------------------------------------

use strict;
use File::Temp qw(tempfile tempdir);
use Encode qw(encode);
use Getopt::Long;
use Pod::Usage;

my $file_sep	= '/';		# Character used to separate directories in
				# pathnames (on most systems this will be /)

my $blurb_filename  = 'input.blurb';
my $output_filename = 'output.blb';
my $version = "perlBlorb 3.0, part of Blorbtools";
my $temp_dir = tempdir(CLEANUP => 1);

my $blurb_line = 0;
my $chunk_count = 0;
my $chunk_opened = 0;
my $important_count = 0;
my $total_size = 0;
my $max_resource_num = 0;
my $scalables = 0;
my $repeaters = 0;
my $next_pnum = 1;
my $next_snum = 3;

my $cover_seen = 0;

my $r_stdx = 600; my $r_stdy = 400;
my $r_minx = 0; my $r_maxx = 0;
my $r_miny = 0; my $r_maxy = 0;
my $resolution_on = 0;

my @p_picno;
my @p_stdp;
my @p_stdq;
my @p_minp;
my @p_maxp;
my @p_minq;
my @p_maxq;

my $chunk_filename;
my @chunk_filename_array;
my @chunk_important_array;
my @chunk_id_array;
my @chunk_number_array;
my @chunk_offset_array;
my @chunk_size_array;

my @looped_fx;
my @looped_num;
my @picture_numbering;
my @sound_numbering;

my %options;
GetOptions('usage|?'	=> \$options{usage},
	'h|help'	=> \$options{help},
	'n|nobuild'	=> \$options{nobuild}
	);


# ---------------------------------------------------------------------------

# ---------------------------------------------------------------------------

sub error
{   my $m = $_[0];
    print STDERR "$blurb_filename, line $blurb_line: Error: $m\n";
}

sub fatal
{   my $m = $_[0];
    die "$blurb_filename, line $blurb_line: Fatal error: $m\n";
}

# ---------------------------------------------------------------------------

sub four_word
{   my $n = $_[0];
    print CHUNK sprintf("%c%c%c%c", ($n / 0x1000000),
                                    ($n / 0x10000)%0x100,
                                    ($n / 0x100)%0x100,
                                    ($n)%0x100);
}

sub two_word
{   my $n = $_[0];
    print CHUNK sprintf("%c%c", ($n / 0x100),
                                  ($n)%0x100);
}

sub one_byte
{   my $n = $_[0];
    print CHUNK sprintf("%c", $n);
}

sub begin_chunk
{   my $id = $_[0];
    my $cnum = $_[1];
    my $chunk_filename = $_[2];
    $chunk_opened = 0;

    if ($cnum > $max_resource_num) { $max_resource_num = $cnum; }

    if ($chunk_filename eq "")
    {   $chunk_filename = sprintf('%s%s%d',
            $temp_dir, $file_sep, $chunk_count);
        open(CHUNK, sprintf(">%s",$chunk_filename))
            or fatal("unable to create temporary file $chunk_filename");
        binmode CHUNK;
        $chunk_opened = 1;
    }

    $chunk_filename_array[$chunk_count] = $chunk_filename;

    $chunk_important_array[$chunk_count] = 0;
    if (($id eq "PNG ") || ($id eq "JPEG") || ($id eq "GIF ")
	|| ($id eq "GFX ")
	|| ($id eq "AIFF") || ($id eq "MOD ") || ($id eq "OGGV")
	|| ($id eq "WAVE") || ($id eq "MIDI") || ($id eq "MP3 ")
	|| ($id eq "ZCOD") || ($id eq "GLUL") || ($id eq "MAGS")
	|| ($id eq "ADRI"))
    {   $chunk_important_array[$chunk_count] = 1;
        $important_count = $important_count + 1;
    }

    $chunk_id_array[$chunk_count] = $id;
    $chunk_number_array[$chunk_count] = $cnum;
    $chunk_offset_array[$chunk_count] = $total_size;
}

sub end_chunk
{   my ($size, $blen, $buffer);

    if ($chunk_opened == 1)
    {   close(CHUNK);
    }

    $chunk_filename = $chunk_filename_array[$chunk_count];

    open(CHUNK, $chunk_filename)
        or fatal("unable to open $chunk_filename for size counting");
    binmode(CHUNK);

    for ($size = 0, $blen = 1; $blen > 0; )
    {   $blen = read(CHUNK, $buffer, 1024);
        $size = $size + $blen;
    }

    close(CHUNK);

    if ($chunk_id_array[$chunk_count] ne "AIFF") {
        $size = $size + 8;
    }

    $chunk_size_array[$chunk_count] = $size;

    # Pad chunk to an even number of bytes
    if ($size % 2 == 1) {
	$size = $size + 1;
    }

    $total_size = $total_size + $size;

    $chunk_count++;
}

sub author_chunk
{   my $t = $_[0];
    begin_chunk("AUTH", 0, "");
    print CHUNK $t;
    end_chunk();
}

sub copyright_chunk
{   my $t = $_[0];
    begin_chunk("(c) ", 0, "");
    print CHUNK $t;
    end_chunk();
}

sub release_chunk
{   my $t = $_[0];
    begin_chunk("RelN", 0, "");
    two_word($t);
    end_chunk();
}

sub palette_simple_chunk
{   my $t = $_[0];
    begin_chunk("Plte", 0, "");
    one_byte($t);
    end_chunk();
}

sub frontispiece_chunk
{   my $t = $_[0];
    begin_chunk("Fspc", 0, "");
    four_word($t);
    end_chunk();
    identify("PICTURE_cover", $t);
}

sub storyname_chunk
{   my $t = $_[0];
    my $foo = encode("UTF16-BE", $t);
    begin_chunk("SNam", 0, "");
    print CHUNK $foo;
    end_chunk();
}

# ---------------------------------------------------------------------------

# The mod file formats listed here are the ones supported by libmodplug,
# which is the mod player library used in Unix Frotz and Windows Frotz.
#
sub ismod
{
    my $ext = $_[0];

    if ($ext eq "mod") { return 1; }
    if ($ext eq "xm")  { return 1; }
    if ($ext eq "it")  { return 1; }
    if ($ext eq "s3m") { return 1; }
    if ($ext eq "669") { return 1; }
    if ($ext eq "amf") { return 1; }
    if ($ext eq "ams") { return 1; }
    if ($ext eq "dbm") { return 1; }
    if ($ext eq "dmf") { return 1; }
    if ($ext eq "dsm") { return 1; }
    if ($ext eq "far") { return 1; }
    if ($ext eq "j2b") { return 1; }
    if ($ext eq "mdl") { return 1; }
    if ($ext eq "mt2") { return 1; }
    if ($ext eq "mtm") { return 1; }
    if ($ext eq "otk") { return 1; }
    if ($ext eq "psm") { return 1; }
    if ($ext eq "ptm") { return 1; }
    if ($ext eq "stm") { return 1; }
    if ($ext eq "utl") { return 1; }
    if ($ext eq "umx") { return 1; }
    if ($ext eq "fnk") { return 1; } # libmodplug doesn't support this one.
    return 0;
}



# ---------------------------------------------------------------------------

sub identify
{   print STDOUT "Constant $_[0] = $_[1];\n";
}

sub interpret
{   my $command = $_[0];
    my $rest;

    $blurb_line++;

    if ($command =~ /^\s*\!/)
    {   # This is a comment line
        return;
    }
    if ($command =~ /^\s*$/m)
    {   # This is a blank line
        return;
    }
    if ($command =~ /^\s*copyright\s+"(.*)"/)
    {   copyright_chunk($1);
        return;
    }
    if ($command =~ /^\s*release\s+(\d*)/)
    {   release_chunk($1);
        return;
    }
    if ($command =~ /^\s*resolution\s+(\d*)x(\d*)\s*(.*)$/m)
    {   $r_stdx = $1; $r_stdy = $2;
        $r_minx = 0; $r_maxx = 0;
        $r_miny = 0; $r_maxy = 0;

        $resolution_on = 1;

        $rest = $3;
        if ($rest =~ /^\s*min\s+(\d*)x(\d*)\s*$/m)
        {   $r_minx = $1;
            $r_miny = $2;
            return;
        }
        if ($rest =~ /^\s*max\s+(\d*)x(\d*)\s*$/m)
        {   $r_maxx = $1;
            $r_maxy = $2;
            return;
        }
        if ($rest =~ /^\s*min\s+(\d*)x(\d*)\s*max\s+(\d*)x(\d*)\s*$/m)
        {   $r_minx = $1;
            $r_miny = $2;
            $r_maxx = $3;
            $r_maxy = $4;
            return;
        }
        if ($rest =~ /^\s*$/m)
        {   return;
        }
    }
    if ($command =~ /^\s*palette\s+(\d*)\s*bit/)
    {   if (($1 == 16) || ($1 == 32))
        {   palette_simple_chunk($1);
            return;
        }
        error("palette can only be 16 or 32 bit");
        return;
    }
    if ($command =~ /^\s*palette\s*\{(.*)$/m)
    {   $rest = $1;
        begin_chunk("Plte", 0, "");
        while (not($rest =~ /^\s*\}/))
        {   if ($rest =~ /^\s*$/m)
            {   $rest = <BLURB> or fatal("end of blurb file in 'palette'");
                $blurb_line = $blurb_line + 1;
            }
            else
            {   if ($rest =~
            /^\s*([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})\s*(.*)$/m)
                {   $rest = $4;
                    one_byte(hex($1));
                    one_byte(hex($2));
                    one_byte(hex($3));
                }
                else
                {   $rest =~ /^\s*(\S+)\s*(.*)$/m;
                    error("palette entry not six hex digits: $1");
                    $rest = $2;
                }
            }
        }
        end_chunk();
        return;
    }
    if ($command =~ /^\s*storyname\s+"(.*)"\s*$/m)
    {	storyname_chunk($1);
	return;
    }
    if ($command =~ /^\s*storyfile\s+"(.*)"\s+include\s*$/m)
    {   my $filename = $1;
	my $ext = ($filename =~ m/([^.]+)$/)[0];
	if ($ext =~ m/z[1-8]/) {
	    begin_chunk("ZCOD", 0, $filename);
	} elsif ($ext eq "ulx") {
	    begin_chunk("GLUL", 0, $filename);
	} elsif ($ext eq "taf") {
	    begin_chunk("ADRI", 0, $filename);
	} elsif ($ext eq "mag") {
	    begin_chunk("MAGS", 0, $filename);
	} else {
	    fatal("unknown executable extension $ext");
	}
	end_chunk();
        return;
    }
    # Do we need to generate an IFhd chunk?
    if ($command =~ /^\s*storyfile\s+"(.*)"/)
    {	my $buffer;
 	open(IDFILE, $1) or fatal("unable to open story file $1");
        binmode(IDFILE);
        begin_chunk("IFhd", 0, "");
        $version = unpack("C", getc(IDFILE));
        print STDOUT "! Identifying v$version story file $1\n";

        read IDFILE, $buffer, 1;
        one_byte(unpack("C",getc(IDFILE)));
        one_byte(unpack("C",getc(IDFILE)));
        read IDFILE, $buffer, 14;
        one_byte(unpack("C",getc(IDFILE)));
        one_byte(unpack("C",getc(IDFILE)));
        one_byte(unpack("C",getc(IDFILE)));
        one_byte(unpack("C",getc(IDFILE)));
        one_byte(unpack("C",getc(IDFILE)));
        one_byte(unpack("C",getc(IDFILE)));
        read IDFILE, $buffer, 4;
        one_byte(unpack("C",getc(IDFILE)));
        one_byte(unpack("C",getc(IDFILE)));
        one_byte(0);
        one_byte(0);
        one_byte(0);
        end_chunk();
        close(IDFILE);
        return;
    }

    # Generate Pict chunks
    if ($command =~ /^\s*picture\s+([a-zA-Z_0-9]*)\s*"(.*)"\s*(.*)$/m ||
	$command =~ /^\s*cover\s+"(.*)"\s*$/m)
    {   my $pnumt = $1;
	my $pfile = $2;
	my $rest = $3;
	my $pnum;

	my $ext = ($pfile =~ m/([^.]+)$/)[0];

        if ($pnumt =~ /^\d+$/m)
        {   $pnum = $pnumt;
            if ($pnum < $next_pnum)
            {   error("picture number must be >= $next_pnum to avoid clash");
            }
            else
            {   $next_pnum = $pnum + 1;
            }
        }
        elsif ($command =~ /^\s*cover\s+"(.*)"\s*$/m)
	{
	    if ($cover_seen) { fatal("Only one 'cover' command allowed");}
	    $cover_seen = 1;
	    $pnum = $next_pnum;
	    $next_pnum++;
	    $pfile = $pnumt;
	    $ext = ($pfile =~ m/([^.]+)$/)[0];
	    frontispiece_chunk($pnum);
	}
	else
        {   $pnum = $next_pnum;
            $next_pnum = $next_pnum + 1;
            if ($pnumt ne "")
            {   identify("PICTURE_$pnumt", $pnum);
            }
	    else
	    {   fatal("picture resource is missing an ID");
	    }
        }

	if ($ext eq "jpg" or $ext eq "jpeg")
	{   begin_chunk("JPEG", $pnum, $pfile);
	    end_chunk();
	} elsif ($ext eq "png") {
	    begin_chunk("PNG ", $pnum, $pfile);
	    end_chunk();
	} elsif ($ext eq "gif") {
	    begin_chunk("GIF ", $pnum, $pfile);
	    end_chunk();
	} elsif ($ext eq "gfx") {
	    begin_chunk("GFX ", $pnum, $pfile);
	    end_chunk();
	} else {
	    fatal("Unknown picture type");
	}

        if ($rest =~ /^\s*$/m)
        {   return;
        }

        $scalables = $scalables + 1;
        $resolution_on = 1;

        $p_picno[$scalables] = $pnum;
        $p_stdp[$scalables] = 1; $p_stdq[$scalables] = 1;
        $p_minp[$scalables] = -1; $p_maxp[$scalables] = -1;
        $p_minq[$scalables] = -1; $p_maxq[$scalables] = -1;

        if ($rest =~ /^\s*scale\s+(\d*)\/(\d*)\s*$/m)
        {   $p_stdp[$scalables] = $1;
            $p_stdq[$scalables] = $2;
            return;
        }
        if ($rest =~ /^\s*scale\s+max\s*(\d*)\/(\d*)\s*$/m)
        {   $p_maxp[$scalables] = $1;
            $p_maxq[$scalables] = $2;
            return;
        }
        if ($rest =~ /^\s*scale\s+min\s*(\d*)\/(\d*)\s*$/m)
        {   $p_minp[$scalables] = $1;
            $p_minq[$scalables] = $2;
            return;
        }
        if ($rest =~
            /^\s*scale\s+min\s*(\d*)\/(\d*)\s+max\s*(\d*)\/(\d*)\s*$/m)
        {   $p_minp[$scalables] = $1;
            $p_minq[$scalables] = $2;
            $p_maxp[$scalables] = $3;
            $p_maxq[$scalables] = $4;
            return;
        }

        if ($rest =~ /^\s*scale\s*(\d*)\/(\d*)\s*max\s*(\d*)\/(\d*)\s*$/m)
        {   $p_stdp[$scalables] = $1;
            $p_stdq[$scalables] = $2;
            $p_maxp[$scalables] = $3;
            $p_maxq[$scalables] = $4;
            return;
        }
        if ($rest =~ /^\s*scale\s*(\d*)\/(\d*)\s*min\s*(\d*)\/(\d*)\s*$/m)
        {   $p_stdp[$scalables] = $1;
            $p_stdq[$scalables] = $2;
            $p_minp[$scalables] = $3;
            $p_minq[$scalables] = $4;
            return;
        }
        if ($rest =~
  /^\s*scale\s*(\d*)\/(\d*)\s*min\s*(\d*)\/(\d*)\s+max\s*(\d*)\/(\d*)\s*$/m)
        {   $p_stdp[$scalables] = $1;
            $p_stdq[$scalables] = $2;
            $p_minp[$scalables] = $3;
            $p_minq[$scalables] = $4;
            $p_maxp[$scalables] = $5;
            $p_maxq[$scalables] = $6;
            return;
        }
    }

    # Generate Snd chunks
    if ($command =~ /^\s*sound\s+([a-zA-Z_0-9]*)\s*"(.*)"\s*(.*)$/m)
    {   my $snum;
	my $snumt = $1;
        my $fxfile = $2;
        my $repeats = $3;

	my $ext = ($fxfile =~ m/([^.]+)$/)[0];

	if ($snumt eq "")
	{   fatal("sound resource is missing an ID");
	}

        if ($snumt =~ /^\d+$/m)
        {   $snum = $snumt;
            if ($snum < $next_snum)
            {   error("sound number must be >= $next_snum to avoid clash");
            }
            else
            {   $next_snum = $snum + 1;
            }
        }
        else
        {   $snum = $next_snum;
            $next_snum = $next_snum + 1;
            if ($snumt ne "")
            {   identify("SOUND_$snumt", $snum);
            }
        }

        if (ismod($ext))
        {   begin_chunk("MOD ", $snum, $fxfile);
	    end_chunk();
            return;
        }

        if ($ext eq "ogg")
        {   begin_chunk("OGGV", $snum, $fxfile);
	    end_chunk();
	    return;
	}

	if ($ext eq "aiff")
	{   begin_chunk("AIFF", $snum, $fxfile);
	    end_chunk();
	}

	if ($ext eq "wav")
	{   begin_chunk("WAVE", $snum, $fxfile);
	    end_chunk();
	}

	if ($ext eq "mp3")
	{   begin_chunk("MP3 ", $snum, $fxfile);
	    end_chunk();
	}

	if ($ext eq "mid")
	{   begin_chunk("MIDI", $snum, $fxfile);
	    end_chunk();
	}

        if ($repeats =~ /^repeat\s+forever\s*$/m)
        {   $looped_fx[$repeaters] = $snum;
            $looped_num[$repeaters] = 0;
            $repeaters = $repeaters + 1;
            return;
        }

        if ($repeats =~ /^repeat\s+(\d*)\s*$/m)
        {   $looped_fx[$repeaters] = $snum;
            $looped_num[$repeaters] = $1;
            $repeaters = $repeaters + 1;
            return;
        }

        if ($repeats eq "") { return; }
    }

    $command =~ m/^\s*(\S+)\s*(.*)$/m;

    if (($1 eq "copyright") || ($1 eq "palette") || ($1 eq "picture")
        || ($1 eq "release") || ($1 eq "resolution") || ($1 eq "sound")
        || ($1 eq "storyfile"))
    {   error("incorrect syntax for $1 command");
        return;
    }

    error("no such blurb command: $1");
} # sub interpret

# ---------------------------------------------------------------------------

# These variables only used in the main routine
my $c;
my $x;
my $type;
my $pcount = 0;
my $scount = 0;
my $portion;
my $past_idx_offset;
my $iff_size;

my ($sec,$min,$hour,$mday,$month,$year,$wday,$yday,$isdst) = localtime(time);

my $blorbdate = sprintf("%02d%02d%02d at %02d:%02d.%02d",
                 $year, $month + 1, $mday, $hour, $min, $sec);

print STDOUT "! $version [executing on $blorbdate]\n";
print STDOUT "! The blorb spell (safely protect a small object ";
print STDOUT "as though in a strong box).\n";


pod2usage(1) if $options{usage};
pod2usage(-verbose => 3) if $options{help};

if ($ARGV[0]) {
	$blurb_filename = $ARGV[0];
}

if ($ARGV[1]) {
	$output_filename = "$ARGV[1]";
}

author_chunk("$version on $blorbdate");

if ($options{nobuild})
{
    if ($^O eq 'MSWin32' || $^O eq 'MSWin64')
    {
        $output_filename = "nul";
    } else {
	$output_filename = "/dev/null";
    }
}

open (BLURB, $blurb_filename)
    or fatal("can't open blurb file $blurb_filename");

while ($c = <BLURB>)
{
    interpret($c);
}

close BLURB;

if ($resolution_on == 1)
{
    begin_chunk("Reso", 0, "");
    four_word($r_stdx);
    four_word($r_stdy);
    four_word($r_minx);
    four_word($r_miny);
    four_word($r_maxx);
    four_word($r_maxy);

    for ($x=1; $x<=$scalables; $x=$x+1)
    {   four_word($p_picno[$x]);
        four_word($p_stdp[$x]);
        four_word($p_stdq[$x]);

        if ($p_minp[$x] == -1)
        {   $p_minp[$x] = $p_stdp[$x]; $p_minq[$x] = $p_stdq[$x]; }

        if ($p_maxp[$x] == -1)
        {   $p_maxp[$x] = $p_stdp[$x]; $p_maxq[$x] = $p_stdq[$x]; }

        four_word($p_minp[$x]);
        four_word($p_minq[$x]);
        four_word($p_maxp[$x]);
        four_word($p_maxq[$x]);
    }
    end_chunk();
}

if ($repeaters > 0)
{   begin_chunk("Loop", 0, "");
    for ($x=0; $x<$repeaters; $x = $x + 1)
    {   four_word($looped_fx[$x]);
        four_word($looped_num[$x]);
    }
    end_chunk();
}

# ---------------------------------------------------------------------------

# Calculate the IFF file size
$past_idx_offset = 12 + 12 + 12 * $important_count;
$iff_size = $past_idx_offset + $total_size;

# Now construct the IFF file from the chunks

open(CHUNK, ">$output_filename")
    or fatal("unable to open $output_filename for output");
binmode(CHUNK);

print CHUNK "FORM";
four_word($iff_size - 8);
print CHUNK "IFRS";

# Resource Index chunk
print CHUNK "RIdx";
four_word(4 + $important_count*12);
four_word($important_count);

for ($x = 0; $x < $chunk_count; $x = $x + 1)
{   if ($chunk_important_array[$x] == 1)
    {   $type = $chunk_id_array[$x];
	if (($type eq "PNG ") || ($type eq "JPEG") || ($type eq "GIF ")
		|| ($type eq "GFX "))
	{   $type = "Pict";
	}
        if (($type eq "AIFF") || ($type eq "MOD ") || ($type eq "OGGV")
		|| ($type eq "MP3 "))
        {   $type = "Snd ";
        }
	if (($type eq "ZCOD") || ($type eq "GLUL") || ($type eq "MAGS")
		|| ($type eq "ADRI"))
	{   $type = "Exec";
	}
        print CHUNK $type;
        four_word($chunk_number_array[$x]);
        four_word($past_idx_offset + $chunk_offset_array[$x]);
    }
}

for ($x = 0; $x <= $max_resource_num; $x = $x + 1)
{   $picture_numbering[$x] = -1;
    $sound_numbering[$x] = -1;
}

for ($x = 0; $x < $chunk_count; $x = $x + 1)
{   $type = $chunk_id_array[$x];
    if (($type eq "PNG ") || ($type eq "JPEG") || ($type eq "GIF ")
		|| ($type eq "GFX "))
    {   $picture_numbering[$chunk_number_array[$x]] = $x;
        $pcount = $pcount + 1;
    }
    if (($type eq "AIFF") || ($type eq "MOD ") || ($type eq "OGGV")
		|| ($type eq "WAVE") || ($type eq "MP3 "))
    {   $sound_numbering[$chunk_number_array[$x]] = $x;
        $scount++;
    }
    if ($type ne "AIFF") {
        print CHUNK $type;
        four_word(($chunk_size_array[$x]) - 8);
    }

#print "type: $type  size: ". $chunk_size_array[$x]."\n";

    $chunk_filename = $chunk_filename_array[$x];
    open(CHUNKSUB, $chunk_filename)
        or fatal("unable to read data from $chunk_filename");
    binmode(CHUNKSUB);

    while(read CHUNKSUB, $portion, 16384) {
	print CHUNK $portion;
    }
    close(CHUNKSUB);

    if (($chunk_size_array[$x] % 2) == 1) {
	printf CHUNK sprintf("%c", 0);
    }
}

close(CHUNK);

print STDOUT "! Blorb file data written to $output_filename\n";
print STDOUT "! Completed: size $iff_size bytes ";
print STDOUT "($pcount pictures, $scount sounds)\n";


# ---------------------------------------------------------------------------

__END__

=head1 NAME

pblorb.pl - Generate a blorb file according to a supplied blurb file

=head1 SYNOPSIS

B<pblorb.pl> - [-n] <story.blurb> [<output.blorb>]

Use -h or --help for verbose help.

=head1 DESCRIPTION

The blorb spell safely protects a small object as though in a strong box.

B<pblorb.pl> generates a blorb file according to the supplied blurb file.

=head1 OPTIONS
	B<-?>            Print simple usage message.
	B<-h --help>     Print verbose help message.
	B<-n --nobuild>  Don't build a blorb.  Just parse the blurb file.
	B< >

=head1 APPLICATION

A blorb file is an IFF (Interchange File Format) file that wraps up
executables, sound, graphics, and other resources into a single file for
use with interactive fiction game interpreters.  The format was
originally conceived for use with Z-machine and Glulx interpreters, but
nothing particularly limits it use to these two.  This script also
provides support for building blorb files for use with ADRIFT and
Magnetic Scrolls interpreters.

A blurb file is a text file that describes the contents of the
soon-to-be-built blorb file.  The blurb is given to pblorb.pl at the
command line which is then interpreted.  A blorb is then created
containing the files specified along with any non-file information
given.

=head1 GRAMMAR

This section is intended as a quick reference on blurb grammar.  A full
description can be found at Andrew Plotkin's website (see below).  Blank
lines are ignored.  The comment character is '!'.  Everything past that
character is ignored.  Each command describes a chunk to be added to the
Blorb file.

=over

=item author <string>

Adds this author name to the file.

=item copyright <string>

Adds this copyright declaration to the blorb file.  Normally this is
short text like "(c) J.Mango Pineapple 2007" rather than a lengthy legal
discorse.

=item release <number>

Give this release number to the blorb file

=item auxiliary <filename> <string>

Tells the interpreter that an auxiliary file - for instance, a PDF
manual - is associated with the release but will not be embedded
directly into the blorb file.

=item ifiction <filename> include

Include an XML file containing a valid iFiction record for this work.

=item storyfile	<filename>

=item storyfile	<filename> include

Specifies the filename of the story file.  If the "include" option is
used, the story file will be embedded in the blorb file.

=item palette 16 bit

=item palette 32 bit

=item palette {<colour-1> <colour-N>}

Signal the interpreter which colour scheme is in use.  The first two
options suggest that the pictures are best displayed using at least
16-bit or 32-bit colours.  The third specifies colours used in the
pictures in terms of red/green/blue levels, and the braces allow the
sequence of colours to continue over many lines.  At least one and at
most 256 colours may be defined in this way.  This is only a
"suggestion" to the interpreter.  Only meaningful for Z-machine V6.

=item resolution <dim>

=item resolution <dim> min <dim>

=item resolution <dim> max <dim>

=item resolution <dim> min <dim> max <dim>

Signal the interpreter the preferred screen size in real pixels.  The
minimum and maximum values are the extremes at which the designer thinks
the game will be playable.  These are optional with default values being
0 x 0 and infinity x infinity.  Only meaningful for Z-machine V6.

=item sound <id> <filename>

=item sound <id> <filename> repeat <number>

=item sound <id> <filename> repeat forever

Take the named sound file and make it a sound effect with the given ID.
The ID may be an integer (starting with 3) or a string.  If a string is
provided, pblorb.pl will emit a Inform6 constant declaration associating
that string with an automatically assigned number.  This allows the
author to refer to "SOUND_buzzer" instead of "4".  The repeat
information is only meaningful for Z-machine V3.

=item picture <id> <filename>

=item picture <id> <filename> scale <ratio>

=item picture <id> <filename> scale min <ratio>

=item picture <id> <filename> scale <ratio> min <ratio>

Take the named image file and make it a picture with the given ID.  The
ID rules are the same as with sounds except pictures may start at 1.
Scales are expressed as fractions, so "scale 3/1" means "Always display
three times its normal size.".  "scale num 1/10 max 8/1" means "Display
this anywhere between one tenth normal size and eight times normal size,
but if possible it ought to be just its normal size.".

=item cover <filename>

Includes this image file as a picture resource marked as "cover art".

=back

=head1 NOTES

This program complies the Blorb Standard version 2.0.4 and the Treaty of
Babel revision 9.

The Blorb Format was created by Andrew Plotkin in 1998.  For more
information, see L<http://www.eblong.com/zarf/blorb/>

For information on the Treaty of Babel, see
L<http://babel.ifarchive.org/>

For more information on IFF (Interchange File Format), see
L<https://en.wikipedia.org/wiki/Interchange_File_Format>

=head1 AUTHORS
    (c) Graham Nelson  1998 (original script to v1.03)
    (c) David Griffith 2015

=cut
