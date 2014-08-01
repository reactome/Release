<?php

/**
 * Provides an entity for Lightbox Libraries.
 *
 * Properties:
 * - name
 * - code
 * - css_stylesheets
 * - scripts
 */
class C_Lightbox_Library extends C_DataMapper_Model
{
	function define($properties=array(), $mapper=FALSE, $context=FALSE)
	{
		parent::define($mapper, $properties, $context);
		$this->add_mixin('Mixin_Lightbox_Library_Validation');
		$this->implement('I_Lightbox_Library');
	}

	function initialize($properties=array(), $mapper=FALSE, $context=FALSE)
	{
		// Get the mapper is not specified
		if (!$mapper) {
			$mapper = $this->get_registry()->get_utility($this->_mapper_interface);
		}

		// Initialize
		parent::initialize($mapper, $properties);
	}
}

class Mixin_Lightbox_Library_Validation extends Mixin
{
	function validation()
	{
		$this->object->validates_presence_of('name');
		$this->object->validates_uniqueness_of('name');

        // We need to convert some urls
        $scripts = array();
        foreach (explode("\n", $this->object->scripts) as $script) {
            if ($script) $scripts[] = $this->_convert_url($script);
        }
        $this->object->scripts = implode("\n", $scripts);

        $styles = array();
        foreach (explode("\n", $this->object->styles) as $style) {
            if ($style) $styles[] = $this->_convert_url($style);
        }
        $this->object->styles = implode("\n", $styles);

		return $this->object->is_valid();
	}

    function _convert_url($url)
    {
        // Convert absolute url
        if (strpos("http://", $url) === 0 OR strpos("://", $url) === 0 OR strpos("https://", $url) === 0) {
            if (strpos("://", $url) === 0) {
                $url = str_replace("://", 'http://', $url);
            }
            $url = str_replace(home_url(), '', $url);
            if ($url[0] != '/')  $url = '/'.$url;
        }

        return $url;
    }
}