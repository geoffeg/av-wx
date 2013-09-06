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
}