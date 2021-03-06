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
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/
package org.olat.core.util.threadlog;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.olat.core.configuration.PersistedProperties;

/**
 * This class manages the ip addresses and the associated loglevel/appender pairs
 * in conjunction with the ThreadLocalLogLevelManager which it calls to do the
 * actual threadlocal based log level controlling.
 * <p>
 * This class is basically a Map containing the ip addresses for which log levels/appenders
 * are modified.
 * <P>
 * Initial Date:  13.09.2010 <br>
 * @author Stefan
 */
public class RequestBasedLogLevelManager {
	
	/** Name of the persistentproperty which contains the list of ip addresses to loglevel/appenders **/
	private final String PROP_NAME_REQUESTBASED_IPS = "RequestBasedIps";
	
	/** The core of this class is this map containing the list of ip addresses mapped to logconfigs **/
	private final Map<String,LogConfig> remoteAddrs2LogConfigs = new ConcurrentHashMap<String,LogConfig>();
	
	/** A reference to the persistentProperties used to persistent the ip address map **/
	private PersistedProperties persistentProperties;
	
	/** A reference to the ThreadLocalLogLevelManager is used to trigger the actual threadlocal based log level controlling**/
	private final ThreadLocalLogLevelManager threadLocalLogLevelManager;
	
	/** semi-old-school way of allowing controllers to access a manager - via this INSTANCE construct **/
	private static RequestBasedLogLevelManager INSTANCE;
	
	/** semi-old-school way of allowing controllers to access a manager - via this INSTANCE construct **/
	public static RequestBasedLogLevelManager getInstance() {
		return INSTANCE;
	}
	
	/**
	 * Creates the RequestBasedLogLevelManager - which is a SINGLETON and should only be installed
	 * once per VM.
	 * @param threadLocalLogLevelManager the ThreadLocalLogLevelManager is used to trigger the actual threadlocal based log level controlling
	 */
	public RequestBasedLogLevelManager(ThreadLocalLogLevelManager threadLocalLogLevelManager) {
		if (threadLocalLogLevelManager==null) {
			throw new IllegalArgumentException("threadLocalLogLevelManager must not be null");
		}
		this.threadLocalLogLevelManager = threadLocalLogLevelManager;
		INSTANCE = this;
	}
	
	/** Sets the PersistedProperties of this manager **/
	public void setPersistentProperties(PersistedProperties persistentProperties) {
		if (persistentProperties==null) {
			throw new IllegalArgumentException("persistentProperties must not be null");
		}
		this.persistentProperties = persistentProperties;
		init();
	}
	
	/** (re)initializes the manager by resetting the map and loading it using the PersistentProperties mechanism **/
	void init() {
		reset();
		String ipsAndLevels = loadIpsAndLevels();

		if (ipsAndLevels!=null) {
			String[] ipsAndLevelArray = ipsAndLevels.split("\r\n");
			for (int i = 0; i < ipsAndLevelArray.length; i++) {
				String anIpAndLevel = ipsAndLevelArray[i];
				if (anIpAndLevel!=null && anIpAndLevel.length()>0 && anIpAndLevel.contains("=")) {
					setLogLevelAndAppender(anIpAndLevel);
				}
			}
		}
	}
	
	/** Loads the ip to loglevel/appender map using the PersistentProperties mechanism **/
	public String loadIpsAndLevels() {
		try{
			return persistentProperties.getStringPropertyValue(PROP_NAME_REQUESTBASED_IPS, true);
		} catch(Exception e) {
			Logger.getLogger(RequestBasedLogLevelManager.class).warn("loadIpsAndLevels: Error loading property value "+PROP_NAME_REQUESTBASED_IPS, e);
			return null;
		}
	}
	
	/** Stores the ip to loglevel/appender map using the PersistentProperties mechanism **/
	public void storeIpsAndLevels(String ipsAndLevels) {
		try{
			persistentProperties.setStringProperty(PROP_NAME_REQUESTBASED_IPS, ipsAndLevels, true);
		} catch(Exception e) {
			Logger.getLogger(RequestBasedLogLevelManager.class).warn("storeIpsAndLevels: Error storing property value "+PROP_NAME_REQUESTBASED_IPS, e);
		}
	}
	
	/** 
	 * Clears the in-memory ip to loglevel/appender map - not a full reinit method, use init for that
	 * @see RequestBasedLogLevelManager#init()
	 **/
	public void reset() {
		remoteAddrs2LogConfigs.clear();
	}
	
	/**
	 * Sets a particular IP to a particular loglevel/appender using the format
	 * 192.168.0.1=DEBUG,DebugLog
	 * @param configStr a one line configuration string in the following format: 192.168.0.1=DEBUG,DebugLog
	 */
	public void setLogLevelAndAppender(String configStr) {
		StringTokenizer st = new StringTokenizer(configStr, "=");
		String anIpAddress = st.nextToken();
		String logConfig = st.nextToken();
		Level level;
		Appender appender;
		if (logConfig.contains(",")) {
			st = new StringTokenizer(logConfig, ",");
			level = Level.toLevel(st.nextToken());
			String categoryAppenderStr = st.nextToken();
			Logger l = Logger.getLogger(categoryAppenderStr);
			if (l!=null) {
				appender = l.getAppender(categoryAppenderStr);
				if (appender==null) {
					appender = Logger.getRootLogger().getAppender(categoryAppenderStr);
				}
			} else {
				appender = null;
			}
		} else {
			level = Level.toLevel(logConfig);
			appender = null;
		}
		setLogLevelAndAppenderForRemoteAddr(anIpAddress, level, appender);
	}
	
	/** internal helper method which takes care of the actual modifying of the ip to loglevel/appender map **/
	private void setLogLevelAndAppenderForRemoteAddr(String remoteAddr, Priority level, Appender appender) {
		if (level==null) {
			remoteAddrs2LogConfigs.remove(remoteAddr);
		} else {
			remoteAddrs2LogConfigs.put(remoteAddr, new LogConfig(level, appender));
		}
	}
	
	/**
	 * Activates the ThreadLocalAwareLogger for this request if the remote address matches a configured IP.
	 * <p>
	 * This method is used very frequently and should hence be performant!
	 * @param request the request of which the remote address is matched against the map of ips
	 */
	public void activateRequestBasedLogLevel(HttpServletRequest request) {
		LogConfig logConfig = remoteAddrs2LogConfigs.get(request.getRemoteAddr());
		if (logConfig!=null) {
			threadLocalLogLevelManager.forceThreadLocalLogLevel(logConfig);
		} else {
			threadLocalLogLevelManager.releaseForcedThreadLocalLogLevel();
		}
	}
	
	/**
	 * Deactivate the ThreadLocalAwareLogger if it was previously activated - does nothing otherwise
	 */
	public void deactivateRequestBasedLogLevel() {
		threadLocalLogLevelManager.releaseForcedThreadLocalLogLevel();
	}

}
