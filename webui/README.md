# Schema Guru Web UI

[ ![License] [license-image] ] [license]

Schema Guru Web UI is a SPA interface for Schema Guru, allowing you to derive JSON schema using your web browser in a
very flexible and descriptive way.

## Quickstart

Download the latest Schema Guru Web UI from Bintray:

```bash
$ wget http://dl.bintray.com/snowplow/snowplow-generic/schema_guru_webui_0.2.0.zip
$ unzip schema_guru_webui_0.2.0.zip
$ ./schema-guru-webui-0.2.0
```

Web UI has only two optional CLI options: --interface and --port.

Everything else is described in main Schema Guru README.

## Functionality

* Drag and drop one or many JSON instances
* Type and submit one JSON instance
* Visual diff
* Display content of instance and possible parse errors with click on instance
* Specify enum cardinality

## Build

Web UI built on top of Facebook's React.js framework and uses Gulp and Browserify for build.

All development code stored in js directory.
After build, concatenated and minified files will be stored in dist directory.

NOTE: package.json is located in the root of the project, and node_modules should be placed
      here too in order to prevent sbt from put it into jar

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0

