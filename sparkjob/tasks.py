# Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
#
# This program is licensed to you under the Apache License Version 2.0,
# and you may not use this file except in compliance with the Apache License Version 2.0.
# You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the Apache License Version 2.0 is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.

from invoke import run, task

import boto.s3
from boto.s3.connection import Location
from boto.s3.key import Key

import boto.emr
from boto.emr.step import InstallHiveStep, ScriptRunnerStep
from boto.emr.bootstrap_action import BootstrapAction

DIR_WITH_JAR = "./target/scala-2.10/"
JAR_FILE  = "schema-guru-sparkjob-0.6.1"

S3_REGIONS = { 'us-east-1': Location.DEFAULT,
               'us-west-1': Location.USWest,
               'us-west-2': Location.USWest2,
               'eu-west-1': Location.EU,
               'ap-southeast-1': Location.APSoutheast,
               'ap-southeast-2': Location.APSoutheast2,
               'ap-northeast-1': Location.APNortheast,
               'sa-east-1': Location.SAEast }

S3_LOCATIONS = {v: k for k, v in S3_REGIONS.items()}

# Taken from https://github.com/bencpeters/save-tweets/blob/ac276fac41e676ee12a426df56cbc60138a12e62/save-tweets.py
def get_valid_location(region):
    if region not in [i for i in dir(boto.s3.connection.Location) \
                               if i[0].isupper()]:
        try:
            return S3_REGIONS[region]
        except KeyError:
            raise ValueError("%s is not a known AWS location. Valid choices " \
                 "are:\n%s" % (region,  "\n".join( \
                 ["  *%s" % i for i in S3_REGIONS.keys()])))
    else:
        return getattr(Location, region)

def get_valid_region(location):
    return S3_LOCATIONS[location]

@task
def test():
    run('cd .. && sbt "project schema-guru-sparkjob" "test"', pty=True)

@task
def assembly():
   run('cd .. && sbt "project schema-guru-sparkjob" "assembly"', pty=True)

@task
def upload(profile, jar_bucket):
    c = boto.connect_s3(profile_name=profile)
    b = c.get_bucket(jar_bucket)

    k2 = Key(b)
    k2.key = "jar/" + JAR_FILE
    k2.set_contents_from_filename(DIR_WITH_JAR + JAR_FILE)

@task
def run_emr(profile, input_path, output_path, errors_path, log_path, ec2_keyname):
    c = boto.connect_s3(profile_name=profile)
    jar_bucket = c.get_bucket(input_path.split("/")[0])
    r = get_valid_region(jar_bucket.get_location())

    bootstrap_actions = [
        BootstrapAction("Install Spark", "s3://support.elasticmapreduce/spark/install-spark", ["-x"])
    ]

    args = [
        "/home/hadoop/spark/bin/spark-submit",
        "--deploy-mode",
        "cluster",
        "--master",
        "yarn-cluster",
        "--class",
        "com.snowplowanalytics.schemaguru.sparkjob.SchemaDeriveJob",
        "s3://snowplow-hosted-assets/schema-guru/spark/" + JAR_FILE,
        "--ndjson", # Assuming your source files contain many JSONs each, one per line
        "--errors-path",
        "s3n://" + errors_path,              # trailing slash is required
        "--output",
        "s3n://" + output_path,              # ...here too
        "s3n://" + input_path,               # ...here too
    ]
    steps = [
        InstallHiveStep(),
        ScriptRunnerStep("Run SchemaDeriveJob", step_args=args)
    ]

    conn = boto.emr.connect_to_region(r, profile_name=profile)
    job_id = conn.run_jobflow(
        name="Schema Derive Spark",
        log_uri="s3://" + log_path,
        ec2_keyname=ec2_keyname,
        master_instance_type="m3.xlarge",
        slave_instance_type="m3.xlarge",
        num_instances=3,
        enable_debugging=True,
        ami_version="3.8",
        steps=steps,
        bootstrap_actions=bootstrap_actions,
        job_flow_role="EMR_EC2_DefaultRole",
        service_role="EMR_DefaultRole"
    )
    print("Started jobflow " + job_id)
