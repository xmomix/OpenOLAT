/**
 * OLAT - Online Learning and Training<br>
 * http://www.olat.org
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.notifications;

import java.util.Date;
import java.util.List;

import org.olat.commons.rss.RSSUtil;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.tabbedpane.TabbedPaneChangedEvent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.dtabs.Activateable;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.util.Util;
import org.olat.home.GuestHomeSite;

/**
 * Description:<br>
 * The subscription and notification controller combines the users subscription
 * management and his personal news into one view.
 * 
 * <P>
 * Initial Date: 22.12.2009 <br>
 * 
 * @author gnaegi
 */
public class NotificationSubscriptionAndNewsController extends BasicController implements Activateable, Activateable2 {
	private Identity subscriberIdentity;
	private TabbedPane tabbedPane;
	private Panel subscriptionPanel, rssPanel;
	private NotificationSubscriptionController subscriptionCtr;
	private NotificationNewsController newsCtr;

	protected NotificationSubscriptionAndNewsController(Identity subscriberIdentity, UserRequest ureq, WindowControl wControl,
			Date newsSinceDate) {
		super(ureq, wControl, Util.createPackageTranslator(GuestHomeSite.class, ureq.getLocale()));
		this.subscriberIdentity = subscriberIdentity;
		tabbedPane = new TabbedPane("tabbedPane", getLocale());
		tabbedPane.addListener(this);
		
		// Add news view
		newsCtr = NotificationUIFactory.createNewsListingController(subscriberIdentity, ureq, getWindowControl(), newsSinceDate);
		listenTo(newsCtr);
		tabbedPane.addTab(translate("overview.tab.news"), newsCtr.getInitialComponent());
		// Add subscription view, initialize with an empty panel and create real
		// controller only when user clicks the tab
		subscriptionPanel = new Panel("subscriptionPanel");
		tabbedPane.addTab(translate("overview.tab.subscriptions"), subscriptionPanel);
		// Add RSS info page
		rssPanel = new Panel("rssPanel");
		tabbedPane.addTab(translate("overview.tab.rss"), rssPanel);
		//
		putInitialPanel(tabbedPane);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
	// controllers dispsed by basic controller
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component,
	 *      org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if (source == tabbedPane) {
			if (event instanceof TabbedPaneChangedEvent) {
				TabbedPaneChangedEvent tabbedEvent = (TabbedPaneChangedEvent) event;
				// Lazy initialize the notification subscription controller when the
				// user clicks the tab the first time
				if (tabbedEvent.getNewComponent() == subscriptionPanel && subscriptionCtr == null) {
					subscriptionCtr = NotificationUIFactory.createSubscriptionListingController(subscriberIdentity, ureq, getWindowControl());
					listenTo(subscriptionCtr);
					subscriptionPanel.setContent(subscriptionCtr.getInitialComponent());
				}
				// Lazy initialize the notification subscription controller when the
				// user clicks the tab the first time
				else if (tabbedEvent.getNewComponent() == rssPanel && rssPanel.getContent() == null) {
					VelocityContainer notificationsRssVC = createVelocityContainer("notificationsRSS");
					String rssLink = RSSUtil.getPersonalRssLink(ureq);
					notificationsRssVC.contextPut("rssLink", rssLink);
					User user = subscriberIdentity.getUser();
					String fullName = user.getProperty(UserConstants.FIRSTNAME, getLocale()) + " " + user.getProperty(UserConstants.LASTNAME, getLocale()); 
					notificationsRssVC.contextPut("fullName", fullName);
					rssPanel.setContent(notificationsRssVC);
				}
				//fxdiff BAKS-7 Resume function
				tabbedPane.addToHistory(ureq, getWindowControl());
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	protected void event(UserRequest ureq, Controller source, Event event) {
		if (source == subscriptionCtr) {
			if (event == Event.CHANGED_EVENT) {
				// Reload table model from news controller to reflect change in
				// subscriptions
				newsCtr.updateNewsDataModel();
			}
		}
	}

	@Override
	public void activate(UserRequest ureq, String viewIdentifier) {
		if(viewIdentifier == null) return;
		if(viewIdentifier.startsWith("[news:0]")) {
			String selection = viewIdentifier.substring(8);
			newsCtr.activate(ureq, selection);
		}
	}

	@Override
	//fxdiff BAKS-7 Resume function
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(entries == null || entries.isEmpty()) return;
		
		String path = entries.get(0).getOLATResourceable().getResourceableTypeName();
		if("news".equals(path)) {
			activate(ureq, "[news:0]");
		} else if("notifications".equals(path)) {
			List<ContextEntry> subEntries = entries.subList(1, entries.size());
			newsCtr.activate(ureq, subEntries, entries.get(0).getTransientState());
		} else {
			tabbedPane.activate(ureq, entries, state);
		}
	}
}
