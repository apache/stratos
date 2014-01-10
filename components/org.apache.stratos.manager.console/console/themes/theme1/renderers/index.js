var render = function (theme, data, meta, require) {
    // Re-create the data structure of the cartridges.
    /* var mycartridges = [
     {
     kind: "cartridges",
     cartridges: []}
     ];
     var cartridgesToPush;
     for(var i=0;i<data.mycartridges.length;i++){
     if(data.mycartridges[i].category == undefined){
     cartridgesToPush = null;
     for(var j=0;j<mycartridges.length;j++){
     if(mycartridges[j].kind == "cartridges" ){
     cartridgesToPush = mycartridges[j].cartridges;
     }
     }
     cartridgesToPush.push(data.mycartridges[i]);
     }else{
     cartridgesToPush = null;
     for (var j = 0; j < mycartridges.length; j++) {
     if (mycartridges[j].kind == data.mycartridges[i].category) {
     cartridgesToPush = mycartridges[j].cartridges;
     }
     }
     if(cartridgesToPush == null){
     mycartridges.push({kind:data.mycartridges[i].category,cartridges:[data.mycartridges[i]]})
     }else{
     cartridgesToPush.push(data.mycartridges[i]);
     }
     }
     }*/

    theme('index', {
        body: [
            {
                partial: 'mycartridges',
                context: {
                    title: 'My Cartridges',
                    mycartridges: {}
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
};