<?php

class A_Miscellaneous_Form extends Mixin
{
	function get_model()
	{
		return C_Settings_Model::get_instance('global');
	}

	function get_title()
	{
		return __('Miscellaneous', 'nggallery');
	}

	function render()
	{
		return $this->object->render_partial(
            'photocrati-nextgen_other_options#misc_tab',
            array(
                'mediarss_activated'       => C_NextGen_Settings::get_instance()->useMediaRSS,
                'mediarss_activated_label' => __('Add MediaRSS link?', 'nggallery'),
                'mediarss_activated_help'  => __('When enabled, adds a MediaRSS link to your header. Third-party web services can use this to publish your galleries', 'nggallery'),
                'mediarss_activated_no'    => __('No'),
                'mediarss_activated_yes'   => __('Yes'),

                'galleries_in_feeds'       => C_NextGen_Settings::get_instance()->galleries_in_feeds,
                'galleries_in_feeds_label' => __('Display galleries in feeds', 'nggallery'),
                'galleries_in_feeds_help'  => __('NextGEN hides its gallery displays in feeds other than MediaRSS. This enables image galleries in feeds.', 'nggallery'),
                'galleries_in_feeds_no'    => __('No'),
                'galleries_in_feeds_yes'   => __('Yes'),

                'cache_label'        => __('Clear image cache', 'nggallery'),
                'cache_confirmation' => __("Completely clear the NextGEN cache of all image modifications?\n\nChoose [Cancel] to Stop, [OK] to proceed.", 'nggallery'),

                 'slug_field' => $this->_render_text_field(
                     (object)array('name' => 'misc_settings'),
                     'router_param_slug',
                     __('Permalink slug', 'nggallery'),
                     $this->object->get_model()->router_param_slug
                 ),

                'maximum_entity_count_field' => $this->_render_number_field(
                    (object)array('name' => 'misc_settings'),
                    'maximum_entity_count',
                    __('Maximum image count', 'nggallery'),
                    $this->object->get_model()->maximum_entity_count,
                    __('This is the maximum limit of images that NextGEN will restrict itself to querying', 'nggallery')
                        . " \n "
                        . __('Note: This limit will not apply to slideshow widgets or random galleries if/when those galleries specify their own image limits', 'nggallery'),
                    FALSE,
                    '',
                    1
                )
            ),
            TRUE
        );
	}

    function cache_action()
    {
        $cache   = $this->get_registry()->get_utility('I_Cache');
        $cache->flush_galleries();
		C_Photocrati_Cache::flush();
		C_Photocrati_Cache::flush('displayed_galleries');
		C_Photocrati_Cache::flush('displayed_gallery_rendering');
    }

	function save_action()
	{
		if (($settings = $this->object->param('misc_settings')))
        {
			// The Media RSS setting is actually a local setting, not a global one
			$local_settings = C_NextGen_Settings::get_instance();
			$local_settings->set('useMediaRSS', $settings['useMediaRSS']);
			unset($settings['useMediaRSS']);

            // It's important the router_param_slug never be empty
            if (empty($settings['router_param_slug']))
                $settings['router_param_slug'] = 'nggallery';

			// If the router slug has changed, then flush the cache
			if ($settings['router_param_slug'] != $this->object->get_model()->router_param_slug) {
				C_Photocrati_Cache::flush();
			}

			// Save both setting groups
			$this->object->get_model()->set($settings)->save();
			$local_settings->save();
		}
	}
}
