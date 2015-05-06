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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/* ------------------------------------------------------------ */
/* -------------------- class PageDataStore ------------------- */
/* ------------------------------------------------------------ */

public class PageDataStore
{
    
    // ---------------------------------------- PageDataStore instance variables
    
    private CrudeFragmentPhrasePrefixHashSet _pageTitlePrefixSet;
    private Hashtable<String, PageData> _pageTitleBag;
    private Ranker _ranker;
    private StopList _stopList;
    
    // ---------------------------------------- PageDataStore constructor
    
    public PageDataStore(Ranker ranker, String articleTitlesFilePath) throws Exception
    {
        List<PageData> pageDataList = PageData.readPageTitles(articleTitlesFilePath);
        _stopList = new StopList(pageDataList);
        _ranker = ranker;

        List<PageData> pageDataList2 = new ArrayList<PageData>();
        for (PageData pageData : pageDataList)
        {
            if (isHighQualityPage(pageData))
            {
                pageDataList2.add(pageData);
            }
        }
        pageDataList = pageDataList2;

        _pageTitlePrefixSet = createPageTitlePrefixSet(pageDataList);
        _pageTitleBag = createPageTitleBag(pageDataList);
        _stopList = new StopList(pageDataList);
        _ranker = ranker;
    }

    // ---------------------------------------- PageDataStore findMatchingPageTitleAtPosition

    public PageTitleMatch findMatchingPageTitleAtPosition(List<TextFragment> fragmentList, int startIndex)
    {
        int maxMatchCount = findLongestPossibleMatchingPageTitleAtPosition(fragmentList, startIndex);
        for (int count = maxMatchCount; count > 0; count--)
        {
            String phrase = TextFragment.subJoin(fragmentList, startIndex, count);
            String pageTitle = findMatchingPageTitle(phrase);
            if (pageTitle != null)
            {
                return new PageTitleMatch(pageTitle, count);
            }
        }
        return null;
    }

    // ---------------------------------------- PageDataStore class PageTitleMatch

    public static class PageTitleMatch
    {
        String _pageTitle;
        int _fragmentMatchCount;
        
        public PageTitleMatch(String pageTitle, int fragmentMatchCount)
        {
            _pageTitle = pageTitle;
            _fragmentMatchCount = fragmentMatchCount;
        }

        String getPageTitle()       { return _pageTitle; }
        int getFragmentMatchCount() { return _fragmentMatchCount; }
    }

    // ---------------------------------------- PageDataStore findLongestPossibleMatchingPageTitleAtPosition
    
    private int findLongestPossibleMatchingPageTitleAtPosition(List<TextFragment> fragmentList, int startIndex)
    {
        int maxCount = fragmentList.size() - startIndex;
        if (maxCount == 0) return 0;
        final TextFragment.FragmentType Whitespace = TextFragment.FragmentType.Whitespace;
        if (fragmentList.get(startIndex).getType() == Whitespace) return 0;
        for (int count = 1; count <= maxCount; count++)
        {
            int endIndex = startIndex + count;
            if (fragmentList.get(endIndex - 1).getType() == Whitespace) continue;
            if (_pageTitlePrefixSet.contains(fragmentList, startIndex, endIndex)) continue;
            return count - 1;   // we went one too far
        }
        return maxCount;    // we matched all the way to the end
    }

    // ---------------------------------------- PageDataStore findMatchingPageTitle
    
        public static void testPresent(PageDataStore pageDataStore, String possiblePageTitle)
        {
            if (pageDataStore.findMatchingPageTitle(possiblePageTitle) == null)
            {
                System.out.printf("### %s\n", possiblePageTitle);
            }
        }

        public static void testNotPresent(PageDataStore pageDataStore, String possiblePageTitle)
        {
            if (pageDataStore.findMatchingPageTitle(possiblePageTitle) != null)
            {
                System.out.printf("### [%s]\n", possiblePageTitle);
            }           
        }

        public static void L2TEST_testLookup(PageDataStore pageDataStore)
        {
            testNotPresent(pageDataStore, "George");
            testNotPresent(pageDataStore, "David");
//          testNotPresent(pageDataStore, "Tamara");
            testNotPresent(pageDataStore, "Wednesday");
            testNotPresent(pageDataStore, "Thursday");
            testNotPresent(pageDataStore, "February");
            
            testPresent(pageDataStore, "Americans");
            testPresent(pageDataStore, "Doha");
            testPresent(pageDataStore, "Iraq");
            testPresent(pageDataStore, "Washington");
            testPresent(pageDataStore, "Michigan");
            testPresent(pageDataStore, "NORAD");
            testPresent(pageDataStore, "Boston");
            testPresent(pageDataStore, "Palestinian");
            testPresent(pageDataStore, "Lebanese");
            testPresent(pageDataStore, "Hezbollah");
            testPresent(pageDataStore, "Islamic");
            testPresent(pageDataStore, "Hamas");
            testPresent(pageDataStore, "Muslims");
            testPresent(pageDataStore, "Shia");
            testPresent(pageDataStore, "Sunni");
            
        }

    public String findMatchingPageTitle(String possiblePageTitle)
    {
        PageData matchingPageData = _pageTitleBag.get(possiblePageTitle.toLowerCase());
        if (matchingPageData == null) return null;
        
        String matchingPageTitle = matchingPageData.getTitle();
        int matchStrength = caseSensitiveMatch(possiblePageTitle, matchingPageTitle);   
        if (matchStrength < 0) return null;
        
        int wordCount = matchingPageData.getWordCount();
        if (possiblePageTitle.startsWith("the "))
        {
            wordCount--;
        }
        
        if (wordCount == 1)
        {
            if (matchStrength < 1) return null;
        }

        return matchingPageTitle;
    }

    // ---------------------------------------- PageDataStore isHighQualityPage
    
    private boolean isHighQualityPage(PageData pageData)
    {
        String pageTitle = pageData.getTitle();
        
        Integer[] results = _ranker.phraseWeightAndWordCount(pageTitle);
        int frequencyRank = results[0];
        int wordCount = results[1];
        int wordsCharCount = results[2];
        int refRank = pageData.getRefRank();

//      System.out.printf("*** %-32s %-32s %7dM %7dF %7dR\n", 
//              "[" + possiblePageTitle + "]", "[" + matchingPageTitle + "]", matchStrength, frequencyRank, refRank);

        if (wordsCharCount < 4) return false;
        
        if (pageTitle.startsWith("The "))
        {
            wordCount = wordCount - 1;
        }

        if (wordCount == 1)
        {
            if (_stopList.hasWord(pageTitle)) return false;

            if (frequencyRank < 50000)
            {
                if (5 * refRank < frequencyRank) return true;
                return false;
            }

            if (refRank < 5 * frequencyRank) return true;

            return false;
        }
        
        if (wordCount == 2)
        {
            if (refRank < 10 * frequencyRank) return true;

            return false;
        }
        
        if (wordCount > 2)
        {
            if (refRank < 10 * frequencyRank) return true;

            return false;
        }

        return false;
    }

    // ---------------------------------------- PageDataStore caseSensitiveMatch
    
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

    // ---------------------------------------- PageDataStore createPageTitlePrefixSet
        
    private static CrudeFragmentPhrasePrefixHashSet createPageTitlePrefixSet(List<PageData> pageDataList)
    {
        List<String> pageTitleList = new ArrayList<String>();
        for (PageData pageData : pageDataList)
        {
            String pageTitle = pageData.getTitle();
            pageTitleList.add(pageTitle);
        }
        return new CrudeFragmentPhrasePrefixHashSet(pageTitleList);
    }

    // ---------------------------------------- PageDataStore createPageTitleBag

    private static Hashtable<String, PageData> createPageTitleBag(List<PageData> pageDataList) throws Exception
    {
        long startTimeMs = new Date().getTime();
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

}




/* ------------------------------------------------------------ */
/* ---------- class CrudeFragmentPhrasePrefixHashSet ---------- */
/* ------------------------------------------------------------ */

class CrudeFragmentPhrasePrefixHashSet
{
    // ---------------------------------------- CrudeFragmentPhrasePrefixHashSet instance variables
    
    private CrudeIntegerHashSet _crudeHashSet;

    // ---------------------------------------- CrudeFragmentPhrasePrefixHashSet constructor
    
    public CrudeFragmentPhrasePrefixHashSet(List<String> phraseList)
    {
        // Allocate a crude integer hash set that we are confident will be considerably
        // larger than our total number of prefixes.  We need a lot more clear bits 
        // than set bits in order to get good performance.
        int slotCount = totalLength(phraseList) * 10;
        _crudeHashSet = new CrudeIntegerHashSet(slotCount);
        storePrefixesForPhrases(_crudeHashSet, phraseList);
    }

    // ---------------------------------------- CrudeFragmentPhrasePrefixHashSet contains
    
    public boolean contains(List<TextFragment> fragmentList, int startIndex, int endIndex)
    {
        int hashCode = TextFragment.computeHashCode(fragmentList, startIndex, endIndex);
        return _crudeHashSet.contains(hashCode);
    }

    public static void L1TEST_contains()
    {
        String phrase = "foo bar";
        List<TextFragment> fragmentList = TextFragment.splitTextIntoFragments(phrase);
        List<String> phraseList = new ArrayList<String>();
        phraseList.add(phrase);
        CrudeFragmentPhrasePrefixHashSet set = new CrudeFragmentPhrasePrefixHashSet(phraseList);
        assert set.contains(fragmentList, 0, 1);
        assert set.contains(fragmentList, 0, 2);
        assert set.contains(fragmentList, 0, 3);
        
        List<TextFragment> fragmentList2 = TextFragment.splitTextIntoFragments("foo baz");
        assert set.contains(fragmentList2, 0, 1);
        assert set.contains(fragmentList2, 0, 2);
        assert !set.contains(fragmentList2, 0, 3);
    }

    // ---------------------------------------- CrudeFragmentPhrasePrefixHashSet totalLength
    
    private static int totalLength(List<String> phraseList)
    {
        int totalLength = 0;
        for (String phrase : phraseList)
        {
            totalLength += phrase.length();
        }
        return totalLength;
    }

    public static void L1TEST_totalLength()
    {
        List<String> phraseList = new ArrayList<String>();
        phraseList.add("123");
        assert totalLength(phraseList) == 3;
        phraseList.add("456");
        assert totalLength(phraseList) == 6;
    }

    // ---------------------------------------- CrudeFragmentPhrasePrefixHashSet storePrefixesForPhrases
    
    private static void storePrefixesForPhrases(CrudeIntegerHashSet crudeHashSet, List<String> phraseList)
    {
        for (String phrase : phraseList)
        {
            storePrefixesForPhrase(crudeHashSet, phrase);
        }
    }

    // ---------------------------------------- CrudeFragmentPhrasePrefixHashSet storePrefixesForPhrase
    
    private static void storePrefixesForPhrase(CrudeIntegerHashSet crudeHashSet, String phrase)
    {
        List<TextFragment> fragmentList = TextFragment.splitTextIntoFragments(phrase);
        for (int i = 0; i < fragmentList.size(); i++)
        {
            int hashCode = TextFragment.computeHashCode(fragmentList, 0, i + 1);
            crudeHashSet.add(hashCode);         
        }
    }
    
    public static void L1TEST_storePrefixesForPhrase()
    {
        String phrase = "foo bar";
        List<TextFragment> fragmentList = TextFragment.splitTextIntoFragments("foo bar");
        CrudeIntegerHashSet set = new CrudeIntegerHashSet(1000);
        storePrefixesForPhrase(set, phrase);
        assert !set.contains(0);
        assert set.contains(TextFragment.computeHashCode(fragmentList, 0, 1));
        assert set.contains(TextFragment.computeHashCode(fragmentList, 0, 2));
        assert set.contains(TextFragment.computeHashCode(fragmentList, 0, 3));
    }

}

/* ------------------------------------------------------------ */
/* ----------------- class CrudeIntegerHashSet ---------------- */
/* ------------------------------------------------------------ */
/**
 *  A CrudeIntegerHashSet will always return true if it contains a particular
 *  value.  However, in some circumstances it will also return true for 
 *  values it does not contain.  In order to minimize these circumstances,
 *  the "slotCount" used to create the object should be carefully chosen
 *  to always be much larger than the total number of keys that will
 *  ever be stored in a particular object.  It's called "crude" for a reason.
 */

class CrudeIntegerHashSet
{
    // ---------------------------------------- CrudeIntegerHashSet instance variables
    
    private BitSet _bitSet;

    // ---------------------------------------- CrudeIntegerHashSet constructor
    
    public CrudeIntegerHashSet(int slotCount)
    {
        _bitSet = new BitSet(slotCount);
    }

    // ---------------------------------------- CrudeIntegerHashSet add
    
    public void add(int key)
    {
        int slotAddress = computeSlotAddress(key);
        _bitSet.set(slotAddress);
    }

    // ---------------------------------------- CrudeIntegerHashSet contains
    
    public boolean contains(int key)
    {
        int slotAddress = computeSlotAddress(key);
        return _bitSet.get(slotAddress);
    }

    // ---------------------------------------- CrudeIntegerHashSet computeSlotAddress
    
    private int computeSlotAddress(int key)
    {
        int slotAddress = Math.abs(key) % _bitSet.size();
        return slotAddress;
    }

    // ---------------------------------------- CrudeIntegerHashSet test code
    
    public static void L1TEST_CrudeIntegerHashSet()
    {
        CrudeIntegerHashSet set = new CrudeIntegerHashSet(4);
        assert !set.contains(0);
        assert !set.contains(1);
        assert !set.contains(2);
        assert !set.contains(3);
        
        set.add(0);
        assert set.contains(0);
        assert !set.contains(1);
        assert !set.contains(2);
        assert !set.contains(3);
        
        set.add(2);
        assert set.contains(0);
        assert !set.contains(1);
        assert set.contains(2);
        assert !set.contains(3);
        
        set.add(set._bitSet.size() + 1);
        assert set.contains(set._bitSet.size() + 1);
        assert set.contains(1);
    }
    
}

