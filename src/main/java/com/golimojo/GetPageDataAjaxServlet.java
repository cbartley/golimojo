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
import java.net.URLDecoder;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.golimojo.QdmlParser.QdmlFragment;

@SuppressWarnings("serial")
public class GetPageDataAjaxServlet extends GolimojoServlet 
{
    // ---------------------------------------- GetPageDataAjaxServlet class variables
    
    private static Linker our_linker = null;

    // ---------------------------------------- GetPageDataAjaxServlet setSharedLinker
    
    public static void setSharedLinker(Linker linker)
    {
        our_linker = linker;
    }
    
    // ---------------------------------------- GetPageDataAjaxServlet customDoGet
    
    public void customDoGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        long servletStartTimeMs = System.currentTimeMillis();

        // Get the target URL.
        String pathInfo = URLDecoder.decode(request.getQueryString(), "UTF-8");
        String urlArg = "http://"+ pathInfo;
        
        // Read the target page. 
        List<QdmlFragment> fragmentList = HttpReader.readAndParseHttpFile(urlArg);

        // Reset the content type to XML.
        response.setContentType("text/xml");

        // Find the links and output the XML representation for them.
        PrintWriter out = response.getWriter();
        try
        {
            if (our_linker != null)
            {
                long startTimeMs = System.currentTimeMillis();
                List<String> pageTitleList = our_linker.findLinks(fragmentList);
                generatePageTitleXml(out, pageTitleList);
                long elapsedTimeMs = System.currentTimeMillis() - startTimeMs;
                System.out.printf("### %s: elapsed time: %1.2f\n", this.getClass().getName(), (0.001 * elapsedTimeMs));
                System.out.printf("... %d\n", pageTitleList.size());
            }
        }
        finally
        {
            out.close();
        }

        long servletElapsedTimeMs = System.currentTimeMillis() - servletStartTimeMs;
        System.out.printf("--- %s: elapsed time: %1.2f\n", this.getClass().getName(), (0.001 * servletElapsedTimeMs));
    }
    
    // ---------------------------------------- GetPageDataAjaxServlet generatePageTitleXml
    
    private static void generatePageTitleXml(PrintWriter writer, List<String> pageTitleList)
    {
        writer.println("<page-titles>");
        for (String pageTitle : pageTitleList)
        {
            if (pageTitle.indexOf('[') == -1 && pageTitle.indexOf(']') == -1)
            {
                writer.print("\t");
                writer.print("<page-title>");
                writer.print("<![CDATA[");
                writer.print(pageTitle);
                writer.print("]]>");
                writer.println("</page-title>");
            }
        }
        writer.println("</page-titles>");
    }

}
