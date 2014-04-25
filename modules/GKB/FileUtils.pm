package GKB::FileUtils;

=head1 NAME

GKB::FileUtils

=head1 SYNOPSIS

File manipulation utilities

=head1 DESCRIPTION

A Perl utility module for the Reactome book package, providing specialized
methods for manipulating files.

=head1 SEE ALSO

=head1 AUTHOR

David Croft E<lt>croft@ebi.ac.ukE<gt>

Copyright (c) 2005 European Bioinformatics Institute and Cold Spring
Harbor Laboratory.

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself.  See DISCLAIMER for
disclaimers of warranty.

=cut

use vars qw(@ISA $AUTOLOAD %ok_field);
use strict;
use Bio::Root::Root;
#use Fcntl qw(O_RDONLY O_WRONLY O_CREAT);
use GKB::Config;

@ISA = qw(Bio::Root::Root);

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
    $self->throw("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
}  

# Create a new instance of this class
sub new {
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
}

sub read_file {
    my ($self, $file_name) = @_;

    my $content = "";

    if (!(-e $file_name)) {
	return $content;
    }

    unless (open(READ_FILE, "<$file_name")) {
 #   unless (sysopen(READ_FILE, $file_name, O_RDONLY)) {
		print STDERR "FileUtils.read_file: WARNING - problem reading file $file_name\n";
		return $content;
    }

    while (<READ_FILE>) {
	$content .= $_;
    }

    close(READ_FILE);

    return $content;
}

# Uses untainting to get round perl -T flag, watch out!
sub write_file {
    my ($self, $file_name, $content) = @_;

	# Untainting trick to get around -T limitations
	$file_name =~ /^(.*)$/;
	my $safe_file_name = $1;
	
    unless (open(WRITE_FILE, ">$safe_file_name")) {
		print STDERR "FileUtils.write_file: WARNING - problem opening file $safe_file_name for writing\n";
		return ;
    }

    print WRITE_FILE $content;

    close(WRITE_FILE);
}

# Removes the contents of the file, leaving an empty file.
sub empty_file {
    my ($self, $file_name) = @_;

    $self->write_file($file_name, '');
}

# Runs a command within "open" and returns the command's output.
sub run_command {
    my ($self, $command) = @_;

    my $content = "";

    unless (open(READ_FILE, "$command  2>&1 |")) {
 		print STDERR "FileUtils.run_command: WARNING - problem executing command $command\n";
		return $content;
    }

    while (<READ_FILE>) {
		$content .= $_;
    }

    close(READ_FILE);

    return $content;
}

# Print a GD::Image object to a temporary (JPEG) file.
# Arguments:
#
# image - GD::Image object
# filename - if defined, dumps into this filename.  Otherwise, constructs a temporary file.
# png_flag - if 1, generates png files rather than jpeg.
# Returns filename if successful, empty string otherwise.
sub print_image {
    my ($self, $image, $filename, $png_flag) = @_;

    if (!(defined $image)) {
    	print STDERR "FileUtils.print_image: WARNING - image is undef, skipping!\n";
    	return undef;
    }
    
    my $extension = "jpg";
    if (defined $png_flag && $png_flag == 1) {
    	$extension = "png";
    }

    # Dump image into temporary file
    my $image_file = $GK_TMP_IMG_DIR . "/";
    if (defined $filename) {
    	$image_file .= "$filename.$extension";
    } else {
    	$image_file .= rand() * 1000 . $$ . ".$extension";
    }

    if (open(REACTOMEIMAGE, ">$image_file")) {
    	my $image_print_ok = 0;
    	eval {
    	    if ($png_flag) {
				print REACTOMEIMAGE $image->png;
    	    } else {
				print REACTOMEIMAGE $image->jpeg;
    	    }
    	    $image_print_ok = 1;
    	};
		close(REACTOMEIMAGE);
    	if ($image_print_ok == 0) {
    	    print STDERR "FileUtils.print_image: WARNING - could not print image file $image_file, you probably need to reconfigure libgd or the GD Perl package to recognize this image type.\n";
    	    return undef;
    	}
		return $image_file;
    } else {
		print STDERR "FileUtils.print_image: WARNING - cant open $image_file\n";
    }

    return undef;
}

1;
