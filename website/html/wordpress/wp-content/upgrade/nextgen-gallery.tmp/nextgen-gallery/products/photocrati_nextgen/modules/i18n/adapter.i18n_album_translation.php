<?php
class A_I18N_Album_Translation extends Mixin
{
    function initialize()
    {
        if (!is_admin())
        {
            $this->object->add_post_hook(
                'set_defaults',
                'Sets NextGEN Album Defaults (translated strings)',
                get_class(),
                'translate_album'
            );
        }
    }

    function translate_album($entity)
    {
        if (!empty($entity->name))
            $entity->name = M_I18N::translate($entity->name, 'album_' . $entity->{$entity->id_field} . '_name');
        if (!empty($entity->albumdesc))
            $entity->albumdesc = M_I18N::translate($entity->albumdesc, 'album_' . $entity->{$entity->id_field} . '_description');

        // these fields are set when the album is a child to another album
        if (!empty($entity->title))
            $entity->title = M_I18N::translate($entity->title, 'album_' . $entity->{$entity->id_field} . '_name');
        if (!empty($entity->galdesc))
            $entity->galdesc = M_I18N::translate($entity->galdesc, 'album_' . $entity->{$entity->id_field} . '_description');
    }

}
