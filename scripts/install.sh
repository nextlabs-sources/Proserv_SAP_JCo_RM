if [ -z "$JRE_HOME" ]; then
    echo "Please set JRE_HOME to the location of Java Runtime"
    exit 1
fi

$JRE_HOME/java -classpath jars/*:xlib/jar/* com.nextlabs.installer.EDRMInstaller
