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
package org.olat.modules.assessment.manager;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.TypedQuery;

import org.olat.basesecurity.Group;
import org.olat.basesecurity.IdentityRef;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.modules.assessment.AssessmentEntry;
import org.olat.modules.assessment.model.AssessmentEntryImpl;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 20.07.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class AssessmentEntryDAO {
	
	@Autowired
	private DB dbInstance;
	
	public AssessmentEntry createCourseNodeAssessment(Identity assessedIdentity,
			RepositoryEntry entry, String subIdent, RepositoryEntry referenceEntry) {
		
		AssessmentEntryImpl data = new AssessmentEntryImpl();
		data.setCreationDate(new Date());
		data.setLastModified(data.getCreationDate());
		data.setIdentity(assessedIdentity);
		data.setRepositoryEntry(entry);
		data.setSubIdent(subIdent);
		data.setReferenceEntry(referenceEntry);
		dbInstance.getCurrentEntityManager().persist(data);
		return data;
	}
	
	public AssessmentEntry createCourseNodeAssessment(Identity assessedIdentity,
			RepositoryEntry entry, String subIdent, RepositoryEntry referenceEntry,
			Float score, Boolean passed) {
		
		AssessmentEntryImpl data = new AssessmentEntryImpl();
		data.setCreationDate(new Date());
		data.setLastModified(data.getCreationDate());
		data.setIdentity(assessedIdentity);
		data.setRepositoryEntry(entry);
		data.setSubIdent(subIdent);
		data.setReferenceEntry(referenceEntry);
		if(score != null) {
			data.setScore(new BigDecimal(score));
		}
		data.setPassed(passed);
		dbInstance.getCurrentEntityManager().persist(data);
		return data;
	}
	
	public AssessmentEntry loadAssessmentEntryById(Long id) {
		List<AssessmentEntry> nodeAssessment = dbInstance.getCurrentEntityManager()
				.createNamedQuery("loadAssessmentEntryById", AssessmentEntry.class)
				.setParameter("key", id)
				.getResultList();
		return nodeAssessment.isEmpty() ? null : nodeAssessment.get(0);
	}

	public AssessmentEntry loadAssessmentEntry(IdentityRef assessedIdentity, RepositoryEntryRef entry, String subIdent) {
		TypedQuery<AssessmentEntry> query;
		if(subIdent == null) {
			query = dbInstance.getCurrentEntityManager()
				.createNamedQuery("loadAssessmentEntryByRepositoryEntryAndUserAndNullSubIdent", AssessmentEntry.class);
		} else {
			query = dbInstance.getCurrentEntityManager()
				.createNamedQuery("loadAssessmentEntryByRepositoryEntryAndUserAndSubIdent", AssessmentEntry.class)
				.setParameter("subIdent", subIdent);
		}
		List<AssessmentEntry> entries = query
			.setParameter("repositoryEntryKey", entry.getKey())
			.setParameter("identityKey", assessedIdentity.getKey())
			.getResultList();
		return entries.isEmpty() ? null : entries.get(0);
	}
	
	public AssessmentEntry loadAssessmentEntry(IdentityRef assessedIdentity, RepositoryEntryRef entry, String subIdent, String referenceSoftKey) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("select data from assessmententry data");
		if(referenceSoftKey != null) {
			sb.append(" inner join data.referenceEntry referenceEntry");
		}
		
		sb.append(" where data.repositoryEntry.key=:repositoryEntryKey and data.identity.key=:identityKey");
		if(subIdent != null) {
			sb.append(" and data.subIdent=:subIdent");
		} else {
			sb.append(" and data.subIdent is null");
		}
		
		if(referenceSoftKey != null) {
			sb.append(" and referenceEntry.softkey=:softkey");
		} else {
			sb.append(" and data.referenceEntry is null");
		}

		TypedQuery<AssessmentEntry> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), AssessmentEntry.class)
				.setParameter("repositoryEntryKey", entry.getKey())
				.setParameter("identityKey", assessedIdentity.getKey());
		if(subIdent != null) {
			query.setParameter("subIdent", subIdent);
		}
		if(referenceSoftKey != null) {
			query.setParameter("softkey", referenceSoftKey);
		}
		List<AssessmentEntry> entries = query.getResultList();
		return entries.isEmpty() ? null : entries.get(0);
	}
	
	public AssessmentEntry loadAssessmentEntry(IdentityRef assessedIdentity, RepositoryEntryRef entry, String subIdent, RepositoryEntryRef referenceEntry) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("select data from assessmententry data");
		if(referenceEntry != null) {
			sb.append(" inner join data.referenceEntry referenceEntry");
		}
		
		sb.append(" where data.repositoryEntry.key=:repositoryEntryKey and data.identity.key=:identityKey");
		if(subIdent != null) {
			sb.append(" and data.subIdent=:subIdent");
		} else {
			sb.append(" and data.subIdent is null");
		}
		
		if(referenceEntry != null) {
			sb.append(" and referenceEntry.key=:referenceEntryKey");
		} else {
			sb.append(" and data.referenceEntry is null");
		}

		TypedQuery<AssessmentEntry> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), AssessmentEntry.class)
				.setParameter("repositoryEntryKey", entry.getKey())
				.setParameter("identityKey", assessedIdentity.getKey());
		if(subIdent != null) {
			query.setParameter("subIdent", subIdent);
		}
		if(referenceEntry != null) {
			query.setParameter("referenceEntryKey", referenceEntry.getKey());
		}
		List<AssessmentEntry> entries = query.getResultList();
		return entries.isEmpty() ? null : entries.get(0);
	}
	
	public AssessmentEntry updateAssessmentEntry(AssessmentEntry nodeAssessment) {
		((AssessmentEntryImpl)nodeAssessment).setLastModified(new Date());
		return dbInstance.getCurrentEntityManager().merge(nodeAssessment);
	}
	
	public List<AssessmentEntry> loadAssessmentEntryBySubIdent(RepositoryEntryRef entry, String subIdent) {
		return dbInstance.getCurrentEntityManager()
				.createNamedQuery("loadAssessmentEntryByRepositoryEntryAndSubIdent", AssessmentEntry.class)
				.setParameter("repositoryEntryKey", entry.getKey())
				.setParameter("subIdent", subIdent)
				.getResultList();
	}
	
	public List<Identity> getAllIdentitiesWithAssessmentData(RepositoryEntryRef entry) {
		StringBuilder sb = new StringBuilder();
		sb.append("select distinct data.identity from assessmententry data where data.repositoryEntry.key=:repositoryEntryKey");
		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Identity.class)
				.setParameter("repositoryEntryKey", entry.getKey())
				.getResultList();
	}

	public List<AssessmentEntry> loadAssessmentEntriesByAssessedIdentity(Identity assessedIdentity, RepositoryEntry entry) {
		StringBuilder sb = new StringBuilder();
		sb.append("select data from assessmententry data where data.repositoryEntry.key=:repositoryEntryKey and data.identity.key=:identityKey");
		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), AssessmentEntry.class)
				.setParameter("repositoryEntryKey", entry.getKey())
				.setParameter("identityKey", assessedIdentity.getKey())
				.getResultList();
	}

	public List<AssessmentEntry> loadAssessmentEntryByBusinessGroup(Group assessedGroup, RepositoryEntry entry, String subIdent) {
		StringBuilder sb = new StringBuilder();
		sb.append("select data from assessmententry data")
		  .append(" inner join data.repositoryEntry v")
		  .append(" inner join v.groups as relGroup on relGroup.defaultGroup=false")
		  .append(" where relGroup.group.key=:groupKey and v.key=:repositoryEntryKey and data.subIdent=:subIdent");
		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), AssessmentEntry.class)
				.setParameter("repositoryEntryKey", entry.getKey())
				.setParameter("groupKey", assessedGroup.getKey())
				.setParameter("subIdent", subIdent)
				.getResultList();
	}
	
	/**
	 * Delete all the entry where the specified repository entry is
	 * referenced as a test.
	 * 
	 * @param entry
	 * @return
	 */
	public int deleteEntryForReferenceEntry(RepositoryEntryRef entry) {
		StringBuilder sb = new StringBuilder();
		sb.append("delete from assessmententry data where data.referenceEntry.key=:referenceKey");
		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString())
				.setParameter("referenceKey", entry.getKey())
				.executeUpdate();
	}
	
	/**
	 * Delete all entries where the specified repository entry (typically
	 * a course) is linked to them.
	 * 
	 * @param entry
	 * @return
	 */
	public int deleteEntryForRepositoryEntry(RepositoryEntryRef entry) {
		StringBuilder sb = new StringBuilder();
		sb.append("delete from assessmententry data where data.repositoryEntry.key=:entryKey");
		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString())
				.setParameter("entryKey", entry.getKey())
				.executeUpdate();
	}
}