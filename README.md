### Quickstart

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

### Packaging

```bash
VERSION=0.1.0
cat bin/jarx-stub.sh target/scala-2.10/schema-guru.jar > target/scala-2.10/schema-guru
chmod +x target/scala-2.10/schema-guru
zip -j "package/snowplow_schema_guru_${VERSION}_linux.zip" target/scala-2.10/schema-guru
```

Then upload `package/snowplow_schema_guru_${VERSION}_linux.zip` to Snowplow's open source Bintray.
