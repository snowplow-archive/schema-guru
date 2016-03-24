/*
 * Copyright (c) 2012-2016 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.schemaddl.generators.redshift

// We're in schemadd.generators package because getColumnsDdl is private
// TODO: Create ALTER TABLE DDL in schema-ddl

// Schema Guru
import com.snowplowanalytics.schemaguru.Common.SchemaVer
import com.snowplowanalytics.schemaguru.Migrations.SchemaDiff

// Schema DDL
import com.snowplowanalytics.schemaddl.SchemaData.SelfDescInfo
import com.snowplowanalytics.schemaddl.generators.redshift.Ddl.Column
import com.snowplowanalytics.schemaddl.generators.redshift.RedshiftDdlGenerator._
import com.snowplowanalytics.schemaddl.utils.StringUtils._

/**
 * Class representing a migration between two additions
 *
 * @param vendor Schema vendor
 * @param name Schema name
 * @param from source Schema version
 * @param to target Schema version
 * @param diff ordered map of added Schema properties
 */
case class RedshiftMigration(
  vendor: String,
  name: String,
  from: SchemaVer,
  to: SchemaVer,
  diff: SchemaDiff) {
  def toDdl: String = toDdl(Some("atomic"))
  def toDdl(tableSchema: Option[String]) = RedshiftMigration.toDdl(this, tableSchema)
}

object RedshiftMigration {
  /**
   * Get same DDL file with all column names being snakified
   */
  def snakifyColumns(columns: List[Column]): List[Column] = {
    columns.map { column =>
      column.copy(columnName = snakify(column.columnName)) }
  }

  /**
   * Get lenghts of longest cells for nice DDL output
   *
   * @param columns list of table columns
   * @return tuple where each element is length of cell in DDL text
   */
  def getTabulation(columns: List[Column]): (Int, Int, Int, Int, Int) = {
    val longestName       = columns.map(_.nameDdl.length).max
    val longestDataType   = columns.map(_.dataTypeDdl.length).max
    val longestAttribute  = columns.map(_.attributesDdl.length).max
    val longestConstraint = columns.map(_.constraintsDdl.length).max
    (1, longestName, longestDataType, longestAttribute, longestConstraint)
  }

  /**
   * Transform migration to a DDL string
   *
   * @param migration migration to process
   * @param tableSchema optional DB schema (not JSON Schema!)
   * @return DDL ready to be written to file
   */
  def toDdl(migration: RedshiftMigration, tableSchema: Option[String]) = {
    val description    = SelfDescInfo(migration.vendor, migration.name, migration.to.asString)
    val oldSchemaUri   = getSchemaName(SelfDescInfo(migration.vendor, migration.name, migration.from.asString))
    val newSchemaUri   = getSchemaName(description)                           // e.g. iglu:com.acme/event/jsonschema/1-0-1
    val tableName      = getTableName(description)                            // e.g. com_acme_event_1
    val fullTableName  = tableSchema.map(_ + ".").getOrElse("") + tableName   // e.g. atomic.com_acme_event_1
    val quotedSchema   = tableSchema.map(n => "\"" + n +"\".").getOrElse("")  // e.g. "atomic".

    val transaction    =
      if (migration.diff.added.nonEmpty) {
        val columns    = snakifyColumns(getColumnsDdl(migration.diff.added, Set.empty, 4096).toList)
        val tabulation = getTabulation(columns)
        val columnsDdl = columns.map(_.toFormattedDdl(tabulation)).map("    ADD COLUMN" + _).mkString(",\n") + ";"
        s"""|  ALTER TABLE $fullTableName
            |$columnsDdl
            |""".stripMargin
      } else { """   -- NO ADDED COLUMNS CAN BE EXPRESSED IN SQL MIGRATION""" }

    s"""|-- WARNING: only apply this file to your database if the following SQL returns the expected:
        |--
        |-- SELECT pg_catalog.obj_description(c.oid) FROM pg_catalog.pg_class c WHERE c.relname = '$tableName';
        |--  obj_description
        |-- -----------------
        |--  $oldSchemaUri
        |-- (1 row)
        |
        |BEGIN TRANSACTION;
        |
        |$transaction
        |
        |  COMMENT ON TABLE ${quotedSchema}"${tableName}" IS '$newSchemaUri';
        |
        |END TRANSACTION; """.stripMargin
  }
}
