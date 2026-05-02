#!/bin/bash
VERSION=$(curl -s https://repo1.maven.org/maven2/org/duckdb/duckdb_jdbc/maven-metadata.xml | grep -oPm1 "(?<=<release>)[^<]+")
curl -L -o duckdb.jar "https://repo1.maven.org/maven2/org/duckdb/duckdb_jdbc/${VERSION}/duckdb_jdbc-${VERSION}.jar"
echo "Nedladdad version: $VERSION"
