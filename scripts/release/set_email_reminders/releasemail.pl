#!/usr/bin/perl
use strict;
use warnings;
use MIME::Lite;

my $from = "lmatthews.nyumc\@gmail.com";
my @list = qw/bj1@ebi.ac.uk broose.may@gmail.com Karen.Rothfels@oicr.on.ca lmatthews.nyumc@gmail.com gillespm@gmail.com Marija.Orlic-Milacic@oicr.on.ca mwilliam@ebi.ac.uk mcaudy@gmail.com phani@ebi.ac.uk robin.haw@oicr.on.ca sjupe@ebi.ac.uk vshamovsky@gmail.com/;
my $to = join ",", @list;
my $cc = "peter.d'eustachio\@nyumc.org";
my $subject = $ARGV[0];
my $version = $ARGV[1];
my $datafreeze = $ARGV[2] . " " . $ARGV[3] . " " . $ARGV[4];
my $finalslice = $ARGV[5] . " " . $ARGV[6]; 
my $body;


if ($subject eq "-df") { # Data freeze 
    $body =<<END;

	Just a reminder that the data freeze for the Version $version release is this coming $datafreeze.  ALL projects (e.g NOT just those being released in V$version) must be committed by 12 pm EST on that day. No commits should be made after that time as the database updates will be running.  I will send around a message when it is safe to commit again. When gk_central is reopened, please be sure to synchronize any local projects with gk_central and opt to update your Uniprot, GO, and Chebi instances from the database as necessary. 

	Before committing projects, please make sure that:

	1. release_Date and do_Release flags are set (please keep do_Release flags set as "true" for events that are in review, even if the review is not yet complete)

	2. all QA has been completed in gk-central including:

	Mandatory attribute check
	Reaction imbalance check
	Compartment checks
	Species check
	  
    3. Diagrams committed and deployed
    4. Author, reviewer, editor slots filled
    5. "revised" slots filled for any events that have been updated.
    
    From that time until the final slice on $finalslice, no changes (other than text changes or requested QA fixes) should be made to projects that have been released or are scheduled for release in V$version. 
END
    $subject = "Data Freeze";
} elsif ($subject eq "-rr") { # Reviewer request
    $body =<<END;
Hi Curators,

If you have not done so already, please forward the names of perspective reviewers for your modules by the end of the day tomorrow. Make sure to include full name and initial (if applicable), institution and e-mail address.

I will be sending the request letters out over the next few days.

Thanks,

Lisa 

END
    $subject = "Reviewer Request";
} elsif ($subject eq "-fs") {
    $body = "In preparation for the final slice, gk_central is now closed.  Please do not make any commits to the database until further notice";
    $subject = "Final Slice";
} elsif ($subject eq "-rl") {
    $body = "Please send out reminder letters to reviewers that have accepted.";
    $subject = "Reminder Letters";
    $to = $from;
} elsif ($subject eq "-i") {
	$body =<<END;
Dear Illustrator,

A week from today, the the editorial staff will be sending modules out for external and internal review for the upcoming 
release. Before this time, please commit, to cvs, any illustrations that need to be reviewed and send the managing editor
the file names.

Thank you!

END
	$subject = "Illustrator Reminder";
	$to = "heeyeon.song\@oicr.on.ca";
} else {
	die "Choose a valid subject";
}

my $msg = MIME::Lite->new(
    From => $from,
    To => $from,
    #Cc => $cc,
    Subject => $subject,
    Type => "multipart/mixed"
);

$msg->attach(
    Type => "TEXT",
    Data => $body
);

$msg->send() or die "Mail could not be sent: $!";
