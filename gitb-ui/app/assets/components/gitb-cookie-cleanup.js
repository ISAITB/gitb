function setupLoginOptionElement(elementId, parameter) {
    let loginElement = document.getElementById(elementId);
    if (loginElement) {
        loginElement.onclick = function () {
            option(parameter)
        }
    }
}
function option(value) {
    if (sessionStorage) {
        if (value) {
            sessionStorage.setItem('com.itb.loginOption', value);
        } else {
            sessionStorage.setItem('com.itb.loginOption', 'none');
        }
    }
    window.location.href = 'app';
}
function eraseCookies() {
    if (sessionStorage) {
        sessionStorage.clear();
    }
    let cookiePath = document.getElementById('cp-div').textContent;
    document.cookie = 'ITB_REQUESTED_URL=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
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
