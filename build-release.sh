#!/bin/sh

#######################################################################################################################
#
#  Ontop Build Script
# 
#  <xiao(a)inf.unibz.it>
#
#######################################################################################################################


export VERSION=1.16
export REVISION=2-SNAPSHOT

if type -p java; then
    echo Found java executable in PATH
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    echo Found java executable in JAVA_HOME
    _java="$JAVA_HOME/bin/java"
else
    echo "ERROR: Java is not installed!"
    exit 1
fi

if [[ "$_java" ]]; then
    JAVA_VER=$(java -version 2>&1 | sed 's/java version "\(.*\)\.\(.*\)\..*"/\2/; 1q')
    #echo version "$version"
    if [[ "$JAVA_VER" -ne "8" ]]; then
        echo "ERROR: Java 8 is required for building Ontop! Current Java version: $JAVA_VER"
        exit 1
    fi
fi


# location for the build ROOT folder (i.e. the directory of this script)
export BUILD_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# location for the build dependencies home 
export ONTOP_DEP_HOME=${BUILD_ROOT}/ontop-build-dependencies


if [ -d "${ONTOP_DEP_HOME}" ] && [ -f "${ONTOP_DEP_HOME}/.git" ]
then
  echo ""
  echo "========================================="
  echo " Starting Ontop build script ... "
  echo "-----------------------------------------"
  echo ""
else
  echo "ERROR: git submodule 'ontop-build-dependencies' is missing or uninitiated!"
  echo "Please run 'git submodule init && git submodule update'"
  exit 1
fi

# location for protege clean folder
export PROTEGE_COPY_FILENAME=protege-5.0.0-beta-21-platform-independent
export PROTEGE_MAIN_FOLDER_NAME=Protege-5.0.0-beta-21
export PROTEGE_MAIN_PLUGIN=ontop-protege-plugin

# location and name for jetty distribution (should be ZIP)
export JETTY_COPY_FILENAME=jetty-distribution-8.1.9
export JETTY_INNER_FOLDERNAME=jetty-distribution-8.1.9

# name of the wars for sesame and workbench WEB-APPs  (these have to be already customized with stylesheets)
export OPENRDF_SESAME_FILENAME=openrdf-sesame
export OPENRDF_WORKBENCH_FILENAME=openrdf-workbench
export ONTOP_SESAME_WEBAPPS=ontop-sesame-webapps

# folder names of the output
export PROTEGE_DIST=ontop-protege
export QUEST_SESAME_DIST=ontop-sesame
export QUEST_JETTY_DIST=ontop-jetty
export ONTOP_DIST=ontop-dist

# jar name of the pretege plugin
export PROTEGE_PLUGIN_NAME=it.unibz.inf.ontop.protege

#
# Start building the packages
#

cd ${BUILD_ROOT}

echo ""
echo "========================================="
echo " Cleaning                                "
echo "-----------------------------------------"
echo ""

mvn clean

echo ""
echo "========================================="
echo " Compiling Ontop $VERSION.$REVISION      "
echo "-----------------------------------------"
echo ""

echo "pluginVersion=$VERSION.$REVISION" >  ${BUILD_ROOT}/obdalib-core/src/main/resources/it/unibz/krdb/obda/utils/version.properties

mvn install -DskipTests

#
echo ""
echo "========================================="
echo " Building Protege distribution package   "
echo "-----------------------------------------"
echo ""

rm -fr ${BUILD_ROOT}/ontop-protege/dist
cd ${BUILD_ROOT}/ontop-protege/
mvn bundle:bundle -DskipTests

rm -fr ${BUILD_ROOT}/quest-distribution/${PROTEGE_DIST}
mkdir ${BUILD_ROOT}/quest-distribution/${PROTEGE_DIST}
cp target/${PROTEGE_PLUGIN_NAME}-${VERSION}.${REVISION}.jar \
  ${BUILD_ROOT}/quest-distribution/${PROTEGE_DIST}/${PROTEGE_PLUGIN_NAME}-${VERSION}.${REVISION}.jar

cp ${ONTOP_DEP_HOME}/${PROTEGE_COPY_FILENAME}.zip ${BUILD_ROOT}/quest-distribution/${PROTEGE_DIST}/

cd ${BUILD_ROOT}/quest-distribution/${PROTEGE_DIST}/

mkdir -p ${PROTEGE_MAIN_FOLDER_NAME}/plugins
cp ${PROTEGE_PLUGIN_NAME}-${VERSION}.${REVISION}.jar ${PROTEGE_MAIN_FOLDER_NAME}/plugins/
cp ${ONTOP_DEP_HOME}/org.protege.osgi.jdbc.jar ${PROTEGE_MAIN_FOLDER_NAME}/plugins/
cp ${ONTOP_DEP_HOME}/org.protege.osgi.jdbc.prefs.jar ${PROTEGE_MAIN_FOLDER_NAME}/plugins/
zip ${BUILD_ROOT}/quest-distribution/${PROTEGE_DIST}/${PROTEGE_MAIN_PLUGIN}-${VERSION}.${REVISION}.zip ${PROTEGE_MAIN_FOLDER_NAME}/plugins/*.*

zip ${PROTEGE_COPY_FILENAME}.zip ${PROTEGE_MAIN_FOLDER_NAME}/plugins/*
mv ${PROTEGE_COPY_FILENAME}.zip ontop-protege-bundle-${VERSION}.${REVISION}.zip

rm -fr ${PROTEGE_MAIN_FOLDER_NAME}
cd ${BUILD_ROOT}/quest-distribution

# Packing the sesame distribution
#
echo ""
echo "========================================="
echo " Building Sesame distribution package    "
echo "-----------------------------------------"
echo ""

rm -fr ${QUEST_SESAME_DIST}
mkdir -p ${QUEST_SESAME_DIST}/WEB-INF/lib
mvn assembly:assembly -DskipTests
cp target/ontop-distribution-${VERSION}.${REVISION}-sesame-bin.jar ${QUEST_SESAME_DIST}/WEB-INF/lib/ontop-distribution-${VERSION}.${REVISION}.jar
unzip -q -d ${QUEST_SESAME_DIST}/WEB-INF/lib/ target/ontop-distribution-${VERSION}.${REVISION}-dependencies.zip
cp ${ONTOP_DEP_HOME}/${OPENRDF_SESAME_FILENAME}.war ${QUEST_SESAME_DIST}/
cp ${ONTOP_DEP_HOME}/${OPENRDF_WORKBENCH_FILENAME}.war ${QUEST_SESAME_DIST}/

cd ${QUEST_SESAME_DIST}
echo ""
echo "[INFO] Adding QuestSesame and dependency JARs to openrdf-sesame.war"
jar -uf ${OPENRDF_SESAME_FILENAME}.war WEB-INF/lib/*

echo "[INFO] Adding QuestSesame and dependency JARs to openrdf-workbench.war"
jar -uf ${OPENRDF_WORKBENCH_FILENAME}.war WEB-INF/lib/*

zip ${ONTOP_SESAME_WEBAPPS}-${VERSION}.${REVISION}.zip ${OPENRDF_SESAME_FILENAME}.war ${OPENRDF_WORKBENCH_FILENAME}.war

rm -fr WEB-INF
cd ${BUILD_ROOT}/quest-distribution

# Packaging the sesame jetty distribution
#
echo ""
echo "========================================="
echo " Building Sesame Jetty distribution package"
echo "-----------------------------------------"
rm -fr ${QUEST_JETTY_DIST}
mkdir ${QUEST_JETTY_DIST}
cp ${ONTOP_DEP_HOME}/${JETTY_COPY_FILENAME}.zip ${QUEST_JETTY_DIST}/ontop-jetty-bundle-${VERSION}.${REVISION}.zip

export JETTY_FOLDER=${JETTY_INNER_FOLDERNAME}
cd ${QUEST_JETTY_DIST}
mkdir -p ${JETTY_INNER_FOLDERNAME}/webapps
cp ${BUILD_ROOT}/quest-distribution/${QUEST_SESAME_DIST}/${OPENRDF_SESAME_FILENAME}.war ${JETTY_FOLDER}/webapps
cp ${BUILD_ROOT}/quest-distribution/${QUEST_SESAME_DIST}/${OPENRDF_WORKBENCH_FILENAME}.war ${JETTY_FOLDER}/webapps

zip ontop-jetty-bundle-${VERSION}.${REVISION}.zip ${JETTY_FOLDER}/webapps/*

rm -fr ${JETTY_FOLDER}
cd ${BUILD_ROOT}/quest-distribution

# Packaging the OWL-API distribution
#
echo ""
echo "========================================="
echo " Building Ontop distribution package"
echo "-----------------------------------------"
rm -fr ${ONTOP_DIST}
mkdir ${ONTOP_DIST}
echo "[INFO] Copying files..."
cp target/ontop-distribution-${VERSION}.${REVISION}-bin.zip ${ONTOP_DIST}/ontop-distribution-${VERSION}.${REVISION}.zip

echo ""
echo "========================================="
echo " Done."
echo "-----------------------------------------"
echo ""
