	var cordova = window.cordova || window.Cordova;
	var exec = require('cordova/exec');

	var CDV = {};

	CDV.FB = {};

	CDV.FB.init = function(apiKey, fail) {
		// create the fb-root element if it doesn't exist
		if (!document.getElementById('fb-root')) {
			var elem = document.createElement('div');
			elem.id = 'fb-root';
			document.body.appendChild(elem);
			console.log('element created')
		}
		exec(function() {
			var authResponse = JSON.parse(localStorage.getItem('cdv_fb_session') || '{"expiresIn":0}');
			if (authResponse && authResponse.expirationTime) { 
				var nowTime = (new Date()).getTime();
				if (authResponse.expirationTime > nowTime) { 
					// Update expires in information
					updatedExpiresIn = Math.floor((authResponse.expirationTime - nowTime) / 1000);
					authResponse.expiresIn = updatedExpiresIn;

					localStorage.setItem('cdv_fb_session', JSON.stringify(authResponse));
					FB.Auth.setAuthResponse(authResponse, 'connected');
				}
			}
			console.log('Cordova Facebook Connect plugin initialized successfully.');
		}, (fail?fail:null), 'ConnectPlugin', 'init', [apiKey]);
	};

	CDV.FB.login = function(params, cb, fail) {
		params = params || { scope: '' };
		exec(function(e) { // login
			if (e.authResponse && e.authResponse.expiresIn) {
				var expirationTime = e.authResponse.expiresIn === 0
				? 0 
						: (new Date()).getTime() + e.authResponse.expiresIn * 1000;
				e.authResponse.expirationTime = expirationTime; 
			}
			localStorage.setItem('cdv_fb_session', JSON.stringify(e.authResponse));
			FB.Auth.setAuthResponse(e.authResponse, 'connected');
			if (cb) cb(e);
		}, (fail?fail:null), 'ConnectPlugin', 'login', params.scope.split(',') );
	};

	CDV.FB.logout = function(cb, fail) {
		exec(function(e) {
			localStorage.removeItem('cdv_fb_session');
			FB.Auth.setAuthResponse(null, 'notConnected');
			if (cb) cb(e);
		}, (fail?fail:null), 'ConnectPlugin', 'logout', []);
	};

	CDV.FB.getLoginStatus = function(cb, fail) {
		exec(function(e) {
			if (cb) cb(e);
		}, (fail?fail:null), 'ConnectPlugin', 'getLoginStatus', []);
	};

	CDV.FB.dialog = function(params, cb, fail) {
		exec(function(e) { // login
			if (cb) cb(e);
		}, (fail?fail:null), 'ConnectPlugin', 'showDialog', [params] );
	};
	module.exports = CDV;

