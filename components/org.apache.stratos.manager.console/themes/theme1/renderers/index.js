var render = function (theme, data, meta, require) {
    theme('index', {
        body: [
            {
                partial: 'mycartridges',
                context: {
                    title:'My Cartridges',
                    mycartridges:data.mycartridges
                }
            }
        ],
        header: [
            {
                partial: 'header',
                context:{
                    title:'My Cartridges',
                    my_cartridges:true,
                    link:'/cartridges.jag',
                    name:'Subscribe to Cartridge',
                    css:"btn-important"
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