<?php

class A_I18N_Routing_App extends Mixin
{
    function initialize()
    {
        $this->object->add_pre_hook(
            'execute_route_handler',
            "Suppresses qTranslate's 'Hide Untranslated Content' feature when handling requests",
            get_class(),
            'fix_routed_apps_qtranslate_compat'
        );
    }

    function fix_routed_apps_qtranslate_compat()
    {
        if (!empty($GLOBALS['q_config']) && defined('QTRANS_INIT'))
        {
            global $q_config;
            $q_config['hide_untranslated'] = 0;
        }
    }
}