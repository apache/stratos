var send = function(path) {
    var mime,
        file = new File(path);
    if(!file.isExists()) {
        response.sendError(404, 'Request resource not found');
        return;
    }
    mime = require('/modules/mime.js');
    response.addHeader('Content-Type', mime.getType(path));
    file.open('r');
    print(file.getStream());
    file.close();
};