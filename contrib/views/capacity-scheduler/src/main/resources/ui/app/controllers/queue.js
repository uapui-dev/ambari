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

var _runState = 'RUNNING';
var _stopState = 'STOPPED';

App.QueueController = Ember.ObjectController.extend({
  needs:['queues'],
  actions:{
    setState:function (state) {
      this.set('content.state', (state === "running") ? (this.get('content.state') == null) ? null : _runState : _stopState );
    },
    createQ:function () {
      this.get('controllers.queues').send('createQ',this.get('content'));
    },
    addQ:function (path) {
      this.get('controllers.queues').send('addQ',path);
    },
    delQ:function (record) {
      this.get('controllers.queues').send('delQ',record);
    },
    renameQ:function (opt) {

      var queue = this.get('content'),
          store = this.get('store'),
          queuesController = this.get('controllers.queues'),
          parentPath = queue.get('parentPath'),
          name, renamedQueueBackup;

      if (opt == 'ask') {
        this.set('tmpName', queue.getProperties('name','path','id'));
        queue.addObserver('name',this,this.setQueuePath);
        this.toggleProperty('isRenaming');
        return;
      }
      if (opt == 'cancel') {
        queue.set('name',this.get('tmpName.name'));
        queue.removeObserver('name',this,this.setQueuePath);
        this.toggleProperty('isRenaming');
        return;
      }

      if (opt && !queue.get('errors.path')) {
        name = queue.get('name');
        this.toggleProperty('isRenaming');
        queue.removeObserver('name',this,this.setQueuePath);
        queue.set('name',this.get('tmpName.name'));

        if (queue.get('isNewQueue')) {
          renamedQueueBackup = this.get('store').buildDeletedQueue(queue);
        }

        store.recurceRemoveQueue(queue).then(function (queue) {
          return (queue.get('isNewQueue')) ? renamedQueueBackup : store.get('deletedQueues').findBy('path',queue.get('path'));
        }).then(function (deletedQueue) {
          var targetDeleted =  store.get('deletedQueues').findBy('path',[parentPath,name].join('.')),
              queuePrototype = (targetDeleted) ? store.createFromDeleted(targetDeleted) : store.copyFromDeleted(deletedQueue,parentPath,name);

          return store.saveAndUpdateQueue(queuePrototype,deletedQueue);
        }).then(Em.run.bind(this,'transitionToRoute','queue'));
      }

    },
    toggleProperty:function (property,target) {
      target = target || this;
      target.toggleProperty(property);
    }
  },

  /**
   * Collection of modified fields in queue.
   * @type {Object} - { [fileldName] : {Boolean} }
   */
  queueDirtyFilelds:{},

  /**
   * Represents renaming status.
   * @type {Boolean}
   */
  isRenaming:false,

  /**
   * Object contains temporary name and path while renaming the queue.
   * @type {Object} - { name : {String}, path : {String} , id : {String}}
   */
  tmpName:{},

  /**
   * Possible values for ordering policy
   * @type {Array}
   */
  orderingPolicyValues: [null,'fifo', 'fair'],


  // COMPUTED PROPERTIES

  /**
   * Alias for user admin status.
   * @type {Boolean}
   */
  isOperator:Em.computed.alias('controllers.queues.isOperator'),

  /**
   * Inverted user admin status.
   * @type {Boolean}
   */
  isNotOperator:Em.computed.alias('controllers.queues.isNotOperator'),

  /**
   * All queues in store.
   * @type {DS.RecordArray}
   */
  allQueues:Em.computed.alias('controllers.queues.content'),

  /**
   * Array of leaf queues.
   * @return {Array}
   */
  leafQueues:function () {
    return this.get('allQueues').filterBy('parentPath',this.get('content.parentPath'));
  }.property('allQueues.length','content.parentPath'),

  /**
   * Parent of current queue.
   * @return {App.Queue}
   */
  parentQueue: function () {
    return this.store.getById('queue',this.get('content.parentPath').toLowerCase());
  }.property('content.parentPath'),

  /**
   * Returns true if queue is root.
   * @type {Boolean}
   */
  isRoot:Ember.computed.match('content.id', /^(root|root.default)$/),

  /**
   * Represents queue run state. Returns true if state is null.
   * @return {Boolean}
   */
  isRunning: function() {
    return this.get('content.state') == _runState || this.get('content.state') == null;
  }.property('content.state'),

  /**
   * Queue's acl_administer_queue property can be set to '*' (everyone) or ' ' (nobody) thru this property.
   *
   * @param  {String} key
   * @param  {String} value
   * @return {String} - '*' if equal to '*' or 'custom' in other case.
   */
  acl_administer_queue: function (key, value) {
    return this.handleAcl('content.acl_administer_queue',value);
  }.property('content.acl_administer_queue'),

  /**
   * Returns true if acl_administer_queue is set to '*'
   * @type {Boolean}
   */
  aaq_anyone:Ember.computed.equal('acl_administer_queue', '*'),

  /**
   * Queue's acl_submit_applications property can be set to '*' (everyone) or ' ' (nobody) thru this property.
   *
   * @param  {String} key
   * @param  {String} value
   * @return {String} - '*' if equal to '*' or 'custom' in other case.
   */
  acl_submit_applications: function (key, value) {
    return this.handleAcl('content.acl_submit_applications',value);
  }.property('content.acl_submit_applications'),

  /**
   * Returns true if acl_submit_applications is set to '*'
   * @type {Boolean}
   */
  asa_anyone:Ember.computed.equal('acl_submit_applications', '*'),

  /**
   * Error messages for queue path.
   * @type {Array}
   */
  pathErrors:Ember.computed.mapBy('content.errors.path','message'),

  /**
   * Current ordering policy value of queue.
   * @param  {String} key
   * @param  {String} value
   * @return {String}
   */
  currentOP:function (key,val) {
    if (arguments.length > 1) {
      if (!this.get('isFairOP')) {
        this.send('rollbackProp','enable_size_based_weight',this.get('content'));
      };
      this.set('content.ordering_policy',val || null);
    }
    return this.get('content.ordering_policy');
  }.property('content.ordering_policy'),

  /**
   * Does ordering policy is equal to 'fair'
   * @type {Boolean}
   */
  isFairOP: Em.computed.equal('content.ordering_policy','fair'),



  // OBSERVABLES

  /**
   * Keeps track of leaf queues and sets 'queues' value of parent to list of their names.
   * @method queueNamesControl
   */
  queueNamesControl:function () {
    if (this.get('parentQueue')) this.set('parentQueue.queuesArray',this.get('leafQueues').mapBy('name'));
  }.observes('allQueues.length','allQueues.@each.name','content'),

  /**
   * Adds observers for each queue attribute.
   * @method dirtyObserver
   */
  dirtyObserver:function () {
    this.get('content.constructor.transformedAttributes.keys.list').forEach(function(item) {
      this.addObserver('content.' + item,this,'propertyBecomeDirty');
    }.bind(this));
  }.observes('content'),



  // METHODS

  /**
   * Sets ACL value to '*' or ' ' and returns '*' and 'custom' respectively.
   * @param  {String} key   - ACL attribute
   * @param  {String} value - ACL value
   * @return {String}
   */
  handleAcl:function (key,value) {
    if (value) {
      this.set(key,(value == '*')?'*':' ');
    }
    return (this.get(key) == '*' || this.get(key) == null) ? '*' : 'custom';
  },

  /**
   * Sets queue path and id in accordance with the name.
   * Also adds errors to queue if name is incorrect.
   *
   * @method setQueuePath
   */
  setQueuePath:function (queue) {
    var name = queue.get('name').replace(/\s|\./g, ''),
        parentPath = queue.get('parentPath'),
        foundWithName;

    queue.set('name',name);

    foundWithName = this.store.all('queue').find(function (q) {
      return q.get('path') === [queue.get('parentPath'),queue.get('name')].join('.');
    });

    if (!Em.isEmpty(foundWithName) && queue.changedAttributes().hasOwnProperty('name')) {
      return this.get('content').get('errors').add('path', 'Queue already exists');
    } else if (name == '') {
      queue.get('errors').add('path', 'This field is required');
    } else {
      queue.get('errors').remove('path');
    }
  },

  /**
   * Adds modified queue fileds to q queueDirtyFilelds collection.
   * @param  {String} controller
   * @param  {String} property
   * @method propertyBecomeDirty
   */
  propertyBecomeDirty:function (controller, property) {
    var queueProp = property.split('.').objectAt(1);
    this.set('queueDirtyFilelds.' + queueProp, this.get('content').changedAttributes().hasOwnProperty(queueProp));
  }
});

App.ErrorController = Ember.ObjectController.extend();
