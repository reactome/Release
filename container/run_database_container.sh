#! /bin/bash
# example for $PATH_TO_DATABASE_ROOT: "/media/Data/reactome/databases/"
PATH_TO_DATABASE_ROOT=$1
docker run --name release_database --net reactome_release -p 3306:3306 \
 	-v $PATH_TO_DATABASE_ROOT/gk_central_reactomerelease_20171024.sql:/docker-entrypoint-initdb.d/gk_central.sql \
	-v $PATH_TO_DATABASE_ROOT/test_slice_62.sql:/docker-entrypoint-initdb.d/test_slice_62.sql \
	-v $PATH_TO_DATABASE_ROOT/stable_identifiers_20171024.sql:/docker-entrypoint-initdb.d/stable_identifiers.sql \
	-e MYSQL_ROOT_PASSWORD=root  mysql
