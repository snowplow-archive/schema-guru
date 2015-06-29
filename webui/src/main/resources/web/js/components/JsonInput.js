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

var React = require('react');

var GuruActions = require('../actions');

/**
 * Textarea with JSON
 */
module.exports = React.createClass({
    validate: function (ev) {
        var value = ev.target.value;
        try {
            JSON.parse(value);
            this.setState({valid: true, value: value})
        } catch (ex)  {
            this.setState({valid: false, value: value})
        }
    },
    submit: function() {
        GuruActions.textInstanceAdded(this.state.value)
    },
    getButtonText: function() {
        return this.state.valid ? "Submit" : "You have to enter valid JSON"
    },

    // React methods
    getInitialState: function () {
        return {
            valid: false,
            value: ''
        }
    },
    render: function() {
        return (
            <div>
                <textarea onChange={this.validate} placeholder="Or type it here..."></textarea>
                <br />
                <button onClick={this.submit} disabled={!this.state.valid}>{this.getButtonText()}</button>
            </div>)
    }
});

