var server = {};

(function (server) {

    var USER = 'server.user';

    var log = new Log();
    
    /**
     * Returns the currently logged in user
    */
    server.current = function (session, user) {
        if (arguments.length > 1) {
            session.put(USER, user);
            return user;
        }
        return session.get(USER);
    };
    
}(server));
