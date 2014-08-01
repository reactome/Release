<?php

class A_NextGen_Basic_SinglePic_Form extends Mixin_Display_Type_Form
{
	/**
	 * Returns the name of the display type
	 * @return string
	 */
	function get_display_type_name()
	{
		return NGG_BASIC_SINGLEPIC;
	}

	/**
	 * Returns the name of the fields to render for the SinglePic
	 */
	function _get_field_names()
	{
		return array(
            'nextgen_basic_singlepic_dimensions',
            'nextgen_basic_singlepic_link',
            'nextgen_basic_singlepic_link_target',
            'nextgen_basic_singlepic_float',
            'nextgen_basic_singlepic_quality',
            'nextgen_basic_singlepic_crop',
            'nextgen_basic_singlepic_display_watermark',
            'nextgen_basic_singlepic_display_reflection',
            'nextgen_basic_templates_template'
        );
	}

	    function _render_nextgen_basic_singlepic_dimensions_field($display_type)
    {
        return $this->object->render_partial(
            'photocrati-nextgen_basic_singlepic#nextgen_basic_singlepic_settings_dimensions',
            array(
                'display_type_name' => $display_type->name,
                'dimensions_label' => __('Thumbnail dimensions', 'nggallery'),
                'width_label' => __('Width'),
                'width' => $display_type->settings['width'],
                'height_label' => __('Height'),
                'height' => $display_type->settings['height'],
            ),
            True
        );
    }

    function _render_nextgen_basic_singlepic_link_field($display_type)
    {
        return $this->object->render_partial(
            'photocrati-nextgen_basic_singlepic#nextgen_basic_singlepic_settings_link',
            array(
                'display_type_name' => $display_type->name,
                'link_label' => __('Link'),
                'link' => $display_type->settings['link'],
            ),
            True
        );
    }

    function _render_nextgen_basic_singlepic_link_target_field($display_type)
    {
        return $this->_render_select_field(
            $display_type,
            'link_target',
            __('Link target', 'nggallery'),
            array(
                '_self'   => __('Self', 'nggallery'),
                '_blank'  => __('Blank', 'nggallery'),
                '_parent' => __('Parent', 'nggallery'),
                '_top'    => __('Top', 'nggallery'),
            ),
            $display_type->settings['link_target']
        );
    }

    function _render_nextgen_basic_singlepic_quality_field($display_type)
    {
        return $this->object->render_partial(
            'photocrati-nextgen_basic_singlepic#nextgen_basic_singlepic_settings_quality',
            array(
                'display_type_name' => $display_type->name,
                'quality_label' => __('Image quality', 'nggallery'),
                'quality' => $display_type->settings['quality'],
            ),
            True
        );
    }

    function _render_nextgen_basic_singlepic_display_watermark_field($display_type)
    {
        return $this->_render_radio_field(
            $display_type,
            'display_watermark',
            __('Display watermark', 'nggallery'),
            $display_type->settings['display_watermark']
        );
    }

    function _render_nextgen_basic_singlepic_display_reflection_field($display_type)
    {
        return $this->_render_radio_field(
            $display_type,
            'display_reflection',
            __('Display reflection', 'nggallery'),
            $display_type->settings['display_reflection']
        );
    }

    function _render_nextgen_basic_singlepic_crop_field($display_type)
    {
        return $this->_render_radio_field(
            $display_type,
            'crop',
            __('Crop thumbnail', 'nggallery'),
            $display_type->settings['crop']
        );
    }

    function _render_nextgen_basic_singlepic_float_field($display_type)
    {
        return $this->_render_select_field(
            $display_type,
            'float',
            __('Float', 'nggallery'),
            array(
                '' => __('None', 'nggallery'),
                'left' => __('Left', 'nggallery'),
                'right' => __('Right', 'nggallery')
            ),
            $display_type->settings['float']
        );
    }
}