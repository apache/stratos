/*  Prototype JavaScript framework, version 1.5.0
 *  (c) 2005-2007 Sam Stephenson
 *
 *  Prototype is freely distributable under the terms of an MIT-style license.
 *  For details, see the Prototype web site: http://prototype.conio.net/
 *
/*--------------------------------------------------------------------------*/

var Prototype = {
  Version: '1.5.0',
  BrowserFeatures: {
    XPath: !!document.evaluate
  },

  ScriptFragment: '(?:<script.*?>)((\n|\r|.)*?)(?:<\/script>)',
  emptyFunction: function() {},
  K: function(x) { return x }
}

var Class = {
  create: function() {
    return function() {
      this.initialize.apply(this, arguments);
    }
  }
}

var $A = Array.from = function(iterable) {
  if (!iterable) return [];
  if (iterable.toArray) {
    return iterable.toArray();
  } else {
    var results = [];
    for (var i = 0, length = iterable.length; i < length; i++)
      results.push(iterable[i]);
    return results;
  }
}


var Abstract = new Object();

Object.extend = function(destination, source) {
  for (var property in source) {
    destination[property] = source[property];
  }
  return destination;
}

Object.extend(Object, {
  inspect: function(object) {
    try {
      if (object === undefined) return 'undefined';
      if (object === null) return 'null';
      return object.inspect ? object.inspect() : object.toString();
    } catch (e) {
      if (e instanceof RangeError) return '...';
      throw e;
    }
  },

  keys: function(object) {
    var keys = [];
    for (var property in object)
      keys.push(property);
    return keys;
  },

  values: function(object) {
    var values = [];
    for (var property in object)
      values.push(object[property]);
    return values;
  },

  clone: function(object) {
    return Object.extend({}, object);
  }
});

Function.prototype.bind = function() {
  var __method = this, args = $A(arguments), object = args.shift();
  return function() {
  	if (typeof $A == "function")
    return __method.apply(object, args.concat($A(arguments)));
  }
}

Function.prototype.bindAsEventListener = function(object) {
  var __method = this, args = $A(arguments), object = args.shift();
  return function(event) {

  	if (typeof $A == "function")
    return __method.apply(object, [( event || window.event)].concat(args).concat($A(arguments)));
  }
}

Object.extend(Number.prototype, {
  toColorPart: function() {
    var digits = this.toString(16);
    if (this < 16) return '0' + digits;
    return digits;
  },

  succ: function() {
    return this + 1;
  },

  times: function(iterator) {
    $R(0, this, true).each(iterator);
    return this;
  }
});

var Try = {
  these: function() {
    var returnValue;

    for (var i = 0, length = arguments.length; i < length; i++) {
      var lambda = arguments[i];
      try {
        returnValue = lambda();
        break;
      } catch (e) {}
    }

    return returnValue;
  }
}

/*--------------------------------------------------------------------------*/

var PeriodicalExecuter = Class.create();
PeriodicalExecuter.prototype = {
  initialize: function(callback, frequency) {
    this.callback = callback;
    this.frequency = frequency;
    this.currentlyExecuting = false;

    this.registerCallback();
  },

  registerCallback: function() {
    this.timer = setInterval(this.onTimerEvent.bind(this), this.frequency * 1000);
  },

  stop: function() {
    if (!this.timer) return;
    clearInterval(this.timer);
    this.timer = null;
  },

  onTimerEvent: function() {
    if (!this.currentlyExecuting) {
      try {
        this.currentlyExecuting = true;
        this.callback(this);
      } finally {
        this.currentlyExecuting = false;
      }
    }
  }
}
String.interpret = function(value){
  return value == null ? '' : String(value);
}

Object.extend(String.prototype, {
  gsub: function(pattern, replacement) {
    var result = '', source = this, match;
    replacement = arguments.callee.prepareReplacement(replacement);

    while (source.length > 0) {
      if (match = source.match(pattern)) {
        result += source.slice(0, match.index);
        result += String.interpret(replacement(match));
        source  = source.slice(match.index + match[0].length);
      } else {
        result += source, source = '';
      }
    }
    return result;
  },

  sub: function(pattern, replacement, count) {
    replacement = this.gsub.prepareReplacement(replacement);
    count = count === undefined ? 1 : count;

    return this.gsub(pattern, function(match) {
      if (--count < 0) return match[0];
      return replacement(match);
    });
  },

  scan: function(pattern, iterator) {
    this.gsub(pattern, iterator);
    return this;
  },

  truncate: function(length, truncation) {
    length = length || 30;
    truncation = truncation === undefined ? '...' : truncation;
    return this.length > length ?
      this.slice(0, length - truncation.length) + truncation : this;
  },

  strip: function() {
    return this.replace(/^\s+/, '').replace(/\s+$/, '');
  },

  stripTags: function() {
    return this.replace(/<\/?[^>]+>/gi, '');
  },

  stripScripts: function() {
    return this.replace(new RegExp(Prototype.ScriptFragment, 'img'), '');
  },

  extractScripts: function() {
    var matchAll = new RegExp(Prototype.ScriptFragment, 'img');
    var matchOne = new RegExp(Prototype.ScriptFragment, 'im');
    return (this.match(matchAll) || []).map(function(scriptTag) {
      return (scriptTag.match(matchOne) || ['', ''])[1];
    });
  },

  evalScripts: function() {
    return this.extractScripts().map(function(script) { return eval(script) });
  },

  escapeHTML: function() {
    var div = document.createElement('div');
    var text = document.createTextNode(this);
    div.appendChild(text);
    return div.innerHTML;
  },

  unescapeHTML: function() {
    var div = document.createElement('div');
    div.innerHTML = this.stripTags();
    return div.childNodes[0] ? (div.childNodes.length > 1 ?
      $A(div.childNodes).inject('',function(memo,node){ return memo+node.nodeValue }) :
      div.childNodes[0].nodeValue) : '';
  },

  toQueryParams: function(separator) {
    var match = this.strip().match(/([^?#]*)(#.*)?$/);
    if (!match) return {};

    return match[1].split(separator || '&').inject({}, function(hash, pair) {
      if ((pair = pair.split('='))[0]) {
        var name = decodeURIComponent(pair[0]);
        var value = pair[1] ? decodeURIComponent(pair[1]) : undefined;

        if (hash[name] !== undefined) {
          if (hash[name].constructor != Array)
            hash[name] = [hash[name]];
          if (value) hash[name].push(value);
        }
        else hash[name] = value;
      }
      return hash;
    });
  },

  toArray: function() {
    return this.split('');
  },

  succ: function() {
    return this.slice(0, this.length - 1) +
      String.fromCharCode(this.charCodeAt(this.length - 1) + 1);
  },

  camelize: function() {
    var parts = this.split('-'), len = parts.length;
    if (len == 1) return parts[0];

    var camelized = this.charAt(0) == '-'
      ? parts[0].charAt(0).toUpperCase() + parts[0].substring(1)
      : parts[0];

    for (var i = 1; i < len; i++)
      camelized += parts[i].charAt(0).toUpperCase() + parts[i].substring(1);

    return camelized;
  },

  capitalize: function(){
    return this.charAt(0).toUpperCase() + this.substring(1).toLowerCase();
  },

  underscore: function() {
    return this.gsub(/::/, '/').gsub(/([A-Z]+)([A-Z][a-z])/,'#{1}_#{2}').gsub(/([a-z\d])([A-Z])/,'#{1}_#{2}').gsub(/-/,'_').toLowerCase();
  },

  dasherize: function() {
    return this.gsub(/_/,'-');
  },

  inspect: function(useDoubleQuotes) {
    var escapedString = this.replace(/\\/g, '\\\\');
    if (useDoubleQuotes)
      return '"' + escapedString.replace(/"/g, '\\"') + '"';
    else
      return "'" + escapedString.replace(/'/g, '\\\'') + "'";
  }
});

String.prototype.gsub.prepareReplacement = function(replacement) {
  if (typeof replacement == 'function') return replacement;
  var template = new Template(replacement);
  return function(match) { return template.evaluate(match) };
}

String.prototype.parseQuery = String.prototype.toQueryParams;

var Template = Class.create();
Template.Pattern = /(^|.|\r|\n)(#\{(.*?)\})/;
Template.prototype = {
  initialize: function(template, pattern) {
    this.template = template.toString();
    this.pattern  = pattern || Template.Pattern;
  },

  evaluate: function(object) {
    return this.template.gsub(this.pattern, function(match) {
      var before = match[1];
      if (before == '\\') return match[2];
      return before + String.interpret(object[match[3]]);
    });
  }
}

var $break    = new Object();
var $continue = new Object();

var Enumerable = {
  each: function(iterator) {
    var index = 0;
    try {
      this._each(function(value) {
        try {
          iterator(value, index++);
        } catch (e) {
          if (e != $continue) throw e;
        }
      });
    } catch (e) {
      if (e != $break) throw e;
    }
    return this;
  },

  eachSlice: function(number, iterator) {
    var index = -number, slices = [], array = this.toArray();
    while ((index += number) < array.length)
      slices.push(array.slice(index, index+number));
    return slices.map(iterator);
  },

  all: function(iterator) {
    var result = true;
    this.each(function(value, index) {
      result = result && !!(iterator || Prototype.K)(value, index);
      if (!result) throw $break;
    });
    return result;
  },

  any: function(iterator) {
    var result = false;
    this.each(function(value, index) {
      if (result = !!(iterator || Prototype.K)(value, index))
        throw $break;
    });
    return result;
  },

  collect: function(iterator) {
    var results = [];
    this.each(function(value, index) {
      results.push((iterator || Prototype.K)(value, index));
    });
    return results;
  },

  detect: function(iterator) {
    var result;
    this.each(function(value, index) {
      if (iterator(value, index)) {
        result = value;
        throw $break;
      }
    });
    return result;
  },

  findAll: function(iterator) {
    var results = [];
    this.each(function(value, index) {
      if (iterator(value, index))
        results.push(value);
    });
    return results;
  },

  grep: function(pattern, iterator) {
    var results = [];
    this.each(function(value, index) {
      var stringValue = value.toString();
      if (stringValue.match(pattern))
        results.push((iterator || Prototype.K)(value, index));
    })
    return results;
  },

  include: function(object) {
    var found = false;
    this.each(function(value) {
      if (value == object) {
        found = true;
        throw $break;
      }
    });
    return found;
  },

  inGroupsOf: function(number, fillWith) {
    fillWith = fillWith === undefined ? null : fillWith;
    return this.eachSlice(number, function(slice) {
      while(slice.length < number) slice.push(fillWith);
      return slice;
    });
  },

  inject: function(memo, iterator) {
    this.each(function(value, index) {
      memo = iterator(memo, value, index);
    });
    return memo;
  },

  invoke: function(method) {
    var args = $A(arguments).slice(1);
    return this.map(function(value) {
      return value[method].apply(value, args);
    });
  },

  max: function(iterator) {
    var result;
    this.each(function(value, index) {
      value = (iterator || Prototype.K)(value, index);
      if (result == undefined || value >= result)
        result = value;
    });
    return result;
  },

  min: function(iterator) {
    var result;
    this.each(function(value, index) {
      value = (iterator || Prototype.K)(value, index);
      if (result == undefined || value < result)
        result = value;
    });
    return result;
  },

  partition: function(iterator) {
    var trues = [], falses = [];
    this.each(function(value, index) {
      ((iterator || Prototype.K)(value, index) ?
        trues : falses).push(value);
    });
    return [trues, falses];
  },

  pluck: function(property) {
    var results = [];
    this.each(function(value, index) {
      results.push(value[property]);
    });
    return results;
  },

  reject: function(iterator) {
    var results = [];
    this.each(function(value, index) {
      if (!iterator(value, index))
        results.push(value);
    });
    return results;
  },

  sortBy: function(iterator) {
    return this.map(function(value, index) {
      return {value: value, criteria: iterator(value, index)};
    }).sort(function(left, right) {
      var a = left.criteria, b = right.criteria;
      return a < b ? -1 : a > b ? 1 : 0;
    }).pluck('value');
  },

  toArray: function() {
    return this.map();
  },

  zip: function() {
    var iterator = Prototype.K, args = $A(arguments);
    if (typeof args.last() == 'function')
      iterator = args.pop();

    var collections = [this].concat(args).map($A);
    return this.map(function(value, index) {
      return iterator(collections.pluck(index));
    });
  },

  size: function() {
    return this.toArray().length;
  },

  inspect: function() {
    return '#<Enumerable:' + this.toArray().inspect() + '>';
  }
}

Object.extend(Enumerable, {
  map:     Enumerable.collect,
  find:    Enumerable.detect,
  select:  Enumerable.findAll,
  member:  Enumerable.include,
  entries: Enumerable.toArray
});

Object.extend(Array.prototype, Enumerable);

if (!Array.prototype._reverse)
  Array.prototype._reverse = Array.prototype.reverse;

Object.extend(Array.prototype, {
  _each: function(iterator) {
    for (var i = 0, length = this.length; i < length; i++)
      iterator(this[i]);
  },

  clear: function() {
    this.length = 0;
    return this;
  },

  first: function() {
    return this[0];
  },

  last: function() {
    return this[this.length - 1];
  },

  compact: function() {
    return this.select(function(value) {
      return value != null;
    });
  },

  flatten: function() {
    return this.inject([], function(array, value) {
      return array.concat(value && value.constructor == Array ?
        value.flatten() : [value]);
    });
  },

  without: function() {
    var values = $A(arguments);
    return this.select(function(value) {
      return !values.include(value);
    });
  },

  indexOf: function(object) {
    for (var i = 0, length = this.length; i < length; i++)
      if (this[i] == object) return i;
    return -1;
  },

  reverse: function(inline) {
    return (inline !== false ? this : this.toArray())._reverse();
  },

  reduce: function() {
    return this.length > 1 ? this : this[0];
  },

  uniq: function() {
    return this.inject([], function(array, value) {
      return array.include(value) ? array : array.concat([value]);
    });
  },

  clone: function() {
    return [].concat(this);
  },

  size: function() {
    return this.length;
  },

  inspect: function() {
    return '[' + this.map(Object.inspect).join(', ') + ']';
  }
});

Array.prototype.toArray = Array.prototype.clone;

function $w(string){
  string = string.strip();
  return string ? string.split(/\s+/) : [];
}

if(window.opera){
  Array.prototype.concat = function(){
    var array = [];
    for(var i = 0, length = this.length; i < length; i++) array.push(this[i]);
    for(var i = 0, length = arguments.length; i < length; i++) {
      if(arguments[i].constructor == Array) {
        for(var j = 0, arrayLength = arguments[i].length; j < arrayLength; j++)
          array.push(arguments[i][j]);
      } else {
        array.push(arguments[i]);
      }
    }
    return array;
  }
}
var Hash = function(obj) {
  Object.extend(this, obj || {});
};

Object.extend(Hash, {
  toQueryString: function(obj) {
    var parts = [];

	  this.prototype._each.call(obj, function(pair) {
      if (!pair.key) return;

      if (pair.value && pair.value.constructor == Array) {
        var values = pair.value.compact();
        if (values.length < 2) pair.value = values.reduce();
        else {
        	key = encodeURIComponent(pair.key);
          values.each(function(value) {
            value = value != undefined ? encodeURIComponent(value) : '';
            parts.push(key + '=' + encodeURIComponent(value));
          });
          return;
        }
      }
      if (pair.value == undefined) pair[1] = '';
      parts.push(pair.map(encodeURIComponent).join('='));
	  });

    return parts.join('&');
  }
});

Object.extend(Hash.prototype, Enumerable);
Object.extend(Hash.prototype, {
  _each: function(iterator) {
    for (var key in this) {
      var value = this[key];
      if (value && value == Hash.prototype[key]) continue;

      var pair = [key, value];
      pair.key = key;
      pair.value = value;
      iterator(pair);
    }
  },

  keys: function() {
    return this.pluck('key');
  },

  values: function() {
    return this.pluck('value');
  },

  merge: function(hash) {
    return $H(hash).inject(this, function(mergedHash, pair) {
      mergedHash[pair.key] = pair.value;
      return mergedHash;
    });
  },

  remove: function() {
    var result;
    for(var i = 0, length = arguments.length; i < length; i++) {
      var value = this[arguments[i]];
      if (value !== undefined){
        if (result === undefined) result = value;
        else {
          if (result.constructor != Array) result = [result];
          result.push(value)
        }
      }
      delete this[arguments[i]];
    }
    return result;
  },

  toQueryString: function() {
    return Hash.toQueryString(this);
  },

  inspect: function() {
    return '#<Hash:{' + this.map(function(pair) {
      return pair.map(Object.inspect).join(': ');
    }).join(', ') + '}>';
  }
});

function $H(object) {
  if (object && object.constructor == Hash) return object;
  return new Hash(object);
};
ObjectRange = Class.create();
Object.extend(ObjectRange.prototype, Enumerable);
Object.extend(ObjectRange.prototype, {
  initialize: function(start, end, exclusive) {
    this.start = start;
    this.end = end;
    this.exclusive = exclusive;
  },

  _each: function(iterator) {
    var value = this.start;
    while (this.include(value)) {
      iterator(value);
      value = value.succ();
    }
  },

  include: function(value) {
    if (value < this.start)
      return false;
    if (this.exclusive)
      return value < this.end;
    return value <= this.end;
  }
});

var $R = function(start, end, exclusive) {
  return new ObjectRange(start, end, exclusive);
}

var Ajax = {
  getTransport: function() {
    return Try.these(
      function() {return new XMLHttpRequest()},
      function() {return new ActiveXObject('Msxml2.XMLHTTP')},
      function() {return new ActiveXObject('Microsoft.XMLHTTP')}
    ) || false;
  },

  activeRequestCount: 0
}

Ajax.Responders = {
  responders: [],

  _each: function(iterator) {
    this.responders._each(iterator);
  },

  register: function(responder) {
    if (!this.include(responder))
      this.responders.push(responder);
  },

  unregister: function(responder) {
    this.responders = this.responders.without(responder);
  },

  dispatch: function(callback, request, transport, json) {
    this.each(function(responder) {
      if (typeof responder[callback] == 'function') {
        try {
          responder[callback].apply(responder, [request, transport, json]);
        } catch (e) {}
      }
    });
  }
};

Object.extend(Ajax.Responders, Enumerable);

Ajax.Responders.register({
  onCreate: function() {
    Ajax.activeRequestCount++;
  },
  onComplete: function() {
    Ajax.activeRequestCount--;
  }
});

Ajax.Base = function() {};
Ajax.Base.prototype = {
  setOptions: function(options) {
    this.options = {
      method:       'post',
      asynchronous: true,
      contentType:  'application/x-www-form-urlencoded',
      encoding:     'UTF-8',
      parameters:   ''
    }
    Object.extend(this.options, options || {});

    this.options.method = this.options.method.toLowerCase();
    if (typeof this.options.parameters == 'string')
      this.options.parameters = this.options.parameters.toQueryParams();
  }
}

Ajax.Request = Class.create();
Ajax.Request.Events =
  ['Uninitialized', 'Loading', 'Loaded', 'Interactive', 'Complete'];

Ajax.Request.prototype = Object.extend(new Ajax.Base(), {
  _complete: false,

  initialize: function(url, options) {
    this.transport = Ajax.getTransport();
    this.setOptions(options);
    this.request(url);
  },

  request: function(url) {
    this.url = url;
    this.method = this.options.method;
    var params = this.options.parameters;

    if (!['get', 'post'].include(this.method)) {
      // simulate other verbs over post
      params['_method'] = this.method;
      this.method = 'post';
    }

    params = Hash.toQueryString(params);
    if (params && /Konqueror|Safari|KHTML/.test(navigator.userAgent)) params += '&_='

    // when GET, append parameters to URL
    if (this.method == 'get' && params)
      this.url += (this.url.indexOf('?') > -1 ? '&' : '?') + params;

    try {
      Ajax.Responders.dispatch('onCreate', this, this.transport);

      this.transport.open(this.method.toUpperCase(), this.url,
        this.options.asynchronous);

      if (this.options.asynchronous)
        setTimeout(function() { this.respondToReadyState(1) }.bind(this), 10);

      this.transport.onreadystatechange = this.onStateChange.bind(this);
      this.setRequestHeaders();

      var body = this.method == 'post' ? (this.options.postBody || params) : null;

      this.transport.send(body);

      /* Force Firefox to handle ready state 4 for synchronous requests */
      if (!this.options.asynchronous && this.transport.overrideMimeType)
        this.onStateChange();

    }
    catch (e) {
      this.dispatchException(e);
    }
  },

  onStateChange: function() {
    var readyState = this.transport.readyState;
    if (readyState > 1 && !((readyState == 4) && this._complete))
      this.respondToReadyState(this.transport.readyState);
  },

  setRequestHeaders: function() {
    var headers = {
      'X-Requested-With': 'XMLHttpRequest',
      'X-Prototype-Version': Prototype.Version,
      'Accept': 'text/javascript, text/html, application/xml, text/xml, */*'
    };

    if (this.method == 'post') {
      headers['Content-type'] = this.options.contentType +
        (this.options.encoding ? '; charset=' + this.options.encoding : '');

      /* Force "Connection: close" for older Mozilla browsers to work
       * around a bug where XMLHttpRequest sends an incorrect
       * Content-length header. See Mozilla Bugzilla #246651.
       */
      if (this.transport.overrideMimeType &&
          (navigator.userAgent.match(/Gecko\/(\d{4})/) || [0,2005])[1] < 2005)
            headers['Connection'] = 'close';
    }

    // user-defined headers
    if (typeof this.options.requestHeaders == 'object') {
      var extras = this.options.requestHeaders;

      if (typeof extras.push == 'function')
        for (var i = 0, length = extras.length; i < length; i += 2)
          headers[extras[i]] = extras[i+1];
      else
        $H(extras).each(function(pair) { headers[pair.key] = pair.value });
    }

    for (var name in headers)
      this.transport.setRequestHeader(name, headers[name]);
  },

  success: function() {
    return !this.transport.status
        || (this.transport.status >= 200 && this.transport.status < 300);
  },

  respondToReadyState: function(readyState) {
    var state = Ajax.Request.Events[readyState];
    var transport = this.transport, json = this.evalJSON();

    if (state == 'Complete') {
      try {
        this._complete = true;
        (this.options['on' + this.transport.status]
         || this.options['on' + (this.success() ? 'Success' : 'Failure')]
         || Prototype.emptyFunction)(transport, json);
      } catch (e) {
        this.dispatchException(e);
      }

      if ((this.getHeader('Content-type') || 'text/javascript').strip().
        match(/^(text|application)\/(x-)?(java|ecma)script(;.*)?$/i))
          this.evalResponse();
    }

    try {
      (this.options['on' + state] || Prototype.emptyFunction)(transport, json);
      Ajax.Responders.dispatch('on' + state, this, transport, json);
    } catch (e) {
      this.dispatchException(e);
    }

    if (state == 'Complete') {
      // avoid memory leak in MSIE: clean up
      this.transport.onreadystatechange = Prototype.emptyFunction;
    }
  },

  getHeader: function(name) {
    try {
      return this.transport.getResponseHeader(name);
    } catch (e) { return null }
  },

  evalJSON: function() {
    try {
      var json = this.getHeader('X-JSON');
      return json ? eval('(' + json + ')') : null;
    } catch (e) { return null }
  },

  evalResponse: function() {
    try {
      return eval(this.transport.responseText);
    } catch (e) {
      this.dispatchException(e);
    }
  },

  dispatchException: function(exception) {
    (this.options.onException || Prototype.emptyFunction)(this, exception);
    Ajax.Responders.dispatch('onException', this, exception);
  }
});

Ajax.Updater = Class.create();

Object.extend(Object.extend(Ajax.Updater.prototype, Ajax.Request.prototype), {
  initialize: function(container, url, options) {
    this.container = {
      success: (container.success || container),
      failure: (container.failure || (container.success ? null : container))
    }

    this.transport = Ajax.getTransport();
    this.setOptions(options);

    var onComplete = this.options.onComplete || Prototype.emptyFunction;
    this.options.onComplete = (function(transport, param) {
      this.updateContent();
      onComplete(transport, param);
    }).bind(this);

    this.request(url);
  },

  updateContent: function() {
    var receiver = this.container[this.success() ? 'success' : 'failure'];
    var response = this.transport.responseText;

    if (!this.options.evalScripts) response = response.stripScripts();

    if (receiver = $(receiver)) {
      if (this.options.insertion)
        new this.options.insertion(receiver, response);
      else
        receiver.update(response);
    }

    if (this.success()) {
      if (this.onComplete)
        setTimeout(this.onComplete.bind(this), 10);
    }
  }
});

Ajax.PeriodicalUpdater = Class.create();
Ajax.PeriodicalUpdater.prototype = Object.extend(new Ajax.Base(), {
  initialize: function(container, url, options) {
    this.setOptions(options);
    this.onComplete = this.options.onComplete;

    this.frequency = (this.options.frequency || 2);
    this.decay = (this.options.decay || 1);

    this.updater = {};
    this.container = container;
    this.url = url;

    this.start();
  },

  start: function() {
    this.options.onComplete = this.updateComplete.bind(this);
    this.onTimerEvent();
  },

  stop: function() {
    this.updater.options.onComplete = undefined;
    clearTimeout(this.timer);
    (this.onComplete || Prototype.emptyFunction).apply(this, arguments);
  },

  updateComplete: function(request) {
    if (this.options.decay) {
      this.decay = (request.responseText == this.lastText ?
        this.decay * this.options.decay : 1);

      this.lastText = request.responseText;
    }
    this.timer = setTimeout(this.onTimerEvent.bind(this),
      this.decay * this.frequency * 1000);
  },

  onTimerEvent: function() {
    this.updater = new Ajax.Updater(this.container, this.url, this.options);
  }
});
function $(element) {
  if (arguments.length > 1) {
    for (var i = 0, elements = [], length = arguments.length; i < length; i++)
      elements.push($(arguments[i]));
    return elements;
  }
  if (typeof element == 'string')
    element = document.getElementById(element);
  return Element.extend(element);
}

if (Prototype.BrowserFeatures.XPath) {
  document._getElementsByXPath = function(expression, parentElement) {
    var results = [];
    var query = document.evaluate(expression, $(parentElement) || document,
      null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
    for (var i = 0, length = query.snapshotLength; i < length; i++)
      results.push(query.snapshotItem(i));
    return results;
  };
}

document.getElementsByClassName = function(className, parentElement) {
  if (Prototype.BrowserFeatures.XPath) {
    var q = ".//*[contains(concat(' ', @class, ' '), ' " + className + " ')]";
    return document._getElementsByXPath(q, parentElement);
  } else {
    var children = ($(parentElement) || document.body).getElementsByTagName('*');
    var elements = [], child;
    for (var i = 0, length = children.length; i < length; i++) {
      child = children[i];
      if (Element.hasClassName(child, className))
        elements.push(Element.extend(child));
    }
    return elements;
  }
};

/*--------------------------------------------------------------------------*/

if (!window.Element)
  var Element = new Object();

Element.extend = function(element) {
  if (!element || _nativeExtensions || element.nodeType == 3) return element;

  if (!element._extended && element.tagName && element != window) {
    var methods = Object.clone(Element.Methods), cache = Element.extend.cache;

    if (element.tagName == 'FORM')
      Object.extend(methods, Form.Methods);
    if (['INPUT', 'TEXTAREA', 'SELECT'].include(element.tagName))
      Object.extend(methods, Form.Element.Methods);

    Object.extend(methods, Element.Methods.Simulated);

    for (var property in methods) {
      var value = methods[property];
      if (typeof value == 'function' && !(property in element))
        element[property] = cache.findOrStore(value);
    }
  }

  element._extended = true;
  return element;
};

Element.extend.cache = {
  findOrStore: function(value) {
    return this[value] = this[value] || function() {
      return value.apply(null, [this].concat($A(arguments)));
    }
  }
};

Element.Methods = {
  visible: function(element) {
    return $(element).style.display != 'none';
  },

  toggle: function(element) {
    element = $(element);
    Element[Element.visible(element) ? 'hide' : 'show'](element);
    return element;
  },

  hide: function(element) {
    $(element).style.display = 'none';
    return element;
  },

  show: function(element) {
    $(element).style.display = '';
    return element;
  },

  remove: function(element) {
    element = $(element);
    element.parentNode.removeChild(element);
    return element;
  },

  update: function(element, html) {
    html = typeof html == 'undefined' ? '' : html.toString();
    $(element).innerHTML = html.stripScripts();
    setTimeout(function() {html.evalScripts()}, 10);
    return element;
  },

  replace: function(element, html) {
    element = $(element);
    html = typeof html == 'undefined' ? '' : html.toString();
    if (element.outerHTML) {
      element.outerHTML = html.stripScripts();
    } else {
      var range = element.ownerDocument.createRange();
      range.selectNodeContents(element);
      element.parentNode.replaceChild(
        range.createContextualFragment(html.stripScripts()), element);
    }
    setTimeout(function() {html.evalScripts()}, 10);
    return element;
  },

  inspect: function(element) {
    element = $(element);
    var result = '<' + element.tagName.toLowerCase();
    $H({'id': 'id', 'className': 'class'}).each(function(pair) {
      var property = pair.first(), attribute = pair.last();
      var value = (element[property] || '').toString();
      if (value) result += ' ' + attribute + '=' + value.inspect(true);
    });
    return result + '>';
  },

  recursivelyCollect: function(element, property) {
    element = $(element);
    var elements = [];
    while (element = element[property])
      if (element.nodeType == 1)
        elements.push(Element.extend(element));
    return elements;
  },

  ancestors: function(element) {
    return $(element).recursivelyCollect('parentNode');
  },

  descendants: function(element) {
    return $A($(element).getElementsByTagName('*'));
  },

  immediateDescendants: function(element) {
    if (!(element = $(element).firstChild)) return [];
    while (element && element.nodeType != 1) element = element.nextSibling;
    if (element) return [element].concat($(element).nextSiblings());
    return [];
  },

  previousSiblings: function(element) {
    return $(element).recursivelyCollect('previousSibling');
  },

  nextSiblings: function(element) {
    return $(element).recursivelyCollect('nextSibling');
  },

  siblings: function(element) {
    element = $(element);
    return element.previousSiblings().reverse().concat(element.nextSiblings());
  },

  match: function(element, selector) {
    if (typeof selector == 'string')
      selector = new Selector(selector);
    return selector.match($(element));
  },

  up: function(element, expression, index) {
    return Selector.findElement($(element).ancestors(), expression, index);
  },

  down: function(element, expression, index) {
    return Selector.findElement($(element).descendants(), expression, index);
  },

  previous: function(element, expression, index) {
    return Selector.findElement($(element).previousSiblings(), expression, index);
  },

  next: function(element, expression, index) {
    return Selector.findElement($(element).nextSiblings(), expression, index);
  },

  getElementsBySelector: function() {
    var args = $A(arguments), element = $(args.shift());
    return Selector.findChildElements(element, args);
  },

  getElementsByClassName: function(element, className) {
    return document.getElementsByClassName(className, element);
  },

  readAttribute: function(element, name) {
    element = $(element);
    if (document.all && !window.opera) {
      var t = Element._attributeTranslations;
      if (t.values[name]) return t.values[name](element, name);
      if (t.names[name])  name = t.names[name];
      var attribute = element.attributes[name];
      if(attribute) return attribute.nodeValue;
    }
    return element.getAttribute(name);
  },

  getHeight: function(element) {
    return $(element).getDimensions().height;
  },

  getWidth: function(element) {
    return $(element).getDimensions().width;
  },

  classNames: function(element) {
    return new Element.ClassNames(element);
  },

  hasClassName: function(element, className) {
    if (!(element = $(element))) return;
    var elementClassName = element.className;
    if (elementClassName.length == 0) return false;
    if (elementClassName == className ||
        elementClassName.match(new RegExp("(^|\\s)" + className + "(\\s|$)")))
      return true;
    return false;
  },

  addClassName: function(element, className) {
    if (!(element = $(element))) return;
    Element.classNames(element).add(className);
    return element;
  },

  removeClassName: function(element, className) {
    if (!(element = $(element))) return;
    Element.classNames(element).remove(className);
    return element;
  },

  toggleClassName: function(element, className) {
    if (!(element = $(element))) return;
    Element.classNames(element)[element.hasClassName(className) ? 'remove' : 'add'](className);
    return element;
  },

  observe: function() {
    Event.observe.apply(Event, arguments);
    return $A(arguments).first();
  },

  stopObserving: function() {
    Event.stopObserving.apply(Event, arguments);
    return $A(arguments).first();
  },

  // removes whitespace-only text node children
  cleanWhitespace: function(element) {
    element = $(element);
    var node = element.firstChild;
    while (node) {
      var nextNode = node.nextSibling;
      if (node.nodeType == 3 && !/\S/.test(node.nodeValue))
        element.removeChild(node);
      node = nextNode;
    }
    return element;
  },

  empty: function(element) {
    return $(element).innerHTML.match(/^\s*$/);
  },

  descendantOf: function(element, ancestor) {
    element = $(element), ancestor = $(ancestor);
    while (element = element.parentNode)
      if (element == ancestor) return true;
    return false;
  },

  scrollTo: function(element) {
    element = $(element);
    var pos = Position.cumulativeOffset(element);
    window.scrollTo(pos[0], pos[1]);
    return element;
  },

  getStyle: function(element, style) {
    element = $(element);
    if (['float','cssFloat'].include(style))
      style = (typeof element.style.styleFloat != 'undefined' ? 'styleFloat' : 'cssFloat');
    style = style.camelize();
    var value = element.style[style];
    if (!value) {
      if (document.defaultView && document.defaultView.getComputedStyle) {
        var css = document.defaultView.getComputedStyle(element, null);
        value = css ? css[style] : null;
      } else if (element.currentStyle) {
        value = element.currentStyle[style];
      }
    }

    if((value == 'auto') && ['width','height'].include(style) && (element.getStyle('display') != 'none'))
      value = element['offset'+style.capitalize()] + 'px';

    if (window.opera && ['left', 'top', 'right', 'bottom'].include(style))
      if (Element.getStyle(element, 'position') == 'static') value = 'auto';
    if(style == 'opacity') {
      if(value) return parseFloat(value);
      if(value = (element.getStyle('filter') || '').match(/alpha\(opacity=(.*)\)/))
        if(value[1]) return parseFloat(value[1]) / 100;
      return 1.0;
    }
    return value == 'auto' ? null : value;
  },

  setStyle: function(element, style) {
    element = $(element);
    for (var name in style) {
      var value = style[name];
      if(name == 'opacity') {
        if (value == 1) {
          value = (/Gecko/.test(navigator.userAgent) &&
            !/Konqueror|Safari|KHTML/.test(navigator.userAgent)) ? 0.999999 : 1.0;
          if(/MSIE/.test(navigator.userAgent) && !window.opera)
            element.style.filter = element.getStyle('filter').replace(/alpha\([^\)]*\)/gi,'');
        } else if(value === '') {
          if(/MSIE/.test(navigator.userAgent) && !window.opera)
            element.style.filter = element.getStyle('filter').replace(/alpha\([^\)]*\)/gi,'');
        } else {
          if(value < 0.00001) value = 0;
          if(/MSIE/.test(navigator.userAgent) && !window.opera)
            element.style.filter = element.getStyle('filter').replace(/alpha\([^\)]*\)/gi,'') +
              'alpha(opacity='+value*100+')';
        }
      } else if(['float','cssFloat'].include(name)) name = (typeof element.style.styleFloat != 'undefined') ? 'styleFloat' : 'cssFloat';
      element.style[name.camelize()] = value;
    }
    return element;
  },

  getDimensions: function(element) {
    element = $(element);
    var display = $(element).getStyle('display');
    if (display != 'none' && display != null) // Safari bug
      return {width: element.offsetWidth, height: element.offsetHeight};

    // All *Width and *Height properties give 0 on elements with display none,
    // so enable the element temporarily
    var els = element.style;
    var originalVisibility = els.visibility;
    var originalPosition = els.position;
    var originalDisplay = els.display;
    els.visibility = 'hidden';
    els.position = 'absolute';
    els.display = 'block';
    var originalWidth = element.clientWidth;
    var originalHeight = element.clientHeight;
    els.display = originalDisplay;
    els.position = originalPosition;
    els.visibility = originalVisibility;
    return {width: originalWidth, height: originalHeight};
  },

  makePositioned: function(element) {
    element = $(element);
    var pos = Element.getStyle(element, 'position');
    if (pos == 'static' || !pos) {
      element._madePositioned = true;
      element.style.position = 'relative';
      // Opera returns the offset relative to the positioning context, when an
      // element is position relative but top and left have not been defined
      if (window.opera) {
        element.style.top = 0;
        element.style.left = 0;
      }
    }
    return element;
  },

  undoPositioned: function(element) {
    element = $(element);
    if (element._madePositioned) {
      element._madePositioned = undefined;
      element.style.position =
        element.style.top =
        element.style.left =
        element.style.bottom =
        element.style.right = '';
    }
    return element;
  },

  makeClipping: function(element) {
    element = $(element);
    if (element._overflow) return element;
    element._overflow = element.style.overflow || 'auto';
    if ((Element.getStyle(element, 'overflow') || 'visible') != 'hidden')
      element.style.overflow = 'hidden';
    return element;
  },

  undoClipping: function(element) {
    element = $(element);
    if (!element._overflow) return element;
    element.style.overflow = element._overflow == 'auto' ? '' : element._overflow;
    element._overflow = null;
    return element;
  }
};

Object.extend(Element.Methods, {childOf: Element.Methods.descendantOf});

Element._attributeTranslations = {};

Element._attributeTranslations.names = {
  colspan:   "colSpan",
  rowspan:   "rowSpan",
  valign:    "vAlign",
  datetime:  "dateTime",
  accesskey: "accessKey",
  tabindex:  "tabIndex",
  enctype:   "encType",
  maxlength: "maxLength",
  readonly:  "readOnly",
  longdesc:  "longDesc"
};

Element._attributeTranslations.values = {
  _getAttr: function(element, attribute) {
    return element.getAttribute(attribute, 2);
  },

  _flag: function(element, attribute) {
    return $(element).hasAttribute(attribute) ? attribute : null;
  },

  style: function(element) {
    return element.style.cssText.toLowerCase();
  },

  title: function(element) {
    var node = element.getAttributeNode('title');
    return node.specified ? node.nodeValue : null;
  }
};

Object.extend(Element._attributeTranslations.values, {
  href: Element._attributeTranslations.values._getAttr,
  src:  Element._attributeTranslations.values._getAttr,
  disabled: Element._attributeTranslations.values._flag,
  checked:  Element._attributeTranslations.values._flag,
  readonly: Element._attributeTranslations.values._flag,
  multiple: Element._attributeTranslations.values._flag
});

Element.Methods.Simulated = {
  hasAttribute: function(element, attribute) {
    var t = Element._attributeTranslations;
    attribute = t.names[attribute] || attribute;
    return $(element).getAttributeNode(attribute).specified;
  }
};

// IE is missing .innerHTML support for TABLE-related elements
if (document.all && !window.opera){
  Element.Methods.update = function(element, html) {
    element = $(element);
    html = typeof html == 'undefined' ? '' : html.toString();
    var tagName = element.tagName.toUpperCase();
    if (['THEAD','TBODY','TR','TD'].include(tagName)) {
      var div = document.createElement('div');
      switch (tagName) {
        case 'THEAD':
        case 'TBODY':
          div.innerHTML = '<table><tbody>' +  html.stripScripts() + '</tbody></table>';
          depth = 2;
          break;
        case 'TR':
          div.innerHTML = '<table><tbody><tr>' +  html.stripScripts() + '</tr></tbody></table>';
          depth = 3;
          break;
        case 'TD':
          div.innerHTML = '<table><tbody><tr><td>' +  html.stripScripts() + '</td></tr></tbody></table>';
          depth = 4;
      }
      $A(element.childNodes).each(function(node){
        element.removeChild(node)
      });
      depth.times(function(){ div = div.firstChild });

      $A(div.childNodes).each(
        function(node){ element.appendChild(node) });
    } else {
      element.innerHTML = html.stripScripts();
    }
    setTimeout(function() {html.evalScripts()}, 10);
    return element;
  }
};

Object.extend(Element, Element.Methods);

var _nativeExtensions = false;

if(/Konqueror|Safari|KHTML/.test(navigator.userAgent))
  ['', 'Form', 'Input', 'TextArea', 'Select'].each(function(tag) {
    var className = 'HTML' + tag + 'Element';
    if(window[className]) return;
    var klass = window[className] = {};
    klass.prototype = document.createElement(tag ? tag.toLowerCase() : 'div').__proto__;
  });

Element.addMethods = function(methods) {
  Object.extend(Element.Methods, methods || {});

  function copy(methods, destination, onlyIfAbsent) {
    onlyIfAbsent = onlyIfAbsent || false;
    var cache = Element.extend.cache;
    for (var property in methods) {
      var value = methods[property];
      if (!onlyIfAbsent || !(property in destination))
        destination[property] = cache.findOrStore(value);
    }
  }

  if (typeof HTMLElement != 'undefined') {
    copy(Element.Methods, HTMLElement.prototype);
    copy(Element.Methods.Simulated, HTMLElement.prototype, true);
    copy(Form.Methods, HTMLFormElement.prototype);
    [HTMLInputElement, HTMLTextAreaElement, HTMLSelectElement].each(function(klass) {
      copy(Form.Element.Methods, klass.prototype);
    });
    _nativeExtensions = true;
  }
}

var Toggle = new Object();
Toggle.display = Element.toggle;

/*--------------------------------------------------------------------------*/

Abstract.Insertion = function(adjacency) {
  this.adjacency = adjacency;
}

Abstract.Insertion.prototype = {
  initialize: function(element, content) {
    this.element = $(element);
    this.content = content.stripScripts();

    if (this.adjacency && this.element.insertAdjacentHTML) {
      try {
        this.element.insertAdjacentHTML(this.adjacency, this.content);
      } catch (e) {
        var tagName = this.element.tagName.toUpperCase();
        if (['TBODY', 'TR'].include(tagName)) {
          this.insertContent(this.contentFromAnonymousTable());
        } else {
          throw e;
        }
      }
    } else {
      this.range = this.element.ownerDocument.createRange();
      if (this.initializeRange) this.initializeRange();
      this.insertContent([this.range.createContextualFragment(this.content)]);
    }

    setTimeout(function() {content.evalScripts()}, 10);
  },

  contentFromAnonymousTable: function() {
    var div = document.createElement('div');
    div.innerHTML = '<table><tbody>' + this.content + '</tbody></table>';
    return $A(div.childNodes[0].childNodes[0].childNodes);
  }
}

var Insertion = new Object();

Insertion.Before = Class.create();
Insertion.Before.prototype = Object.extend(new Abstract.Insertion('beforeBegin'), {
  initializeRange: function() {
    this.range.setStartBefore(this.element);
  },

  insertContent: function(fragments) {
    fragments.each((function(fragment) {
      this.element.parentNode.insertBefore(fragment, this.element);
    }).bind(this));
  }
});

Insertion.Top = Class.create();
Insertion.Top.prototype = Object.extend(new Abstract.Insertion('afterBegin'), {
  initializeRange: function() {
    this.range.selectNodeContents(this.element);
    this.range.collapse(true);
  },

  insertContent: function(fragments) {
    fragments.reverse(false).each((function(fragment) {
      this.element.insertBefore(fragment, this.element.firstChild);
    }).bind(this));
  }
});

Insertion.Bottom = Class.create();
Insertion.Bottom.prototype = Object.extend(new Abstract.Insertion('beforeEnd'), {
  initializeRange: function() {
    this.range.selectNodeContents(this.element);
    this.range.collapse(this.element);
  },

  insertContent: function(fragments) {
    fragments.each((function(fragment) {
      this.element.appendChild(fragment);
    }).bind(this));
  }
});

Insertion.After = Class.create();
Insertion.After.prototype = Object.extend(new Abstract.Insertion('afterEnd'), {
  initializeRange: function() {
    this.range.setStartAfter(this.element);
  },

  insertContent: function(fragments) {
    fragments.each((function(fragment) {
      this.element.parentNode.insertBefore(fragment,
        this.element.nextSibling);
    }).bind(this));
  }
});

/*--------------------------------------------------------------------------*/

Element.ClassNames = Class.create();
Element.ClassNames.prototype = {
  initialize: function(element) {
    this.element = $(element);
  },

  _each: function(iterator) {
    this.element.className.split(/\s+/).select(function(name) {
      return name.length > 0;
    })._each(iterator);
  },

  set: function(className) {
    this.element.className = className;
  },

  add: function(classNameToAdd) {
    if (this.include(classNameToAdd)) return;
    this.set($A(this).concat(classNameToAdd).join(' '));
  },

  remove: function(classNameToRemove) {
    if (!this.include(classNameToRemove)) return;
    this.set($A(this).without(classNameToRemove).join(' '));
  },

  toString: function() {
    return $A(this).join(' ');
  }
};

Object.extend(Element.ClassNames.prototype, Enumerable);
var Selector = Class.create();
Selector.prototype = {
  initialize: function(expression) {
    this.params = {classNames: []};
    this.expression = expression.toString().strip();
    this.parseExpression();
    this.compileMatcher();
  },

  parseExpression: function() {
    function abort(message) { throw 'Parse error in selector: ' + message; }

    if (this.expression == '')  abort('empty expression');

    var params = this.params, expr = this.expression, match, modifier, clause, rest;
    while (match = expr.match(/^(.*)\[([a-z0-9_:-]+?)(?:([~\|!]?=)(?:"([^"]*)"|([^\]\s]*)))?\]$/i)) {
      params.attributes = params.attributes || [];
      params.attributes.push({name: match[2], operator: match[3], value: match[4] || match[5] || ''});
      expr = match[1];
    }

    if (expr == '*') return this.params.wildcard = true;

    while (match = expr.match(/^([^a-z0-9_-])?([a-z0-9_-]+)(.*)/i)) {
      modifier = match[1], clause = match[2], rest = match[3];
      switch (modifier) {
        case '#':       params.id = clause; break;
        case '.':       params.classNames.push(clause); break;
        case '':
        case undefined: params.tagName = clause.toUpperCase(); break;
        default:        abort(expr.inspect());
      }
      expr = rest;
    }

    if (expr.length > 0) abort(expr.inspect());
  },

  buildMatchExpression: function() {
    var params = this.params, conditions = [], clause;

    if (params.wildcard)
      conditions.push('true');
    if (clause = params.id)
      conditions.push('element.readAttribute("id") == ' + clause.inspect());
    if (clause = params.tagName)
      conditions.push('element.tagName.toUpperCase() == ' + clause.inspect());
    if ((clause = params.classNames).length > 0)
      for (var i = 0, length = clause.length; i < length; i++)
        conditions.push('element.hasClassName(' + clause[i].inspect() + ')');
    if (clause = params.attributes) {
      clause.each(function(attribute) {
        var value = 'element.readAttribute(' + attribute.name.inspect() + ')';
        var splitValueBy = function(delimiter) {
          return value + ' && ' + value + '.split(' + delimiter.inspect() + ')';
        }

        switch (attribute.operator) {
          case '=':       conditions.push(value + ' == ' + attribute.value.inspect()); break;
          case '~=':      conditions.push(splitValueBy(' ') + '.include(' + attribute.value.inspect() + ')'); break;
          case '|=':      conditions.push(
                            splitValueBy('-') + '.first().toUpperCase() == ' + attribute.value.toUpperCase().inspect()
                          ); break;
          case '!=':      conditions.push(value + ' != ' + attribute.value.inspect()); break;
          case '':
          case undefined: conditions.push('element.hasAttribute(' + attribute.name.inspect() + ')'); break;
          default:        throw 'Unknown operator ' + attribute.operator + ' in selector';
        }
      });
    }

    return conditions.join(' && ');
  },

  compileMatcher: function() {
    this.match = new Function('element', 'if (!element.tagName) return false; \
      element = $(element); \
      return ' + this.buildMatchExpression());
  },

  findElements: function(scope) {
    var element;

    if (element = $(this.params.id))
      if (this.match(element))
        if (!scope || Element.childOf(element, scope))
          return [element];

    scope = (scope || document).getElementsByTagName(this.params.tagName || '*');

    var results = [];
    for (var i = 0, length = scope.length; i < length; i++)
      if (this.match(element = scope[i]))
        results.push(Element.extend(element));

    return results;
  },

  toString: function() {
    return this.expression;
  }
}

Object.extend(Selector, {
  matchElements: function(elements, expression) {
    var selector = new Selector(expression);
    return elements.select(selector.match.bind(selector)).map(Element.extend);
  },

  findElement: function(elements, expression, index) {
    if (typeof expression == 'number') index = expression, expression = false;
    return Selector.matchElements(elements, expression || '*')[index || 0];
  },

  findChildElements: function(element, expressions) {
    return expressions.map(function(expression) {
      return expression.match(/[^\s"]+(?:"[^"]*"[^\s"]+)*/g).inject([null], function(results, expr) {
        var selector = new Selector(expr);
        return results.inject([], function(elements, result) {
          return elements.concat(selector.findElements(result || element));
        });
      });
    }).flatten();
  }
});

function $$() {
  return Selector.findChildElements(document, $A(arguments));
}
var Form = {
  reset: function(form) {
    $(form).reset();
    return form;
  },

  serializeElements: function(elements, getHash) {
    var data = elements.inject({}, function(result, element) {
      if (!element.disabled && element.name) {
        var key = element.name, value = $(element).getValue();
        if (value != undefined) {
          if (result[key]) {
            if (result[key].constructor != Array) result[key] = [result[key]];
            result[key].push(value);
          }
          else result[key] = value;
        }
      }
      return result;
    });

    return getHash ? data : Hash.toQueryString(data);
  }
};

Form.Methods = {
  serialize: function(form, getHash) {
    return Form.serializeElements(Form.getElements(form), getHash);
  },

  getElements: function(form) {
    return $A($(form).getElementsByTagName('*')).inject([],
      function(elements, child) {
        if (Form.Element.Serializers[child.tagName.toLowerCase()])
          elements.push(Element.extend(child));
        return elements;
      }
    );
  },

  getInputs: function(form, typeName, name) {
    form = $(form);
    var inputs = form.getElementsByTagName('input');

    if (!typeName && !name) return $A(inputs).map(Element.extend);

    for (var i = 0, matchingInputs = [], length = inputs.length; i < length; i++) {
      var input = inputs[i];
      if ((typeName && input.type != typeName) || (name && input.name != name))
        continue;
      matchingInputs.push(Element.extend(input));
    }

    return matchingInputs;
  },

  disable: function(form) {
    form = $(form);
    form.getElements().each(function(element) {
      element.blur();
      element.disabled = 'true';
    });
    return form;
  },

  enable: function(form) {
    form = $(form);
    form.getElements().each(function(element) {
      element.disabled = '';
    });
    return form;
  },

  findFirstElement: function(form) {
    return $(form).getElements().find(function(element) {
      return element.type != 'hidden' && !element.disabled &&
        ['input', 'select', 'textarea'].include(element.tagName.toLowerCase());
    });
  },

  focusFirstElement: function(form) {
    form = $(form);
    form.findFirstElement().activate();
    return form;
  }
}

Object.extend(Form, Form.Methods);

/*--------------------------------------------------------------------------*/

Form.Element = {
  focus: function(element) {
    $(element).focus();
    return element;
  },

  select: function(element) {
    $(element).select();
    return element;
  }
}

Form.Element.Methods = {
  serialize: function(element) {
    element = $(element);
    if (!element.disabled && element.name) {
      var value = element.getValue();
      if (value != undefined) {
        var pair = {};
        pair[element.name] = value;
        return Hash.toQueryString(pair);
      }
    }
    return '';
  },

  getValue: function(element) {
    element = $(element);
    var method = element.tagName.toLowerCase();
    return Form.Element.Serializers[method](element);
  },

  clear: function(element) {
    $(element).value = '';
    return element;
  },

  present: function(element) {
    return $(element).value != '';
  },

  activate: function(element) {
    element = $(element);
    element.focus();
    if (element.select && ( element.tagName.toLowerCase() != 'input' ||
      !['button', 'reset', 'submit'].include(element.type) ) )
      element.select();
    return element;
  },

  disable: function(element) {
    element = $(element);
    element.disabled = true;
    return element;
  },

  enable: function(element) {
    element = $(element);
    element.blur();
    element.disabled = false;
    return element;
  }
}

Object.extend(Form.Element, Form.Element.Methods);
var Field = Form.Element;
var $F = Form.Element.getValue;

/*--------------------------------------------------------------------------*/

Form.Element.Serializers = {
  input: function(element) {
    switch (element.type.toLowerCase()) {
      case 'checkbox':
      case 'radio':
        return Form.Element.Serializers.inputSelector(element);
      default:
        return Form.Element.Serializers.textarea(element);
    }
  },

  inputSelector: function(element) {
    return element.checked ? element.value : null;
  },

  textarea: function(element) {
    return element.value;
  },

  select: function(element) {
    return this[element.type == 'select-one' ?
      'selectOne' : 'selectMany'](element);
  },

  selectOne: function(element) {
    var index = element.selectedIndex;
    return index >= 0 ? this.optionValue(element.options[index]) : null;
  },

  selectMany: function(element) {
    var values, length = element.length;
    if (!length) return null;

    for (var i = 0, values = []; i < length; i++) {
      var opt = element.options[i];
      if (opt.selected) values.push(this.optionValue(opt));
    }
    return values;
  },

  optionValue: function(opt) {
    // extend element because hasAttribute may not be native
    return Element.extend(opt).hasAttribute('value') ? opt.value : opt.text;
  }
}

/*--------------------------------------------------------------------------*/

Abstract.TimedObserver = function() {}
Abstract.TimedObserver.prototype = {
  initialize: function(element, frequency, callback) {
    this.frequency = frequency;
    this.element   = $(element);
    this.callback  = callback;

    this.lastValue = this.getValue();
    this.registerCallback();
  },

  registerCallback: function() {
    setInterval(this.onTimerEvent.bind(this), this.frequency * 1000);
  },

  onTimerEvent: function() {
    var value = this.getValue();
    var changed = ('string' == typeof this.lastValue && 'string' == typeof value
      ? this.lastValue != value : String(this.lastValue) != String(value));
    if (changed) {
      this.callback(this.element, value);
      this.lastValue = value;
    }
  }
}

Form.Element.Observer = Class.create();
Form.Element.Observer.prototype = Object.extend(new Abstract.TimedObserver(), {
  getValue: function() {
    return Form.Element.getValue(this.element);
  }
});

Form.Observer = Class.create();
Form.Observer.prototype = Object.extend(new Abstract.TimedObserver(), {
  getValue: function() {
    return Form.serialize(this.element);
  }
});

/*--------------------------------------------------------------------------*/

Abstract.EventObserver = function() {}
Abstract.EventObserver.prototype = {
  initialize: function(element, callback) {
    this.element  = $(element);
    this.callback = callback;

    this.lastValue = this.getValue();
    if (this.element.tagName.toLowerCase() == 'form')
      this.registerFormCallbacks();
    else
      this.registerCallback(this.element);
  },

  onElementEvent: function() {
    var value = this.getValue();
    if (this.lastValue != value) {
      this.callback(this.element, value);
      this.lastValue = value;
    }
  },

  registerFormCallbacks: function() {
    Form.getElements(this.element).each(this.registerCallback.bind(this));
  },

  registerCallback: function(element) {
    if (element.type) {
      switch (element.type.toLowerCase()) {
        case 'checkbox':
        case 'radio':
          Event.observe(element, 'click', this.onElementEvent.bind(this));
          break;
        default:
          Event.observe(element, 'change', this.onElementEvent.bind(this));
          break;
      }
    }
  }
}

Form.Element.EventObserver = Class.create();
Form.Element.EventObserver.prototype = Object.extend(new Abstract.EventObserver(), {
  getValue: function() {
    return Form.Element.getValue(this.element);
  }
});

Form.EventObserver = Class.create();
Form.EventObserver.prototype = Object.extend(new Abstract.EventObserver(), {
  getValue: function() {
    return Form.serialize(this.element);
  }
});
if (!window.Event) {
  var Event = new Object();
}

Object.extend(Event, {
  KEY_BACKSPACE: 8,
  KEY_TAB:       9,
  KEY_RETURN:   13,
  KEY_ESC:      27,
  KEY_LEFT:     37,
  KEY_UP:       38,
  KEY_RIGHT:    39,
  KEY_DOWN:     40,
  KEY_DELETE:   46,
  KEY_HOME:     36,
  KEY_END:      35,
  KEY_PAGEUP:   33,
  KEY_PAGEDOWN: 34,

  element: function(event) {
    return event.target || event.srcElement;
  },

  isLeftClick: function(event) {
    return (((event.which) && (event.which == 1)) ||
            ((event.button) && (event.button == 1)));
  },

  pointerX: function(event) {
    return event.pageX || (event.clientX +
      (document.documentElement.scrollLeft || document.body.scrollLeft));
  },

  pointerY: function(event) {
    return event.pageY || (event.clientY +
      (document.documentElement.scrollTop || document.body.scrollTop));
  },

  stop: function(event) {
    if (event.preventDefault) {
      event.preventDefault();
      event.stopPropagation();
    } else {
      event.returnValue = false;
      event.cancelBubble = true;
    }
  },

  // find the first node with the given tagName, starting from the
  // node the event was triggered on; traverses the DOM upwards
  findElement: function(event, tagName) {
    var element = Event.element(event);
    while (element.parentNode && (!element.tagName ||
        (element.tagName.toUpperCase() != tagName.toUpperCase())))
      element = element.parentNode;
    return element;
  },

  observers: false,

  _observeAndCache: function(element, name, observer, useCapture) {
    if (!this.observers) this.observers = [];
    if (element.addEventListener) {
      this.observers.push([element, name, observer, useCapture]);
      element.addEventListener(name, observer, useCapture);
    } else if (element.attachEvent) {
      this.observers.push([element, name, observer, useCapture]);
      element.attachEvent('on' + name, observer);
    }
  },

  unloadCache: function() {
    if (!Event.observers) return;
    for (var i = 0, length = Event.observers.length; i < length; i++) {
      Event.stopObserving.apply(this, Event.observers[i]);
      Event.observers[i][0] = null;
    }
    Event.observers = false;
  },

  observe: function(element, name, observer, useCapture) {
    element = $(element);
    useCapture = useCapture || false;

    if (name == 'keypress' &&
        (navigator.appVersion.match(/Konqueror|Safari|KHTML/)
        || element.attachEvent))
      name = 'keydown';

    Event._observeAndCache(element, name, observer, useCapture);
  },

  stopObserving: function(element, name, observer, useCapture) {
    element = $(element);
    useCapture = useCapture || false;

    if (name == 'keypress' &&
        (navigator.appVersion.match(/Konqueror|Safari|KHTML/)
        || element.detachEvent))
      name = 'keydown';

    if (element.removeEventListener) {
      element.removeEventListener(name, observer, useCapture);
    } else if (element.detachEvent) {
      try {
        element.detachEvent('on' + name, observer);
      } catch (e) {}
    }
  }
});

/* prevent memory leaks in IE */
if (navigator.appVersion.match(/\bMSIE\b/))
  Event.observe(window, 'unload', Event.unloadCache, false);
var Position = {
  // set to true if needed, warning: firefox performance problems
  // NOT neeeded for page scrolling, only if draggable contained in
  // scrollable elements
  includeScrollOffsets: false,

  // must be called before calling withinIncludingScrolloffset, every time the
  // page is scrolled
  prepare: function() {
    this.deltaX =  window.pageXOffset
                || document.documentElement.scrollLeft
                || document.body.scrollLeft
                || 0;
    this.deltaY =  window.pageYOffset
                || document.documentElement.scrollTop
                || document.body.scrollTop
                || 0;
  },

  realOffset: function(element) {
    var valueT = 0, valueL = 0;
    do {
      valueT += element.scrollTop  || 0;
      valueL += element.scrollLeft || 0;
      element = element.parentNode;
    } while (element);
    return [valueL, valueT];
  },

  cumulativeOffset: function(element) {
    var valueT = 0, valueL = 0;
    do {
      valueT += element.offsetTop  || 0;
      valueL += element.offsetLeft || 0;
      element = element.offsetParent;
    } while (element);
    return [valueL, valueT];
  },

  positionedOffset: function(element) {
    var valueT = 0, valueL = 0;
    do {
      valueT += element.offsetTop  || 0;
      valueL += element.offsetLeft || 0;
      element = element.offsetParent;
      if (element) {
        if(element.tagName=='BODY') break;
        var p = Element.getStyle(element, 'position');
        if (p == 'relative' || p == 'absolute') break;
      }
    } while (element);
    return [valueL, valueT];
  },

  offsetParent: function(element) {
    if (element.offsetParent) return element.offsetParent;
    if (element == document.body) return element;

    while ((element = element.parentNode) && element != document.body)
      if (Element.getStyle(element, 'position') != 'static')
        return element;

    return document.body;
  },

  // caches x/y coordinate pair to use with overlap
  within: function(element, x, y) {
    if (this.includeScrollOffsets)
      return this.withinIncludingScrolloffsets(element, x, y);
    this.xcomp = x;
    this.ycomp = y;
    this.offset = this.cumulativeOffset(element);

    return (y >= this.offset[1] &&
            y <  this.offset[1] + element.offsetHeight &&
            x >= this.offset[0] &&
            x <  this.offset[0] + element.offsetWidth);
  },

  withinIncludingScrolloffsets: function(element, x, y) {
    var offsetcache = this.realOffset(element);

    this.xcomp = x + offsetcache[0] - this.deltaX;
    this.ycomp = y + offsetcache[1] - this.deltaY;
    this.offset = this.cumulativeOffset(element);

    return (this.ycomp >= this.offset[1] &&
            this.ycomp <  this.offset[1] + element.offsetHeight &&
            this.xcomp >= this.offset[0] &&
            this.xcomp <  this.offset[0] + element.offsetWidth);
  },

  // within must be called directly before
  overlap: function(mode, element) {
    if (!mode) return 0;
    if (mode == 'vertical')
      return ((this.offset[1] + element.offsetHeight) - this.ycomp) /
        element.offsetHeight;
    if (mode == 'horizontal')
      return ((this.offset[0] + element.offsetWidth) - this.xcomp) /
        element.offsetWidth;
  },

  page: function(forElement) {
    var valueT = 0, valueL = 0;

    var element = forElement;
    do {
      valueT += element.offsetTop  || 0;
      valueL += element.offsetLeft || 0;

      // Safari fix
      if (element.offsetParent==document.body)
        if (Element.getStyle(element,'position')=='absolute') break;

    } while (element = element.offsetParent);

    element = forElement;
    do {
      if (!window.opera || element.tagName=='BODY') {
        valueT -= element.scrollTop  || 0;
        valueL -= element.scrollLeft || 0;
      }
    } while (element = element.parentNode);

    return [valueL, valueT];
  },

  clone: function(source, target) {
    var options = Object.extend({
      setLeft:    true,
      setTop:     true,
      setWidth:   true,
      setHeight:  true,
      offsetTop:  0,
      offsetLeft: 0
    }, arguments[2] || {})

    // find page position of source
    source = $(source);
    var p = Position.page(source);

    // find coordinate system to use
    target = $(target);
    var delta = [0, 0];
    var parent = null;
    // delta [0,0] will do fine with position: fixed elements,
    // position:absolute needs offsetParent deltas
    if (Element.getStyle(target,'position') == 'absolute') {
      parent = Position.offsetParent(target);
      delta = Position.page(parent);
    }

    // correct by body offsets (fixes Safari)
    if (parent == document.body) {
      delta[0] -= document.body.offsetLeft;
      delta[1] -= document.body.offsetTop;
    }

    // set position
    if(options.setLeft)   target.style.left  = (p[0] - delta[0] + options.offsetLeft) + 'px';
    if(options.setTop)    target.style.top   = (p[1] - delta[1] + options.offsetTop) + 'px';
    if(options.setWidth)  target.style.width = source.offsetWidth + 'px';
    if(options.setHeight) target.style.height = source.offsetHeight + 'px';
  },

  absolutize: function(element) {
    element = $(element);
    if (element.style.position == 'absolute') return;
    Position.prepare();

    var offsets = Position.positionedOffset(element);
    var top     = offsets[1];
    var left    = offsets[0];
    var width   = element.clientWidth;
    var height  = element.clientHeight;

    element._originalLeft   = left - parseFloat(element.style.left  || 0);
    element._originalTop    = top  - parseFloat(element.style.top || 0);
    element._originalWidth  = element.style.width;
    element._originalHeight = element.style.height;

    element.style.position = 'absolute';
    element.style.top    = top + 'px';
    element.style.left   = left + 'px';
    element.style.width  = width + 'px';
    element.style.height = height + 'px';
  },

  relativize: function(element) {
    element = $(element);
    if (element.style.position == 'relative') return;
    Position.prepare();

    element.style.position = 'relative';
    var top  = parseFloat(element.style.top  || 0) - (element._originalTop || 0);
    var left = parseFloat(element.style.left || 0) - (element._originalLeft || 0);

    element.style.top    = top + 'px';
    element.style.left   = left + 'px';
    element.style.height = element._originalHeight;
    element.style.width  = element._originalWidth;
  }
}

// Safari returns margins on body which is incorrect if the child is absolutely
// positioned.  For performance reasons, redefine Position.cumulativeOffset for
// KHTML/WebKit only.
if (/Konqueror|Safari|KHTML/.test(navigator.userAgent)) {
  Position.cumulativeOffset = function(element) {
    var valueT = 0, valueL = 0;
    do {
      valueT += element.offsetTop  || 0;
      valueL += element.offsetLeft || 0;
      if (element.offsetParent == document.body)
        if (Element.getStyle(element, 'position') == 'absolute') break;

      element = element.offsetParent;
    } while (element);

    return [valueL, valueT];
  }
}

Element.addMethods();

// script.aculo.us scriptaculous.js v1.7.0, Fri Jan 19 19:16:36 CET 2007

// Copyright (c) 2005, 2006 Thomas Fuchs (http://script.aculo.us, http://mir.aculo.us)
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
// For details, see the script.aculo.us web site: http://script.aculo.us/

var Scriptaculous = {
  Version: '1.7.0',
  require: function(libraryName) {
    // inserting via DOM fails in Safari 2.0, so brute force approach
    document.write('<script type="text/javascript" src="'+libraryName+'"></script>');
  },
  load: function() {
    if((typeof Prototype=='undefined') ||
       (typeof Element == 'undefined') ||
       (typeof Element.Methods=='undefined') ||
       parseFloat(Prototype.Version.split(".")[0] + "." +
                  Prototype.Version.split(".")[1]) < 1.5)
       throw("script.aculo.us requires the Prototype JavaScript framework >= 1.5.0");

    $A(document.getElementsByTagName("script")).findAll( function(s) {
      return (s.src && s.src.match(/scriptaculous\.js(\?.*)?$/))
    }).each( function(s) {
      var path = s.src.replace(/scriptaculous\.js(\?.*)?$/,'');
      var includes = s.src.match(/\?.*load=([a-z,]*)/);
      (includes ? includes[1] : 'builder,effects,dragdrop,controls,slider').split(',').each(
       function(include) { Scriptaculous.require(path+include+'.js') });
    });
  }
}

Scriptaculous.load();

/*
 Do not remove or change this notice.
 overlibmws.js core module - Copyright Foteos Macrides 2002-2007. All rights reserved.
   Initial: August 18, 2002 - Last Revised: May 1, 2007
 This module is subject to the same terms of usage as for Erik Bosrup's overLIB,
 though only a minority of the code and API now correspond with Erik's version.
 See the overlibmws Change History and Command Reference via:

	http://www.macridesweb.com/oltest/

 Published under an open source license: http://www.macridesweb.com/oltest/license.html
 Give credit on sites that use overlibmws and submit changes so others can use them as well.
 You can get Erik's version via: http://www.bosrup.com/web/overlib/
*/

// PRE-INIT -- Ignore these lines, configuration is below.
var OLloaded=0,pmCnt=1,pMtr=new Array(),OLcmdLine=new Array(),OLrunTime=new Array(),OLv,OLudf,
OLpct=new Array("83%","67%","83%","100%","117%","150%","200%","267%"),OLrefXY,OLbubblePI=0,
OLcrossframePI=0,OLdebugPI=0,OLdraggablePI=0,OLexclusivePI=0,OLfilterPI=0,OLfunctionPI=0,
OLhidePI=0,OLiframePI=0,OLmodalPI=0,OLovertwoPI=0,OLscrollPI=0,OLshadowPI=0,OLprintPI=0;
if(typeof OLgateOK=='undefined')var OLgateOK=1;
var OLp1or2c='inarray,caparray,caption,closetext,right,left,center,autostatuscap,padx,pady,'
+'below,above,vcenter,donothing',OLp1or2co='nofollow,background,offsetx,offsety,fgcolor,'
+'bgcolor,cgcolor,textcolor,capcolor,width,wrap,wrapmax,height,border,base,status,autostatus,'
+'snapx,snapy,fixx,fixy,relx,rely,midx,midy,ref,refc,refp,refx,refy,fgbackground,bgbackground,'
+'cgbackground,fullhtml,capicon,textfont,captionfont,textsize,captionsize,timeout,delay,hauto,'
+'vauto,nojustx,nojusty,fgclass,bgclass,cgclass,capbelow,textpadding,textfontclass,'
+'captionpadding,captionfontclass,sticky,noclose,mouseoff,offdelay,closecolor,closefont,'
+'closesize,closeclick,closetitle,closefontclass,decode',OLp1or2o='text,cap,close,hpos,vpos,'
+'padxl,padxr,padyt,padyb',OLp1co='label',OLp1or2=OLp1or2co+','+OLp1or2o,OLp1=OLp1co+','+'frame';
OLregCmds(OLp1or2c+','+OLp1or2co+','+OLp1co);
function OLud(v){return eval('typeof ol_'+v+'=="undefined"')?1:0;}

// DEFAULT CONFIGURATION -- See overlibConfig.txt for descriptions
if(OLud('fgcolor'))var ol_fgcolor="#ccccff";
if(OLud('bgcolor'))var ol_bgcolor="#333399";
if(OLud('cgcolor'))var ol_cgcolor="#333399";
if(OLud('textcolor'))var ol_textcolor="#000000";
if(OLud('capcolor'))var ol_capcolor="#ffffff";
if(OLud('closecolor'))var ol_closecolor="#eeeeff";
if(OLud('textfont'))var ol_textfont="Verdana,Arial,Helvetica";
if(OLud('captionfont'))var ol_captionfont="Verdana,Arial,Helvetica";
if(OLud('closefont'))var ol_closefont="Verdana,Arial,Helvetica";
if(OLud('textsize'))var ol_textsize=1;
if(OLud('captionsize'))var ol_captionsize=1;
if(OLud('closesize'))var ol_closesize=1;
if(OLud('fgclass'))var ol_fgclass="";
if(OLud('bgclass'))var ol_bgclass="";
if(OLud('cgclass'))var ol_cgclass="";
if(OLud('textpadding'))var ol_textpadding=2;
if(OLud('textfontclass'))var ol_textfontclass="";
if(OLud('captionpadding'))var ol_captionpadding=2;
if(OLud('captionfontclass'))var ol_captionfontclass="";
if(OLud('closefontclass'))var ol_closefontclass="";
if(OLud('close'))var ol_close="Close";
if(OLud('closeclick'))var ol_closeclick=0;
if(OLud('closetitle'))var ol_closetitle="Click to Close";
if(OLud('text'))var ol_text="Default Text";
if(OLud('cap'))var ol_cap="";
if(OLud('capbelow'))var ol_capbelow=0;
if(OLud('background'))var ol_background="";
if(OLud('width'))var ol_width=200;
if(OLud('wrap'))var ol_wrap=0;
if(OLud('wrapmax'))var ol_wrapmax=0;
if(OLud('height'))var ol_height= -1;
if(OLud('border'))var ol_border=1;
if(OLud('base'))var ol_base=0;
if(OLud('offsetx'))var ol_offsetx=10;
if(OLud('offsety'))var ol_offsety=10;
if(OLud('sticky'))var ol_sticky=0;
if(OLud('nofollow'))var ol_nofollow=0;
if(OLud('noclose'))var ol_noclose=0;
if(OLud('mouseoff'))var ol_mouseoff=0;
if(OLud('offdelay'))var ol_offdelay=300;
if(OLud('hpos'))var ol_hpos=RIGHT;
if(OLud('vpos'))var ol_vpos=BELOW;
if(OLud('status'))var ol_status="";
if(OLud('autostatus'))var ol_autostatus=0;
if(OLud('snapx'))var ol_snapx=0;
if(OLud('snapy'))var ol_snapy=0;
if(OLud('fixx'))var ol_fixx= -1;
if(OLud('fixy'))var ol_fixy= -1;
if(OLud('relx'))var ol_relx=null;
if(OLud('rely'))var ol_rely=null;
if(OLud('midx'))var ol_midx=null;
if(OLud('midy'))var ol_midy=null;
if(OLud('ref'))var ol_ref="";
if(OLud('refc'))var ol_refc='UL';
if(OLud('refp'))var ol_refp='UL';
if(OLud('refx'))var ol_refx=0;
if(OLud('refy'))var ol_refy=0;
if(OLud('fgbackground'))var ol_fgbackground="";
if(OLud('bgbackground'))var ol_bgbackground="";
if(OLud('cgbackground'))var ol_cgbackground="";
if(OLud('padxl'))var ol_padxl=1;
if(OLud('padxr'))var ol_padxr=1;
if(OLud('padyt'))var ol_padyt=1;
if(OLud('padyb'))var ol_padyb=1;
if(OLud('fullhtml'))var ol_fullhtml=0;
if(OLud('capicon'))var ol_capicon="";
if(OLud('frame'))var ol_frame=self;
if(OLud('timeout'))var ol_timeout=0;
if(OLud('delay'))var ol_delay=0;
if(OLud('hauto'))var ol_hauto=0;
if(OLud('vauto'))var ol_vauto=0;
if(OLud('nojustx'))var ol_nojustx=0;
if(OLud('nojusty'))var ol_nojusty=0;
if(OLud('label'))var ol_label="";
if(OLud('decode'))var ol_decode=0;
// ARRAY CONFIGURATION - See overlibConfig.txt for descriptions.
if(OLud('texts'))var ol_texts=new Array("Text 0","Text 1");
if(OLud('caps'))var ol_caps=new Array("Caption 0","Caption 1");
// END CONFIGURATION -- Don't change anything below, all configuration is above.

// INIT -- Runtime variables.
var o3_text="",o3_cap="",o3_sticky=0,o3_nofollow=0,o3_background="",o3_noclose=0,o3_mouseoff=0,
o3_offdelay=300,o3_hpos=RIGHT,o3_offsetx=10,o3_offsety=10,o3_fgcolor="",o3_bgcolor="",
o3_cgcolor="",o3_textcolor="",o3_capcolor="",o3_closecolor="",o3_width=200,o3_wrap=0,
o3_wrapmax=0,o3_height= -1,o3_border=1,o3_base=0,o3_status="",o3_autostatus=0,o3_snapx=0,
o3_snapy=0,o3_fixx= -1,o3_fixy= -1,o3_relx=null,o3_rely=null,o3_midx=null,o3_midy=null,o3_ref="",
o3_refc='UL',o3_refp='UL',o3_refx=0,o3_refy=0,o3_fgbackground="",o3_bgbackground="",
o3_cgbackground="",o3_padxl=0,o3_padxr=0,o3_padyt=0,o3_padyb=0,o3_fullhtml=0,o3_vpos=BELOW,
o3_capicon="",o3_textfont="Verdana,Arial,Helvetica",o3_captionfont="",o3_closefont="",
o3_textsize=1,o3_captionsize=1,o3_closesize=1,o3_frame=self,o3_timeout=0,o3_delay=0,o3_hauto=0,
o3_vauto=0,o3_nojustx=0,o3_nojusty=0,o3_close="",o3_closeclick=0,o3_closetitle="",o3_fgclass="",
o3_bgclass="",o3_cgclass="",o3_textpadding=2,o3_textfontclass="",o3_captionpadding=2,
o3_captionfontclass="",o3_closefontclass="",o3_capbelow=0,o3_label="",o3_decode=0,
CSSOFF=DONOTHING,CSSCLASS=DONOTHING,OLdelayid=0,OLtimerid=0,OLshowid=0,OLndt=0,over=null,
OLfnRef="",OLhover=0,OLx=0,OLy=0,OLshowingsticky=0,OLallowmove=0,OLcC=null,
OLua=navigator.userAgent.toLowerCase(),
OLns4=(navigator.appName=='Netscape'&&parseInt(navigator.appVersion)==4)?1:0,
OLns6=(document.getElementById)?1:0,
OLie4=(document.all)?1:0,
OLgek=(OLv=OLua.match(/gecko\/(\d{8})/i))?parseInt(OLv[1]):0,
OLmac=(OLua.indexOf('mac')>=0)?1:0,
OLsaf=(OLua.indexOf('safari')>=0)?1:0,
OLkon=(OLua.indexOf('konqueror')>=0)?1:0,
OLkht=(OLsaf||OLkon)?1:0,
OLopr=(OLua.indexOf('opera')>=0)?1:0,
OLop7=(OLopr&&document.createTextNode)?1:0;
if(OLopr){OLns4=OLns6=0;if(!OLop7)OLie4=0;}
var OLieM=((OLie4&&OLmac)&&!(OLkht||OLopr))?1:0,
OLie5=0,OLie55=0;OLie7=0;if(OLie4&&!OLop7){
if((OLv=OLua.match(/msie (\d\.\d+)\.*/i))&&(OLv=parseFloat(OLv[1]))>=5.0){
OLie5=1;OLns6=0;if(OLv>=5.5)OLie55=1;if(OLv>=7.0)OLie7=1;}if(OLns6)OLie4=0;}
if(OLns4)window.onresize=function(){location.reload();}
var OLchkMh=1,OLdw;
if(OLns4||OLie4||OLns6)OLmh();else{overlib=nd=cClick=OLpageDefaults=no_overlib;}

/*
 PUBLIC FUNCTIONS
*/
// Loads defaults then args into runtime variables.
function overlib(){
if(!(OLloaded&&OLgateOK))return;if((OLexclusivePI)&&OLisExclusive(arguments))return true;
if(OLchkMh)OLmh();if(OLndt&&!OLtimerid)OLndt=0;if(over)cClick();OLload(OLp1or2);OLload(OLp1);
OLfnRef="";OLhover=0;OLsetRunTimeVar();OLparseTokens('o3_',arguments);
if(!(over=OLmkLyr()))return false;if(o3_decode)OLdecode();if(OLprintPI)OLchkPrint();
if(OLbubblePI)OLchkForBubbleEffect();if(OLdebugPI)OLsetDebugCanShow();
if(OLshadowPI)OLinitShadow();if(OLiframePI)OLinitIfs();if(OLfilterPI)OLinitFilterLyr();
if(OLexclusivePI&&o3_exclusive&&o3_exclusivestatus!="")o3_status=o3_exclusivestatus;
else if(o3_autostatus==2&&o3_cap!="")o3_status=o3_cap;
else if(o3_autostatus==1&&o3_text!="")o3_status=o3_text;if(!o3_delay){return OLmain();
}else{OLdelayid=setTimeout("OLmain()",o3_delay);if(o3_status!=""){self.status=o3_status;
return true;}else if(!(OLop7&&event&&event.type=='mouseover'))return false;}
}

// Clears popups if appropriate
function nd(time){
if(OLloaded&&OLgateOK){if(!((OLexclusivePI)&&OLisExclusive())){if(time&&over&&!o3_delay){
if(OLtimerid>0)clearTimeout(OLtimerid);OLtimerid=(OLhover&&o3_frame==self&&!OLcursorOff())?0:
setTimeout("cClick()",(o3_timeout=OLndt=time));}else{if(!OLshowingsticky){OLallowmove=0;
if(over)OLhideObject(over);}}}}return false;
}

// Close function for stickies
function cClick(){
if(OLloaded&&OLgateOK){OLhover=0;if(over){if(OLovertwoPI&&over==over2)cClick2();
OLhideObject(over);OLshowingsticky=0;}if(OLmodalPI)OLclearModal();}return false;
}

// Sets page-specific defaults.
function OLpageDefaults(){
OLparseTokens('ol_',arguments);
}

// Gets object referenced by its id or name
function OLgetRef(l,d){var r=OLgetRefById(l,d);return (r)?r:OLgetRefByName(l,d);}

// For unsupported browsers.
function no_overlib(){return false;}

/*
 OVERLIB MAIN FUNCTION SET
*/
function OLmain(){
o3_delay=0;
if(o3_frame==self){if(o3_noclose)OLoptMOUSEOFF(0);else if(o3_mouseoff)OLoptMOUSEOFF(1);}
if(o3_sticky)OLshowingsticky=1;OLdoLyr();OLallowmove=0;if(o3_timeout>0){
if(OLtimerid>0)clearTimeout(OLtimerid);OLtimerid=setTimeout("cClick()",o3_timeout);}
if(o3_ref){OLrefXY=OLgetRefXY(o3_ref);if(OLrefXY[0]==null){o3_ref="";o3_midx=0;o3_midy=0;}}
OLdisp(o3_status);if(OLdraggablePI)OLcheckDrag();
if(o3_status!="")return true;else if(!(OLop7&&event&&event.type=='mouseover'))return false;
}

// Loads o3_ variables
function OLload(c){var i,m=c.split(',');for(i=0;i<m.length;i++)eval('o3_'+m[i]+'=ol_'+m[i]);}

// Chooses LGF
function OLdoLGF(){
return (o3_background!=''||o3_fullhtml)?OLcontentBackground(o3_text,o3_background,o3_fullhtml):
(o3_cap=="")?OLcontentSimple(o3_text):
(o3_sticky)?OLcontentCaption(o3_text,o3_cap,o3_close):OLcontentCaption(o3_text,o3_cap,'');
}

// Makes Layer
function OLmkLyr(id,f,z){
id=(id||'overDiv');f=(f||o3_frame);z=(z||1000);var fd=f.document,d=OLgetRefById(id,fd);
if(!d){if(OLns4)d=fd.layers[id]=new Layer(1024,f);else if(OLie4&&!document.getElementById){
fd.body.insertAdjacentHTML('BeforeEnd','<div id="'+id+'"></div>');d=fd.all[id];
}else{d=fd.createElement('div');if(d){d.id=id;fd.body.appendChild(d);}}if(!d)return null;
if(OLns4)d.zIndex=z;else{var o=d.style;o.position='absolute';o.visibility='hidden';o.zIndex=z;}}
return d;
}

// Creates and writes layer content
function OLdoLyr(){
if(o3_sticky&&OLtimerid>0){clearTimeout(OLtimerid);OLtimerid=0;}
if(o3_background==''&&!o3_fullhtml){
if(o3_fgbackground!='')o3_fgbackground=' background="'+o3_fgbackground+'"';
if(o3_bgbackground!='')o3_bgbackground=' background="'+o3_bgbackground+'"';
if(o3_cgbackground!='')o3_cgbackground=' background="'+o3_cgbackground+'"';
if(o3_fgcolor!='')o3_fgcolor=' bgcolor="'+o3_fgcolor+'"';
if(o3_bgcolor!='')o3_bgcolor=' bgcolor="'+o3_bgcolor+'"';
if(o3_cgcolor!='')o3_cgcolor=' bgcolor="'+o3_cgcolor+'"';
if(o3_height>0)o3_height=' height="'+o3_height+'"';else o3_height='';}
if(!OLns4)OLrepositionTo(over,(OLns6?20:0),0);var lyrHtml=OLdoLGF();
if(o3_wrap&&!o3_fullhtml){OLlayerWrite(lyrHtml);
o3_width=(OLns4?over.clip.width:over.offsetWidth);if(OLie4){var w=OLfd().clientWidth;
if(o3_width>=w){if(OLop7){if(OLovertwoPI&&over==over2){var z=over2.style.zIndex;
o3_frame.document.body.removeChild(over);over2=OLmkLyr('overDiv2',o3_frame,z);over=over2;
}else{o3_frame.document.body.removeChild(over);over=OLmkLyr();}}o3_width=w-20;}}
if(o3_wrapmax<1&&o3_frame.innerWidth)o3_wrapmax=o3_frame.innerWidth-40;
if(o3_wrapmax>0&&o3_width>o3_wrapmax)o3_width=o3_wrapmax;o3_wrap=0;lyrHtml=OLdoLGF();}
OLlayerWrite(lyrHtml);o3_width=(OLns4?over.clip.width:over.offsetWidth);
if(OLbubblePI)OLgenerateBubble(lyrHtml);
}

/*
 LAYER GENERATION FUNCTIONS
*/
// Makes simple table without caption
function OLcontentSimple(txt){
var t=OLbgLGF()+OLfgLGF(txt)+OLbaseLGF();OLsetBackground('');return t;
}

// Makes table with caption and optional close link
function OLcontentCaption(txt,title,close){
var closing=(OLprintPI?OLprintCapLGF():''),closeevent='onmouseover',caption,t,
cC='javascript:return '+OLfnRef+(OLovertwoPI&&over==over2?'cClick2();':'cClick();');
if(o3_closeclick)closeevent=(o3_closetitle?'title="'+o3_closetitle+'" ':'')+'onclick';
if(o3_capicon!='')o3_capicon='<img src="'+o3_capicon+'" /> ';
if(close){closing+='<td align="right"><a href="'+cC+'" '+closeevent+'="'+cC+'"'
+(o3_closefontclass?' class="'+o3_closefontclass+'">':(OLns4?'><':'')
+OLlgfUtil(0,1,'','a',o3_closecolor,o3_closefont,o3_closesize))+close+
(o3_closefontclass?'':(OLns4?OLlgfUtil(1,1,'','a'):''))+'</a></td>';}
caption='<table id="overCap'+(OLovertwoPI&&over==over2?'2':'')+'"'+OLwd(0)
+' border="0" cellpadding="'+o3_captionpadding+'" cellspacing="0"'+(o3_cgclass?' class="'
+o3_cgclass+'"':o3_cgcolor+o3_cgbackground)+'><tr><td'+OLwd(0)+(o3_cgclass?' class="'
+o3_cgclass+'">':'>')+(o3_captionfontclass?'<div class="'+o3_captionfontclass
+'">':OLlgfUtil(0,1,'','div',o3_capcolor,o3_captionfont,o3_captionsize))+o3_capicon+title
+OLlgfUtil(1,1,'','div')+'</td>'+closing+'</tr></table>';
t=OLbgLGF()+(o3_capbelow?OLfgLGF(txt)+caption:caption+OLfgLGF(txt))+OLbaseLGF();
OLsetBackground('');return t;
}

// For BACKGROUND and FULLHTML commands
function OLcontentBackground(txt,image,hasfullhtml){
var t;if(hasfullhtml){t=txt;}else{t='<table'+OLwd(1)+' border="0" cellpadding="0" '
+'cellspacing="0" '+'height="'+o3_height+'"><tr><td colspan="3" height="'+o3_padyt
+'"></td></tr><tr><td width="'+o3_padxl+'"></td><td valign="top"'+OLwd(2)+'>'
+OLlgfUtil(0,0,o3_textfontclass,'div',o3_textcolor,o3_textfont,o3_textsize)+txt+
OLlgfUtil(1,0,'','div')+'</td><td width="'+o3_padxr+'"></td></tr><tr><td colspan="3" height="'
+o3_padyb+'"></td></tr></table>';}OLsetBackground(image);return t;
}

// LGF utilities
function OLbgLGF(){
return '<table'+OLwd(1)+o3_height+' border="0" cellpadding="'+o3_border+'" cellspacing="0"'
+(o3_bgclass?' class="'+o3_bgclass+'"':o3_bgcolor+o3_bgbackground)+'><tr><td>';
}
function OLfgLGF(t){
return '<table'+OLwd(0)+o3_height+' border="0" cellpadding="'+o3_textpadding
+'" cellspacing="0"'+(o3_fgclass?' class="'+o3_fgclass+'"':o3_fgcolor+o3_fgbackground)
+'><tr><td valign="top"'+(o3_fgclass?' class="'+o3_fgclass+'"':'')+'>'
+OLlgfUtil(0,0,o3_textfontclass,'div',o3_textcolor,o3_textfont,o3_textsize)+t
+(OLprintPI?OLprintFgLGF():'')+OLlgfUtil(1,0,'','div')+'</td></tr></table>';
}
function OLlgfUtil(end,stg,tfc,ele,col,fac,siz){
if(end)return('</'+(OLns4?'font'+(stg?'></strong':''):ele)+'>');
else return(tfc?'<div class="'+tfc+'">':((ele=='a'?'':'<')+(OLns4?(stg?'strong><':'')
+'font color="'+col+'" face="'+OLquoteMultiNameFonts(fac)+'" size="'+siz:(ele=='a'?'':ele)
+' style="color:'+col+(stg?';font-weight:bold':'')+';font-family:'+OLquoteMultiNameFonts(fac)
+';font-size:'+siz+';'+(ele=='span'?'text-decoration:underline;':''))+'">'));
}
function OLquoteMultiNameFonts(f){
var i,v,pM=f.split(',');
for(i=0;i<pM.length;i++){v=pM[i];v=v.replace(/^\s+/,'').replace(/\s+$/,'');
if(/\s/.test(v) && !/['"]/.test(v)){v="\'"+v+"\'";pM[i]=v;}}return pM.join();
}
function OLbaseLGF(){
return ((o3_base>0&&!o3_wrap)?('<table width="100%" border="0" cellpadding="0" cellspacing="0"'
+(o3_bgclass?' class="'+o3_bgclass+'"':'')+'><tr><td height="'+o3_base
+'"></td></tr></table>'):'')+'</td></tr></table>';
}
function OLwd(a){
return(o3_wrap?'':' width="'+(!a?'100%':(a==1?o3_width:(o3_width-o3_padxl-o3_padxr)))+'"');
}

// Loads image into the div.
function OLsetBackground(i){
if(i==''){if(OLns4)over.background.src=null;else{if(OLns6)over.style.width='';
over.style.backgroundImage='none';}}else{if(OLns4)over.background.src=i;else{
if(OLns6)over.style.width=o3_width+'px';over.style.backgroundImage='url('+i+')';}}
}

/*
 HANDLING FUNCTIONS
*/
// Displays layer
function OLdisp(s){
if(OLmodalPI)OLchkModal();if(!OLallowmove){if(OLshadowPI)OLdispShadow();
if(OLiframePI)OLdispIfs();OLplaceLayer();if(OLndt)OLshowObject(over);
else OLshowid=setTimeout("OLshowObject(over)",1);
OLallowmove=(o3_sticky||o3_nofollow)?0:1;}OLndt=0;if(s!="")self.status=s;
}

// Decides placement of layer.
function OLplaceLayer(){
var snp,X,Y,pgLeft,pgTop,pWd=o3_width,pHt,iWd=100,iHt=100,SB=0,LM=0,CX=0,TM=0,BM=0,CY=0,
o=OLfd(),nsb=(OLgek>=20010505&&!o3_frame.scrollbars.visible)?1:0;
if(!OLkht&&o&&o.clientWidth)iWd=o.clientWidth;
else if(o3_frame.innerWidth){SB=Math.ceil(1.4*(o3_frame.outerWidth-o3_frame.innerWidth));
if(SB>20)SB=20;iWd=o3_frame.innerWidth;}
pgLeft=(OLie4)?o.scrollLeft:o3_frame.pageXOffset;
if(OLie55&&OLfilterPI&&o3_filter&&o3_filtershadow)SB=CX=5;else
if((OLshadowPI)&&bkdrop&&o3_shadow&&o3_shadowx){SB+=((o3_shadowx>0)?o3_shadowx:0);
LM=((o3_shadowx<0)?Math.abs(o3_shadowx):0);CX=Math.abs(o3_shadowx);}
if(o3_ref!=""||o3_fixx> -1||o3_relx!=null||o3_midx!=null){
if(o3_ref!=""){X=OLrefXY[0];if(OLie55&&OLfilterPI&&o3_filter&&o3_filtershadow){
if(o3_refp=='UR'||o3_refp=='LR')X-=5;}
else if((OLshadowPI)&&bkdrop&&o3_shadow&&o3_shadowx){
if(o3_shadowx<0&&(o3_refp=='UL'||o3_refp=='LL'))X-=o3_shadowx;else
if(o3_shadowx>0&&(o3_refp=='UR'||o3_refp=='LR'))X-=o3_shadowx;}
}else{if(o3_midx!=null){
X=parseInt(pgLeft+((iWd-pWd-SB-LM)/2)+o3_midx);
}else{if(o3_relx!=null){
if(o3_relx>=0)X=pgLeft+o3_relx+LM;else X=pgLeft+o3_relx+iWd-pWd-SB;
}else{X=o3_fixx+LM;}}}
}else{
if(o3_hauto){
if(o3_hpos==LEFT&&OLx-pgLeft<iWd/2&&OLx-pWd-o3_offsetx<pgLeft+LM)o3_hpos=RIGHT;else
if(o3_hpos==RIGHT&&OLx-pgLeft>iWd/2&&OLx+pWd+o3_offsetx>pgLeft+iWd-SB)o3_hpos=LEFT;}
X=(o3_hpos==CENTER)?parseInt(OLx-((pWd+CX)/2)+o3_offsetx):
(o3_hpos==LEFT)?OLx-o3_offsetx-pWd:OLx+o3_offsetx;
if(o3_snapx>1){
snp=X % o3_snapx;
if(o3_hpos==LEFT){X=X-(o3_snapx+snp);}else{X=X+(o3_snapx-snp);}}}
if(!o3_nojustx&&X+pWd>pgLeft+iWd-SB)
X=iWd+pgLeft-pWd-SB;if(!o3_nojustx&&X-LM<pgLeft)X=pgLeft+LM;
pgTop=OLie4?o.scrollTop:o3_frame.pageYOffset;
if(!OLkht&&!nsb&&o&&o.clientHeight)iHt=o.clientHeight;
else if(o3_frame.innerHeight)iHt=o3_frame.innerHeight;
if(OLbubblePI&&o3_bubble)pHt=OLbubbleHt;else pHt=OLns4?over.clip.height:over.offsetHeight;
if((OLshadowPI)&&bkdrop&&o3_shadow&&o3_shadowy){TM=(o3_shadowy<0)?Math.abs(o3_shadowy):0;
if(OLie55&&OLfilterPI&&o3_filter&&o3_filtershadow)BM=CY=5;else
BM=(o3_shadowy>0)?o3_shadowy:0;CY=Math.abs(o3_shadowy);}
if(o3_ref!=""||o3_fixy> -1||o3_rely!=null||o3_midy!=null){
if(o3_ref!=""){Y=OLrefXY[1];if(OLie55&&OLfilterPI&&o3_filter&&o3_filtershadow){
if(o3_refp=='LL'||o3_refp=='LR')Y-=5;}else if((OLshadowPI)&&bkdrop&&o3_shadow&&o3_shadowy){
if(o3_shadowy<0&&(o3_refp=='UL'||o3_refp=='UR'))Y-=o3_shadowy;else
if(o3_shadowy>0&&(o3_refp=='LL'||o3_refp=='LR'))Y-=o3_shadowy;}
}else{if(o3_midy!=null){
Y=parseInt(pgTop+((iHt-pHt-CY)/2)+o3_midy);
}else{if(o3_rely!=null){
if(o3_rely>=0)Y=pgTop+o3_rely+TM;else Y=pgTop+o3_rely+iHt-pHt-BM;}else{
Y=o3_fixy+TM;}}}
}else{
if(o3_vauto){
if(o3_vpos==ABOVE&&OLy-pgTop<iHt/2&&OLy-pHt-o3_offsety<pgTop)o3_vpos=BELOW;else
if(o3_vpos==BELOW&&OLy-pgTop>iHt/2&&OLy+pHt+o3_offsety+((OLns4||OLkht)?17:0)>pgTop+iHt-BM)
o3_vpos=ABOVE;}Y=(o3_vpos==VCENTER)?parseInt(OLy-((pHt+CY)/2)+o3_offsety):
(o3_vpos==ABOVE)?OLy-(pHt+o3_offsety+BM):OLy+o3_offsety+TM;
if(o3_snapy>1){
snp=Y % o3_snapy;
if(pHt>0&&o3_vpos==ABOVE){Y=Y-(o3_snapy+snp);}else{Y=Y+(o3_snapy-snp);}}}
if(!o3_nojusty&&Y+pHt+BM>pgTop+iHt)Y=pgTop+iHt-pHt-BM;if(!o3_nojusty&&Y-TM<pgTop)Y=pgTop+TM;
OLrepositionTo(over,X,Y);
if(OLshadowPI)OLrepositionShadow(X,Y);if(OLiframePI)OLrepositionIfs(X,Y);
if(OLns6&&o3_frame.innerHeight){iHt=o3_frame.innerHeight;OLrepositionTo(over,X,Y);}
if(OLscrollPI)OLchkScroll(X-pgLeft,Y-pgTop);
}

// Chooses body or documentElement
function OLfd(f){
var fd=((f)?f:o3_frame).document,fdc=fd.compatMode,fdd=fd.documentElement;
return (!OLop7&&fdc&&fdc!='BackCompat'&&fdd&&fdd.clientWidth)?fd.documentElement:fd.body;
}

// Gets location of REFerence object
function OLgetRefXY(r,d){
var o=OLgetRef(r,d),ob=o,rXY=[o3_refx,o3_refy],of;if(!o)return [null,null];
if(OLns4){if(typeof o.length!='undefined'&&o.length>1){ob=o[0];
rXY[0]+=o[0].x+o[1].pageX;rXY[1]+=o[0].y+o[1].pageY;}else{
if((o.toString().indexOf('Image')!= -1)||(o.toString().indexOf('Anchor')!= -1)){
rXY[0]+=o.x;rXY[1]+=o.y;}else{rXY[0]+=o.pageX;rXY[1]+=o.pageY;}}
}else{rXY[0]+=OLpageLoc(o,'Left');rXY[1]+=OLpageLoc(o,'Top');}
of=OLgetRefOffsets(ob);rXY[0]+=of[0];rXY[1]+=of[1];return rXY;
}

// Seeks REFerence by id
function OLgetRefById(l,d){
l=(l||'overDiv');d=(d||o3_frame.document);var j,r;if(OLie4&&d.all)return d.all[l];
if(d.getElementById)return d.getElementById(l);if(d.layers&&d.layers.length>0){
if(d.layers[l])return d.layers[l];for(j=0;j<d.layers.length;j++){
r=OLgetRefById(l,d.layers[j].document);if(r)return r;}}return null;
}

// Seeks REFerence by name
function OLgetRefByName(l,d){
d=(d||o3_frame.document);var j,r,v=OLie4?d.all.tags('iframe'):
OLns6?d.getElementsByTagName('iframe'):null;
if(typeof d.images!='undefined'&&d.images[l])return d.images[l];
if(typeof d.anchors!='undefined'&&d.anchors[l])return d.anchors[l];
if(v)for(j=0;j<v.length;j++)if(v[j].name==l)return v[j];
if(d.layers&&d.layers.length>0)for(j=0;j<d.layers.length;j++){
r=OLgetRefByName(l,d.layers[j].document);
if(r&&r.length>0)return r;else if(r)return [r,d.layers[j]];}return null;
}

// Gets layer vs REFerence offsets
function OLgetRefOffsets(o){
var c=o3_refc.toUpperCase(),p=o3_refp.toUpperCase(),W=0,H=0,pW=0,pH=0,of=[0,0];
pW=(OLbubblePI&&o3_bubble)?o3_width:OLns4?over.clip.width:over.offsetWidth;
pH=(OLbubblePI&&o3_bubble)?OLbubbleHt:OLns4?over.clip.height:over.offsetHeight;
if((!OLop7)&&o.toString().indexOf('Image')!= -1){W=o.width;H=o.height;
}else if((!OLop7)&&o.toString().indexOf('Anchor')!= -1){c=o3_refc='UL';}else{
W=(OLns4)?o.clip.width:o.offsetWidth;H=(OLns4)?o.clip.height:o.offsetHeight;}
if((OLns4||(OLns6&&OLgek))&&o.border){W+=2*parseInt(o.border);H+=2*parseInt(o.border);}
if(c=='UL'){of=(p=='UR')?[-pW,0]:(p=='LL')?[0,-pH]:(p=='LR')?[-pW,-pH]:[0,0];
}else if(c=='UR'){of=(p=='UR')?[W-pW,0]:(p=='LL')?[W,-pH]:(p=='LR')?[W-pW,-pH]:[W,0];
}else if(c=='LL'){of=(p=='UR')?[-pW,H]:(p=='LL')?[0,H-pH]:(p=='LR')?[-pW,H-pH]:[0,H];
}else if(c=='LR'){of=(p=='UR')?[W-pW,H]:(p=='LL')?[W,H-pH]:(p=='LR')?[W-pW,H-pH]:[W,H];}
return of;
}

// Gets x or y location of object
function OLpageLoc(o,t){
var l=0,s=o;while(o.offsetParent&&o.offsetParent.tagName.toLowerCase()!='html'){
l+=o['offset'+t];o=o.offsetParent;}l+=o['offset'+t];while(s=s.parentNode){
if((s['scroll'+t]>0)&&s.tagName.toLowerCase()=='div')l-=s['scroll'+t];}return l;
}

// Moves layer
function OLmouseMove(e){
var e=(e||event);OLcC=(OLovertwoPI&&over2&&over==over2?cClick2:cClick);
OLx=(e.pageX||e.clientX+OLfd().scrollLeft);OLy=(e.pageY||e.clientY+OLfd().scrollTop);
if((OLallowmove&&over)&&(o3_frame==self||over==OLgetRefById()
||(OLovertwoPI&&over2==over&&over==OLgetRefById('overDiv2')))){
OLplaceLayer();if(OLhidePI)OLhideUtil(0,1,1,0,0,0);}
if(OLhover&&over&&o3_frame==self&&OLcursorOff())if(o3_offdelay<1)OLcC();else
{if(OLtimerid>0)clearTimeout(OLtimerid);OLtimerid=setTimeout("OLcC()",o3_offdelay);}
}

// Capture mouse and chain other scripts.
function OLmh(){
var fN,f,j,k,s,mh=OLmouseMove,w=(OLns4&&window.onmousemove),re=/function[ ]*(\w*)\(/;
OLdw=document;if(document.onmousemove||w){if(w)OLdw=window;f=OLdw.onmousemove.toString();
fN=f.match(re);if(!fN||fN[1]=='anonymous'||fN[1]=='OLmouseMove'){OLchkMh=0;return;}
if(fN[1])s=fN[1]+'(e)';else{j=f.indexOf('{');k=f.lastIndexOf('}')+1;s=f.substring(j,k);}
s+=';OLmouseMove(e);';mh=new Function('e',s);}
OLdw.onmousemove=mh;if(OLns4)OLdw.captureEvents(Event.MOUSEMOVE);
}

/*
 PARSING
*/
function OLparseTokens(pf,ar){
var i,v,md= -1,par=(pf!='ol_'),p=OLpar,q=OLparQuo,t=OLtoggle;OLudf=(par&&!ar.length?1:0);
for(i=0;i<ar.length;i++){if(md<0){if(typeof ar[i]=='number'){OLudf=(par?1:0);i--;}
else{switch(pf){case 'ol_':ol_text=ar[i];break;default:o3_text=ar[i];}}md=0;}else{
if(ar[i]==INARRAY){OLudf=0;eval(pf+'text=ol_texts['+ar[++i]+']');continue;}
if(ar[i]==CAPARRAY){eval(pf+'cap=ol_caps['+ar[++i]+']');continue;}
if(ar[i]==CAPTION){q(ar[++i],pf+'cap');continue;}
if(Math.abs(ar[i])==STICKY){t(ar[i],pf+'sticky');continue;}
if(Math.abs(ar[i])==NOFOLLOW){t(ar[i],pf+'nofollow');continue;}
if(ar[i]==BACKGROUND){q(ar[++i],pf+'background');continue;}
if(Math.abs(ar[i])==NOCLOSE){t(ar[i],pf+'noclose');continue;}
if(Math.abs(ar[i])==MOUSEOFF){t(ar[i],pf+'mouseoff');continue;}
if(ar[i]==OFFDELAY){p(ar[++i],pf+'offdelay');continue;}
if(ar[i]==RIGHT||ar[i]==LEFT||ar[i]==CENTER){p(ar[i],pf+'hpos');continue;}
if(ar[i]==OFFSETX){p(ar[++i],pf+'offsetx');continue;}
if(ar[i]==OFFSETY){p(ar[++i],pf+'offsety');continue;}
if(ar[i]==FGCOLOR){q(ar[++i],pf+'fgcolor');continue;}
if(ar[i]==BGCOLOR){q(ar[++i],pf+'bgcolor');continue;}
if(ar[i]==CGCOLOR){q(ar[++i],pf+'cgcolor');continue;}
if(ar[i]==TEXTCOLOR){q(ar[++i],pf+'textcolor');continue;}
if(ar[i]==CAPCOLOR){q(ar[++i],pf+'capcolor');continue;}
if(ar[i]==CLOSECOLOR){q(ar[++i],pf+'closecolor');continue;}
if(ar[i]==WIDTH){p(ar[++i],pf+'width');continue;}
if(Math.abs(ar[i])==WRAP){t(ar[i],pf+'wrap');continue;}
if(ar[i]==WRAPMAX){p(ar[++i],pf+'wrapmax');continue;}
if(ar[i]==HEIGHT){p(ar[++i],pf+'height');continue;}
if(ar[i]==BORDER){p(ar[++i],pf+'border');continue;}
if(ar[i]==BASE){p(ar[++i],pf+'base');continue;}
if(ar[i]==STATUS){q(ar[++i],pf+'status');continue;}
if(Math.abs(ar[i])==AUTOSTATUS){v=pf+'autostatus';
eval(v+'=('+ar[i]+'<0)?('+v+'==2?2:0):('+v+'==1?0:1)');continue;}
if(Math.abs(ar[i])==AUTOSTATUSCAP){v=pf+'autostatus';
eval(v+'=('+ar[i]+'<0)?('+v+'==1?1:0):('+v+'==2?0:2)');continue;}
if(ar[i]==CLOSETEXT){q(ar[++i],pf+'close');continue;}
if(ar[i]==SNAPX){p(ar[++i],pf+'snapx');continue;}
if(ar[i]==SNAPY){p(ar[++i],pf+'snapy');continue;}
if(ar[i]==FIXX){p(ar[++i],pf+'fixx');continue;}
if(ar[i]==FIXY){p(ar[++i],pf+'fixy');continue;}
if(ar[i]==RELX){p(ar[++i],pf+'relx');continue;}
if(ar[i]==RELY){p(ar[++i],pf+'rely');continue;}
if(ar[i]==MIDX){p(ar[++i],pf+'midx');continue;}
if(ar[i]==MIDY){p(ar[++i],pf+'midy');continue;}
if(ar[i]==REF){q(ar[++i],pf+'ref');continue;}
if(ar[i]==REFC){q(ar[++i],pf+'refc');continue;}
if(ar[i]==REFP){q(ar[++i],pf+'refp');continue;}
if(ar[i]==REFX){p(ar[++i],pf+'refx');continue;}
if(ar[i]==REFY){p(ar[++i],pf+'refy');continue;}
if(ar[i]==FGBACKGROUND){q(ar[++i],pf+'fgbackground');continue;}
if(ar[i]==BGBACKGROUND){q(ar[++i],pf+'bgbackground');continue;}
if(ar[i]==CGBACKGROUND){q(ar[++i],pf+'cgbackground');continue;}
if(ar[i]==PADX){p(ar[++i],pf+'padxl');p(ar[++i],pf+'padxr');continue;}
if(ar[i]==PADY){p(ar[++i],pf+'padyt');p(ar[++i],pf+'padyb');continue;}
if(Math.abs(ar[i])==FULLHTML){t(ar[i],pf+'fullhtml');continue;}
if(ar[i]==BELOW||ar[i]==ABOVE||ar[i]==VCENTER){p(ar[i],pf+'vpos');continue;}
if(ar[i]==CAPICON){q(ar[++i],pf+'capicon');continue;}
if(ar[i]==TEXTFONT){q(ar[++i],pf+'textfont');continue;}
if(ar[i]==CAPTIONFONT){q(ar[++i],pf+'captionfont');continue;}
if(ar[i]==CLOSEFONT){q(ar[++i],pf+'closefont');continue;}
if(ar[i]==TEXTSIZE){q(ar[++i],pf+'textsize');continue;}
if(ar[i]==CAPTIONSIZE){q(ar[++i],pf+'captionsize');continue;}
if(ar[i]==CLOSESIZE){q(ar[++i],pf+'closesize');continue;}
if(ar[i]==TIMEOUT){p(ar[++i],pf+'timeout');continue;}
if(ar[i]==DELAY){p(ar[++i],pf+'delay');continue;}
if(Math.abs(ar[i])==HAUTO){t(ar[i],pf+'hauto');continue;}
if(Math.abs(ar[i])==VAUTO){t(ar[i],pf+'vauto');continue;}
if(Math.abs(ar[i])==NOJUSTX){t(ar[i],pf+'nojustx');continue;}
if(Math.abs(ar[i])==NOJUSTY){t(ar[i],pf+'nojusty');continue;}
if(Math.abs(ar[i])==CLOSECLICK){t(ar[i],pf+'closeclick');continue;}
if(ar[i]==CLOSETITLE){q(ar[++i],pf+'closetitle');continue;}
if(ar[i]==FGCLASS){q(ar[++i],pf+'fgclass');continue;}
if(ar[i]==BGCLASS){q(ar[++i],pf+'bgclass');continue;}
if(ar[i]==CGCLASS){q(ar[++i],pf+'cgclass');continue;}
if(ar[i]==TEXTPADDING){p(ar[++i],pf+'textpadding');continue;}
if(ar[i]==TEXTFONTCLASS){q(ar[++i],pf+'textfontclass');continue;}
if(ar[i]==CAPTIONPADDING){p(ar[++i],pf+'captionpadding');continue;}
if(ar[i]==CAPTIONFONTCLASS){q(ar[++i],pf+'captionfontclass');continue;}
if(ar[i]==CLOSEFONTCLASS){q(ar[++i],pf+'closefontclass');continue;}
if(Math.abs(ar[i])==CAPBELOW){t(ar[i],pf+'capbelow');continue;}
if(ar[i]==LABEL){q(ar[++i],pf+'label');continue;}
if(Math.abs(ar[i])==DECODE){t(ar[i],pf+'decode');continue;}
if(ar[i]==DONOTHING){continue;}
i=OLparseCmdLine(pf,i,ar);}}
if((OLfunctionPI)&&OLudf&&o3_function)o3_text=o3_function();
if(pf=='o3_')OLfontSize();
}
function OLpar(a,v){eval(v+'='+a);}
function OLparQuo(a,v){eval(v+"='"+OLescSglQt(a)+"'");}
function OLescSglQt(s){return s.toString().replace(/\\/g,"\\\\").replace(/'/g,"\\'");}
function OLtoggle(a,v){eval(v+'=('+v+'==0&&'+a+'>=0)?1:0');}
function OLhasDims(s){return /[%\-a-z]+$/.test(s);}
function OLfontSize(){
var i;if(OLhasDims(o3_textsize)){if(OLns4)o3_textsize="2";}else
if(!OLns4){i=parseInt(o3_textsize);o3_textsize=(i>0&&i<8)?OLpct[i]:OLpct[0];}
if(OLhasDims(o3_captionsize)){if(OLns4)o3_captionsize="2";}else
if(!OLns4){i=parseInt(o3_captionsize);o3_captionsize=(i>0&&i<8)?OLpct[i]:OLpct[0];}
if(OLhasDims(o3_closesize)){if(OLns4)o3_closesize="2";}else
if(!OLns4){i=parseInt(o3_closesize);o3_closesize=(i>0&&i<8)?OLpct[i]:OLpct[0];}
if(OLprintPI)OLprintDims();
}
function OLdecode(){
var re=/%[0-9A-Fa-f]{2,}/,t=o3_text,c=o3_cap,u=unescape,d=!OLns4&&(!OLgek||OLgek>=20020826)
&&typeof decodeURIComponent?decodeURIComponent:u;if(typeof(window.TypeError)=='function'){
if(re.test(t)){eval(new Array('try{','o3_text=d(t);','}catch(e){','o3_text=u(t);',
'}').join('\n'))};if(c&&re.test(c)){eval(new Array('try{','o3_cap=d(c);','}catch(e){',
'o3_cap=u(c);','}').join('\n'))}}else{if(re.test(t))o3_text=u(t);if(c&&re.test(c))o3_cap=u(c);}
}

/*
 LAYER FUNCTIONS
*/
// Writes to layer
function OLlayerWrite(t){
t+="\n";if(OLns4){over.document.write(t);over.document.close();}
else if(typeof over.innerHTML!='undefined'){if(OLieM)over.innerHTML='';over.innerHTML=t;
}else{var range=o3_frame.document.createRange();range.setStartAfter(over);
var domfrag=range.createContextualFragment(t);while(over.hasChildNodes()){
over.removeChild(over.lastChild);}over.appendChild(domfrag);}
if(OLprintPI)over.print=o3_print?t:null;
}

// Makes object visible
function OLshowObject(o){
OLshowid=0;o=(OLns4)?o:o.style;
if(((OLfilterPI)&&!OLchkFilter(o))||!OLfilterPI)o.visibility="visible";
if(OLshadowPI)OLshowShadow();if(OLiframePI)OLshowIfs();if(OLhidePI)OLhideUtil(1,1,0);
}

// Hides object
function OLhideObject(o){
if(OLshowid>0){clearTimeout(OLshowid);OLshowid=0;}
if(OLtimerid>0)clearTimeout(OLtimerid);if(OLdelayid>0)clearTimeout(OLdelayid);
OLtimerid=0;OLdelayid=0;self.status="";o3_label=ol_label;if(o3_frame!=self)o=OLgetRefById();
if(o){if(o.onmouseover)o.onmouseover=null;if(OLscrollPI&&o==over)OLclearScroll();
if(OLdraggablePI)OLclearDrag();if(OLfilterPI)OLcleanupFilter(o);if(OLshadowPI)OLhideShadow();
var os=(OLns4)?o:o.style;if(((OLfilterPI)&&!OLchkFadeOut(os))||!OLfilterPI){
os.visibility="hidden";if(!OLie55||!OLfilterPI||!o3_filter||o3_fadeout<0)o.innerHTML='';}
if(OLhidePI&&o==over)OLhideUtil(0,0,1);if(OLiframePI)OLhideIfs(o);}
}

// Moves layer
function OLrepositionTo(o,xL,yL){
o=(OLns4)?o:o.style;o.left=(OLns4?xL:xL+'px');o.top=(OLns4?yL:yL+'px');
}

// Handle NOCLOSE-MOUSEOFF
function OLoptMOUSEOFF(c){
if(!c)o3_close="";
over.onmouseover=function(){OLhover=1;if(OLtimerid>0){clearTimeout(OLtimerid);OLtimerid=0;}}
}
function OLcursorOff(){
var o=(OLns4?over:over.style),pHt=OLns4?over.clip.height:over.offsetHeight,
left=parseInt(o.left),top=parseInt(o.top),
right=left+o3_width,bottom=top+((OLbubblePI&&o3_bubble)?OLbubbleHt:pHt);
if(OLx<left||OLx>right||OLy<top||OLy>bottom)return true;return false;
}

/*
 REGISTRATION
*/
function OLsetRunTimeVar(){
if(OLrunTime.length)for(var k=0;k<OLrunTime.length;k++)OLrunTime[k]();
}
function OLparseCmdLine(pf,i,ar){
if(OLcmdLine.length){for(var k=0;k<OLcmdLine.length;k++){
var j=OLcmdLine[k](pf,i,ar);if(j>-1){i=j;break;}}}return i;
}
function OLregCmds(c){
if(typeof c!='string')return;var pM=c.split(',');pMtr=pMtr.concat(pM);
for(var i=0;i<pM.length;i++)eval(pM[i].toUpperCase()+'='+pmCnt++);
}
function OLregRunTimeFunc(f){
if(typeof f=='object')OLrunTime=OLrunTime.concat(f);else OLrunTime[OLrunTime.length++]=f;
}
function OLregCmdLineFunc(f){
if(typeof f=='object')OLcmdLine=OLcmdLine.concat(f);else OLcmdLine[OLcmdLine.length++]=f;
}

OLloaded=1;


/*
 overlibmws_crossframe.js plug-in module - Copyright Foteos Macrides 2003-2007. All rights reserved.
   For support of FRAME.
   Initial: August 3, 2003 - Last Revised: January 1, 2007
 See the Change History and Command Reference for overlibmws via:

	http://www.macridesweb.com/oltest/

 Published under an open source license: http://www.macridesweb.com/oltest/license.html
*/

OLloaded=0;
OLregCmds('frame');

function OLparseCrossframe(pf,i,ar){
var k=i,v;
if(k<ar.length){
if(ar[k]==FRAME){v=ar[++k];if(pf=='ol_')ol_frame=v;else OLoptFRAME(v);return k;}}
return -1;
}

function OLgetFrameRef(thisFrame,ofrm){
var i,v,retVal='';for(i=0;i<thisFrame.length;i++){if((((thisFrame[i].length>0)))&&(((OLns4))||
((OLie4)&&(v=thisFrame[i].document.all.tags('iframe'))!=null&&v.length==0)||
((OLns6)&&(v=thisFrame[i].document.getElementsByTagName('iframe'))!=null&&v.length==0))){
retVal=OLgetFrameRef(thisFrame[i],ofrm);if(retVal=='')continue;}
else if(thisFrame[i]!=ofrm)continue;retVal='['+i+']'+retVal;break;}
return retVal;
}

function OLoptFRAME(frm){
o3_frame=OLmkLyr('overDiv',frm)?frm:self;if(o3_frame!=self){
var l,tFrm=OLgetFrameRef(top.frames,o3_frame),sFrm=OLgetFrameRef(top.frames,ol_frame);
if(sFrm.length==tFrm.length) {l=tFrm.lastIndexOf('[');if(l){
while(sFrm.substring(0,l)!=tFrm.substring(0,l))l=tFrm.lastIndexOf('[',l-1);
tFrm=tFrm.substr(l);sFrm=sFrm.substr(l);}}var i,k,cnt=0,p='',str=tFrm;
while((k=str.lastIndexOf('['))!= -1){cnt++;str=str.substring(0,k);}
for(i=0;i<cnt;i++)p=p+'parent.';OLfnRef=p+'frames'+sFrm+'.';}
}

OLregCmdLineFunc(OLparseCrossframe);

OLcrossframePI=1;
OLloaded=1;

/*
 overlibmws_iframe.js plug-in module - Copyright Foteos Macrides 2003-2007. All rights reserved.
   Masks system controls to prevent obscuring of popops for IE v5.5 or higher.
   Initial: October 19, 2003 - Last Revised: April 22, 2007
 See the Change History and Command Reference for overlibmws via:

	http://www.macridesweb.com/oltest/

 Published under an open source license: http://www.macridesweb.com/oltest/license.html
*/

OLloaded=0;

var OLifsP1=null,OLifsSh=null,OLifsP2=null;

// IFRAME SHIM SUPPORT FUNCTIONS
function OLinitIfs(){
if(!OLie55)return;
if((OLovertwoPI)&&over2&&over==over2){
var o=o3_frame.document.all['overIframeOvertwo'];
if(!o||OLifsP2!=o){OLifsP2=null;OLgetIfsP2Ref();}return;}
o=o3_frame.document.all['overIframe'];
if(!o||OLifsP1!=o){OLifsP1=null;OLgetIfsRef();}
if((OLshadowPI)&&o3_shadow){o=o3_frame.document.all['overIframeShadow'];
if(!o||OLifsSh!=o){OLifsSh=null;OLgetIfsShRef();}}
}

function OLsetIfsRef(o,i,z){
o.id=i;o.src='javascript:false;';o.scrolling='no';var os=o.style;os.position='absolute';
os.top='0px';os.left='0px';os.width='1px';os.height='1px';os.visibility='hidden';
os.zIndex=over.style.zIndex-z;os.filter='Alpha(style=0,opacity=0)';
}

function OLgetIfsRef(){
if(OLifsP1||!OLie55)return;
OLifsP1=o3_frame.document.createElement('iframe');
OLsetIfsRef(OLifsP1,'overIframe',2);
o3_frame.document.body.appendChild(OLifsP1);
}

function OLgetIfsShRef(){
if(OLifsSh||!OLie55)return;
OLifsSh=o3_frame.document.createElement('iframe');
OLsetIfsRef(OLifsSh,'overIframeShadow',3);
o3_frame.document.body.appendChild(OLifsSh);
}

function OLgetIfsP2Ref(){
if(OLifsP2||!OLie55)return;
OLifsP2=o3_frame.document.createElement('iframe');
OLsetIfsRef(OLifsP2,'overIframeOvertwo',1);
o3_frame.document.body.appendChild(OLifsP2);
}

function OLsetDispIfs(o,w,h){
var os=o.style;
os.width=w+'px';os.height=h+'px';os.clip='rect(0px '+w+'px '+h+'px 0px)';
o.filters.alpha.enabled=true;
}

function OLdispIfs(){
if(!OLie55)return;
var wd=over.offsetWidth,ht=over.offsetHeight;
if(OLfilterPI&&o3_filter&&o3_filtershadow){wd+=5;ht+=5;}
if((OLovertwoPI)&&over2&&over==over2){
if(!OLifsP2)return;
OLsetDispIfs(OLifsP2,wd,ht);return;}
if(!OLifsP1)return;
OLsetDispIfs(OLifsP1,wd,ht);
if((!OLshadowPI)||!o3_shadow||!OLifsSh)return;
OLsetDispIfs(OLifsSh,wd,ht);
}

function OLshowIfs(){
if(OLifsP1){OLifsP1.style.visibility="visible";
if((OLshadowPI)&&o3_shadow&&OLifsSh)OLifsSh.style.visibility="visible";}
}

function OLhideIfs(o){
if(!OLie55||o!=over)return;
if(OLifsP1)OLifsP1.style.visibility="hidden";
if((OLshadowPI)&&o3_shadow&&OLifsSh)OLifsSh.style.visibility="hidden";
}

function OLrepositionIfs(X,Y){
if(OLie55){if((OLovertwoPI)&&over2&&over==over2){
if(OLifsP2)OLrepositionTo(OLifsP2,X,Y);}
else{if(OLifsP1){OLrepositionTo(OLifsP1,X,Y);if((OLshadowPI)&&o3_shadow&&OLifsSh)
OLrepositionTo(OLifsSh,X+o3_shadowx,Y+o3_shadowy);}}}
}

OLiframePI=1;
OLloaded=1;

/*
 overlibmws_hide.js plug-in module - Copyright Foteos Macrides 2003-2007. All rights reserved.
   For hiding elements.
   Initial: November 13, 2003 - Last Revised: March 10, 2007
 See the Change History and Command Reference for overlibmws via:

	http://www.macridesweb.com/oltest/

 Published under an open source license: http://www.macridesweb.com/oltest/license.html
*/

OLloaded=0;
var OLhideCmds='hideselectboxes,hidebyid,hidebyidall,hidebyidns4';
OLregCmds(OLhideCmds);

// DEFAULT CONFIGURATION
if(OLud('hideselectboxes'))var ol_hideselectboxes=0;
if(OLud('hidebyid'))var ol_hidebyid='';
if(OLud('hidebyidall'))var ol_hidebyidall='';
if(OLud('hidebyidns4'))var ol_hidebyidns4='';
// END CONFIGURATION

var o3_hideselectboxes=0,o3_hidebyid='',o3_hidebyidall='',o3_hidebyidns4='',
OLselectOK=(OLie7||OLop7||OLgek>=20030624)?1:0;

function OLloadHide(){
OLload(OLhideCmds);
}

function OLparseHide(pf,i,ar){
var k=i,q=OLparQuo;
if(k<ar.length){
if(Math.abs(ar[k])==HIDESELECTBOXES){OLtoggle(ar[k],pf+'hideselectboxes');return k;}
if(ar[k]==HIDEBYID){q(ar[++k],pf+'hidebyid');return k;}
if(ar[k]==HIDEBYIDALL){q(ar[++k],pf+'hidebyidall');return k;}
if(ar[k]==HIDEBYIDNS4){q(ar[++k],pf+'hidebyidns4');return k;}}
return -1;
}

function OLchkHide(hide){
if(OLiframePI&&OLie55)return;if(OLmodalPI&&o3_modal)o3_hideselectboxes=0;var id,o,i;
if(o3_hidebyid&&typeof o3_hidebyid=='string'&&!(o3_hideselectboxes&&OLns6)&&!OLop7&&!OLns4){
id=o3_hidebyid.replace(/[ ]/ig,'').split(',');for(i=0;i<id.length;i++){
o=(OLie4?o3_frame.document.all[id[i]]:OLns6?o3_frame.document.getElementById(id[i]):null);
if(o)o.style.visibility=(hide?'hidden':'visible');}}
if(o3_hidebyidall&&typeof o3_hidebyidall=='string'){
id=o3_hidebyidall.replace(/[ ]/ig,'').split(',');for(i=0;i<id.length;i++){
o=OLgetRefById(id[i]);if(o){o=(OLns4)?o:o.style;
o.visibility=(hide?'hidden':'visible');}}}
if(o3_hidebyidns4&&OLns4&&typeof o3_hidebyidns4=='string'){
id=o3_hidebyidns4.replace(/[ ]/ig,'').split(',');for(i=0;i<id.length;i++){
o=eval('o3_frame.document.'+id[i]);if(o)o.visibility=(hide?'hidden':'visible');}}
}

function OLselectBoxes(hide,all){
if((OLiframePI&&OLie55)||OLselectOK||OLns4)return;var sel=OLie4?
o3_frame.document.all.tags('select'):o3_frame.document.getElementsByTagName('select'),
px=over.offsetLeft,py=over.offsetTop,pw=over.offsetWidth,ph=over.offsetHeight,bx=px,by=py,
bw=pw,bh=ph,sx,sy,sw,sh,i,sp,si;if((OLshadowPI)&&bkdrop&&o3_shadow){bx=bkdrop.offsetLeft;
by=bkdrop.offsetTop;bw=bkdrop.offsetWidth;bh=bkdrop.offsetHeight;}for(i=0;i<sel.length;i++){
sx=0;sy=0;si=0;if(sel[i].offsetParent){sp=sel[i];while(sp.offsetParent&&
sp.offsetParent.tagName.toLowerCase()!='body'){if(sp.offsetParent.id=='overDiv'||
sp.offsetParent.id=='overDiv2')si=1;sp=sp.offsetParent;sx+=sp.offsetLeft;sy+=sp.offsetTop;}
sx+=sel[i].offsetLeft;sy+=sel[i].offsetTop;sw=sel[i].offsetWidth;sh=sel[i].offsetHeight;
if(si||(!OLie4&&sel[i].size<2))continue;else if(hide){if((px+pw>sx&&px<sx+sw&&py+ph>sy&&
py<sy+sh)||(bx+bw>sx&&bx<sx+sw&&by+bh>sy&&by<sy+sh)){if(sel[i].style.visibility!="hidden")
sel[i].style.visibility="hidden";}}else{if(all||(!(OLovertwoPI&&over==over2)&&(px+pw<sx||
px>sx+sw||py+ph<sy||py>sy+sh)&&(bx+bw<sx||bx>sx+sw||by+bh<sy||by>sy+sh))){
if(sel[i].style.visibility!="visible")sel[i].style.visibility="visible";}}}}
}

function OLhideUtil(a1,a2,a3,a4,a5,a6){
if(a4==null){OLchkHide(a1);if(o3_hideselectboxes)OLselectBoxes(a2,a3);}else{OLchkHide(a1);
OLchkHide(a2);if(o3_hideselectboxes){OLselectBoxes(a3,a4);OLselectBoxes(a5,a6);}}
}

OLregRunTimeFunc(OLloadHide);
OLregCmdLineFunc(OLparseHide);

OLhidePI=1;
OLloaded=1;

/*
 overlibmws_shadow.js plug-in module - Copyright Foteos Macrides 2003-2007. All rights reserved.
   For support of the SHADOW feature.
   Initial: July 14, 2003 - Last Revised: January 1, 2007
 See the Change History and Command Reference for overlibmws via:

	http://www.macridesweb.com/oltest/

 Published under an open source license: http://www.macridesweb.com/oltest/license.html
*/

OLloaded=0;
var OLshadowCmds='shadow,shadowx,shadowy,shadowcolor,shadowimage,shadowopacity';
OLregCmds(OLshadowCmds);

// DEFAULT CONFIGURATION
if(OLud('shadow'))var ol_shadow=0;
if(OLud('shadowx'))var ol_shadowx=5;
if(OLud('shadowy'))var ol_shadowy=5;
if(OLud('shadowcolor'))var ol_shadowcolor="#666666";
if(OLud('shadowimage'))var ol_shadowimage="";
if(OLud('shadowopacity'))var ol_shadowopacity=60;
// END CONFIGURATION

var o3_shadow=0,o3_shadowx=5,o3_shadowy=5,o3_shadowcolor="#666666",o3_shadowimage="";
var o3_shadowopacity=60,bkdrop=null;

function OLloadShadow(){
OLload(OLshadowCmds);
}

function OLparseShadow(pf,i,ar){
var k=i,p=OLpar,q=OLparQuo;
if(k<ar.length){
if(Math.abs(ar[k])==SHADOW){OLtoggle(ar[k],pf+'shadow');return k;}
if(ar[k]==SHADOWX){p(ar[++k],pf+'shadowx');return k;}
if(ar[k]==SHADOWY){p(ar[++k],pf+'shadowy');return k;}
if(ar[k]==SHADOWCOLOR){q(ar[++k],pf+'shadowcolor');return k;}
if(ar[k]==SHADOWIMAGE){q(ar[++k],pf+'shadowimage');return k;}
if(ar[k]==SHADOWOPACITY){p(ar[++k],pf+'shadowopacity');return k;}}
return -1;
}

function OLdispShadow(){
if(o3_shadow){OLgetShadowLyrRef();if(bkdrop)OLgenerateShadowLyr();}
}

function OLinitShadow(){
if(OLie55&&OLfilterPI&&o3_filter){if(o3_shadow){o3_shadow=0;
if(!o3_filtershadow){o3_filtershadow=2;o3_filtershadowcolor=o3_shadowcolor;}}return;}
var o;if(!(o=OLmkLyr((OLovertwoPI&&over2&&over==over2?'backdrop2':'backdrop'),
o3_frame,999))||bkdrop!=o){bkdrop=null;OLgetShadowLyrRef();}
}

function OLgetShadowLyrRef(){
if(bkdrop||!o3_shadow)return;
bkdrop=OLgetRefById((OLovertwoPI&&over2&&over==over2?'backdrop2':'backdrop'));
if(!bkdrop){o3_shadow=0;bkdrop=null;}
}

function OLgenerateShadowLyr(){
var wd=(OLns4?over.clip.width:over.offsetWidth),hgt=(OLns4?over.clip.height:over.offsetHeight);
if(OLns4){bkdrop.clip.width=wd;bkdrop.clip.height=hgt;
if(o3_shadowimage)bkdrop.background.src=o3_shadowimage;
else{bkdrop.bgColor=o3_shadowcolor;bkdrop.zIndex=over.zIndex -1;}
}else{var o=bkdrop.style;o.width=wd+'px';o.height=hgt+'px';
if(o3_shadowimage)o.backgroundImage="url("+o3_shadowimage+")";
else o.backgroundColor=o3_shadowcolor;
o.clip='rect(0px '+wd+'px '+hgt+'px 0px)';o.zIndex=over.style.zIndex -1;
if(o3_shadowopacity){var op=o3_shadowopacity;op=(op<=100&&op>0?op:100);
if(OLie4&&!OLieM&&typeof o.filter=='string'){
o.filter='Alpha(opacity='+op+')';if(OLie55&&typeof bkdrop.filters=='object')
bkdrop.filters.alpha.enabled=1;}else{op=op/100;OLopBk(op);}}}
}

function OLopBk(op){
var o=bkdrop.style;
if(typeof o.opacity!='undefined')o.opacity=op;
else if(typeof o.MozOpacity!='undefined')o.MozOpacity=op;
else if(typeof o.KhtmlOpacity!='undefined')o.KhtmlOpacity=op;
}

function OLcleanUpShadow(){
if(!bkdrop)return;
if(OLns4){bkdrop.bgColor=null;bkdrop.background.src=null;}else{
var o=bkdrop.style;o.backgroundColor='transparent';o.backgroundImage='none';
if(OLie4&&!OLieM&&typeof o.filter=='string'){
o.filter='Alpha(opacity=100)';if(OLie55&&typeof bkdrop.filters=='object')
bkdrop.filters.alpha.enabled=0;}else OLopBk(1.0);
if(OLns6){o.width=1+'px';o.height=1+'px';
OLrepositionTo(bkdrop,o3_frame.pageXOffset,o3_frame.pageYOffset);}}
}

function OLshowShadow(){if(bkdrop&&o3_shadow){var o=(OLns4?bkdrop:bkdrop.style);
if(!OLns4&&!OLieM&&(OLfilterPI&&o3_filter&&o3_fadein))OLopOvSh(1);o.visibility="visible";}
}

function OLhideShadow(){
if(bkdrop&&o3_shadow){var o=OLgetRefById((OLovertwoPI&&over2&&over==over2?
'backdrop2':'backdrop'));if(o&&o==bkdrop){var os=(OLns4?bkdrop:bkdrop.style);
if(OLns4||OLieM||!OLfilterPI||((OLfilterPI)&&(!o3_filter||!o3_fadeout||!OLhasOp()))){
os.visibility="hidden";OLcleanUpShadow();}}}
}

function OLrepositionShadow(X,Y){
if(bkdrop&&o3_shadow)OLrepositionTo(bkdrop,X+o3_shadowx,Y+o3_shadowy);
}

OLregRunTimeFunc(OLloadShadow);
OLregCmdLineFunc(OLparseShadow);

OLshadowPI=1;
OLloaded=1;

/**
 * Copyright 2005 Darren L. Spurgeon
 * Copyright 2007 Jens Kapitza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


var AjaxJspTag = {
  Version: '1.3'
};

/**
 * AjaxTags
 */

AjaxJspTag.Base = function() {};
AjaxJspTag.Base.prototype = {

  resolveParameters: function() {
    // Strip URL of querystring and append it to parameters
    var qs = delimitQueryString(extractQueryString(this.url));
    if (this.options.parameters) {
      this.options.parameters += ',' + qs;
    } else {
      this.options.parameters = qs;
    }
    this.url = trimQueryString(this.url);

    if ((this.options.parameters.length > 0 ) && (this.options.parameters.charAt(this.options.parameters.length - 1) === ',')) {
      this.options.parameters = this.options.parameters.substr(0,this.options.parameters.length-1);
    }
  }

};

/**
 * Prefunction Invoke Ajax.Update TAG
 */
AjaxJspTag.PreFunctionUpdateInvoke = Class.create();
AjaxJspTag.PreFunctionUpdateInvoke.prototype = Object.extend(new AjaxJspTag.Base(), {

  initialize: function(ajaxupdateData) {
  this.preFunction = ajaxupdateData.preFunction;
  if (isFunction(this.preFunction))
  {
  	this.preFunction();
  }
  if (this.cancelExecution) {
	    	this.cancelExecution = false;
	    	return ;
      	}
  var thisCall = new Ajax.Updater(ajaxupdateData.id,ajaxupdateData.href,{onComplete: ajaxupdateData.postFunction});
  }


});


/**
 * UPDATEFIELD TAG
 */
AjaxJspTag.UpdateField = Class.create();
AjaxJspTag.UpdateField.prototype = Object.extend(new AjaxJspTag.Base(), {

  initialize: function(url, options) {
    this.url = url;
    this.setOptions(options);
    this.setListeners();
    addAjaxListener(this);
  },
  reload: function () {
    this.setListeners();
  },
  setOptions: function(options) {
    this.options = Object.extend({
      parameters: options.parameters || '',
      doPost: options.doPost || false,
      valueUpdateByName:  options.valueUpdateByName || false,
      eventType: options.eventType ? options.eventType : "click",
      parser: options.parser ? options.parser :  ( options.valueUpdateByName ? new ResponseXmlParser(): new ResponseTextParser()),
      handler: options.handler ? options.handler : this.handler
    }, options || {});
  },

  setListeners: function() {
    eval("$(this.options.action).on"+this.options.eventType+" = this.execute.bindAsEventListener(this)");
  },

  execute: function(e) {
    if (isFunction(this.options.preFunction))
    {
    	this.options.preFunction();
	}
	if (this.options.cancelExecution) {
	    this.cancelExecution = false;
	    return ;
      }
    // parse parameters and do replacements
    var params = buildParameterString(this.options.parameters);

    // parse targets
    var targetList = this.options.target.split(',');

    var obj = this; // required because 'this' conflict with Ajax.Request
    var setFunc = this.setField;
    var aj = new Ajax.Request(this.url, {
      asynchronous: true,
      method: obj.options.doPost ? 'post':'get',
      evalScripts: true,
      parameters: params,
      onSuccess: function(request) {
        obj.options.parser.load(request);
        var results = obj.options.parser.itemList;
        obj.options.handler(request, {targets: targetList, items: results});
      },
      onFailure: function(request) {
        if (isFunction(obj.options.errorFunction)){
         obj.options.errorFunction(request,obj.options.parser);
     	}
      },
      onComplete: function(request) {
        if (isFunction(obj.options.postFunction)) { obj.options.postFunction(); }
      }
    });
  },

  handler: function(request, optionsArr) {
  // this points to options
    for (var i=0; i<optionsArr.targets.length && i<optionsArr.items.length; i++) {
   	namedIndex = i;
   	if (this.valueUpdateByName) {
    	for (j=0; j <optionsArr.items.length; j++) {
    		if (optionsArr.targets[i]  ===  optionsArr.items[j][0]) {
    			namedIndex = j;
    		}
    	}
    }
    $(optionsArr.targets[i]).value = optionsArr.items[namedIndex][1];
    }
  }

});



/**
 * CALLBACK TAG
 */
AjaxJspTag.Callback = Class.create();
AjaxJspTag.Callback.prototype = Object.extend(new AjaxJspTag.Base(), {

  initialize: function(url, options) {
    this.url = url;
    this.setOptions(options);
    this.errorCount = 0;
    addOnLoadEvent(this );
  },
  setOptions: function(options) {
    this.options = Object.extend({
      parameters: options.parameters || '',
      parser: options.parser ? options.parser : new ResponseCallBackXmlParser(),
      plainText: options.plainText ?   true : false ,
      handler: options.handler ? options.handler : this.handler
    }, options || {});
  },
  onload: function(){
  	this.run();
  },
  run: function(){
  // wenn fehler kommen den client veranlassen eben nicht mehr versuchen sich anzumelden
    if (!this.isRunning && this.errorCount < 100) {
      this.execute();
    }
  },
  execute: function(e) {
    if (isFunction(this.options.preFunction)) { this.options.preFunction();}
	if (this.options.cancelExecution) {
	    this.cancelExecution = false;
	    return ;
      }
    // parse parameters and do replacements
    //var params = buildParameterString(this.options.parameters);

    // parse targets
    this.isRunning = true;
    var obj = this; // required because 'this' conflict with Ajax.Request
    var aj = new Ajax.Request(this.url, {
      asynchronous: true,
      method:  'post',
      evalScripts: true,
      onSuccess: function(request) {
        obj.options.parser.load(request);
        obj.options.list = obj.options.parser.items;
        obj.errorCount = 0;

      },
      onFailure: function(request) {
        if (isFunction(obj.options.errorFunction)) {obj.options.errorFunction();}
        obj.isRunning = false;
        obj.errorCount++;
      },
      onComplete: function(request) {
      	// nun this.list kann mit der antwor alles gemacht werden was man will
        if (isFunction(obj.options.postFunction)) {obj.options.postFunction();}
        obj.isRunning = false;
        obj.run();
      }
    });
  }
});
/// callback -- ende





/**
 * SELECT TAG
 */
AjaxJspTag.Select = Class.create();
AjaxJspTag.Select.prototype = Object.extend(new AjaxJspTag.Base(), {

  initialize: function(url, options) {
    this.url = url;
    this.setOptions(options);
    this.setListeners();

    if (parseBoolean(this.options.executeOnLoad)) {
      this.execute();
    }
   addAjaxListener(this);
  },
  reload: function () {
    this.setListeners();
  },

  setOptions: function(options) {
    this.options = Object.extend({
      parameters: options.parameters || '',
      doPost: options.doPost || false,
      emptyOptionValue: options.emptyOptionValue || '',
      emptyOptionName:  options.emptyOptionName || '',
      eventType: options.eventType ? options.eventType : "change",
      parser: options.parser ? options.parser : new ResponseXmlParser(),
      handler: options.handler ? options.handler : this.handler
    }, options || {});
  },

  setListeners: function() {
  $(this.options.source).ajaxSelect = this;

    Event.observe($(this.options.source),
      this.options.eventType,
      this.execute.bindAsEventListener(this),
      false);
    eval("$(this.options.source).on"+this.options.eventType+" = function(){return false;};");
  },

  execute: function(e) {
    if (isFunction(this.options.preFunction)) {this.options.preFunction();}
	if (this.options.cancelExecution) {
	    this.cancelExecution = false;
	    return ;
      }
    // parse parameters and do replacements
    var params = buildParameterString(this.options.parameters);

    var obj = this; // required because 'this' conflict with Ajax.Request
    var aj = new Ajax.Request(this.url, {
      asynchronous: true,
      method: obj.options.doPost ? 'post':'get',
      evalScripts: true,
      parameters: params,
      onSuccess: function(request) {
        obj.options.parser.load(request);
        var results = obj.options.parser.itemList;
        obj.options.handler(request, {target: obj.options.target,
                                      items: results });
      },
      onFailure: function(request) {
        if (isFunction(obj.options.errorFunction)){ obj.options.errorFunction();}
      },
      onComplete: function(request) {
        if (isFunction(obj.options.postFunction)) {obj.options.postFunction();}
      }
    });
  },

  handler: function(request, options) {
    // build an array of option values to be set as selected

    $(options.target).options.length = 0;
    $(options.target).disabled = false;
    for (var i=0; i<options.items.length; i++) {
      var newOption = new Option(options.items[i][0], options.items[i][1]);
      //$(options.target).options[i] = new Option(options.items[i][0], options.items[i][1]);
      // set the option as selected if it is in the default list
      if ( newOption.selected == false && options.items[i].length == 3 && parseBoolean(options.items[i][2]) ){
           newOption.selected = true;
      }
      $(options.target).options[i] = newOption;
    }


    if (options.items.length == 0)
    {
      $(options.target).options[i] = new Option(this.emptyOptionName, this.emptyOptionValue);
    	$(options.target).disabled = true;
    }
    // auch ein SELECT TAG ?
   	if ($(options.target).ajaxSelect && $(options.target).ajaxSelect.execute)
   	{
   		$(options.target).ajaxSelect.execute();
   	}
  }

});


/**
 * HTMLCONTENT TAG
 */
AjaxJspTag.HtmlContent = Class.create();
AjaxJspTag.HtmlContent.prototype = Object.extend(new AjaxJspTag.Base(), {

  initialize: function(url, options) {
    this.url = url;
    this.setOptions(options);
    this.setListeners();
    addAjaxListener(this);

  },
  reload: function(){
  	this.setListeners();
  },
  setOptions: function(options) {
    this.options = Object.extend({
      parameterName: options.parameterName ? options.parameterName : AJAX_DEFAULT_PARAMETER,
      parameters: options.parameters || '',
      doPost: options.doPost || false,

      preFunctionParameter:options.preFunctionParameter || null,
      errorFunctionParameter:  options.errorFunctionParameter || null,
      postFunctionParameter: options.postFunctionParameter || null,

      eventType: options.eventType ? options.eventType : "click",
      parser: options.parser ? options.parser : new ResponseHtmlParser(),
      handler: options.handler ? options.handler : this.handler
    }, options || {});
  },

  setListeners: function() {
    if (this.options.source) {
      eval("$(this.options.source).on"+this.options.eventType+" = this.execute.bindAsEventListener(this)");
    } else if (this.options.sourceClass) {
      var elementArray = document.getElementsByClassName(this.options.sourceClass);
      for (var i=0; i<elementArray.length; i++) {
        eval("elementArray[i].on"+this.options.eventType+" = this.execute.bindAsEventListener(this)");
      }
    }
  },

  execute: function(e) {
  	this.options.preFunctionParameters = evalJScriptParameters(  this.options.preFunctionParameter);

    if (isFunction(this.options.preFunction)) {this.options.preFunction();}
	if (this.options.cancelExecution) {
	    this.cancelExecution = false;
	    return ;
      }
    // replace default parameter with value/content of source element selected
    var ajaxParameters = this.options.parameters;
    if (this.options.sourceClass) {
      var re = new RegExp("(\\{"+this.options.parameterName+"\\})", 'g');
      var elem = Event.element(e);
      if (elem.type) {
        ajaxParameters = ajaxParameters.replace(re, $F(elem));
      } else {
        ajaxParameters = ajaxParameters.replace(re, elem.innerHTML);
      }
    }

    // parse parameters and do replacements
    var params = buildParameterString(ajaxParameters);

    var obj = this; // required because 'this' conflict with Ajax.Request
    var aj = new Ajax.Updater(this.options.target, this.url, {
      asynchronous: true,
      method: obj.options.doPost ? 'post':'get',
      evalScripts: true,
      parameters: params,
      onFailure: function(request) {
        obj.options. errorFunctionParameters =  evalJScriptParameters(  obj.options.errorFunctionParameter  );
        if (isFunction(obj.options.errorFunction)) {obj.options.errorFunction();}
      },
      onComplete: function(request) {
        obj.options. postFunctionParameters =  evalJScriptParameters(   obj.options.postFunctionParameter);
        if (isFunction(obj.options.postFunction)) {obj.options.postFunction();}
      }
    });
  }

});

/**
 * TREE TAG
 */
AjaxJspTag.Tree = Class.create();
AjaxJspTag.Tree.prototype = Object.extend(new AjaxJspTag.Base(), {

  initialize: function(url, options) {
    this.url = url;
    this.setOptions(options);
    this.execute();
  },

  setOptions: function(options) {
    this.options = Object.extend({
      parameters: options.parameters || '',
      eventType: options.eventType ? options.eventType : "click",
      parser: options.parser ? options.parser : new ResponseXmlToHtmlLinkListParser(),
      handler: options.handler ? options.handler : this.handler,
      collapsedClass: options.collapsedClass ? options.collapsedClass : "collapsedNode",
      expandedClass: options.expandedClass ? options.expandedClass : "expandedNode",
      treeClass: options.treeClass ? options.treeClass : "tree",
      nodeClass: options.nodeClass || ''
    }, options || {});
    this.calloutParameter = AJAX_DEFAULT_PARAMETER;
  },

  execute: function(e) {
    if (isFunction(this.options.preFunction)) {this.options.preFunction();}
	if (this.options.cancelExecution) {
	    this.cancelExecution = false;
	    return ;
      }

    //if the node is expanded, just collapse it
    if(this.options.target != null) {
      var imgElem = $("span_" + this.options.target);
      if(imgElem != null) {
        var expanded = this.toggle(imgElem);
        if(!expanded) {
          $(this.options.target).innerHTML = "";
             if (! $(this.options.target).style)
       			$(this.options.target).setAttribute("style","");

     		$(this.options.target).style.display ="none";

          return;
        }
      }
    }
    // indicator

    // parse parameters and do replacements
    var ajaxParameters = this.options.parameters || '';
    var re = new RegExp("(\\{"+this.calloutParameter+"\\})", 'g');
    ajaxParameters = ajaxParameters.replace(re, this.options.target);

    var params = buildParameterString(ajaxParameters);

    var obj = this; // required because 'this' conflict with Ajax.Request
    var aj = new Ajax.Request(this.url, {
      asynchronous: true,
      method: 'get',
      evalScripts: true,
      parameters: params,
      onSuccess: function(request) {
      // IE 5,6 BUG
      	objx = new Object();
		objx.responseXML = request.responseXML;

         obj.options.parser.load(Object.extend(objx, {
                                                   collapsedClass: obj.options.collapsedClass,
                                                   treeClass:      obj.options.treeClass,
                                                   nodeClass:      obj.options.nodeClass}));
         obj.options.handler(objx, {target:    obj.options.target,
                                       parser:    obj.options.parser,
                                       eventType: obj.options.eventType,
                                       url:       obj.url});
      },
      onFailure: function(request) {
        if (isFunction(obj.options.errorFunction)){ obj.options.errorFunction();}
      },
      onComplete: function(request) {
        if (isFunction(obj.options.postFunction)) {obj.options.postFunction();}
		// damit htmlcontent wieder geht
        reloadAjaxListeners();
      }
    });
  },

  toggle: function (e) {
    var expanded =  e.className == this.options.expandedClass;
    e.className =  expanded ? this.options.collapsedClass : this.options.expandedClass;
    return !expanded;
  },

  handler: function(request, options) {
    var parser = options.parser;
    var target = $(options.target);
    if (parser.content == null) {

         // div.setAttribute("style","");
        //  div.style.display ="none";

    if (!target.style)
      target.setAttribute("style","");

    target.style.display ="none";

    	 target.innerHTML = "";
     	return;
    }



    target.appendChild(parser.content);

    if (!target.style)
      target.setAttribute("style","");

    target.style.display ="block";

    var images = target.getElementsByTagName("span");
    for (var i=0; i<images.length; i++) {
      //get id
      var id = images[i].id.substring(5);
      var toggleFunction = "function() {toggleTreeNode('" +  id + "', '" + options.url + "', null);}";
      eval("images[i].on" + options.eventType + "=" + toggleFunction);
    }

    //toggle the one that must be expanded
    var expandedNodes = parser.expandedNodes;
    for (var i=0; i<expandedNodes.length; i++) {
       toggleTreeNode(expandedNodes[i], options.url, null);
    }
  }

});

/**
 * TABPANEL TAG
 */
AjaxJspTag.TabPanel = Class.create();
AjaxJspTag.TabPanel.prototype = Object.extend(new AjaxJspTag.Base(), {

  initialize: function(url, options) {
    this.url = url;
    this.setOptions(options);
    this.execute();
  },

  setOptions: function(options) {
    this.options = Object.extend({
      parameters: options.parameters || '',
      eventType: options.eventType ? options.eventType : "click",
      parser: options.parser ? options.parser : new ResponseHtmlParser(),
      handler: options.handler ? options.handler : this.handler
    }, options || {});
  },

  execute: function(e) {
    if (isFunction(this.options.preFunction)){ this.options.preFunction();}
	if (this.options.cancelExecution) {
	    this.cancelExecution = false;
	    return ;
      }
    // parse parameters and do replacements
    this.resolveParameters();
    var params = buildParameterString(this.options.parameters);

    var obj = this; // required because 'this' conflict with Ajax.Request
    var aj = new Ajax.Updater(this.options.target, this.url, {
      asynchronous: true,
      method: 'get',
      evalScripts: true,
      parameters: params,
      onSuccess: function(request) {
        var src;
        if (obj.options.source) {
          src = obj.options.source;
        } else {
          src = document.getElementsByClassName(obj.options.currentStyleClass,
                                                $(obj.options.panelId))[0];
        }
        obj.options.handler(request, {source: src,
                                      panelStyleId: obj.options.panelId,
                                      currentStyleClass: obj.options.currentStyleClass});
      },
      onFailure: function(request) {
        if (isFunction(obj.options.errorFunction)){ obj.options.errorFunction();}
      },
      onComplete: function(request) {
        if (isFunction(obj.options.postFunction)){ obj.options.postFunction();}
      }
    });
  },

  handler: function(request, options) {
    // find current anchor
    var cur = document.getElementsByClassName(options.currentStyleClass, $(options.panelStyleId));
    // remove class
    if(cur.length > 0)
        cur[0].className = '';
    // add class to selected tab
    options.source.className = options.currentStyleClass;
  }

});


/**
 * PORTLET TAG
 */
AjaxJspTag.Portlet = Class.create();
AjaxJspTag.Portlet.prototype = Object.extend(new AjaxJspTag.Base(), {

  initialize: function(url, options) {
    this.url = url;
    this.setOptions(options);
    this.setListeners();
    if (parseBoolean(this.options.executeOnLoad )) {
      this.execute();
    }
    if (this.preserveState) this.checkCookie();

    if (parseBoolean(this.options.startMinimize)) {
   		this.togglePortlet();
    }
    addAjaxListener(this);
    // should i reloadAjaxListeners() after execute?
  },
  reload: function () {
    this.setListeners();
  },

  setOptions: function(options) {
    this.options = Object.extend({
      parameters: options.parameters || '',
      target: options.source+"Content",
      close: options.source+"Close",
      startMinimize: options.startMinimize || false,
      refresh: options.source+"Refresh",
      toggle: options.source+"Size",
      isMaximized: true,
      expireDays: options.expireDays || "0",
      expireHours: options.expireHours || "0",
      expireMinutes: options.expireMinutes || "0",
      executeOnLoad: evalBoolean(options.executeOnLoad, true),
      refreshPeriod: options.refreshPeriod || null,
      eventType: options.eventType ? options.eventType : "click",
      parser: options.parser ? options.parser : new ResponseHtmlParser(),
      handler: options.handler ? options.handler : this.handler
    }, options || {});

    if (parseInt(this.options.expireDays) > 0
        || parseInt(this.options.expireHours) > 0
        || parseInt(this.options.expireMinutes) > 0) {
      this.preserveState = true;
      this.options.expireDate = getExpDate(
        parseInt(this.options.expireDays),
        parseInt(this.options.expireHours),
        parseInt(this.options.expireMinutes));
    }

    this.isAutoRefreshSet = false;
  },

  setListeners: function() {
    if (this.options.imageClose) {
      eval("$(this.options.close).on"+this.options.eventType+" = this.closePortlet.bindAsEventListener(this)");
    }
    if (this.options.imageRefresh) {
      eval("$(this.options.refresh).on"+this.options.eventType+" = this.refreshPortlet.bindAsEventListener(this)");
    }
    if (this.options.imageMaximize && this.options.imageMinimize) {
      eval("$(this.options.toggle).on"+this.options.eventType+" = this.togglePortlet.bindAsEventListener(this)");
    }
  },

  execute: function(e) {
    if (isFunction(this.options.preFunction)){ this.options.preFunction();}
	if (this.options.cancelExecution) {
	    this.cancelExecution = false;
	    return ;
      }
    // parse parameters and do replacements
    this.resolveParameters();
    var params = buildParameterString(this.options.parameters);

    var obj = this; // required because 'this' conflict with Ajax.Request
    if (this.options.refreshPeriod && this.isAutoRefreshSet == false) {
      // periodic updater
      var freq = this.options.refreshPeriod;
      this.ajaxPeriodicalUpdater = new Ajax.PeriodicalUpdater(this.options.target, this.url, {
        asynchronous: true,
        method: 'get',
        evalScripts: true,
        parameters: params,
        frequency: freq,
        onFailure: function(request) {
          if (isFunction(obj.options.errorFunction)){ obj.options.errorFunction();}
        },
        onComplete: function(request) {},
        onSuccess: function(request) {
          if (isFunction(obj.options.postFunction)) {obj.options.postFunction();}
        }
      });

      this.isAutoRefreshSet = true;
    } else {
      // normal updater
      this.ajaxUpdater = new Ajax.Updater(this.options.target, this.url, {
        asynchronous: true,
        method: 'get',
        parameters: params,
        evalScripts: true,
        onFailure: function(request) {
          if (isFunction(obj.options.errorFunction)) {obj.options.errorFunction();}
        },
        onComplete: function(request) {
          if (isFunction(obj.options.postFunction)) {obj.options.postFunction();}
        }
      });
    }

  },

  checkCookie: function() {
    // Check cookie for save state
    var cVal = getCookie("AjaxJspTag.Portlet."+this.options.source);
    if (cVal != null) {
      if (cVal == AJAX_PORTLET_MIN) {
        this.togglePortlet();
      } else if (cVal == AJAX_PORTLET_CLOSE) {
        this.closePortlet();
      }
    }
  },

  stopAutoRefresh: function() {
    // stop auto-update if present
    if (this.ajaxPeriodicalUpdater != null
        && this.options.refreshPeriod
        && this.isAutoRefreshSet == true) {
      this.ajaxPeriodicalUpdater.stop();
    }
  },

  startAutoRefresh: function() {
    // stop auto-update if present
    if (this.ajaxPeriodicalUpdater != null && this.options.refreshPeriod) {
      this.ajaxPeriodicalUpdater.start();
    }
  },

  refreshPortlet: function(e) {
    // clear existing updater
    this.stopAutoRefresh();
    if (this.ajaxPeriodicalUpdater != null) {
      this.startAutoRefresh();
    } else {
      this.execute();
    }
  },

  closePortlet: function(e) {
    this.stopAutoRefresh();
    Element.remove(this.options.source);
    // Save state in cookie
    if (this.preserveState) {
      setCookie("AjaxJspTag.Portlet."+this.options.source,
        AJAX_PORTLET_CLOSE,
        this.options.expireDate);
    }
  },

  togglePortlet: function(e) {
    Element.toggle(this.options.target);
    if (this.options.isMaximized) {
    if (this.options.imageMaximize){
      $(this.options.toggle).src = this.options.imageMaximize;
      }
      this.stopAutoRefresh();
    } else {
     if (this.options.imageMinimize){
      $(this.options.toggle).src = this.options.imageMinimize;
      }
      this.startAutoRefresh();
    }
    this.options.isMaximized = !this.options.isMaximized;
    // Save state in cookie
    if (this.preserveState) {
      setCookie("AjaxJspTag.Portlet."+this.options.source,
        (this.options.isMaximized === true ? AJAX_PORTLET_MAX : AJAX_PORTLET_MIN),
        this.options.expireDate);
    }
  }

});


/**
 * AUTOCOMPLETE TAG
 */
Ajax.XmlToHtmlAutocompleter = Class.create();
Object.extend(Object.extend(Ajax.XmlToHtmlAutocompleter.prototype,  Autocompleter.Base.prototype), {
  initialize: function(element, update, url, options) {
    this.baseInitialize(element, update, options);
    this.options.asynchronous  = true;
    this.options.onComplete    = this.onComplete.bind(this);
    this.options.defaultParams = this.options.parameters || null;
    this.url                   = url;
  },
  // onblur hack IE works with FF
     onBlur: function (event) {
  	  // Dont hide the div on "blur" if the user clicks scrollbar
	if(Element.getStyle(this.update, 'height') != ''){
 		var x=999999;
 		var y=999999;
 		var offsets = Position.positionedOffset(this.update);
 		var top = offsets[1];
 		var left = offsets[0];
 		var data = Element.getDimensions(this.update);
 		var width = data.width;
 		var height = data.height;
 		if (event)
 		{
 			x=event.x-left;
 			y=event.y -top;
 		}

        if (x > 0 && x <  width && y > 0 && y < height )
        {
      	this.element.focus();
        return;
        }
      }

        // needed to make click events working
    setTimeout(this.hide.bind(this), 250);
    this.hasFocus = false;
    this.active = false;


  },
  getUpdatedChoices: function() {
   if (isFunction(this.options.preFunction)){ this.options.preFunction();}
      // preFunction can cancelExecution set this.cancelExecution = true;
	  if (this.options.cancelExecution) {
	    this.cancelExecution = false;
	    this.stopIndicator();
	    return ;
      }
    entry = encodeURIComponent(this.options.paramName) + '=' +
      encodeURIComponent(this.getToken());

    this.options.parameters = this.options.callback ?
      this.options.callback(this.element, entry) : entry;

    // parse parameters and do replacements
    var params = buildParameterString(this.options.defaultParams);
    if (!isEmpty(params) || (isString(params) && params.length > 0)) {
      this.options.parameters += '&' + params;
    }

    new Ajax.Request(this.url, this.options);
  },
  onComplete: function(request) {
    var parser = this.options.parser;
    parser.load(request);
    this.updateChoices(parser.content);
  }

});

AjaxJspTag.Autocomplete = Class.create();
AjaxJspTag.Autocomplete.prototype = Object.extend(new AjaxJspTag.Base(), {

  initialize: function(url, options) {
    this.url = url;
    this.setOptions(options);
    // create DIV
    new Insertion.After(this.options.source, '<div id="' + this.options.divElement + '" class="' + this.options.className + '"></div>');
    this.execute();

  },

  setOptions: function(options) {
    this.options = Object.extend({
      divElement: "ajaxAuto_" + options.source,
      indicator: options.indicator || '',
      parameters: options.parameters || '',
      parser: options.parser ? options.parser : new ResponseXmlToHtmlListParser(),
      handler: options.handler ? options.handler : this.handler
    }, options || {});
  },

  execute: function(e) {
      // preFunction moved bevor request now
    var obj = this; // required because 'this' conflict with Ajax.Request
    var aj = new Ajax.XmlToHtmlAutocompleter(
                     this.options.source,
                     this.options.divElement,
                     this.url, {minChars: obj.options.minimumCharacters,
                                tokens: obj.options.appendSeparator,
                                indicator: obj.options.indicator,
                                parameters: obj.options.parameters,
                                evalScripts: true,
                                preFunction: obj.options.preFunction,
                                parser: obj.options.parser,
                                afterUpdateElement: function(inputField, selectedItem) {
                                  obj.options.handler(null, {
                                    selectedItem: selectedItem,
                                    tokens: obj.options.appendSeparator,
                                    target: obj.options.target,
                                    inputField: inputField,
                                    postFunction: obj.options.postFunction,
                                    list:obj.options.parser.getArray(),
                                    options: obj.options,
                                    autocomplete:aj
                                    }
                                  );
                                }
                               }
             );
  },

  handler: function(request, options) {
    if (options.target) {
      if (options.tokens) {
        if ($(options.target).value.length > 0) {
          $(options.target).value += options.tokens;
        }
        $(options.target).value += options.selectedItem.id;
      } else {
        $(options.target).value = options.selectedItem.id;
      }
    }
    options.selectedIndex = options.autocomplete.index;
    options.selectedObject = options.list[options.autocomplete.index];

    if (isFunction(options.postFunction)) {
      //Disable onupdate event handler of input field
      //because, postFunction can change the content of
      //input field and get into eternal loop.
      var onupdateHandler = $(options.inputField).onupdate;
      $(options.inputField).onupdate = '';

      options.postFunction();
      //Enable onupdate event handler of input field
      $(options.inputField).onupdate = onupdateHandler;
    }
  }

});


/**
 * TOGGLE TAG
 */
AjaxJspTag.Toggle = Class.create();
AjaxJspTag.Toggle.prototype = Object.extend(new AjaxJspTag.Base(), {

  initialize: function(url, options) {
    this.url = url;
    this.setOptions(options);

    // create message DIV
    if (this.options.messageClass) {
      this.messageContainer = new Insertion.Top($(this.options.source),
        '<div id="'+ this.options.source +'_message" class="' + this.options.messageClass +'"></div>');
    }

    this.setListeners();
    addAjaxListener(this);
  },
  reload: function () {
    this.setListeners();
  },

  setOptions: function(options) {
    this.options = Object.extend({
      parameters: options.parameters || 'rating={ajaxParameter}',
      parser: options.parser ? options.parser : new ResponseTextParser(),
      handler: options.handler ? options.handler : this.handler,
      updateFunction: options.updateFunction || false
    }, options || {});
    this.ratingParameter = AJAX_DEFAULT_PARAMETER;
  },

  setListeners: function() {
    // attach events to anchors
    var elements = $(this.options.source).getElementsByTagName('a');
    for (var j=0; j<elements.length; j++) {
      elements[j].onmouseover = this.raterMouseOver.bindAsEventListener(this);
      elements[j].onmouseout = this.raterMouseOut.bindAsEventListener(this);
      elements[j].onclick = this.raterClick.bindAsEventListener(this);
    }
  },

  getCurrentRating: function(list) {
    var selectedIndex = -1;
    for (var i=0; i<list.length; i++) {
      if (Element.hasClassName(list[i], this.options.selectedClass)) {
        selectedIndex = i;
      }
    }
    return selectedIndex;
  },

  getCurrentIndex: function(list, elem) {
    var currentIndex = 0;
    for (var i=0; i<list.length; i++) {
      if (elem == list[i]) {
        currentIndex = i;
      }
    }
    return currentIndex;
  },

  raterMouseOver: function (e) {
    // get containing div
    var container = Event.findElement(e, 'div');

    // get list of all anchors
    var elements = container.getElementsByTagName('a');

    // find the current rating
    var selectedIndex = this.getCurrentRating(elements);

    // find the index of the 'hovered' element
    var currentIndex = this.getCurrentIndex(elements, Event.element(e));

    // set message
    if (this.options.messageClass) {
      $(container.id+'_message').innerHTML = Event.element(e).title;
    }

    // iterate over each anchor and apply styles
    for (var i=0; i<elements.length; i++) {
      if (selectedIndex > -1) {
        if (i <= selectedIndex && i <= currentIndex)
          Element.addClassName(elements[i], this.options.selectedOverClass);
        else if (i <= selectedIndex && i > currentIndex)
          Element.addClassName(elements[i], this.options.selectedLessClass);
        else if (i > selectedIndex && i <= currentIndex)
          Element.addClassName(elements[i], this.options.overClass);
      } else {
        if (i <= currentIndex) Element.addClassName(elements[i], this.options.overClass);
      }
    }
  },

  raterMouseOut: function (e) {
    // get containing div
    var container = Event.findElement(e, 'div');

    // get list of all anchors
    var elements = container.getElementsByTagName('a');

    // clear message
    if (this.options.messageClass) {
      $(container.id+'_message').innerHTML = '';
    }

    // iterate over each anchor and apply styles
    for (var i=0; i<elements.length; i++) {
      Element.removeClassName(elements[i], this.options.selectedOverClass);
      Element.removeClassName(elements[i], this.options.selectedLessClass);
      Element.removeClassName(elements[i], this.options.overClass);
    }
  },

  raterClick: function (e) {
    // get containing div
    var container = Event.findElement(e, 'div');

    // get list of all anchors
    var elements = container.getElementsByTagName('a');

    // find the index of the 'hovered' element
    var currentIndex = this.getCurrentIndex(elements, Event.element(e));

    // update styles
    for (var i=0; i<elements.length; i++) {
      Element.removeClassName(elements[i], this.options.selectedOverClass);
      Element.removeClassName(elements[i], this.options.selectedLessClass);
      Element.removeClassName(elements[i], this.options.overClass);
      if (i <= currentIndex) {
        if (Element.hasClassName(container, 'onoff')
              && Element.hasClassName(elements[i], this.options.selectedClass)) {
          Element.removeClassName(elements[i], this.options.selectedClass);
        } else {
          Element.addClassName(elements[i], this.options.selectedClass);
        }
      } else if (i > currentIndex) {
        Element.removeClassName(elements[i], this.options.selectedClass);
      }
    }

    // send AJAX
    var ratingToSend = elements[currentIndex].title;
    if (Element.hasClassName(container, 'onoff')) {
      // send opposite of what was selected
      var ratings = this.options.ratings.split(',');
      if (ratings[0] == ratingToSend) ratingToSend = ratings[1];
      else ratingToSend = ratings[0];
      elements[currentIndex].title = ratingToSend;
    }
    this.execute(ratingToSend);

    // set field (if defined)
    if (this.options.state) {
      $(this.options.state).value = ratingToSend;
    }
  },

  execute: function(ratingValue) {
    if (isFunction(this.options.preFunction)){ this.options.preFunction();}
	if (this.options.cancelExecution) {
	    this.cancelExecution = false;
	    return ;
      }
    // parse parameters and do replacements
    var ajaxParameters = this.options.parameters || '';
    var re = new RegExp("(\\{"+this.ratingParameter+"\\})", 'g');
    ajaxParameters = ajaxParameters.replace(re, ratingValue);
    var params = buildParameterString(ajaxParameters);

    var obj = this; // required because 'this' conflict with Ajax.Request
    var toggleStateFunc = this.getToggleStateValue;
    var aj = new Ajax.Request(this.url, {
      asynchronous: true,
      method: 'get',
      evalScripts: true,
      parameters: params,
      onSuccess: function(request) {
        obj.options.parser.load(request);
        var results = obj.options.parser.itemList;
        obj.options.handler(request, {items: results});
      },
      onFailure: function(request) {
        if (isFunction(obj.options.errorFunction)){ obj.options.errorFunction();}
      },
      onComplete: function(request) {
        if (isFunction(obj.options.postFunction)) {obj.options.postFunction();}
      }
    });
  },

  handler: function(request, roptions) {
  //daten in items
  	var erg = roptions.items[0][0] ; // on/off / 1,2,3
  	try  {
  	this.updateFunction(erg);
    // TODO: anything?
    } catch (e) {} // muss nicht forhanden sein
  },

  getToggleStateValue: function(name, results) {
    for (var i=0; i<results.length; i++) {
      if (results[i][0] == name) {return results[i][1];}
    }
    return "";
  }

});


/**
 * CALLOUT TAG
 */
AjaxJspTag.Callout = Class.create();
AjaxJspTag.Callout.prototype = Object.extend(new AjaxJspTag.Base(), {

  initialize: function(url, options) {
    this.url = url;
    this.setOptions(options);
    this.setListeners();
    addAjaxListener(this);
  },
  reload: function () {
    this.setListeners();
  },

  setOptions: function(options) {
    this.options = Object.extend({
      parameters: options.parameters || '',
      overlib: options.overlib || AJAX_CALLOUT_OVERLIB_DEFAULT,
      parser: options.parser ? options.parser : new ResponseXmlToHtmlParser(),
      handler: options.handler ? options.handler : this.handler,
      doPost: options.doPost? true : false ,
      openEvent: options.openEvent ? options.openEvent : "mouseover",
      closeEvent: options.closeEvent ? options.closeEvent : "mouseout"
    }, options || {});
    this.calloutParameter = AJAX_DEFAULT_PARAMETER;
  },

  setListeners: function() {
    if (this.options.sourceClass) {
      var elemList = document.getElementsByClassName(this.options.sourceClass);
      for (var i=0; i<elemList.length; i++) {
        eval("elemList[i].on"+this.options.openEvent+" = this.calloutOpen.bindAsEventListener(this)");
        eval("elemList[i].on"+this.options.closeEvent+" = this.calloutClose.bindAsEventListener(this)");
      }
    }
  },

  calloutOpen: function(e) {
    this.execute(e);
  },

  calloutClose: function(e) {
    nd();
  },

  execute: function(e) {
    if (isFunction(this.options.preFunction)){ this.options.preFunction();}
	if (this.options.cancelExecution) {
	    this.cancelExecution = false;
	    return ;
      }
    // parse parameters and do replacements
    var ajaxParameters = this.options.parameters || '';
    var re = new RegExp("(\\{"+this.calloutParameter+"\\})", 'g');
    var elem = Event.element(e);
    if (elem.type) {
      ajaxParameters = ajaxParameters.replace(re, $F(elem));
    } else {
      ajaxParameters = ajaxParameters.replace(re, elem.innerHTML);
    }
    var params = buildParameterString(ajaxParameters);

    var obj = this; // required because 'this' conflict with Ajax.Request
    var aj = new Ajax.Request(this.url, {
      asynchronous: true,
      method: obj.options.doPost ? 'post':'get',
      evalScripts: true,
      parameters: params,
      onSuccess: function(request) {
        obj.options.parser.load(request);
        obj.options.handler(obj.options.parser.content, {title: obj.options.title,
                                                         overlib: obj.options.overlib});
      },
      onFailure: function(request) {
        if (isFunction(obj.options.errorFunction)){ obj.options.errorFunction();}
      },
      onComplete: function(request) {
        if (isFunction(obj.options.postFunction)){ obj.options.postFunction();}
      }
    });
  },

  handler: function(content, options) {
  if (content != "") { // #4
    if (options.overlib) {
      if (options.title) {
        return eval("overlib(content,CAPTION,options.title,"+options.overlib+")");
      } else {
        return eval("overlib(content,"+options.overlib+")");
      }
    } else {
      if (options.title) {
        return overlib(content,CAPTION,options.title);
      } else {
        return overlib(content);
      }
    }
  }
 }
});


/**
 * Copyright 2005 Darren L. Spurgeon
 * Copyright 2007 Jens Kapitza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * Global Variables
 */
AJAX_DEFAULT_PARAMETER = "ajaxParameter";
AJAX_PORTLET_MAX = 1;
AJAX_PORTLET_MIN = 2;
AJAX_PORTLET_CLOSE = 3;
AJAX_CALLOUT_OVERLIB_DEFAULT = "STICKY,CLOSECLICK,DELAY,250,TIMEOUT,5000,VAUTO,WRAPMAX,240,CSSCLASS,FGCLASS,'olfg',BGCLASS,'olbg',CGCLASS,'olcg',CAPTIONFONTCLASS,'olcap',CLOSEFONTCLASS,'olclo',TEXTFONTCLASS,'oltxt'";


/**
 * Type Detection
 */
function isAlien(a) {
  return isObject(a) && typeof a.constructor != 'function';
}

function isArray(a) {
  return isObject(a) && a.constructor == Array;
}

function isBoolean(a) {
  return typeof a == 'boolean';
}

function isEmpty(o) {
  var i, v;
  if (isObject(o)) {
    for (i in o) {
      v = o[i];
      if (isUndefined(v) && isFunction(v)) {
        return false;
      }
    }
  }
  return true;
}

function isFunction(a) {
  return typeof a == 'function';
}

function isNull(a) {
  return typeof a == 'object' && !a;
}

function isNumber(a) {
  return typeof a == 'number' && isFinite(a);
}

function isObject(a) {
  return (a && typeof a == 'object') || isFunction(a);
}

function isString(a) {
  return typeof a == 'string';
}

function isUndefined(a) {
  return typeof a == 'undefined';
}


/**
 * Utility Functions
 */

function addOnLoadEvent(func) {
  var oldonload = window.onload;
  if (isFunction(func)) {
  	if (!isFunction(oldonload)) {
  	  window.onload = func;
  	} else {
  	  window.onload = function() {
  	    oldonload();
  	    func();
  	  };
  	}
  } else {
  	if (isObject(func) && isFunction(func.onload)) {
  		// callback event?
  	  window.onload = function() {
  	   if (isFunction(oldonload)) {
  		 oldonload();
  	   }
  	   // onload des objektes aufrufen
  	   func.onload();
  	  };
  	}
  }
}

/*
 * Extract querystring from a URL
 */
function extractQueryString(url) {
  return ( (url.indexOf('?') >= 0) && (url.indexOf('?') < (url.length-1))) ? url.substr(url.indexOf('?')+1): '';
}

/*
 * Trim the querystring from a URL
 */
function trimQueryString(url) {
  return (url.indexOf('?') >= 0) ? url.substring(0, url.indexOf('?')) : url;
}

function delimitQueryString(qs) {
  var ret = '';
  var params = "";
  if (qs.length > 0) {
	params = qs.split('&');
    for (i=0; i<params.length; i++) {
      if (i > 0)
      {
      	ret += ',';
      }
      ret += params[i];
    }
  }
  return ret;
}

function trim(str) {
	return str.replace(/^\s*/,"").replace(/\s*$/,"");
}

// encode , =
function buildParameterString(parameterList) {
  var returnString = '';
  var params = (parameterList || '').split(',');
  if (params !== null) {
    for (p=0; p<params.length; p++) {
     	pair = params[p].split('=');
       	key = trim(pair[0]); // trim string no spaces allowed in key a, b should work
       	val = pair[1];
      // if val is not null and it contains a match for a variable, then proceed
      if (!isEmpty(val) || isString(val)) {
        	varList = val.match( new RegExp("\\{[\\w\\.\\(\\)\\[\\]]*\\}", 'g') );
        if (!isNull(varList)) {
          	field = $(varList[0].substring(1, varList[0].length-1));
          switch (field.type) {
            case 'checkbox':
            case 'radio':
            case 'text':
            case 'textarea':
            case 'password':
            case 'hidden':
            case 'select-one':
              returnString += '&' + key + '=' + encodeURIComponent(field.value);
              break;
            case 'select-multiple':
              fieldValue = $F(varList[0].substring(1, varList[0].length-1));
              for (i=0; i<fieldValue.length; i++) {
                returnString += '&' + key + '=' + encodeURIComponent(fieldValue[i]);
              }
              break;
            default:
              returnString += '&' + key + '=' + encodeURIComponent(field.innerHTML);
              break;
          }
        } else {
          // just add back the pair
          returnString += '&' + key + '=' + encodeURIComponent(val);
        }
      }
    }
  }

  if (returnString.charAt(0) == '&') {
    returnString = returnString.substr(1);
  }
  return returnString;
}

function evalBoolean(value, defaultValue) {
  if (!isNull(value) && isString(value)) {
    return (parseBoolean(value)) ? "true" : "false";
  } else {
    return defaultValue === true ? "true" : "false";
  }
}
function parseBoolean(value) {
  if (!isNull(value) && isString(value)) {
    return ( "true" == value.toLowerCase() || "yes" == value.toLowerCase());
  } else {
  	if (isBoolean(value)) {return value; }
    return false;
  }
}

// read function parameterstring
function evalJScriptParameters(paramString) {
	if (isNull(paramString) || !isString(paramString))
	{
		return null;
	}
	return eval("new Array("+paramString+")");
}

// listener wieder anhaengen fuer TREE tag wird von htmlcontent benutzt
function reloadAjaxListeners(){
	for (i=0; i < this.ajaxListeners.length; i++){
		if ( isFunction(this.ajaxListeners[i].reload) ) {
			this.ajaxListeners[i].reload();
		}
	}
}

function addAjaxListener(obj) {
	if (!this.ajaxListeners) {
		this.ajaxListeners = new Array(obj);
	} else {
		this.ajaxListeners.push(obj);
	}
}

/* ---------------------------------------------------------------------- */
/* Example File From "_JavaScript and DHTML Cookbook"
   Published by O'Reilly & Associates
   Copyright 2003 Danny Goodman
*/

// http://jslint.com/
// Missing radix parameter -- setDate setHours setMinutes

// utility function to retrieve a future expiration date in proper format;
// pass three integer parameters for the number of days, hours,
// and minutes from now you want the cookie to expire; all three
// parameters required, so use zeros where appropriate
function getExpDate(days, hours, minutes) {
  var expDate = new Date();
  if (typeof days == "number" && typeof hours == "number" && typeof hours == "number") {
    expDate.setDate(expDate.getDate() + parseInt(days));
    expDate.setHours(expDate.getHours() + parseInt(hours));
    expDate.setMinutes(expDate.getMinutes() + parseInt(minutes));
    return expDate.toGMTString();
  }
}

// utility function called by getCookie()
function getCookieVal(offset) {
  var endstr = document.cookie.indexOf (";", offset);
  if (endstr == -1) {
    endstr = document.cookie.length;
  }
  return unescape(document.cookie.substring(offset, endstr));
}

// primary function to retrieve cookie by name
function getCookie(name) {
  var arg = name + "=";
  var alen = arg.length;
  var clen = document.cookie.length;
  var i = 0;
  var j;
  while (i < clen) {
    j = i + alen;
    if (document.cookie.substring(i, j) == arg) {
      return getCookieVal(j);
    }
    i = document.cookie.indexOf(" ", i) + 1;
    if (i == 0) {
    	break;
    }
  }
  return null;
}

// store cookie value with optional details as needed
function setCookie(name, value, expires, path, domain, secure) {
  document.cookie = name + "=" + escape (value) +
    ((expires) ? "; expires=" + expires : "") +
    ((path) ? "; path=" + path : "") +
    ((domain) ? "; domain=" + domain : "") +
    ((secure) ? "; secure" : "");
}

// remove the cookie by setting ancient expiration date
function deleteCookie(name,path,domain) {
  if (getCookie(name)) {
    document.cookie = name + "=" +
      ((path) ? "; path=" + path : "") +
      ((domain) ? "; domain=" + domain : "") +
      "; expires=Thu, 01-Jan-70 00:00:01 GMT";
  }
}
/* ---------------------------------------------------------------------- */
/* End Copyright 2003 Danny Goodman */


/**
 * Copyright 2005 Darren L. Spurgeon
 * Copyright 2007 Jens Kapitza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
* Response Parsers
*/
var AbstractResponseParser = function () {
	this.getArray = function () {
		return null;
	};
};


/**
* parser to work easy with xml response
* create  to make them known
*/
var DefaultResponseParser = Class.create();
var ResponseTextParser = Class.create();
var ResponseXmlParser = Class.create();
var ResponseHtmlParser = Class.create();
var ResponseXmlToHtmlParser = Class.create();
var ResponseCallBackXmlParser = Class.create();
var ResponsePlainTextXmlToHtmlParser = Class.create();
var ResponseXmlToHtmlListParser = Class.create();
var ResponseXmlToHtmlLinkListParser = Class.create();

DefaultResponseParser.prototype = Object.extend(new AbstractResponseParser(), {
  initialize: function() {
    this.type = "xml";
  },
  getArray: function () {
    return this.itemList;
  },
  load: function(request) {
    this.content = request.responseXML;
    this.parse();
    this.prepareData( this.itemList);
  },
  // format <name><value><value><value>....<value>
  prepareData: function( dataarray ) {},

  parse: function() {
    root = this.content.documentElement;
    responseNodes = root.getElementsByTagName("response");
    this.itemList = [];
    if (responseNodes.length > 0) {
      responseNode = responseNodes[0];
      itemNodes = responseNode.getElementsByTagName("item");
      for (i=0; i<itemNodes.length; i++) {
        nameNodes = itemNodes[i].getElementsByTagName("name");
        valueNodes = itemNodes[i].getElementsByTagName("value");
        if (nameNodes.length > 0 && valueNodes.length > 0) {
          name = nameNodes[0].firstChild ? nameNodes[0].firstChild.nodeValue : "";
          myData = [];
          myData.push(name);
            for (j=0; j <valueNodes.length; j++) {
              value = valueNodes[j].firstChild ? valueNodes[j].firstChild.nodeValue: "";
       		  myData.push(value);
            }
          this.itemList.push(myData);
        }
      }
    }
  }
});


ResponseTextParser.prototype = Object.extend(new AbstractResponseParser(), {
  initialize: function() {
    this.type = "text";
  },

  load: function(request) {
    this.content = request.responseText;
    this.split();
  },

  split: function() {
    this.itemList = [];
    var lines = this.content.split('\n');
    for (i=0; i<lines.length; i++) {
      this.itemList.push(lines[i].split(','));
    }
  }
});

ResponseXmlParser.prototype = Object.extend(new DefaultResponseParser(), {
  prepareData: function(request,dataarray) {
  }
});


ResponseHtmlParser.prototype = Object.extend(new AbstractResponseParser(), {
  initialize: function() {
    this.type = "html";
  },

  load: function(request) {
    this.content = request.responseText;
  }
});

ResponseXmlToHtmlParser.prototype = Object.extend(new DefaultResponseParser(), {
  initialize: function() {
    this.type = "xmltohtml";
  	this.plaintext = false;
  },
  prepareData: function( dataarray) {
   this.contentdiv = document.createElement("div");

   for (i=0; i < dataarray.length; i++)
   {
     h1 =  document.createElement("h1");
     if (!this.plaintext) {
       h1.innerHTML += dataarray[i][0];
     } else {
       h1.appendChild(document.createTextNode(dataarray[i][0]));
     }
     this.contentdiv.appendChild(h1);
     for (j=1; j < dataarray[i].length; j++) {
       div =  document.createElement("div");
       if (!this.plaintext) {
         div.innerHTML += dataarray[i][j];
       } else {
         div.appendChild(document.createTextNode(dataarray[i][j]));
       }
       this.contentdiv.appendChild(div);
     }
   }
   //#4
   if (dataarray.length >= 1) {
   	this.content =  this.contentdiv.innerHTML;
   }
	else {
	   this.content = ""; // keine daten dann ''
	}
   // skip plz


  }
});

// server callback

ResponseCallBackXmlParser.prototype = Object.extend(new DefaultResponseParser(), {
  initialize: function() {
    this.type = "xml";
  },
  prepareData: function( dataarray) {
   this.items = [];

   for (i=0; i < dataarray.length; i++)
   {
	this.items.push( [ dataarray[i][0],dataarray[i][1],(dataarray[i][2] ? true : false) ] );
   }
  }
});



ResponsePlainTextXmlToHtmlParser.prototype = Object.extend(new ResponseXmlToHtmlParser(), {
  initialize: function() {
    this.type = "xmltohtml";
  	this.plaintext = true;
  }
});



ResponseXmlToHtmlListParser.prototype = Object.extend(new DefaultResponseParser(), {
  initialize: function() {
    this.type = "xmltohtmllist";
    this.plaintext =  true;
  },


  prepareData: function( dataarray) {
    this.contentdiv = document.createElement("div");
    ul = document.createElement("ul");
    for (i=0; i < dataarray.length; i++)
    {
      liElement = document.createElement("li");
      liElement.id=dataarray[i][1];
      if (this.plaintext) {
        liElement.appendChild(document.createTextNode(dataarray[i][0]));
      } else {
        liElement.innerHTML = dataarray[i][0];
      }
      ul.appendChild(liElement);
    }
    this.contentdiv.appendChild(ul);
    this.content = this.contentdiv.innerHTML;
  }
});

ResponseXmlToHtmlLinkListParser.prototype = Object.extend(new AbstractResponseParser(), {
  initialize: function() {
    this.type = "xmltohtmllinklist";
  },

  load: function(request) {
    this.xml = request.responseXML;
    this.collapsedClass = request.collapsedClass;
    this.treeClass = request.treeClass;
    this.nodeClass = request.nodeClass;
    this.expandedNodes = [];
    this.parse();
  },

  parse: function() {
    var ul = document.createElement('ul');
    ul.className = this.treeClass;
    var root = this.xml.documentElement;

    var responseNodes = root.getElementsByTagName("response");
    if (responseNodes.length > 0) {
      responseNode = responseNodes[0];
      itemNodes = responseNode.getElementsByTagName("item");

      if (itemNodes.length === 0) {
      	ul = null;
      }
      for (i=0; i<itemNodes.length; i++) {
       	nameNodes = itemNodes[i].getElementsByTagName("name");
        valueNodes = itemNodes[i].getElementsByTagName("value");


        urlNodes = itemNodes[i].getElementsByTagName("url");
        collapsedNodes = itemNodes[i].getElementsByTagName("collapsed");

        leafnodes = itemNodes[i].getElementsByTagName("leaf");

        if (nameNodes.length > 0 && valueNodes.length > 0) {
          name = nameNodes[0].firstChild.nodeValue;
          value = valueNodes[0].firstChild.nodeValue;
          url = "#";
          try {
          	url = urlNodes[0].firstChild.nodeValue;
          } catch (ex) {
          // default url is link
          }
          leaf = false;
          try {
          	leaf = leafnodes[0].firstChild.nodeValue;
          } catch (ex) {
          // no leaf flag found
          }

          collapsed =  false;
          try {
	         collapsed = parseBoolean(collapsedNodes[0].firstChild.nodeValue);
            } catch (ex) {
          // it is not collapsed as default
          }

          li = document.createElement('li');
          li.id = "li_" + value;
          ul.appendChild(li);

          if (!parseBoolean(leaf))
          {
          	span = document.createElement('span');
          	li.appendChild(span);
          	// img geht im IE nicht
          	span.id = "span_" + value;
          	span.className = this.collapsedClass;
		  }

          link  = document.createElement('a');
          li.appendChild(link);
          link.href = url;
          link.className = this.nodeClass;
          link.appendChild(document.createTextNode(name));

          div = document.createElement('div');
          li.appendChild(div);
          div.id = value;
          div.setAttribute("style","");
          div.style.display ="none";

          if(!collapsed) {
            this.expandedNodes.push(value);
          }
        }
      }
    }
    this.content = ul;
  }
});
