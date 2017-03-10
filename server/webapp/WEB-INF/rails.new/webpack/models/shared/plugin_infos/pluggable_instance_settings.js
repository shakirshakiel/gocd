/*
 * Copyright 2017 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var _      = require('lodash');
var Stream = require('mithril/stream');
var Mixins = require('models/mixins/model_mixins');

var PluggableInstanceSettings = function (data) {
  this.viewTemplate   = Stream(data.viewTemplate);
  this.configurations = Stream(data.configurations);
};

PluggableInstanceSettings.fromJSON = function (data = {}) {
  return new PluggableInstanceSettings({
    configurations: PluggableInstanceSettings.Configurations.fromJSON(data.configurations),
    viewTemplate:   _.get(data, 'view.template'),
  });
};

PluggableInstanceSettings.Configurations = function (data) {
  Mixins.HasMany.call(this, {
    factory:    PluggableInstanceSettings.Configurations.Configuration.create,
    as:         'Configuration',
    collection: data,
    uniqueOn:   'key'
  });

};

PluggableInstanceSettings.Configurations.Configuration = function (data) {
  this.parent   = Mixins.GetterSetter();
  this.key      = Stream(data.key);
  this.metadata = Stream(data.metadata);
};

PluggableInstanceSettings.Configurations.Configuration.create = function (data) {
  return new PluggableInstanceSettings.Configurations.Configuration(data);
};

PluggableInstanceSettings.Configurations.Configuration.fromJSON = function (data = {}) {
  return new PluggableInstanceSettings.Configurations.Configuration.create({
    key:      data.key,
    metadata: data.metadata
  });
};

Mixins.fromJSONCollection({
  parentType: PluggableInstanceSettings.Configurations,
  childType:  PluggableInstanceSettings.Configurations.Configuration,
  via:        'addConfiguration'
});


module.exports = PluggableInstanceSettings;
