#!/bin/bash

# Print a message
e() {
    echo -e "$1"
}

main() {

    e "Checking dependency versions"
    mvn versions:display-dependency-updates

    e "Checking plugin updates"
    mvn versions:display-plugin-updates
}

main
