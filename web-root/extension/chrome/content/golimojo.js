/************************************************************
Copyright (c) 2005, 2010, 2015, Curtis Bartley and Golimojo Project Contributors.
All rights reserved.
 
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
 
* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.
 
* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
 
* Neither the name of the Golimojo Project, Curtis Bartley, nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.
 
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
************************************************************/

// ------------------------------------------------------------
// ----------------------- class Starter ----------------------
// ------------------------------------------------------------

// ---------------------------------------- Starter constructor

function Starter()
{
    this.sidebarMonitor = new SidebarMonitor("viewGolimojoSidebar");
    Tools.registerObserver(this, "StartDocumentLoad");
}

// ---------------------------------------- Starter observe

Starter.prototype.observe = function (source, eventName)
{
    Tools.unregisterObserver(this, "StartDocumentLoad");
    if (this.isFirstRunForNewExtension())
    {
        this.showWelcomeScreen();
    }
}

// ---------------------------------------- Starter isFirstRunForNewExtension

Starter.prototype.isFirstRunForNewExtension = function ()
{
    var lastBuildId = Tools.getPreference("buildId");
    if (lastBuildId == window.buildId) return false;
    Tools.setPreference("buildId", window.buildId);
    return true;
}

// ---------------------------------------- Starter showWelcomeScreen

Starter.prototype.showWelcomeScreen = function ()
{
    // Always show the sidebar on first start.
    this.sidebarMonitor.openSidebar();

    // Skip the welcome screen if we're obviously running a test configuration.
    if (serverName == "localhost") return;

    // Load the welcome screen into the first tab.
    var tabWindowList = Tools.getTabWindows();
    if (tabWindowList.length > 0)
    {
        var tabWindow = tabWindowList[0];
        tabWindow.location.href = "http://" + serverName + ":" + serverPort + "/extension-welcome.html";
    }
}

// ------------------------------------------------------------
// ------------------- class SidebarMonitor -------------------
// ------------------------------------------------------------

// ---------------------------------------- SidebarMonitor constructor

function SidebarMonitor(sidebarCommandName)
{
    this.sidebarCommandName = sidebarCommandName;
    this.isSidebarOpen = this.checkIsSidebarOpen();
    this.driver = new Driver();
    Tools.registerObserver(this, "golimojo-sidebar-opened");
    Tools.registerObserver(this, "golimojo-sidebar-closed");
    Tools.registerObserver(this, "golimojo-sidebar-select");
}

// ---------------------------------------- SidebarMonitor openSidebar

SidebarMonitor.prototype.openSidebar = function ()
{
    if (!this.isSidebarOpen) toggleSidebar(this.sidebarCommandName);
}

// ---------------------------------------- SidebarMonitor observe

SidebarMonitor.prototype.observe = function (sidebarWindow, eventName)
{
    // If the event belonged to a sidebar in another window, then ignore it.
    if (sidebarWindow.top != window) return;

    if (eventName == "golimojo-sidebar-opened")
    {
        sidebarWindow.sidebar.initialize(Tools);
        this.driver.engage();
    }

    else if (eventName == "golimojo-sidebar-closed")
    {
        this.driver.disengage();
    }

    else if (eventName == "golimojo-sidebar-select")
    {
        Log.print("golimojo-sidebar-select " + sidebarWindow.sidebar.linkStyle + " " + sidebarWindow.sidebar.linkColor);
        
        var linkStyle = sidebarWindow.sidebar.linkStyle;
        var linkColor = sidebarWindow.sidebar.linkColor;
        Styler.styler.updateStyles(Tools.getTabWindows(), linkStyle, linkColor);
    }
}

// ---------------------------------------- SidebarMonitor checkIsSidebarOpen

SidebarMonitor.prototype.checkIsSidebarOpen = function ()
{
    var sidebarBox = document.getElementById("sidebar-box");
    return (sidebarBox.getAttribute("sidebarcommand") == this.sidebarCommandName);
}

// ------------------------------------------------------------
// ----------------------- class Driver -----------------------
// ------------------------------------------------------------

function Driver()
{
    this.observe = Tools.createCallback(this.observe, this);
}

// ---------------------------------------- Driver observe

Driver.prototype.observe = function (windowWrapper)
{
    WindowDriver.engageWindow(windowWrapper.wrappedJSObject);
}

// ---------------------------------------- Driver engage

Driver.prototype.engage = function ()
{
    // Listen for the EndDocumentLoadEvent.
    with (Components.classes["@mozilla.org/observer-service;1"])
    {
        with (getService(Components.interfaces["nsIObserverService"]))
        {
            addObserver(this, "EndDocumentLoad", false);
        }
    }
    
    // If there are any windows already open then we want to engage them
    // directly since we may be too late to get an event for them.
    var tabWindowList = Tools.getTabWindows();
    for (var i = 0; i < tabWindowList.length; i++)
    {
        var tabWindow = tabWindowList[i];
        WindowDriver.engageWindow(tabWindow);
    }
}

// ---------------------------------------- Driver disengage

Driver.prototype.disengage = function ()
{
    // Stop listening for events.
    with (Components.classes["@mozilla.org/observer-service;1"])
    {
        with (getService(Components.interfaces["nsIObserverService"]))
        {
            removeObserver(this, "EndDocumentLoad");
        }
    }
    
    // Disengage any currently engaged windows.
    var tabWindowList = Tools.getTabWindows();
    for (var i = 0; i < tabWindowList.length; i++)
    {
        var tabWindow = tabWindowList[i];
        WindowDriver.disengageWindow(tabWindow);
    }
}

// ------------------------------------------------------------
// -------------------- class WindowDriver --------------------
// ------------------------------------------------------------

// ---------------------------------------- WindowDriver constructor

function WindowDriver(targetWin)
{
    this.onPageShowCallback = Tools.createCallback(this.onPageShow, this);
    this.onPageHideCallback = Tools.createCallback(this.onPageHide, this);
    targetWin.golimojoDriver = this;
    this.targetWin = targetWin;
    this.linker = null;
}

// ---------------------------------------- WindowDriver engageWindow (static)

WindowDriver.engageWindow = function (targetWin)
{
    if (targetWin.location.protocol != "http:") return;
    if (targetWin.gxPageType == "SystemPage") return;
    if (targetWin.golimojoDriver != null) return;

    targetWin.golimojoDriver = new WindowDriver(targetWin);
    targetWin.golimojoDriver.engage();
}

// ---------------------------------------- WindowDriver disengageWindow (static)

WindowDriver.disengageWindow = function (targetWin)
{
    if (targetWin.golimojoDriver != null) 
    {
        targetWin.golimojoDriver.disengage();
        targetWin.golimojoDriver = null;
    }
}

// ---------------------------------------- WindowDriver engage

WindowDriver.prototype.engage = function ()
{
    this.targetWin.addEventListener("pageshow", this.onPageShowCallback, false);
    this.targetWin.addEventListener("pagehide", this.onPageHideCallback, false);

    function onPageDataReceived(pageData)
    {
        var pageTitleList = pageData.pageTitleList;
        this.linker = new Linker(this.targetWin.document, pageTitleList);
        this.linker.engage();
    }

    var ajaxCall = new AjaxGetPageDataCall(this.targetWin.location.href);
    ajaxCall.send(Tools.createCallback(onPageDataReceived, this));
}

// ---------------------------------------- WindowDriver disengage

WindowDriver.prototype.disengage = function ()
{
    this.targetWin.removeEventListener("pageshow", this.onPageShowCallback, false);
    this.targetWin.removeEventListener("pagehide", this.onPageHideCallback, false);
    if (this.linker != null)
    {
        this.linker.disengage();
    }
}

// ---------------------------------------- WindowDriver onPageShow

WindowDriver.prototype.onPageShow = function ()
{
    if (this.linker != null)
    {
        this.linker.startLinking();
    }
}

// ---------------------------------------- WindowDriver onPageHide

WindowDriver.prototype.onPageHide = function ()
{
    if (this.linker != null)
    {
        this.linker.stopLinking();
    }
}

// ------------------------------------------------------------
// ----------------- class AjaxGetPageDataCall ----------------
// ------------------------------------------------------------

// ---------------------------------------- AjaxGetPageDataCall constructor

function AjaxGetPageDataCall(url)
{
    var ajaxServletUrl = 
        "http://#serverName#:#serverPort#/servlet/get-page-data"
        .replace(/#serverName#/g, window.serverName)
        .replace(/#serverPort#/g, ""  + window.serverPort)
    ;
    
    var strippedUrl = url.replace("http://", "");
    this.ajaxCallUrl = ajaxServletUrl + "?" + escape(strippedUrl);
}

// ---------------------------------------- AjaxGetPageDataCall send

AjaxGetPageDataCall.prototype.send = function (callback)
{
    var request = this.createAjaxRequest();

    // Define the handler function.
    function handleReadyStateChange()
    {
        if (request.readyState == 4)
        {
            callback(new PageData(request.responseXML));
        }
    }

    // Send the request asynchronously.
    Log.print("Sending Ajax Request: " + this.ajaxCallUrl);
    handleReadyStateChangeCallback = Tools.createCallback(handleReadyStateChange, this);
    request.onreadystatechange = handleReadyStateChangeCallback;
    request.open("GET", this.ajaxCallUrl);
    request.send("");
}

// ---------------------------------------- AjaxGetPageDataCall createAjaxRequest

AjaxGetPageDataCall.prototype.createAjaxRequest = function ()
{
    if (window.XMLHttpRequest != null) return new XMLHttpRequest();
    if (window.ActiveXObject != null) return new ActiveXObject("Microsoft.XMLHTTP");
    throw new Error("XMLHttpRequest is not supported!");
}

// ------------------------------------------------------------
// ---------------------- class PageData ----------------------
// ------------------------------------------------------------

// ---------------------------------------- PageData constructor

function PageData(xmlRootNode)
{
    this.pageTitleList = this.getPageTitlesFromXml(xmlRootNode);
}

// ---------------------------------------- PageData getPageTitlesFromXml

PageData.prototype.getPageTitlesFromXml = function (xmlRootNode)
{
    var pageTitleList = [];
    var pageTitleElementList = xmlRootNode.getElementsByTagName("page-title");
    for (var i = 0; i < pageTitleElementList.length; i++)
    {
        var pageTitleElement = pageTitleElementList[i];
        var pageTitle = pageTitleElement.textContent;
        pageTitleList.push(pageTitle);
    }
    return pageTitleList;
}

// ------------------------------------------------------------
// ---------------------- initialization ----------------------
// ------------------------------------------------------------

try
{
    var gStarter = new Starter();
}
catch (e)
{
    Log.print(e);
}
