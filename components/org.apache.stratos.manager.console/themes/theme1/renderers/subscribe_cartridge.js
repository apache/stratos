var render = function (theme, data, meta, require) {
    theme('index', {
        body: [
            {
                partial: 'subscribe_cartridge',
                context: {
                    title:data.name
                }
            }
        ],
        header: [
            {
                partial: 'header',
                context:{
                    title:'Subscribe Cartridge',
                    my_cartridges:true,
                    link: '/cartridges.jag',
                    name: 'Select different Cartridge',
                    css: "btn-back"
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