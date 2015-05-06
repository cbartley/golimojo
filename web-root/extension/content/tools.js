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
    if (tabLog != null)
    {
        tabLog.logMessage(msgText);
    }
}

// ---------------------------------------- static Log printHtml

Log.printHtml = function (msgHtml)
{
    var tabLog = Log.getTabLog();
    if (tabLog != null)
    {
        tabLog.logHtmlMessage(msgHtml);
    }
}

// ---------------------------------------- static Log getTabLog

Log.getTabLog = function (tabLog)
{
    var tabWindowList = Tools.getTabWindows();
    for (var i = 0; i < tabWindowList.length; i++)
    {
        var tabWindow = tabWindowList[i];
        var tabLog = tabWindow.golimojoTabLog;
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

// ---------------------------------------- static Log interrogate

Log.interrogate = function (obj)
{

    try
    {
        function supportsInterface(obj, interface)
        {
            try
            {
                obj.QueryInterface(interface);
                return true;
            }
            catch (e)
            {
                return false;
            }
        }

        for (var id in Components.interfaces)
        {
            var interface = Components.interfaces[id];
            if (supportsInterface(obj, interface))
            {
                Log.print("--- " + id);
            }
        }
    }
    catch (e)
    {
        Log.print(e);
    }

}

// ---------------------------------------- static Log traverseTree

Log.traverseTree = function (node)
{
    Log.print("+++ " + node);
    for (var i = 0; i < node.childCount; i++)
    {
        var child = node.childAt(i);
        Log.traverseTree(child);
    }


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

// ---------------------------------------- static Tools registerObserver

Tools.registerObserver = function (observer, eventName)
{
    // Make sure the observe method is properly wrapped as a callback.
    if (!observer.observe.callbacked)
    {
        observer.observe = Tools.createCallback(observer.observe, observer);
        observer.observe.callbacked = true;
    }

    // Register the observer.
    with (Components.classes["@mozilla.org/observer-service;1"])
    {
        with (getService(Components.interfaces["nsIObserverService"]))
        {
            addObserver(observer, eventName, false);
        }
    }
}

// ---------------------------------------- static Tools unregisterObserver

Tools.unregisterObserver = function (observer, eventName)
{
    with (Components.classes["@mozilla.org/observer-service;1"])
    {
        with (getService(Components.interfaces["nsIObserverService"]))
        {
            removeObserver(observer, eventName, false);
        }
    }
}

// ---------------------------------------- static Tools notifyObservers

Tools.notifyObservers = function (subject, eventName)
{
    with (Components.classes["@mozilla.org/observer-service;1"])
    {
        with (getService(Components.interfaces["nsIObserverService"]))
        {
            notifyObservers(subject, eventName, null);
        }
    }
}

// ---------------------------------------- static Tools getTabWindows

Tools.getTabWindows = function ()
{
    var tabWindowList = [];
    var tabBrowser = top.getBrowser();
    var browserList = tabBrowser.browsers;
    for (var i = 0; i < browserList.length; i++)
    {
        var browser = browserList[i];
        var contentWindow = browser.contentWindow.wrappedJSObject;
        tabWindowList.push(contentWindow);
    }
    return tabWindowList;
}

// ---------------------------------------- static Tools getPreference

Tools.getPreference = function (prefName)
{
    var prefsSubBranch = Tools.getPreferenceBranch();
if (prefsSubBranch == null) return null;
    if (!prefsSubBranch.prefHasUserValue(prefName)) return null;
    return prefsSubBranch.getCharPref(prefName);
}

// ---------------------------------------- static Tools getPreference

Tools.setPreference = function (prefName, value)
{
    var prefsSubBranch = Tools.getPreferenceBranch();
    prefsSubBranch.setCharPref(prefName, value);
}

// ---------------------------------------- static Tools getPreferenceBranch

Tools.getPreferenceBranch = function ()
{
    var prefsSubBranchName = "extensions.golimojo.";
    var prefsService = Components.classes["@mozilla.org/preferences-service;1"];
    var prefsBranch = prefsService.getService(Components.interfaces.nsIPrefBranch);
if (prefsBranch.getBranch == null) return null;
    var prefsSubBranch = prefsBranch.getBranch(prefsSubBranchName);
    return prefsSubBranch;
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
