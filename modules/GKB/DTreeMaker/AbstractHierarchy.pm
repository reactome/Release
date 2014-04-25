package GKB::DTreeMaker::AbstractHierarchy;

use strict;
use vars qw($AUTOLOAD %ok_field);
use Carp;
use CGI::Util 'rearrange';

for my $attr
    (qw(
        urlmaker
        webutils
        js_dtree_var_name
        use_hierarchy_type
	) ) { $ok_field{$attr}++; }

sub new {
    my($pkg, @args) = @_;
    my $self = bless {}, $pkg;
    my ($debug,$urlmaker,$wu,$use_hierarchy_type) = rearrange
	([qw(
	     DEBUG
	     URLMAKER
             WEBUTILS
             USE_HIERARCHY_TYPE
	     )], @args);
#    $self->debug($debug);
    if ($wu) {
	$self->webutils($wu);
    } else {
	confess("Need WebUtils");
    }
    if ($urlmaker) {
	$self->urlmaker($urlmaker);
    } elsif ($wu && $wu->urlmaker) {
	$self->urlmaker($wu->urlmaker);
    } else {
	confess("Need URLMaker");
    }
    $use_hierarchy_type && $self->use_hierarchy_type($use_hierarchy_type);
    return $self;
}

sub AUTOLOAD {
    my $self = shift;
    my $attr = $AUTOLOAD;
    $attr =~ s/.*:://;
    return unless $attr =~ /[^A-Z]/;  # skip DESTROY and all-cap methods
    confess("invalid attribute method: ->$attr()") unless $ok_field{$attr};
    $self->{$attr} = shift if @_;
    return $self->{$attr};
}  

sub _set_js_dtree_var_name {
    my ($self,$instance) = @_;
    $self->js_dtree_var_name('d_' . $instance->db_id);
}

sub next_node_id {
    return ++$_[0]->{'next_node_id'};
}

sub reset_next_node_id {
    $_[0]->{'next_node_id'} = 0;
}

sub get_node_count {
    return $_[0]->{'next_node_id'} + 1;
}

1;
