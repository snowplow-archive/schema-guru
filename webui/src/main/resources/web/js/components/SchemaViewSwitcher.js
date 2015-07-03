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
 * Tabs responsible for switching view of schema
 */
module.exports = React.createClass({
    getClassName: function(key) {
        return key == this.props.currentView ? 'active' : ''
    },

    // React methods
    render: function () {
        return (<div className="schema-view">
            <a className={this.getClassName("diff")} onClick={GuruActions.viewShowDiff} href="#">Diff</a>
            <a className={this.getClassName("plain")} onClick={GuruActions.viewShowPlain} href="#">Plain</a>
        </div>)
    }
});

