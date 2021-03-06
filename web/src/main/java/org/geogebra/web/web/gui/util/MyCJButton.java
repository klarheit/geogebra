package org.geogebra.web.web.gui.util;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;

/**
 * @author gabor
 * 
 * MyCanvasJbutton a Canvas that used as a button
 *
 */
public class MyCJButton extends Composite implements MouseDownHandler,
        MouseUpHandler, HasSetIcon {
	
	private Label button;
	/** whether this is enabled */
	boolean isEnabled;
	private ImageOrText icon;
	private Label buttonContent;
	private boolean imageMode = false;
	
	/**
	 * Creates a new button
	 * 
	 */
	
	public MyCJButton() {
		button = new Label("");
		buttonContent = new Label("");
		buttonContent.setStyleName("buttonContent");
		button.getElement().appendChild(buttonContent.getElement());
		button.addMouseDownHandler(this);
		button.addMouseUpHandler(this);
		
		initWidget(button);
		setStyleName("MyCanvasButton");
		isEnabled = true;
    }

	/**
	 * @return {@link Label}
	 */
	public Label getButtonContent() {
		return buttonContent;
	}

	/**
	 * sets the text of the button
	 * 
	 * @param text
	 *            String
	 */
	public void setText(String text) {
		button.setText(text);
	}

	@Override
	public void onMouseUp(MouseUpEvent event) {
		if (!isEnabled) {
			return;
		}

		setDownState(false);
	}

	@Override
	public void onMouseDown(MouseDownEvent event) {
		if (!isEnabled) {
			return;
		}
		setDownState(true);
	}
	
	private void setDownState(boolean downState) {
	   //TODO less visible
	   if(downState){
		   this.addStyleName("selected");
	   }else{
		   this.removeStyleName("selected");
	   }
    }
	
	@Override
	public void setIcon(ImageOrText icon) {
		if(this.imageMode && icon.getUrl() == null){
			return;
		}
		if(icon.getUrl() != null){
			this.imageMode = true;
		}		
		this.icon = icon;
		icon.applyToLabel(buttonContent);
		setDownState(false);
	}
	
	/**
	 * @return {@link ImageOrText}
	 */
	public ImageOrText getIcon(){
		return this.icon;
	}

	/**
	 * @return {@code true} if button is enabled
	 */
	protected boolean isEnabled() {
		return isEnabled ;
	}
	
	/**
	 * @param enabled
	 *            boolean
	 */
	public void setEnabled(boolean enabled) {
		isEnabled = enabled;
		if (enabled) {
			removeStyleName("disabled");
		} else {
			addStyleName("disabled");

		}
	}

	/**
	 * adds a clickHandler to the button and calls the given clickhandler only
	 * if the button is enabled
	 * 
	 * @param handler
	 *            {@link ClickHandler}
	 */
	public void addActionListener(final ClickHandler handler) {
		button.addClickHandler(new ClickHandler() {

			public void onClick(ClickEvent event) {
				if (!isEnabled) {
					return;
				}
				handler.onClick(event);
			}
		});
	}
	
	/**
	 * adds the given clickhandler to the button
	 * 
	 * @param handler
	 *            {@link ClickHandler}
	 * @return {@link HandlerRegistration}
	 */
	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return button.addClickHandler(handler);
	}

	/**
	 * Sets the toolTip text
	 * 
	 * @param toolTipText
	 *            toolTip string
	 */
	public void setToolTipText(String toolTipText) {
		setTitle(toolTipText);
	}
}
