package net.bieli.HomeAutomation.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.UUID;

public class DeviceIdFactory {

    protected static final String PREFS_FILE = "device_id.xml";
    protected static final String PREFS_DEVICE_ID = "device_id";
    protected volatile static UUID uuid;

    public DeviceIdFactory(Context context) {
        if (uuid == null) {
            synchronized (DeviceIdFactory.class) {
                if (uuid == null) {
                    final SharedPreferences prefs = context
                            .getSharedPreferences(PREFS_FILE, 0);
                    final String id = prefs.getString(PREFS_DEVICE_ID, null);
                    if (id != null) {
                        // Use the ids previously computed and stored in the
                        // prefs file
                        uuid = UUID.fromString(id);
                    } else {
                        final String androidId = Secure.getString(
                            context.getContentResolver(), Secure.ANDROID_ID);
                        
                        // Use the Android ID unless it's broken, in which case
                        // fallback on deviceId,
                        // unless it's not available, then fallback on a random
                        // number which we store to a prefs file
                        try {
                            if (!"9774d56d682e549c".equals(androidId)) {
                                uuid = UUID.nameUUIDFromBytes(androidId
                                        .getBytes("utf8"));
                            } else {
                                final String deviceId = (
                                    (TelephonyManager) context
                                    .getSystemService(Context.TELEPHONY_SERVICE))
                                    .getDeviceId();
                                uuid = deviceId != null ? UUID
                                    .nameUUIDFromBytes(deviceId
                                            .getBytes("utf8")) : UUID
                                    .randomUUID();
                            }
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                        // Write the value out to the prefs file
                        prefs.edit()
                                .putString(PREFS_DEVICE_ID, uuid.toString())
                                .commit();
                    }
                }
            }
        }
    }

    /**
     * Returns a unique UUID for the current android device. As with all UUIDs,
     * this unique ID is "very highly likely" to be unique across all Android
     * devices. Much more so than ANDROID_ID is.
     * 
     * The UUID is generated by using ANDROID_ID as the base key if appropriate,
     * falling back on TelephonyManager.getDeviceID() if ANDROID_ID is known to
     * be incorrect, and finally falling back on a random UUID that's persisted
     * to SharedPreferences if getDeviceID() does not return a usable value.
     * 
     * In some rare circumstances, this ID may change. In particular, if the
     * device is factory reset a new device ID may be generated. In addition, if
     * a user upgrades their phone from certain buggy implementations of Android
     * 2.2 to a newer, non-buggy version of Android, the device ID may change.
     * Or, if a user uninstalls your app on a device that has neither a proper
     * Android ID nor a Device ID, this ID may change on reinstallation.
     * 
     * Note that if the code falls back on using TelephonyManager.getDeviceId(),
     * the resulting ID will NOT change after a factory reset. Something to be
     * aware of.
     * 
     * Works around a bug in Android 2.2 for many devices when using ANDROID_ID
     * directly.
     * 
     * @see http://code.google.com/p/android/issues/detail?id=10603
     * 
     * @return a UUID that may be used to uniquely identify your device for most
     *         purposes.
     */
    public UUID getDeviceUuid() {
        return uuid;
    }
    
    public static String getImei(Context context) {
        TelephonyManager manager = 
        		(TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE); 

    	return manager.getDeviceId(); 
    }
    
    public static String getInternalAndroidId() {
    	String serial = ""; 

    	try {
    	    Class<?> c = Class.forName("android.os.SystemProperties");
    	    Method get = c.getMethod("get", String.class);
    	    serial = (String) get.invoke(c, "ro.serialno");
    	} catch (Exception ignored) {
    	}
    	
    	return serial;
    }
    

/*	private static String getDeviceId(Context context) {
//		WifiManager wm = (WifiManager) getBaseContext().getSystemService(Context.WIFI_SERVICE);
//		String mac = wm.getConnectionInfo().getMacAddress();
//		
//        Log.v(LOG_TAG, "getDeviceId getMacAddress" + mac);
//		
//		return mac; 
				
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;

        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(
    		androidId.hashCode(),
    		((long) tmDevice.hashCode() << 32) | tmSerial.hashCode()
		);

        return deviceUuid.toString();
	}*/
	
	/**
	 * Return pseudo unique ID
	 * @return ID
	 */
	public static String getUniquePsuedoID()
	{
	    // If all else fails, if the user does have lower than API 9 (lower
	    // than Gingerbread), has reset their phone or 'Secure.ANDROID_ID'
	    // returns 'null', then simply the ID returned will be solely based
	    // off their Android device information. This is where the collisions
	    // can happen.
	    // Thanks http://www.pocketmagic.net/?p=1662!
	    // Try not to use DISPLAY, HOST or ID - these items could change.
	    // If there are collisions, there will be overlapping data
	    String m_szDevIDShort = "35" + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10) + (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10) + (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10) + (Build.PRODUCT.length() % 10);

	    // Thanks to @Roman SL!
	    // http://stackoverflow.com/a/4789483/950427
	    // Only devices with API >= 9 have android.os.Build.SERIAL
	    // http://developer.android.com/reference/android/os/Build.html#SERIAL
	    // If a user upgrades software or roots their phone, there will be a duplicate entry
	    String serial = null;
	    try
	    {
	        serial = android.os.Build.class.getField("SERIAL").get(null).toString();

	        // Go ahead and return the serial for api => 9
	        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
	    }
	    catch (Exception e)
	    {
	        // String needs to be initialized
	        serial = "serial"; // some value
	    }

	    // Thanks @Joe!
	    // http://stackoverflow.com/a/2853253/950427
	    // Finally, combine the values we have found by using the UUID class to create a unique identifier
	    return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
	}
	
	public static String getToken(Context context) {
//    	String token = DeviceIdFactory.getInternalAndroidId();
//
//    	if (token.length() == 0) {
//    		token = DeviceIdFactory.getImei(context);
//
//    		if (token.length() == 0) {
//    			DeviceIdFactory uid = new DeviceIdFactory(context);
//    			token = uid.getDeviceUuid().toString(); 
//    			//TODO: need refactor
//    			//getUniquePsuedoID(); //getDeviceId();
//        	}
//    	}

		String token = DeviceIdFactory.getImei(context);

    	try
    	{
    		token = Long.toString(Long.parseLong(token), 36);
    	} catch (Exception e) {
    		token = "_token_";
    	}
    	
    	return token;
	}
	
	public static String getShortToken(Context context) {
		String shortToken = "35" +
            Build.BOARD.length()%10 + Build.BRAND.length()%10 +
            Build.CPU_ABI.length()%10 + Build.DEVICE.length()%10 +
            Build.DISPLAY.length()%10 + Build.HOST.length()%10 +
            Build.ID.length()%10 + Build.MANUFACTURER.length()%10 +
            Build.MODEL.length()%10 + Build.PRODUCT.length()%10 +
            Build.TAGS.length()%10 + Build.TYPE.length()%10 +
            Build.USER.length()%10;

    	try
    	{
    		shortToken = Long.toString(Long.parseLong(shortToken), 36);
    	} catch (Exception e) {
    		shortToken = "_token_";
    	}

    	return shortToken;
	}
}