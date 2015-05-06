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
// ----------------------- class Sidebar ----------------------
// ------------------------------------------------------------

// ---------------------------------------- Sidebar constructor

function Sidebar(sidebarWindow)
{
    this.sidebarWindow = sidebarWindow;
    this.sidebarWindow.sidebar = this;
    this.tools = null;

    this.linkStyleListBox = null;
    this.linkColorListBox = null;

    this.linkStyle = null;
    this.linkColor = null;

    this.sidebarWindow.onload = Tools.createCallback(this.onload, this);
    this.sidebarWindow.onunload = Tools.createCallback(this.onunload, this);
}

// ---------------------------------------- Sidebar onload

Sidebar.prototype.onload = function ()
{
    this.linkStyleListBox = document.getElementById("linkStyleListBox");
    this.linkColorListBox = document.getElementById("linkColorListBox");
    this.linkStyleListBox.onselect = Tools.createCallback(this.onselect, this);
    this.linkColorListBox.onselect = Tools.createCallback(this.onselect, this);
    Tools.notifyObservers(this.sidebarWindow, "golimojo-sidebar-opened");
    this.onselect();
}

// ---------------------------------------- Sidebar initialize

Sidebar.prototype.initialize = function (tools)
{
    this.tools = tools;
    this.linkStyleListBox.value = tools.getPreference("linkStyle") || this.linkStyleListBox.value;
    this.linkColorListBox.value = tools.getPreference("linkColor") || this.linkColorListBox.value;
}

// ---------------------------------------- Sidebar onunload

Sidebar.prototype.onunload = function ()
{
    Tools.notifyObservers(this.sidebarWindow, "golimojo-sidebar-closed");
}

// ---------------------------------------- Sidebar onselect

Sidebar.prototype.onselect = function ()
{
    this.linkStyle = this.linkStyleListBox.value;
    this.linkColor = this.linkColorListBox.value;
    Tools.notifyObservers(this.sidebarWindow, "golimojo-sidebar-select");
    this.tools.setPreference("linkStyle", this.linkStyle);
    this.tools.setPreference("linkColor", this.linkColor);
}

// ------------------------------------------------------------
// ---------------------- initialization ----------------------
// ------------------------------------------------------------

try
{
    var gSidebar = new Sidebar(window);
}
catch (e)
{
    alert(Log.inspectObject(e));
    Log.print(e);
}
