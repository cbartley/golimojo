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
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

@SuppressWarnings("serial")
public class MissingLinkProxyServlet extends HttpServlet 
{
    // ---------------------------------------- MissingLinkProxyServlet class variables
    
    private static Linker ourLinker = null;
    
    public static void setSharedLinker(Linker linker)
    {
        ourLinker = linker;
    }
    
    // ---------------------------------------- MissingLinkProxyServlet doGet
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {       
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        

        String pathInfo = request.getPathInfo();
        String urlArg = "http:/"+ pathInfo;
        
        try
        {
                String htmlText = Parser.readHttpFile(urlArg);
                List<HtmlFragment> fragmentList = Parser.parse(htmlText);
                if (ourLinker != null)
                {
                    fragmentList = ourLinker.findLinks(fragmentList, urlArg);                   
                }
                String htmlTextOut = Parser.join(fragmentList);
                out.print(htmlTextOut);
        }
        catch (Exception e)
        {
                System.out.println(e);
                throw new ServletException(e);
        }

        out.close();

        
        
        
        
        
        
        
        
        out.println(
            "<html>\n" +
            "   <head>" +
            "       <title>Missing Link Proxy Servlet</title>\n" +
            "   </head>\n" +
            "   <body>\n" +
            "       <h1>Missing Link Proxy Servlet</h1>\n" +
            "   </body>\n" +
            "</html>\n" +
            "\n"
        );
    }
    
    
}
