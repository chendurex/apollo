#!/bin/sh

# apollo config db info
apollo_config_db_url=jdbc:mysql://t8t.sc.apollo.db:3306/ApolloConfigDB?characterEncoding=utf8
apollo_config_db_username=root
apollo_config_db_password=admin

# apollo portal db info
apollo_portal_db_url=jdbc:mysql://t8t.sc.apollo.db:3306/ApolloPortalDB?characterEncoding=utf8
apollo_portal_db_username=root
apollo_portal_db_password=admin

# meta server url, different environments should have different meta server addresses
dev_meta=http://dev.t8t.sc.apollo:80
test_meta=http://test.t8t.sc.apollo:80
uat_meta=http://uat.t8t.sc.apollo:80
pro_meta=http://pro.t8t.sc.apollo:80

META_SERVERS_OPTS="-Ddev_meta=$dev_meta -Dtest_meta=$test_meta -Duat_meta=$uat_meta -Dpro_meta=$pro_meta"

# =============== Please do not modify the following content =============== #
# go to script directory
cd "${0%/*}"

cd ..

# package config-service and admin-service
echo "==== starting to build config-service and admin-service ===="

mvn clean package -DskipTests -pl apollo-configservice,apollo-adminservice -am -Dapollo_profile=github -Dspring_datasource_url=$apollo_config_db_url -Dspring_datasource_username=$apollo_config_db_username -Dspring_datasource_password=$apollo_config_db_password

echo "==== building config-service and admin-service finished ===="

echo "==== starting to build portal ===="

mvn clean package -DskipTests -pl apollo-portal -am -Dapollo_profile=github,auth -Dspring_datasource_url=$apollo_portal_db_url -Dspring_datasource_username=$apollo_portal_db_username -Dspring_datasource_password=$apollo_portal_db_password $META_SERVERS_OPTS

echo "==== building portal finished ===="

echo "==== starting to build client ===="

mvn clean deploy -DskipTests -pl apollo-client -am $META_SERVERS_OPTS

echo "==== building client finished ===="

