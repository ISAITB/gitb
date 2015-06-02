/*
Name: 			View - Contact Advanced
Written by: 	Okler Themes - (http://www.okler.net)
Version: 		1.0
*/

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

			var submitButton = $("#contactFormSubmit"),
				contactForm = $("#contactForm");

			submitButton.on("click", function() {
				if(contactForm.valid()) {
					submitButton.button("loading");
				}
			});

		},

		validations: function() {

			$("#contactForm").validate({
				onkeyup: false,
				onclick: false,
				onfocusout: false,
				rules: {
					name: {
						required: true
					},
					email: {
						required: true,
						email: true
					},
					subject: {
						required: true
					},
					message: {
						required: true
					},
					captcha: {
						required: true,
						captcha: true
					},
					'checkboxes[]': {
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
			});

			$.validator.addMethod("captcha", function () {
				var captchaValid = false;
				var phpquery = $.ajax({
					url: "php/contact-form-verify-captcha.php",
					type: "POST",
					async: false,
					dataType: "json",
					data: {
						captcha: $.trim($("#contactForm #captcha").val())
					},
					success: function (data) {
						if (data.response == "success") {
							captchaValid = true;
						} else {

						}
					}
				});
				if (captchaValid) {
					return true;
				}
			}, "");

		}

	};

	Contact.initialize();

})();