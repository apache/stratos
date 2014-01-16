var render = function (theme, data, meta, require) {
    for(var i=0;i<data.step_data.length;i++){
        data.step_data[i].key = data.step_data[i].name.replace(/ /g,'');
    }
    var title;
    var wizard_on_val = [];
    for(var i=0; i<5 ;i++){
        if(i <= data.wizard.step-1){
            wizard_on_val.push(true);
        }else{
            wizard_on_val.push(false);
        }
    }
    var config_status = data.wizard;
    if( config_status.step == 1 ){
        title = 'Partition Deployment';
    }else if( config_status.step == 2 ){
        title = 'Policy Deployment';
    }else if( config_status.step == 3 ){
        title = 'Lb';
    }else if( config_status.step == 4 ){
        title = 'Cartridge Deployment';
    }else if( config_status.step == 5 ){
        title = 'Multi-Tenant Service Deployment';
    }
    theme('index', {
        body: [
            {
                partial: 'configure_stratos_wizard',
                context: {
                    title:title,
                    step_data:data.step_data,
                    step:config_status.step,
                    wizard_on_1:wizard_on_val[0],
                    wizard_on_2:wizard_on_val[1],
                    wizard_on_3:wizard_on_val[2],
                    wizard_on_4:wizard_on_val[3],
                    wizard_on_5:wizard_on_val[4]
                }
            }
        ],
        header: [
            {
                partial: 'header',
                context:{
                    title:'Configure Stratos',
                    button:{
                        link:'/',
                        name:'Deploy New Cartridge',
                        class_name:''
                    },
                    has_help:false,
                    step_data:true,
                    config_status:data.config_status,
                    wizard_on:true,
                    wizard_on_1:wizard_on_val[0],
                    wizard_on_2:wizard_on_val[1],
                    wizard_on_3:wizard_on_val[2],
                    wizard_on_4:wizard_on_val[3],
                    wizard_on_5:wizard_on_val[4],
                    step:step,
                    configure_stratos:true
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
};