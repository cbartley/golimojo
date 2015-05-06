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
import java.net.URLEncoder;
import java.util.*;

import com.golimojo.QdmlParser.QdmlEndTagFragment;
import com.golimojo.QdmlParser.QdmlFragment;
import com.golimojo.QdmlParser.QdmlStartTagFragment;
import com.golimojo.QdmlParser.QdmlElementFragment;
import com.golimojo.QdmlParser.QdmlTextNodeFragment;
import com.golimojo.TextFragment.FragmentType;

public class Linker 
{

    // ---------------------------------------- Linker instance variables
    
    private PageDataStore _pageDataStore;
    
    // ---------------------------------------- Linker constructor
    
    public Linker(PageDataStore pageDataStore) throws Exception
    {
        _pageDataStore = pageDataStore;
    }

    // ---------------------------------------- Linker getExampleHtmlDescription
    
    public static String getExampleHtmlDescription()
    {
        return  "Here's an excerpt from Buzz Aldrin's NASA " +
                "<a href='http://www.jsc.nasa.gov/Bios/htmlbios/aldrin-b.html'>biography</a>:";
    }

    // ---------------------------------------- Linker getExampleHtml

    public static String getExampleHtml()
    {
        return  "Prior to joining NASA, Aldrin flew 66 combat missions in F-86's while on " +
                "duty in Korea. At Nellis Air Force Base, Nevada, he served as an aerial " +
                "gunnery instructor. Following his assignment as aide to the dean of " +
                "faculty at the Air Force Academy, Aldrin flew F-100's as a flight " +
                "commander at Bitburg, Germany. He went on to receive a doctorate at MIT, " +
                "and was then assigned to the Gemini Target Office of the Air Force Space " +
                "Systems Division, Los Angeles. In March 1972, Aldrin retired from Air " +
                "Force active duty, after 21 years of service. As a USAF jet fighter pilot " +
                "during the Korean War, he shot down two MIG 15 aircraft.";
    }

    // ---------------------------------------- Linker addLinksToHtmlFragmentText

    public String addLinksToHtmlFragmentText(String htmlFragmentText)
    {
        try
        {
            Reader reader = new StringReader(htmlFragmentText);
            String[] encodingInOut = new String[] {"UTF-8"};
            List<QdmlFragment> fragmentList = HttpReader.readAndParseFromReader(reader, encodingInOut);
            fragmentList.add(0, new QdmlStartTagFragment("BODY", ""));  // rude hack
            fragmentList = addLinksToHtmlDocument(fragmentList);
            return QdmlParser.join(fragmentList);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return "";
        }
    }

    // ---------------------------------------- Linker addLinksToHtmlDocument

    public List<QdmlFragment> addLinksToHtmlDocument(List<QdmlFragment> fragmentList, String baseUrl)
    {
        // Assign the HTML tag an ID.  We can use the ID to specify higher priority rules in our CSS.
        int htmlStartTagIndex = findStartTag(fragmentList, "HTML");
        if (htmlStartTagIndex >= 0)
        {
            QdmlStartTagFragment htmlStartTagFragment =
                (QdmlStartTagFragment)fragmentList.get(htmlStartTagIndex);
            String tagText = htmlStartTagFragment.getText();
            String newTagText = tagText.substring(0, tagText.length() - 1) + " id='html'>";
            fragmentList.set(htmlStartTagIndex, new QdmlStartTagFragment("HTML", newTagText));
        }

        // Insert BASE and STYLE elements immediately after the HEAD.
        int headStartTagIndex = findStartTag(fragmentList, "HEAD");
        if (headStartTagIndex >= 0)
        {
            QdmlElementFragment elem = new QdmlElementFragment("<base href='" + baseUrl + "'>\n");
            fragmentList.add(headStartTagIndex + 1, elem);
            QdmlFragment styleElem = createGolimojoStyleElement();
            fragmentList.add(headStartTagIndex + 2, styleElem);         
        }

        // Now add the links.
        return addLinksToHtmlDocument(fragmentList);
    }

    // ---------------------------------------- Linker findStartTag
    
    private static int findStartTag(List<QdmlFragment> fragmentList, String tagName)
    {
        for (int i = 0; i < fragmentList.size(); i++)
        {
            QdmlFragment fragment = fragmentList.get(i);
            if (fragment instanceof QdmlStartTagFragment)
            {
                QdmlStartTagFragment startTag = (QdmlStartTagFragment)fragment;
                if (startTag.isStartTagCi(tagName)) return i;
            }
        }
        return -1;
    }

    // ---------------------------------------- Linker findLinks
    
    public List<String> findLinks(List<QdmlFragment> fragmentList)
    {
        // Find all the matching page titles in the HTML.
        List<String> pageTitleList = new ArrayList<String>();
        for (QdmlFragment fragment : fragmentList)
        {
            findLinksInFragment(fragment, pageTitleList);
        }
        
        // Remove duplicates.
        Set<String> pageTitleSet = new HashSet<String>(pageTitleList);
        pageTitleList = new ArrayList<String>(pageTitleSet);

        // Sort by longest string first.
        Comparator<String> longestStringFirstComparator = new Comparator<String>()
        {
            public int compare(String s1, String s2)
            {
                return s2.length() - s1.length();
            }
        };
        Collections.sort(pageTitleList, longestStringFirstComparator);
        
        return pageTitleList;
    }

    // ---------------------------------------- Linker findLinksInFragment
    
    public void findLinksInFragment(QdmlFragment fragment, List<String> pageTitleReceiverList)
    {
        if (fragment instanceof QdmlTextNodeFragment)
        {
            String text = ((QdmlTextNodeFragment)fragment).getText();
            findLinksInText(text, pageTitleReceiverList);
        }
    }

    // ---------------------------------------- Linker createGolimojoStyleElement

    private static QdmlFragment createGolimojoStyleElement()
    {
        String[] fragments = new String[]
        {
            "<style>",
            createStyles(),
            "</style>",
        };
        String styleText = join(fragments, "\n");
        return new QdmlElementFragment(styleText);
    }

    // ---------------------------------------- Linker createStyles

    private static String createStyles()
    {
        String[] fragments = new String[]
        {
            createStyle("#html", "red", "underline", "none"),
            createStyle("#html.black", "black", "underline", "none"),
            createStyle("#html.blue", "blue", "underline", "none"),
            createStyle("#html.red", "red", "underline", "none"),
            createStyle("#html.green", "green", "underline", "none"),
            createStyle("#html.yellow", "yellow", "underline", "none"),
            ""
        };

        return join(fragments, "\n");
    }

    // ---------------------------------------- Linker createStyle

    private static String createStyle(String keySelector, String color, String textDecoration, String borderStyle)
    {
        String[] lineList = new String[]
        {
            "   #key-selector# a.golimojo-wikipedia-link:link",
            "   , #key-selector# a.golimojo-wikipedia-link:visited",
            "   , #key-selector# a.golimojo-wikipedia-link:hover",
            "   , #key-selector# a.golimojo-wikipedia-link:active",
            "   {",
            "       color: #color# !important;",
            "       text-decoration: #text-decoration# !important;",
            "       border-bottom: #border-style# !important;",
            "   }",
            ""
        };

        String text = join(lineList, "\n");
        text = text.replace("#key-selector#", keySelector);
        text = text.replace("#color#", color);
        text = text.replace("#text-decoration#", textDecoration);
        text = text.replace("#border-style#", borderStyle);

        return text;
    }

    // ---------------------------------------- Linker join

    private static String join(String[] strings, String jointText)
    {
        StringBuffer sbJoin = new StringBuffer();
        for (String string : strings)
        {
            if (sbJoin.length() > 0)
            {
                sbJoin.append(jointText);
            }
            sbJoin.append(string);
        }
        return sbJoin.toString();
    }

    // ---------------------------------------- Linker findLinksInText
    
    private void findLinksInText(String text, List<String> pageTitleReceiverList)
    {
        List<TextFragment> fragmentList = TextFragment.splitTextIntoFragments(text);
        int index = 0;
        int endIndex = fragmentList.size();
        while (index < endIndex)
        {
            PageDataStore.PageTitleMatch pageTitleMatch = 
                _pageDataStore.findMatchingPageTitleAtPosition(fragmentList, index);
            if (pageTitleMatch != null)
            {
                pageTitleReceiverList.add(pageTitleMatch.getPageTitle());
                index += pageTitleMatch.getFragmentMatchCount();
            }
            else
            {
                index += 1;
            }
        }
    }

    // ---------------------------------------- Linker addLinksToHtmlDocument

    private List<QdmlFragment> addLinksToHtmlDocument(List<QdmlFragment> fragmentList)
    {   
        boolean inBody = false;
        boolean inStyle = false;
        boolean inScript = false;
        boolean inAnchor = false;
        
        List<QdmlFragment> fragmentOutList = new ArrayList<QdmlFragment>();
        for (int i = 0; i < fragmentList.size(); i++)
        {
            QdmlFragment fragment = fragmentList.get(i);
            
            boolean isTextNode = (fragment instanceof QdmlTextNodeFragment);
            
            inBody = isInsideElement(fragment, "BODY", inBody);
            inStyle = isInsideElement(fragment, "STYLE", inStyle);
            inScript = isInsideElement(fragment, "SCRIPT", inScript);
            inAnchor = isInsideElement(fragment, "A", inAnchor);
            
            if (!isTextNode || !inBody || inStyle || inScript || inAnchor)
            {
                fragmentOutList.add(fragment);
            }
            else
            {
                QdmlTextNodeFragment textNode = (QdmlTextNodeFragment)fragment;
                addLinksToTextNode(textNode, fragmentOutList);
            }
        }

        return fragmentOutList;
    }
    
    // ---------------------------------------- Linker isInsideElement

    private boolean isInsideElement(QdmlFragment fragment, String tagName, boolean insideElementNow)
    {
        if (fragment instanceof QdmlStartTagFragment)
        {
            QdmlStartTagFragment startTag = (QdmlStartTagFragment)fragment;
            if (startTag.isStartTagCi(tagName)) return true;
        }
        if (fragment instanceof QdmlEndTagFragment)
        {
            QdmlEndTagFragment endTag = (QdmlEndTagFragment)fragment;
            if (endTag.isEndTagCi(tagName)) return false;
        }
        return insideElementNow;
    }
    
    // ---------------------------------------- Linker addLinksToTextNode
    
    private void addLinksToTextNode(QdmlTextNodeFragment textNode, List<QdmlFragment> fragmentOutList)
    {
        String text = textNode.toString();
        String linkedText = addLinksToText(_pageDataStore, text);
        fragmentOutList.add(new QdmlTextNodeFragment(linkedText));
    }

    // ---------------------------------------- Linker addLinksToText
    
    private static String addLinksToText(PageDataStore pageDataStore, String text)
    {
        List<TextFragment> fragmentList = TextFragment.splitTextIntoFragments(text);
        List<TextFragment> receiverList = new ArrayList<TextFragment>();
        addLinksToFragmentList(pageDataStore, fragmentList, receiverList);
        return TextFragment.join(receiverList);
    }

    // ---------------------------------------- Linker addLinksToFragmentList
    
    private static void addLinksToFragmentList(PageDataStore pageDataStore, List<TextFragment> fragmentList, List<TextFragment> receiverList)
    {
        int index = 0;
        int endIndex = fragmentList.size();
        while (index < endIndex)
        {
            PageDataStore.PageTitleMatch pageTitleMatch = 
                pageDataStore.findMatchingPageTitleAtPosition(fragmentList, index);
            if (pageTitleMatch != null)
            {
                String pageTitle = pageTitleMatch.getPageTitle();
                int fragmentMatchCount = pageTitleMatch.getFragmentMatchCount();
                copyLinkedPhrase(fragmentList, index, fragmentMatchCount, pageTitle, receiverList);
                index += fragmentMatchCount;
            }
            else
            {
                receiverList.add(fragmentList.get(index));
                index += 1;
            }
        }
    }

    // ---------------------------------------- Linker copyLinkedPhrase

    private static void copyLinkedPhrase(List<TextFragment> fragmentList, int index, int fragmentCount, String pageTitle, List<TextFragment> receiverList)
    {
        String url = buildWikipediaUrl(pageTitle);
        String startTagText = "<a class='golimojo-wikipedia-link' href='" + url + "' target='_top'>";
        String endTagText = "</a>";
        TextFragment anchorStartTagFragment = new TextFragment(FragmentType.Other, startTagText);
        TextFragment anchorEndTagFragment = new TextFragment(FragmentType.Other, endTagText);
        
        receiverList.add(anchorStartTagFragment);
        receiverList.addAll(fragmentList.subList(index, index + fragmentCount));
        receiverList.add(anchorEndTagFragment);
    }

    // ---------------------------------------- Linker buildWikipediaUrl

    private static String buildWikipediaUrl(String pageTitle)
    {
        try
        {
            String urlPrefix = "http://en.wikipedia.org/wiki/";
            String encodedPageName = URLEncoder.encode(pageTitle.replace(" ", "_"), "UTF-8");
            return urlPrefix + encodedPageName;
        } 
        catch (UnsupportedEncodingException e)
        {
            System.err.println("Could not encode '" + pageTitle + "'!");
            return "about:blank";
        }
    }

}
