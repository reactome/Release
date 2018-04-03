#! /bin/bash
# example for $PATH_TO_DATABASE_ROOT: "/media/Data/reactome/databases/"
PATH_TO_DATABASE_ROOT=$1
RELEASE_NUMBER=$2
PREV_RELEASE_NUMBER=$3
# TODO: clean up to have more generic names of database files - i.e. get rid of things like "20171024" in db name.
docker run --name release_database -p 3306:3306 \
    -v $PATH_TO_DATABASE_ROOT/gk_central.sql:/docker-entrypoint-initdb.d/gk_central.sql \
    -v $PATH_TO_DATABASE_ROOT/test_slice_${RELEASE_NUMBER}.sql:/docker-entrypoint-initdb.d/test_slice_${RELEASE_NUMBER}.sql \
    -v $PATH_TO_DATABASE_ROOT/test_slice_${PREV_RELEASE_NUMBER}.sql:/docker-entrypoint-initdb.d/test_slice_${PREV_RELEASE_NUMBER}.sql \
    -v $PATH_TO_DATABASE_ROOT/stable_identifiers.sql:/docker-entrypoint-initdb.d/stable_identifiers.sql \
    -v $(pwd)/mysql_databases:/var/lib/mysql \
    -e MYSQL_ROOT_PASSWORD=root mysql
# TODO: It looks like the Release step GenerateOrthoInferenceStableIds neesds the previous test_reactome database as well.
# -v $PATH_TO_DATABASE_ROOT/test_reactome_${PREV_RELEASE_NUMBER}.sql:/docker-entrypoint-initdb.d/test_reactome_${PREV_RELEASE_NUMBER}.sql \
