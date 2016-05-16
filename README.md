# Schema Guru

[ ![Build Status] [travis-image] ] [travis]  [ ![Release] [release-image] ] [releases] [ ![License] [license-image] ] [license]

Schema Guru is a tool (CLI, Spark job and web) allowing you to derive **[JSON Schemas] [json-schema]** from a set of JSON instances process and transform it into different data definition formats.

Current primary features include:

- deriviation of JSON Schema from set of JSON instances (``schema`` command)
- generation of **[Redshift] [redshift]** table DDL and JSONPaths file (``ddl`` command)

Unlike other tools for deriving JSON Schemas, Schema Guru allows you to derive schema from an unlimited set of instances (making schemas much more precise), and supports many more JSON Schema validation properties.

Schema Guru is used heavily in association with Snowplow's own **[Snowplow] [snowplow]**, **[Iglu] [iglu]** and **[Schema DDL] [schema-ddl]** projects.

## User Quickstart

Download the latest Schema Guru from Bintray:

```bash
$ wget http://dl.bintray.com/snowplow/snowplow-generic/schema_guru_0.6.2.zip
$ unzip schema_guru_0.6.2.zip
```

Assuming you have a recent JVM installed.

### CLI

#### Schema derivation

You can use as input either single JSON file or directory with JSON instances (it will be processed recursively).

Following command will print JSON Schema to stdout:

```bash
$ ./schema-guru-0.6.2 schema {{input}}
```

Also you can specify output file for your schema:

```bash
$ ./schema-guru-0.6.2 schema --output {{json_schema_file}} {{input}}
```

You can also switch Schema Guru into **[NDJSON] [ndjson]** mode, where it will look for newline delimited JSONs:

```bash
$ ./schema-guru-0.6.2 schema --ndjson {{input}}
```

You can specify the enum cardinality tolerance for your fields. It means that *all* fields which are found to have less than the specified cardinality will be specified in the JSON Schema using the `enum` property.

```bash
$ ./schema-guru-0.6.2 schema --enum 5 {{input}}
```

If you know that some particular set of values can appear, but don't want to set big enum cardinality, you may want to specify predefined enum set with ``--enum-sets`` multioption, like this:

```bash
$ ./schema-guru-0.6.2 schema --enum-sets iso_4217 --enum-sets iso_3166-1_alpha-3 /path/to/instances
```

Currently Schema Guru includes following built-in enum sets (written as they should appear in CLI):

* [iso_4217] [iso-4217]
* [iso_3166-1_alpha-2] [iso-3166-1-alpha-2]
* [iso_3166-1_alpha-3] [iso-3166-1-alpha-3]
* Special `all` set, which mean all built-in enums will be included

If you need to include very specific enum set, you can define it by yourself in JSON file with array like this:

```json
["Mozilla Firefox", "Google Chrome", "Netscape Navigator", "Internet Explorer"]
```

And pass path to this file instead of enum name:

```bash
$ ./schema-guru-0.6.2 schema --enum-sets all --enum-sets /path/to/browsers.json /path/to/instances
```

Schema Guru will derive `minLength` and `maxLength` properties for strings based on shortest and longest strings.
But this may be a problem if you process small amount of instances. 
To avoid this too strict Schema, you can use `--no-length` option.

```bash
$ ./schema-guru-0.6.2 schema --no-length /path/to/few-instances
```

#### DDL derivation

Like for Schema derivation, for DDL input may be also single file with JSON Schema or directory containing JSON Schemas.

Currently we support DDL only for **[Amazon Redshift] [redshift]**, but in future releases you'll be able to specify another with ``--db`` option.

Following command will just save Redshift (default ``--db`` value) DDL to current dir.

```bash
$ ./schema-guru-0.6.2 ddl {{input}}
```

If you specified as input a directory with several Self-describing JSON Schemas belonging to a single REVISION, Schema Guru will also generate a migrations.
So, you can migratte any of previous tables to any of subsequent.
For example, having following list of Self-describing JSON Schemas as input:

* schemas/com.acme/click_event/1-0-0
* schemas/com.acme/click_event/1-0-1
* schemas/com.acme/click_event/1-0-2

You will have following migrations as output:

* sql/com.acme/click_event/1-0-0/1-0-1 to alter table from 1-0-0 to 1-0-1
* sql/com.acme/click_event/1-0-0/1-0-2 to alter table from 1-0-0 to 1-0-2
* sql/com.acme/click_event/1-0-1/1-0-2 to alter table from 1-0-1 to 1-0-2

This migrations (and all subsequent table definitions) are aware of column order and it will never put a new column in the middle of table,
so you can safely alter your tables while they belong to a single REVISION.

You also can specify directory for output:

```bash
$ ./schema-guru-0.6.2 ddl --output {{ddl_dir}} {{input}}
```

If you're not a Snowplow Platform user, don't use **[Self-describing Schema] [self-describing]** or just don't want anything specific to it you can produce raw schema:

```bash
$ ./schema-guru-0.6.2 ddl --raw {{input}}
```

But bear in mind that Self-describing Schemas bring many benefits. 
For example, raw Schemas will not preserve an order for your columns (it just impossible!) and also you will not have a migrations.

You may also want to get JSONPaths file for Redshift's **[COPY] [redshift-copy]** command. It will place ``jsonpaths`` dir alongside with ``sql``:

```bash
$ ./schema-guru-0.6.2 ddl --with-json-paths {{input}}
```

The most embarrassing part of shifting from dynamic-typed world to static-typed is product types (or union types) like this in JSON Schema: ``["integer", "string"]``.
How to represent them in SQL DDL? It's a taught question and we think there's no ideal solution.
Thus we provide you two options. By default product types will be transformed as most general ``VARCHAR(4096)``.
But there's another way - you can split column with product types into separate ones with it's types as postfix, for example property ``model`` with type ``["string", "integer"]`` will be transformed into two columns ``mode_string`` and ``model_integer``.
This behavior can be achieved with ``--split-product-types``.

Another thing everyone need to consider is default VARCHAR size. If there's no clues about it (like ``maxLength``) 4096 will be used.
You can also specify this default value:

```bash
$ ./schema-guru-0.6.2 ddl --varchar-size 32 {{input}}
```

You can also specify Redshift Schema for your table. For non-raw mode ``atomic`` used as default.

```bash
$ ./schema-guru-0.6.2 ddl --raw --schema business {{input}}
```

Some users do not full rely on Schema Guru JSON Schema derivation or DDL generation and edit their DDLs manually.
By default, Schema Guru will not override your files (either DDLs and migrations) if user made any significant changes (comments and whitespaces are not significant).
Instead Schema Guru will just warn user that file has been changed manually.
To change this behavior you may specify ``--force`` flag.

```bash
$ ./schema-guru-0.6.2 ddl --force {{input}}
```


### Web UI

You can access our hosted demo of the Schema Guru web UI at [schemaguru.snplowanalytics.com] [webui-hosted]. To run it locally:

```bash
$ wget http://dl.bintray.com/snowplow/snowplow-generic/schema_guru_webui_0.6.2.zip
$ unzip schema_guru_webui_0.6.2.zip
$ ./schema-guru-webui-0.6.2
```

The above will run a Spray web server containing Schema Guru on [0.0.0.0:8000] [webui-local]. Interface and port can be specified by `--interface` and `--port` respectively.

### Apache Spark

Since version 0.4.0 Schema Guru shipping with Spark job for deriving JSON Schemas.
To help users getting started with Schema Guru on Amazon Elastic MapReduce we provide [pyinvoke] [pyinvoke] ``tasks.py``.

Recommended way to start is install all requirements and assembly fatjar as described in [Developer Quickstart](#developer-quickstart).

Before run you need:

* An AWS CLI profile, e.g. *my-profile*
* A EC2 keypair, e.g. *my-ec2-keypair*
* At least one Amazon S3 bucket, e.g. *my-bucket*

To provision the cluster and start the job you need to use `run_emr` task:

```bash
$ cd sparkjob
$ inv run_emr my-profile my-bucket/input/ my-bucket/output/ my-bucket/errors/ my-bucket/logs my-ec2-keypair
```

If you need some specific options for Spark job, you can specify these in `tasks.py`. 
The Spark job accepts the same options as the CLI application, but note that `--output` isn't optional and we have a new optional `--errors-path`.
Also, instead of specifying some of predefined enum sets you can just enable it with `--enum-sets` flag, so it has the same behaviour as `--enum-sets all`.

## Developer Quickstart

Assuming git, **[Vagrant] [vagrant-install]** and **[VirtualBox] [virtualbox-install]** installed:

```bash
 host$ git clone https://github.com/snowplow/schema-guru.git
 host$ cd schema-guru
 host$ vagrant up && vagrant ssh
guest$ cd /vagrant
guest$ sbt assembly
```

Also, optional:

```bash
guest$ sbt "project schema-guru-webui" assembly
guest$ sbt "project schema-guru-sparkjob" assembly
```

You can also deploy the Schema Guru web GUI onto Elastic Beanstalk:

```
guest$ cd beanstalk && zip beanstalk.zip *
```

Now just create a new Docker app in the **[Elastic Beanstalk Console] [beanstalk-console]** and upload this zipfile.

## User Manual

### Functionality

#### Schema derivation

* Takes a directory as an argument and will print out the resulting JsonSchema:
  - Processes each JSON sequentially
  - Merges all results into one master Json Schema
* Recognizes following JSON Schema formats:
  - uuid
  - date-time (according to ISO-8601)
  - IPv4 and IPv6 addresses
  - HTTP, HTTPS, FTP URLs
* Recoginzed minLength and maxLength properties for strings
* Recognizes base64 pattern for strings
* Detects integer ranges according to Int16, Int32, Int64
* Detects misspelt properties and produce warnings
* Detects enum values with specified cardinality
* Detects known enum sets built-in or specified by user
* Allows to output **[Self-describing JSON Schema] [self-describing]**
* Allows to produce JSON Schemas with different names based on given JSON Path
* Supports **[Newline Delimited JSON] [ndjson]**

#### DDL derivation

* Correctly transforms some of string formats
  - uuid becomes ``CHAR(36)``
  - ipv4 becomes ``VARCHAR(14)``
  - ipv6 becomes ``VARCHAR(39)``
  - date-time becomes ``TIMESTAMP``
* Handles properties with only enums
* Property with ``maxLength(n)`` and ``minLength(n)`` becomes ``CHAR(n)``
* Can output JSONPaths file
* Can split product types
* Number with ``multiplyOf`` 0.01 becomes ``DECIMAL``
* Handles Self-describing JSON and can produce raw DDL
* Recognizes integer size by ``minimum`` and ``maximum`` values
* Object without ``properties``, but with ``patternProperties`` becomes ``VARCHAR(4096)``

### Assumptions

* All JSONs in the directory are assumed to be of the same event type and will be merged together
* All JSONs are assumed to start with either `{ ... }` or `[ ... ]`
  - If they do not they are discarded
* Schema should be as strict as possible - e.g. no `additionalProperties` are allowed currently

### Self-describing JSON
``schema`` command allows you to produce **[Self-describing JSON Schema] [self-describing]**.
To produce it you need to specify vendor, name (if segmentation isn't using, see below), and version (optional, default value is 1-0-0).

```bash
$ ./schema-guru-0.6.2 schema --vendor {{your_company}} --name {{schema_name}} --schemaver {{version}} {{input}}
```

### Schema Segmentation

If you have set of mixed JSONs from one vendor, but with slightly different structure, like:

```json
{ "version": 1,
  "type": "track",
  "userId": "019mr8mf4r",
  "event": "Purchased an Item",
  "properties": {
    "revenue": "39.95",
    "shippingMethod": "2-day" },
  "timestamp" : "2012-12-02T00:30:08.276Z" }
```

and

```json
{ "version": 1,
  "type": "track",
  "userId": "019mr8mf4r",
  "event": "Posted a Comment",
  "properties": {
    "body": "This book is gorgeous!",
    "attachment": false },
  "timestamp" : "2012-12-02T00:28:02.273Z" }
```

You can run it as follows:
```bash
$ ./schema-guru-0.6.2 schema --output {{output_dir}} --schema-by $.event {{mixed_jsons_directory}}
```

It will put two (or may be more) JSON Schemas into output dir: Purchased_an_Item.json and Posted_a_comment.json.
They will be derived from JSONs only with corresponding event property, without any intersections.
Assuming that provided JSON Path contain valid string.
All schemas where this JSON Path is absent or contains not a string value will be merged into unmatched.json schema in the same output dir.
Also, when Self-describing JSON Schema producing, it will take schema name in the same way and --name argument can be omitted (it will replace name specified with option).

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

Schema Guru is copyright 2014-2016 Snowplow Analytics Ltd.

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

[release-image]: http://img.shields.io/badge/release-0.6.2-blue.svg?style=flat
[releases]: https://github.com/snowplow/schema-guru/releases

[json-schema]: http://json-schema.org/

[ndjson]: http://ndjson.org/
[ndjson-spec]: http://dataprotocols.org/ndjson/

[webui-local]: http://0.0.0.0:8000
[webui-hosted]: http://schemaguru.snowplowanalytics.com

[snowplow]: https://github.com/snowplow/snowplow
[iglu]: https://github.com/snowplow/iglu
[schema-ddl]: https://github.com/snowplow/schema-ddl
[self-describing]: http://snowplowanalytics.com/blog/2014/05/15/introducing-self-describing-jsons/

[redshift]: http://aws.amazon.com/redshift/
[redshift-copy]: http://docs.aws.amazon.com/redshift/latest/dg/r_COPY.html

[vagrant-install]: http://docs.vagrantup.com/v2/installation/index.html
[virtualbox-install]: https://www.virtualbox.org/wiki/Downloads
[pyinvoke]: http://www.pyinvoke.org/

[beanstalk-console]: http://console.aws.amazon.com/elasticbeanstalk

[iso-4217]: https://en.wikipedia.org/wiki/ISO_4217
[iso-3166-1-alpha-2]: https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
[iso-3166-1-alpha-3]: https://en.wikipedia.org/wiki/ISO_3166-1_alpha-3
