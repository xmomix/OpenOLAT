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
package org.olat.modules.forms.model.jpa;

import javax.persistence.Query;

import org.olat.core.commons.persistence.QueryBuilder;
import org.olat.modules.forms.EvaluationFormSessionStatus;
import org.olat.modules.forms.EvaluationFormSurvey;
import org.olat.modules.forms.SessionFilter;

/**
 * 
 * Initial date: 10.09.2018<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class SurveyFilter implements SessionFilter {

	private final EvaluationFormSurvey survey;

	public SurveyFilter(EvaluationFormSurvey survey) {
		this.survey = survey;
	}

	@Override
	public String getSelectKeys() {
		QueryBuilder sb = new QueryBuilder(128);
		sb.append("select sessionFilter.key");
		sb.append("  from evaluationformsession sessionFilter");
		sb.and().append("sessionFilter.status = '").append(EvaluationFormSessionStatus.done).append("'");
		sb.and().append("sessionFilter.survey.key = :surveyFilterKey");
		return sb.toString();
	}

	@Override
	public void addParameters(Query query) {
		query.setParameter("surveyFilterKey", survey.getKey());
	}

}
