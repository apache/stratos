var render = function (theme, data, meta, require) {
    if(data.error.length == 0 ){
        theme('index', {
            body: [
                {
                    partial: 'cartridge_info',
                    context: {
                        title:'Cartridges',
                        cartridgeInfo:data.cartridgeInfo.cartridge,
                        lbclusterinfo:data.lbCluster.cluster,
                        clusterinfo:data.clusterInfo.cluster,
                        host:data.cartridgeInfo.cartridge.hostName
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