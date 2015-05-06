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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.*;
import javax.servlet.http.*;

import com.golimojo.QdmlParser.QdmlFragment;
import com.golimojo.QdmlParser.QdmlStartTagFragment;

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
        String localName = request.getLocalName();
        int localPort = request.getLocalPort();
        String proxyPageUrl = "http://" + localName + ":" + localPort + "/proxy.html";

        String pathInfo = URLDecoder.decode(request.getQueryString(), "UTF-8");
        String urlArg = "http://"+ pathInfo;
        
        List<QdmlFragment> fragmentList = HttpReader.readAndParseHttpFile(urlArg);
        
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        try
        {
            if (ourLinker != null)
            {
                long startTimeMs = System.currentTimeMillis();
                fragmentList = ourLinker.addLinksToHtmlDocument(fragmentList, urlArg);
                long elapsedTimeMs = System.currentTimeMillis() - startTimeMs;
                System.out.printf("### elapsed time: %1.2f\n", (0.001 * elapsedTimeMs));
                fragmentList = stripProblemTags(fragmentList);
                rewriteAnchors(fragmentList, urlArg, proxyPageUrl);
            }
            String htmlTextOut = QdmlParser.join(fragmentList);
            out.print(htmlTextOut);
        }
        finally
        {
            out.close();
        }
    }
    
    // ---------------------------------------- MissingLinkProxyServlet rewriteAnchors
    
    private static void rewriteAnchors(List<QdmlFragment> fragmentList, String baseUrl, String proxyPageUrl)
    {
        for (QdmlFragment fragment : fragmentList)
        {
            if (fragment.isStartTagCi("A"))
            {
                remapAnchorStartTag((QdmlStartTagFragment)fragment, baseUrl, proxyPageUrl);
            }
        }
    }
    
    // ---------------------------------------- MissingLinkProxyServlet stripProblemTags
    
    private static List<QdmlFragment> stripProblemTags(List<QdmlFragment> fragmentList)
    {
        List<QdmlFragment> fragmentListOut = new ArrayList<QdmlFragment>();
        for (QdmlFragment fragment : fragmentList)
        {
            if (isMetaExpiresTag(fragment)) continue;
            if (fragment.isStartTagCi("SCRIPT")) continue;
            if (fragment.isEndTagCi("SCRIPT")) continue;
            fragmentListOut.add(fragment);
        }
        return fragmentListOut;
    }
    
    // ---------------------------------------- MissingLinkProxyServlet isMetaExpiresTag

    private static boolean isMetaExpiresTag(QdmlFragment fragment)
    {
        if (fragment.isStartTagCi("META"))
        {
            String tagText = fragment.getText().toLowerCase();
            if (tagText.indexOf("http-equiv") == -1) return false;
            if (tagText.indexOf("expires") == -1) return false;
            return true;
        }
        return false;
    }
    
        public static void L1TEST_isMetaExpiresTag()
        {
            assert isMetaExpiresTag(new QdmlStartTagFragment("META", "<META HTTP-EQUIV='Expires' CONTENT='Thu, 30 Dec 2049 16:00:00 GMT'>"));
            assert !isMetaExpiresTag(new QdmlStartTagFragment("META", "<META HTTP-EQUIV='Last-Modified' CONTENT='Thu, 23 Feb 2006 15:45:00 GMT'>"));
            
        }
    
    // ---------------------------------------- MissingLinkProxyServlet remapAnchorStartTag
    
    private static void remapAnchorStartTag(QdmlStartTagFragment anchorStartTagFragment, String baseUrl, String proxyPageUrl)
    {
        String anchorText = anchorStartTagFragment.getText();
        String remappedAnchorText = remapAnchorText(anchorText, baseUrl);
        remappedAnchorText = remappedAnchorText.replace("#proxy#", proxyPageUrl);
        anchorStartTagFragment.setText(remappedAnchorText);
    }
    
    // ---------------------------------------- MissingLinkProxyServlet remapAnchorText
        
    private static String remapAnchorText(String anchorText, String baseUrl)
    {
        String[] fragments = splitAnchorAtHrefUrl(anchorText);
        
        // Proxy the URL if we can.
        if (fragments != null)
        {
            String url = fragments[1];
            String proxyUrl = createProxyUrl(url, baseUrl);
            if (proxyUrl != null)
            {
                anchorText = fragments[0] + proxyUrl + fragments[2];
            }
        }
        
        // Proxied or not, we want to target it to the top window.
        String prefix = anchorText.substring(0, anchorText.length() - 1);
        String suffix = anchorText.substring(anchorText.length() - 1);
        assert suffix.equals(">");
        String targetedAnchorText = prefix + " target='_top'" + suffix;
        return targetedAnchorText;
    }
    
        public static void L1TEST_remapAnchorText()
        {
            assert remapAnchorText("<a href='https://www.example.com'>", "http://www.example.com/index.html")
                        .equals("<a href='https://www.example.com' target='_top'>");
            assert remapAnchorText("<a href='http://www.example.com'>", "http://www.example.com/index.html")
                        .equals("<a href='#proxy#?www.example.com'>");
            assert remapAnchorText("<a href='test.html'>", "http://www.example.com/index.html")
                        .equals("<a href='#proxy#?www.example.com%2Ftest.html'>");
            assert remapAnchorText("<a href='#fragment'>", "http://www.example.com/index.html")
                        .equals("<a href='#proxy#?www.example.com%2Findex.html%23fragment'>");
            
        }
    
    // ---------------------------------------- MissingLinkProxyServlet splitAnchorAtHrefUrl
        
    private static Pattern our_anchorHrefUrlPattern = 
        Pattern.compile("^(.*href[ \\t\\r\\n]*=[ \\t\\r\\n]*)((\\\".*?\\\")|('.*?'))(.*)$");

    private static String[] splitAnchorAtHrefUrl(String anchorText)
    {
        Matcher matcher = our_anchorHrefUrlPattern.matcher(anchorText);
        if (matcher.matches())
        {
            // Extract our primary groups.
            String prefix = matcher.group(1);
            String url = matcher.group(2);
            String suffix = matcher.group(5);       // groups 3 and 4 are inside group 2
            
            // Move the begin and end quotes from the url to the prefix and suffix, respectively.
            String prefix2 = prefix + url.charAt(0);
            String suffix2 = url.charAt(url.length() - 1) + suffix;
            String url2 = url.substring(1, url.length() - 1);
            
            return new String[] {prefix2, url2, suffix2};
        }
        return null;
    }
        
        public static void L1TEST_splitAnchorAtHrefUrl()
        {
            assert splitAnchorAtHrefUrl("href='...'") != null;
            assert splitAnchorAtHrefUrl("href=\"...\"") != null;
            assert splitAnchorAtHrefUrl("href=...") == null;

            assert splitAnchorAtHrefUrl("href ='...'") != null;
            assert splitAnchorAtHrefUrl("href= '...'") != null;
            assert splitAnchorAtHrefUrl("href\t\r\n=\n\r\t'...'") != null;
            
            assert splitAnchorAtHrefUrl("href='...' ...")[0].equals("href='");
            assert splitAnchorAtHrefUrl("href='...' ...")[1].equals("...");
            assert splitAnchorAtHrefUrl("href='...' ...")[2].equals("' ...");

            assert splitAnchorAtHrefUrl("<a href=\"/intl/en/ads/\">")[1].equals("/intl/en/ads/");
        }

    // ---------------------------------------- MissingLinkProxyServlet createProxyUrl

    private static String createProxyUrl(String url, String baseUrl)
    {
        // Case 1: Standard absolute URL
        if (url.startsWith("http://"))
        {
            return createProxyUrlForAbsoluteUrl(url);
        }
        
        // Case 2: Other protocol
        if (url.indexOf(':') >= 0)
        {
            return null;
        }

        // Case 3: Relative URL
        url = concatRelativeUrl(baseUrl, url);
        if (url == null) return null;
        return createProxyUrlForAbsoluteUrl(url);
    }

        public static void L1TEST_createProxyUrl()
        {
            final String baseUrl = "http://www.example.com/index.html";
            assert createProxyUrl("ftp://www.example.com/index.html", baseUrl) == null;
            assert createProxyUrl("https://www.example.com/index.html", baseUrl) == null;
            assert createProxyUrl("#fragment", baseUrl)
                        .equals("#proxy#?www.example.com%2Findex.html%23fragment");
            assert createProxyUrl("/test/test.html", baseUrl)
                        .equals("#proxy#?www.example.com%2Ftest%2Ftest.html");
            assert createProxyUrl("test/test.html", baseUrl)
                        .equals("#proxy#?www.example.com%2Ftest%2Ftest.html");
            assert createProxyUrl("test/test.html", "http://www.example.com/test/index.html")
                        .equals("#proxy#?www.example.com%2Ftest%2Ftest%2Ftest.html");
            assert createProxyUrl("http://www.example.com/index.html", baseUrl)
                        .equals("#proxy#?www.example.com%2Findex.html");
        }
    
    // ---------------------------------------- MissingLinkProxyServlet createProxyUrlForAbsoluteUrl
        
    private static String createProxyUrlForAbsoluteUrl(String url)
    {
        if (url.startsWith("http://"))
        {
            try
            {
                url = url.replaceFirst("http://", "");
                url = "#proxy#?" + URLEncoder.encode(url, "UTF-8");
                return url;
            }
            catch (UnsupportedEncodingException e)
            {
            }
        }
        return null;
    }
    
        public static void L1TEST_createProxyUrlForAbsoluteUrl()
        {
            assert createProxyUrlForAbsoluteUrl("https://www.example.com/test/test.html") == null;
            assert createProxyUrlForAbsoluteUrl("http://www.example.com/test/test.html")
                        .equals("#proxy#?www.example.com%2Ftest%2Ftest.html");
        }
    
    // ---------------------------------------- MissingLinkProxyServlet concatRelativeUrl

    private static String concatRelativeUrl(String url, String relUrl)
    {
        try
        {
            return new URL(new URL(url), relUrl).toString();
        } 
        catch (MalformedURLException e)
        {
            return null;
        }
    }
    
        public static void L1TEST_concatRelativeUrl()
        {
            assert concatRelativeUrl("http://www.example.com/index.html", "image.gif")
                    .equals("http://www.example.com/image.gif");
            assert concatRelativeUrl("http://www.example.com/index.html", "#fragment")
                    .equals("http://www.example.com/index.html#fragment");
        }
        
}
