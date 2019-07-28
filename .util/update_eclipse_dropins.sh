#!/bin/bash

# Update the dropins/ folder of the Eclipse installation used to develop Xatkit languages with the most recently built ones.

# Fill the XATKIT_ECLIPSE_DROPINS variable with the path of the dropins/ folder of the Eclipse installation. Do not include the last '/' character in the path.
# Fill the XATKIT_VERSION variable with the version number of the Xatkit build. If the version is a snapshot use X.Y.Z-SNAPSHOT

# If you update this file locally please ignore it using the following command:
# git update-index --skip-worktree update_eclipse_dropins.sh
# If a modification is done on the file upstream git pull will fail, in this case run 
# git update-index --no-skip-worktree update_eclipse_dropins.sh
# git stash
# git pull
# git stash pop
# git update-index --skip-worktree update_eclipse_dropins.sh

#XATKIT_ECLIPSE_DROPINS=<path to eclipse/dropins>
#XATKIT_VERSION=<Xatkit version number>

if [ -z $XATKIT_ECLIPSE_DROPINS ]
then
	echo "Cannot run the script, please set the XATKIT_ECLIPSE_DROPINS variable with the path of the dropins/ folder of your Eclipse installation"
	exit 1
fi

if [ -z $XATKIT_VERSION ]
then
	echo "Cannot run the script, please set the XATKIT_VERSION variable with the version number of Xatkit build. If the version is a snapshot use X.Y.Z-SNAPSHOT"
	exit 1
fi

echo "Deleting jars in eclipse/dropins ..."

rm "$XATKIT_ECLIPSE_DROPINS"/common-*.jar
rm "$XATKIT_ECLIPSE_DROPINS"/core_resources-*.jar
rm "$XATKIT_ECLIPSE_DROPINS"/execution-*.jar
rm "$XATKIT_ECLIPSE_DROPINS"/intent-*.jar
rm "$XATKIT_ECLIPSE_DROPINS"/platform-*.jar

echo "Done"

working_directory=$(basename "$(pwd)")
if [ $working_directory == ".util" ]
then
	cd ..
	working_directory=$(basename "$(pwd)")
fi
if [ $working_directory != "xatkit" ]
then
	echo "Cannot execute the script, try to run it from the .util/ directory"
	exit 1
fi

echo "Copying jars ..."

cp metamodels/common/target/common-"$XATKIT_VERSION".jar $XATKIT_ECLIPSE_DROPINS
cp core_resources/target/core_resources-"$XATKIT_VERSION".jar $XATKIT_ECLIPSE_DROPINS
cp metamodels/execution/target/execution-"$XATKIT_VERSION".jar $XATKIT_ECLIPSE_DROPINS
cp metamodels/intent/target/intent-"$XATKIT_VERSION".jar $XATKIT_ECLIPSE_DROPINS
cp metamodels/platform/target/platform-"$XATKIT_VERSION".jar $XATKIT_ECLIPSE_DROPINS

echo "Done"
