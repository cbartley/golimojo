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
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.*;
import javax.servlet.http.*;

@SuppressWarnings("serial")
public class TemplateServlet extends HttpServlet 
{
    
    // ---------------------------------------- MissingLinkProxyServlet doGet
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        try
        {   
            String pathToRequestedTemplateFile = getServletContext().getRealPath(request.getServletPath());
            String templateText = readFile(pathToRequestedTemplateFile);
            Dictionary<String, String> subDict = new Hashtable<String, String>();
            String queryString = request.getQueryString();
            subDict.put("queryString", queryString != null ? queryString : "null");
            subDict.put("localName", request.getLocalName());
            subDict.put("localPort", Integer.toString(request.getLocalPort()));
            String finalText = applySubstitutions(templateText, subDict);
            out.print(finalText);
        }
        finally
        {
            out.close();
        }
    }
    
    // ---------------------------------------- MissingLinkProxyServlet applySubstitutions
    
    private static String readFile(String pathToFile) throws IOException
    {
        Reader reader = new BufferedReader(new FileReader(pathToFile));
        
        StringBuffer sbText = new StringBuffer();
        while (true)
        {
            int chInt = reader.read();
            if (chInt == -1) break;
            char ch = (char)chInt;
            sbText.append(ch);
        }
        
        return sbText.toString();
    }
    
    // ---------------------------------------- MissingLinkProxyServlet applySubstitutions

    private static String applySubstitutions(String templateText, Dictionary<String, String> subDict)
    {
        // Substitutions are of the form "{{variable}}".  We split at "{{" or "}}"
        // which gives us an array where odd locations (1, 3, 5, ...) are variable
        // names to be substituted, and even locations are literal text runs which
        // may be empty strings but usually aren't.
        String[] fragments = templateText.split("(\\{\\{)|(\\}\\})");

        // Replace each odd location in the array with its substitution.
        for (int i = 1; i < fragments.length; i += 2)
        {
            String key = fragments[i];
            String value = subDict.get(key);
            if (value == null)
            {
                value = "{{" + key + "}}";
                System.err.printf("Error -- attempt to substitute unknown variable: '%s'\n", key);
            }
            fragments[i] = value;
        }

        // Re-assemble the fragments into the final output text.
        StringBuffer sbFinalText = new StringBuffer();
        for (String fragment : fragments)
        {
            sbFinalText.append(fragment);
        }
        
        // Return the result.
        return sbFinalText.toString();
    }
    
    
        public static void L1TEST_split()
        {
            // --- First, let's verify split works as expected. ---

            assert "[foo]".split("[\\[\\]]")[1].equals("foo");
            assert "alpha[foo]".split("[\\[\\]]")[1].equals("foo");
            assert "alpha[foo]bravo".split("[\\[\\]]")[1].equals("foo");
            assert "[foo]bravo".split("[\\[\\]]")[1].equals("foo");

            assert "[foo][bar]".split("[\\[\\]]")[1].equals("foo");
            assert "alpha[foo][bar]".split("[\\[\\]]")[1].equals("foo");
            assert "alpha[foo]bravo[bar]".split("[\\[\\]]")[1].equals("foo");
            assert "alpha[foo]bravo[bar]charlie".split("[\\[\\]]")[1].equals("foo");
            assert "alpha[foo][bar]charlie".split("[\\[\\]]")[1].equals("foo");

            assert "[foo][bar]".split("[\\[\\]]")[3].equals("bar");
            assert "alpha[foo][bar]".split("[\\[\\]]")[3].equals("bar");
            assert "alpha[foo]bravo[bar]".split("[\\[\\]]")[3].equals("bar");
            assert "alpha[foo]bravo[bar]charlie".split("[\\[\\]]")[3].equals("bar");
            assert "alpha[foo][bar]charlie".split("[\\[\\]]")[3].equals("bar");
            
            // --- Now, verify our function ---
            
            Dictionary<String, String> subDict = new Hashtable<String, String>();
            subDict.put("foo", "FOO");
            subDict.put("bar", "BAR");
            assert applySubstitutions("{{foo}}{{bar}}", subDict).equals("FOOBAR");
            assert applySubstitutions("alpha{{foo}}{{bar}}", subDict).equals("alphaFOOBAR");
            assert applySubstitutions("alpha{{foo}}bravo{{bar}}", subDict).equals("alphaFOObravoBAR");
            assert applySubstitutions("alpha{{foo}}bravo{{bar}}charlie", subDict).equals("alphaFOObravoBARcharlie");
            assert applySubstitutions("alpha{{foo}}{{bar}}charlie", subDict).equals("alphaFOOBARcharlie");
            
        }
    
    

}
