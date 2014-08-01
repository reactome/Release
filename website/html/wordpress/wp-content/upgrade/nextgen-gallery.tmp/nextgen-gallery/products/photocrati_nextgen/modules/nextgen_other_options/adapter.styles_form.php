<?php

class A_Styles_Form extends Mixin
{
	function get_model()
	{
		return C_Settings_Model::get_instance();
	}

	function get_title()
	{
		return __('Styles', 'nggallery');
	}

	function render()
	{
		return $this->object->render_partial('photocrati-nextgen_other_options#styling_tab', array(
            'activateCSS_label'         => __('Enable custom CSS', 'nggallery'),
            'activateCSS'               => $this->object->get_model()->activateCSS,
			'select_stylesheet_label'	=>	__('What stylesheet would you like to use?', 'nggallery'),
			'stylesheets'				=>	C_NextGen_Style_Manager::get_instance()->find_all_stylesheets(),
			'activated_stylesheet'		=>	$this->object->get_model()->CSSfile,
			'hidden_label'				=>	__('(Show Customization Options)', 'nggallery'),
			'active_label'				=>	__('(Hide Customization Options)', 'nggallery'),
			'cssfile_contents_label'	=>	__('File Content:', 'nggallery'),
			'writable_label'			=>	__('Changes you make to the contents will be saved to', 'nggallery'),
			'readonly_label'			=>	__('You could edit this file if it were writable', 'nggallery')
		), TRUE);
	}

	function save_action()
	{
		// Ensure that we have
		if (($settings = $this->object->param('style_settings')))
        {
			$this->object->get_model()->set($settings)->save();

			// Are we to modify the CSS file?
			if (($contents = $this->object->param('cssfile_contents')))
            {
				// Find filename
				$css_file = $settings['CSSfile'];
				$styles = C_NextGen_Style_Manager::get_instance();
				$styles->save($contents, $css_file);
			}
		}
	}
}
