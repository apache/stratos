var render = function (theme, data, meta, require) {
      // Re-create the data structure of the cartridges.

    theme('plain', {
        body: [
            {
                partial: '404',
                context: {
                    title: 'My Cartridges'
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