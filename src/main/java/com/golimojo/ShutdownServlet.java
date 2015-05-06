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

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.*;
import javax.servlet.http.*;

// ------------------------------------------------------------
// ------------------- class ShutdownServlet ------------------
// ------------------------------------------------------------
/**
 * The ShutdownServlet provides a means of shutting the server down
 * from the browser.  This is strictly a development aid for quick-
 * turnaround browser-based testing.  It's not intended for general
 * use.  This feature is disabled by default.  It should only ever
 * be enabled for special testing-specific launch configurations.
 * It should never, ever be enabled for a release build.
 */

@SuppressWarnings("serial")
public class ShutdownServlet extends HttpServlet
{

    // ---------------------------------------- XpiServlet class variables
    
    private static boolean our_acceptShutdownRequestFlag = false;

    // ---------------------------------------- XpiServlet setAcceptShutdownRequest
    
    public static void setAcceptShutdownRequest(boolean acceptFlag)
    {
        our_acceptShutdownRequestFlag = acceptFlag;
    }

    // ---------------------------------------- XpiServlet doGet
    
    public final void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        // Set the standard content type and character encoding.
        response.setContentType("application/octet-stream");
        
        // Turn of caching.
        response.setDateHeader("Expires", 0);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
        
        PrintWriter out = response.getWriter();
        try
        {
            out.println("<message>shutdown request received</message>");
        }
        finally
        {
            out.close();
        }
        
        // If we're accepting shutdown requests, then kill the server now.
        if (our_acceptShutdownRequestFlag)
        {
            System.out.printf("### %s: Shutting down on request!\n", this.getClass());
            System.exit(0);
        }   
    }
}
