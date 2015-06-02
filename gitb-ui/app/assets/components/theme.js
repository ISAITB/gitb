/*
Name: 			Core Initializer
Written by: 	Okler Themes - (http://www.okler.net)
Version: 		2.9.0 - Wed Mar 19 2014 16:59:18
*/

(function() {

	"use strict";

	var Core = {

		initialized: false,

		initialize: function() {

			if (this.initialized) return;
			this.initialized = true;

			this.build();
			this.events();

		},

		build: function() {

			// Adds browser version on html class.
			$.browserSelector();

			// Adds window smooth scroll on chrome.
			if($("html").hasClass("chrome")) {
				$.smoothScroll();
			}

			// Scroll to Top Button.
			$.scrollToTop();

			// Nav Menu
			this.navMenu();

			// Header Search
			this.headerSearch();

			// Animations
			this.animations();

			// Word Rotate
			this.wordRotate();

			// Newsletter
			this.newsletter();

			// Featured Boxes
			this.featuredBoxes();

			// Tooltips
			$("a[rel=tooltip]").tooltip();

			// Owl Carousel
			this.owlCarousel();

			// Sort
			this.sort();

			// Toggle
			this.toggle();

			// Twitter
			this.latestTweets();

			// Flickr Feed
			this.flickrFeed();

			// Lightbox
			this.lightbox();

			// Media Element
			this.mediaElement();

			// Parallax
			this.parallax();

			// Account
			this.account();

		},

		events: function() {

			// Window Resize
			$(window).afterResize(function() {

				// Featured Boxes
				Core.featuredBoxes();

				// Sticky Menu
				Core.checkStickyMenu();

				// Revolution Slider Fix
				Core.fixRevolutionSlider();

				// Isotope
				if($(".isotope").get(0)) {
					$(".isotope").isotope('reLayout');
				}

				// Product Info Box
				Core.productInfoBox();

			});

			// Anchors Position
			$("a[data-hash]").on("click", function(e) {

				e.preventDefault();
				var header = $("body header:first"),
					headerHeight = header.height(),
					target = $(this).attr("href"),
					$this = $(this);

				if($(window).width() > 991) {
					$("html,body").animate({scrollTop: $(target).offset().top - (headerHeight + 50)}, 600, "easeOutQuad");
				} else {
					$("html,body").animate({scrollTop: $(target).offset().top - 30}, 600, "easeOutQuad");
				}

				return false;

			});

			$("body").waitForImages(function() {
				Core.productInfoBox();
			});

		},

		navMenu: function() {

			// Responsive Menu Events
			var addActiveClass = false;

			$("#mainMenu li.dropdown > a, #mainMenu li.dropdown-submenu > a").on("click", function(e) {

				if($(window).width() > 979) return;

				e.preventDefault();

				addActiveClass = $(this).parent().hasClass("resp-active");

				$("#mainMenu").find(".resp-active").removeClass("resp-active");

				if(!addActiveClass) {
					$(this).parents("li").addClass("resp-active");
				}

				return;

			});

			// Submenu Check Visible Space
			$("#mainMenu li.dropdown-submenu").hover(function() {

				if($(window).width() < 767) return;

				var subMenu = $(this).find("ul.dropdown-menu");

				if(!subMenu.get(0)) return;

				var screenWidth = $(window).width(),
					subMenuOffset = subMenu.offset(),
					subMenuWidth = subMenu.width(),
					subMenuParentWidth = subMenu.parents("ul.dropdown-menu").width(),
					subMenuPosRight = subMenu.offset().left + subMenu.width();

				if(subMenuPosRight > screenWidth) {
					subMenu.css("margin-left", "-" + (subMenuParentWidth + subMenuWidth + 10) + "px");
				} else {
					subMenu.css("margin-left", 0);
				}

			});

			// Mega Menu
			$(document).on("click", ".mega-menu .dropdown-menu", function(e) {
				e.stopPropagation()
			});

			// Mobile Redirect
			$(".mobile-redirect").on("click", function() {
				if($(window).width() < 991) {
					self.location = $(this).attr("href");
				}
			});

		},

		stickyMenu: function() {

			if($("body").hasClass("boxed"))
				return false;

			var $this = this,
				$body = $("body"),
				header = $("header:first"),
				headerContainer = header.parent(),
				menuAfterHeader = (typeof header.data('after-header') !== 'undefined'),
				headerHeight = header.height(),
				flatParentItems = $("header.flat-menu ul.nav-main > li > a"),
				logoWrapper = header.find(".logo"),
				logo = header.find(".logo img"),
				logoWidth = logo.attr("width"),
				logoHeight = logo.attr("height"),
				logoPaddingTop = parseInt(logo.attr("data-sticky-padding") ? logo.attr("data-sticky-padding") : "28"),
				logoSmallWidth = parseInt(logo.attr("data-sticky-width") ? logo.attr("data-sticky-width") : "82"),
				logoSmallHeight = parseInt(logo.attr("data-sticky-height") ? logo.attr("data-sticky-height") : "40");

			if(menuAfterHeader) {
				headerContainer.css("min-height", header.height());
			}

			$(window).afterResize(function() {
				headerContainer.css("min-height", header.height());
			});

			$this.checkStickyMenu = function() {

				if($body.hasClass("boxed") || $(window).width() < 991) {
					$this.stickyMenuDeactivate();
					header.removeClass("fixed")
					return false;
				}

				if(!menuAfterHeader) {

					if($(window).scrollTop() > ((headerHeight - 15) - logoSmallHeight)) {

						$this.stickyMenuActivate();

					} else {

						$this.stickyMenuDeactivate();

					}

				} else {

					if($(window).scrollTop() > header.parent().offset().top) {

						header.addClass("fixed");

					} else {

						header.removeClass("fixed");

					}

				}

			}

			$this.stickyMenuActivate = function() {

				if($body.hasClass("sticky-menu-active"))
					return false;

				logo.stop(true, true);

				$body.addClass("sticky-menu-active").css("padding-top", headerHeight);
				flatParentItems.addClass("sticky-menu-active");

				logoWrapper.addClass("logo-sticky-active");

				logo.animate({
					width: logoSmallWidth,
					height: logoSmallHeight,
					top: logoPaddingTop + "px"
				}, 200, function() {});

			}

			$this.stickyMenuDeactivate = function() {

				if($body.hasClass("sticky-menu-active")) {

					$body.removeClass("sticky-menu-active").css("padding-top", 0);
					flatParentItems.removeClass("sticky-menu-active");

					logoWrapper.removeClass("logo-sticky-active");

					logo.animate({
						width: logoWidth,
						height: logoHeight,
						top: "0px"
					}, 200);

				}

			}


			$(window).on("scroll", function() {

				$this.checkStickyMenu();

			});

			$this.checkStickyMenu();

		},

		headerSearch: function() {

			$("#searchForm").validate({
				rules: {
					q: {
						required: true
					}
				},
	            errorPlacement: function(error, element) {

	            },
				highlight: function (element) {
					$(element)
						.closest(".input-group")
						.removeClass("has-success")
						.addClass("has-error");
				},
				success: function (element) {
					$(element)
						.closest(".input-group")
						.removeClass("has-error")
						.addClass("has-success");
				}
			});

		},

		animations: function() {

			// Animation Appear
			$("[data-appear-animation]").each(function() {

				var $this = $(this);

				$this.addClass("appear-animation");

				if(!$("html").hasClass("no-csstransitions") && $(window).width() > 767) {

					$this.appear(function() {

						var delay = ($this.attr("data-appear-animation-delay") ? $this.attr("data-appear-animation-delay") : 1);

						if(delay > 1) $this.css("animation-delay", delay + "ms");
						$this.addClass($this.attr("data-appear-animation"));

						setTimeout(function() {
							$this.addClass("appear-animation-visible");
						}, delay);

					}, {accX: 0, accY: -150});

				} else {

					$this.addClass("appear-animation-visible");

				}

			});

			// Animation Progress Bars
			$("[data-appear-progress-animation]").each(function() {

				var $this = $(this);

				$this.appear(function() {

					var delay = ($this.attr("data-appear-animation-delay") ? $this.attr("data-appear-animation-delay") : 1);

					if(delay > 1) $this.css("animation-delay", delay + "ms");
					$this.addClass($this.attr("data-appear-animation"));

					setTimeout(function() {

						$this.animate({
							width: $this.attr("data-appear-progress-animation")
						}, 1500, "easeOutQuad", function() {
							$this.find(".progress-bar-tooltip").animate({
								opacity: 1
							}, 500, "easeOutQuad");
						});

					}, delay);

				}, {accX: 0, accY: -50});

			});

			// Count To
			$(".counters [data-to]").each(function() {

				var $this = $(this);

				$this.appear(function() {

					$this.countTo({
						onComplete: function() {
							if($this.data("append")) {
								$this.html($this.html() + $this.data("append"));
							}
						}
					});

				}, {accX: 0, accY: -150});

			});

			/* Circular Bars - Knob */
			if(typeof($.fn.knob) != "undefined") {
				$(".knob").knob({});
			}

		},

		wordRotate: function() {

			$(".word-rotate").each(function() {

				var $this = $(this),
					itemsWrapper = $(this).find(".word-rotate-items"),
					items = itemsWrapper.find("> span"),
					firstItem = items.eq(0),
					firstItemClone = firstItem.clone(),
					itemHeight = 0,
					currentItem = 1,
					currentTop = 0;

				itemHeight = firstItem.height();

				itemsWrapper.append(firstItemClone);

				$this
					.height(itemHeight)
					.addClass("active");

				setInterval(function() {

					currentTop = (currentItem * itemHeight);

					itemsWrapper.animate({
						top: -(currentTop) + "px"
					}, 300, "easeOutQuad", function() {

						currentItem++;

						if(currentItem > items.length) {

							itemsWrapper.css("top", 0);
							currentItem = 1;

						}

					});

				}, 2000);

			});

		},

		newsletter: function() {

			$("#newsletterForm").validate({
				submitHandler: function(form) {

					$.ajax({
						type: "POST",
						url: $("#newsletterForm").attr("action"),
						data: {
							"email": $("#newsletterForm #email").val()
						},
						dataType: "json",
						success: function (data) {
							if (data.response == "success") {

								$("#newsletterSuccess").removeClass("hidden");
								$("#newsletterError").addClass("hidden");

								$("#newsletterForm #email")
									.val("")
									.blur()
									.closest(".control-group")
									.removeClass("success")
									.removeClass("error");

							} else {

								$("#newsletterError").html(data.message);
								$("#newsletterError").removeClass("hidden");
								$("#newsletterSuccess").addClass("hidden");

								$("#newsletterForm #email")
									.blur()
									.closest(".control-group")
									.removeClass("success")
									.addClass("error");

							}
						}
					});

				},
				rules: {
					email: {
						required: true,
						email: true
					}
				},
	            errorPlacement: function(error, element) {

	            },
				highlight: function (element) {
					$(element)
						.closest(".control-group")
						.removeClass("success")
						.addClass("error");
				},
				success: function (element) {
					$(element)
						.closest(".control-group")
						.removeClass("error")
						.addClass("success");
				}
			});

		},

		featuredBoxes: function() {

			$("div.featured-box").css("height", "auto");

			$("div.featured-boxes").each(function() {

				var wrapper = $(this);
				var minBoxHeight = 0;

				$("div.featured-box", wrapper).each(function() {
					if($(this).height() > minBoxHeight)
						minBoxHeight = $(this).height();
				});

				$("div.featured-box", wrapper).height(minBoxHeight);

			});

		},

		owlCarousel: function(options) {

			var total = $("div.owl-carousel:not(.manual)").length,
				count = 0;

			$("div.owl-carousel:not(.manual)").each(function() {

				var slider = $(this);

				var defaults = {
					 // Most important owl features
					items : 5,
					itemsCustom : false,
					itemsDesktop : [1199,4],
					itemsDesktopSmall : [980,3],
					itemsTablet: [768,2],
					itemsTabletSmall: false,
					itemsMobile : [479,1],
					singleItem : true,
					itemsScaleUp : false,

					//Basic Speeds
					slideSpeed : 200,
					paginationSpeed : 800,
					rewindSpeed : 1000,

					//Autoplay
					autoPlay : false,
					stopOnHover : false,

					// Navigation
					navigation : false,
					navigationText : ["<i class=\"icon icon-chevron-left\"></i>","<i class=\"icon icon-chevron-right\"></i>"],
					rewindNav : true,
					scrollPerPage : false,

					//Pagination
					pagination : true,
					paginationNumbers: false,

					// Responsive
					responsive: true,
					responsiveRefreshRate : 200,
					responsiveBaseWidth: window,

					// CSS Styles
					baseClass : "owl-carousel",
					theme : "owl-theme",

					//Lazy load
					lazyLoad : false,
					lazyFollow : true,
					lazyEffect : "fade",

					//Auto height
					autoHeight : false,

					//JSON
					jsonPath : false,
					jsonSuccess : false,

					//Mouse Events
					dragBeforeAnimFinish : true,
					mouseDrag : true,
					touchDrag : true,

					//Transitions
					transitionStyle : false,

					// Other
					addClassActive : false,

					//Callbacks
					beforeUpdate : false,
					afterUpdate : false,
					beforeInit: false,
					afterInit: false,
					beforeMove: false,
					afterMove: false,
					afterAction: false,
					startDragging : false,
					afterLazyLoad : false
				}

				var config = $.extend({}, defaults, options, slider.data("plugin-options"));

				// Initialize Slider
				slider.owlCarousel(config).addClass("owl-carousel-init");

			});

		},

		sort: function() {

			$("ul.sort-source").each(function() {

				var source = $(this);
				var destination = $("ul.sort-destination[data-sort-id=" + $(this).attr("data-sort-id") + "]");

				if(destination.get(0)) {

					var minParagraphHeight = 0;
					var paragraphs = $("span.thumb-info-caption p", destination);

					paragraphs.each(function() {
						if($(this).height() > minParagraphHeight)
							minParagraphHeight = ($(this).height() + 10);
					});

					paragraphs.height(minParagraphHeight);

					$(window).load(function() {

						destination.isotope({
							itemSelector: "li",
							layoutMode: 'sloppyMasonry'
						});

						source.find("a").click(function(e) {

							e.preventDefault();

							var $this = $(this),
								filter = $this.parent().attr("data-option-value");

							source.find("li.active").removeClass("active");
							$this.parent().addClass("active");

							destination.isotope({
								filter: filter
							});

							if(window.location.hash != "" || filter.replace(".","") != "*") {
								self.location = "#" + filter.replace(".","");
							}

							return false;

						});

						$(window).bind("hashchange", function(e) {

							var hashFilter = "." + location.hash.replace("#",""),
								hash = (hashFilter == "." || hashFilter == ".*" ? "*" : hashFilter);

							source.find("li.active").removeClass("active");
							source.find("li[data-option-value='" + hash + "']").addClass("active");

							destination.isotope({
								filter: hash
							});

						});

						var hashFilter = "." + (location.hash.replace("#","") || "*");

						var initFilterEl = source.find("li[data-option-value='" + hashFilter + "'] a");

						if(initFilterEl.get(0)) {
							source.find("li[data-option-value='" + hashFilter + "'] a").click();
						} else {
							source.find("li:first-child a").click();
						}

					});

				}

			});

		},

		toggle: function() {

			var $this = this,
				previewParClosedHeight = 25;

			$("section.toggle > label").prepend($("<i />").addClass("icon icon-plus"));
			$("section.toggle > label").prepend($("<i />").addClass("icon icon-minus"));
			$("section.toggle.active > p").addClass("preview-active");
			$("section.toggle.active > div.toggle-content").slideDown(350, function() {});

			$("section.toggle > label").click(function(e) {

				var parentSection = $(this).parent(),
					parentWrapper = $(this).parents("div.toogle"),
					previewPar = false,
					isAccordion = parentWrapper.hasClass("toogle-accordion");

				if(isAccordion && typeof(e.originalEvent) != "undefined") {
					parentWrapper.find("section.toggle.active > label").trigger("click");
				}

				parentSection.toggleClass("active");

				// Preview Paragraph
				if(parentSection.find("> p").get(0)) {

					previewPar = parentSection.find("> p");
					var previewParCurrentHeight = previewPar.css("height");
					previewPar.css("height", "auto");
					var previewParAnimateHeight = previewPar.css("height");
					previewPar.css("height", previewParCurrentHeight);

				}

				// Content
				var toggleContent = parentSection.find("> div.toggle-content");

				if(parentSection.hasClass("active")) {

					$(previewPar).animate({
						height: previewParAnimateHeight
					}, 350, function() {
						$(this).addClass("preview-active");
					});

					toggleContent.slideDown(350, function() {});

				} else {

					$(previewPar).animate({
						height: previewParClosedHeight
					}, 350, function() {
						$(this).removeClass("preview-active");
					});

					toggleContent.slideUp(350, function() {});

				}

			});

		},

		lightbox: function(options) {

			if(typeof($.magnificPopup) == "undefined") {
				return false;
			}

			// Internationalization of Lightbox
			$.extend(true, $.magnificPopup.defaults, {
				tClose: 'Close (Esc)', // Alt text on close button
				tLoading: 'Loading...', // Text that is displayed during loading. Can contain %curr% and %total% keys
				gallery: {
					tPrev: 'Previous (Left arrow key)', // Alt text on left arrow
					tNext: 'Next (Right arrow key)', // Alt text on right arrow
					tCounter: '%curr% of %total%' // Markup for "1 of 7" counter
				},
				image: {
					tError: '<a href="%url%">The image</a> could not be loaded.' // Error message when image could not be loaded
				},
				ajax: {
					tError: '<a href="%url%">The content</a> could not be loaded.' // Error message when ajax request failed
				}
			});

			$(".lightbox").each(function() {

				var el = $(this);

				var config, defaults = {}
				if(el.data("plugin-options"))
					config = $.extend({}, defaults, options, el.data("plugin-options"));

				$(this).magnificPopup(config);

			});

		},

		flickrFeed: function(options) {

			$("ul.flickr-feed").each(function() {

				var el = $(this);

				var defaults = {
					limit: 6,
					qstrings: {
						id: ''
					},
					itemTemplate: '<li><a href="{{image_b}}"><span class="thumbnail"><img alt="{{title}}" src="{{image_s}}" /></span></a></li>'
				}

				var config = $.extend({}, defaults, options, el.data("plugin-options"));

				el.jflickrfeed(config, function(data) {

					el.magnificPopup({
						delegate: "a",
						type: "image",
						gallery: {
							enabled: true,
							navigateByImgClick: true,
							preload: [0,1]
						},
						zoom: {
							enabled: true,
							duration: 300,
							opener: function(element) {
								return element.find('img');
							}
						}
					});

				});


			});

		},

		mediaElement: function(options) {

			if(typeof(mejs) == "undefined") {
				return false;
			}

			$("video:not(.manual)").each(function() {

				var el = $(this);

				var defaults = {
					defaultVideoWidth: 480,
					defaultVideoHeight: 270,
					videoWidth: -1,
					videoHeight: -1,
					audioWidth: 400,
					audioHeight: 30,
					startVolume: 0.8,
					loop: false,
					enableAutosize: true,
					features: ['playpause','progress','current','duration','tracks','volume','fullscreen'],
					alwaysShowControls: false,
					iPadUseNativeControls: false,
					iPhoneUseNativeControls: false,
					AndroidUseNativeControls: false,
					alwaysShowHours: false,
					showTimecodeFrameCount: false,
					framesPerSecond: 25,
					enableKeyboard: true,
					pauseOtherPlayers: true,
					keyActions: []
				}

				var config = $.extend({}, defaults, options, el.data("plugin-options"));

				el.mediaelementplayer(config);

			});

		},

		parallax: function() {

			if(typeof($.stellar) == "undefined") {
				return false;
			}

			$(window).load(function () {

				if($(".parallax").get(0)) {
					if(!Modernizr.touch) {
						$(window).stellar({
							responsive:true,
							scrollProperty: 'scroll',
							parallaxElements: false,
							horizontalScrolling: false,
							horizontalOffset: 0,
							verticalOffset: 0
						});
					} else {
						$(".parallax").addClass("disabled");
					}
				}
			});

		},

		latestTweets: function() {

			var wrapper = $("#tweet"),
				accountId = wrapper.data("account-id");

			if(wrapper.get(0) && accountId != "") {
				getTwitters("tweet", {
					id: accountId,
					count: 2
				});

				wrapper.before($("<a />").addClass("twitter-account").html("@" + accountId).attr("href", "http://www.twitter.com/" + accountId).attr("target", "_blank"));

			} else {
				wrapper.empty();
			}

		},

		fixRevolutionSlider: function() {

			$(".revslider-initialised").each(function() {
				try{
					$(this).revredraw();
				} catch(e) {}
			});

		},

		productInfoBox: function() {

			if($(window).width() > 991) {
				$(".product-thumb-info").css("min-height", "auto");

				$(".product-thumb-info-list").each(function() {

					var wrapper = $(this);
					var minBoxHeight = 0;

					$(".product-thumb-info", wrapper).each(function() {
						if($(this).height() > minBoxHeight)
							minBoxHeight = $(this).height();
					});

					$(".product-thumb-info", wrapper).height(minBoxHeight);

				});
			} else {
				$(".product-thumb-info").css("min-height", "auto");
			}

		},

		account: function() {

			var headerAccountWrapper = $("#headerAccount"),
				closeEventAdded = false;

			// Events
			headerAccountWrapper.find("input").on("focus", function() {
				headerAccountWrapper.addClass("open");

				if(closeEventAdded) return;

				$(document).mouseup(function(e) {
				    if (!headerAccountWrapper.is(e.target) && headerAccountWrapper.has(e.target).length === 0) {
			        	headerAccountWrapper.removeClass("open");
			    	}
				});

				closeEventAdded = true;
			});

			$("#headerSignUp").on("click", function(e) {
				e.preventDefault();
				headerAccountWrapper.addClass("signup").removeClass("signin").removeClass("recover");
				headerAccountWrapper.find(".signup-form input:first").focus();
			});

			$("#headerSignIn").on("click", function(e) {
				e.preventDefault();
				headerAccountWrapper.addClass("signin").removeClass("signup").removeClass("recover");
				headerAccountWrapper.find(".signin-form input:first").focus();
			});

			$("#headerRecover").on("click", function(e) {
				e.preventDefault();
				headerAccountWrapper.addClass("recover").removeClass("signup").removeClass("signin");
				headerAccountWrapper.find(".recover-form input:first").focus();
			});

			$("#headerRecoverCancel").on("click", function(e) {
				e.preventDefault();
				headerAccountWrapper.addClass("signin").removeClass("signup").removeClass("recover");
				headerAccountWrapper.find(".signin-form input:first").focus();
			});

		}

	};

	Core.initialize();

	$(window).load(function () {

		// Sticky Meny
		Core.stickyMenu();

	});

})();