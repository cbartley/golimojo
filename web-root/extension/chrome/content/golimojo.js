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
// ----------------------- class Driver -----------------------
// ------------------------------------------------------------

// ---------------------------------------- Driver constructor

function Driver(targetWin)
{
    this.targetWin = targetWin;
    this.linker = null;
}

// ---------------------------------------- Driver start

Driver.prototype.start = function ()
{
Log.print("### start");
    var onPageShowCallback = Tools.createCallback(this.onPageShow, this);
    var onPageHideCallback = Tools.createCallback(this.onPageHide, this);
    this.targetWin.addEventListener("pageshow", onPageShowCallback, false);
    this.targetWin.addEventListener("pagehide", onPageHideCallback, false);

    function onPageDataReceived(pageData)
    {
        var pageTitleList = pageData.pageTitleList;
        this.startLinking(this.targetWin.document, pageTitleList);
    }

    var ajaxCall = new AjaxGetPageDataCall(this.targetWin.location.href);
    ajaxCall.send(Tools.createCallback(onPageDataReceived, this));
}

// ---------------------------------------- Driver onPageShow

Driver.prototype.onPageShow = function ()
{
Log.print("### pageshow");
    this.linker.startLinking();
}

// ---------------------------------------- Driver onPageHide

Driver.prototype.onPageHide = function ()
{
Log.print("### pagehide");
    this.linker.stopLinking();
}

// ---------------------------------------- Driver startLinking

Driver.prototype.startLinking = function (targetDoc, pageTitleList)
{
    this.linker = new Linker(targetDoc, pageTitleList);
    this.linker.startLinking();
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
// ------------------------- class Log ------------------------
// ------------------------------------------------------------

// ---------------------------------------- Log constructor

function Log()
{
}

// ---------------------------------------- static Log print

Log.print = function (msg)
{
    var msgText = Log.stringify(msg);
    var tabLog = Log.getTabLog();
    tabLog.logMessage(msgText);
}

// ---------------------------------------- static Log printHtml

Log.printHtml = function (msgHtml)
{
    var tabLog = Log.getTabLog();
    tabLog.logHtmlMessage(msgHtml);
}

// ---------------------------------------- static Log getTabLog

Log.getTabLog = function (tabLog)
{
    var tabBrowser = getBrowser();
    var browserList = tabBrowser.browsers;
    for (var i = 0; i < browserList.length; i++)
    {
        var browser = browserList[i];
        var contentWindow = browser.contentWindow.wrappedJSObject;
        var tabLog = contentWindow.golimojoTabLog;
        if (tabLog != null) return tabLog;
    }
    return null;
}

// ---------------------------------------- static Log stringify

Log.stringify = function (msg)
{
    if (msg instanceof Error) return Log.inspectObject(msg);
    return "" + msg;
}

// ---------------------------------------- static Log inspectObject

Log.inspectObject = function (obj)
{
    var filler = "........................................";
    function fixedWidth(s, width)
    {
        var widthFiller = filler.substring(0, width);
        return s + widthFiller.substring(s.length);
    }

    var idList = [];
    var maxIdLen = 0;
    for (var id in obj)
    {
        maxIdLen = Math.max(id.length, maxIdLen);
        idList.push(id);
    }

    var lineList = [];
    for (var id in obj)
    {
        var value = obj[id];
        var line = fixedWidth(id, maxIdLen) + " ==> " + value;
        lineList.push(line);
    }
    return lineList.join("\n");
}

// ------------------------------------------------------------
// ------------------------ class Tools -----------------------
// ------------------------------------------------------------

// ---------------------------------------- Tools constructor

function Tools()
{
}

// ---------------------------------------- static Tools createCallback

Tools.createCallback = function (fun, receiver)
{
    function callback()
    {
        try
        {
            return fun.apply(receiver, arguments);
        }
        catch (e)
        {
            Log.print(e);
            return null;
        }
    }

    return callback;
}


// ------------------------------------------------------------
// ------------------------ class Test ------------------------
// ------------------------------------------------------------

// ---------------------------------------- Test constructor

function Test()
{
}

/* ---------------------------------------- static Test assert */

Test.assert = function (testResult, optionalContext, optionalValue)
{
    if (testResult) return;
    var msg = "Error: Assertion Failed!\n";
    if (optionalContext != null) 
    {
        msg += "[context] " + optionalContext + "\n";
    }
    if (optionalValue != null)
    {
        msg += "[value]   " + optionalValue + "\n";
    }
    alert(msg);
    throw msg;
}

// ------------------------------------------------------------
// ---------------------- initialization ----------------------
// ------------------------------------------------------------

function initialize()
{
    // Declare a function to handle the end-document-load event.
    function onEndDocumentLoad(windowWrapper)
    {
        var win = windowWrapper.wrappedJSObject;
        if (win.location.protocol != "http:") return;
        if (win.golimojoDriver != null) return;
        win.golimojoDriver = new Driver(win);
        win.golimojoDriver.start();
    }

    // Create an observer object and bind the callback function to it.
    var observer = {};
    observer.observe = Tools.createCallback(onEndDocumentLoad, observer);

    // Register the observer.
    with (Components.classes["@mozilla.org/observer-service;1"])
    {
        with (getService(Components.interfaces["nsIObserverService"]))
        {
            addObserver(observer, "EndDocumentLoad", false);
        }
    }

                    var xobserver = 
                    {
                        observe: function (obj, eventName)
                        {
                            Log.print("### " + eventName);
                            try
                            {
                                if (obj != null && eventName == "chrome-webnavigation-create")
                                {
                                    var docShell = obj.QueryInterface(Components.interfaces.nsIDocShell);
                                    Log.print("*** " + docShell);

                                }
                            }
                            catch (e)
                            {
                                Log.print("### " + e);
                            }
                        }
                    };

                    // Register the observer.
                    with (Components.classes["@mozilla.org/observer-service;1"])
                    {
                        with (getService(Components.interfaces["nsIObserverService"]))
                        {
                            addObserver(xobserver, "*", false);
                        }
                    }

}

initialize();
