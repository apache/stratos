var getType = function (path) {
    var index = path.lastIndexOf('.');
    var ext = index < path.length ? path.substring(index + 1) : '';
    switch (ext) {
        case 'js':
            return 'application/javascript';
        case 'css':
            return 'text/css';
        case 'html':
            return 'text/html';
        case 'png':
            return 'image/png';
        case 'gif':
            return 'image/gif';
        case 'jpeg':
            return 'image/jpeg';
        case 'jpg':
            return 'image/jpg';
        default :
            return 'text/plain';
    }
};