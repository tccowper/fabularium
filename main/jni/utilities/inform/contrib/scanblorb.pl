#!/usr/bin/perl -w
# ---------------------------------------------------------------------------
#  scanBlorb: a perl script for scanning Blorb files
#  (c) Graham Nelson 1998
#
#  Distributed under the terms of the Artistic License 2.0
#
#  (c) Richard Poole  2004
#  (c) David Griffith 2015
#
#  Latest version is at https://github.com/DavidGriffith/blorbtools
# ---------------------------------------------------------------------------


use strict;
use Getopt::Long;
use Pod::Usage;
use Encode qw(decode);

require 5.6.0;

my $buffer;
my %options;
my %images;
my %sounds;
my %execs;

my $imagecount;
my $execcount;
my $soundcount;

my $xmlcount = 0;
my $resocount = 0;
my $apalcount = 0;
my $loopcount = 0;

GetOptions('usage|?'	=> \$options{usage},
	'h|help'	=> \$options{help},
	'i|images'	=> \$options{images},
	's|sound'	=> \$options{sound},
	'e|exec'	=> \$options{exec},
	'x|xml'		=> \$options{xml},
	'a|all'		=> \$options{all}
	);

my $input_filename = $ARGV[0];
my $output_filename;


pod2usage(1) if $options{usage};
pod2usage(-verbose => 3) if $options{help};
pod2usage(1) if !$input_filename;



if ($options{all}) {
	$options{images} = 1;
	$options{sound} = 1;
	$options{exec} = 1;
	$options{xml} = 1;
}

sub array_diff(\@\@);

my $version = "scanBlorb 3.0, part of Blorbtools";

my ($sec,$min,$hour,$mday,$month,$year) = (localtime(time))[0, 1, 2, 3, 4, 5];

my $blorbdate = sprintf("%04d/%02d/%02d at %02d:%02d.%02d",
                 $year + 1900, $month + 1, $mday, $hour, $min, $sec);

my @adrift_magic_380 = (0x3c, 0x42, 0x3f, 0xc9,
			0x6a, 0x87, 0xc2, 0xcf,
			0x94, 0x45, 0x36, 0x61);

my @adrift_magic_390 = (0x3c, 0x42, 0x3f, 0xc9,
			0x6a, 0x87, 0xc2, 0xcf,
			0x94, 0x45, 0x37, 0x61);

my @adrift_magic_400 = (0x3c, 0x42, 0x3f, 0xc9,
			0x6a, 0x87, 0xc2, 0xcf,
			0x93, 0x45, 0x3e, 0x61);

my @adrift_magic_500 = (0x3c, 0x42, 0x3f, 0xc9,
			0x6a, 0x87, 0xc2, 0xcf,
			0x92, 0x45, 0x3e, 0x61);

print STDOUT "$version [executing on $blorbdate]\n\n";

open (BLORB, $input_filename) or die "Can't load $input_filename.";
binmode(BLORB);

read BLORB, $buffer, 12;

my ($groupid, $length, $type) = unpack("NNN", $buffer);

if (!$groupid || ($groupid != 0x464F524D)) { die "Not a valid FORM file!\n";}

$type == 0x49465253 or die "Not a Blorb file!\n";

print "File length is apparently $length bytes\n";

my ($size, $pos);

for($pos = 12; $pos < $length; $pos += $size + ($size % 2) + 8) {
	my $chunkdata;

	read(BLORB, $buffer, 8) == 8
		or die("Incomplete chunk header at $pos\n");

	$size = (unpack("NN", $buffer))[1]; # second word of header
	my $type = substr($buffer, 0, 4);
	printf "%06x: $type chunk with $size bytes of data\n", $pos;

	read(BLORB, $chunkdata, $size) == $size
		or die("Incomplete chunk at $pos\n");
	if($size % 2) { read(BLORB, $buffer, 1); }

	# optional chunks
	if ($type eq "(c) ") {
		print "\tCopyright: $chunkdata\n";
	}
	if ($type eq "AUTH") {
		print "\tAuthor: $chunkdata\n";
	}
	if ($type eq "ANNO") {
		print "\tAnnotations: $chunkdata\n";
	}

	# zcode executable: look into its magic insides
	if ($type eq "ZCOD") {
		my ($version, $release) = (unpack("CCn", $chunkdata))[0,2];
		my $serialcode = substr($chunkdata, 0x12, 6);
	    print "\t$release.$serialcode (version $version)\n";
	}

	# glulx executable: look into its magic insides
	if($type eq "GLUL") {
		my ($major, $minor, $minimus) = (unpack("xxxxnCC", $chunkdata))[0,1,2];
		print "\tGlulx version $major.$minor.$minimus\n";
	}

	# adrift executable: look into its magic insides
	if ($type eq "ADRI") {
		my @adrift_header = unpack("CCCCCCCCCCCC", $chunkdata);
		if (!array_diff(@adrift_header, @adrift_magic_380)) {
			$version = "3.80";
		} elsif (!array_diff(@adrift_header, @adrift_magic_390)) {
			$version = "3.90";
		} elsif (!array_diff(@adrift_header, @adrift_magic_400)) {
			$version = "4.00";
		} elsif (!array_diff(@adrift_header, @adrift_magic_500)) {
			$version = "5.0";
		} else {
			$version = "unknown";
		}
		print "\tADRIFT Generator version $version\n";
	}

	# magnetic scrolls executable
	if ($type eq "MAGS") {
		print "\tMagnetic Scrolls\n";
	}

	# game identifier chunk: probably only if no executable chunk
	if ($type eq "IFhd") {
	    my $release = unpack("n", substr($chunkdata,0,3));
	    my $serialcode = substr($chunkdata, 2, 6);
	    print "\t$release.$serialcode\n";
	}

	# release number chunk: zcode games only
	if ($type eq "RelN") {
		my $relnum = unpack("n", $chunkdata);
	    print "\tRelease number $relnum\n";
	}

	# Frontispiece chunk
	if ($type eq "Fspc") {
		print "\tFrontispiece: " . unpack("n", substr($chunkdata, 2)) . "\n";
	}

	# SNam chunk
	if ($type eq "SNam") {
		print "\tStory name: " . decode("UTF16-BE", $chunkdata) . "\n";
	}

	# Resolution chunk
	if ($options{images} && $type eq "Reso") {
		$output_filename = "reso_$resocount";
		$output_filename .= ".bin";
		$resocount++;
		dumpchunk($output_filename, $pos, $chunkdata);
	}

	# Adaptive Palette chunk
	if ($options{images} && $type eq "APal") {
		$output_filename = "apal_$apalcount";
		$output_filename .= ".bin";
		$apalcount++;
		dumpchunk($output_filename, $pos, $chunkdata);
	}

	# IFmd chunk
	if ($options{xml} && ($type eq "IFmd")) {
		$output_filename = "ifmd_$xmlcount";
		$output_filename .= ".xml";
		$xmlcount++;
		dumpchunk($output_filename, $pos, $chunkdata);
	}

	# Dumping Exec chunks
	if ($options{exec} && ($type eq "ZCOD" or $type eq "GLUL" or
			$type eq "MAGS")) {
		$output_filename = "exec_";
		$output_filename .= sprintf '%0*d', length($execcount) , $execs{$pos};
		if ($type eq "ZCOD") {
			$output_filename .= ".z" . unpack("C", $chunkdata);
		} elsif ($type eq "GLUL") {
			$output_filename .= ".ulx";
		} elsif ($type eq "ADRI") {
			$output_filename .= ".taf";
		} elsif ($type eq "MAGS") {
			$output_filename .= ".mag";
		} else {
			warn_resource($pos), next;
		}
		dumpchunk($output_filename, $pos, $chunkdata);
	}

	# Dumping Loop chunk
	if ($options{sound} && $type eq "Loop") {
		$output_filename = "loop_$loopcount" . ".bin";
		$loopcount++;
		dumpchunk($output_filename, $pos, $chunkdata);
	}

	# Dumping Snd chunks
	if ($options{sound} && ($type eq "FORM" or $type eq "MOD " or
			$type eq "OGGV" or $type eq "SONG" or
			$type eq "MP3 " or $type eq "WAVE" or
			$type eq "MIDI")) {
		$output_filename = "snd_";
		$output_filename .= sprintf '%0*d', length($soundcount) , $sounds{$pos};
		if ($type eq "FORM") {
			$output_filename .= ".aiff";
			$chunkdata = "FORM" . pack("N", $size) . $chunkdata;
		} elsif ($type eq "MOD ") {
			$output_filename .= ".mod";
		} elsif ($type eq "OGGV") {
			$output_filename .= ".ogg";
		} elsif ($type eq "SONG") {
			$output_filename .= ".song";
		} elsif ($type eq "MP3 ") {
			$output_filename .= ".mp3";
		} elsif ($type eq "WAVE") {
			$output_filename .= ".wav";
		} elsif ($type eq "MIDI") {
			$output_filename .= ".mid";
		} else {
			warn_resource($pos), next;
		}
		dumpchunk($output_filename, $pos, $chunkdata);
	}

	# Dumping Pict chunks
	if ($options{images} && ($type eq "PNG " or $type eq "JPEG" or $type eq "GIF " or $type eq "Rect" or $type eq "GFX ")) {
		$output_filename = "pict_";
		$output_filename .= sprintf '%0*d', length($imagecount) , $images{$pos};
		if ($type eq "PNG ") {
			$output_filename .= ".png";
		} elsif ($type eq "JPEG") {
			$output_filename .= ".jpg";
		} elsif ($type eq "GIF ") {
			$output_filename .= ".gif";
		} elsif ($type eq "GFX ") {
			$output_filename .= ".gfx";
		} elsif ($type eq "Rect") {
			my $width = unpack("n", substr($chunkdata, 2));
			my $height = unpack("n", substr($chunkdata, 6));
			$output_filename .= ".rect";
		} else {
			warn_resource($pos), next;
		}
		dumpchunk($output_filename, $pos, $chunkdata);
	}

	# resource index chunk: always present
	if ($type eq "RIdx") {
		print "\tResources index:\n";

		my $numres = unpack("N", $chunkdata);
		substr($chunkdata, 0, 4) = "";
		while($numres--) {
			my($usage, $number, $start) =
				unpack("a4 NN NN", substr($chunkdata, 0, 12, ""));
			printf("\t\t%06x: %s %d\n", $start, $usage, $number);
			$images{$start} = $number if $usage eq "Pict";
			$execs{$start} = $number if $usage eq "Exec";
			$sounds{$start} = $number if $usage eq "Snd ";
		}
		$imagecount = keys %images;
		$soundcount = keys %sounds;
		$execcount = keys %execs;
	}
}

close(BLORB);

# ---------------------------------------------------------------------------

sub array_diff(\@\@) {
	my %e = map { $_ => undef } @{$_[1]};
	return @{[ ( grep { (exists $e{$_}) ? ( delete $e{$_} ) : ( 1 ) } @{ $_[0] } ), keys %e ] };
}

sub warn_resource {
	my ($pos, @junk) = @_;
	my $errstr = sprintf("No resource information for chunk at %0x06x\n", $pos);
	warn($errstr);
}

sub dumpchunk {
	my ($filename, $pos, $chunkdata, @junk) = @_;

	open CHUNKFH, ">$filename"
		or warn "Failed to open handle for $filename: $!\n", next;
	binmode CHUNKFH;
	$\ = undef;
	print CHUNKFH $chunkdata;
	close CHUNKFH
		or warn "Failed to close handle for $filename: $!\n", next;
}

# ---------------------------------------------------------------------------

__END__


=head1 NAME

scanblorb.pl - Examine a Blorb resource file and optionally dump its contents

=head1 SYNOPSIS

B<scanblorb.pl> [options...] <blorbfile>

Use -h or --help for verbose help.

=head1 DESCRIPTION

Reads a Blorb interactive fiction resource file, checks for validity,
and reports on its contents.  The contents of the Blorb file can also be
extracted to individual files.

=head2 Option flags
	B<-?>		Print simple usage message.
	B<-h --help>	Print verbose help message.
	B<-a --all>	Extract all embedded file chunks.
	B<-e --exec>	Extract only the executable chunk.
	B<-i --images>	Extract only image chunks.
	B<-s --sound>	Extract only sound chunks.
	B< >

=head1 APPLICATION

This script is intended to assist in dissecting and reverse-engineering
blorb files.  Currently chunks having to do with Zcode, Glulx,
ADRIFT, and Magnetic Scrolls executable formats are recognized.

Running the script without any options on a blorb file will result in a
list of chunks found and some information about them.  Chunks that
started off as standalone files may be extracted to the currect
directory by using the appropriate option.  The -a option will cause all
embedded files to be extracted.

Version 3.0

=head1 NOTES

The Blorb format was created by Andrew Plotkin in 1998.  This script
conforms to version 2.0.4 of the Blorb Specification.  See
L<http://www.eblong.com/zarf/blorb/>

=head1 AUTHORS
    (c) Graham Nelson  1998 (original script up to 1.03)
    (c) Richard Poole  2004 (version 2.0)
    (c) David Griffith 2015 (version 3.0 and later)

=cut
