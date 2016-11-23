# Utility class for different kinds of validations
# The validation functions push your message to alerts and returns a validation boolean
class ValidationService

  @$inject = ['Constants']
  constructor: (@Constants) ->

    @alerts = []

  # checks that the specified input is not zero
  numberRequireZero: (input, message) ->
    valid = true
    if input != 0
      @alerts.push({type:'danger', msg:message}) if message?
      valid = false
    valid

  # checks that the specified input is not undefined nor empty
  requireNonNull: (input, message) ->
    valid = true
    if input == undefined || input.trim() == ''
      @alerts.push({type:'danger', msg:message}) if message?
      valid = false
    valid

  # checks object not null
  objectNonNull: (obj, message) ->
    valid = true
    if obj == undefined
      @alerts.push({type:'danger', msg:message}) if message?
      valid = false
    valid

  # validates email
  validateEmail: (email, message) ->
    valid = true
    if !@Constants.EMAIL_REGEX.test(email)
      @alerts.push({type:'danger', msg:message}) if message?
      valid = false
    valid

  # validates string equal
  validatePasswords: (password, cpassword, message) ->
    valid = true
    if !@requireNonNull(password) || !@requireNonNull(cpassword) || password != cpassword
      @alerts.push({type:'danger', msg:message}) if message?
      valid = false
    valid

  # returns alert messages
  getAlerts: () ->
    @alerts

  # push new alert
  pushAlert: (alert) ->
    @alerts.push(alert)

  # removes alert at specified index
  clearAlert: (index) ->
    @alerts.splice(index, 1)

  # clears all alert messages
  clearAll: () ->
    @alerts = []

services.service('ValidationService', ValidationService)