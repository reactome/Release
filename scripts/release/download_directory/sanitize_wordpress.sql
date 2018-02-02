-- default passwords will all be 'password' - users who use this database should probably reset this.
update wp_users set user_login = concat('user',ID), user_pass = MD5('password'), user_nicename= concat('user',ID), user_email='', user_url='', user_registered=now(), display_name='';
update wp_usermeta set meta_value = 'first_name' where meta_key='first_name';
update wp_usermeta set meta_value = 'last_name' where meta_key='last_name';
update wp_usermeta set meta_value = concat('nickname',umeta_id) where meta_key= 'nickname' ;
update wp_usermeta set meta_value = 'description' where meta_key='description';
update wp_usermeta set meta_value = '' where meta_key='session_tokens';
update wp_usermeta set meta_value = '' where meta_key='aim';
update wp_usermeta set meta_value = '' where meta_key='jabber';
update wp_usermeta set meta_value = '' where meta_key='email';
update wp_usermeta set meta_value = '' where meta_key='yim';
update wp_options set option_value = '' where option_name='admin_email';
update wp_options set option_value = 'http://localhost:8080/wordpress' where option_name = 'siteurl';
update wp_options set option_value = 'http://localhost:8080/wordpress' where option_name = 'home';
update wp_options set option_value = '' where option_name = 'clean_contact_email';
delete from wp_blc_links;
delete from wp_blc_instances;
delete from wp_blc_sync;
commit;
