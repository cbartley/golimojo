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

        return addLinksToHtmlDocument(fragmentList);
    }
    
    // ---------------------------------------- Linker addLinksToHtmlDocument

    public List<QdmlFragment> addLinksToHtmlDocument(List<QdmlFragment> fragmentList)
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
        while (index < fragmentList.size())
        {
            int phraseFragmentCount = findLongestPhraseAtPosition(pageDataStore, fragmentList, index);

            // If we matched a phrase, copy and link it.
            if (phraseFragmentCount > 0)
            {
                String phraseText = TextFragment.subJoin(fragmentList, index, phraseFragmentCount);
                String pageTitle = pageDataStore.findMatchingPageTitle(phraseText);
                copyLinkedPhrase(fragmentList, index, phraseFragmentCount, pageTitle, receiverList);
                index += phraseFragmentCount;
            }
        
            // Otherwise copy over one fragment and advance to the next one.
            else
            {
                receiverList.add(fragmentList.get(index));
                index += 1;
            }
        }
    }

    // ---------------------------------------- Linker findLongestPhraseAtPosition

    private static int findLongestPhraseAtPosition(PageDataStore pageDataStore, List<TextFragment> fragmentList, int index)
    {
        int maxFragmentCount = 7;
        int minFragmentCount = 1;
        maxFragmentCount = Math.min(maxFragmentCount, fragmentList.size() - index);
        for (int i = maxFragmentCount; i >= minFragmentCount; i--)
        {
            String phrase = TextFragment.subJoin(fragmentList, index, i);
            if (pageDataStore.findMatchingPageTitle(phrase) != null) return i;
        }

        return 0;
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
