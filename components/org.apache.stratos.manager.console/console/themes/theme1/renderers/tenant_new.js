var render = function (theme, data, meta, require) {

    var create_btn_class = 'btn-default';
    var title = 'Tenant Management - Add New Tenant';
    if(data.error.length == 0 ){
        theme('index', {
            body: [
                {
                    partial: 'tenant_new',
                    context: {
                        title:title,
                        tenants:data.tenants
                    }
                }
            ],
            header: [
                {
                    partial: 'header',
                    context:{
                        title:title,
                        button:{
                            link:'/tenant_management.jag',
                            name:'Tenant Management',
                            class_name:create_btn_class,
                            class_icon:'fa fa-arrow-left'
                        },
                        has_help:true,
                        help:"Tenants you create has permission to view and subscribe to Cartridges. Tenants don't have permission to do Partition deployment, Policy deployment, LB Creation, and MT service deployment.",
                        tenant_mgt:true
                    }
                }
            ],
            title:[
                {
                    partial:'title',
                    context:{
                        title:title
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