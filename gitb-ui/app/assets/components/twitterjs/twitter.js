/*
Plugin Name: 	Twitter Feed
Written by: 	Okler Themes - (http://www.okler.net)
Version: 		0.1
Based On: 		http://www.webdevdoor.com/php/authenticating-twitter-feed-timeline-oauth/
*/

var twitterFeed = {

	options: {
		id: "",
		count: 2,
		URL: "php/twitter-feed.php",
		wrapper: "tweet"
	},

	initialize: function( options ) {
		this.options = $.extend( {}, this.options, options );

		this.options.URL = this.options.URL + "?twitteruser=" + this.options.id + "&notweets=" + this.options.count;

		this.build();

		return this;
	},

	build: function() {

		var $this = this;

		var showdirecttweets = false;
		var showretweets = true;
		var showtweetlinks = true;
		var showprofilepic = true;

		$.getJSON($this.options.URL,
			function (feeds) {

				var feedHTML = '';
				var displayCounter = 1;
				for (var i = 0; i < feeds.length; i++) {
					var tweetscreenname = feeds[i].user.name;
					var tweetusername = feeds[i].user.screen_name;
					var profileimage = feeds[i].user.profile_image_url_https;
					var status = feeds[i].text;
					var isaretweet = false;
					var isdirect = false;
					var tweetid = feeds[i].id_str;

					if (typeof feeds[i].retweeted_status != 'undefined') {
						profileimage = feeds[i].retweeted_status.user.profile_image_url_https;
						tweetscreenname = feeds[i].retweeted_status.user.name;
						tweetusername = feeds[i].retweeted_status.user.screen_name;
						tweetid = feeds[i].retweeted_status.id_str
						isaretweet = true;
					};

					//Check to see if the tweet is a direct message
					if (feeds[i].text.substr(0, 1) == "@") {
						isdirect = true;
					}

					if (((showretweets == true) || ((isaretweet == false) && (showretweets == false))) && ((showdirecttweets == true) || ((showdirecttweets == false) && (isdirect == false)))) {
						if ((feeds[i].text.length > 1) && (displayCounter <= $this.options.count)) {
							if (showtweetlinks == true) {
								status = $this.addLink(status);
							}

							feedHTML += '<i class="icon icon-twitter"></i> ' + status + ' <a class="time"  href="https://twitter.com/' + tweetusername + '/status/' + tweetid + '">' + $this.relativeTime(feeds[i].created_at) + '</a>';

							displayCounter++;
						}
					}
				}

				$("#" + $this.options.wrapper).html(feedHTML);
			});

	},

	addLink: function(data) {

		//Add link to all http:// links within tweets
		data = data.replace(/((https?|s?ftp|ssh)\:\/\/[^"\s\<\>]*[^.,;'">\:\s\<\>\)\]\!])/g, function (url) {
			return '<a href="' + url + '" >' + url + '</a>';
		});

		//Add link to @usernames used within tweets
		data = data.replace(/\B@([_a-z0-9]+)/ig, function (reply) {
			return '<a href="http://twitter.com/' + reply.substring(1) + '" style="font-weight:lighter;" >' + reply.charAt(0) + reply.substring(1) + '</a>';
		});

		return data;

	},

	relativeTime: function(timeValue) {

		var values = timeValue.split(" ");
		timeValue = values[1] + " " + values[2] + ", " + values[5] + " " + values[3];
		var parsed_date = Date.parse(timeValue);
		var relative_to = (arguments.length > 1) ? arguments[1] : new Date();
		var delta = parseInt((relative_to.getTime() - parsed_date) / 1000);
		var shortdate = timeValue.substr(4, 2) + " " + timeValue.substr(0, 3);
		delta = delta + (relative_to.getTimezoneOffset() * 60);

		if (delta < 60) {
			return '1m';
		} else if (delta < 120) {
			return '1m';
		} else if (delta < (60 * 60)) {
			return (parseInt(delta / 60)).toString() + 'm';
		} else if (delta < (120 * 60)) {
			return '1h';
		} else if (delta < (24 * 60 * 60)) {
			return (parseInt(delta / 3600)).toString() + 'h';
		} else if (delta < (48 * 60 * 60)) {
			return shortdate;
		} else {
			return shortdate;
		}

	}

};

// Compatibility with old function
var getTwitters = function(wrapper, options) {
	options.wrapper = wrapper;
	twitterFeed.initialize(options);
}