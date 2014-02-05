var caramel = require('caramel');

caramel.configs({
    context: '/console',
    cache: true,
    negotiation: true,
    themer: function () {
        return 'theme1';
    }/*,
     languagesDir: '/i18n',
     language: function() {
     return 'si';
     }*/
});
