package GKB::Release::Step;

use GKB::Release::Utils;
use GKB::Release::Config;

use Moose;

has 'passwords' => (
	is => "ro",
	isa => 'ArrayRef[Str]',
	default => sub { []; }
);

has 'user_input' => (
	is => 'rw',
	isa => 'HashRef[HashRef]',
	default => sub { {} ; }
);

has 'gkb' => (
	is => 'ro',
	isa => 'Str',
	required => 1
);

has 'directory' => (
	is => 'rw',
	isa => 'Str',
	default => $release
);

has 'name' => (
	is => 'ro',
	isa => 'Str',
	lazy => 1,
	default => sub {
		my $name = $0;
		$name =~ s/.pm//;
		return $name;
	}
);

has 'host' => (
	is => 'ro',
	isa => 'Str',
	lazy => 1,
	default => sub {
		my $host = `hostname -f`;
		#my $host = "reactomecurator.oicr.on.ca";
		chomp $host;
		return $host;
	}
);

has 'mail' => (
	is => 'rw',
	isa => 'HashRef[Str]'
);

sub run {
	my $self = shift;
	
	chdir $self->directory;
	set_environment($self->host);
	$self->run_commands($self->gkb);
}

sub set_user_input_and_passwords {
	my $self = shift;
	
	$self->_set_passwords();
	$self->_set_user_input();
}

sub _set_passwords {
	my $self = shift;
	
	foreach my $password (@{$self->passwords}) {
		getpass($password);
	}
}

sub _set_user_input {
	my $self = shift;
	
	foreach my $input_name (keys %{$self->user_input}) {
		my $query = $self->user_input->{$input_name}->{'query'};
		my $hide_keystrokes = $self->user_input->{$input_name}->{'hide_keystrokes'};
		
		$self->user_input->{$input_name}->{'response'} = prompt($query, $hide_keystrokes);
	}
}

sub run_commands {
	die "The run_commands method must be overridden!";
}

1;
