var render = function (theme, data, meta, require) {
    theme('index', {
        body: [
            {
                partial: 'account_recovery',
                context: data.profile
            }
        ],
        header: [
            {
                partial: 'header',
                context:{
                    title:'Account Recovery',
                    accountRecovery:true,
                    breadcrumb:[
                        {link:'/', name:'Home',isLink:true},
                        {link:'', name:'Account Recovery',isLink:false}
                    ]
                }
            }
        ]
    });
};