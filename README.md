# Schema Guru

[ ![Build Status] [travis-image] ] [travis]  [ ![License] [license-image] ] [license] [ ![Release] [release-image] ] [releases]

Schema guru is tool (currently only CLI) allowing you to derive [JSON Schema] [json-schema] from set of available JSONs.

Unlike other tools for deriving JSON Schema, Schema Guru allows you to derive schema from unlimited set of instances,
and thus provide you drastically increased accuracy and much more validation properties.

## Quickstart

Assuming git, Vagrant and VirtualBox are installed:

```bash
 host$ git clone https://github.com/snowplow/schema-guru.git
 host$ cd schema-guru
 host$ vagrant up && vagrant ssh
guest$ cd /vagrant
guest$ sbt compile
``` 

To run SchemaGuru from SBT:

```bash
guest$ sbt
schema-guru > run --dir {{jsons_directory}}
```

Also you can specify output file for your schema:

```bash
schema-guru > run --dir {{jsons_directory}} --output {{json_schema_file}}
```

Or you can analyze only one JSON instance:

```bash
schema-guru > run --file {{json_instance}}
```

### Current Assumptions

* All JSONs in the directory are assumed to be of the same event type and will be merged together.
* All JSONs are assumed to start with either `{ ... }` or `[ ... ]`
  - If they do not they are discarded

# Example

Here's example including many of subtle mistakes which couldn't be found by tools working with single instances.

First instance:
```
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
```
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

Schema!
```
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

### Current Functionality

* Can take a directory as an argument and will print out the resulting JsonSchema:
  - Processes each JSON sequentially
  - Merges all results into one master JsonSchema
* Recognize following JSON Schema formats:
  - uuid
  - date-time (accoridng to ISO-8601:2004)
  - IPv4 and IPv6 addresses
  - HTTP, HTTPS, FTP URLs
* Detect integer ranges according to Int16, Int32, Int64

### What it does not do yet...

SchemaGuru also produces a very strict Schema in that there are zero additionalProperties allowed currently.

### Packaging

```bash
VERSION=0.1.0
cat bin/jarx-stub.sh target/scala-2.10/schema-guru.jar > target/scala-2.10/schema-guru
chmod +x target/scala-2.10/schema-guru
zip -j "package/snowplow_schema_guru_${VERSION}_linux.zip" target/scala-2.10/schema-guru
```

Then upload `package/snowplow_schema_guru_${VERSION}_linux.zip` to Snowplow's open source Bintray.


[travis]: https://travis-ci.org/snowplow/schema-guru
[travis-image]: https://travis-ci.org/snowplow/schema-guru.png?branch=master

[license-image]: http://img.shields.io/badge/license-Apache--2-blue.svg?style=flat
[license]: http://www.apache.org/licenses/LICENSE-2.0

[release-image]: http://img.shields.io/badge/release-unreleased-blue.svg?style=flat
[releases]: https://github.com/snowplow/schema-guru/releases

[json-schema]: http://json-schema.org/
