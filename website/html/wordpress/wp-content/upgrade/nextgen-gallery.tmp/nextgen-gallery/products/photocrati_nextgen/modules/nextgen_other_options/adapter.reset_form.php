<?php

class A_Reset_Form extends Mixin
{
	function get_title()
	{
		return __('Reset Options', 'nggallery');
	}

	function render()
	{
		return $this->object->render_partial(
            'photocrati-nextgen_other_options#reset_tab',
            array(
                'reset_value'			=> __('Reset all options to default settings', 'nggallery'),
                'reset_warning'			=> __('Replace all existing options and gallery options with their default settings', 'nggallery'),
                'reset_label'			=> __('Reset settings', 'nggallery'),
                'reset_confirmation'	=> __("Reset all options to default settings?\n\nChoose [Cancel] to Stop, [OK] to proceed.", 'nggallery')
                // 'uninstall_label'		=> _('Deactivate & Uninstall'),
				// 'uninstall_confirmation'=>_("Completely uninstall NextGEN Gallery (will reset settings and de-activate)?\n\nChoose [Cancel] to Stop, [OK] to proceed."),
            ),
            TRUE
        );
	}

	function reset_action()
	{
        global $wpdb;

        // Flush the cache
        C_Photocrati_Cache::flush('all');

        // Uninstall the plugin
        $settings = C_NextGen_Settings::get_instance();

        if (defined('NGG_PRO_PLUGIN_VERSION') || defined('NEXTGEN_GALLERY_PRO_VERSION'))
		    C_Photocrati_Installer::uninstall('photocrati-nextgen-pro');

		C_Photocrati_Installer::uninstall('photocrati-nextgen');

        // removes all ngg_options entry in wp_options
        $settings->reset();
        $settings->destroy();

        // clear NextGEN's capabilities from the roles system
        $capabilities = array(
            "NextGEN Gallery overview",
            "NextGEN Use TinyMCE",
            "NextGEN Upload images",
            "NextGEN Manage gallery",
            "NextGEN Manage others gallery",
            "NextGEN Manage tags",
            "NextGEN Edit album",
            "NextGEN Change style",
            "NextGEN Change options",
            "NextGEN Attach Interface"
        );
        $roles = array("subscriber", "contributor", "author", "editor", "administrator");
        foreach ($roles as $role) {
            $role = get_role($role);
            foreach ($capabilities as $capability) {
                $role->remove_cap($capability);
            }
        }

        // Some installations of NextGen that upgraded from 1.9x to 2.0x have duplicates installed,
        // so for now (as of 2.0.21) we explicitly remove all display types and lightboxes from the
        // db as a way of fixing this.
        $wpdb->query($wpdb->prepare("DELETE FROM {$wpdb->posts} WHERE post_type = %s", 'display_type'));
        $wpdb->query($wpdb->prepare("DELETE FROM {$wpdb->posts} WHERE post_type = %s", 'lightbox_library'));

        // the installation will run on the next page load; so make our own request before reloading the browser
        wp_remote_get(
            admin_url('plugins.php'),
            array(
                'timeout' => 180,
                'blocking' => true,
                'sslverify' => false
            )
        );

        header('Location: ' . $_SERVER['REQUEST_URI']);
        throw new E_Clean_Exit();
	}

    /*
	function uninstall_action()
	{
		$installer = C_Photocrati_Installer::get_instance();
		$installer->uninstall(NGG_PLUGIN_BASENAME, TRUE);
		deactivate_plugins(NGG_PLUGIN_BASENAME);
		wp_redirect(admin_url('/plugins.php'));
	}
    */
}
