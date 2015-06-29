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
var Reflux = require('reflux');

var InstancesBlock = require('./components/InstancesBlock');
var SchemaBlock = require('./components/SchemaBlock');
var InstancesStore = require('./stores/InstancesStore');
var SchemaStore = require('./stores/SchemaStore');

/**
 * Root component
 */
var Container = React.createClass({
    mixins: [Reflux.connect(InstancesStore, "instances"),
             Reflux.connect(SchemaStore, "schema")],

    render: function () {
        return (
            <div className="wrapper">
                <InstancesBlock instances={this.state.instances} />
                <SchemaBlock schema={this.state.schema} />

                <footer>&copy; 2015 Snowplow Analytics</footer>
            </div>
        )
    }
});

document.addEventListener('DOMContentLoaded', function(){
    var mountNode = document.getElementById('root');
    React.render(<Container />, mountNode);
});