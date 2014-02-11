var render = function (theme, data, meta, require) {
    theme('index', {
        body: [
            {
                partial: 'subscribe_cartridge',
                context: {
                    autoScalePolicies:data.autoScalePolicies.autoscalePolicy,
                    deploymentPolicies:data.deploymentPolicies.deploymentPolicy,
                    cartridge:data.cartridge.cartridge,
                    cartridgeType:meta.request.getParameter('cartridgeType')
                }
            }
        ],
        header: [
                    {
                        partial: 'header',
                        context:{
                            title:'Subscribe Cartridge',
                            my_cartridges:true,
                            button:{
                                link: '/cartridges.jag',
                                name: 'Select different Cartridge',
                                class_name: "btn-default",
                                class_icon: "icon-arrow-left"
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
                    title:"Subscribe Cartridge -" + data.cartridge.cartridge.cartridgeType + " " + data.cartridge.cartridge.version + " Cartridge",
                    cartridge:data.cartridge.cartridge,
                }
            }
        ]
    });

    var log = new Log();
    log.info("jssssss...: " + stringify(data.cartridge.cartridge));
};