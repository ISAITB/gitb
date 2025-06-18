/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
    let contextPath = document.getElementById('ctx-div').textContent;
    window.location.href = contextPath+'app';
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
