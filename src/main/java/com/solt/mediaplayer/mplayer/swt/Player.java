package com.solt.mediaplayer.mplayer.swt;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import com.solt.mediaplayer.mplayer.Language;
import com.solt.mediaplayer.mplayer.LanguageSource;
import com.solt.mediaplayer.mplayer.MPlayer;
import com.solt.mediaplayer.mplayer.MediaPlaybackState;
import com.solt.mediaplayer.mplayer.MetaDataListener;
import com.solt.mediaplayer.mplayer.StateListener;
import com.solt.mediaplayer.mplayer.util.Utils;
import com.sun.org.apache.bcel.internal.Constants;

public class 
Player 
{
	private Composite			parent;
	private Display 			display;
	private MPlayerFrame 		playerFrame;
	private FullScreenControls 	controls;		
	
	private boolean overControls;
	private boolean isPaused;
	
	private BufferingControls	bufferingControls;
	
	private Thread hideThread;
	
	private long lastMoveTime;
	private Object hideThreadWait = new Object();
	
	private Cursor hiddenCursor;
	private Rectangle controlsSize;
	
	private Listener keyListener;
	private boolean autoResize;
	
	
	public Player(final Composite _parent) {

		parent	= _parent;
		display = parent.getDisplay();
		playerFrame = new MPlayerFrame(parent);
		controls = new FullScreenControls(playerFrame, parent.getShell());
		//playerFrame.setControls(controls.getRealShell());
		controlsSize = controls.getShell().getBounds();
		
		bufferingControls = new BufferingControls(playerFrame, parent.getShell());
			
		Color white = display.getSystemColor (SWT.COLOR_WHITE);
		Color black = display.getSystemColor (SWT.COLOR_BLACK);
		PaletteData palette = new PaletteData (new RGB [] {white.getRGB(), black.getRGB()});
		ImageData sourceData = new ImageData (16, 16, 1, palette);
		sourceData.transparentPixel = 0;
		hiddenCursor = new Cursor(display, sourceData, 0, 0);
		
		lastMoveTime = System.currentTimeMillis();
		
		hideThread = new Thread("auto hide controls") {
			private boolean	destroyed;
			
			public void run() {
				try {
					display.asyncExec(
						new Runnable() 
						{
							public void
							run()
							{
								if ( parent.isDisposed()){
									
									synchronized (hideThreadWait){
										
										destroyed = true;
										
										hideThreadWait.notifyAll();
									}
								}else{
									parent.addDisposeListener(
										new DisposeListener()
										{
											public void 
											widgetDisposed(
												DisposeEvent arg0) 
											{
												synchronized (hideThreadWait) {
													destroyed = true;
													
													hideThreadWait.notifyAll();
												}
											}
											
										});
								}
							}
						});
					
					while(!parent.isDisposed()) {						
						if(overControls || isPaused) {
							synchronized (hideThreadWait) {
								if ( destroyed ){
									return;
								}
								hideThreadWait.wait();
							}
						} else {
							long currentTime = System.currentTimeMillis();
							long delta = currentTime - lastMoveTime;
							long waitFor = 2000 - delta;
	
							if(waitFor > 0) {							
									Thread.sleep(waitFor);							
							} else {
								controls.hide();
								hideCursor();								
								synchronized (hideThreadWait) {
									if ( destroyed ){
										return;
									}
									hideThreadWait.wait();
								}
								
							}
						}
					}
					

				} catch (Exception e) {
					e.printStackTrace();
				}
			
			};
		};
		
		hideThread.setDaemon(true);
		hideThread.start();
		
		playerFrame.addMetaDataListener(new MetaDataListener() {
			
			public void receivedVideoResolution(final int width, final int height) {
				
			}
			
			public void receivedDuration(float durationInSecs) {
				// TODO Auto-generated method stub
				
			}
			
			public void receivedDisplayResolution( int width, int height) {
				if(autoResize) {
					setSize( width, height );
				}
			}
			
			public void foundSubtitle(Language language) {
				// TODO Auto-generated method stub
				
			}
			
			public void foundAudioTrack(Language language) {
				// TODO Auto-generated method stub
				
			}
			
			public void activeSubtitleChanged(String subtitleId,LanguageSource source) {
				// TODO Auto-generated method stub
				
			}
			
			public void activeAudioTrackChanged(String audioTrackId) {
				// TODO Auto-generated method stub
				
			}
		});
		
		Listener move_listener = 
			new Listener() {
				public void handleEvent(Event evt) {
					overControls = false;
					controls.show();
					playerFrame.setCursor(null);
					controls.setCursor(null);
					lastMoveTime = System.currentTimeMillis();
					synchronized (hideThreadWait) {
						hideThreadWait.notify();
					}				
				}
			};
		
				// hook into both so that playback without video (i.e. audio)  works as playerFame isn't picking up events in that case
			
		parent.addListener(SWT.MouseMove, move_listener );
		playerFrame.addListener(SWT.MouseMove, move_listener );
		
		playerFrame.addListener(SWT.MouseEnter, new Listener() {
			
			
			public void handleEvent(Event arg0) {
				overControls = false;
				
			}
		});
		
		playerFrame.addListener(SWT.MouseExit, new Listener() {
			public void handleEvent(Event evt) {
				Rectangle bounds = parent.getShell().getBounds();
				if(evt.x > 0 && evt.y > 0  && evt.x < bounds.width && evt.y < bounds.height) {
					controls.setFocus();
				}
			}
		});
				
		controls.getShell().addListener(SWT.MouseEnter, new Listener() {
			public void handleEvent(Event arg0) {
				overControls = true;
				controls.setFocus();
			}
		});
		controls.getShell().addListener(SWT.MouseExit, new Listener() {
			public void handleEvent(Event evt) {
				if(evt.x <= 0 || evt.y <= 0 || evt.x >= controlsSize.width || evt.y >= controlsSize.height) {
					overControls = false;
				}
			}
		});
		
		playerFrame.addStateListener(new StateListener() {
			
			
			public void stateChanged(MediaPlaybackState newState) {
				if(newState == MediaPlaybackState.Paused) {
					isPaused = true;
				} else {
					isPaused = false;
				}	
				
				if ( newState == MediaPlaybackState.Failed ){
					
					bufferingControls.setFailed( "azemp.failed", newState.getDetails());
					
				}else if(newState == MediaPlaybackState.Closed) {
					display.asyncExec(new Runnable() {
						
						public void run() {
							if(playerFrame.isDisposed()) return;
							if(playerFrame.getFullscreen()) {
								playerFrame.setFullscreen(false);
							}
							if(playerFrame.getDurationInSecs() == 0) {
								MessageBox mb = new MessageBox(parent.getShell(),SWT.OK | SWT.ERROR);
								mb.setText("Error");
								File f = new File(playerFrame.getOpenedFile());
								if(!(f.exists() && f.isFile())) {
									mb.setMessage("Video file not found");
								} else {
									mb.setMessage("The video could not be loaded.");
								}
								mb.open();
							}
						}
					});
				}
				
			}
		});
		
		playerFrame.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event evt) {
				playerFrame.setFullscreen(!playerFrame.getFullscreen());
			}
		});
		
		keyListener = new Listener() {
			public void handleEvent(Event event) {
				//System.out.println(SWT.ALT + " " + event.stateMask + " '" + event.character + "'");
				switch(event.character) {
					case SWT.ESC :
						if(playerFrame.getFullscreen()) {
							playerFrame.setFullscreen(false);
						}
						break;
					case ' ' :
						playerFrame.togglePause();
						break;
					case 'f' :
					case 'F' :
						if((event.stateMask & SWT.COMMAND) != 0) {
							playerFrame.setFullscreen(!playerFrame.getFullscreen());
						}
						break;
					case '\r':
						if((event.stateMask & SWT.ALT) != 0 ) {
							playerFrame.setFullscreen(!playerFrame.getFullscreen());
						}
						break;
					case 'w' :
						if((event.stateMask & SWT.COMMAND) != 0) {
							parent.getShell().close();
						}
						break;
						
				}
			}
		};
		parent.addListener(SWT.KeyDown,keyListener);
		controls.getShell().addListener(SWT.KeyDown, keyListener);
		
		parent.getShell().addListener(SWT.Dispose, new Listener() {
			public void handleEvent(Event arg0) {
				if(hiddenCursor != null && !hiddenCursor.isDisposed()) {
					hiddenCursor.dispose();
					
				}
			}
		});
	}
	
	private void hideCursor() {
		display.asyncExec(new Runnable() {
			public void run() {
				playerFrame.setCursor(hiddenCursor);
				controls.setCursor(hiddenCursor);
			}
		});
	}
	
	public void
	setSize(
		final int		width,
		final int		height )
	{
		display.asyncExec(new Runnable() {
			
			public void run() {
				if(parent.isDisposed()) return;
				Shell s = parent.getShell();
				Rectangle client = s.getClientArea();
				Rectangle bounds = s.getBounds();
				
				Monitor monitor = s.getMonitor();
				Rectangle monitorBounds = monitor.getClientArea();
				
				//1. Let's compute the target size for the shell
				int targetWidth,targetHeight;
				targetWidth = width + (bounds.width - client.width);
				targetHeight = height + (bounds.height - client.height);
				//We're going to allow for a small shell border (20 px) to get hidden from the screen, so that 1080p content
				//Can fit on a 1920x1200/1080 screen even with a shell border, and we'll move the shell outside the monitor area
				if(targetWidth > monitorBounds.width + 20) {
					targetWidth = monitorBounds.width;
					targetHeight = targetWidth * targetHeight / width;
				}
				if(targetHeight > monitorBounds.height) {
					targetHeight = monitorBounds.height;
					targetWidth = targetWidth * targetHeight / height;
				}
				
				Rectangle currentBounds = s.getBounds();
				
				int targetX,targetY;
				targetX = currentBounds.x;
				targetY = currentBounds.y;
				
				if(targetX + targetWidth > monitorBounds.x + monitorBounds.width) {
					targetX = monitorBounds.width - targetWidth;
					//If we've gone outside the screen, let's only do it by half
					//(assuming left and right shell borders are the same size)
					if(targetX < 0) {
						targetX /= 2;
					}
				}
				
				if(targetY + targetHeight > monitorBounds.y + monitorBounds.height) {
					targetY = monitorBounds.height - targetHeight;
					//If we've gone outside the screen we're doing something wrong ...
					if(targetY < 0) {
						targetY = 0;
					}
				}
				
				Rectangle targetBounds = new Rectangle(targetX,targetY,targetWidth,targetHeight);
				
				resizeParentShell(s,currentBounds,targetBounds);
				
			}
		});	
	}
	
	public void open(String file, boolean stream_mode ) {
		
		bufferingControls.hide();
		if ( !stream_mode ){
			playerFrame.clearDurationInSecs();
		}
		controls.prepare();
		controls.setPlayEnabled( true );
		controls.setSeekMaxTime( stream_mode?0:-1 );
		playerFrame.open(file);		
	}
	
	public void prepare()
	{
		stop();
		playerFrame.clearDurationInSecs();
		controls.prepare();
		controls.setPlayEnabled( false );
		controls.setSeekMaxTime( 0 );
	}
	
	public void stop() {
		playerFrame.stop();
	}
	
	public void pause() {
		playerFrame.doPause();
		controls.setPlayEnabled( false );
	}
	
	public void resume() {
		bufferingControls.hide();
		controls.setPlayEnabled( true );
		playerFrame.doResume();
	}
	
	public boolean
	isActive()
	{
		return( !playerFrame.isDisposed());
	}
	
	
	public void 
	playStats(
		Map<String,Object>		map )
	{		
		Long	file_size 	= (Long)map.get( "file_size" );
		Long	cont_done	= (Long)map.get( "cont_done" );
		Long	duration	= (Long)map.get( "duration" );
		
		Long	min_secs	= (Long)map.get( "buffer_min" );

		Integer buffer_secs	= (Integer)map.get( "buffer_secs" );
		
		if ( file_size == null || file_size == 0 || cont_done == null || duration == null || min_secs == null || buffer_secs == null ){
			
			return;
		}
		
		float max;
		
		if ((long)cont_done == (long)file_size ){
			
			max = duration;
			
		}else{
						
			float existing_max = controls.getSeekMaxTime();
			
			// System.out.println( map );
			
			min_secs += 10;
			
			if ( buffer_secs >= min_secs ){
				
				// we're playing and have enough buffer so the max seek must at least be where we currently
				// are
								
				max = controls.getCurrentTimeSecs();
			
				float	estimated_max_time_pos = cont_done * duration / ( file_size * 1000 ) - min_secs;

				float	extra_secs = estimated_max_time_pos - max;
				
				if ( extra_secs > 0 ){
					
					max = max + extra_secs*9/10;
				}
				
				if ( existing_max >= max ){
					
					max = existing_max;
				}
			}else{
								
				max = controls.getCurrentTimeSecs();
				
				if ( max - existing_max > min_secs ){

						// keep at least min-secs behind current
					
					max = max - min_secs;
					
				}else{
				
					// not enough buffer, stick with where we were

					max = existing_max;
				}
			}
			
			if ( max > duration ){
				
				max = duration;
			}
		}
		
		controls.setSeekMaxTime( max );
	}
	
	public void 
	buffering(
		Map<String,Object>		map )
	{
		int	state = (Integer)map.get( "state" );
		
		// System.out.println( "buffering: " + map );

		bufferingControls.show();
		controls.setPlayEnabled( false );
		
		String	line1 = null;
		String	line2 = null;
		String	line3	= "";
		
		Long	elapsed = (Long)map.get( "dl_time" );
		Long	dl_rate	= (Long)map.get( "dl_rate" );
		Long	dl_size = (Long)map.get( "dl_size" );
		
		Long	stream_rate = (Long)map.get( "stream_rate" );
		
		long elapsed_sec = elapsed==null?0:(elapsed/1000);
		
		if ( dl_rate != null && dl_size != null ){
			
//			line3 	= 
//				MessageText.getString( 
//					stream_rate==null?"azemp.play_in.rate1":"azemp.play_in.rate2", 
//					new String[]{ 
//						trim(DisplayFormatters.formatByteCountToKiBEtcPerSec( dl_rate )),
//						trim(DisplayFormatters.formatByteCountToKiBEtc( dl_size )),
//						stream_rate==null?"":trim(DisplayFormatters.formatByteCountToKiBEtcPerSec( stream_rate ))
//					});
		}
		
		if ( state == 1 ){
			
//			line1	= MessageText.getString( "azemp.analysing_md" );
			line2	= (String)map.get( "msg" );
			
			if ( line2 == null ){
				
				line2 = "";
			}
			
		}else if ( state == 2 ){
			
			Integer i_preview = (Integer)map.get( "preview" );
		
			boolean	preview = i_preview != null && i_preview == 1;

			Integer i_eta = (Integer)map.get( "eta" );
					
			int 	eta = i_eta==null?0:i_eta;
		
//			line1	= MessageText.getString( "azemp.buffering" );
			
			if ( eta > 30*60 && elapsed_sec < 60 ){

//				line2 	= MessageText.getString( preview?"azemp.preview_in.calc":"azemp.play_in.calc" );
				
			}else if ( eta > 60*60 ){
			
//				line2 	= MessageText.getString( preview?"azemp.preview_in.ages":"azemp.play_in.ages" );

			}else{
				
				if ( eta < 0 ){
					
					eta = 0;
				}
				
				String time_str = Utils.getFormatedTime(eta,true);
				
				if ( time_str.startsWith( "0:00:" )){
					
					time_str = time_str.substring( 3 );
				}
				
//				line2 	= MessageText.getString( preview?"azemp.preview_in.time":"azemp.play_in.time", new String[]{ time_str });
			}
		}else{
			
//			line1	= MessageText.getString( "azemp.failed" );
			line2	= (String)map.get( "msg" );
			
			if ( line2 == null ){
				
				line2 = "";
			}		
		}
		
		bufferingControls.updateText( line1, line2, line3 );
	}
	
	private String
	trim(
		String	str )
	{
		int pos = str.indexOf('.');
		
		if ( pos != -1 ){
			
			String s = str.substring( 0, pos );
			
			while( str.charAt(pos) != ' ' ){
				
				pos++;
			}
			
			return( s + ' ' + str.substring( pos ));
		}
		
		return( str );
	}
	
	private void resizeParentShell(final Shell s,final Rectangle currentBounds,
			final Rectangle targetBounds) {
		
		if(Utils.isMacOSX()) {} else {
			s.setBounds(targetBounds.x,targetBounds.y,targetBounds.width,targetBounds.height);
		}
	}
	
	public void setAutoResize(boolean b) {
		autoResize = b;
		
	}
	
	public void addStateListener(StateListener listener) {
		playerFrame.addStateListener(listener);
	}
	
	public void
	setDurationInSeconds(
		float	secs )
	{
		playerFrame.setDurationInSecs( secs );
	}
	
	public float
	getDurationInSeconds()
	{
		return( playerFrame.getDurationInSecs());
	}
	
	public static void play(final Shell shell, String fileOrUrl) throws Exception {
		File player_binary = new File("bin\\mplayer.exe");
		
		MPlayer.initialise( player_binary );
		
//		ResourceBundle bundle = 
//			ResourceBundle.getBundle( 
//				// "com/azureus/plugins/azemp/skins/skin",
//				"com/azureus/plugins/azemp/internat/Messages",
//				Locale.getDefault(), Player.class.getClassLoader());
//		
//		new LocaleUtilitiesImpl(null).integrateLocalisedMessageBundle( bundle );
		
		System.out.println(SWT.getVersion());
		final Display display = shell.getDisplay();
		shell.setLocation(200,200);
		shell.setSize(720,480);
		
		shell.setText("Loading...");
		shell.setLayout(new FillLayout());
		shell.forceFocus();
		//shell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		
		final Player player = new Player(shell);

		player.setAutoResize(true);

		player.addStateListener(new StateListener() {
			
			public void stateChanged(MediaPlaybackState newState) {
				if(newState == MediaPlaybackState.Closed) {
					shell.getDisplay().asyncExec(new Runnable() {
						
						public void run() {
							shell.close();
							
						}
					});					
				}
			}
		});
		
		//shell.setFullScreen(true);
		
		player.open( fileOrUrl, true );
		
		shell.open();
		
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

}
