#!/bin/bash

JDK="oraclejdk8"

TYPE="Javadoc"

DOCS_REPO="$TRAVIS_REPO_SLUG-docs"

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
