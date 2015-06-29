/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0, and
 * you may not use this file except in compliance with the Apache License
 * Version 2.0.  You may obtain a copy of the Apache License Version 2.0 at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License Version 2.0 is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the Apache License Version 2.0 for the specific language
 * governing permissions and limitations there under.
 */
'use strict';

var Reflux = require('reflux');

module.exports = Reflux.createActions([
    "instancesAdded",           // we dropped something in dropzone
    "textInstanceAdded",        // we submitted a textarea with JSON
    "schemaReceived",           // we got response from server
    "instancePreviewLoaded",    // file uploaded and we can access to preview
    "instanceToggled",          // we include/exclude some instance
    "switchSchemaView",         // we switched view from plain to diff
    "addErrors",                // we received some errors
    "addWarning",               // we received warning
    "viewShowDiff",             // something wants to look at diff
    "viewShowPlain",            // something wants to look at plain schema
    "enumCardinalityChanged"    // input with enum cardinality changed
]);
