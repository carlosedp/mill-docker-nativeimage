#!/bin/bash

GROUPID=com.carlosedp
ARTIFACT=mill-docker-nativeimage
MILLVER=mill0.10_2.13

REPO=$(echo ${GROUPID} | sed "s/\./\//g")/${ARTIFACT}

echo "Fetching release and snapshot versions for ${REPO}"

# Fetch latest release and snapshot versions
LASTRELEASE=$(curl -sL https://repo1.maven.org/maven2/"${REPO}_${MILLVER}"/maven-metadata.xml |grep latest |head -1 |sed -e 's/<[^>]*>//g' |tr -d " ")

echo "Latest library release: ${LASTRELEASE}"

# Update Readme
echo "Updating readme versions..."

## Update Release
sed -i "s/ivy.*ReleaseVerMill/ivy.\`${GROUPID}::${ARTIFACT}::${LASTRELEASE}\`  \/\/ReleaseVerMill/" Readme.md

echo "Finished"