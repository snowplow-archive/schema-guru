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
var ValidationSign = require('./ValidationSign');

/**
 * Table row with JSON instance
 */
module.exports = React.createClass({
    togglePreview: function () {
        this.setState({
            showPreview: !this.state.showPreview
        })
    },
    /**
     * Include/exclude current instance
     * @param e click event
     */
    toggleInstance: function (e) {
        e.stopPropagation();
        this.setState({
            include: !this.state.include
        }, () => GuruActions.instanceToggled(this.props.instance, this.state.include))
    },
    /**
     * Return hidden by CSS HTML with preview and possible with error
     * @returns {XML}
     */
    preview: function () {
        var tip;
        if (this.state.showPreview) {
            try {           // validate JSON
                tip = JSON.stringify(JSON.parse(this.props.instance.data), null, " ");
            } catch (e) {
                if (this.props.instance.type !== ('application/json')) {
                    tip = (<div>
                             <div>{this.props.instance.error}</div>
                             <div>{this.props.instance.errorMessage}</div>
                           </div>)
                }
                else {
                    tip = (<div>
                             <div>{this.props.instance.data}</div>
                             <div>{this.props.instance.error}</div>
                             <div>{this.props.instance.errorMessage}</div>
                           </div>)
                }
            }
            return <pre onClick={e => e.stopPropagation()} className="preview-tip">{tip}</pre>
        } else return <div></div>
    },
    /**
     * Return HTML with checkbox. It dissabled in case of invalid JSON instance
     * @returns {XML}
     */
    getCheckbox: function () {
        return this.props.instance.error ?
            <td><input type="checkbox" onClick={this.toggleInstance} checked={false} disabled /></td> :
            <td><input type="checkbox" onClick={this.toggleInstance} defaultChecked={this.state.include} /></td>

    },

    // React methods
    getInitialState: function () {
        return {
            showPreview: false,
            include: true
        }
    },
    componentDidMount: function () {
        var reader = new FileReader();
        reader.readAsText(this.props.instance);
        reader.onload = e => GuruActions.instancePreviewLoaded(this.props.instance, e.target.result)
    },
    render: function () {
        return (
            <tr onClick={this.togglePreview} >
                <td><ValidationSign error={this.props.instance.error} /></td>
                <td className="instance-name">{this.props.instance.name} { this.preview() }</td>
                <td>{this.props.instance.size} bytes</td>
                { this.getCheckbox() }
            </tr>
        )
    }
});

