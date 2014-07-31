<?php

class A_Watermarks_Form extends Mixin
{
	function get_model()
	{
		return C_Settings_Model::get_instance();
	}

	function get_title()
	{
		return __('Watermarks', 'nggallery');
	}

	/**
	 * Gets all fonts installed for watermarking
	 * @return array
	 */
	function _get_watermark_fonts()
	{
		$retval = array();
        $path = implode(DIRECTORY_SEPARATOR, array(
           rtrim(NGGALLERY_ABSPATH, "/\\"),
            'fonts'
        ));
		foreach (scandir($path) as $filename) {
			if (strpos($filename, '.') === 0) continue;
			else $retval[] = $filename;
		}
		return $retval;
	}

	/**
	 * Gets watermark sources, along with their respective fields
	 * @return array
	 */
	function _get_watermark_sources()
	{
		// We do this so that an adapter can add new sources
		return array(
			__('Using an Image', 'nggallery') => 'image',
			__('Using Text', 'nggallery') => 'text'
		);
	}

	/**
	 * Renders the fields for a watermark source (image, text)
	 * @return string
	 */
	function _get_watermark_source_fields()
	{
		$retval = array();
		foreach ($this->object->_get_watermark_sources() as $label => $value) {
			$method = "_render_watermark_{$value}_fields";
            if ($this->object->has_method($method)) {
                $retval[$value] = $this->object->call_method($method);
            }
		}
		return $retval;
	}

	/**
	 * Render fields that are needed when 'image' is selected as a watermark
	 * source
	 * @return string
	 */
	function _render_watermark_image_fields()
	{
        $message = __('An absolute or relative (to the site document root) file system path', 'nggallery');
        if (ini_get('allow_url_fopen'))
            $message = __('An absolute or relative (to the site document root) file system path or an HTTP url', 'nggallery');

        return $this->object->render_partial('photocrati-nextgen_other_options#watermark_image_fields', array(
			'image_url_label'			=>	__('Image URL:', 'nggallery'),
			'watermark_image_url'		=>	$this->object->get_model()->wmPath,
            'watermark_image_text'      =>  $message
		), TRUE);
	}

	/**
	 * Render fields that are needed when 'text is selected as a watermark
	 * source
	 * @return string
	 */
	function _render_watermark_text_fields()
	{
		$settings = $this->object->get_model();
		return $this->object->render_partial('photocrati-nextgen_other_options#watermark_text_fields', array(
			'fonts'						=>	$this->object->_get_watermark_fonts($settings),
			'font_family_label'			=>	__('Font Family:', 'nggallery'),
			'font_family'				=>	$settings->wmFont,
			'font_size_label'			=>	__('Font Size:', 'nggallery'),
			'font_size'					=>	$settings->wmSize,
			'font_color_label'			=>	__('Font Color:', 'nggallery'),
			'font_color'				=>	strpos($settings->wmColor, '#') === 0 ?
											$settings->wmColor : "#{$settings->wmColor}",
			'watermark_text_label'		=>	__('Text:', 'nggallery'),
			'watermark_text'			=>	$settings->wmText,
			'opacity_label'				=>	__('Opacity:', 'nggallery'),
			'opacity'					=>	$settings->wmOpaque,
		), TRUE);
	}

    function _get_preview_image()
    {
        $registry = $this->object->get_registry();
        $storage  = $registry->get_utility('I_Gallery_Storage');
        $image    = $registry->get_utility('I_Image_Mapper')->find_first();
        $imagegen = $registry->get_utility('I_Dynamic_Thumbnails_Manager');
        $size     = $imagegen->get_size_name(array(
            'height'    => 250,
            'crop'      => FALSE,
            'watermark' => TRUE
        ));
        $url = $image ? $storage->get_image_url($image, $size) : NULL;
        $abspath = $image ? $storage->get_image_abspath($image, $size) : NULL;
        return (array('url' => $url, 'abspath' => $abspath));
    }

	function render()
	{
		$settings = $this->get_model();
        $image    = $this->object->_get_preview_image();

		return $this->render_partial('photocrati-nextgen_other_options#watermarks_tab', array(
			'notice'					=>	__('Please note: You can only activate the watermark under Manage Gallery. This action cannot be undone.', 'nggallery'),
			'watermark_source_label'	=>	__('How will you generate a watermark?', 'nggallery'),
			'watermark_sources'			=>	$this->object->_get_watermark_sources(),
			'watermark_fields'			=>	$this->object->_get_watermark_source_fields($settings),
			'watermark_source'			=>	$settings->wmType,
			'position_label'			=>	__('Position:', 'nggallery'),
			'position'					=>	$settings->wmPos,
			'offset_label'				=>	__('Offset:', 'nggallery'),
			'offset_x'					=>	$settings->wmXpos,
			'offset_y'					=>	$settings->wmYpos,
			'hidden_label'				=>	__('(Show Customization Options)', 'nggallery'),
			'active_label'				=>	__('(Hide Customization Options)', 'nggallery'),
            'thumbnail_url'             => $image['url'],
            'preview_label'             => __('Preview of saved settings:', 'nggallery'),
            'refresh_label'             => __('Refresh preview image', 'nggallery'),
            'refresh_url'               => $settings->ajax_url
		), TRUE);
	}

	function save_action()
	{
		if (($settings = $this->object->param('watermark_options'))) {
			$this->object->get_model()->set($settings)->save();
            $image = $this->object->_get_preview_image();
            if (is_file($image['abspath']))
                @unlink($image['abspath']);
		}
	}
}
