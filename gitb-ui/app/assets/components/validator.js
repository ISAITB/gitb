
(function() {

	"use strict";

	var Contact = {

		initialized: false,

		initialize: function() {

			if (this.initialized) return;
			this.initialized = true;

			this.build();
			this.events();

		},

		build: function() {

			this.validations();

		},

		events: function() {



		},

		validations: function() {

			var validatedForm = $("#validatedForm");
			var validatedForm1 = $("#validatedForm1");
			var validatedForm2 = $("#validatedForm2");
			var loginForm = $("#loginForm");
			var registerForm = $("#registerForm");
			var mainLoginForm = $("#mainLoginForm");
			var mainRegisterForm = $("#mainRegisterForm");
			var mainResetForm = $("#mainResetForm");

			var handler = {
				submitHandler: function(form) {
	            },
	            rules: {
	                name: {
	                    required: true
	                },
	                email: {
	                    required: true,
	                    email: true
	                },
	                sname: {
	                    required: true
	                },
	                fname: {
	                    required: true
	                },
	                password: {
	                    required: true
	                },
	                subject: {
	                    required: true
	                },
	                message: {
	                    required: true
	                }
	            },
	            highlight: function (element) {
	                $(element)
	                    .parent()
	                    .removeClass("has-success")
	                    .addClass("has-error");
	            },
	            success: function (element) {
	                $(element)
	                    .parent()
	                    .removeClass("has-error")
	                    .addClass("has-success")
	                    .find("label.error")
	                    .remove();
	            }
			}

			validatedForm.validate(handler);
			validatedForm1.validate(handler);
			validatedForm2.validate(handler);
			loginForm.validate(handler);
			registerForm.validate(handler);
            mainLoginForm.validate(handler);
            mainRegisterForm.validate(handler);
            mainResetForm.validate(handler);

		}

	};

	Contact.initialize();

})();