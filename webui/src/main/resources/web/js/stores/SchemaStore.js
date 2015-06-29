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

var _ = require('lodash');
var Reflux = require('reflux');
var jsondiffpatch = require('jsondiffpatch');
var formatters = require('../formatters');

var GuruActions = require('../actions');

/**
 * This store is responsible for showing and processing JSON Schema
 */
module.exports = Reflux.createStore({
    listenables: GuruActions,

    // public state accessible from components
    state: {
        currentView: 'diff',    // current schema tab
        schemaText: '{}',       // textual representation of Schema (diff of plain text)
        warning: {}             // warning about possible duplicates
    },

    // private auxiliary state
    schema: {},         // JSON representation of Schema
    previous: {},       // previous JSON representation of Schema
    htmlDelta: '{}',    // cached HTML representation of diffed Schema

    getInitialState: function() {
        return this.state
    },

    /**
     * Handle all data necessary for diff computation
     */
    computeDelta: function() {
        var delta = jsondiffpatch.diff(this.previous, this.schema);
        var formatter = new formatters.HtmlFormatter();
        this.htmlDelta = formatter.format(delta, this.schema);
        this.previous = this.schema;
    },

    /**
     * Decide what tab to show
     */
    show: function() {
        switch (this.state.currentView) {
            case "diff":
                GuruActions.viewShowDiff();
                break;
            case "plain":
                GuruActions.viewShowPlain();
                break;
        }
    },

    /**
     * Fire addErrors action if errors found in response
     * @param res full superagent's response object
     */
    processErrors: function(res) {
        if (typeof res.body.errors !== 'undefined' && res.body.errors.length > 0) {
            GuruActions.addErrors(res.body.errors);
        }
    },

    /**
     * Fire addWarning if warning found in response
     * @param res full superagent's response object
     */
    processWarning: function(res) {
        if (typeof res.body.warning !== 'undefined') {
            GuruActions.addWarning(res.body.warning);
        }
    },

    /**
     * Handle schemaReceived action
     * @param err contains data on HTTP code != 200
     *        so far Guru API endpoint always return code 200
     * @param res our response object
     */
    onSchemaReceived: function(err, res) {
        this.processErrors(res);
        this.processWarning(res);
        this.schema = res.body.schema;
        if (_.isEqual(this.previous, this.schema)) { return }
        this.computeDelta();
        this.show();
    },

    /**
     * Handle viewShowDiff action
     */
    onViewShowDiff: function() {
        this.state.currentView = 'diff';
        this.state.schemaText = this.htmlDelta;
        this.trigger(this.state);
    },

    /**
     * Handle viewShowPlain action
     */
    onViewShowPlain: function() {
        this.state.currentView = 'plain';
        this.state.schemaText = JSON.stringify(this.schema, null, "  ");
        this.trigger(this.state);
    },

    /**
     * Handle addWarning action
     */
    onAddWarning: function(warning) {
        this.state.warning = warning;
        this.trigger(this.state);
    }
});

