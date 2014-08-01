<?php

/**
 * Provides the display settings form for the NextGen Basic Slideshow
 */
class A_NextGen_Basic_Slideshow_Form extends Mixin_Display_Type_Form
{
	function get_display_type_name()
	{
		return NGG_BASIC_SLIDESHOW;
	}

    function enqueue_static_resources()
    {
        wp_enqueue_script(
            'nextgen_basic_slideshow_settings-js',
            $this->get_static_url('photocrati-nextgen_basic_gallery#slideshow/nextgen_basic_slideshow_settings.js'),
            array('jquery.nextgen_radio_toggle')
        );
	
	$atp = $this->object->get_registry()->get_utility('I_Attach_To_Post_Controller');
	
	if ($atp != null) {
		$atp->mark_script('nextgen_basic_slideshow_settings-js');
	}
    }

    /**
     * Returns a list of fields to render on the settings page
     */
    function _get_field_names()
    {
        return array(
            'nextgen_basic_slideshow_gallery_dimensions',
            'nextgen_basic_slideshow_cycle_effect',
            'nextgen_basic_slideshow_cycle_interval',
            'nextgen_basic_slideshow_flash_enabled',
            'nextgen_basic_slideshow_flash_background_music',
            'nextgen_basic_slideshow_flash_stretch_image',
            'nextgen_basic_slideshow_flash_transition_effect',
            'nextgen_basic_slideshow_flash_shuffle',
            'nextgen_basic_slideshow_flash_next_on_click',
            'nextgen_basic_slideshow_flash_navigation_bar',
            'nextgen_basic_slideshow_flash_loading_icon',
            'nextgen_basic_slideshow_flash_watermark_logo',
            'nextgen_basic_slideshow_flash_slow_zoom',
            'nextgen_basic_slideshow_flash_xhtml_validation',
            'nextgen_basic_slideshow_flash_background_color',
            'nextgen_basic_slideshow_flash_text_color',
            'nextgen_basic_slideshow_flash_rollover_color',
            'nextgen_basic_slideshow_flash_screen_color',
            'nextgen_basic_slideshow_show_thumbnail_link',
            'nextgen_basic_slideshow_thumbnail_link_text'
        );
    }

    function _render_nextgen_basic_slideshow_cycle_interval_field($display_type)
    {
        return $this->_render_number_field(
            $display_type,
            'cycle_interval',
            __('Interval', 'nggallery'),
            $display_type->settings['cycle_interval'],
            '',
            FALSE,
            __('# of seconds', 'nggallery'),
            1
        );
    }

    function _render_nextgen_basic_slideshow_cycle_effect_field($display_type)
    {
        return $this->_render_select_field(
            $display_type,
            'cycle_effect',
            'Effect',
			array(
			'fade' => 'fade',
			'blindX' => 'blindX',
			'cover' => 'cover',
			'scrollUp' => 'scrollUp',
			'scrollDown' => 'scrollDown',
			'shuffle' => 'shuffle',
			'toss' => 'toss',
			'wipe' => 'wipe'
			),
            $display_type->settings['cycle_effect'],
            '',
            FALSE
        );
    }

    function _render_nextgen_basic_slideshow_gallery_dimensions_field($display_type)
    {
        return $this->render_partial('photocrati-nextgen_basic_gallery#slideshow/nextgen_basic_slideshow_settings_gallery_dimensions', array(
            'display_type_name' => $display_type->name,
            'gallery_dimensions_label' => __('Maximum dimensions', 'nggallery'),
            'gallery_dimensions_tooltip' => __('Certain themes may allow images to flow over their container if this setting is too large', 'nggallery'),
            'gallery_width' => $display_type->settings['gallery_width'],
            'gallery_height' => $display_type->settings['gallery_height'],
        ), True);
    }

    function _render_nextgen_basic_slideshow_flash_enabled_field($display_type)
    {
        return $this->_render_radio_field(
            $display_type,
            'flash_enabled',
            __('Enable flash slideshow', 'nggallery'),
            $display_type->settings['flash_enabled'],
            __('Integrate the flash based slideshow for all flash supported devices', 'nggallery')
        );
    }

    function _render_nextgen_basic_slideshow_flash_shuffle_field($display_type)
    {
        return $this->_render_radio_field(
            $display_type,
            'flash_shuffle',
            __('Shuffle', 'nggallery'),
            $display_type->settings['flash_shuffle'],
            '',
            empty($display_type->settings['flash_enabled']) ? TRUE : FALSE
        );
    }

    function _render_nextgen_basic_slideshow_flash_next_on_click_field($display_type)
    {
        return $this->_render_radio_field(
            $display_type,
            'flash_next_on_click',
            __('Show next image on click', 'nggallery'),
            $display_type->settings['flash_next_on_click'],
            '',
            empty($display_type->settings['flash_enabled']) ? TRUE : FALSE
        );
    }

    function _render_nextgen_basic_slideshow_flash_navigation_bar_field($display_type)
    {
        return $this->_render_radio_field(
            $display_type,
            'flash_navigation_bar',
            __('Show navigation bar', 'nggallery'),
            $display_type->settings['flash_navigation_bar'],
            '',
            empty($display_type->settings['flash_enabled']) ? TRUE : FALSE
        );
    }

    function _render_nextgen_basic_slideshow_flash_loading_icon_field($display_type)
    {
        return $this->_render_radio_field(
            $display_type,
            'flash_loading_icon',
            __('Show loading icon', 'nggallery'),
            $display_type->settings['flash_loading_icon'],
            '',
            empty($display_type->settings['flash_enabled']) ? TRUE : FALSE
        );
    }

    function _render_nextgen_basic_slideshow_flash_watermark_logo_field($display_type)
    {
        return $this->_render_radio_field(
            $display_type,
            'flash_watermark_logo',
            __('Use watermark logo', 'nggallery'),
            $display_type->settings['flash_watermark_logo'],
            __('Use the watermark image in the Flash object. Note: this does not watermark the image itself, and cannot be applied with text watermarks', 'nggallery'),
            empty($display_type->settings['flash_enabled']) ? TRUE : FALSE
        );
    }

    function _render_nextgen_basic_slideshow_flash_stretch_image_field($display_type)
    {
        return $this->_render_select_field(
            $display_type,
            'flash_stretch_image',
            __('Stretch image', 'nggallery'),
			array(
                'true'  => __('true',  'nggallery'),
                'false' => __('false', 'nggallery'),
                'fit'   => __('fit',   'nggallery'),
                'none'  => __('none',  'nggallery')
            ),
            $display_type->settings['flash_stretch_image'],
            '',
            empty($display_type->settings['flash_enabled']) ? TRUE : FALSE
        );
    }

    function _render_nextgen_basic_slideshow_flash_transition_effect_field($display_type)
    {
        return $this->_render_select_field(
            $display_type,
            'flash_transition_effect',
            'Transition / fade effect',
            array(
                'fade'     => __('fade',     'nggallery'),
                'bgfade'   => __('bgfade',   'nggallery'),
                'slowfade' => __('slowfade', 'nggallery'),
                'circles'  => __('circles',  'nggallery'),
                'bubbles'  => __('bubbles',  'nggallery'),
                'blocks'   => __('blocks',   'nggallery'),
                'fluids'   => __('fluids',   'nggallery'),
                'flash'    => __('flash',    'nggallery'),
                'lines'    => __('lines',    'nggallery'),
                'random'   => __('random',   'nggallery')
            ),
            $display_type->settings['flash_transition_effect'],
            '',
            empty($display_type->settings['flash_enabled']) ? TRUE : FALSE
        );
    }

    function _render_nextgen_basic_slideshow_flash_slow_zoom_field($display_type)
    {
        return $this->_render_radio_field(
            $display_type,
            'flash_slow_zoom',
            __('Use slow zooming effect', 'nggallery'),
            $display_type->settings['flash_slow_zoom'],
            '',
            empty($display_type->settings['flash_enabled']) ? TRUE : FALSE
        );
    }

    function _render_nextgen_basic_slideshow_flash_background_music_field($display_type)
    {
        return $this->_render_text_field(
            $display_type,
            'flash_background_music',
            __('Background music (url)', 'nggallery'),
            $display_type->settings['flash_background_music'],
            '',
            empty($display_type->settings['flash_enabled']) ? TRUE : FALSE,
            'http://...'
        );
    }

    function _render_nextgen_basic_slideshow_flash_xhtml_validation_field($display_type)
    {
        return $this->_render_radio_field(
            $display_type,
            'flash_xhtml_validation',
            __('Try XHTML validation', 'nggallery'),
            $display_type->settings['flash_xhtml_validation'],
            'Uses CDATA. Important: Could cause problems with some older browsers',
            empty($display_type->settings['flash_enabled']) ? TRUE : FALSE
        );
    }

    function _render_nextgen_basic_slideshow_flash_background_color_field($display_type)
    {
        return $this->_render_color_field(
            $display_type,
            'flash_background_color',
            __('Background', 'nggallery'),
            $display_type->settings['flash_background_color'],
            '',
            empty($display_type->settings['flash_enabled']) ? TRUE : FALSE
        );
    }

    function _render_nextgen_basic_slideshow_flash_text_color_field($display_type)
    {
        return $this->_render_color_field(
            $display_type,
            'flash_text_color',
            __('Texts / buttons', 'nggallery'),
            $display_type->settings['flash_text_color'],
            '',
            empty($display_type->settings['flash_enabled']) ? TRUE : FALSE
        );
    }

    function _render_nextgen_basic_slideshow_flash_rollover_color_field($display_type)
    {
        return $this->_render_color_field(
            $display_type,
            'flash_rollover_color',
            __('Rollover / active', 'nggallery'),
            $display_type->settings['flash_rollover_color'],
            '',
            empty($display_type->settings['flash_enabled']) ? TRUE : FALSE
        );
    }

    function _render_nextgen_basic_slideshow_flash_screen_color_field($display_type)
    {
        return $this->_render_color_field(
            $display_type,
            'flash_screen_color',
            __('Screen', 'nggallery'),
            $display_type->settings['flash_screen_color'],
            '',
            empty($display_type->settings['flash_enabled']) ? TRUE : FALSE
        );
    }

    /**
     * Renders the show_thumbnail_link settings field
     *
     * @param C_Display_Type $display_type
     * @return string
     */
    function _render_nextgen_basic_slideshow_show_thumbnail_link_field($display_type)
    {
        return $this->_render_radio_field(
            $display_type,
            'show_thumbnail_link',
            __('Show thumbnail link', 'nggallery'),
            $display_type->settings['show_thumbnail_link']
        );
    }

    /**
     * Renders the thumbnail_link_text settings field
     *
     * @param C_Display_Type $display_type
     * @return string
     */
    function _render_nextgen_basic_slideshow_thumbnail_link_text_field($display_type)
    {
        return $this->_render_text_field(
            $display_type,
            'thumbnail_link_text',
            __('Thumbnail link text', 'nggallery'),
            $display_type->settings['thumbnail_link_text'],
            '',
            !empty($display_type->settings['show_thumbnail_link']) ? FALSE : TRUE
        );
    }
}
