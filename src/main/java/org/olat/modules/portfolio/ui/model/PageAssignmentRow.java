/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.portfolio.ui.model;

import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.modules.portfolio.Assignment;

/**
 * 
 * Initial date: 12.07.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class PageAssignmentRow {
	
	private Assignment assignment;
	
	private FormLink editLink, openLink, startLink;
	
	public PageAssignmentRow(Assignment assignment) {
		this.assignment = assignment;
	}
	
	public String getTitle() {
		return assignment.getTitle();
	}
	
	public String getSummary() {
		return assignment.getSummary();
	}
	
	public Assignment getAssignment() {
		return assignment;
	}
	
	public void setAssignment(Assignment assignment) {
		this.assignment = assignment;
	}

	public FormLink getEditLink() {
		return editLink;
	}

	public void setEditLink(FormLink editLink) {
		this.editLink = editLink;
	}

	public FormLink getOpenLink() {
		return openLink;
	}

	public void setOpenLink(FormLink openLink) {
		this.openLink = openLink;
	}

	public FormLink getStartLink() {
		return startLink;
	}

	public void setStartLink(FormLink startLink) {
		this.startLink = startLink;
	}
}