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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/* ------------------------------------------------------------ */
/* -------------------- class PageDataStore ------------------- */
/* ------------------------------------------------------------ */

public class PageDataStore
{
    
    // ---------------------------------------- PageDataStore instance variables
    
    private Hashtable<String, PageData> _pageTitleBag;
    private int _maxPageRefCount;
    private Ranker _ranker;
    
    // ---------------------------------------- PageDataStore constructor
    
    public PageDataStore(Ranker ranker, String articleTitlesFilePath) throws Exception
    {
        _pageTitleBag = readPageTitles(ranker, articleTitlesFilePath);
        _maxPageRefCount = computeMaxRefCount(_pageTitleBag);
        _ranker = ranker;
    }

    // ---------------------------------------- PageDataStore readArticleTitles
    
    public String findMatchingPageTitle(String possiblePageTitle)
    {
        PageData matchingPageData = _pageTitleBag.get(possiblePageTitle.toLowerCase());
        if (matchingPageData == null) return null;
        String matchingPageTitle = matchingPageData.getTitle();
        int matchStrength = caseSensitiveMatch(possiblePageTitle, matchingPageTitle);
        Integer[] results = _ranker.phraseWeightAndWordCount(possiblePageTitle);
        int frequencyRank = results[0];
        int wordCount = results[1];
        int wordsCharCount = results[2];
        int refRank = matchingPageData.getRefRank();

        System.out.printf("*** %-32s %-32s %7dM %7dF %7dR\n", 
                "[" + possiblePageTitle + "]", "[" + matchingPageTitle + "]", matchStrength, frequencyRank, refRank);

//      if (wordCount != 1) return null;

        if (matchStrength < 0) return null;
        if (wordsCharCount < 4) return null;
        
        if (possiblePageTitle.startsWith("the "))
        {
            wordCount = wordCount - 1;
        }

        if (wordCount == 1)
        {
            if (matchStrength < 1) return null;

            if (frequencyRank < 50000)
            {
                if (5 * refRank < frequencyRank) return matchingPageTitle;
                return null;
            }

            if (refRank < 5 * frequencyRank) return matchingPageTitle;

            return null;
        }
        
        if (wordCount == 2)
        {
            if (refRank < 10 * frequencyRank) return matchingPageTitle;

            return null;
        }
        
        if (wordCount > 2)
        {
            if (refRank < 10 * frequencyRank) return matchingPageTitle;

            return null;
        }
        
        
        
        return null;
    }

    // ---------------------------------------- PageDataStore xxxx
    
    private static int caseSensitiveMatch(String matchee, String reference)
    {
        // Return -1 if there is no match at all.
        if (!matchee.equalsIgnoreCase(reference)) return -1;

        int strength = 0;
        for (int i = 0; i < matchee.length(); i++)
        {
            boolean matcheeCharIsUpperCase = Character.isUpperCase(matchee.charAt(i));
            boolean referenceCharIsUpperCase = Character.isUpperCase(reference.charAt(i));

            if (matcheeCharIsUpperCase && referenceCharIsUpperCase)
            {
                strength++;
            }
            
            else if (!matcheeCharIsUpperCase && referenceCharIsUpperCase && i > 0)
            {
                // Uppercase reference characters must be fully matched -- except for the first character.
                // This is because wikipedia articles pretty much always start with an uppercase letter.
                return -1;
            }

            else if (matcheeCharIsUpperCase && !referenceCharIsUpperCase)
            {
                // We still match but this will be a minimum strength match.
                return 0;   // We match, but not strongly.
            }
        }

        return strength;
    }
    
        public static void L1TEST_caseSensitiveMatch()
        {
            assert caseSensitiveMatch("the times", "the times") == 0;
            assert caseSensitiveMatch("The Times", "the times") == 0;
            assert caseSensitiveMatch("The times", "the times") == 0;
            assert caseSensitiveMatch("the Times", "the times") == 0;
            
            assert caseSensitiveMatch("the", "The") == 0;
            assert caseSensitiveMatch("The", "The") == 1;

            // Note the looser constraints on the first character.
            assert caseSensitiveMatch("the Times", "The Times") == 1;
            assert caseSensitiveMatch("The Times", "The Times") == 2;
            assert caseSensitiveMatch("The times", "The Times") < 0;
            assert caseSensitiveMatch("THE TIMES", "The Times") == 0;
            assert caseSensitiveMatch("The TimeS", "The Times") == 0;

            assert caseSensitiveMatch("the times", "the timesx") < 0;
            assert caseSensitiveMatch("the times", "xhe times") < 0;
            assert caseSensitiveMatch("the times", "txe times") < 0;
            
            assert caseSensitiveMatch("The New York Times", "The New York Times") == 4;
            assert caseSensitiveMatch("the New York Times", "The New York Times") == 3;
            assert caseSensitiveMatch("The New York times", "The New York Times") < 0;
            assert caseSensitiveMatch("The NEW York Times", "The New York Times") == 0;
        }

    // ---------------------------------------- PageDataStore xxxx
    
    private static boolean partiallyCaseSensitivePageTitleMatch(String matchee, String matchStandard)
    {
        // If it doesn't match at all then return false immediately.
        if (!matchee.equalsIgnoreCase(matchStandard)) return false;
        
        // If any character after the first in the standard is uppercase then we
        // require at least that that character be upper case in the matchee.
        // We ignore the first character since Wikipedia articles pretty much 
        // all seem to start with an uppercase character.
        for (int i = 1; i < matchee.length(); i++)
        {
            if (Character.isUpperCase(matchStandard.charAt(i)))
            {
                if (!Character.isUpperCase(matchee.charAt(i))) return false;
            }
        }

        // We matched!
        return true;
    }
    
        public static void L1TEST_partiallyCaseSensitivePageTitleMatch()
        {
            assert partiallyCaseSensitivePageTitleMatch("the times", "the times");
            assert partiallyCaseSensitivePageTitleMatch("The Times", "the times");
            assert partiallyCaseSensitivePageTitleMatch("The times", "the times");
            assert partiallyCaseSensitivePageTitleMatch("the Times", "the times");

            // Note the looser constraints on the first character.
            assert partiallyCaseSensitivePageTitleMatch("the Times", "The Times");
            assert partiallyCaseSensitivePageTitleMatch("The Times", "The Times");
            assert !partiallyCaseSensitivePageTitleMatch("The times", "The Times");
            assert partiallyCaseSensitivePageTitleMatch("THE TIMES", "The Times");
            assert partiallyCaseSensitivePageTitleMatch("The TimeS", "The Times");

            assert !partiallyCaseSensitivePageTitleMatch("the times", "the timesx");
            assert !partiallyCaseSensitivePageTitleMatch("the times", "xhe times");
            assert !partiallyCaseSensitivePageTitleMatch("the times", "txe times");
        }

    // ---------------------------------------- PageDataStore readPageTitles

    private static Hashtable<String, PageData> readPageTitles(Ranker ranker, String articleTitlesFilePath) throws Exception
    {
        long startTimeMs = new Date().getTime();

        List<PageData> pageDataList = readPageTitlesIntoList(ranker, articleTitlesFilePath);
StopList.shuffle(pageDataList);
        computeAndApplyRankings(pageDataList);
        Hashtable<String, PageData> articleTitleBag = new Hashtable<String, PageData>();
        for (PageData pageData : pageDataList)
        {
            String lowerCasePageTitle = pageData.getTitle().toLowerCase();
            if (articleTitleBag.get(lowerCasePageTitle) == null)
            {
                articleTitleBag.put(lowerCasePageTitle, pageData);
            }
        }

        long endTimeMs = new Date().getTime();
        long elapsedTimeMs = endTimeMs - startTimeMs;
        System.out.println(pageDataList.size());
        System.out.printf("%.2f\n", new Object[] {elapsedTimeMs / 1000.0});

        return articleTitleBag;
    }

    // ---------------------------------------- PageDataStore readPageTitlesIntoList

    private static List <PageData> readPageTitlesIntoList(Ranker ranker, String articleTitlesFilePath) throws Exception
    {
        List<PageData> pageDataList = new ArrayList<PageData>();
        BufferedReader in = new BufferedReader(new FileReader(articleTitlesFilePath));
        try
        {

            int articleTitleCount = 0;
            while (true)
            {
                String articleTitleLine = in.readLine();
                if (articleTitleLine == null) break;
                articleTitleLine = articleTitleLine.replace("*** ", "");
                String[] articleTitleFields = articleTitleLine.split("\t");
                if (articleTitleFields.length < 2) continue;
                int articleRefCount = Integer.parseInt(articleTitleFields[0]);
                String articleTitle = articleTitleFields[1];
                articleTitle = articleTitle.trim();
                articleTitle = articleTitle.replace('_', ' ');
                PageData pageData = new PageData(articleTitle, articleRefCount);
                pageDataList.add(pageData);
                articleTitleCount++;
            }
        }
        finally
        {
            in.close();
        }
        
        return pageDataList;

    }

    // ---------------------------------------- PageDataStore xxxxx
    
    private static void computeAndApplyRankings(List<PageData> pageDataList)
    {
        Comparator<PageData> refCountComparator = new Comparator<PageData>()
        {
            public int compare(PageData pd1, PageData pd2)
            {
                // Sort first by refcount descending.
                int refCountComp = -(pd1.getRefCount() - pd2.getRefCount());
                if (refCountComp != 0) return refCountComp;
                int titleComp = pd1.getTitle().compareTo(pd2.getTitle());
                return titleComp;
            }
        };

        Collections.sort((List<PageData>)pageDataList, refCountComparator);
        
        for (int i = 0; i < pageDataList.size(); i++)
        {
            pageDataList.get(i).setRefRank(i);
        }
        
    }

    // ---------------------------------------- PageDataStore computeMaxRefCount
    
    private static int computeMaxRefCount(Hashtable<String, PageData> pageDataBag)
    {
        int maxRefCount = 0;
        for (PageData pageData : pageDataBag.values())
        {
            maxRefCount = Math.max(maxRefCount, pageData.getRefCount());
        }
        return maxRefCount;
    }

    // ---------------------------------------- class PageData
    
    public static class PageData
    {
        private String _title;
        private int _refCount;
        private int _refRank;
        
        public PageData(String title, int refCount)
        {
            _title = title;
            _refCount = refCount;
            _refRank = -1;
        }

        public String getTitle()
        {
            return _title;
        }

        public int getRefCount()
        {
            return _refCount;
        }

        public int getRefRank()
        {
            return _refRank;
        }

        public void setRefRank(int rank)
        {
            _refRank = rank;
        }
    }

}
