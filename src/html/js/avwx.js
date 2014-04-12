$(function(){
	var Report = Backbone.Model.extend({
		cardinal_direction: function() {
			var bearing = this.get("bearingTo");
			var direction = Math.round(bearing/22.5);
			var points = ["N","NNE","NE","ENE","E","ESE", "SE", "SSE","S","SSW","SW","WSW","W","WNW","NW","NNW"];
			return points[(direction % 16)];
		}
	});

	var Metar = Report.extend({
		url: function() {
			return "/api/metar/"  + encodeURIComponent(this.id);
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

	var Taf = Report.extend({
		url: function() {
			return "/api/taf/" + encodeURIComponent(this.id);
		},
		report_age: function() {
			var ms_diff = new Date().getTime() - new Date(this.get("issue_time")).getTime();
			return parseInt(ms_diff / 1000 / 60);
		},
		raw_text: function() { // Add line breaks and spaces after TEMPO, BECMG and FM on TAFs
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

	// Collections
	var ReportCollection = Backbone.Collection.extend({
		initialize: function(models, options) {
			if (options)
				this.query = options.query;
		},
		parse: function(response, options){
			return response.reports;
		}
	});

	var MetarCollection = ReportCollection.extend({
		model: Metar,

		url: function() {
			if (this.query)
				return "/api/metar/"  + encodeURIComponent(this.query);
			else
				return "/api/metar/";				
		},
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
		poller_options : { delay: 60000, delayed: true },
		tagName: 'ul',
		template: _.template($("#reports-template").html()),

		initialize: function() {
			this.render = _.wrap(this.render, function(render) {
				// this.beforeRender();
				render.apply(this);
				this.trigger("afterRender", this); //this.afterRender();
			});
			this.collection.on("add", this.render, this);
		},

		render: function() {
			this.$el.empty();
			this.collection.each(function(report) {
				var reportView = new ReportView({model: report, id: "report-" + report.attributes.station_id});
				this.$el.append(reportView.render().el);
			}, this);
			$('#metar-container').html(this.$el);
			return this;
		},

		poll: function(data) {
			var view = this;
			Backbone.Poller.reset();
			this.poller = Backbone.Poller.get(this.collection, _.extend(this.poller_options, { "data" : data }));
			this.poller.on('success', function(model) {
				view.render(model);
			}).start();
		}
	});

	var AppRouter = Backbone.Router.extend({
		searchInput: $("#search"),
		routes: {
			""      : "metar",
			"m"     : "metar",
			"m/:id" : "searchMetar",
			"t"     : "taf",
			"t/:id" : "taf"
		},
		
		metar: function(search) {
			this.searchInput.val(search);
			$("#taf-mode").removeClass("mode-selected");
			$("#metar-mode").addClass("mode-selected")

			var metars = new MetarCollection([], { query : search });

			if (navigator.geolocation) {
				navigator.geolocation.getCurrentPosition(geoSuccess, geoFailure, { timeout: 5000 });
			}

			function geoSuccess(position) {
				report.fetch({update: true, data : { "latitude" : position.coords.latitude, "longitude" : position.coords.longitude}});
				var reports = new ReportsView({collection : report});
			}

			function geoFailure() {
				report.fetch({update: true});
				var reports = new ReportsView({collection : report});
			}
		},

		searchMetar: function(search) {
			this.searchInput.val(search);
			$("#taf-mode").removeClass("mode-selected");
			$("#metar-mode").addClass("mode-selected")

			var metars = new MetarCollection([], { query : search });
			metars.fetch();
			var view = new ReportsView({collection : metars});

			view.once("afterRender", function(view) {
				// Now try and "enhance" the data with the more accurate location
				if (navigator.geolocation) {
					navigator.geolocation.getCurrentPosition(geoSuccess, geoFailure, { timeout: 5000 });
				}

				function geoSuccess(position) {
					var coords = { "geo" : position.coords.latitude + "," + position.coords.longitude };
					metars.fetch({update: true, data : coords});
					view.poll(coords);
					view.on("afterRender", function() { $('.report-position').removeClass("transparent"); })
				}

				function geoFailure() {
					$('.report-position').removeClass("transparent");
					view.poll();
				}
			});
		},

		taf: function(search) {
			this.searchInput.val(search);
			$("#taf-mode").addClass("mode-selected");
			$("#metar-mode").removeClass("mode-selected")

			var report = new TafCollection([], { query : search });
			var router = this;

			if (navigator.geolocation) {
				navigator.geolocation.getCurrentPosition(geoSuccess, geoFailure, { timeout: 5000 });
			}

			function geoSuccess(position) {
				report.fetch({update: true, data: { "latitude" : position.coords.latitude, "longitude" : position.coords.longitude }});
				var reports = new ReportsView({collection : report});
			}

			function geoFailure() {
				report.fetch({update : true});
				var reports = new ReportsView({collection : report});
			}
		},

		searchTaf: function(search) {
			this.searchInput.val(search);
			$("#taf-mode").addClass("mode-selected");
			$("#metar-mode").removeClass("mode-selected")

			var report = new TafCollection([], { query : search });
			var router = this;

			if (navigator.geolocation) {
				navigator.geolocation.getCurrentPosition(geoSuccess, geoFailure, { timeout: 5000 });
			}

			function geoSuccess(position) {
				report.fetch({update: true, data: { "latitude" : position.coords.latitude, "longitude" : position.coords.longitude }});
				var reports = new ReportsView({collection : report});
			}

			function geoFailure() {
				report.fetch({update : true});
				var reports = new ReportsView({collection : report});
			}
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
			if (searchValue) {
				this.router.navigate("m/" + searchValue, {trigger: true});
			} else {
				this.router.navigate("m", {trigger: true})
			}
		},

		showTafs: function() {
			var searchValue = $("#search").val();
			if (searchValue) {
				this.router.navigate("t/" + searchValue, {trigger: true});				
			} else {
				this.router.navigate("t", {trigger: true});
			}
		},

		search: function(e) {
			e.preventDefault();
			var searchValue = $("#search").val();
			var currentRoute = this.router.current().route;
			if (currentRoute == "metar") {
				this.router.navigate("m/" + searchValue, {trigger: true});
			} else if (currentRoute == "taf") {
				this.router.navigate("t/" + searchValue, {trigger: true});
			}
		},

		location: function() {
			var currentRoute = this.router.current().route;
			$("#search").val("")
			if (currentRoute == "metar") {
				this.router.navigate("m", {trigger: true});
			} else if (currentRoute == "taf") {
				this.router.navigate("t", {trigger: true});
			}
		}
	});
	
	var app = new AppView();
});