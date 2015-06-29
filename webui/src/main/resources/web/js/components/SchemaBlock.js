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

var SchemaViewSwitcher = require('./SchemaViewSwitcher');
var SchemaWarning = require('./SchemaWarning');

/**
 * Component displaying schema
 */
module.exports = React.createClass({
    render: function () {
        return (
            <div className='schema'>
                <h2>Your schema</h2>
                <SchemaViewSwitcher currentView={this.props.schema.currentView} />
                <pre dangerouslySetInnerHTML={{__html: this.props.schema.schemaText}} />
                <SchemaWarning warning={this.props.schema.warning} />
            </div>)
    }
});

