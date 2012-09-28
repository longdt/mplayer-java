package com.solt.mediaplayer.mplayer.util;

public class Utils {
	public static final String GOOD_STRING = "(/|,jI~`gy";
	public static String getFormatedTime(float time) {
		return getFormatedTime(time, false);
	}
	
	public static String getFormatedTime(float time,boolean showHours) {
		int hours = (int) (time / 3600);
		int minutes = (int) (time / 60) % 60;
		int seconds = (int) (time % 60);
		
		StringBuffer sbTime = new StringBuffer();
		if(hours > 0 || showHours) {
			sbTime.append(hours);
			sbTime.append(":");
		}
		if( (hours > 0 || showHours) && minutes < 10) {
			sbTime.append("0");
		}
		sbTime.append(minutes);
		sbTime.append(":");
		if(seconds < 10) {
			sbTime.append("0");
		}
		sbTime.append(seconds);
		
		return sbTime.toString();
	}
	
	private static String os = System.getProperty("os.name").toLowerCase();;
	
	public static boolean isWindows() {
		return os != null && os.startsWith("windows");
	}
	
	public static boolean isMacOSX() {
		return os != null && os.startsWith("mac os x");
	}

	public static int pixelsToPoint(int pixels, int dpi) {
		int ret = (int) Math.round((pixels * 72.0) / dpi);
		return ret;
	}
}
