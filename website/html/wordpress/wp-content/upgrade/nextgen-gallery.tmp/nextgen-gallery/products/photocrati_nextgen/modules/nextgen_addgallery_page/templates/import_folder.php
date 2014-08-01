<p><?php _e('Select a folder to import. The folder name will be used as the title of the gallery.', 'nggallery'); ?></p>
<div id="file_browser">
</div>
<p>
    <input type="checkbox" id="import_keep_location" name="keep_location" value="on" /> <label for="import_keep_location"> <?php esc_html_e('Keep images in original location.', 'nggallery'); ?> <span style="font-size: 85%"><?php esc_html_e('Caution: If you keep images in the original folder and later delete the gallery, the images in that folder might be deleted depending on your settings.', 'nggallery'); ?></span></label><br/><br/>
    <input type="button" id="import_button" name="import_folder" value="<?php _e('Import Folder', 'nggallery'); ?>" class="button-primary"/>
</p>
<script type="text/javascript">
    var selected_folder = null;
    jQuery(function($){
        // Only run this function once!
        if (typeof($(window).data('ready')) == 'undefined')
            $(window).data('ready', true);
        else return;

        // Render file browser
        $('#file_browser').fileTree({
            root:           '/',
            script:         photocrati_ajax.url,
            post_params:    {action: 'browse_folder', token: ''}
        }, function(file){
            selected_folder = file;
            $('#file_browser a').each(function(){
                $(this).removeClass('selected_folder');
            })
            $('#file_browser a[rel="'+file+'"]').addClass('selected_folder');
            file = file.split("/");
            file.pop();
            file = '/'+file.pop();
            $('#import_button').val("Import "+file);
        });

        // Import the folder
        $('#import_button').click(function(e){
            e.preventDefault();

            // Show progress bar
            var progress_bar =  $.nggProgressBar({
                title: '<?php _e("Importing gallery", 'nggallery'); ?>',
                infinite: true,
                starting_value: '<?php _e('In Progress...', 'nggallery'); ?>'
            });

            // Start importing process
            var post_params = {
                action: 'import_folder',
                folder: selected_folder,
                keep_location: $('#import_keep_location').is(":checked") ? 'on' : 'off'
            };
            $.post(photocrati_ajax.url, post_params, function(response){
                if (typeof(response) != 'object') response = JSON.parse(response);
                if (typeof(response.error) == 'string') {
                    progress_bar.set(response.error);
                }
                else {
                    var message = "<?php __('Done! Successfully imported %s images', 'nggallery'); ?>";
                    progress_bar.set(message.replace('%s', response.image_ids.length));
                }
                progress_bar.close(2000);
            });
        })
    });
</script>
