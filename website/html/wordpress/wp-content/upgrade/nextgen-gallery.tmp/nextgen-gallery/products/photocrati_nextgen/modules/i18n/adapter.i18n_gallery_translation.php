<?php
class A_I18N_Gallery_Translation extends Mixin
{
    function initialize()
    {
        if (!is_admin())
        {
            $this->object->add_post_hook(
                'set_defaults',
                'Sets NextGEN Gallery Defaults (translated strings)',
                get_class(),
                'translate_gallery'
            );
        }
    }

    function translate_gallery($entity)
    {
        if (!empty($entity->title))
            $entity->title = M_I18N::translate($entity->title, 'gallery_' . $entity->{$entity->id_field} . '_name');
        if (!empty($entity->galdesc))
            $entity->galdesc = M_I18N::translate($entity->galdesc, 'gallery_' . $entity->{$entity->id_field} . '_description');
    }

}
