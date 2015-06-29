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
var React = require('react');

var Instance = require('./Instance');

/**
 * Uploaded instances
 */
module.exports = React.createClass({
    render: function () {
        if (this.props.instances.length == 0) { return <table className="instance-list"></table> }
        else { return (
            <table className="instance-list">
                <thead>
                <tr><th></th><th>Filename</th><th>Size</th><th>Include</th></tr>
                </thead>
                <tbody>
                    { _.map(this.props.instances, i => <Instance key={i.name} instance={i} /> ) }
                </tbody>
            </table>
        )}
    }
});


