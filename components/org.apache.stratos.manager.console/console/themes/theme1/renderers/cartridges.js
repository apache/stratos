var render = function (theme, data, meta, require) {
    // Re-create the data structure of the cartridges.
    var log = new Log();
    var cartridges_old = data.cartridges;
    var lb_cartridges_old = data.availableLbCartridges;
    var cartridges_new = [
        {
            kind: "Framework",
            cartridges: []}
    ];
    var cartridgesToPush;
    for (var i = 0; i < cartridges_old.length; i++) {
        if (cartridges_old[i].provider == undefined || (cartridges_old[i].provider.toLowerCase() != "application" && cartridges_old[i].provider.toLowerCase() != "data" )) {
            cartridgesToPush = null;
            for (var j = 0; j < cartridges_new.length; j++) {
                if (cartridges_new[j].kind == "Framework") {
                    cartridgesToPush = cartridges_new[j].cartridges;
                }
            }
            cartridgesToPush.push(cartridges_old[i]);
        } else {
            cartridgesToPush = null;
            for (var j = 0; j < cartridges_new.length; j++) {
                if (cartridges_new[j].kind == cartridges_old[i].provider) {
                    cartridgesToPush = cartridges_new[j].cartridges;
                }
            }
            if (cartridgesToPush == null) {
                var kind = cartridges_old[i].provider;
                cartridges_new.push({kind: cartridges_old[i].provider, cartridges: [cartridges_old[i]]})
            } else {
                cartridgesToPush.push(cartridges_old[i]);
            }
        }
    }
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