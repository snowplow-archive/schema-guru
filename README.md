# Schema Guru

[! [Build Status] [travis-image] ] [travis]

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

### Current Assumptions

* All JSONs in the directory are assumed to be of the same event type and will be merged together.
* All JSONs are assumed to start with either `{ ... }` or `[ ... ]`
  - If they do not they are discarded

### Current Functionality

* Can take a directory as an argument and will print out the resulting JsonSchema:
  - Processes each JSON sequentially
  - Merges all results into one master JsonSchema

### What it does not do yet...

SchemaGuru only looks at the type of the field in the JSON, currently it does not bother trying to decipher what sort of field it could be.  For example detecting date-times in Strings or determining any hard limits on a particular field.

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
