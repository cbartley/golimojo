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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.golimojo.QuickLog.LogEntry;

// ------------------------------------------------------------
// -------------------- class StatsServlet --------------------
// ------------------------------------------------------------

@SuppressWarnings("serial")
public class StatsServlet extends HtmlGenServlet
{
    
    // ---------------------------------------- StatsServlet customDoGet
    
    public void customDoGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        PrintWriter writer = response.getWriter();
        try
        {   
            String queryString = request.getQueryString();
            if (queryString != null)
            {
            }

            HtmlElementNode htmlTree = HTML
            (
                HEAD
                (
                    TITLE("Stats"),
                    STYLE
                    (
                        CSSCLASS
                        (
                            ".stats-table",
                            CSSATTRIBUTE("width", "100%"),
                            CSSATTRIBUTE("table-layout", "fixed")
                        )
                    )
                ),
                BODY
                (
                    TABLE
                    (
                        ATTRIBUTE("class", "stats-table"),
                        TBODY
                        (
                            createStatusRows()
                        )
                    )
                )
            );

            htmlTree.generate(writer);
        }
        finally
        {
            writer.close();
        }
    }
    
    private static Object[] createStatusRows()
    {
        List<LogEntry> logEntryList = getSharedLog().copyToList();
        
        Object[] rows = new Object[logEntryList.size()];
        for (int i = 0; i < logEntryList.size(); i++)
        {
            LogEntry logEntry = logEntryList.get(i);
            rows[i] = TR(TD(logEntry.getClientAddress()), TD(logEntry.getUrl()));
        }
        return rows;
    }

}

// ------------------------------------------------------------
// ------------------- class HtmlGenServlet -------------------
// ------------------------------------------------------------

@SuppressWarnings("serial")
class HtmlGenServlet extends GolimojoServlet
{
    public static HtmlElementNode HTML(Object... childNodes)
    {
        return new HtmlElementNode("html", childNodes);
    }
    
    public static HtmlElementNode HEAD(Object... childNodes)
    {
        return new HtmlElementNode("head", childNodes);
    }
    
    public static HtmlElementNode TITLE(Object... childNodes)
    {
        return new HtmlElementNode("title", childNodes);
    }
    
    public static HtmlStyleElementNode STYLE(CssClass... classes)
    {
        return new HtmlStyleElementNode(classes);
    }

    public static HtmlElementNode BODY(Object... childNodes)
    {
        return new HtmlElementNode("body", childNodes);
    }

    public static HtmlElementNode TABLE(Object... childNodes)
    {
        return new HtmlElementNode("table", childNodes);
    }

    public static HtmlElementNode TBODY(Object... childNodes)
    {
        return new HtmlElementNode("tbody", childNodes);
    }

    public static HtmlElementNode TR(Object... childNodes)
    {
        return new HtmlElementNode("tr", childNodes);
    }

    public static HtmlElementNode TD(Object... childNodes)
    {
        return new HtmlElementNode("td", childNodes);
    }

    public static HtmlElementNode DIV(Object... childNodes)
    {
        return new HtmlElementNode("div", childNodes);
    }

    public static HtmlElementAttribute ATTRIBUTE(String attributeName, String attributeValue)
    {
        return new HtmlElementAttribute(attributeName, attributeValue);
    }
    
    public static CssClass CSSCLASS(String className, CssAttribute... attributes)
    {
        return new CssClass(className, attributes);
    }
    
    public static CssAttribute CSSATTRIBUTE(String attributeName, String attributeValue)
    {
        return new CssAttribute(attributeName, attributeValue);
    }
    
    
    
}

// ------------------------------------------------------------
// ------------------- class HtmlElementNode ------------------
// ------------------------------------------------------------

class HtmlElementNode
{
    private final String _elementName;
    private final Object[] _childNodes;
    
    public HtmlElementNode(String elementName, Object... childNodes)
    {
        _elementName = elementName;
        _childNodes = childNodes;
    }

    public Object[] getChildNodes()
    {
        return _childNodes;
    }

    public String getElementName()
    {
        return _elementName;
    }

    public void generate(PrintWriter writer)
    {
        generate(writer, 0);
    }

    public void generate(PrintWriter writer, int depth)
    {
        // Generate the start tag
        generateIndentation(writer, depth);
        writer.print("<");
        writer.print(_elementName);
        for (Object childNode : _childNodes)
        {
            if (childNode instanceof HtmlElementAttribute)
            {
                writer.print(" ");
                ((HtmlElementAttribute)childNode).generate(writer);
            }
        }
        writer.print(">\n");

        // Generate the child nodes.
        generateChildNodes(writer, depth);
        
        // Generate the end tag.
        generateIndentation(writer, depth);
        writer.printf("</%s>\n", _elementName);
    }

    protected void generateChildNodes(PrintWriter writer, int depth)
    {
        for (Object childNode : _childNodes)
        {
            if (childNode instanceof HtmlElementNode)
            {
                ((HtmlElementNode)childNode).generate(writer, depth + 1);
            }
            else if (childNode instanceof HtmlElementAttribute)
            {
                // do nothing
            }
            else
            {
                generateIndentation(writer, depth + 1);
                writer.printf("%s\n", childNode.toString());
            }
        }
    }

    private static void generateIndentation(PrintWriter writer, int depth)
    {
        for (int i = 0; i < depth; i++)
        {
            writer.print("\t");
        }
    }

}

// ------------------------------------------------------------
// ---------------- class HtmlStyleElementNode ----------------
// ------------------------------------------------------------

class HtmlStyleElementNode extends HtmlElementNode
{
    private final CssClass[] _classes;
    
    public HtmlStyleElementNode(CssClass... classes)
    {
        super("style");
        _classes = classes;
    }

    
    protected void generateChildNodes(PrintWriter writer, int depth)
    {
        for (CssClass cssClass : _classes)
        {
            cssClass.generate(writer, depth + 1);
        }
    }
    
}

// ------------------------------------------------------------
// ---------------- class HtmlElementAttribute ----------------
// ------------------------------------------------------------

class HtmlElementAttribute
{
    private final String _attributeName;
    private final String _attributeValue;
    
    public HtmlElementAttribute(String attributeName, String attributeValue)
    {
        assert attributeName != null;
        if (attributeValue != null) assert attributeValue.indexOf('"') == -1;
        _attributeName = attributeName;
        _attributeValue = attributeValue;
    }
    
    public void generate(PrintWriter writer)
    {
        writer.print(_attributeName);
        if (_attributeValue != null)
        {
            writer.print("=");
            writer.print("\"");
            writer.print(_attributeValue);
            writer.print("\"");
        }
    }
}

// ------------------------------------------------------------
// ---------------------- class CssClass ----------------------
// ------------------------------------------------------------

class CssClass
{
    private final String _className;
    private final CssAttribute[] _attributes;

    public CssClass(String className, CssAttribute... attributes)
    {
        _className = className;
        _attributes = attributes;
    }
    
    public void generate(PrintWriter writer, int depth)
    {
        generateIndentation(writer, depth);
        writer.print(_className);
        writer.print("\n");
        
        generateIndentation(writer, depth);
        writer.print("{\n");
        
        for (CssAttribute attribute : _attributes)
        {
            attribute.generate(writer, depth + 1);
        }
        
        generateIndentation(writer, depth);
        writer.write("}\n");
        writer.write("\n");
    }
    
    private static void generateIndentation(PrintWriter writer, int depth)
    {
        for (int i = 0; i < depth; i++)
        {
            writer.print("\t");
        }
    }
    
}

// ------------------------------------------------------------
// -------------------- class CssAttribute --------------------
// ------------------------------------------------------------

class CssAttribute
{
    private final String _attributeName;
    private final String _attributeValue;
    
    public CssAttribute(String attributeName, String attributeValue)
    {
        assert attributeName != null;
        assert attributeValue != null;
        _attributeName = attributeName;
        _attributeValue = attributeValue;
    }
    
    public void generate(PrintWriter writer, int depth)
    {
        generateIndentation(writer, depth);
        writer.print(_attributeName);
        writer.print(": ");
        writer.print(_attributeValue);
        writer.print(";\n");
    }
    
    private static void generateIndentation(PrintWriter writer, int depth)
    {
        for (int i = 0; i < depth; i++)
        {
            writer.print("\t");
        }
    }

}

