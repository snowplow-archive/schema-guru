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
var request = require('superagent');
var Reflux = require('reflux');

var GuruActions = require('../actions');

/**
 * This store is responsible for storing, posting, including and annotating with errors
 * our JSON instances
 */
module.exports = Reflux.createStore({
    listenables: GuruActions,

    textInstanceCounter: 0, // private auxiliary counter
    enumCardinality: 0,     // value of enum cardinality tolerance
    instances: [],          // list of File object with some additional attributes
                            // TODO: consider changing array to object
                            // TODO: find a place to maintain these mutable attributes
                            //       error, errorMessage, included, data
                            // TODO: reduce number of methods can toggle data

    getInitialState: function() {
        return this.instances
    },

    /**
     * POST instances to Schema Guru API endpoint
     * @param instances
     */
    postInstances: function(instances) {
        var req = request.post('/upload');
        instances.forEach(file => req.attach(file.name, file));
        req.field('enumCardinality', this.enumCardinality)
           .end(GuruActions.schemaReceived);
    },

    /**
     * Use it to get items only with included checkbox
     * @returns Array of File
     */
    getIncludedItems: function() {
        return _.filter(this.instances, i => i.included === true || i.included === undefined)
    },

    /**
     * Handle addErrors action
     * Annotate all invalid instances with error messages
     * @param errors
     */
    onAddErrors: function (errors) {
        var self = this;
        _.forEach(errors, (e) => {          // add error message to corresponding instance
            _.map(self.instances, (i) => {
                if (e.file === i.name) {
                    i.error = e.error;
                    i.errorMessage = e.message;
                }
                return i
            })
        });
        this.trigger(this.instances);
    },

    /**
     * Handle instancePreviewLoaded action
     * Annotate every instance with it's preview when it loaded
     * @param instance
     * @param preview
     */
    onInstancePreviewLoaded: function (instance, preview) {
        _.map(this.instances, i => {
            if (i === instance) { i.data = preview } i
        });
        this.trigger(this.instances);
    },

    /**
     * Handle instancesAdded action
     * Maintain only unique names in our this.instances, and send them to server
     * @param instances
     */
    onInstancesAdded: function(instances) {
        // only unique names
        this.instances = _.unique(this.instances.concat(instances), i => i.name);

        this.postInstances(this.getIncludedItems());
        this.trigger(this.instances)
    },

    /**
     * Handle instanceToggled action
     * Post instances again, but without unchecked ones
     * @param instance
     * @param included
     */
    onInstanceToggled: function(instance, included) {
        this.instances.forEach(i => {
            if (i === instance) { i.included = included  }
        });
        this.postInstances(this.getIncludedItems());
    },

    /**
     * Handle textInstanceAdded
     * Wrap our text instance in array and send it to server
     * @param value
     */
    onTextInstanceAdded: function(value) {
        this.textInstanceCounter += 1;
        var file = new File([value], "text_instance_" + this.textInstanceCounter + ".json");
        GuruActions.instancesAdded([file])
    },

    /**
     * Handle enumCardinalityChanged event
     * Set InstanceStore's private var
     * @param cardinality
     */
    onEnumCardinalityChanged: function(cardinality) {
        this.enumCardinality = cardinality;
    }
});
