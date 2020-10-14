# This is required because Xatkit is not yet on Maven Central or similar

# Print a message
e() {
    echo -e "$1"
}

main() {
	
	# Do not print the build log, it is already available in the Xatkit build
    e "Building Required Xatkit Dependencies"
    cd /tmp
    git clone https://github.com/xatkit-bot-platform/xatkit.git > /dev/null
    cd xatkit
    git submodule update --init --recursive
    mvn clean install -N -DskipTests > /dev/null
    cd xatkit-metamodels
    git checkout master
    mvn clean install -DskipTests > /dev/null
    e "Done"
}

main