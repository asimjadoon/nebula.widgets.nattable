/*******************************************************************************
 * Copyright (c) 2012, 2013 Original authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Original authors and others - initial API and implementation
 ******************************************************************************/
package org.eclipse.nebula.widgets.nattable.viewport;

import org.eclipse.nebula.widgets.nattable.edit.command.EditUtils;
import org.eclipse.nebula.widgets.nattable.layer.IUniqueIndexLayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public abstract class ScrollBarHandlerTemplate implements Listener {

	public static final int DEFAULT_OFFSET = 1;
	protected final ViewportLayer viewportLayer;
	protected final IUniqueIndexLayer scrollableLayer;
	protected final IScroller<?> scroller;
	
	/**
	 * Flag to remember if the scroll bar is moved by dragging.
	 * Needed because if the scroll bar is moved by dragging, there will be 
	 * another event that is handled for releasing the drag mode. 
	 * We only need to handle the dragging once, otherwise if the 
	 * DialogErrorHandling strategy is used, the dialog would be showed
	 * twice.
	 */
	private boolean dragging = false;
	
	/**
	 * Flag to remember if the drag operation should be handled or not.
	 * The value will be set on trying to commit an possible open editor. If that
	 * is not possible, the value will be set to <code>false</code> which will
	 * result in not performing a scrolling operation.
	 * <p>
	 * This is necessary to avoid inconsistent state when having an editor that
	 * contains invalid data and you are trying to scroll. If scrolling would be
	 * handled, the open editor wouldn't close which results in broken rendering. 
	 * </p>
	 */
	private boolean globalHandle = true;

	public ScrollBarHandlerTemplate(ViewportLayer viewportLayer, IScroller<?> scroller) {
		this.viewportLayer = viewportLayer;
		this.scrollableLayer = viewportLayer.getScrollableLayer();
		this.scroller = scroller;
		this.scroller.addListener(SWT.Selection, this);
	}
	
	public void dispose() {
		if (this.scroller != null && !this.scroller.isDisposed()) {
			this.scroller.removeListener(SWT.Selection, this);
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.dragging) {
			//Only try to commit and close an possible open editor once
			//when starting the drag operation. Otherwise the conversion
			//and validation errors would raise multiple times.
			if (!EditUtils.commitAndCloseActiveEditor()) {
				this.globalHandle = false;
			}
		}
		
		boolean handle = this.globalHandle;
		
		if (event.detail == SWT.DRAG) {
			this.dragging = true;
		}
		else {
			//dragging is finished so we reset the global states
			this.dragging = false;
			this.globalHandle = true;
		}
			
		if (handle && event.widget == scroller.getUnderlying()) {
			setViewportOrigin(getViewportMinimumOrigin() + scroller.getSelection());
			setScrollIncrement();
			event.doit = false;
		} else {
			adjustScrollBar();
		}
	}

	void adjustScrollBar() {
		
		if (scroller.isDisposed()) {
			return;
		}
		int startPixel = getViewportOrigin() - getViewportMinimumOrigin();
		
		scroller.setSelection(startPixel);
	}

	void recalculateScrollBarSize() {
		if (scroller.isDisposed()) {
			return;
		}
		
		int max = getScrollableLayerSpan() - getViewportMinimumOrigin();
		if (! scroller.isDisposed()) {
			scroller.setMaximum(max);
		}
		
		int viewportWindowSpan = getViewportWindowSpan();
		
		int thumbSize;
		if (viewportWindowSpan < max && viewportWindowSpan != 0) {
			thumbSize = viewportWindowSpan;
			scroller.setEnabled(true);
			scroller.setVisible(true);
			
			setScrollIncrement();
			
			scroller.setPageIncrement(viewportWindowSpan);
		} else {
			thumbSize = max;
			scroller.setEnabled(false);
			scroller.setVisible(false);
		}
		scroller.setThumb(thumbSize);
		
		adjustScrollBar();
	}

	void setScrollIncrement() {
		int scrollIncrement = Math.min(getScrollIncrement(), getViewportWindowSpan() / 4);
		scroller.setIncrement(scrollIncrement);
	}
	
	/**
	 * Methods to be implemented by the Horizontal/Vertical scroll bar handlers.
	 * @return
	 */
	abstract int getViewportWindowSpan();
	
	abstract int getScrollableLayerSpan();

	abstract boolean keepScrolling();

	abstract int getViewportOrigin();

	abstract int getViewportMinimumOrigin();

	abstract void setViewportOrigin(int pixel);

	abstract MoveDirectionEnum scrollDirectionForEventDetail(int eventDetail);
	
	abstract int getScrollIncrement();

}
