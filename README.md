# Introduction
JSCompressor is a command line application used to monitor a directory of Javascript files and generate a minified Javascript file. 
The minified file is updated whenever a Javascript file is added, edited, or deleted in the source directory.

Usage:
```
java -jar JSCompressor.jar /path/to/javascript/project /releases/project-name.min.js
```

## Maven Quickstart
```
git clone https://github.com/pborissow/JSCompressor.git
cd JSCompressor
mvn install
java -jar JSCompressor.jar /path /output.js
```
