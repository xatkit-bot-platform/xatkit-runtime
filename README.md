jarvis
======
[![Build Status](https://travis-ci.com/gdaniel/jarvis.svg?token=FBbqzUpaXaqnawrfdPca&branch=master)](https://travis-ci.com/gdaniel/jarvis)
[![codecov](https://codecov.io/gh/gdaniel/jarvis/branch/master/graph/badge.svg?token=02TcDpkeLm)](https://codecov.io/gh/gdaniel/jarvis)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/8f852d0d41b24f4f9a989db243647ac2)](https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=gdaniel/jarvis&amp;utm_campaign=Badge_Grade)

A generic bot platform.

## Build Source

    mvn clean install
    
## Generate Javadoc

In order to generate the Javadoc locally, you first need to build the jarvis sources, then run

    mvn javadoc:javadoc
    mvn javadoc:aggregate
    
The generated documentation is stored in the `target/site` folder at the root of the project.

You can also generate a `jar` version of the Javadoc by running

    mvn javadoc:jar
    mvn javadoc:aggregate-jar