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

import java.io.*;
import java.net.URLDecoder;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.*;
import javax.servlet.http.*;

@SuppressWarnings("serial")
public class TemplateServlet extends GolimojoServlet 
{
    
    // ---------------------------------------- TemplateServlet instance variables
    
    private static String our_exampleBeforeHtml = "";
    private static String our_exampleAfterHtml = "";
    
    // ---------------------------------------- TemplateServlet customDoGet
    
    public void customDoGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        // Some test URLs have leaked into the web at large.  If we get called 
        // through one and it doesn't look like a test, redirect to the main site.
        String testUrlRedirectUrl = checkForTestUrlRedirect(request);
        if (testUrlRedirectUrl != null)
        {
            response.sendRedirect(testUrlRedirectUrl);
            return;
        }

        PrintWriter out = response.getWriter();
        try
        {
            String pathToRequestedTemplateFile = getServletContext().getRealPath(request.getServletPath());
            Dictionary<String, String> subDict = createSubstitutionDictionary(request);
            String finalText = Template.applySubstitutionsToFile(pathToRequestedTemplateFile, subDict);
            
            // If there's an embedded redirect command, then do the redirect now.
            String url = checkForRedirectUrl(finalText);
            if (url != null)
            {
                response.sendRedirect(url);
                return;
            }
            
            out.print(finalText);
        }
        finally
        {
            out.close();
        }
    }
    
    // ---------------------------------------- TemplateServlet checkForTestUrlRedirect
    
    private String checkForTestUrlRedirect(HttpServletRequest request)
    {
        String referer = request.getHeader("referer");
        if (referer == null) return null;
        String requestUrl = request.getRequestURL().toString().toLowerCase();
        String requestToRedirectPrefix = "http://golimojo.dnsalias.net:8080/";
        if (!requestUrl.startsWith(requestToRedirectPrefix)) return null;
        referer = referer.toLowerCase();
        if (!referer.startsWith("http")) return null;
        if (referer.startsWith(requestToRedirectPrefix)) return null;
        String redirectUrl = requestUrl.replace(requestToRedirectPrefix, "http://www.golimojo.com/");
        return redirectUrl;
    }   

    // ---------------------------------------- TemplateServlet setSharedExampleBeforeHtml
    
    public static void setSharedExampleBeforeHtml(String exampleBeforeHtml)
    {
        our_exampleBeforeHtml = exampleBeforeHtml;
    }

    // ---------------------------------------- TemplateServlet setSharedExampleAfterHtml
    
    public static void setSharedExampleAfterHtml(String exampleAfterHtml)
    {
        our_exampleAfterHtml = exampleAfterHtml;        
    }

    // ---------------------------------------- TemplateServlet createSubstitutionDictionary

    private Dictionary<String, String> createSubstitutionDictionary(HttpServletRequest request) throws UnsupportedEncodingException
    {
        Dictionary<String, String> subDict = new Hashtable<String, String>();

        String requestPath = request.getServletPath();
        String requestPathPlusQueryString = requestPath;
        String queryString = request.getQueryString();
        if (queryString != null)
        {
            requestPathPlusQueryString += "?" + queryString;
        }
        
        if (queryString == null)
        {
            queryString = "null";
        }
        
        subDict.put("requestPath", requestPath);
        subDict.put("requestPathPlusQueryString", requestPathPlusQueryString);
        subDict.put("queryString", queryString);
        subDict.put("queryStringUnescaped", URLDecoder.decode(queryString, "UTF-8"));
        subDict.put("serverName", request.getServerName());
        subDict.put("serverPort", Integer.toString(request.getServerPort()));
        subDict.put("exampleBeforeHtml", our_exampleBeforeHtml);
        subDict.put("exampleAfterHtml", our_exampleAfterHtml);
        return subDict;
    }
    
    // ---------------------------------------- TemplateServlet checkForRedirectUrl
    
    private static String checkForRedirectUrl(String pageText)
    {
        Pattern p = Pattern.compile("###[ \t]*redirect[ \t]*(.*?)###");
        Matcher m = p.matcher(pageText);
        if (m.find() && m.groupCount() == 1) 
        {
            String url = m.group(1).trim();
            return url;
        }
        return null;
    }

}
