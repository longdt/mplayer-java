package com.solt.mediaplayer.mplayer.swt.osx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.eclipse.swt.SWT;
//import org.eclipse.swt.internal.cocoa.NSString;
//import org.eclipse.swt.internal.cocoa.NSView;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.solt.mediaplayer.mplayer.swt.MPlayerRendererInterface;


public class MPlayerRendererCanvasOSX implements MPlayerRendererInterface {
	
	String connectionName;
	
	/*
	 * The native id of the VideoOpenGLView
	 */
	int /*long*/ id;
	
	/*
	 * creates a VideoOpenGLView as a child of parent
	 */
	public static native int /*long*/ initWith(int /*long*/ parent, int /*long*/ connectionName);
	
	/*
	 * Called when we're done using the view so that the native code can clean things up
	 */
	public static native void dispose(int /*long*/ id);
	
	static{
		//Load the jnilib on demand
		try {
			//The jnilib is bundled as a resource, we first need to copy it to a file to make
			//it accessible by the OS
			InputStream is = MPlayerRendererCanvasOSX.class.getClassLoader().getResourceAsStream("com/vuze/swt/osx/lib/libMPlayerRendererCanvasOSX.jnilib");
			File fTemp = File.createTempFile("libMPlayerRendererCanvasOSX", ".jnilib");
			fTemp.deleteOnExit();
			
			FileOutputStream fos = new FileOutputStream(fTemp);
			
			byte[] buffer = new byte[4096];
			int len;
			
			while((len = is.read(buffer) ) != -1) {
				fos.write(buffer, 0, len);
			}
			fos.close();
			
			//Load the native library in the JVM
			System.load(fTemp.getAbsolutePath());
			
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public MPlayerRendererCanvasOSX(Canvas canvas) {
		
		synchronized (this.getClass()) {
			//Ugly but gets the job done
			this.connectionName = "vuze_" + System.currentTimeMillis();
			try {
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		try {
			Class clazz = canvas.getClass();
			Field fView = clazz.getField("view");
			
//			NSView view = (NSView) fView.get(canvas);
//			NSString nsConnectionName = NSString.stringWith(connectionName);
			
//			id = initWith(view.id,nsConnectionName.id);
			
			//When our parent gets disposed, we may need to clean a few things in the native code
			canvas.addListener(SWT.Dispose, new Listener() {
				public void handleEvent(Event arg0) {
					dispose(id);
				}
			});
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	public String[] getExtraMplayerOptions() {
		return new String[] {"-vo","corevideo:buffer_name=" + connectionName};
	}
	

}
