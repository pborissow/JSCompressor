# Introduction
JSCompressor is a command line application used to monitor a directory containing unminified Javascript source code (*.js files) and create a single, minified Javascript file. The minified file is automatically updated whenever a Javascript file is added, edited, or deleted in the source directory. 

This live monitoring and minification is a great tool to use while developing or debugging complex Javascript projects with multiple source files. However, you can achieve better minificaton (more compact files) using a tool like Babel. In other words, use this tool for dev but use something like Babel for generating public releases.

![Screenshot](https://user-images.githubusercontent.com/10224874/207311409-8873da18-a7eb-4b81-9ccb-c4f5f7c82d44.jpg)

# Usage:
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
