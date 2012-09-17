/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');
require('controllers/installer/step1_controller');

describe('App.InstallerStep1Controller', function () {

  describe('#validateStep1()', function () {
    it('should return false and sets invalidClusterName to true if cluster name is empty', function () {
      var controller = App.InstallerStep1Controller.create();
      controller.set('clusterName', '');
      expect(controller.validateStep1()).to.equal(false);
      expect(controller.get('invalidClusterName')).to.equal(true);
    })
    it('should return false and sets invalidClusterName to true if cluster name has whitespaces', function () {
      var controller = App.InstallerStep1Controller.create();
      controller.set('clusterName', 'My Cluster');
      expect(controller.validateStep1()).to.equal(false);
      expect(controller.get('invalidClusterName')).to.equal(true);
    })
    it('should return false and sets invalidClusterName to true if cluster name has special characters', function () {
      var controller = App.InstallerStep1Controller.create();
      controller.set('clusterName', 'my-cluster');
      expect(controller.validateStep1()).to.equal(false);
      expect(controller.get('invalidClusterName')).to.equal(true);
    })
    it('should return true, sets invalidClusterName to false, and sets cluster name in db if cluster name is valid', function () {
      var controller = App.InstallerStep1Controller.create();
      var clusterName = 'mycluster1';
      controller.set('clusterName', clusterName);
      // fake login so clusterName is properly retrieved from App.db
      App.db.setLoginName('myuser');
      expect(controller.validateStep1()).to.equal(true);
      expect(controller.get('invalidClusterName')).to.equal(false);
      expect(App.db.getClusterName()).to.equal(clusterName);
    })
  })

})