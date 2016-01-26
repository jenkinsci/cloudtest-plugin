#!/bin/bash
# clean previous entries
rm -rf ~/.jenkins/plugins/cloudtest*
mvn package -Dmaven.test.skip=true
cp target/cloudtest.hpi ~/.jenkins/plugins/

