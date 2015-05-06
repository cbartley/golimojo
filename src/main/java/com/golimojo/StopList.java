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

public class StopList
{

    // ---------------------------------------- StopList instance variables
    
    private HashSet<String> _stopWordBag;

    // ---------------------------------------- StopList constructor
    
    public StopList(List<PageData> pageDataList)
    {
        List<String> stopWordList = identifyStopWords(pageDataList);
        _stopWordBag = new HashSet<String>(stopWordList);

        _stopWordBag.add("Sunday");
        _stopWordBag.add("Monday");
        _stopWordBag.add("Tuesday");
        _stopWordBag.add("Wednesday");
        _stopWordBag.add("Thursday");
        _stopWordBag.add("Friday");
        _stopWordBag.add("Saturday");

        _stopWordBag.add("January");
        _stopWordBag.add("February");
        _stopWordBag.add("March");
        _stopWordBag.add("April");
        _stopWordBag.add("May");
        _stopWordBag.add("June");
        _stopWordBag.add("July");
        _stopWordBag.add("August");
        _stopWordBag.add("September");
        _stopWordBag.add("October");
        _stopWordBag.add("November");
        _stopWordBag.add("December");
        
        _stopWordBag.add("Rhodes");
        _stopWordBag.add("I'll");
    }

    // ---------------------------------------- StopList hasWord
    
    public boolean hasWord(String word)
    {
        return _stopWordBag.contains(word);
    }

    // ---------------------------------------- StopList identifyStopWords
    
    private static List<String> identifyStopWords(List<PageData> pageDataList)
    {
        final int topCount = 200;
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
        
        // Now use our refined probabl first name list to generate a refined probable last name list.
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
        return stopWordList;
    }

    // ---------------------------------------- StopList extractPossibleNames
    
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

    // ---------------------------------------- StopList selectItemsUsingFilter
    
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

    // ---------------------------------------- StopList extractTopOrderedUniqueItems
    
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

    // ---------------------------------------- StopList createCountedKeyBag
    
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

}
