<?php
/** 
 * The base configurations of the WordPress.
 *
 * This file has the following configurations: MySQL settings, Table Prefix,
 * Secret Keys, WordPress Language, and ABSPATH. You can find more information by
 * visiting {@link http://codex.wordpress.org/Editing_wp-config.php Editing
 * wp-config.php} Codex page. You can get the MySQL settings from your web host.
 *
 * This file is used by the wp-config.php creation script during the
 * installation. You don't have to use the web site, you can just copy this file
 * to "wp-config.php" and fill in the values.
 *
 * @package WordPress
 */

/** Require SSL for admin login */
define('FORCE_SSL_ADMIN', true);

/** Database Charset to use in creating database tables. */
define('DB_CHARSET', 'utf8');

/** The Database Collate type. Don't change this if in doubt. */
define('DB_COLLATE', '');

/**#@+
 * Authentication Unique Keys.
 *
 * Change these to different unique phrases!
 * You can generate these using the {@link https://api.wordpress.org/secret-key/1.1/ WordPress.org secret-key service}
 * You can change these at any point in time to invalidate all existing cookies. This will force all users to have to log in again.
 *
 * @since 2.6.0
 */
define('AUTH_KEY',        'P;E`BCwHj#-ep?&bx]ZN^P`=hs?3uF*Tv9@_iqIL1mVt|$Q`_TI4K&>KuFw%p#5p');
define('SECURE_AUTH_KEY', 'Q$H@$*+l/S%vAw||Hc :+6: [5sM%qVFw}(]+z+q:!>Cq{SeM|o* P>0UWqL6-Rm');
define('LOGGED_IN_KEY',   'zBo2TD UcF+#AH?C/Q+Z:80x{T.T5y+Z-K7v?b@J(l<4)!_O+%Z`M)6Sdh,&$BM.');
define('NONCE_KEY',       '9PH1)t5H.0A*P#B.nswgA>p.0,f$R!&@(o+D:e]@^[M<l@vRgx%?^BX+$p<k(Pq-');
/**#@-*/

/**
 * WordPress Database Table prefix.
 *
 * You can have multiple installations in one database if you give each a unique
 * prefix. Only numbers, letters, and underscores please!
 */
$table_prefix  = 'wp_';

/**
 * WordPress Localized Language, defaults to English.
 *
 * Change this to localize WordPress.  A corresponding MO file for the chosen
 * language must be installed to wp-content/languages. For example, install
 * de.mo to wp-content/languages and set WPLANG to 'de' to enable German
 * language support.
 */
define ('WPLANG', '');

/* That's all, stop editing! Happy blogging. */

/** WordPress absolute path to the Wordpress directory. */
if ( !defined('ABSPATH') )
	define('ABSPATH', dirname(__FILE__) . '/');

/** Sets up WordPress vars and included files. */
require_once(ABSPATH . 'wp-settings.php');

/** Set up WordPress without using FTP */
define('FS_METHOD', 'direct');

/** Repair Wordpress database */
define('WP_ALLOW_REPAIR', true);

/**  Allowing File Size  up to  64MB */
define('WP_MEMORY_LIMIT' , '64M');
