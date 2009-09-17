package com.idega.block.email.mailing.list.presentation;

import javax.faces.component.UIComponent;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.email.EmailConstants;
import com.idega.block.email.mailing.list.business.MailingListManager;
import com.idega.block.web2.business.Web2Business;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.builder.business.BuilderLogic;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.Block;
import com.idega.presentation.IWContext;
import com.idega.presentation.Image;
import com.idega.presentation.TableCell2;
import com.idega.presentation.TableRow;
import com.idega.presentation.text.Link;
import com.idega.presentation.text.Text;
import com.idega.util.ArrayUtil;
import com.idega.util.PresentationUtil;
import com.idega.util.StringUtil;
import com.idega.util.URIUtil;
import com.idega.util.expression.ELUtil;

public abstract class BasicMailingList extends Block {
	
	@Autowired
	MailingListManager mailingListManager;
	
	@Autowired
	Web2Business web2;
	
	IWBundle bundle;
	IWResourceBundle iwrb;
	
	@Override
	public void main(IWContext iwc) throws Exception {
		ELUtil.getInstance().autowire(this);
		bundle = getBundle(iwc);
		iwrb = bundle.getResourceBundle(iwc);
		PresentationUtil.addStyleSheetToHeader(iwc, bundle.getVirtualPathWithFileNameString("style/mailing_lists.css"));
		
		doBusiness(iwc);
		present(iwc);
	}
	
	@Override
	public String getBundleIdentifier() {
		return EmailConstants.IW_BUNDLE_IDENTIFIER;
	}
	
	protected abstract void doBusiness(IWContext iwc);
	
	protected abstract void present(IWContext iwc) throws Exception;
	
	protected void addCell(TableRow row, UIComponent component, String styleClass) {
		TableCell2 cell = row.createCell();
		cell.add(component);
		if (styleClass != null) {
			cell.setStyleClass(styleClass);
		}
	}
	
	protected void addCell(TableRow row, UIComponent component) {
		addCell(row, component, null);
	}
	
	protected void addCell(TableRow row, String text, String styleClass) {
		addCell(row, new Text(text), styleClass);
	}
	
	protected void addCell(TableRow row, String text) {
		addCell(row, text, null);
	}
	
	protected Link getLink(String title, AdvancedProperty... params) {
		return getLink(title, null, null, params);
	}
	
	protected Link getLink(String title, String uriToImage, AdvancedProperty... params) {
		return getLink(title, null, uriToImage, params);
	}
	
	protected Link getLink(String title, String uri, String uriToImage, AdvancedProperty... params) {
		Image image = null;
		if (!StringUtil.isEmpty(uriToImage)) {
			image = new Image(uriToImage);
			image.setTitle(title);
		}
		
		Link link = image == null ? new Link(title) : new Link(image);
		link.setTitle(title);
		
		if (StringUtil.isEmpty(uri) && !ArrayUtil.isEmpty(params)) {
			for (AdvancedProperty param: params) {
				link.addParameter(param.getId(), param.getValue());
			}
		} else {
			URIUtil uriUtil = new URIUtil(uri);
			if (!ArrayUtil.isEmpty(params)) {
				for (AdvancedProperty param: params) {
					uriUtil.setParameter(param.getId(), param.getValue());
				}
			}
			link.setURL(uriUtil.getUri());
		}
		
		return link;
	}
	
	protected String getUriToMailingListViewer(IWContext iwc) {
		return BuilderLogic.getInstance().getFullPageUrlByPageType(iwc.isLoggedOn() ? iwc.getCurrentUser() : null, "mailing_list_viewer", true);
	}
}
