var render = function (theme, data, meta, require) {
    var hasError = true;
    if(data.error == ""){
        hasError = false;
    }
    theme('index', {
        body: [
            {
                partial: 'login',
                context: {
                    error:data.error,
                    hasError:hasError
                }
            }
        ],
        header: [
            {
                partial: 'header',
                context:{
                    title:'Login',
                    login:true,
                    breadcrumb:[
                                {link:'/', name:'Login',isLink:false}
                            ]
                }
            }
        ],
        title: [
            {
                partial: 'title',
                context: {
                    title: "Apache Stratos Login"
                }
            }
        ]
    });
};