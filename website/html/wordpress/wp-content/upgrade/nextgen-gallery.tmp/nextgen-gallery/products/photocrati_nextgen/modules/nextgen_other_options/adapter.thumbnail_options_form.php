<?php

class A_Thumbnail_Options_Form extends Mixin
{
	function get_model()
	{
		return C_Settings_Model::get_instance();
	}

	function get_title()
	{
		return __('Thumbnail Options', 'nggallery');
	}

	function render()
	{
		$settings = $this->object->get_model();
		
		return $this->render_partial('photocrati-nextgen_other_options#thumbnail_options_tab', array(
			'thumbnail_dimensions_label'	=>	__('Default thumbnail dimensions:', 'nggallery'),
			'thumbnail_dimensions_help'		=>	__('When generating thumbnails, what image dimensions do you desire?', 'nggallery'),
			'thumbnail_dimensions_width'	=>	$settings->thumbwidth,
			'thumbnail_dimensions_height'	=>	$settings->thumbheight,
			'thumbnail_crop_label'		    =>	__('Set fix dimension?', 'nggallery'),
			'thumbnail_crop_help'		    =>	__('Ignore the aspect ratio, no portrait thumbnails?', 'nggallery'),
			'thumbnail_crop'				=>	$settings->thumbfix,
			'thumbnail_quality_label'		=>	__('Adjust Thumbnail Quality?', 'nggallery'),
			'thumbnail_quality_help'		=>	__('When generating thumbnails, what image quality do you desire?', 'nggallery'),
			'thumbnail_quality'				=>	$settings->thumbquality,
			'size_list_label'		        =>	__('Size List', 'nggallery'),
			'size_list_help'		        =>	__('List of default sizes used for thumbnails and images', 'nggallery'),
			'size_list'		                =>	$settings->thumbnail_dimensions,
		), TRUE);
	}

	function save_action()
	{
		if (($settings = $this->object->param('thumbnail_settings'))) {
			$this->object->get_model()->set($settings)->save();
		}
	}
}
