<?php
class A_I18N_Image_Translation extends Mixin
{
    function initialize()
    {
        if (!is_admin())
        {
            $this->object->add_post_hook(
                'set_defaults',
                'Sets NextGEN Image Defaults (translated strings)',
                get_class(),
                'translate_image'
            );
        }
    }

    function translate_image($entity)
    {
        if (!empty($entity->description))
            $entity->description = M_I18N::translate($entity->description, 'pic_' . $entity->{$entity->id_field} . '_description');
        if (!empty($entity->alttext))
            $entity->alttext = M_I18N::translate($entity->alttext, 'pic_' . $entity->{$entity->id_field} . '_alttext');
    }

}
