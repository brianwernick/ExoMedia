/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devbrackets.android.exomedia.core.builder;

import android.annotation.TargetApi;
import android.media.MediaDrm.KeyRequest;
import android.media.MediaDrm.ProvisionRequest;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.util.Util;

import java.io.IOException;
import java.util.UUID;

/**
 * A {@link MediaDrmCallback} for Widevine test content.
 */
@TargetApi(18)
public class WidevineTestMediaDrmCallback implements MediaDrmCallback {

  private static final String WIDEVINE_GTS_DEFAULT_BASE_URI = "https://proxy.uat.widevine.com/proxy";

  private final String defaultUri;

  public WidevineTestMediaDrmCallback(String contentId, String provider) {
    String params = "?video_id=" + contentId + "&provider=" + provider;
    defaultUri = WIDEVINE_GTS_DEFAULT_BASE_URI + params;
    Log.i("WideVineTest", "WidevineTestMediaDrmCallback defaultUri: " + defaultUri);
  }

  @Override
  public byte[] executeProvisionRequest(UUID uuid, ProvisionRequest request) throws IOException {
    String url = request.getDefaultUrl() + "&signedRequest=" + new String(request.getData());
    return Util.executePost(url, null, null);
  }

  @Override
  public byte[] executeKeyRequest(UUID uuid, KeyRequest request) throws IOException {
    Log.i("WideVineTest", "request: " + request );
    String url = request.getDefaultUrl();
    if (TextUtils.isEmpty(url)) {
      url = defaultUri;
    }
    Log.i("WideVineTest", "url: " + url + " request: " + request);
    return Util.executePost(url, request.getData(), null);
  }

}
