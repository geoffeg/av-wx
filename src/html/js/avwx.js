$(function(){
	// Models, Collections //
	var Report = Backbone.Model.extend({
		url: function() {
			return "/api/metar/"  + encodeURIComponent(this.id);
		},
		initialize: function() {
//			console.log('model init', this.attributes);
		},
		cardinal_direction: function() {
			var bearing = this.get("bearingTo");
			var direction = Math.round(bearing/22.5);
			var points = ["N","NNE","NE","ENE","E","ESE", "SE", "SSE","S","SSW","SW","WSW","W","WNW","NW","NNW"];
			return points[(direction % 16)];
		},
		report_age: function() {
			var ms_diff = new Date().getTime() - new Date(this.get("observation_time")).getTime();
			return parseInt(ms_diff / 1000 / 60);
		},
		toTemplate: function() {
			var j = this.toJSON();
			j.cardinal_direction = this.cardinal_direction();
			j.report_age = this.report_age();
			return j;
		}
	});

	// TODO: Just extend report for both Taf and Metar
	var Taf = Report.extend({
		url: function() {
			return "/api/taf/" + encodeURIComponent(this.id);
		},
		report_age: function() {
			var ms_diff = new Date().getTime() - new Date(this.get("issue_time")).getTime();
			return parseInt(ms_diff / 1000 / 60);
		},
		raw_text: function() {
			return this.get("raw_text").replace(/(TEMPO|BECMG|FM[0-9]{6})/gi, "\n $1");
		},
		toTemplate: function() {
			var j = this.toJSON();
			j.raw_text = this.raw_text();
			j.report_age = this.report_age();
			j.cardinal_direction = this.cardinal_direction();
			return j;
		}
	});

	var ReportCollection = Backbone.Collection.extend({
		model: Report,

		initialize: function(models, options) {
			if (options)
				this.query = options.query;
			// this.findLocation();
		},

		url: function() {
			var apiUrl = "http://api.av-wx.com";
			if (window.location.host.match("^local")) {
				apiUrl = "http://local.api.av-wx.com/api";
			}

			if (this.query)
				return apiUrl + "/metar/"  + encodeURIComponent(this.query);
			else
				return apiUrl + "/metar/";				
		},

		parse: function(response, options){
			return response.reports;
		}
	});

	var TafCollection = ReportCollection.extend({
		model: Taf,

		url: function() {
			if (this.query) {
				return "/api/taf/" + encodeURIComponent(this.query);
			} else {
				return "/api/taf/";
			}
		}
	});

	// Views //
	var ReportView = Backbone.View.extend({
		tagName: "li",

		template: _.template($("#report-template").html()),

		initialize: function() {
			_.bindAll(this, "render");
			this.model.bind("change", this.render);
		},

		render: function() {
			this.$el.html(this.template(this.model.toTemplate()));
			return this;
		}
	});

	var ReportsView = Backbone.View.extend({
		tagName: 'ul',
		//el: "#metar-container",
		template: _.template($("#reports-template").html()),

		initialize: function() {
			this.collection.on("add", this.render, this);
			this.collection.on("reset", this.refresh, this);
		},

		refresh: function() {
			this.collection.fetch({reset: false});
		},

		render: function() {
			this.$el.empty();
			this.collection.each(function(report) {
				var reportView = new ReportView({model: report, id: "metar-" + report.attributes.station_id});
				this.$el.append(reportView.render().el);
			}, this);
			$('#metar-container').html(this.$el);
			return this;
		}
	});

	var AppRouter = Backbone.Router.extend({
		searchInput: $("#search"),
		routes: {
			""      : "localMetars",
			"m"     : "localMetars",
			"m/:id" : "metar",
			"t"     : "localTafs",
			"t/:id" : "taf"
		},
		execute: function(callback, args) {
			// Cancel the auto-refresh timer
			console.log("execute");
			clearTimeout(this.reportsRefresh);
			if (callback) callback.apply(this, args);
		},

		index: function() {
			console.log("index");
		},

		localMetars: function() {
			var report = new ReportCollection();
			var router = this;
			$("#taf-mode").removeClass("mode-selected");
			$("#metar-mode").addClass("mode-selected")

			if (navigator.geolocation) {
				navigator.geolocation.getCurrentPosition(geoSuccess, geoFailure, { timeout: 5000 });
			}

			function geoSuccess(position) {
				console.log("got location")
				report.fetch({update: true, data : { "latitude" : position.coords.latitude, "longitude" : position.coords.longitude}});
				var reports = new ReportsView({collection : report});
			}

			function geoFailure() {
				console.log("Could not get geolocation");
				report.fetch({update: true});
				var reports = new ReportsView({collection : report});
			}
			router.reportsRefresh = setTimeout(function() {
				console.log("refresh via localMetars");
				router.localMetars();
			}, 60000);
		},

		metar: function(search) {
			this.searchInput.val(search);
			$("#taf-mode").removeClass("mode-selected");
			$("#metar-mode").addClass("mode-selected")

			var report = new ReportCollection([], { query : search });
			var router = this;

			if (navigator.geolocation) {
				navigator.geolocation.getCurrentPosition(geoSuccess, geoFailure, { timeout: 5000 });
			}

			function geoSuccess(position) {
				console.log("got location")
				report.fetch({update: true, data : { "latitude" : position.coords.latitude, "longitude" : position.coords.longitude}});
				var reports = new ReportsView({collection : report});
			}

			function geoFailure() {
				console.log("Could not get geolocation");
				report.fetch({update: true});
				var reports = new ReportsView({collection : report});
			}
			router.reportsRefresh = setTimeout(function() {
				console.log("refresh via metars");
				router.metar(search);
			}, 60000);
		},

		localTafs: function(search) {
			console.log("taf")
			$("#taf-mode").addClass("mode-selected");
			$("#metar-mode").removeClass("mode-selected")
			var report = new TafCollection();
			var router = this;

			if (navigator.geolocation) {
				navigator.geolocation.getCurrentPosition(geoSuccess, geoFailure, { timeout: 5000 });
			}

			function geoSuccess(position) {
				console.log("taf got location");
				report.fetch({update: true, data: { "latitude" : position.coords.latitude, "longitude" : position.coords.longitude }});
				var reports = new ReportsView({collection : report});
			}

			function geoFailure() {
				console.log("taf geolocation failed");
				report.fetch({update : true});
				var reports = new ReportsView({collection : report});
			}
			router.reportsRefresh = setTimeout(function() {
				console.log("refresh via metars");
				router.localTaf();
			}, 60000);
		},

		taf: function(search) {
			console.log("taf")
			$("#taf-mode").addClass("mode-selected");
			$("#metar-mode").removeClass("mode-selected")
			var report = new TafCollection([], { query : search });
			var router = this;

			if (navigator.geolocation) {
				navigator.geolocation.getCurrentPosition(geoSuccess, geoFailure, { timeout: 5000 });
			}

			function geoSuccess(position) {
				console.log("taf got location");
				report.fetch({update: true, data: { "latitude" : position.coords.latitude, "longitude" : position.coords.longitude }});
				var reports = new ReportsView({collection : report});
			}

			function geoFailure() {
				console.log("taf geolocation failed");
				report.fetch({update : true});
				var reports = new ReportsView({collection : report});
			}
			router.reportsRefresh = setTimeout(function() {
				console.log("refresh via metars");
				router.taf(search);
			}, 60000);
		},
		current : function() {
			var Router = this,
				fragment = Backbone.history.fragment,
				routes = _.pairs(Router.routes),
				route = null, params = null, matched;

			matched = _.find(routes, function(handler) {
				route = _.isRegExp(handler[0]) ? handler[0] : Router._routeToRegExp(handler[0]);
				return route.test(fragment);
			});

			if(matched) {
       			params = Router._extractParameters(route, fragment);
        		route = matched[1];
    		}

    		return {
    			route : route,
    			fragment : fragment,
    			params : params
   			 };
		}
	});

	var AppView = Backbone.View.extend({
		el: $('body'),

		events: {
			'click ul#mode li#metar-mode' : 'showMetars',
			'click ul#mode li#taf-mode'   : 'showTafs',
			'submit'                      : 'search',
			'click #location-icon'        : 'location',
		},

		initialize: function() {
			this.router = new AppRouter();
			Backbone.history.start({pushState: true});
		},

		showMetars: function() {
			var searchValue = $("#search").val();
			var currentRoute = this.router.current().route;
			if (currentRoute == "localTafs") {
				this.router.navigate("m", {trigger: true})
			} else if (currentRoute == "taf") {
				this.router.navigate("m/" + searchValue, {trigger: true});
			}
		},

		showTafs: function() {
			var searchValue = $("#search").val();
			var currentRoute = this.router.current().route;
			if (currentRoute == "localMetars") {
				this.router.navigate("t", {trigger: true});
			} else if (currentRoute == "metar") {
				this.router.navigate("t/" + searchValue, {trigger: true});
			}		
		},

		search: function(e) {
			e.preventDefault();
			var searchValue = $("#search").val();
			var currentRoute = this.router.current().route;
			if (currentRoute == "localMetars" || currentRoute == "metar") {
				this.router.navigate("m/" + searchValue, {trigger: true});
			} else if (currentRoute == "localTafs" || currentRoute == "tafs") {
				this.router.navigate("t/" + searchValue, {trigger: true});
			}
		},

		location: function() {
			var currentRoute = this.router.current().route;
			$("#search").val("")
			if (currentRoute == "localMetars" || currentRoute == "metar") {
				this.router.navigate("m", {trigger: true});
			} else if (currentRoute == "localTafs" || currentRoute == "tafs") {
				this.router.navigate("t", {trigger: true});
			}
		}
	});
	
	var app = new AppView();
});