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
import java.io.StringWriter;

import javax.servlet.*;
import javax.servlet.http.*;

// ------------------------------------------------------------
// ------------------- class GolimojoServlet ------------------
// ------------------------------------------------------------

@SuppressWarnings("serial")
public class GolimojoServlet extends HttpServlet
{
    // ---------------------------------------- GolimojoServlet class variables
    
    private static QuickLog our_log = null;

    // ---------------------------------------- GolimojoServlet getSharedLog
    
    public static QuickLog getSharedLog()
    {
        return our_log;
    }

    // ---------------------------------------- GolimojoServlet setSharedLog
    
    public static void setSharedLog(QuickLog log)
    {
        our_log = log;
    }

    // ---------------------------------------- GolimojoServlet doGet
    
    public final void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        // Log the request.
        String queryString = request.getQueryString();
        String requestUrl = request.getRequestURL().toString();
        if (queryString != null)
        {
            requestUrl = requestUrl + "?" + queryString;
        }
        our_log.logRequest(request.getRemoteHost(), requestUrl);
        
        // Set the standard content type and character encoding.
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        
        // Turn of !@#$ caching.
        response.setDateHeader("Expires", 0);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");

        // Chain to the subclass to let it do its thing.
        try
        {
            customDoGet(request, response);
        }
        catch (Throwable t)
        {
            // Jetty just prints the exception message, which in many
            // cases is so short as to be practically meaningless.
            // Throw a new exception with more information.
            throw new ServletException(getDetailedDescription(t), t);
        }
    }

    // ---------------------------------------- GolimojoServlet customDoGet
    
    protected void customDoGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {

    }

    // ---------------------------------------- GolimojoServlet getDetailedDescription
    
    private static String getDetailedDescription(Throwable t)
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        t.printStackTrace(printWriter);
        return stringWriter.toString();
    }
    
}
