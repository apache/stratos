//var engine = require('caramel').engine('handlebars');

var engine = require('caramel').engine('handlebars', (function () {
    return {
        partials: function (Handlebars) {
            Handlebars.registerHelper("ifCon", function(conditional, options) {
              if (options.hash.desired === options.hash.type) {
                options.fn(this);
              } else {
                options.inverse(this);
              }
            });
            /*
            Handlebars.registerHelper('ifCond', function (v1, operator, v2, options) {

                switch (operator) {
                    case '==':
                        return (v1 == v2) ? options.fn(this) : options.inverse(this);
                    case '===':
                        return (v1 === v2) ? options.fn(this) : options.inverse(this);
                    case '<':
                        return (v1 < v2) ? options.fn(this) : options.inverse(this);
                    case '<=':
                        return (v1 <= v2) ? options.fn(this) : options.inverse(this);
                    case '>':
                        return (v1 > v2) ? options.fn(this) : options.inverse(this);
                    case '>=':
                        return (v1 >= v2) ? options.fn(this) : options.inverse(this);
                    default:
                        return options.inverse(this);
                }
            });
            */
        }
    }
}));