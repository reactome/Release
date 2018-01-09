## Deprecated scripts and files

This directory has some scripts and files which are currently considered deprecated and may be removed entirely in the future. Below you will see the name of a file in this directory, and the reason for deprecation.

### archive_old_database.pl
May have been used in the past (possibly by cron), but not recently.

### check_and_start_reactome_mart.pl
Reactome Mart no longer exists.

### check_attribute_values.pl
Purpose no longer known.

### check_inverse_attributes.pl
Very old script, probably not used in a very long time, exact purpose not known.

### check_rest.pl
No longer needed - confirmed by Guanming (2018-01-09).

### check_stable_identifiers.pl
Hasn't been touched or run in years, probably won't run even correctly anymore. Original purpose not clear.

### countInstancesInDB.pl
Just a very simple wrapper to an SQL of the form:
```SQL
SELECT COUNT(*) FROM ${TABLE_NAME} ;
```

### inferred2OrthologousEvent.pl
Has not been used in a very long time. The functionality in here probably now exists in the main Orthoinference process.

### install_solr.sh
Installs solr.
Probably based on an old version (hasn't been touched in a couple of years) of https://github.com/reactome/search-indexer/blob/master/setup-solr.sh
But in that case, the newer one (see link) should probably be used.

### internal2stable.pl
Not used anymore, probably has been replaced with something newer.

### migrate_user_account_source.sh
Not relevant to this repository.

### migrate_user_account_target.sh
Not relevant to this repository.

### set_user_credentials.sh
No longer relevant.

### start_reactome_mart.pl
Reactome Mart no longer exists.

### stop_reactome_mart.pl
Reactome Mart no longer exists.

### updateDatabase.pl
Purpose unclear. Has not be run or touched in years.

### csv-heatmap/*
The widget is no longer available on the main Reactome site, so these scripts are no longer needed.
