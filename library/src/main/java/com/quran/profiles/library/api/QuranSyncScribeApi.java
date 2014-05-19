package com.quran.profiles.library.api;

import org.scribe.builder.api.DefaultApi20;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.extractors.JsonTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Verb;
import org.scribe.utils.OAuthEncoder;

/**
 * Quran Sync OAuth 2.0 provider
 * Created by ahmedre on 5/18/14.
 */
public class QuranSyncScribeApi extends DefaultApi20 {
  private static final String BASE_URL = "http://profiles.quran.com/oauth";
  private static final String AUTHORIZATION_URL = BASE_URL +
      "/authorize?client_id=%s&redirect_uri=%s&response_type=code";

  @Override
  public String getAccessTokenEndpoint() {
    return BASE_URL + "/token?grant_type=authorization_code";
  }

  @Override
  public String getAuthorizationUrl(OAuthConfig config) {
    return String.format(AUTHORIZATION_URL, config.getApiKey(),
        OAuthEncoder.encode(config.getCallback()));
  }

  @Override
  public Verb getAccessTokenVerb() {
    return Verb.POST;
  }

  @Override
  public AccessTokenExtractor getAccessTokenExtractor() {
    return new JsonTokenExtractor();
  }
}
