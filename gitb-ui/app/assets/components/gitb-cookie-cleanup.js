function setupLoginOptionElement(elementId, parameter) {
    let loginElement = document.getElementById(elementId);
    if (loginElement) {
        loginElement.onclick = function () {
            option(parameter)
        }
    }
}
function option(value) {
    if (value) {
        document.cookie = 'LOGIN_OPTION='+value+";SameSite=Strict";
    } else {
        document.cookie = 'LOGIN_OPTION=none;SameSite=Strict';
    }
    window.location.href = 'app';
}
function eraseCookies() {
    let cookiePath = document.getElementById('cp-div').textContent;
    document.cookie = 'LOGIN_OPTION=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
    document.cookie = 'tat=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
    document.cookie = 'tat=;path='+cookiePath+';expires=Thu, 01 Jan 1970 00:00:01 GMT;';
}
$(document).ready(function() {
    eraseCookies();
    setupLoginOptionElement('loginOption');
    setupLoginOptionElement('linkOption', 'link');
    setupLoginOptionElement('registerOption', 'register');
    setupLoginOptionElement('demoOption', 'demo');
    setupLoginOptionElement('migrateOption', 'migrate');
});
