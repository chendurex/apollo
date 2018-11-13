#!/bin/sh

# apollo config db info
apollo_config_db_url=jdbc:mysql://apollodb.we.com:3306/ApolloConfigDB?characterEncoding=utf8
apollo_config_db_username=root
apollo_config_db_password=admin

# apollo portal db info
apollo_portal_db_url=jdbc:mysql://apollodb.we.com:3306/ApolloPortalDB?characterEncoding=utf8
apollo_portal_db_username=root
apollo_portal_db_password=admin

# meta server url, different environments should have different meta server addresses
dev_meta=http://a.dev.apollo.we.com:9090,http://b.dev.apollo.we.com:9090,http://c.dev.apollo.we.com:9090
test_meta=http://a.test.apollo.we.com:9090,http://b.test.apollo.we.com:9090,http://c.test.apollo.we.com:9090
uat_meta=http://a.uat.apollo.we.com:9090,http://b.uat.apollo.we.com:9090,http://c.uat.apollo.we.com:9090
pro_meta=http://a.pro.apollo.we.com:9090,http://b.pro.apollo.we.com:9090,http://c.pro.apollo.we.com:9090
idc_meta=http://a.idc.apollo.we.com:9090,http://b.idc.apollo.we.com:9090,http://c.idc.apollo.we.com:9090

META_SERVERS_OPTS="-Ddev_meta=$dev_meta -Dtest_meta=$test_meta -Duat_meta=$uat_meta -Dpro_meta=$pro_meta -Didc_meta=$idc_meta"

# =============== Please do not modify the following content =============== #
# go to script directory
cd "${0%/*}"

cd ..

# package config-service and admin-service
echo "==== starting to build config-service and admin-service ===="

mvn clean package -DskipTests -pl apollo-configservice,apollo-adminservice -am -Dapollo_profile=github -Dspring_datasource_url=$apollo_config_db_url -Dspring_datasource_username=$apollo_config_db_username -Dspring_datasource_password=$apollo_config_db_password

echo "==== building config-service and admin-service finished ===="

echo "==== starting to build portal ===="

mvn clean package -DskipTests -pl apollo-portal -am -Dapollo_profile=github,t8t -Dspring_datasource_url=$apollo_portal_db_url -Dspring_datasource_username=$apollo_portal_db_username -Dspring_datasource_password=$apollo_portal_db_password $META_SERVERS_OPTS

echo "==== building portal finished ===="

echo "==== starting to build client ===="

mvn clean install -DskipTests -pl apollo-client -am $META_SERVERS_OPTS

echo "==== building client finished ===="

