class RestService

    constructor: (@$http, @$q, @$log, @AuthProvider) ->
        @$log.debug "Constructing RestService..."

    get: (config) ->
        @call('GET', config)

    post: (config) ->
        @call('POST', config)

    put: (config) ->
        @call('PUT', config)

    delete: (config) ->
        @call('DELETE', config)

    call: (method, config) ->
        @authenticate(config)
        .then(
            (config) =>
                if config.data?
                    config.data = _.pick config.data, (value, key)->
                        value?
                if !config.toJSON?
                    config.toJSON = false
                @sendRequest(method, config.path.substring(1), config.params, config.data, config.toJSON, config.responseType);
            )

    authenticate: (config) ->
        deferred = @$q.defer()
        if config.authenticate
            if @AuthProvider.isAuthenticated
                deferred.resolve(config)
            else
                #TODO: show popup to user so that he can enter his credentials
        else
            deferred.resolve(config)

        deferred.promise

    sendRequest: (_method, _url, _params, _data, _toJSON, _responseType) ->
        options = @configureOptions(_method, _url, _params, _data, _toJSON, _responseType)
        @$http(options)
        .then(
            (result) =>
                result.data
            )

    refreshTokens: () ->
        "" #TODO: check if access token expired and refresh it, if necessary

    configureOptions: (_method, _path, _params, _data, _toJSON, _responseType) ->
        options = { }
        options.method = _method
        options.url    = _path
        options.params = _params
        options.data   = _data
        options.responseType = _responseType

        if _method == 'POST' || _method == 'PUT'
            if !_toJSON && _data
                options.headers = { 'Content-Type': 'application/x-www-form-urlencoded'  }
                options.transformRequest = (data) -> $.param(data)
        options

services.service('RestService', RestService)