package com.devbrackets.android.exomedia.widevine;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.drm.DrmErrorEvent;
import android.drm.DrmEvent;
import android.drm.DrmInfo;
import android.drm.DrmInfoEvent;
import android.drm.DrmInfoRequest;
import android.drm.DrmManagerClient;
import android.drm.DrmStore;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class I3Widevine {

	public static final String TAG = "I3Widevine";

	/**
	 * Settings
	 */

	// public static String ASSET_URI =
	// "http://commondatastorage.googleapis.com/wvmedia/tears_high_1080p_4br_tp.wvm";
	// public static String DRM_SERVER_URI =
	// "https://staging.shibboleth.tv/widevine/cypherpc/cgi-bin/GetEMMs.cgi";
	// public static String PORTAL_NAME = "OEM";

	// public static final String DRM_SERVER_URI =
	// "http://192.168.110.90/widevine/cgi-bin/GetEMMs.cgi";
	public static final String DRM_SERVER_URI = "http://video.i3television.es/proxy_widevine/antena3";
	public static String PORTAL_NAME = "antena3";

	public static final String WIDEVINE_MIME_TYPE = "video/wvm";

	private DrmManagerClient mDrmManager;

	/**
	 * Manages Widevine DRM
	 * 
	 * @param context
	 */
	public I3Widevine(Context context) {
		mDrmManager = new DrmManagerClient(context);

		mDrmManager.setOnInfoListener(new DrmManagerClient.OnInfoListener() {
			// @Override
			public void onInfo(DrmManagerClient client, DrmInfoEvent event) {
				if (event.getType() == DrmInfoEvent.TYPE_RIGHTS_INSTALLED) {
					Log.i(TAG, "Rights installed");
				}
			}
		});

		mDrmManager.setOnEventListener(new DrmManagerClient.OnEventListener() {

			public void onEvent(DrmManagerClient client, DrmEvent event) {
				switch (event.getType()) {
				case DrmEvent.TYPE_DRM_INFO_PROCESSED:
					Log.i(TAG, "Info Processed");
					break;
				case DrmEvent.TYPE_ALL_RIGHTS_REMOVED:
					Log.i(TAG, "All rights removed");
					break;
				}
			}
		});

		mDrmManager.setOnErrorListener(new DrmManagerClient.OnErrorListener() {
			public void onError(DrmManagerClient client, DrmErrorEvent event) {
				switch (event.getType()) {
				case DrmErrorEvent.TYPE_NO_INTERNET_CONNECTION:
					Log.i(TAG, "No Internet Connection");
					break;
				case DrmErrorEvent.TYPE_NOT_SUPPORTED:
					Log.i(TAG, "Not Supported");
					break;
				case DrmErrorEvent.TYPE_OUT_OF_MEMORY:
					Log.i(TAG, "Out of Memory");
					break;
				case DrmErrorEvent.TYPE_PROCESS_DRM_INFO_FAILED:
					Log.i(TAG, "Process DRM Info failed");
					break;
				case DrmErrorEvent.TYPE_REMOVE_ALL_RIGHTS_FAILED:
					Log.i(TAG, "Remove All Rights failed");
					break;
				case DrmErrorEvent.TYPE_RIGHTS_NOT_INSTALLED:
					Log.i(TAG, "Rights not installed");
					break;
				case DrmErrorEvent.TYPE_RIGHTS_RENEWAL_NOT_ALLOWED:
					Log.i(TAG, "Rights renewal not allowed");
					break;
				}
			}
		});
	}

	public DrmInfoRequest getDrmInfoRequest(String assetUri) {
		DrmInfoRequest rightsAcquisitionInfo = null;

		try {
			rightsAcquisitionInfo = new DrmInfoRequest(DrmInfoRequest.TYPE_RIGHTS_ACQUISITION_INFO, WIDEVINE_MIME_TYPE);

			rightsAcquisitionInfo.put("WVDRMServerKey", DRM_SERVER_URI);
			rightsAcquisitionInfo.put("WVAssetURIKey", assetUri);
			rightsAcquisitionInfo.put("WVDeviceIDKey", getSerialNumber());
			rightsAcquisitionInfo.put("WVPortalKey", PORTAL_NAME);
			// rightsAcquisitionInfo.put("WVCAUserDataKey", USER_DATA);
		} catch (Exception e) {
			Log.e(TAG, "error getting drm info", e);
		}

		return rightsAcquisitionInfo;
	}

  public static String getSerialNumber() {
	String serialNumber = "";

	try {
	  Class<?> c = Class.forName("android.os.SystemProperties");
	  Method get = c.getMethod("get", String.class, String.class);
	  serialNumber = (String) (get.invoke(c, "ro.serialno", "unknown"));
	} catch (Exception e) {
	  Log.e(TAG, "error getting serial number", e);
	}

	return serialNumber;
  }

	public int checkRightsStatus(String assetUri) {
		// Need to use acquireDrmInfo prior to calling checkRightsStatus
		mDrmManager.acquireDrmInfo(getDrmInfoRequest(assetUri));
		int status = mDrmManager.checkRightsStatus(assetUri);
		Log.i(TAG, "deviceId=" + getSerialNumber() + " checkRightsStatus  = " + status);

		return status;
	}

	public int acquireRights(String assetUri) {
		DrmInfoRequest rightsAcquisitionInfo = getDrmInfoRequest(assetUri);
		int rights = mDrmManager.acquireRights(rightsAcquisitionInfo);
		Log.i(TAG, "deviceId=" + getSerialNumber() + " acquireRights=" + rights);

		return rights;
	}

	public int removeRights(String assetUri) {
		// Need to use acquireDrmInfo prior to calling removeRights
		mDrmManager.acquireDrmInfo(getDrmInfoRequest(assetUri));
		int removeStatus = mDrmManager.removeRights(assetUri);
		Log.i(TAG, "removeRights = " + removeStatus);

		return removeStatus;
	}

	public int removeAllRights() {
		int removeAllStatus = mDrmManager.removeAllRights();

		Log.i(TAG, "removeAllRights = " + removeAllStatus);

		return removeAllStatus;
	}

	public void showRights(String assetUri) {
		Log.i(TAG, "showRights");

		// Need to use acquireDrmInfo prior to calling getConstraints
		mDrmManager.acquireDrmInfo(getDrmInfoRequest(assetUri));
		ContentValues values = mDrmManager.getConstraints(assetUri, DrmStore.Action.PLAY);

		if (values != null) {
			Set<String> keys = values.keySet();

			// Locale locale = new Locale("es");
			Locale locale = Locale.getDefault();

			for (String key : keys) {
				// time
				if (key.toLowerCase(locale).contains("time")) {
					Log.i(TAG, key + " = " + SecondsToDHMS(values.getAsLong(key)));
				}
				// license type
				else if (key.toLowerCase(locale).contains("licensetype")) {
					Log.i(TAG, key + " = " + licenseType(values.getAsInteger(key)));
				}
				// licensed resolution
				else if (key.toLowerCase(locale).contains("licensedresolution")) {
					Log.i(TAG, key + " = " + licenseResolution(values.getAsInteger(key)));
				}
				// any
				else {
					Log.i(TAG, key + " = " + values.get(key));
				}
			}
		} else {
			Log.i(TAG, "No Rights");
		}
	}

	/**
	 * rights
	 */

	private static final long seconds_per_minute = 60;
	private static final long seconds_per_hour = 60 * seconds_per_minute;
	private static final long seconds_per_day = 24 * seconds_per_hour;

	private String SecondsToDHMS(long seconds) {
		int days = (int) (seconds / seconds_per_day);
		seconds -= days * seconds_per_day;
		int hours = (int) (seconds / seconds_per_hour);
		seconds -= hours * seconds_per_hour;
		int minutes = (int) (seconds / seconds_per_minute);
		seconds -= minutes * seconds_per_minute;
		return Integer.toString(days) + "d " + Integer.toString(hours) + "h " + Integer.toString(minutes) + "m " + Long.toString(seconds)
				+ "s";
	}

	private String licenseType(int code) {
		switch (code) {
		case 1:
			return "Streaming";
		case 2:
			return "Offline";
		case 3:
			return "Both";
		default:
			return "Unknown";
		}
	}

	private String licenseResolution(int code) {
		switch (code) {
		case 1:
			return "SD only";
		case 2:
			return "HD or SD content";
		default:
			return "Unknown";
		}
	}

	/**
	 * provision
	 */

	private final static long DEVICE_IS_PROVISIONED = 0;
	@SuppressWarnings("unused")
	private final static long DEVICE_IS_NOT_PROVISIONED = 1;
	private final static long DEVICE_IS_PROVISIONED_SD_ONLY = 2;
	private long mWVDrmInfoRequestStatusKey = DEVICE_IS_PROVISIONED;

	public void registerPortal(String portal) {
		DrmInfoRequest request = new DrmInfoRequest(DrmInfoRequest.TYPE_REGISTRATION_INFO, WIDEVINE_MIME_TYPE);
		request.put("WVPortalKey", portal);
		DrmInfo response = mDrmManager.acquireDrmInfo(request);

		String drmInfoRequestStatusKey = (String) response.get("WVDrmInfoRequestStatusKey");
		if (null != drmInfoRequestStatusKey && !drmInfoRequestStatusKey.equals("")) {
			mWVDrmInfoRequestStatusKey = Long.parseLong(drmInfoRequestStatusKey);
		}
	}

	public boolean isProvisionedDevice() {
		return ((mWVDrmInfoRequestStatusKey == DEVICE_IS_PROVISIONED) || (mWVDrmInfoRequestStatusKey == DEVICE_IS_PROVISIONED_SD_ONLY));
	}

}
