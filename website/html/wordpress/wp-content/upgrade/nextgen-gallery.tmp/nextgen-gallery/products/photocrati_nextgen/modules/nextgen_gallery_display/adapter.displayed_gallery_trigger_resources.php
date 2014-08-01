<?php

class A_Displayed_Gallery_Trigger_Resources extends Mixin
{
    protected $run_once = FALSE;

    function initialize()
    {
        $this->object->add_post_hook(
            'enqueue_frontend_resources',
            'Enqueues resources for trigger buttons',
            get_class(),
            'enqueue_displayed_gallery_trigger_buttons_resources'
        );
    }

    function enqueue_displayed_gallery_trigger_buttons_resources($displayed_gallery = FALSE)
    {
        if (!wp_style_is('fontawesome', 'registered'))
        {
            if (strpos(strtolower($_SERVER['SERVER_SOFTWARE']), 'microsoft-iis') !== FALSE) {
                wp_register_style('fontawesome', site_url('/?ngg_serve_fontawesome_css=1'));
            } else {
                $router = C_Component_Registry::get_instance()->get_utility('I_Router');
                wp_register_style('fontawesome', $router->get_static_url('photocrati-nextgen_gallery_display#fontawesome/font-awesome.css'));
            }
        }

        if (!$this->run_once
        &&  !empty($displayed_gallery)
        &&  !empty($displayed_gallery->display_settings['ngg_triggers_display'])
        &&  $displayed_gallery->display_settings['ngg_triggers_display'] !== 'never')
        {
            $pro_active = FALSE;
            if (defined('NGG_PRO_PLUGIN_VERSION'))
                $pro_active = 'NGG_PRO_PLUGIN_VERSION';
            if (defined('NEXTGEN_GALLERY_PRO_VERSION'))
                $pro_active = 'NEXTGEN_GALLERY_PRO_VERSION';
            if (!empty($pro_active))
                $pro_active = constant($pro_active);
            if (!is_admin() && (empty($pro_active) || version_compare($pro_active, '1.0.11') >= 0))
            {
                wp_enqueue_style('fontawesome');
                $this->run_once = TRUE;
            }
        }
    }
}

