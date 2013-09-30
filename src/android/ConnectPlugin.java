package org.apache.cordova.facebook;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.JSONUtils;
import org.apache.cordova.PluginResult;
import org.apache.cordova.facebook.Facebook.DialogListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;


public class ConnectPlugin extends CordovaPlugin {

    public static final String SINGLE_SIGN_ON_DISABLED = "service_disabled";
    private final String TAG = "ConnectPlugin";

    private Facebook facebook;
    private String userId;
    //used for dialog auth
    private String[] permissions = new String[] {};
    private Bundle paramBundle;
    private String method;
    public CallbackContext cb;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext)throws JSONException {
    	this.cb = callbackContext;
        PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
        pr.setKeepCallback(true);

        if (action.equals("init")) {
            try {
                String appId = args.getString(0);

                facebook = new Facebook(appId);

                Log.d(TAG, "init: Initializing plugin.");

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());
                String access_token = prefs.getString("access_token", null);
                Long expires = prefs.getLong("access_expires", -1);

                if (access_token != null && expires != -1) {
                    this.facebook.setAccessToken(access_token);
                    this.facebook.setAccessExpires(expires);
                	  try {
                        JSONObject o = new JSONObject(this.facebook.request("/me"));
                        this.userId = o.getString("id");
                    } catch (MalformedURLException e) {
                       
                        e.printStackTrace();
                    } catch (IOException e) {
                       
                        e.printStackTrace();
                    } catch (JSONException e) {
                       
                        e.printStackTrace();
                    }
                }

                if(facebook.isSessionValid() && this.userId != null) {
                    //return new PluginResult(PluginResult.Status.OK, this.getResponse());
                    pr = new PluginResult(PluginResult.Status.OK, this.getResponse());
                    callbackContext.sendPluginResult(pr);
                }
                else {
                	pr = new PluginResult(PluginResult.Status.NO_RESULT);
                	//return new PluginResult(PluginResult.Status.NO_RESULT);
                	callbackContext.sendPluginResult(pr);
                }
            } catch (JSONException e) {
               
                e.printStackTrace();
                pr = new PluginResult(PluginResult.Status.ERROR, "Invalid JSON args used. expected a string as the first arg.");
                callbackContext.sendPluginResult(pr);
            }
            return true;
        }

        else if (action.equals("login")) {
            if (facebook != null) {
            	final ConnectPlugin me = this;
                String[] permissions = new String[args.length()];
                try {
                    for (int i=0; i<args.length(); i++) {
                        permissions[i] = args.getString(i);
                    }
                } catch (JSONException e1) {
                   
                    e1.printStackTrace();
                    pr =  new PluginResult(PluginResult.Status.ERROR, "Invalid JSON args used. Expected a string array of permissions.");
                    //return new PluginResult(PluginResult.Status.ERROR, "Invalid JSON args used. Expected a string array of permissions.");
                    callbackContext.sendPluginResult(pr);
                }
                cordova.setActivityResultCallback(this);
//                this.ctx.setActivityResultCallback(this);
                this.permissions = permissions;
                Runnable runnable = new Runnable() {
                    public void run() {
                        me.facebook.authorize(cordova.getActivity(), me.permissions, new AuthorizeListener(me));
                    };
                };
                cordova.getActivity().runOnUiThread(runnable);
//                this.ctx.runOnUiThread(runnable);
            } else {
                pr = new PluginResult(PluginResult.Status.ERROR, "Must call init before login.");
                callbackContext.sendPluginResult(pr);
            }
            return true;
        }

        else if (action.equals("logout")) {
            if (facebook != null) {
                try {
                    facebook.logout(cordova.getActivity());

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.cordova.getActivity());
                    prefs.edit().putLong("access_expires", -1).commit();
                    prefs.edit().putString("access_token", null).commit();
                } catch (MalformedURLException e) {
                   
                    e.printStackTrace();
                    pr = new PluginResult(PluginResult.Status.MALFORMED_URL_EXCEPTION, "Error logging out.");
                    callbackContext.sendPluginResult(pr);
                } catch (IOException e) {
                   
                    e.printStackTrace();
                    pr = new PluginResult(PluginResult.Status.IO_EXCEPTION, "Error logging out.");
                    callbackContext.sendPluginResult(pr);
                }
                pr = new PluginResult(PluginResult.Status.OK, getResponse());
                callbackContext.sendPluginResult(pr);
            } else {
                pr = new PluginResult(PluginResult.Status.ERROR, "Must call init before logout.");
                callbackContext.sendPluginResult(pr);
            }
            return true;
        }

        else if (action.equals("getLoginStatus")) {
            if (facebook != null) {
                pr = new PluginResult(PluginResult.Status.OK, getResponse());
                callbackContext.sendPluginResult(pr);
            } else {
                pr = new PluginResult(PluginResult.Status.ERROR, "Must call init before getLoginStatus.");
                callbackContext.sendPluginResult(pr);
            }
            return true;
        }
        
        else if (action.equals("showDialog")) {
        	if (facebook != null) {
        		Bundle collect = new Bundle();
        		JSONObject params = null;
        		try {
        			params = args.getJSONObject(0);
        		} catch (JSONException e) {
        			params = new JSONObject();
        		}
        		
        		final ConnectPlugin me = this;
        		Iterator<?> iter = params.keys();
        		while (iter.hasNext()) {
        			String key = (String) iter.next();
        			if (key.equals("method")) {
        				try {
        					this.method = params.getString(key);
        				} catch (JSONException e) {
        					Log.w(TAG, "Nonstring method parameter provided to dialog");
        				}
        			} else {
        				try {
        					collect.putString(key, params.getString(key));
        				} catch (JSONException e) {
        					// Need to handle JSON parameters
        					Log.w(TAG, "Nonstring parameter provided to dialog discarded");
        				}
        			}
        		}
        		this.paramBundle =  new Bundle(collect);
        		Runnable runnable = new Runnable() {
        			public void run() {
        				me.facebook.dialog (me.cordova.getActivity(), me.method , me.paramBundle , new UIDialogListener(me));
        			};
        		};
        		cordova.getActivity().runOnUiThread(runnable);
//        		this.ctx.runOnUiThread(runnable);
        	} else {
        		pr = new PluginResult(PluginResult.Status.ERROR, "Must call init before showDialog.");
        		callbackContext.sendPluginResult(pr);
        	}
        	return true;
        }

        //return pr;
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        facebook.authorizeCallback(requestCode, resultCode, data);
    }

    public JSONObject getResponse() {
    	String response;
    	if (facebook.isSessionValid()) {
    		long expiresTimeInterval = facebook.getAccessExpires() - System.currentTimeMillis();
    		long expiresIn = (expiresTimeInterval > 0) ? expiresTimeInterval : 0;
    		response = "{"+
            "\"status\": \"connected\","+
            "\"authResponse\": {"+
              "\"accessToken\": \""+facebook.getAccessToken()+"\","+
              "\"expiresIn\": \""+expiresIn+"\","+
              "\"session_key\": true,"+
              "\"sig\": \"...\","+
              "\"userId\": \""+this.userId+"\""+
            "}"+
          "}";
    	} else {
    		response = "{"+
            "\"status\": \"unknown\""+
          "}";
    	}

        try {
            return new JSONObject(response);
        } catch (JSONException e) {
           
            e.printStackTrace();
        }
        return new JSONObject();
    }
    
    class UIDialogListener implements DialogListener {
   	 final ConnectPlugin fba;

		public UIDialogListener(ConnectPlugin fba){
			super();
			this.fba = fba;
		}

		public void onComplete(Bundle values) {
			//  Handle a successful dialog
			Log.d(TAG,values.toString());
			//this.fba.success(new PluginResult(PluginResult.Status.OK), this.fba.callbackId);
			this.fba.cb.sendPluginResult(new PluginResult(PluginResult.Status.OK));
		}

		public void onFacebookError(FacebookError e) {
           Log.d(TAG, "facebook error");
           this.fba.cb.error("Facebook error: " + e.getMessage());
       }

       public void onError(DialogError e) {
           Log.d(TAG, "other error");
           this.fba.cb.error("Dialog error: " + e.getMessage());
       }

       public void onCancel() {
           Log.d(TAG, "cancel");
           this.fba.cb.error("Cancelled");
       }
	}

    class AuthorizeListener implements DialogListener {
        final ConnectPlugin fba;

        public AuthorizeListener(ConnectPlugin fba){
            super();
            this.fba = fba;
        }

        public void onComplete(Bundle values) {
            //  Handle a successful login

            String token = this.fba.facebook.getAccessToken();
            long token_expires = this.fba.facebook.getAccessExpires();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.fba.cordova.getActivity());
            prefs.edit().putLong("access_expires", token_expires).commit();
            prefs.edit().putString("access_token", token).commit();

          	Log.d(TAG, "authorized");
            Log.d(TAG, values.toString());

            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        JSONObject o = new JSONObject(fba.facebook.request("/me"));
                        fba.userId = o.getString("id");
                        fba.cb.success(getResponse());
                    } catch (MalformedURLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        }

        public void onFacebookError(FacebookError e) {
            Log.d(TAG, "facebook error");
            this.fba.cb.error("Facebook error: " + e.getMessage());
        }

        public void onError(DialogError e) {
            Log.d(TAG, "other error");
            this.fba.cb.error("Dialog error: " + e.getMessage());
        }

        public void onCancel() {
            Log.d(TAG, "cancel");
            this.fba.cb.error("Cancelled");
        }
    }
}
