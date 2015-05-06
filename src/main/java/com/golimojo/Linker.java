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

    // ---------------------------------------- Linker getExampleHtml

    public static String getExampleHtml()
    {
        String[] exampleLines = new String[]
        {
            "... Rhodes spends a great ",
            "deal of time at the <a href='#'>Cavendish Laboratory</a> at Cambridge, the marvelous site where so ",
            "many discoveries were made in the early 20th Century. Giants walk the pages of ",
            "Rhodes' book: men like J.J. Thomson, Ernest Rutherford, Niels Bohr, <a href='#'>Robert ",
            "Oppenheimer</a>, Enrico Fermi, Leo Szilard, Leslie Groves, Vannevar Bush, and Albert ",
            "Einstein all make important appearances throughout the book. ...",
            ""
        };
        
        StringBuffer sbExample = new StringBuffer();
        for (String line : exampleLines)
        {
            sbExample.append(line);
            sbExample.append("\n");
        }
        
        return sbExample.toString();    
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
        // Insert BASE element.
        for (int i = 0; i < fragmentList.size(); i++)
        {
            QdmlFragment fragment = fragmentList.get(i);
            if (fragment instanceof QdmlStartTagFragment)
            {
                QdmlStartTagFragment startTag = (QdmlStartTagFragment)fragment;
                if (startTag.isStartTagCi("HEAD"))
                {
                    QdmlElementFragment elem = new QdmlElementFragment("<base href='" + baseUrl + "'>");
                    fragmentList.add(i + 1, elem);
                    break;
                }
            }
        }

        // Now add the links.
        return addLinksToHtmlDocument(fragmentList);
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
        String startTagText = "<a style='font-weight:bold; color:red' href='" + url + "' target='_top'>";
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
