package GKB::Release::Steps::ClearSearchCache;

use GKB::Release::Config;
use GKB::Release::Utils;

use Moose;
extends qw/GKB::Release::Step/;

#has '+gkb' => ();
#has '+passwords' => ();
has '+directory' => ( default => "$release/clear_search_cache" );
has '+mail' => ( default => sub { 
					my $self = shift;
					return {
						'to' => '',
						'subject' => $self->name,
						'body' => "",
						'attachment' => ""
					};
				}
);

override 'run_commands' => sub {
    my ($self, $gkbdir) = @_;

    my $html_dir = replace_gkb_alias_in_dir($html, "gkb");
    my $img_tmp_dir = "$html_dir/img-tmp";

    $self->cmd("Clearing search cache",
	[
	    ["ssh $live_server 'rm -f $img_tmp_dir/query_store_gk_current* $img_tmp_dir/*.rtf $img_tmp_dir/*.pdf'"]
	]
    );
};
  
1;
