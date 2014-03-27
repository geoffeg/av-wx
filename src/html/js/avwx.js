$(function(){

	// Models, Collections //
	var Report = Backbone.Model.extend({
		url: function() {
			return "/api/metar/"  + encodeURIComponent(this.id) + "?geo=38.5817,-90.295"
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

	var ReportCollection = Backbone.Collection.extend({
		model: Report,

		initialize: function(models, options) {
			this.query = options.query;
		},

		findLocation : function() {
			var model = this;

			if (navigator.geolocation) {
				navigator.geolocation.getCurrentPosition(geoSuccess, geoFailure, { timeout: 6000 });
			}

			function geoSuccess(position) {
				model.latitude =  position.coords.latitude;
				model.longitude = position.coords.longitude;
				model.reset();
			}

			function geoFailure() {
				console.log("Could not get geolocation")
			}

		},

		url: function() {
			if (this.latitude) {
				return "/api/metar/"  + encodeURIComponent(this.query) + "?geo=" + this.latitude + "," + this.longitude;
			} else {
				this.findLocation();
				return "/api/metar/"  + encodeURIComponent(this.query);				
			}
		},

		parse: function(response, options){
			return response.reports;
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
			//this.render();
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
			""      : "index",
			"m"     : "localMetars",
			"m/:id" : "metar",
			"t"     : "localTafs",
			"t/:id" : "taf"
		},

		index: function() {
			console.log("index");
		},

		localMetars: function() {
			var report = new ReportCollection();
			report.fetch({update: true});
			var reports = new ReportsView({collection : report});
		},

		metar: function(search) {
			this.searchInput.val(search);
			var report = new ReportCollection([], { query : search });
			report.fetch({update: true});
			var reports = new ReportsView({collection : report});
			// window.reportsRefresh = setInterval(function() {
			// 	report.fetch({update: true});
			// }, 60000);
		},

	});

	var AppView = Backbone.View.extend({
		el: $('body'),

		events: {
			'click ul#mode li#metar-mode' : 'showMetars',
			'click ul#mode li#taf-mode'   : 'showTafs',
		},

		initialize: function() {
			this.router = new AppRouter();
			Backbone.history.start({pushState: true});
		},

		showMetars: function() {
			this.router.navigate("metar", true);
		},

		showTafs: function() {
			this.router.navigate("taf", true)
		}
	});

	
	
	var app = new AppView();

	
});


var GeoLoc = (function() {
	var latestPosition;
	function positionSuccess(position) {
		clearTimeout(this.lookupTimeout);
		if (position.coords.latitude && position.coords.longitude) { 
			this.latestPosition = position;
		}
		this.successCallback();
	}

	function positionError(error) {
		// TODO don't use null
		this.latestPosition = null;
	}

	var exports = {};

	exports.lookup = function (successCallback, failureCallback) {
		this.successCallback = successCallback;
		this.failureCallback = failureCallback;
		if (navigator.geolocation) {
			navigator.geolocation.getCurrentPosition(this.positionSuccess, this.positionError, { timeout: 6000 });
		}

		if (this.latestPosition) {
			return this.latestPosition;
		} else {
		}
	}

	Lookup.prototype.return
})

/*
function avwx() {

	var t = {};

	function getStationDirection() {
		var direction = Math.round(stationData.bearing/22.5);
		var points = ["N","NNE","NE","ENE","E","ESE", "SE", "SSE","S","SSW","SW","WSW","W","WNW","NW","NNW"];

		return points[(direction % 16)];
	}

	function getZuluTime() {
		var now = new Date();
		return now.getUTCHours() + ":" + now.getUTCMinutes() + "Z";
	}

	t.updateReportAges = function() {

	}

	return t;
}*/