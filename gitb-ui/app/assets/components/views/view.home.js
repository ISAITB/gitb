/*
Name: 			View - Home
Written by: 	Okler Themes - (http://www.okler.net)
Version: 		2.0
*/

(function() {

	"use strict";

	var Home = {

		initialized: false,

		initialize: function() {

			if (this.initialized) return;
			this.initialized = true;

			this.build();
			this.events();

		},

		build: function(options) {

			// Circle Slider
			if($("#fcSlideshow").get(0)) {
				$("#fcSlideshow").flipshow();

				setInterval( function() {
					$("#fcSlideshow div.fc-right span:first").click();
				}, 3000);

			}

			// Revolution Slider Initialize
			$("#revolutionSlider").each(function() {

				var slider = $(this);

				var defaults = {
					delay: 9000,
					startheight: 495,
					startwidth: 960,

					hideThumbs: 10,

					thumbWidth: 100,
					thumbHeight: 50,
					thumbAmount: 5,

					navigationType: "both",
					navigationArrows: "verticalcentered",
					navigationStyle: "round",

					touchenabled: "on",
					onHoverStop: "on",

					navOffsetHorizontal: 0,
					navOffsetVertical: 20,

					stopAtSlide: 0,
					stopAfterLoops: -1,

					shadow: 0,
					fullWidth: "on",
					videoJsPath: "vendor/rs-plugin/videojs/"
				}

				var config = $.extend({}, defaults, options, slider.data("plugin-options"));

				// Initialize Slider
				var sliderApi = slider.revolution(config).addClass("slider-init");

				// Set Play Button to Visible
				sliderApi.bind("revolution.slide.onloaded ",function (e,data) {
					$(".home-player").addClass("visible");
				});

			});

			// Revolution Slider One Page
			if($("#revolutionSliderFullScreen").get(0)) {
				var rev = $("#revolutionSliderFullScreen").revolution({
					delay: 9000,
					startwidth: 1170,
					startheight: 600,

					hideThumbs: 200,

					thumbWidth: 100,
					thumbHeight: 50,
					thumbAmount: 5,

					navigationType: "both",
					navigationArrows: "verticalcentered",
					navigationStyle: "round",

					touchenabled: "on",
					onHoverStop: "on",

					navOffsetHorizontal: 0,
					navOffsetVertical: 20,

					stopAtSlide: -1,
					stopAfterLoops: -1,

					shadow: 0,
					fullWidth: "on",
					fullScreen: "on",
					fullScreenOffsetContainer: ".header",
					videoJsPath: "vendor/rs-plugin/videojs/"
				});

			}

			// Nivo Slider
			if($("#nivoSlider").get(0)) {
				$("#nivoSlider").nivoSlider();
			}

		},

		events: function() {

			this.moveCloud();

		},

		moveCloud: function() {

			var $this = this;

			$(".cloud").animate( {"top": "+=20px"}, 3000, "linear", function() {
				$(".cloud").animate( {"top": "-=20px"}, 3000, "linear", function() {
					$this.moveCloud();
				});
			});

		}

	};

	Home.initialize();

})();