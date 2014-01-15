SDP_HOME=`pwd` 
LIB="$SDP_HOME/lib"
LD_LIBRARY_DIR="$LIB/libbluetooth"
function add_to_bashrc {
    grep -Fxq "$*" ~/.bashrc || echo "$*" >> ~/.bashrc
}
function install_lejos {
    local lLOCATION="`mktemp /tmp/lejos_NXJ_0_9_1beta-3.XXXXXX.tar.gz`"

    echo "> Downloading lejos to $lLOCATION"

    wget -O "$lLOCATION" "http://downloads.sourceforge.net/project/lejos/lejos-NXJ/0.9.1beta/leJOS_NXJ_0.9.1beta-3.tar.gz?r=&ts=1360451745&use_mirror=garr" \
    && echo "> extracting $lLOCATION" \
    && cd $LIB \
    && tar xvf $lLOCATION \
    && mv $LIB/leJOS_NXJ_0.9.1beta-3 $LIB/lejos_nxj \
    && chmod +x lejos_nxj/bin/* \
    && rm $lLOCATION \
    && STR="export PATH=\$PATH:$LIB/lejos_nxj/bin" \
    && add_to_bashrc $STR
}

function install_jbullet {
    local lLOCATION="`mktemp /tmp/jbullet.XXXXXX.zip`"
    local lEXTRACT_DIR="`mktemp -d /tmp/jbullet_extracted.XXXXXX`"

    echo "> Downloading jbullet to $lLOCATION"

    wget -O "$lLOCATION" "http://jbullet.advel.cz/download/jbullet-20101010.zip" \
    && echo "> extracting $lLOCATION" \
    && cd $lEXTRACT_DIR \
    && unzip $lLOCATION \
    && cp -R $lEXTRACT_DIR/jbullet-20101010/lib/* $LIB \
    && rm $lLOCATION \
    && rm -rf $lEXTRACT_DIR
}

function install_jbox2d {
    local lLOCATION="`mktemp /tmp/jbox2d.XXXXXX.zip`"
    local lEXTRACT_DIR="`mktemp -d /tmp/jbox2d_extracted.XXXXXX`"

    echo "> Downloading jbox2d to $lLOCATION"

    wget -O "$lLOCATION" "http://jbox2d.googlecode.com/files/jbox2d-2.1.2.2.zip" \
    && echo "> extracting $lLOCATION" \
    && cd $lEXTRACT_DIR \
    && unzip $lLOCATION \
    && cp $lEXTRACT_DIR/jbox2d-library/target/jbox2d-library-2.1.2.2.jar $LIB \
    && cp $lEXTRACT_DIR/jbox2d-testbed/target/jbox2d-testbed-2.1.2.2.jar $LIB \
    && cp $lEXTRACT_DIR/jbox2d-serialization/target/jbox2d-serialization-1.0.0.jar $LIB \
    && rm $lLOCATION \
    && rm -rf $lEXTRACT_DIR
}

function install_slf4j {
   local lLOCATION="`mktemp /tmp/slf4j.XXXXXXX.tar.gz`"
   local lEXTRACT_DIR="`mktemp -d /tmp/slf4j_extracted.XXXXXX`"

   wget -O "$lLOCATION" "http://www.slf4j.org/dist/slf4j-1.7.2.tar.gz" \
   && echo "> extracting $lLOCATION" \
   && cd $lEXTRACT_DIR \
   && tar xvf $lLOCATION \
   && cp "$lEXTRACT_DIR/slf4j-1.7.2/slf4j-api-1.7.2.jar" $LIB \
   && cp "$lEXTRACT_DIR/slf4j-1.7.2/slf4j-simple-1.7.2.jar" $LIB \
   && rm $lLOCATION \
   && rm -rf $lEXTRACT_DIR
}

function cleanup {
    rm -rf $LIB 
    mkdir -p $LIB
    mkdir -p $LD_LIBRARY_DIR
    STR="export LD_LIBRARY_PATH=\$LD_LIBRARY_PATH:$LD_LIBRARY_DIR"
    add_to_bashrc $STR
}

#I am worried that the pckg-config file references a student's directory
function install_bluetooth {
    local lTMP_BLUETOOTH_LIB="`mktemp /tmp/libbluetooth.XXXXX.tar.gz`"
    wget "https://www.dropbox.com/s/un4al3kzs53murk/libbluetooth.tar.gz" -O $lTMP_BLUETOOTH_LIB \
    && cd $LIB \
    && tar xvf $lTMP_BLUETOOTH_LIB \
    && cd $SDP_HOME \
    && rm $lTMP_BLUETOOTH_LIB
}

function install_mockito {
    wget "http://mockito.googlecode.com/files/mockito-all-1.9.5.jar" -O "$LIB/mockito-all-1.9.5.jar"
}

function install_joptsimple {
    wget -U "SomeUserAgent/1.0" "http://repo1.maven.org/maven2/net/sf/jopt-simple/jopt-simple/4.4/jopt-simple-4.4.jar" -O "$LIB/jopt-simple-4.4.jar"
}

function install_reflections {

    echo "> Downloading reflections"

    wget "http://repo1.maven.org/maven2/org/reflections/reflections/0.9.8/reflections-0.9.8.jar" -O "$LIB/reflections-0.9.8.jar"
}

function install_neuroph {
    local lTMP_NEUROPH="`mktemp -d /tmp/neuroph.XXXXX`"
    wget -O "$lTMP_NEUROPH/neuroph-2.7.zip"  "http://downloads.sourceforge.net/project/neuroph/neuroph-2.7/neuroph-2.7.zip?r=&ts=1360511225&use_mirror=freefr"
    unzip "$lTMP_NEUROPH/neuroph-2.7.zip" -d $LIB
}

################## New functions to install the rest of the libraries - Moataz ##########################

function install_dom4j {

    echo "> Downloading dom4j"

    wget "http://downloads.sourceforge.net/project/dom4j/dom4j-2.0.0-ALPHA-2/dom4j-2.0.0-ALPHA-2.jar?r=http%3A%2F%2Fsourceforge.net%2Fsettings%2Fmirror_choices%3Fprojectname%3Ddom4j%26filename%3Ddom4j-2.0.0-ALPHA-2%2Fdom4j-2.0.0-ALPHA-2.jar&ts=1360545687&use_mirror=garr" -O "$LIB/dom4j-2.0.0-ALPHA-2.jar"
}

function install_jocl {

    local lLOCATION="`mktemp /tmp/JOCL-0.1.8-bin.XXXXXX.zip`"
    local lEXTRACT_DIR="`mktemp -d /tmp/jocl_extracted.XXXXXX`"

    echo "> Downloading jbox2d to $lLOCATION"

    wget -O "$lLOCATION" "http://www.jocl.org/downloads/JOCL-0.1.8-bin.zip" \
    && echo "> extracting $lLOCATION" \
    && cd $lEXTRACT_DIR \
    && unzip $lLOCATION \
    && cp $lEXTRACT_DIR/JOCL-0.1.8-bin/JOCL-0.1.8.jar $LIB \
    && rm $lLOCATION \
    && rm -rf $lEXTRACT_DIR
}

function install_android {

    echo "> Downloading dom4j"

    wget "https://www.dropbox.com/s/87xftetvl3iy3tk/android.jar" -O "$LIB/android.jar"
}

function install_encog {

    local lLOCATION="`mktemp /tmp/encog-core-3.1.0.XXXXXX.zip`"
    local lEXTRACT_DIR="`mktemp -d /tmp/encog_extracted.XXXXXX`"

    echo "> Downloading encog-core-3.1.0 to $lLOCATION"

    wget -O "$lLOCATION" "https://github.com/downloads/encog/encog-java-core/encog-core-3.1.0-release.zip" \
    && echo "> extracting $lLOCATION" \
    && cd $lEXTRACT_DIR \
    && unzip $lLOCATION \
    && cp $lEXTRACT_DIR/encog-core-3.1.0/lib/encog-core-3.1.0.jar $LIB \
    && rm $lLOCATION \
    && rm -rf $lEXTRACT_DIR
}

function install_gson {

    local lLOCATION="`mktemp /tmp/gson-2.2.2.XXXXXX.zip`"
    local lEXTRACT_DIR="`mktemp -d /tmp/gson_extracted.XXXXXX`"

    echo "> Downloading gson-2.2.2 to $lLOCATION"

    wget -O "$lLOCATION" "http://google-gson.googlecode.com/files/google-gson-2.2.2-release.zip" \
    && echo "> extracting $lLOCATION" \
    && cd $lEXTRACT_DIR \
    && unzip $lLOCATION \
    && cp $lEXTRACT_DIR/google-gson-2.2.2/gson-2.2.2.jar $LIB \
    && rm $lLOCATION \
    && rm -rf $lEXTRACT_DIR
}

function install_guava {

    echo "> Downloading guava-14.0"

    wget "http://search.maven.org/remotecontent?filepath=com/google/guava/guava/14.0-rc1/guava-14.0-rc1.jar" -O "$LIB/guava-14.0-rc1.jar"
}

function install_javassist {

    local lLOCATION="`mktemp /tmp/javassist-3.17.1.XXXXXX.zip`"
    local lEXTRACT_DIR="`mktemp -d /tmp/javassist_extracted.XXXXXX`"

    echo "> Downloading javassist-3.17.1-GA to $lLOCATION"

    wget -O "$lLOCATION" "http://downloads.sourceforge.net/project/jboss/Javassist/3.17.1-GA/javassist-3.17.1-GA.zip?r=http%3A%2F%2Fsourceforge.net%2Fprojects%2Fjboss%2Ffiles%2FJavassist%2F3.17.1-GA%2F&ts=1360538757&use_mirror=garr" \
    && echo "> extracting $lLOCATION" \
    && cd $lEXTRACT_DIR \
    && unzip $lLOCATION \
    && cp "$lEXTRACT_DIR/javassist-3.17.1-GA/javassist.jar" $LIB \
    && mv "$LIB/javassist.jar" "$LIB/javassist-3.17.1-GA.jar" \
    && rm $lLOCATION \
    && rm -rf $lEXTRACT_DIR

}

function install_jboss {

    echo "> Downloading guava-14.0"

    wget "http://repo1.maven.org/maven2/org/jboss/jboss-vfs/3.1.0.Final/jboss-vfs-3.1.0.Final.jar" -O "$LIB/jboss-vfs-3.1.0.Final.jar"
}

function install_mysqlconnector {

   local lLOCATION="`mktemp /tmp/mysql-connector-java-5.1.17.XXXXXXX.tar.gz`"
   local lEXTRACT_DIR="`mktemp -d /tmp/mysql-connector_extracted.XXXXXX`"

   wget -O "$lLOCATION" "http://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.23.tar.gz/from/http://cdn.mysql.com/" \
   && echo "> extracting $lLOCATION" \
   && cd $lEXTRACT_DIR \
   && tar xvf $lLOCATION \
   && cp "$lEXTRACT_DIR/mysql-connector-java-5.1.23/mysql-connector-java-5.1.23-bin.jar" $LIB \
   && mv "$LIB/mysql-connector-java-5.1.23-bin.jar" "$LIB/mysql-connector-java-5.1.23.jar" \
   && rm $lLOCATION \
   && rm -rf $lEXTRACT_DIR
}

function install_xml-apis {

    echo "> Downloading xml-apis-2.0.2"

    wget "http://repo1.maven.org/maven2/xml-apis/xml-apis/2.0.2/xml-apis-2.0.2.jar" -O "$LIB/xml-apis-2.0.2.jar"
}

function install_log4j {

   local lLOCATION="`mktemp /tmp/log4j-1.2.17.XXXXXXX.tar.gz`"
   local lEXTRACT_DIR="`mktemp -d /tmp/log4j-1.2.17_extracted.XXXXXX`"

   wget -O "$lLOCATION" "http://apache.mirror.anlx.net/logging/log4j/1.2.17/log4j-1.2.17.tar.gz" \
   && echo "> extracting $lLOCATION" \
   && cd $lEXTRACT_DIR \
   && tar xvf $lLOCATION \
   && cp "$lEXTRACT_DIR/apache-log4j-1.2.17/log4j-1.2.17.jar" $LIB \
   && rm $lLOCATION \
   && rm -rf $lEXTRACT_DIR
}

cleanup
install_lejos # Main API for the lejos system
install_bluetooth
install_slf4j # Seems to be required for the physics demo and reflections
#install_jbullet
install_jbox2d # Body Physics Engine
install_mockito   # Mocking framework -- used in some JUnit tests
install_joptsimple # Simple arguments parser
install_reflections # For processing @Annotations
#install_neuroph # For bezier neural network controller
install_dom4j # Library for working with XML, XPath and XSLT
install_jocl # OpenCL: Library that provides java bindings for OpenCL
install_android
#install_encog # Machine Learning Framework
install_gson # Java Library to convert Java Objects into their JSON Representation
install_guava # Google's core libraries
install_javassist # Library for Java bytecode manipulation
install_jboss # Virtual File System Abstraction used by JBoss Microcontainer and Virtual Deployment Framework
install_mysqlconnector # Standard MySQL Drivers for Java
install_xml-apis # Apache-hosted set of DOM, SAX, and JAXP for use in xml-based projects
install_log4j # Apache logging API
