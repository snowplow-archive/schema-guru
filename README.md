# Schema Guru

[ ![Build Status] [travis-image] ] [travis]  [ ![Release] [release-image] ] [releases] [ ![License] [license-image] ] [license]

Schema Guru is a tool (currently CLI only) allowing you to derive **[JSON Schemas] [json-schema]** from a set of JSON instances.

Unlike other tools for deriving JSON Schemas, Schema Guru allows you to derive schema from an unlimited set of instances (making schemas much more precise), and supports many more JSON Schema validation properties.

Schema Guru is used heavily in association with Snowplow's own **[Snowplow] [snowplow]** and **[Iglu] [iglu]** projects.

## User Quickstart

Download the latest Schema Guru from Bintray:

```bash
$ wget http://dl.bintray.com/snowplow/snowplow-generic/schema_guru_0.1.0.zip
$ unzip schema_guru_0.1.0.zip
```

Assuming you have a recent JVM installed:

```bash
$ ./schema-guru-0.1.0 --dir {{jsons_directory}}
```

Also you can specify output file for your schema:

```bash
$ ./schema-guru-0.1.0 --dir {{jsons_directory}} --output {{json_schema_file}}
```

Or you can analyze a single JSON instance:

```bash
$ ./schema-guru-0.1.0 --file {{json_instance}}
```

## Developer Quickstart

Assuming git, **[Vagrant] [vagrant-install]** and **[VirtualBox] [virtualbox-install]** installed:

```bash
 host$ git clone https://github.com/snowplow/schema-guru.git
 host$ cd schema-guru
 host$ vagrant up && vagrant ssh
guest$ cd /vagrant
guest$ sbt test
```

## User Manual

### Functionality

* Takes a directory as an argument and will print out the resulting JsonSchema:
  - Processes each JSON sequentially
  - Merges all results into one master Json Schema
* Recognizes following JSON Schema formats:
  - uuid
  - date-time (according to ISO-8601)
  - IPv4 and IPv6 addresses
  - HTTP, HTTPS, FTP URLs
* Detects integer ranges according to Int16, Int32, Int64

### Assumptions

* All JSONs in the directory are assumed to be of the same event type and will be merged together
* All JSONs are assumed to start with either `{ ... }` or `[ ... ]`
  - If they do not they are discarded
* Schema should be as strict as possible - e.g. no `additionalProperties` are allowed currently

### Example

Here's an example of some subtle points which a tool working with a single JSON instance would miss.

First instance:

```json
{ "event": {
    "just_a_string": "Any string may be here",
    "sometimes_ip": "192.168.1.101",
    "always_ipv4": "127.0.0.1",
    "id": 43,
    "very_big_int": 9223372036854775102,
    "this_should_be_number": 2.1,
    "nested_object": {
        "title": "Just an nested object",
        "date": "2015-05-29T12:00:00+07:00" }}}
```

Second instance:

```json
{ "event": {
    "just_a_string": "No particular format",
    "sometimes_ip": "This time it's not an IP",
    "always_ipv4": "192.168.1.101",
    "id": 42,
    "very_big_int": 92102,
    "this_should_be_number": 201,
    "not_always_here": 32,
    "nested_object": {
        "title": "Still plain string without format",
        "date": "1961-07-03T12:00:00+07:00" }}}
```

The generated schema:

```json
{ "type" : "object",
  "properties" : {
    "event" : {
      "type" : "object",
      "properties" : {
        "just_a_string" : { "type" : "string" },
        "sometimes_ip" : { "type" : "string" },
        "always_ipv4" : {
          "type" : "string",
          "format" : "ipv4" },
        "id" : {
          "type" : "integer",
          "minimum" : 0,
          "maximum" : 32767 },
        "very_big_int" : {
          "type" : "integer",
          "minimum" : 0,
          "maximum" : 9223372036854775807 },
        "this_should_be_number" : {
          "type" : "number",
          "minimum" : 0 },
        "nested_object" : {
          "type" : "object",
          "properties" : {
            "title" : { "type" : "string" },
            "date" : {
              "type" : "string",
              "format" : "date-time" } },
          "additionalProperties" : false },
        "not_always_here" : {
          "type" : "integer",
          "minimum" : 0,
          "maximum" : 32767 } },
      "additionalProperties" : false } },
  "additionalProperties" : false }
```

## Copyright and License

Schema Guru is copyright 2014-2015 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0] [license]** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[travis]: https://travis-ci.org/snowplow/schema-guru
[travis-image]: https://travis-ci.org/snowplow/schema-guru.png?branch=master

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0

[release-image]: http://img.shields.io/badge/release-0.1.0-blue.svg?style=flat
[releases]: https://github.com/snowplow/schema-guru/releases

[json-schema]: http://json-schema.org/

[snowplow]: https://github.com/snowplow/snowplow
[iglu]: https://github.com/snowplow/iglu

[vagrant-install]: http://docs.vagrantup.com/v2/installation/index.html
[virtualbox-install]: https://www.virtualbox.org/wiki/Downloads
