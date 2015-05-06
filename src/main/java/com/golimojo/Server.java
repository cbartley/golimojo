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

package com.golimojo;

import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.NCSARequestLog;
import org.mortbay.http.handler.DumpHandler;
import org.mortbay.http.handler.NotFoundHandler;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.util.InetAddrPort;

// ------------------------------------------------------------
// ----------------------- class Server -----------------------
// ------------------------------------------------------------

public class Server extends ShellTools {

    // ---------------------------------------- Server main

    public static void main(String[] args) throws Exception {
        boolean doFastStartForTesting = true;
        System.out.printf("*** %s start.\n", doFastStartForTesting ? "Fast"
                : "Slow/Normal");

        // Run the unit tests.
        Tester tester = new Tester("L1TEST");
        tester.runTests();

        // Choose between fast and slow (normal) modes.
        String articleTitlePath = "resource-root/article-titles-small.txt";
        if (!doFastStartForTesting) {
            articleTitlePath = "resource-root/article-titles-medium.txt";
            Tester tester2 = new Tester("L1BTEST");
            tester2.runTests();
        }

        // Start the web server.
        start(8085, articleTitlePath);

        // Auto-install the extension, then auto-launch the browser. Also enable
        // the shutdown servlet so the server can be shutdown from the browser.
        ShutdownServlet.setAcceptShutdownRequest(true);
        String[] extraEnvVariables = new String[] { "MOZ_NO_REMOTE=1" };
        String pathToXpi = FirefoxExtensionConfigurationFactory.lazyCreateXpi(
                "localhost", 8085);

        // Stock command lines for quick and dirty testing. Pick whichever one
        // you want below.
//      String[] commands = new String[8];
//      commands[0] = "{firefoxCmd} -no-remote -Profile {profileDir} -install-global-extension {pathToXpi}";
//      commands[1] = "{firefoxCmd} -no-remote -Profile {profileDir} http://www.washingtonmonthly.com/ http://localhost:8085/javascript/tablog.html";
//      commands[2] = "{firefoxCmd} -no-remote -Profile {profileDir} http://localhost:8085/index.html http://localhost:8085/javascript/tablog.html";
//      commands[3] = "{firefoxCmd} -no-remote -Profile {profileDir} http://localhost:8085/test-files/test.html http://localhost:8085/javascript/tablog.html";
//      commands[4] = "{firefoxCmd} -no-remote -Profile {profileDir} http://localhost:8085/test-files/test-pattern.html http://localhost:8085/javascript/tablog.html";
//      commands[5] = "{firefoxCmd} -no-remote -Profile {profileDir} http://discovermagazine.com/2006/feb/megadeath-in-mexico http://localhost:8085/javascript/tablog.html";
//      commands[6] = "{firefoxCmd} -no-remote -Profile {profileDir} http://localhost:8085/index.html http://localhost:8085/javascript/tablog.html";
//      commands[7] = "{firefoxCmd} -no-remote -Profile {profileDir} http://www.astronautix.com http://localhost:8085/javascript/tablog.html";

        // Firefox on OSX requires an absolute path for the "-Profile" option.
        // Woe to those who do not provide it.
//      String currDir = System.getProperty("user.dir");
//      String profileDir = currDir + "/testing/firefox-profile";
//      system(commands[6], extraEnvVariables, sub("profileDir", profileDir),
//              sub("firefoxCmd", firefoxCmd()), sub("pathToXpi", pathToXpi));
        system("/Applications/Firefox.app/Contents/MacOS/firefox -CreateProfile Golimojo");
        system("/Applications/Firefox.app/Contents/MacOS/firefox --jsconsole -P Golimojo {pathToXpi} http://www.jsc.nasa.gov/Bios/htmlbios/aldrin-b.html", sub("pathToXpi", pathToXpi));
    }

    private static String firefoxCmd() {
        if (onWindows())
            return "cmd /c start /wait Firefox";
        if (onOSX())
            return "open -a Firefox --new --args";
        reportErrorAndQuit("I don't know how to launch Firefox on this platform!");
        return null;
    }

    // ---------------------------------------- Server start

    protected static void start(int port, String articleTitlePath)
            throws Exception {
        // Create the log.
        QuickLog log = new QuickLog();
        AddLinksProxyServlet.setSharedLog(log);
        TemplateServlet.setSharedLog(log);

        // Create the word ranker.
        String pathToWordFrequencyFile = "resource-root/word-frequency.txt";
        Ranker ranker = new Ranker(pathToWordFrequencyFile);

        // Create the page data store.
        PageDataStore pageDataStore = new PageDataStore(ranker,
                articleTitlePath);

        // Create the page linker.
        Linker linker = new Linker(pageDataStore);
        AddLinksProxyServlet.setSharedLinker(linker);
        GetPageDataAjaxServlet.setSharedLinker(linker);
        JsInclusionLinkerServlet.setSharedLinker(linker);

        // Set up the example HTML.
        TemplateServlet.setSharedExampleHtmlDescription(Linker.getExampleHtmlDescription());
        String exampleBeforeHtml = Linker.getExampleHtml();
        TemplateServlet.setSharedExampleBeforeHtml(exampleBeforeHtml);
        String exampleAfterHtml = linker
                .addLinksToHtmlFragmentText(exampleBeforeHtml);
        TemplateServlet.setSharedExampleAfterHtml(exampleAfterHtml);

        // Run level 2 tests.
        Tester tester = new Tester("L2TEST");
        tester.runTests(pageDataStore);

        // Create the server.
        HttpServer server = new HttpServer();

        // Default is no virtual host.
        String host = null;
        HttpContext context = server.getContext(host, "/");
        context.setRequestLog(new NCSARequestLog("jetty.log"));

        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServlet("Add Links", "/servlet/add-links",
                AddLinksProxyServlet.class.getName());
        servletHandler.addServlet("Get Page Data", "/servlet/get-page-data",
                GetPageDataAjaxServlet.class.getName());
        servletHandler.addServlet("Get Page Data JSON",
                "/servlet/get-page-data-json", JsInclusionLinkerServlet.class
                        .getName());
        servletHandler.addServlet("Template", "*.html", TemplateServlet.class
                .getName());
        servletHandler.addServlet("XPI", "/extension/golimojo.xpi",
                GolimojoXpiServlet.class.getName());
        servletHandler.addServlet("Shutdown", "/servlet/shutdown",
                ShutdownServlet.class.getName());
        context.addHandler(servletHandler);

        context.setResourceBase("web-root");
        context.addHandler(new ResourceHandler());
        context.addHandler(new DumpHandler());
        context.addHandler(new NotFoundHandler());

        InetAddrPort address = new InetAddrPort(port);
        server.addListener(address);

        server.start();
    }

}
