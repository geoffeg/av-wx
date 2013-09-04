var currentMetar;
var geolocationTimeout;
var expiryTimer;
var latestPosition;
function updateExpiryTimes(report, mode) {
  if (expiryTimer) clearInterval(expiryTimer);
  expiryTimer = setInterval(function() {
    for (var $i = 0; $i < report[mode].length; $i++) {
      $("#" + mode + "-" + report[mode][$i].icao + " div.position").html(getStationPosition(report[mode][$i]));
    }
  }, 5000);
}

function findByPosition(position) {
  clearTimeout(geolocationTimeout);
  var mode = getMode();
  if (position.coords.latitude && position.coords.longitude) {
    latestPosition = position;
    
    var urlParams = '?lat=' + position.coords.latitude + '&lon=' + position.coords.longitude + "&mode=" + mode;
    search(urlParams);
  } else {
    findByIP();
  }
}

function remoteLog(message) {
  $.ajax({
    url: '/log?message=' + message
  });
}

function findByPositionError(error) {
  clearTimeout(geolocationTimeout);
  if (error) {
    remoteLog("findByPositionError " + error.code + " " + error.message);
  }
  $("#loading-spinner").hide();
  findByIP();
}

function findByZipcode(zipcode) {
  var mode = getMode();
  var urlParams = "?zip=" + zipcode + "&mode=" + mode;
  search(urlParams);
}

function findByIP() {
  search("?mode=" + getMode());
}

function findByIcao(icao) {
  var mode = getMode();
  var urlParams = "?icao=" + icao + "&mode=" + mode;
  search(urlParams);
}

function search(urlParams) {
  $.ajax({
    dataType : 'json',
    url      : '/api.php' + urlParams,
    success  : showReports,
    context  : document,
    error    : errorMessage
  });
}

function errorMessage(data, status, errorText) {
  alert(data.responseText);
}

function getParameterByName(name){
  name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
  var regexS = "[\\?&]" + name + "=([^&#]*)";
  var regex = new RegExp(regexS);
  var results = regex.exec(window.location.search);
  if(results == null)
    return null;
  else
    return decodeURIComponent(results[1].replace(/\+/g, " "));
}

function getZuluTime() {
  var now = new Date();
  $("#current-time").html(now.getUTCHours() + ":" + now.getUTCMinutes() + "Z");
}

function showReports(data, status, jqxhr) {
  //window.history.replaceState("zipcode=" + data.zipcode, data.zipcode, '?zipcode=' + data.zipcode);
  //$("#zipcode").val(data.zipcode);
  $("#loading-spinner").hide();
  currentMetar = data;

  if (getMode() == "metar") {
    loadMetars(data, status, jqxhr);
  } else {
    loadTafs(data, status, jqxhr);
  }
}

function saveSearchInUrl() {
  var searchValue = $("#search").val();
  if (!window.history.replaceState) return;
  if (searchValue.length != 0) {
    window.history.replaceState("search=" + searchValue, searchValue, '?search=' + searchValue + "&mode=" + getMode());
  } else {
    window.history.replaceState("search=null", "search=null", "/");
  }
}

function loadMetars(data, status, jqxhr) {
  saveSearchInUrl();
  $("#metar-container UL").empty();

  for (var $i = 0; $i < data.metar.length; $i++) {
    position = getStationPosition(data.metar[$i]);
    $("#metar-container UL").append("<LI id='metar-" + data.metar[$i].icao + "' ><div class='report'>" + data.metar[$i].metar + "</div><div class='meta " +  data.metar[$i].cat + "'>" + data.metar[$i].cat + "</div><div class='position'>" + position + "</div></LI>");
  }
  updateExpiryTimes(data, "metar");
}

function loadTafs(data, status, jqxhr) {
  saveSearchInUrl();
  $("#metar-container UL").empty();

  for (var $i = 0; $i < data.taf.length; $i++) {
    position = getStationPosition(data.taf[$i]);
    $("#metar-container UL").append("<LI id='taf-" + data.taf[$i].icao + "'><div class='report'>" + data.taf[$i].taf + "</div><div class='position'>" + position + "</div></LI>");
  }
  updateExpiryTimes(data, "taf");
}

function getStationPosition(stationData) {
  var foo = Math.round(stationData.bearing/22.5);
  var points = ["N","NNE","NE","ENE","E","ESE", "SE", "SSE","S","SSW","SW","WSW","W","WNW","NW","NNW"];
  var age = parseInt(new Date().getTime() / 1000) - stationData.created;

  if ($("#search").val().length == 0 || $("#search").val().match(/\d/)) {
    return stationData.distance + " miles " + points[(foo % 16)] + " - " + parseInt(age / 60) + " mins old";
  } else {
    return parseInt(age / 60) + " mins old";
  }
}


function getMode() {
  if ($("#metar-mode").hasClass("mode-selected")) {
    return "metar";
  } else {
    return "taf";
  }
}

function loadReports() {
  $("#metar-container UL").empty();
  $("#loading-spinner").show();
  var searchValue = $("#search").val();
  if ( searchValue.length == 0 ) {
    // There's nothing in the search box, load via geo-location
    if (latestPosition) {
      findByPosition(latestPosition);
    } else {
      if (navigator.geolocation) {
        geolocationTimeout = setTimeout(findByPositionError, 6500);
        navigator.geolocation.getCurrentPosition(findByPosition, findByPositionError, { timeout: 6000 });
      } else {
        $("#loading-spinner").hide();
      }
    }
  } else if (searchValue.match(/\d/)) {
    findByZipcode(searchValue);
  } else if (searchValue.match(/\D/)) {
    findByIcao(searchValue);
  }
}

$(document).ready(function() {
  if (typeof console == "undefined") {
    window.console = {
      log: function () { }
      //log: function (m) { $.ajax({url: '/log.txt?m=' + m }); }
    };
  }

  var urlSearchParameter = getParameterByName("search");
  if (urlSearchParameter != null && urlSearchParameter.length > 0) {
    $("#search").val(getParameterByName("search"));
  }
  var urlModeParameter = getParameterByName("mode");
  if (urlModeParameter != null && (urlModeParameter == "metar" || urlModeParameter == "taf")) {
    $(".mode-selected").removeClass("mode-selected");
    $("#" + urlModeParameter + "-mode").toggleClass("mode-selected", true);
  }
  loadReports();
  $("form").submit(function (e) {
    e.preventDefault();
    loadReports();
  });
  $("#location-icon").click(function (e) {
    e.preventDefault();
    latestPosition = null;
    updateExpiryTimes();
    $("#search").val(undefined);
    loadReports();
  });
  $("#mode LI").click(function (e) {
    $("#metar-mode").toggleClass("mode-selected");
    $("#taf-mode").toggleClass("mode-selected");
    loadReports();
  });
  $("#reload-icon").click(function (e) {
    loadReports();

  });
});
