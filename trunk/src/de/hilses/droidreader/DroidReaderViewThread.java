/*

Copyright (C) 2010 Hans-Werner Hilse <hilse@web.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

*/

package de.hilses.droidreader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Scroller;

/**
 * Thread that cares for blitting Pixmaps onto the Canvas and handles scrolling
 */
class DroidReaderViewThread extends Thread {
	/**
	 * Debug helper
	 */
	protected final static String TAG = "DroidReaderViewThread";
	/**
	 * the SurfaceHolder for our Surface
	 */
	protected final SurfaceHolder mSurfaceHolder;
	
	/**
	 * Paint for not (yet) rendered parts of the page
	 */
	protected final Paint mEmptyPaint;
	/**
	 * Paint for filling the display when there is no PdfPage (yet)
	 */
	protected final Paint mNoPagePaint;
	/**
	 * Paint for the status text
	 */
	protected final Paint mStatusPaint;
	
	/**
	 * Flag that our thread should be running
	 */
	protected boolean mRun = true;
	
	/**
	 * our scroller
	 */
	protected final Scroller mScroller;
	
	protected final DroidReaderDocument mDocument;
	
	/**
	 * Background render thread, using the SurfaceView programming
	 * scheme
	 * @param holder our SurfaceHolder
	 * @param context the Context for our drawing
	 */
	public DroidReaderViewThread(SurfaceHolder holder, Context context,
			DroidReaderDocument document) {
		// store a reference to our SurfaceHolder
		mSurfaceHolder = holder;
		
		mDocument = document;
		
		// initialize Paints for non-Pixmap areas
		mEmptyPaint = new Paint();
		mEmptyPaint.setStyle(Paint.Style.FILL);
		mEmptyPaint.setColor(0xffc0c0c0); // light gray
		
		mNoPagePaint = new Paint();
		mNoPagePaint.setStyle(Paint.Style.FILL);
		mNoPagePaint.setColor(0xff303030); // dark gray
		
		mStatusPaint = new Paint();
		mNoPagePaint.setColor(0xff808080); // medium gray
		
		// the scroller, i.e. the object that calculates/interpolates
		// positions for scrolling/jumping/flinging
		mScroller = new Scroller(context);
	}
	
	/**
	 * Main Thread loop
	 */
	@Override
	public void run() {
		while (mRun) {
			boolean doSleep = true;
			if(mScroller.computeScrollOffset()) {
				Log.d(TAG, "new scroll offset");
				doSleep = false;
				mDocument.offset(mScroller.getCurrX(), mScroller.getCurrY(), true);
			}
			doDraw();
			// if we're allowed, we will go to sleep now
			if(doSleep) {
				try {
					// nothing to do, wait for someone waking us up:
					Log.d(TAG, "ViewThread going to sleep");
					Thread.sleep(3600000);
				} catch (InterruptedException e) {
					Log.d(TAG, "ViewThread woken up");
				}
			}
		}
		// mRun is now false, so we shut down.
		Log.d(TAG, "shutting down");
	}
	
	/**
	 * this does the actual drawing to the Canvas for our surface
	 */
	private void doDraw() {
		Log.d(TAG, "drawing...");
		Canvas c = null;
		try {
			c = mSurfaceHolder.lockCanvas(null);
			if(!mDocument.isPageLoaded()) {
				// no page/document loaded
				Log.d(TAG, "no page loaded.");
				c.drawRect(0, 0, c.getWidth(), c.getHeight(), mNoPagePaint);
			} else if(mDocument.havePixmap()) {
				// we have both page and Pixmap, so draw:
				// background:
				Log.d(TAG, "page loaded, rendering pixmap");
				c.drawRect(0, 0, c.getWidth(), c.getHeight(), mEmptyPaint);
				c.drawBitmap(
						mDocument.mViews[mDocument.mCurrentView].mBuf,
						0,
						mDocument.mViews[mDocument.mCurrentView].mViewBox.width(),
						-mDocument.mOffsetX + mDocument.mViews[mDocument.mCurrentView].mViewBox.left,
						-mDocument.mOffsetY + mDocument.mViews[mDocument.mCurrentView].mViewBox.top,
						mDocument.mViews[mDocument.mCurrentView].mViewBox.width(),
						mDocument.mViews[mDocument.mCurrentView].mViewBox.height(),
						false,
						null);
			} else {
				// page loaded, but no Pixmap yet
				Log.d(TAG, "page loaded, but no active Pixmap.");
				c.drawRect(0, 0, c.getWidth(), c.getHeight(), mEmptyPaint);
			}
		} finally {
			if (c != null) {
				mSurfaceHolder.unlockCanvasAndPost(c);
			}
		}
	}
	
	public void triggerRepaint() {
		Log.d(TAG, "repaint triggered");
		interrupt();
	}
}
