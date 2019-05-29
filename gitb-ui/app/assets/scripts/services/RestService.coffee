class RestService

    @$inject = ['$http', '$q', '$log', 'AuthProvider', '$rootScope', 'Events']
    constructor: (@$http, @$q, @$log, @AuthProvider, @$rootScope, @Events) ->
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
        if config.authenticate && !@AuthProvider.isAuthenticated() && !@AuthProvider.isAuthenticated()
            # Trigger (re)authentication after logout cleanup
            @$rootScope.$emit(@Events.onLogout)
        else
            # Make request
            if config.data?
                config.data = _.pick config.data, (value, key)->
                    value?
            if !config.toJSON?
                config.toJSON = false
            @sendRequest(method, config.path.substring(1), config.params, config.data, config.toJSON, config.responseType);

    sendRequest: (_method, _url, _params, _data, _toJSON, _responseType) ->
        options = @configureOptions(_method, _url, _params, _data, _toJSON, _responseType)
        @$http(options)
        .then(
            (result) =>
                result.data
            (error) =>
                if error? && error.status? && error.status == 401
                    # Handle only authorisation-related errors.
                    @$rootScope.$emit(@Events.onLogout)
                else
                    throw error
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