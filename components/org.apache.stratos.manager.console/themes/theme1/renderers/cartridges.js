var render = function (theme, data, meta, require) {
    theme('index', {
        body: [
            {
                partial: 'cartridges',
                context: {
                    title:'Cartridges',
                    cartridges:data.cartridges
                }
            }
        ],
        header: [
            {
                partial: 'header',
                context:{
                    title:'Cartridges',
                    my_cartridges:true,
                    link:'/',
                    name:'Back To My Cartridges',
                    css:"btn-back"
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