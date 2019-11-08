# This is required because Xatkit is not yet on Maven Central or similar

# Print a message
e() {
    echo -e "$1"
}

main() {
	
	# Do not print the build log, it is already available in the Xatkit build
    e "Building Xatkit Parent"
    cd /tmp
    git clone https://github.com/xatkit-bot-platform/xatkit-releases.git > /dev/null
    cd xatkit-releases
    mvn install -DskipTests > /dev/null
    e "Done"

    e "Building Xatkit Metamodels"
    cd /tmp
    git clone https://github.com/xatkit-bot-platform/xatkit-metamodels.git > /dev/null
    cd xatkit-metamodels
    mvn install -DskipTests > /dev/null
    e "Done"

    e "Building Xatkit Languages"
    cd /tmp
    git clone https://github.com/xatkit-bot-platform/xatkit-eclipse.git > /dev/null
    cd xatkit-eclipse
    mvn install -DskipTests > /dev/null
    e "Done"
}

main