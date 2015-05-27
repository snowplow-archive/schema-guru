# Schema Guru

[! [Build Status] [travis-image] ] [travis]  [ ![License] [license-image] ] [license] [ ![Release] [release-image] ] [releases]

Schema guru is tool (currently only CLI) allowing you to derive [JSON Schema] [json-schema] from set of available JSONs.

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

Also you can specify output file for your schema

```bash
schema-guru > run --dir {{jsons_directory}} --output /Users/guru/new_schema.json
```

### Current Assumptions

* All JSONs in the directory are assumed to be of the same event type and will be merged together.
* All JSONs are assumed to start with either `{ ... }` or `[ ... ]`
  - If they do not they are discarded

### Current Functionality

* Can take a directory as an argument and will print out the resulting JsonSchema:
  - Processes each JSON sequentially
  - Merges all results into one master JsonSchema
* Recognize several string formats
  - uuid
  - date-time (accoridng to ISO-8601:2004)
  - IPv4 and IPv6 addresses
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
[releases]: https://github.com/snowplow/scala-forex/releases

[json-schema]: http://json-schema.org/
