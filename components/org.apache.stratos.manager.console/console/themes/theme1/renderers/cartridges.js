var render = function (theme, data, meta, require) {
    // Re-create the data structure of the cartridges.
    var log = new Log();
    var cartridges_old = data.cartridges.cartridge;
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
            log.info(cartridges_old[i]);
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
    log.info(cartridges_new);
    theme('index', {
        body: [
            {
                partial: 'cartridges',
                context: {
                    title:'Cartridges',
                    cartridges:cartridges_new
                }
            }
        ],
        header: [
            {
                partial: 'header',
                context:{
                    title:'Cartridges',
                    my_cartridges:true,
                    button:{
                        link:'/',
                        name:'Back To My Cartridges',
                        class_name:"btn-default",
                        class_icon: 'icon-arrow-left'
                    },
                    has_help:true,
                    help:'Create cartridges like PHP, Python, Ruby etc.. Or create data cartridges with mySql, PostgreSQL. Directly install applications like Drupal, Wordpress etc..'
                }
            }
        ],
        title:[
            {
                partial:'title',
                context:{
                    title:"My Cartridges"
                }
            }
        ]
    });
};