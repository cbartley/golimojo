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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

/* ------------------------------------------------------------ */
/* --------------------- class PageFilter --------------------- */
/* ------------------------------------------------------------ */

public class PageFilter
{
    // ---------------------------------------- PageFilter removeLowQualityPages

    public static void removeLowQualityPages(Hashtable<String, PageData> pageDataBag, Ranker ranker)
    {
        // TODO: Be more systematic and explicit about these sorts of things.
        addPageAlias(pageDataBag, "MiG-15", "MIG 15");

        List<PageData> pageDataList = new ArrayList<PageData>(pageDataBag.values());
        HashSet<String> stopWordSet = createStopWordSet(pageDataBag);
        
        for (PageData pageData : pageDataList)
        {
            if (!isHighQualityPage(pageData, ranker, stopWordSet))
            {
                pageDataBag.remove(pageData.getTitle().toLowerCase());
            }
        }
    }

    // ---------------------------------------- PageFilter isHighQualityPage

    private static SimpleRule rule1 = new SimpleRule(new Range(1, 1), new Range(null, 50000), 1, 1);
    private static SimpleRule rule2 = new SimpleRule(new Range(1, 1), new Range(50001, null), 5, 1);
    private static SimpleRule rule3 = new SimpleRule(new Range(2, null), new Range(null, null), 13, 1);
    
    private static boolean isHighQualityPage(PageData pageData, Ranker ranker, HashSet<String> stopWordSet)
    {
        String pageTitle = pageData.getTitle();
        
        Ranker.PageScore pageScore = ranker.phraseWeightAndWordCount(pageTitle);
        int wordCount = pageScore.getWordCount();
        int wordsCharCount = pageScore.getWordCharCount();

        if (wordsCharCount < 4) return false;
        
        if (pageTitle.startsWith("The "))
        {
            wordCount = wordCount - 1;
        }
        
        if (!rule1.test(pageData, pageScore)) return false;
        if (!rule2.test(pageData, pageScore)) return false;
        if (!rule3.test(pageData, pageScore)) return false;
        
        if (wordCount == 1 && stopWordSet.contains(pageTitle.toLowerCase())) return false;

        return true;
    }

    // ---------------------------------------- static class SimpleRule
    
    private static class SimpleRule
    {
        private Range _wordCountRange;
        private Range _freqRange;
        int _freqScale;
        int _refScale;
        
        public SimpleRule(Range wordCountRange, Range freqRange, int freqScale, int refScale)
        {
            _wordCountRange = wordCountRange;
            _freqRange = freqRange;
            _freqScale = freqScale;
            _refScale = refScale;
        }
        
        public boolean test(PageData pageData, Ranker.PageScore pageScore)
        {
            int frequencyRank = pageScore.getWeight();
            int wordCount = pageScore.getWordCount();
            int refRank = pageData.getRefRank();
            
            if (!_wordCountRange.inRange(wordCount)) return true;
            if (!_freqRange.inRange(frequencyRank)) return true;
            boolean passed = frequencyRank * _freqScale > refRank * _refScale;
        
            if (!passed)
            {
//              String pageTitle = pageData.getTitle();
//              System.out.printf("%-30s ", "[" + pageTitle + "]");
//              System.out.printf("W %s (%d) ", _wordCountRange, wordCount);
//              System.out.printf("F %-12s ==> ", _freqRange);
//              System.out.printf("F %7d * %2d >? R%7d * %2d", frequencyRank, _freqScale, refRank, _refScale);
//              System.out.printf(" ==> F %7d >? R %7d\n", frequencyRank * _freqScale, refRank * _refScale);
            }
            
            return passed;
        }
        
        
    }

    // ---------------------------------------- static class Range

    private static class Range
    {
        private Integer _rangeStartInclusive;
        private Integer _rangeEndInclusive;

        public Range(Integer rangeStartInclusive, Integer rangeEndInclusive)
        {
            _rangeStartInclusive = rangeStartInclusive;
            _rangeEndInclusive = rangeEndInclusive;
        }
        
        public boolean inRange(int value)
        {
            if (_rangeStartInclusive != null && value < _rangeStartInclusive) return false;
            if (_rangeEndInclusive != null && _rangeEndInclusive < value) return false;
            return true;
        }
        
        public String toString()
        {
            String startText = _rangeStartInclusive != null ? Integer.toString(_rangeStartInclusive) : ".";
            String endText = _rangeEndInclusive != null ? Integer.toString(_rangeEndInclusive) : ".";
            return "[" + startText + ".." + endText + "]";
        }
        
    }

    // ---------------------------------------- PageFilter createStopWordSet
    
    private static HashSet<String> createStopWordSet(Hashtable<String, PageData> pageDataBag)
    {
        List<PageData> pageDataList = createRefCountOrderedPageDataList(pageDataBag);
    
        // Identify "hard" stop words.  Almost all of these 
        // words are common first and last names.
        List<String> stopWordList200 = identifyStopWords(pageDataList, 200);
        makeLowerCase(stopWordList200);
        HashSet<String> stopWordSet200 = new HashSet<String>(stopWordList200);
        
        // Identify softer stop words.  Some of these we'll want to keep
        // as stop words and some we'll want to pass -- note heuristics.
        List<String> stopWordList = identifyStopWords(pageDataList, 2000);
        makeLowerCase(stopWordList);
        HashSet<String> stopWordSet = new HashSet<String>(stopWordList);
        for (String stopWord : stopWordList)
        {
//if (stopWordSet200.contains(stopWord)) System.out.printf("+++ %s\n", stopWord);
            if (stopWordSet200.contains(stopWord)) continue;
            PageData pageData = pageDataBag.get(stopWord);
//if (pageData == null) System.out.printf("+++ %s\n", stopWord);
            if (pageData == null) continue;
//if (pageData.getRefRank() > 10000) System.out.printf("+++ %s\n", stopWord);
            if (pageData.getRefRank() > 10000) continue;
//System.out.printf("--- %s\n", stopWord);
            stopWordSet.remove(stopWord);
        }

        stopWordSet.add("sunday");
        stopWordSet.add("monday");
        stopWordSet.add("tuesday");
        stopWordSet.add("wednesday");
        stopWordSet.add("thursday");
        stopWordSet.add("friday");
        stopWordSet.add("saturday");

        stopWordSet.add("january");
        stopWordSet.add("february");
        stopWordSet.add("march");
        stopWordSet.add("april");
        stopWordSet.add("may");
        stopWordSet.add("june");
        stopWordSet.add("july");
        stopWordSet.add("august");
        stopWordSet.add("september");
        stopWordSet.add("october");
        stopWordSet.add("november");
        stopWordSet.add("december");
        
        stopWordSet.add("rhodes");
        stopWordSet.add("i'll");
        stopWordSet.add("aldrin");

        return stopWordSet;
    }

    // ---------------------------------------- PageFilter createRefCountOrderedPageDataList
    
    private static List<PageData> createRefCountOrderedPageDataList(Hashtable<String, PageData> pageDataBag)
    {
        List<PageData> pageDataList = new ArrayList<PageData>(pageDataBag.values());

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
    
        Collections.sort(pageDataList, refCountComparator);
        return pageDataList;
    }

    // ---------------------------------------- PageFilter makeLowerCase
    
    private static void makeLowerCase(List<String> list)
    {
        for (int i = 0; i < list.size(); i++)
        {
            list.set(i, list.get(i).toLowerCase());
        }
    }

    // ---------------------------------------- PageFilter identifyStopWords
    
    private static List<String> identifyStopWords(List<PageData> pageDataList, int topCount)
    {
        final int firstNameIndex = 0;
        final int lastNameIndex = 1;

        pageDataList = pageDataList.subList(0, Math.min(300 * 1000, pageDataList.size()));

        List<String[]> possibleNamesTupleList = extractPossibleNames(pageDataList);
        
        // Get the most popular "first names" -- these are likely to actually *be* names.
        List<String> topOrderedFirstNameList = extractTopOrderedUniqueItems(possibleNamesTupleList, firstNameIndex);
        topOrderedFirstNameList = topOrderedFirstNameList.subList(0, topCount);
        
        // Match popular first names to "last names" and select the most popular of those.
        List<String[]> possibleLastNameList = selectItemsUsingFilter(possibleNamesTupleList, topOrderedFirstNameList, firstNameIndex);
        List<String> topOrderedLastNameList = extractTopOrderedUniqueItems(possibleLastNameList, lastNameIndex);
        topOrderedLastNameList = topOrderedLastNameList.subList(0, topCount);
        
        // Now use our probable last name list to generate a refined probable first name list.
        List<String[]> possibleFirstNameList = selectItemsUsingFilter(possibleNamesTupleList, topOrderedLastNameList, lastNameIndex);
        topOrderedFirstNameList = extractTopOrderedUniqueItems(possibleFirstNameList, firstNameIndex);
        topOrderedFirstNameList = topOrderedFirstNameList.subList(0, topCount);
        
        // Now use our refined probable first name list to generate a refined probable last name list.
        possibleLastNameList = selectItemsUsingFilter(possibleNamesTupleList, topOrderedFirstNameList, firstNameIndex);
        topOrderedLastNameList = extractTopOrderedUniqueItems(possibleLastNameList, lastNameIndex);
        topOrderedLastNameList = topOrderedLastNameList.subList(0, topCount);

//      for (String name : topOrderedFirstNameList)
//      {
//          System.out.printf("*** %s\n", name);
//      }
        
//      for (String name : topOrderedLastNameList)
//      {
//          System.out.printf("... %s\n", name);
//      }
        
        List<String> stopWordList = new ArrayList<String>();
        stopWordList.addAll(topOrderedFirstNameList);
        stopWordList.addAll(topOrderedLastNameList);
//for (String stopWord : stopWordList) System.out.printf("$$$ %s\n", stopWord);
        return stopWordList;
    }

    // ---------------------------------------- PageFilter extractPossibleNames
    
    private static List<String[]> extractPossibleNames(List<PageData> pageDataList)
    {
        List<String[]> possibleNamesTupleList = new ArrayList<String[]>();
        for (PageData pageData : pageDataList)
        {
            String[] pageTitleTuple = pageData.getTitle().split(" ");
            if (pageTitleTuple.length == 2)
            {
                char ch0 = pageTitleTuple[0].charAt(0);
                char ch1 = pageTitleTuple[1].charAt(0);
                if (Character.isUpperCase(ch0) && Character.isUpperCase(ch1))
                {
                    possibleNamesTupleList.add(pageTitleTuple);
                }
            }
        }
        return possibleNamesTupleList;
    }

    // ---------------------------------------- PageFilter selectItemsUsingFilter
    
    private static List<String[]> selectItemsUsingFilter(List<String[]> itemTupleList, List<String> filterList, int keyIndex)
    {
        Set<String> filterSet = new HashSet<String>(filterList);
        List<String[]> filteredItemTupleList = new ArrayList<String[]>();
        for (String[] itemTuple : itemTupleList)
        {
            String key = itemTuple[keyIndex];
            if (filterSet.contains(key))
            {
                filteredItemTupleList.add(itemTuple);
            }
        }
        return filteredItemTupleList;
    }

        public static void L1TEST_selectItemsUsingFilter()
        {
            List<String[]> itemTupleList = new ArrayList<String[]>();
            itemTupleList.add(new String[] {"baz", "alpha"});
            itemTupleList.add(new String[] {"bar", "bravo"});
            itemTupleList.add(new String[] {"bar", "charlie"});
            itemTupleList.add(new String[] {"foo", "charlie"});
            itemTupleList.add(new String[] {"foo", "charlie"});
            itemTupleList.add(new String[] {"foo", "delta"});
            
            List<String> filterList = new ArrayList<String>();
            
            assert selectItemsUsingFilter(itemTupleList, filterList, 0).size() == 0;
            assert selectItemsUsingFilter(itemTupleList, filterList, 1).size() == 0;
            
            filterList.add("foo");
            assert selectItemsUsingFilter(itemTupleList, filterList, 0).size() == 3;
            assert selectItemsUsingFilter(itemTupleList, filterList, 1).size() == 0;
            
            filterList.add("charlie");
            assert selectItemsUsingFilter(itemTupleList, filterList, 0).size() == 3;
            assert selectItemsUsingFilter(itemTupleList, filterList, 1).size() == 3;
            
            filterList.add("bar");
            filterList.add("bravo");
            assert selectItemsUsingFilter(itemTupleList, filterList, 0).size() == 5;
            assert selectItemsUsingFilter(itemTupleList, filterList, 1).size() == 4;
        }

    // ---------------------------------------- PageFilter extractTopOrderedUniqueItems
    
    private static List<String> extractTopOrderedUniqueItems(List<String[]> itemTupleList, int keyIndex)
    {
        Comparator<Entry<String, Integer>> descendingCountComparator = new Comparator<Entry<String, Integer>>()
        {
            public int compare(Entry<String, Integer> entry1, Entry<String, Integer> entry2)
            {
                int comp = entry1.getValue() - entry2.getValue();
                return -comp;
            }
        };

        
        Hashtable<String, Integer> countedKeyBag = createCountedKeyBag(itemTupleList, keyIndex);
        List<Entry<String, Integer>> entryList = new ArrayList<Entry<String, Integer>>(countedKeyBag.entrySet());
        Collections.sort(entryList, descendingCountComparator);
        
        List<String> topOrderedUniqueItemList = new ArrayList<String>();
        for (Entry<String, Integer> entry : entryList)
        {
            topOrderedUniqueItemList.add(entry.getKey());
        }

        return topOrderedUniqueItemList;
    }
    
        public static void L1TEST_extractTopOrderedUniqueItems()
        {
            List<String[]> itemTupleList = new ArrayList<String[]>();

            itemTupleList.add(new String[] {"baz", "alpha"});
            itemTupleList.add(new String[] {"bar", "bravo"});
            itemTupleList.add(new String[] {"bar", "charlie"});
            itemTupleList.add(new String[] {"foo", "charlie"});
            itemTupleList.add(new String[] {"foo", "charlie"});
            itemTupleList.add(new String[] {"foo", "delta"});

            List<String> topOrderedUniqueItemList = extractTopOrderedUniqueItems(itemTupleList, 0);
            assert topOrderedUniqueItemList.size() == 3;
            assert topOrderedUniqueItemList.get(0).equals("foo");
            assert topOrderedUniqueItemList.get(1).equals("bar");
            assert topOrderedUniqueItemList.get(2).equals("baz");

            topOrderedUniqueItemList = extractTopOrderedUniqueItems(itemTupleList, 1);
            assert topOrderedUniqueItemList.size() == 4;
            assert topOrderedUniqueItemList.get(0).equals("charlie");
        }

    // ---------------------------------------- PageFilter createCountedKeyBag
    
    private static Hashtable<String, Integer> createCountedKeyBag(List<String[]> itemTupleList, int keyIndex)
    {
        Hashtable<String, Integer> countedKeyBag = new Hashtable<String, Integer>();
        for (String[] itemTuple : itemTupleList)
        {
            String key = itemTuple[keyIndex];
            Integer count = countedKeyBag.get(key);
            count = (count == null) ? 1 : count + 1;
            countedKeyBag.put(key, count);
        }
        return countedKeyBag;
    }
    
        public static void L1TEST_createCountedKeyBag()
        {

            List<String[]> itemTupleList = new ArrayList<String[]>();
            assert createCountedKeyBag(itemTupleList, 0) != null;

            String[] itemTuple1 = new String[] {"foo", "bar"};
            itemTupleList.add(itemTuple1);          
            assert createCountedKeyBag(itemTupleList, 0).get("foo") == 1;
            assert createCountedKeyBag(itemTupleList, 0).get("bar") == null;
            assert createCountedKeyBag(itemTupleList, 1).get("foo") == null;
            assert createCountedKeyBag(itemTupleList, 1).get("bar") == 1;
            
            itemTupleList.add(itemTuple1);          
            assert createCountedKeyBag(itemTupleList, 0).get("foo") == 2;
            assert createCountedKeyBag(itemTupleList, 0).get("bar") == null;
            assert createCountedKeyBag(itemTupleList, 1).get("foo") == null;
            assert createCountedKeyBag(itemTupleList, 1).get("bar") == 2;
        }


    // ---------------------------------------- PageFilter addPageAlias
    /**
     * Add an alias for a particular page data, e.g. "MIG 15" for the
     * more common "MiG-15".  A smarter system would be able to handle
     * this kind of thing automatically, but we're not there yet.
     */
    private static void addPageAlias(Hashtable<String, PageData> pageDataBag, String sourceTitle, String aliasTitle)
    {
        PageData pageData = pageDataBag.get(sourceTitle.toLowerCase());
        if (pageData != null)
        {
            PageData pageDataAlias = new PageData(pageData, aliasTitle);
            pageDataBag.put(aliasTitle.toLowerCase(), pageDataAlias);
        }
        else
        {
            System.err.printf("### unknown title for alias: %s\n", sourceTitle);
        }   
    }

}
