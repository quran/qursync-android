package com.quran.profiles.library;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.quran.profiles.library.api.QuranSyncApi;
import com.quran.profiles.library.api.QuranSyncScribeApi;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthConstants;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import android.os.Handler;
import android.os.Message;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

/**
 * Api wrapper for QuranSync
 * Created by ahmedre on 5/18/14.
 */
public class QuranSync {
  public static final String API_BASE = "http://profiles.quran.com/api/v1/";
  public static final int MSG_TOKEN_RECEIVED = 1;

  private static QuranSync sInstance;
  private volatile Token mToken;
  private OAuthService mService;
  private QuranSyncApi mApi;
  private final Object mLock = new Object();

  public static synchronized QuranSync getInstance(
      String apiKey, String apiSecret, String callback) {
    if (sInstance == null) {
      sInstance = new QuranSync(apiKey, apiSecret, callback);
    }
    return sInstance;
  }

  private QuranSync(String apiKey, String apiSecret, String callback) {
    mService = new ServiceBuilder()
        .provider(QuranSyncScribeApi.class)
        .apiKey(apiKey)
        .apiSecret(apiSecret)
        .callback(callback)
        .build();

    Gson gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();
    RestAdapter restAdapter = new RestAdapter.Builder()
        .setEndpoint(API_BASE)
        .setConverter(new GsonConverter(gson))
        .setRequestInterceptor(new RequestInterceptor() {
          @Override
          public void intercept(RequestFacade request) {
            request.addQueryParam(
                OAuthConstants.ACCESS_TOKEN, mToken.getToken());
          }
        }).build();
    mApi = restAdapter.create(QuranSyncApi.class);
  }

  public void setToken(String token) {
    synchronized (mLock) {
      mToken = new Token(token, "");
    }
  }

  public void clearToken() {
    synchronized (mLock) {
      mToken = null;
    }
  }

  public boolean isAuthorized() {
    synchronized (mLock) {
      return mToken != null;
    }
  }

  public String getAuthorizationUrl() {
    return mService.getAuthorizationUrl(null);
  }

  public void getAccessToken(final String code, final Handler callback) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        final Token token = mService.getAccessToken(null, new Verifier(code));
        synchronized (mLock) {
          mToken = token;
        }

        if (callback != null) {
          final Message message =
              callback.obtainMessage(MSG_TOKEN_RECEIVED, token.getToken());
          callback.sendMessage(message);
        }
      }
    }).start();
  }

  public QuranSyncApi getApi() {
    if (isAuthorized()) {
      return mApi;
    } else {
      return null;
    }
  }
}
