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
package org.olat.course.assessment.ui.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityModule;
import org.olat.basesecurity.Group;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableFilter;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableSearchEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SelectionEvent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.stack.BreadcrumbPanelAware;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.components.stack.TooledStackedPanel.Align;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentMainController;
import org.olat.course.assessment.AssessmentToolManager;
import org.olat.course.assessment.bulk.PassedCellRenderer;
import org.olat.course.assessment.model.SearchAssessedIdentityParams;
import org.olat.course.assessment.ui.tool.IdentityListCourseNodeTableModel.IdentityCourseElementCols;
import org.olat.course.certificate.CertificateLight;
import org.olat.course.certificate.CertificatesManager;
import org.olat.course.certificate.ui.DownloadCertificateCellRenderer;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.AssessmentToolOptions;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.CourseNodeFactory;
import org.olat.group.BusinessGroup;
import org.olat.modules.assessment.AssessmentEntry;
import org.olat.modules.assessment.model.AssessmentEntryStatus;
import org.olat.modules.assessment.ui.AssessedIdentityController;
import org.olat.modules.assessment.ui.AssessedIdentityElementRow;
import org.olat.modules.assessment.ui.AssessedIdentityListState;
import org.olat.modules.assessment.ui.AssessmentToolContainer;
import org.olat.modules.assessment.ui.AssessmentToolSecurityCallback;
import org.olat.modules.assessment.ui.ScoreCellRenderer;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryService;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 06.10.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class IdentityListCourseNodeController extends FormBasicController implements Activateable2 {

	private final BusinessGroup group;
	private final CourseNode courseNode;
	private final RepositoryEntry courseEntry;
	private final RepositoryEntry referenceEntry;
	private final boolean isAdministrativeUser;
	private final List<UserPropertyHandler> userPropertyHandlers;
	private final AssessmentToolSecurityCallback assessmentCallback;
	
	private Link nextLink, previousLink;
	private FlexiTableElement tableEl;
	private final TooledStackedPanel stackPanel;
	private final AssessmentToolContainer toolContainer;
	private IdentityListCourseNodeTableModel usersTableModel;
	
	private AssessedIdentityController currentIdentityCtrl;
	
	@Autowired
	private UserManager userManager;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private BaseSecurityModule securityModule;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private CertificatesManager certificatesManager;
	@Autowired
	private AssessmentToolManager assessmentToolManager;
	
	public IdentityListCourseNodeController(UserRequest ureq, WindowControl wControl, TooledStackedPanel stackPanel,
			RepositoryEntry courseEntry, CourseNode courseNode, AssessmentToolContainer toolContainer,
			AssessmentToolSecurityCallback assessmentCallback) {
		super(ureq, wControl, "identity_courseelement");
		setTranslator(Util.createPackageTranslator(AssessmentMainController.class, getLocale(), getTranslator()));
		setTranslator(userManager.getPropertyHandlerTranslator(getTranslator()));
		
		this.group = null;
		this.courseNode = courseNode;
		this.stackPanel = stackPanel;
		this.courseEntry = courseEntry;
		this.toolContainer = toolContainer;
		this.assessmentCallback = assessmentCallback;
		
		if(courseNode.needsReferenceToARepositoryEntry()) {
			referenceEntry = courseNode.getReferencedRepositoryEntry();
		} else {
			referenceEntry = null;
		}
		
		isAdministrativeUser = securityModule.isUserAllowedAdminProps(ureq.getUserSession().getRoles());
		userPropertyHandlers = userManager.getUserPropertyHandlersFor(AssessmentToolConstants.usageIdentifyer, isAdministrativeUser);
		
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		if(formLayout instanceof FormLayoutContainer) {
			FormLayoutContainer layoutCont = (FormLayoutContainer)formLayout;
			layoutCont.contextPut("courseNodeTitle", courseNode.getShortTitle());
			layoutCont.contextPut("courseNodeCssClass", CourseNodeFactory.getInstance().getCourseNodeConfiguration(courseNode.getType()).getIconCSSClass());
		}

		//add the table
		FlexiTableColumnModel columnsModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
		if(isAdministrativeUser) {
			columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(IdentityCourseElementCols.username, "select"));
		}
		
		int colIndex = AssessmentToolConstants.USER_PROPS_OFFSET;
		for (int i = 0; i < userPropertyHandlers.size(); i++) {
			UserPropertyHandler userPropertyHandler	= userPropertyHandlers.get(i);
			boolean visible = UserManager.getInstance().isMandatoryUserProperty(AssessmentToolConstants.usageIdentifyer , userPropertyHandler);
			columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(visible, userPropertyHandler.i18nColumnDescriptorLabelKey(), colIndex, "select", true, "userProp-" + colIndex));
			colIndex++;
		}
		AssessableCourseNode assessableNode = null;
		if(courseNode instanceof AssessableCourseNode) {
			assessableNode = (AssessableCourseNode)courseNode;
			
			if(assessableNode.hasAttemptsConfigured()) {
				columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(IdentityCourseElementCols.attempts, "select"));
			}
			if(assessableNode.hasScoreConfigured()) {
				columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(IdentityCourseElementCols.min, "select", new ScoreCellRenderer()));
				columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(IdentityCourseElementCols.max, "select", new ScoreCellRenderer()));
				columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(IdentityCourseElementCols.score, "select", new ScoreCellRenderer()));
			}
			if(assessableNode.hasPassedConfigured()) {
				columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(IdentityCourseElementCols.passed, new PassedCellRenderer()));
			}
		}

		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(IdentityCourseElementCols.assessmentStatus, new AssessmentStatusCellRenderer()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(IdentityCourseElementCols.initialLaunchDate, "select"));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(IdentityCourseElementCols.lastScoreUpdate, "select"));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(IdentityCourseElementCols.certificate, new DownloadCertificateCellRenderer()));

		usersTableModel = new IdentityListCourseNodeTableModel(columnsModel, assessableNode); 
		tableEl = uifactory.addTableElement(getWindowControl(), "table", usersTableModel, 20, false, getTranslator(), formLayout);
		tableEl.setExportEnabled(true);
		tableEl.setSearchEnabled(new AssessedIdentityListProvider(getIdentity(), courseEntry, referenceEntry, courseNode.getIdent(), assessmentCallback), ureq.getUserSession());
		
		List<FlexiTableFilter> filters = new ArrayList<>();
		filters.add(new FlexiTableFilter(translate("filter.passed"), "passed"));
		filters.add(new FlexiTableFilter(translate("filter.failed"), "failed"));
		filters.add(new FlexiTableFilter(translate("filter.inProgress"), "inProgress"));
		filters.add(new FlexiTableFilter(translate("filter.inReview"), "inReview"));
		filters.add(new FlexiTableFilter(translate("filter.done"), "done"));
		tableEl.setFilters("", filters);
		
		if(assessmentCallback.canAssessBusinessGoupMembers()) {
			List<BusinessGroup> coachedGroups = null;;
			if(assessmentCallback.isAdmin()) {
				ICourse course = CourseFactory.loadCourse(courseEntry);
				coachedGroups = course.getCourseEnvironment().getCourseGroupManager().getAllBusinessGroups();
			} else {
				coachedGroups = assessmentCallback.getCoachedGroups(); 
			}

			if(coachedGroups.size() > 0) {
				List<FlexiTableFilter> groupFilters = new ArrayList<>();
				for(BusinessGroup coachedGroup:coachedGroups) {
					groupFilters.add(new FlexiTableFilter(coachedGroup.getName(), coachedGroup.getKey().toString(), "o_icon o_icon_group"));
				}
				
				tableEl.setExtendedFilterButton(translate("filter.groups"), groupFilters);
			}
		}
	}
	
	private void updateModel(UserRequest ureq, String searchString, List<FlexiTableFilter> filters, List<FlexiTableFilter> extendedFilters) {
		SearchAssessedIdentityParams params = new SearchAssessedIdentityParams(courseEntry, courseNode.getIdent(), referenceEntry, assessmentCallback);
		
		List<AssessmentEntryStatus> assessmentStatus = null;
		if(filters != null && filters.size() > 0) {
			assessmentStatus = new ArrayList<>(filters.size());
			for(FlexiTableFilter filter:filters) {
				if("passed".equals(filter.getFilter())) {
					params.setPassed(true);
				} else if("failed".equals(filter.getFilter())) {
					params.setFailed(true);
				} else if(AssessmentEntryStatus.isValueOf(filter.getFilter())){
					assessmentStatus.add(AssessmentEntryStatus.valueOf(filter.getFilter()));
				}
			}
		}
		params.setAssessmentStatus(assessmentStatus);
		
		List<Long> businessGroupKeys = null;
		if(extendedFilters != null && extendedFilters.size() > 0) {
			businessGroupKeys = new ArrayList<>(extendedFilters.size());
			for(FlexiTableFilter extendedFilter:extendedFilters) {
				if(StringHelper.isLong(extendedFilter.getFilter())) {
					businessGroupKeys.add(Long.parseLong(extendedFilter.getFilter()));
				}
			}
		}
		params.setBusinessGroupKeys(businessGroupKeys);
		params.setSearchString(searchString);
		
		List<Identity> assessedIdentities = assessmentToolManager.getAssessedIdentities(getIdentity(), params);
		List<AssessmentEntry> assessmentEntries = assessmentToolManager.getAssessmentEntries(getIdentity(), params, null);
		Map<Long,AssessmentEntry> entryMap = new HashMap<>();
		assessmentEntries.forEach((entry) -> entryMap.put(entry.getIdentity().getKey(), entry));

		List<AssessedIdentityElementRow> rows = new ArrayList<>(assessedIdentities.size());
		for(Identity assessedIdentity:assessedIdentities) {
			AssessmentEntry entry = entryMap.get(assessedIdentity.getKey());
			rows.add(new AssessedIdentityElementRow(assessedIdentity, entry, userPropertyHandlers, getLocale()));
		}
		
		if(toolContainer.getCertificateMap() == null) {
			List<CertificateLight> certificates = certificatesManager.getLastCertificates(courseEntry.getOlatResource());
			ConcurrentMap<Long, CertificateLight> certificateMap = new ConcurrentHashMap<>();
			for(CertificateLight certificate:certificates) {
				certificateMap.put(certificate.getIdentityKey(), certificate);
			}
			toolContainer.setCertificateMap(certificateMap);
		}
		usersTableModel.setCertificateMap(toolContainer.getCertificateMap());
		usersTableModel.setObjects(rows);
		if(filters != null && filters.size() > 0) {
			usersTableModel.filter(filters.get(0).getFilter());
		}
		tableEl.reset();
		tableEl.reloadData();

		List<String> toolCmpNames = new ArrayList<>();
		if(courseNode instanceof AssessableCourseNode) {
			AssessableCourseNode acn = (AssessableCourseNode)courseNode;
			ICourse course = CourseFactory.loadCourse(courseEntry);
			AssessmentToolOptions options = new AssessmentToolOptions();
			if(group == null) {
				options.setIdentities(assessedIdentities);
				fillAlternativeToAssessableIdentityList(options);
			} else {
				options.setGroup(group);
			}
			
			//TODO qti filter by group?
			List<Controller> tools = acn.createAssessmentTools(ureq, getWindowControl(), stackPanel, course.getCourseEnvironment(), options);
			int count = 0;
			if(tools.size() > 0) {
				for(Controller tool:tools) {
					listenTo(tool);
					String toolCmpName = "ctrl_" + (count++);
					flc.put(toolCmpName, tool.getInitialComponent());
					toolCmpNames.add(toolCmpName);
					if(tool instanceof BreadcrumbPanelAware) {
						((BreadcrumbPanelAware)tool).setBreadcrumbPanel(stackPanel);
					}
				}
			}
			
		}
		flc.contextPut("toolCmpNames", toolCmpNames);
	}
	
	private void updateModel(UserRequest ureq, Identity assessedIdentity) {
		updateModel(ureq, null, null, null);
	}
	
	/*
	private boolean accept(AssessmentEntry entry, SearchAssessedIdentityParams params) {
		boolean ok = true;
		
		if(params.isPassed() && (entry == null || entry.getPassed() == null || !entry.getPassed().booleanValue())) {
			ok &= false;
		}
		
		if(params.isFailed() && (entry == null || entry.getPassed() == null || entry.getPassed().booleanValue())) {
			ok &= false;
		}
		
		if(params.getAssessmentStatus() != null && params.getAssessmentStatus().size() > 0) {
			if(entry == null || entry.getAssessmentStatus() == null) {
				ok &= false;
			} else {
				ok &= !params.getAssessmentStatus().contains(entry.getAssessmentStatus());
			}
		}
		return ok;
	}*/
	
	private void fillAlternativeToAssessableIdentityList(AssessmentToolOptions options) {
		List<Group> baseGroups = new ArrayList<>();
		if((assessmentCallback.canAssessRepositoryEntryMembers()
				&& (assessmentCallback.getCoachedGroups() == null || assessmentCallback.getCoachedGroups().isEmpty()))
				|| assessmentCallback.canAssessNonMembers()) {
			baseGroups.add(repositoryService.getDefaultGroup(courseEntry));
		}
		if(assessmentCallback.getCoachedGroups() != null && assessmentCallback.getCoachedGroups().size() > 0) {
			for(BusinessGroup coachedGroup:assessmentCallback.getCoachedGroups()) {
				baseGroups.add(coachedGroup.getBaseGroup());
			}
		}
		options.setAlternativeToIdentities(baseGroups, assessmentCallback.canAssessNonMembers());
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		String filter = null;
		if(state instanceof AssessedIdentityListState) {
			AssessedIdentityListState listState = (AssessedIdentityListState)state;
			if(StringHelper.containsNonWhitespace(listState.getFilter())) {
				filter = listState.getFilter();
			}
		}

		tableEl.setSelectedFilterKey(filter);
		updateModel(ureq, null, tableEl.getSelectedFilters(), null);
		
		if(entries != null && entries.size() > 0) {
			String resourceType = entries.get(0).getOLATResourceable().getResourceableTypeName();
			if("Identity".equals(resourceType)) {
				Long identityKey = entries.get(0).getOLATResourceable().getResourceableId();
				for(int i=usersTableModel.getRowCount(); i--> 0; ) {
					AssessedIdentityElementRow row = usersTableModel.getObject(i);
					if(row.getIdentityKey().equals(identityKey)) {
						doSelect(ureq, row);
					}
				}
			}	
		}
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}
	
	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if(previousLink == source) {
			doPrevious(ureq);
		} else if(nextLink == source) {
			doNext(ureq);
		}
		super.event(ureq, source, event);
	}

	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		if(currentIdentityCtrl == source) {
			if(event == Event.CHANGED_EVENT) {
				updateModel(ureq, currentIdentityCtrl.getAssessedIdentity());
			} else if(event == Event.DONE_EVENT) {
				updateModel(ureq, currentIdentityCtrl.getAssessedIdentity());
				stackPanel.popController(currentIdentityCtrl);
			} else if(event == Event.CANCELLED_EVENT) {
				stackPanel.popController(currentIdentityCtrl);
			}
		}
		super.event(ureq, source, event);
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(tableEl == source) {
			if(event instanceof SelectionEvent) {
				SelectionEvent se = (SelectionEvent)event;
				String cmd = se.getCommand();
				AssessedIdentityElementRow row = usersTableModel.getObject(se.getIndex());
				if("select".equals(cmd)) {
					doSelect(ureq, row);
				}
			} else if(event instanceof FlexiTableSearchEvent) {
				FlexiTableSearchEvent ftse = (FlexiTableSearchEvent)event;
				updateModel(ureq, ftse.getSearch(), ftse.getFilters(), ftse.getExtendedFilters());
			}
		}
		
		super.formInnerEvent(ureq, source, event);
	}
	
	private void doNext(UserRequest ureq) {
		stackPanel.popController(currentIdentityCtrl);
		
		Identity currentIdentity = currentIdentityCtrl.getAssessedIdentity();
		int index = getIndexOf(currentIdentity);
		if(index >= 0) {
			int nextIndex = index + 1;//next
			if(nextIndex >= 0 && nextIndex < usersTableModel.getRowCount()) {
				doSelect(ureq, usersTableModel.getObject(nextIndex));
			} else if(usersTableModel.getRowCount() > 0) {
				doSelect(ureq, usersTableModel.getObject(0));
			}
		}
	}
	
	private void doPrevious(UserRequest ureq) {
		stackPanel.popController(currentIdentityCtrl);
		
		Identity currentIdentity = currentIdentityCtrl.getAssessedIdentity();
		int index = getIndexOf(currentIdentity);
		if(index >= 0) {
			int previousIndex = index - 1;//next
			if(previousIndex >= 0 && previousIndex < usersTableModel.getRowCount()) {
				doSelect(ureq, usersTableModel.getObject(previousIndex));
			} else if(usersTableModel.getRowCount() > 0) {
				doSelect(ureq, usersTableModel.getObject(usersTableModel.getRowCount() - 1));
			}
		}
	}
	
	private int getIndexOf(Identity identity) {
		for(int i=usersTableModel.getRowCount(); i-->0; ) {
			Long rowIdentityKey = usersTableModel.getObject(i).getIdentityKey();
			if(rowIdentityKey.equals(identity.getKey())) {
				return i;
			}
		}
		return -1;
	}
	
	private void doSelect(UserRequest ureq, AssessedIdentityElementRow row) {
		removeAsListenerAndDispose(currentIdentityCtrl);
		
		Identity assessedIdentity = securityManager.loadIdentityByKey(row.getIdentityKey());
		String fullName = userManager.getUserDisplayName(assessedIdentity);

		OLATResourceable ores = OresHelper.createOLATResourceableInstance("Identity", assessedIdentity.getKey());
		WindowControl bwControl = addToHistory(ureq, ores, null);
		if(courseNode.getParent() == null) {
			currentIdentityCtrl = new AssessmentIdentityCourseController(ureq, bwControl, stackPanel,
					courseEntry, assessedIdentity);
		} else {
			currentIdentityCtrl = new AssessmentIdentityCourseNodeController(ureq, getWindowControl(), stackPanel,
					courseEntry, courseNode, assessedIdentity);
		}
		listenTo(currentIdentityCtrl);
		stackPanel.pushController(fullName, currentIdentityCtrl);
		
		previousLink = LinkFactory.createToolLink("previouselement","", this, "o_icon_previous_toolbar");
		previousLink.setTitle(translate("command.previous"));
		stackPanel.addTool(previousLink, Align.rightEdge, false, "o_tool_previous");
		nextLink = LinkFactory.createToolLink("nextelement","", this, "o_icon_next_toolbar");
		nextLink.setTitle(translate("command.next"));
		stackPanel.addTool(nextLink, Align.rightEdge, false, "o_tool_next");
	}
}