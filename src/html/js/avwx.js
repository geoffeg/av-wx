$(function(){

	// Models //
	var Metar = Backbone.Model.extend({
		url: function() {
			return "/api/metar/"  + encodeURIComponent(this.id) + "?geo=38.5817,-90.295"
		},
		initialize: function() {
			console.log('model init', this.attributes);
		},
		cardinal_direction: function() {
			var bearing = this.get("bearing_to");
			var direction = Math.round(bearing/22.5);
			var points = ["N","NNE","NE","ENE","E","ESE", "SE", "SSE","S","SSW","SW","WSW","W","WNW","NW","NNW"];
			return points[(direction % 16)];
		},
		toTemplate: function() {
			var j = this.toJSON();
			j.cardinal_direction = this.cardinal_direction();
			return j;
		}
	});

	var MetarCollection = Backbone.Collection.extend({
		model: Metar,
		url: "/api/metar/KSET,KSTL,KSUS?geo=38.5817,-90.295",
		parse: function(response, options){
			return response.reports;
		}
	});

	// Views //
	var MetarView = Backbone.View.extend({
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

	var MetarsView = Backbone.View.extend({
		tagName: 'ul',
		//el: "#metar-container",
		template: _.template($("#reports-template").html()),

		initialize: function() {
			this.collection.on("add", this.render, this);
			//this.render();
		},

		render: function() {
			this.collection.each(function(metar) {
				var metarView = new MetarView({model: metar, className: "foo-" + metar.attributes.station_id});
				this.$el.append(metarView.render().el);
			}, this);
			$('#metar-container').html(this.$el);
			return this;
		}
	});

	var AppRouter = Backbone.Router.extend({
		routes: {
			""          : "index",
			"metar/:id" : "search",
		},

		index: function() {
			console.log("index");
		},

		search: function(search) {
			var metar = new MetarCollection();
			metar.fetch();
			console.log(metar);
			var reports = new MetarsView({collection : metar});
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
			Backbone.history.start();
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