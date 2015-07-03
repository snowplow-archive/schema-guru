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

var _ = require("lodash");
var React = require('react');

/**
 * Component responsible for output duplicated keys warning
 */
module.exports = React.createClass({
    render: function () {
        if (_.isEmpty(this.props.warning)) {
            return ( <div></div> )
        }
        else {
            return (
                <div className="warning">
                    <h2>Warning</h2>
                    <div className="warning-message">{this.props.warning.message}</div>
                    <table className="warning-items">
                        <tbody>
                        { _.map(this.props.warning.items, i =>
                            (<tr key={i[0]+i[1]}>
                                <td>{i[0]}</td>
                                <td>{i[1]}</td>
                            </tr>)) }
                        </tbody>
                    </table>
                </div>
            )
        }
    }
});
