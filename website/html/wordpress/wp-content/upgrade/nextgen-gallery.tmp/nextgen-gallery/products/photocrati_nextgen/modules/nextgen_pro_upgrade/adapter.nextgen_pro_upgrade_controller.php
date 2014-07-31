<?php

class A_NextGen_Pro_Upgrade_Controller extends Mixin
{
    function enqueue_backend_resources()
    {
        $this->call_parent('enqueue_backend_resources');
        wp_enqueue_style(
            'nextgen_pro_upgrade_page',
            $this->get_static_url('photocrati-nextgen_pro_upgrade#style.css')
        );
    }

    function get_page_title()
    {
        return 'Upgrade to Pro';
    }

    function get_required_permission()
    {
        return 'NextGEN Change options';
    }

    function index_action()
    {
		$key = C_Photocrati_Cache::generate_key('nextgen_pro_upgrade_page');
		if (($html = C_Photocrati_Cache::get('nextgen_pro_upgrade_page', FALSE))) {
			echo $html;
		}
		else {
			// Get page content

            $template = 'photocrati-nextgen_pro_upgrade#plus';
            if (defined('NGG_PLUS_PLUGIN_BASENAME'))
                $template = 'photocrati-nextgen_pro_upgrade#pro';

            $description = 'Extend NextGEN Gallery with 8 new pro gallery displays, a full screen responsive pro lightbox, commenting / social sharing / deep linking for individual images, ecommerce, digital downloads, and pro email support.';
            $headline = 'Upgrade to NextGEN Plus or NextGEN Pro';

            if (defined('NGG_PLUS_PLUGIN_BASENAME'))
            {
                $description = 'NextGEN Pro now offers ecommerce! Extend NextGEN Gallery and NextGEN Plus with a complete solution for selling prints and digital downloads, including unlimited pricelists, PayPal and Stripe integration, and more.';
                $headline = 'Upgrade to NextGEN Pro with Ecommerce';
            }

			$params = array(
                'description' => $description,
                'headline'    => $headline
			);
			$html = $this->render_view($template, $params, TRUE);

			// Cache it
			C_Photocrati_Cache::set($key, $html);

			// Render it
			echo $html;
		}
    }
}
