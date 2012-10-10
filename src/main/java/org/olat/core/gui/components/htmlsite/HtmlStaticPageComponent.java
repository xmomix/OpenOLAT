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
* <p>
*/ 

package org.olat.core.gui.components.htmlsite;

import javax.servlet.http.HttpServletRequest;

import org.olat.core.CoreSpringFactory;
import org.olat.core.dispatcher.mapper.Mapper;
import org.olat.core.dispatcher.mapper.MapperService;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.ComponentRenderer;
import org.olat.core.gui.media.AsyncMediaResponsible;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.media.NotFoundMediaResource;
import org.olat.core.gui.media.RedirectMediaResource;
import org.olat.core.gui.render.ValidationResult;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.Tracing;
import org.olat.core.util.FileUtils;
import org.olat.core.util.SimpleHtmlParser;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSManager;
import org.olat.core.util.vfs.VFSMediaResource;

/**
 * 
 * @author Felix Jost
 */
public class HtmlStaticPageComponent extends Component implements AsyncMediaResponsible {
	// make public mainly for the IFrameDisplayController
	public static final String OLAT_CMD_PREFIX = "olatcmd/";

	private static final ComponentRenderer RENDERER = new HtmlStaticPageComponentRenderer();

	
	private VFSContainer rootContainer;
	private String currentURI;
	private String htmlHead = null;
	private String jsOnLoad = null;
	private String htmlContent = null;
	private String wrapperCssStyle = null;

	/**
	 * Constructor for an displaying an html page.
	 * 
	 * @param name The component name
	 * @param the root folder which contains the files
	 */
	public HtmlStaticPageComponent(String name, VFSContainer rootContainer) {
		super(name);
		this.rootContainer = rootContainer;
	}

	/**
	 * @see org.olat.core.gui.components.Component#dispatchRequest(org.olat.core.gui.UserRequest)
	 */
	protected void doDispatchRequest(UserRequest ureq) {
		// never called
		throw new AssertException("should never be called: userrequest = " + ureq);
	}

	/**
	 * @see org.olat.core.gui.media.AsyncMediaResponsible#getAsyncMediaResource(org.olat.core.gui.UserRequest)
	 */
	public MediaResource getAsyncMediaResource(UserRequest ureq) {
		// is the path to the desired resource (put together by the webbrowser by
		// combining latesturl and relative link)
		String moduleURI = ureq.getModuleURI();

		MediaResource mr = null;

		//FIXME:fg: FIXED:fj:  HtmlBodyParser now also skips the <link rel="next" etc. links
		//put together under "known issues" etc. --- not here in this
		// code, but make sure that the html generated by docbook for the help
		// course does not contain the following link tags:
		// <link rel="stylesheet" href="../css/olat-help.css" type="text/css"><meta
		// name="generator" content="DocBook XSL Stylesheets V1.61.3">
		// the above one is ok, but the next one is not
		//<link rel="home" href="index.html" title="OLAT Hilfe Kurs">
		// mozilla (and maybe ie also) loads not only the stylesheet, but also the
		// index.html in the background. but .html is for inline-rendering
		// and wastes a timestamp so that the window is messed up (e.g. all clicks
		// all over in olat are considered async

		if (moduleURI != null) {
			// 1. check for an olat command (special link to indicate a command)
			if (moduleURI.startsWith(OLAT_CMD_PREFIX)) {
				String cmdAndSub = moduleURI.substring(OLAT_CMD_PREFIX.length());
				int slpos = cmdAndSub.indexOf('/');
				if (slpos != -1) {
					String cmd = cmdAndSub.substring(0, slpos);
					String subcmd = cmdAndSub.substring(slpos+1);
					OlatCmdEvent aev = new OlatCmdEvent(cmd, subcmd);
					fireEvent(ureq, aev);
					// if the listener(s) accepts the event, then we pass control to the
					// listener by indicating a inline-rerender (return null as
					// Mediaresourse)
					if (aev.isAccepted()) return null;
				} // else ignore (syntax error in command
			}

			// make sure moduleURI does not contain ".." or such (hack attempt)
			// -> ok, userrequest class asserts this.

			VFSItem sourceItem = rootContainer.resolve(moduleURI);
			// return 404 if the requested file does not exist
			if (sourceItem == null || (sourceItem instanceof VFSContainer)) {
				return new NotFoundMediaResource(moduleURI);
			}
			// we know the file exists.
			
			boolean checkRegular = true;
			// check for special case: render the follwing link in a new (server) window and all following clicks as well
			if ( (ureq.getParameter("olatsite") != null) || ( (moduleURI.endsWith(".html") || moduleURI.endsWith(".htm")) && (ureq.getParameter("olatraw") != null) ) ){
				Tracing.logDebug("moduleURI=" + moduleURI, HtmlStaticPageComponent.class);
				ExternalSiteEvent ese = new ExternalSiteEvent(moduleURI);
				fireEvent(ureq, ese);
				if (ese.isAccepted()) {
					mr = ese.getResultingMediaResource();
					Tracing.logDebug("ExternalSiteEvent is accepted", HtmlStaticPageComponent.class);
					checkRegular = false;
				} else {
					// it is a html page with olatraw parameter => redirect to mapper
					Mapper mapper = createMapper(rootContainer);
					// NOTE: do not deregister this mapper, since it could be used a lot later (since it is opened in a new browser window)
					String amapPath;
					
					// Register mapper as cacheable
					String mapperID = VFSManager.getRealPath(rootContainer);
					if (mapperID == null) {
						// Can't cache mapper, no cacheable context available
						amapPath  = CoreSpringFactory.getImpl(MapperService.class).register(ureq.getUserSession(), mapper);
					} else {
						// Add classname to the file path to remove conflicts with other
						// usages of the same file path
						mapperID = this.getClass().getSimpleName() + ":" + mapperID;
						amapPath  = CoreSpringFactory.getImpl(MapperService.class).register(ureq.getUserSession(), mapperID, mapper);				
					}


					ese.setResultingMediaResource(new RedirectMediaResource(amapPath+"/"+moduleURI));
					Tracing.logDebug("RedirectMediaResource=" + amapPath+"/"+moduleURI, HtmlStaticPageComponent.class);
					ese.accept();
					mr = ese.getResultingMediaResource();
					checkRegular = false;

				}
			}
			if (checkRegular) {
				// html and htm files are later rendered inline, all others are served as
				// mediaresource (raw inputstream)
				if ((moduleURI.endsWith(".html") || moduleURI.endsWith(".htm")) && (ureq.getParameter("olatraw") == null)) {
					// we remember what to render inline later and return null to indicate
					// inline rendering
					currentURI = moduleURI;
					getFileContent((VFSLeaf)sourceItem);
					fireEvent(ureq, new NewInlineUriEvent(currentURI));
				} else { // it is indeed an image or such -> serve it
					mr = new VFSMediaResource((VFSLeaf)sourceItem);
				}
			}
		}
		// else // moduleURI == null -> // after a click on some other component
		// -> do a normal inline rendering (reload)
		return mr;
	}

	private Mapper createMapper(final VFSContainer mapperRootContainer) {
		
		Mapper map = new Mapper() {
			public MediaResource handle(String relPath, HttpServletRequest request) {
				Tracing.logDebug("CPComponent Mapper relPath=" + relPath,HtmlStaticPageComponent.class);
				VFSItem currentItem = mapperRootContainer.resolve(relPath);
				if (currentItem == null || (currentItem instanceof VFSContainer)) {
					return new NotFoundMediaResource(relPath);
				}
				VFSMediaResource vmr = new VFSMediaResource((VFSLeaf)currentItem);
				String encoding = SimpleHtmlParser.extractHTMLCharset(((VFSLeaf)currentItem));
				Tracing.logDebug("CPComponent Mapper set encoding=" + encoding,HtmlStaticPageComponent.class);
				vmr.setEncoding(encoding);// 
				return vmr;
			}
			
		};
		return map;
	}



	/**
	 * Sets the start html page, may be null
	 * 
	 * @param currentURI The currentURI to set
	 */
	public void setCurrentURI(String currentURI) {
		if (!isFileTypeSupported(currentURI)) {
			throw new AssertException("can only accept files which are inline renderable(.html, .htm, .txt), but given filename is:" + currentURI);
		}
		this.currentURI = currentURI;
		setDirty(true);
		
		VFSItem sourceItem = null;
		if (rootContainer != null)
			sourceItem = rootContainer.resolve(currentURI);
		if (sourceItem == null || (sourceItem instanceof VFSContainer)) {
			jsOnLoad = null;
			htmlHead = null;
			htmlContent = "File not found: " + currentURI;
			return;
		}
		getFileContent((VFSLeaf)sourceItem);
	}

	/**
	 * Retreives the file from the filesystem and parses the file.
	 */
	private void getFileContent(VFSLeaf sourceFile) {
		if (currentURI == null) return;

		String encoding = SimpleHtmlParser.extractHTMLCharset(sourceFile);
		String data = FileUtils.load(sourceFile.getInputStream(), encoding);
		
		SimpleHtmlParser parser = new SimpleHtmlParser(data);
		htmlHead = parser.getHtmlHead();
		jsOnLoad = parser.getJsOnLoad();
		if (parser.isValidHtml()) {
			htmlContent = parser.getHtmlContent();
		} else {
			htmlContent = data; // if not a full html file, just take the original
		}
	}

	/**
	 * @return Returns the html header from the current page
	 */
	public String getHtmlHead() {
		return htmlHead;
	}

	/**
	 * @return Returns the onload java script calls from the current page
	 */
	public String getJsOnLoad() {
		return jsOnLoad;
	}

	/**
	 * @return Returns the html content from the current page (html body)
	 */
	public String getHtmlContent() {
		return htmlContent;
	}

	/**
	 * @return Returns the current URI as debugginf information
	 */
	public String getExtendedDebugInfo() {
		return "currentURI:" + currentURI + ", htmlcontent len:" + (htmlContent == null ? "null" : "" + htmlContent.length());
	}

	/**
	 * @see org.olat.core.gui.components.Component#validate(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.render.ValidationResult)
	 */
	public void validate(UserRequest ureq, ValidationResult vr) {
		super.validate(ureq, vr);
		boolean redirect = true;
		// FIXME:fj:c make more efficient by comparing latestURL in browser (via vr?, != ureq.getModuleURI() for AJAX!) in case of AJAX enabled
		if (vr.getGlobalSettings().getAjaxFlags().isIframePostEnabled()) {
			redirect = true;
		} else {
			String browserURI = ureq.getModuleURI();
			// browser uri: e.g null or
			if (browserURI == null) { // click on a treenode -> return without redirect
				// only if the currentURI is null (blank content)
				// or it is a root file
				if (currentURI == null || currentURI.indexOf("/") == -1) {
					redirect = false;
				}
			} else if (!ureq.isValidDispatchURI()) { // link from external
				// direct-jump-url or such ->
				// redirect
				redirect = true;
			} else {
				// browser uri != null and normal framework url dispatch = click from
				// within a page; currentURI == browserURI since asyncmedia-call took
				// place before validating.
				// never needs to redirect since browser page calculates relative page and
				// is handled by asyncmediaresponsible
	
				// Exception: if a olatcmd was issued -> the new page has of course not this moduleUri -> redirect needed 
				if (browserURI.startsWith(OLAT_CMD_PREFIX)) {
					//e.g. w:1, t:170, c:207, =http://localhost/olat/auth/1%3A170%3A207%3Anidle%3A71488625713963/olatcmd/gotonode/648
					redirect = true;
				} else {
					//e.g. w:1, t:174, c:207, =http://localhost/olat/auth/1%3A174%3A207%3Anidle%3A71488625714002/deep/index.html
					redirect = false;
				}			
			}
		}
		if (redirect) {
			// Trigger redirect only if not someone else already did issue a redirect.
			// Very bad, what should we do, we have a conflict of interest here we
			// can't solve. First redirect wins...
			if (vr.getNewModuleURI() == null) {
				String newUri = currentURI;
				if (newUri.charAt(0) == '/') {
					newUri = newUri.substring(1);
				}
				vr.setNewModuleURI(newUri);				
			}
		}
		return;
	}

	public ComponentRenderer getHTMLRendererSingleton() {
		return RENDERER;
	}

	/**
	 * Check if a certain file-type (html,htm,txt) is supported.
	 * @param filePath  Full file path, can be null.
	 * @return  true: File-type is supported 
	 */
	public static boolean isFileTypeSupported(String filePath) {
		if (filePath == null) {
			return false;
	  }
		String filePath_ = filePath.toLowerCase();
		return (   filePath_.endsWith(".html") 
				    || filePath_.endsWith(".htm")
				    || filePath_.endsWith(".txt"));
	}

	/**
	 * Set some CSS styles on the element. 
	 * @param wrapperCssStyle CSS rules or NULL to not use them
	 */
	public void setWrapperCssStyle(String wrapperCssStyle) {
		this.wrapperCssStyle = wrapperCssStyle;
	}
	
	/**
	 * Get the wrapper css style or NULL if no wrapper css is defined
	 * @return
	 */
	public String getWrapperCssStyle() {
		return this.wrapperCssStyle;
	}
}