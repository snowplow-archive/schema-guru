# Schema Guru Web UI

[ ![License] [license-image] ] [license]

Schema Guru Web UI is a SPA interface for Schema Guru, allowing you to derive JSON schema using your web browser in a
very flexible and descriptive way.

## Functionality

* Drag and drop one or many JSON instances
* Type and submit one JSON instance
* Visual diff
* Display content of instance and possible parse errors with click on instance

## Build

Web UI built on top of Facebook's React.js framework and uses Gulp and Browserify for build.

All development code stored in js directory.
After build, concatenated and minified files will be stored in dist directory.

For production-ready build you need to use gulp deploy task.

For development you may want to split vendor (3rd-party libs) and bundle files.
Don't forget to include both in index.html.

Also you may want to serve static  files on another webserver to prevent sbt recompile
whole app on every change of static file.

NOTE: package.json is located in the root of the project, and node_modules should be placed
      here too in order to prevent sbt from put it into jar

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0

