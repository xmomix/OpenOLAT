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
package org.olat.modules.quality.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 * Initial date: 12.09.2018<br>
 * 
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class GroupedStatistics {

	private final Map<String, Map<MultiKey, GroupedStatistic>> statistics = new HashMap<>();

	public GroupedStatistics() {
		//
	}

	public GroupedStatistics(Collection<GroupedStatistic> collection) {
		for (GroupedStatistic statistic : collection) {
			putStatistic(statistic);
		}
	}

	public GroupedStatistic getStatistic(String identifier, MultiKey multiKey) {
		Map<MultiKey, GroupedStatistic> grouped = statistics.get(identifier);
		if (grouped != null) {
			return grouped.get(multiKey);
		}
		return null;
	}

	public void putStatistic(GroupedStatistic statistic) {
		String identifier = statistic.getIdentifier();
		MultiKey multiKey = statistic.getMultiKey();
		Map<MultiKey, GroupedStatistic> grouped = statistics.get(identifier);
		if (grouped == null) {
			grouped = new HashMap<>();
			statistics.put(identifier, grouped);
		}
		grouped.put(multiKey, statistic);
	}

	public Map<MultiKey, GroupedStatistic> getStatistics(String identifier) {
		return statistics.get(identifier);
	}

	public Collection<GroupedStatistic> getStatistics() {
		Collection<GroupedStatistic> all = new ArrayList<>();
		for (Map<MultiKey, GroupedStatistic> grouped : statistics.values()) {
			all.addAll(grouped.values());
		}
		return all;
	}
	
	public Set<MultiKey> getKeys() {
		Set<MultiKey> keys = new HashSet<>();
		for (GroupedStatistic statistic : getStatistics()) {
			keys.add(statistic.getMultiKey());
		}
		return keys;
	}
}
