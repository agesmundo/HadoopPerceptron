#!/usr/bin/perl

#convert a corpus from colum format into single line format
#e.g. from:
#-----------
#he	PROM
#and	CONJ
#Mary	NOUN
#walk	VERB
#-----------
#into:
#-----------
#he_PRON and_CONJ Mary_NOUN walk_VERB
#-----------

my $usage ="Usage: $0 <in_file> <out_file>\n";
if ( @ARGV != 2 )
{
	print $usage;
	exit();
}

my $inFileName =$ARGV[0];
my $outFileName =$ARGV[1];

my $column_label_sep="[\s\t]";
my $line_label_sep="_";
my $line_word_sep=" ";

my $prev_line_empty=1;

open (IN, "<$inFileName") or die "cannot read $inFileName, $!";
open (OUT, ">$outFileName") or die "cannot write $outFileName, $!";
while(<IN>) {
	if(/^[\s\t]*$/)
	{
		if(!$prev_line_empty){
			print OUT "\n";
		}
		$prev_line_empty=1;
	}
	else{
		chomp($_);
		s/[\s\t]+/$line_label_sep/g;
		if(!$prev_line_empty){
			print OUT " ";
		}
		print OUT $_;
		$prev_line_empty=0;
	}
}
close(IN);
close(OUT);

