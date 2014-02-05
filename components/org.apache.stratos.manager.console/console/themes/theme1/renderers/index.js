var render = function (theme, data, meta, require) {
      // Re-create the data structure of the cartridges.

    var cartridges_old = data.mycartridges.cartridge;
    var cartridges_new = [
        {
            kind: "Cartridges",
            cartridges: []}
    ];
    var cartridgesToPush;
    for (var i = 0; i < cartridges_old.length; i++) {
        if (cartridges_old[i].category == undefined) {
            cartridgesToPush = null;
            for (var j = 0; j < cartridges_new.length; j++) {
                if (cartridges_new[j].kind == "Cartridges") {
                    cartridgesToPush = cartridges_new[j].cartridges;
                }
            }
            cartridgesToPush.push(cartridges_old[i]);
            var log = new Log("index.js");
            log.info("cartridges old : "+ stringify(cartridges_old) );
            log.info("cartridges new : "+ stringify(cartridges_new) );
            log.info("cartridges to push : "+ stringify(cartridgesToPush));
        } else {
            cartridgesToPush = null;
            for (var j = 0; j < cartridges_new.length; j++) {
                if (cartridges_new[j].kind == cartridges_old[i].category) {
                    cartridgesToPush = cartridges_new[j].cartridges;
                }
            }
            if (cartridgesToPush == null) {
                cartridges_new.push({kind: cartridges_old[i].category, cartridges: [cartridges_old[i]]})
            } else {
                cartridgesToPush.push(cartridges_old[i]);
            }
        }
    }
    var log = new Log();
    log.info("permission object : "+meta.request.permissions);

    theme('index', {
        body: [
            {
                partial: 'mycartridges',
                context: {
                    title: 'My Cartridges',
                    mycartridges: cartridges_new
                }
            }
        ],
        header: [
            {
                partial: 'header',
                context: {
                    title: 'My Cartridges',
                    my_cartridges: true,
                    button: {
                        link: '/cartridges.jag',
                        name: 'Subscribe to Cartridge',
                        class_name: 'btn-important'
                    },
                    has_help: true,
                    help: 'Create cartridges like PHP, Python, Ruby etc.. Or create data cartridges with mySql, PostgreSQL. Directly install applications like Drupal, Wordpress etc..'
                }
            }
        ],
        title: [
            {
                partial: 'title',
                context: {
                    title: "My Cartridges"
                }
            }
        ]
    });
};