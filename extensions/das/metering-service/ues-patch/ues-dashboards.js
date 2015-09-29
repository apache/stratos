(function () {

    var findPlugin = function (type) {
        var plugin = ues.plugins.components[type];
        if (!plugin) {
            throw 'ues dashboard plugin for ' + type + ' cannot be found';
        }
        return plugin;
    };

    var createComponent = function (container, component, done) {
        var type = component.content.type;
        var plugin = findPlugin(type);
        var sandbox = $('<div id="' + component.id + '" class="ues-component"></div>');
        sandbox.appendTo(container);
        plugin.create(sandbox, component, ues.hub, done);
    };

    var updateComponent = function (component, done) {
        var plugin = findPlugin(component.content.type);
        var container = $('#' + component.id);
        plugin.update(container, component, ues.hub, done);
    };

    var destroyComponent = function (component, done) {
        var plugin = findPlugin(component.content.type);
        var container = $('#' + component.id);
        plugin.destroy(container, component, ues.hub, done);
    };

    var componentId = function (clientId) {
        return clientId.split('-').pop();
    };

    var wirings;

    var publishForClient = ues.hub.publishForClient;
    ues.hub.publishForClient = function (container, topic, data) {
        console.log('publishing data container:%s, topic:%s, data:%j', container.getClientID(), topic, data);
        var clientId = componentId(container.getClientID());
        var channels = wirings[clientId + '.' + topic];
        if (!channels) {
            return;
        }
        channels.forEach(function (channel) {
            publishForClient.apply(ues.hub, [container, channel, data]);
        });
    };


    var publish = ues.hub.publish;
    ues.hub.publish = function (topic, data){
        $(".container").find('.ues-component').each(function () {
            var id = $(this).attr('id');
            var channel = id + "." + topic;
            publish.apply(ues.hub, [channel, data]);
        });
    }

    var wires = function (page) {
        var content = page.content;
        var area;
        var blocks;
        var wirez = {};

        var wire = function (wirez, id, listeners) {
            var event;
            var listener;
            for (event in listeners) {
                if (listeners.hasOwnProperty(event)) {
                    listener = listeners[event];
                    if (!listener.on) {
                        continue;
                    }
                    listener.on.forEach(function (notifier) {
                        var channel = notifier.from + '.' + notifier.event;
                        var wire = wirez[channel] || (wirez[channel] = []);
                        wire.push(id + '.' + event);
                    });
                }
            }
        };

        for (area in content) {
            if (content.hasOwnProperty(area)) {
                blocks = content[area];
                blocks.forEach(function (block) {
                    var listeners = block.content.listen;
                    if (!listeners) {
                        return;
                    }
                    wire(wirez, block.id, listeners);
                });
            }
        }
        console.log(wirez);
        return wirez;
    };

    var setDocumentTitle = function (dashboard, page) {
        document.title = dashboard.title + ' | ' + page.title;
    };

    var renderPage = function (element, dashboard, page, done) {
        setDocumentTitle(dashboard, page);
        wirings = wires(page);
        var container;
        var area;
        var layout = $(page.layout.content);
        var content = page.content;
        element.html(layout);
        for (area in content) {
            if (content.hasOwnProperty(area)) {
                container = $('#' + area, layout);
                content[area].forEach(function (options) {
                    createComponent(container, options, function (err) {
                        if (err) {
                            console.error(err);
                        }
                    });
                });
            }
        }
        if (!done) {
            return;
        }
        done();
    };

    var findPage = function (dashboard, id) {
        var i;
        var page;
        var pages = dashboard.pages;
        var length = pages.length;
        for (i = 0; i < length; i++) {
            page = pages[i];
            if (page.id === id) {
                return page;
            }
        }
    };

    var renderDashboard = function (element, dashboard, name, done) {
        name = name || dashboard.landing;
        var page = findPage(dashboard, name);
        if (!page) {
            throw 'requested page : ' + name + ' cannot be found';
        }
        renderPage(element, dashboard, page, done);
    };

    var rewireDashboard = function (page) {
        wirings = wires(page);
    };

    var resolveURI = function (uri) {
        var index = uri.indexOf('://');
        var scheme = uri.substring(0, index);
        var uriPlugin = ues.plugins.uris[scheme];
        if (!uriPlugin) {
            return uri;
        }
        var path = uri.substring(index + 3);
        return uriPlugin(path);
    };

    ues.components = {
        create: createComponent,
        update: updateComponent,
        destroy: destroyComponent
    };

    ues.dashboards = {
        render: renderDashboard,
        rewire: rewireDashboard,
        findPage: findPage,
        resolveURI: resolveURI
    };

    ues.assets = {};

    ues.plugins = {
        assets: {},
        components: {},
        uris: {}
    };

}());