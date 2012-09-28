package com.solt.mediaplayer.mplayer.swt.windows;

import java.lang.reflect.Field;

import org.eclipse.swt.widgets.Canvas;

import com.solt.mediaplayer.mplayer.swt.MPlayerRendererInterface;


public class MPlayerRendererCanvasWindows implements MPlayerRendererInterface {
	
	int id;
	
	public MPlayerRendererCanvasWindows(Canvas canvas) {
		try {
			Class clazz = canvas.getClass();
			Field fId = clazz.getField("handle");
			
			id = (Integer) fId.get(canvas);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String[] getExtraMplayerOptions() {
		
			// when 'id' is negative mplayer fails to find the window - not easily
			// reproducible but guessing that we need to treat as unsigned 32 bit quantity
		
		long l_id = ((long)id)&0x00000000ffffffffL;
		
		return new String[] {"-vo","direct3d","-wid" , ""+l_id};
	}

}
