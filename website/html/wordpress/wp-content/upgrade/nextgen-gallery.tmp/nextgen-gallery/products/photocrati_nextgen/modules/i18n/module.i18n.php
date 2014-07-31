<?php
/***
{
    Module: photocrati-i18n,
    Depends: {photocrati-fs, photocrati-router}
}
 ***/
class M_I18N extends C_Base_Module
{
    function define()
    {
        parent::define(
            'photocrati-i18n',
            'Internationalization',
            "Adds I18N resources and methods",
            '0.1',
            'http://www.nextgen-gallery.com/languages/',
            'Photocrati Media',
            'http://www.photocrati.com'
        );
    }

    function _register_adapters()
    {
        // Provides translating the name & description of images, albums, and galleries
        $this->get_registry()->add_adapter('I_Image_Mapper', 'A_I18N_Image_Translation');
        $this->get_registry()->add_adapter('I_Album_Mapper', 'A_I18N_Album_Translation');
        $this->get_registry()->add_adapter('I_Gallery_Mapper', 'A_I18N_Gallery_Translation');

        // qTranslate requires we disable "Hide Untranslated Content" during routed app requests like
        // photocrati-ajax, when uploading new images, or retrieving dynamically altered (watermarked) images
        $this->get_registry()->add_adapter('I_Routing_App', 'A_I18N_Routing_App');
    }

    function _register_hooks()
    {
        add_action('init', array(&$this, 'register_translation_hooks'), 2);
    }

    function register_translation_hooks()
    {
        $fs = C_Fs::get_instance();
        $dir = str_replace(
            $fs->get_document_root('plugins'),
            '',
            $fs->get_abspath('lang', 'photocrati-i18n')
        );

        // Load text domain
        load_plugin_textdomain('nggallery', false, $dir);

        // Hooks to register image, gallery, and album name & description with WPML
        add_action('ngg_image_updated', array(&$this, 'register_image_strings'));
        add_action('ngg_album_updated', array(&$this, 'register_album_strings'));
        add_action('ngg_created_new_gallery', array(&$this, 'register_gallery_strings'));

        // do not let WPML translate posts we use as a document store
        add_filter('get_translatable_documents', array(&$this, 'wpml_translatable_documents'));

        // see function comments
        add_filter('ngg_displayed_gallery_cache_params', array(&$this, 'set_qtranslate_cache_parameters'));
        add_filter('ngg_displayed_gallery_cache_params', array(&$this, 'set_wpml_cache_parameters'));
    }

    /**
     * When QTranslate is active we must add its language & url-mode settings as display parameters
     * so as to generate a unique cache for each language.
     *
     * @param array $arr
     * @return array
     */
    function set_qtranslate_cache_parameters($arr)
    {
        if (empty($GLOBALS['q_config']) || !defined('QTRANS_INIT'))
            return $arr;

        global $q_config;
        $arr['qtranslate_language'] = $q_config['language'];
        $arr['qtranslate_url_mode'] = $q_config['url_mode'];

        return $arr;
    }

    /**
     * See notes on set_qtranslate_cache_paramters()
     *
     * @param array $arr
     * @return array
     */
    function set_wpml_cache_parameters($arr)
    {
        if (empty($GLOBALS['sitepress']) || !defined('WPML_ST_VERSION'))
            return $arr;

        global $sitepress;
        $settings = $sitepress->get_settings();
        $arr['wpml_language'] = ICL_LANGUAGE_CODE;
        $arr['wpml_url_mode'] = $settings['language_negotiation_type'];

        return $arr;
    }

    /**
     * Registers gallery strings with WPML
     *
     * @param object $gallery
     */
    function register_gallery_strings($gallery_id)
    {
        if (function_exists('icl_register_string'))
        {
            $gallery = $this->get_registry()->get_utility('I_Gallery_Mapper')->find($gallery_id);
            if ($gallery)
            {
                icl_register_string('plugin_ngg', 'gallery_' . $gallery->{$gallery->id_field} . '_name', $gallery->title, TRUE);
                icl_register_string('plugin_ngg', 'gallery_' . $gallery->{$gallery->id_field} . '_description', $gallery->galdesc, TRUE);
            }
        }
    }

    /**
     * Registers image strings with WPML
     *
     * @param object $image
     */
    function register_image_strings($image)
    {
        if (function_exists('icl_register_string'))
        {
            icl_register_string('plugin_ngg', 'pic_' . $image->{$image->id_field} . '_description', $image->description, TRUE);
            icl_register_string('plugin_ngg', 'pic_' . $image->{$image->id_field} . '_alttext', $image->alttext, TRUE);
        }
    }

    /**
     * Registers album strings with WPML
     *
     * @param object $album
     */
    function register_album_strings($album)
    {
        if (function_exists('icl_register_string'))
        {
            icl_register_string('plugin_ngg', 'album_' . $album->{$album->id_field} . '_name', $album->name, TRUE);
            icl_register_string('plugin_ngg', 'album_' . $album->{$album->id_field} . '_description', $album->albumdesc, TRUE);
        }
    }

    /**
     * NextGEN stores some data in custom posts that MUST NOT be automatically translated by WPML
     *
     * @param array $icl_post_types
     * @return array $icl_post_types without any NextGEN custom posts
     */
    function wpml_translatable_documents($icl_post_types = array())
    {
        $nextgen_post_types = array(
            'ngg_album',
            'ngg_gallery',
            'ngg_pictures',
            'displayed_gallery',
            'display_type',
            'gal_display_source',
            'lightbox_library',
            'photocrati-comments'
        );
        foreach ($icl_post_types as $ndx => $post_type) {
            if (in_array($post_type->name, $nextgen_post_types))
                unset($icl_post_types[$ndx]);
        }
        return $icl_post_types;
    }

    static function translate($in, $name = null)
    {
        if (function_exists('langswitch_filter_langs_with_message'))
            $in = langswitch_filter_langs_with_message($in);

        if (function_exists('polyglot_filter'))
            $in = polyglot_filter($in);

        if (function_exists('qtrans_useCurrentLanguageIfNotFoundUseDefaultLanguage'))
            $in = qtrans_useCurrentLanguageIfNotFoundUseDefaultLanguage($in);

        if (is_string($name) && !empty($name) && function_exists('icl_translate'))
            $in = icl_translate('plugin_ngg', $name, $in, true);

        $in = apply_filters('localization', $in);

        return $in;
    }

    function get_type_list()
    {
        return array(
            'A_I18N_Image_Translation' => 'adapter.i18n_image_translation.php',
            'A_I18N_Album_Translation' => 'adapter.i18n_album_translation.php',
            'A_I18N_Gallery_Translation' => 'adapter.i18n_gallery_translation.php',
            'A_I18N_Routing_App' => 'adapter.i18n_routing_app.php'
        );
    }
}

new M_I18N();
