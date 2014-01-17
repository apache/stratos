var render = function (theme, data, meta, require) {
    theme('index', {
        body: [
            {
                partial: 'subscribe_cartridge',
                context: {
                    title:data.name,
                    autoScalePolicies:data.autoScalePolicies,
                    deploymentPolicies:data.deploymentPolicies,
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
                    title:"Subscribe Cartridge -" + data.name
                }
            }
        ]
    });
};