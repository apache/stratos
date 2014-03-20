var render = function (theme, data, meta, require) {
    if(data.error.length == 0 ){
        var cartridge = data.cartridge.cartridge;
        if(cartridge == undefined){
            cartridge = data.cartridge;
        }
        theme('index', {
            body: [
                {
                    partial: 'subscribe_cartridge',
                    context: {
                        autoScalePolicies:data.autoScalePolicies.autoscalePolicy,
                        deploymentPolicies:data.deploymentPolicies.deploymentPolicy,
                        cartridge:cartridge,
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
                        title:"Subscribe Cartridge -" + cartridge.cartridgeType + " " + cartridge.version + " Cartridge",
                        cartridge:cartridge
                    }
                }
            ]
        });
    }else{
        theme('index', {
            body: [
                {
                    partial: 'error_page',
                    context: {
                        title:'Error',
                        error:data.error
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
    }
};