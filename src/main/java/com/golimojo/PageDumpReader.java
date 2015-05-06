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
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;

import com.golimojo.QdmlParser.QdmlFragment;
import com.golimojo.TextFragment.FragmentType;

public class PageDumpReader
{

    // ---------------------------------------- PageDumpReader main
    
    /**
     * @param args
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception
    {
        String pathToSmallPageDumpFile = "temp/truncated-enwiki-pages-articles.xml";
        String pathToLargePageDumpFile = "temp/enwiki-pages-articles.xml";
//      Object[] results = readPageDump(pathToSmallPageDumpFile);
//      Object[] results = readPageDump(pathToLargePageDumpFile);
//      Hashtable<String, PageData> pageDataBag = (Hashtable<String, PageData>)results[0];
//      Hashtable<String, WordData> wordDataBag = (Hashtable<String, WordData>)results[1];

//      String pathToPageDumpFile = pathToSmallPageDumpFile;
        String pathToPageDumpFile = pathToLargePageDumpFile;
        Hashtable<String, PageData> pageDataBag = readPageDumpForPageData(pathToPageDumpFile);
        dumpPageData(pageDataBag, "temp/page-data-dump.txt", 100 * 1000);
        dumpPageData(pageDataBag, "temp/page-data-dump-large.txt", 2 * 1000 * 1000);

        pageDataBag = null;
        
        Hashtable<String, WordData> wordDataBag = readPageDumpForWordData(pathToPageDumpFile);
        dumpWordData(wordDataBag, "temp/word-data-dump.txt");

        System.out.println("Done.");
    }

    // ---------------------------------------- PageDumpReader readPageDump

    public static Object[] readPageDump(String pathToPageDumpFile) throws IOException
    {
        Hashtable<String, PageData> pageDataBag = null;
        QdmlParser parser = new QdmlParser();
        BufferedReader reader = new BufferedReader(new FileReader(pathToPageDumpFile));
        try
        {
            pageDataBag = readTitlesFromPageDump(parser, reader);
        }
        finally
        {
            reader.close();
        }

        parser = new QdmlParser();
        reader = new BufferedReader(new FileReader(pathToPageDumpFile));
        Hashtable <String, WordData> wordDataBag = new Hashtable <String, WordData>();
        try
        {
            readPageRefs(parser, reader, pageDataBag, wordDataBag);
        }
        finally
        {
            reader.close();
        }
        
        return new Object[] {pageDataBag, wordDataBag};
    }

    // ---------------------------------------- PageDumpReader readPageDump

    public static Hashtable<String, PageData> readPageDumpForPageData(String pathToPageDumpFile) 
        throws IOException
    {
        Hashtable<String, PageData> pageDataBag = null;
        QdmlParser parser = new QdmlParser();
        BufferedReader reader = new BufferedReader(new FileReader(pathToPageDumpFile));
        try
        {
            pageDataBag = readTitlesFromPageDump(parser, reader);
        }
        finally
        {
            reader.close();
        }

        parser = new QdmlParser();
        reader = new BufferedReader(new FileReader(pathToPageDumpFile));
        try
        {
            readPageRefs(parser, reader, pageDataBag, null);
        }
        finally
        {
            reader.close();
        }

        return pageDataBag;
    }

    // ---------------------------------------- PageDumpReader readPageDumpForWordData

    public static Hashtable <String, WordData> readPageDumpForWordData(String pathToPageDumpFile) 
        throws IOException
    {
        QdmlParser parser = new QdmlParser();
        BufferedReader reader = new BufferedReader(new FileReader(pathToPageDumpFile));
        Hashtable <String, WordData> wordDataBag = new Hashtable <String, WordData>();
        try
        {
            readPageRefs(parser, reader, null, wordDataBag);
        }
        finally
        {
            reader.close();
        }
        
        return wordDataBag;
    }

    // ---------------------------------------- PageDumpReader readTitlesFromPageDump
    
//  ;;;;;;; private static int maxCount = 200 * 1000;

    private static Hashtable<String, PageData> readTitlesFromPageDump(QdmlParser parser, Reader reader) 
        throws IOException
    {
        int counter = 0;
        Hashtable<String, PageData> pageDataBag = new Hashtable<String, PageData>();
        while (true)
        {
            counter++;
            if ((counter) % 1000 == 0) System.out.println("### " + counter);
//          ;;;;if (counter > maxCount) break;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
            String title = readEnclosedText(parser, reader, "title");
            if (title == null) break;
//          String text = readEnclosedText(parser, reader, "text");
//          if (text == null) break;
//          if (text.startsWith("#REDIRECT")) continue;     // Try skipping redirected pages
            PageData pageData = new PageData(title);
            pageDataBag.put(title, pageData);
        }

        return pageDataBag;
    }

    // ---------------------------------------- PageDumpReader readPageRefs

    private static void readPageRefs(QdmlParser parser, Reader reader, Hashtable<String, PageData> pageDataBag, Hashtable<String, WordData> wordDataBag)
        throws IOException
    {
        int counter = 0;
        while (true)
        {
            counter++;
//          ;;;;if (counter > maxCount) break;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
            if ((counter) % 1000 == 0) System.out.println("*** " + counter);
            String text = readEnclosedText(parser, reader, "text");
            if (text == null) break;
            if (pageDataBag != null) recordPageReferences(pageDataBag, text);
            ;;if (counter % 15 != 0) continue;
            if (wordDataBag != null) recordWordReferences(wordDataBag, text);
        }
    }

    // ---------------------------------------- PageDumpReader readEnclosedText

    private static String readEnclosedText(QdmlParser parser, Reader reader, String tagName) 
        throws IOException
    {
        QdmlFragment fragment = readToStartTag(parser, reader, tagName);
        if (fragment == null) return null;
        
        StringBuffer sbText = new StringBuffer();
        while (true)
        {
            fragment = readFragment(parser, reader);
            if (fragment == null) return null;
            if (fragment.isEndTagCs(tagName)) break;
            if (fragment.isTextNode())
            {
                sbText.append(fragment.toString());
            }
        }
        
        return sbText.toString();
    }

    // ---------------------------------------- PageDumpReader readToStartTag
    
    private static QdmlFragment readToStartTag(QdmlParser parser, Reader reader, String tagName)
        throws IOException
    {
        while (true)
        {
            QdmlFragment fragment = readFragment(parser, reader);
            if (fragment == null) return null;
            if (fragment.isStartTagCs(tagName)) return fragment;
        }
    }

    // ---------------------------------------- PageDumpReader readFragment
    
    private static QdmlFragment readFragment(QdmlParser parser, Reader reader) throws IOException
    {
        while (true)
        {
            int chInt = reader.read();
            QdmlFragment fragment = parser.parseCharacter(chInt);
            if (fragment != null) return fragment;
            if (chInt == -1) return null;
        }
    }

    // ---------------------------------------- PageDumpReader recordPageReferences

    private static void recordPageReferences(Hashtable<String, PageData> pageDataBag, String text)
    {
        String[] fragments = text.split("(\\[\\[)|(\\]\\])");
        for (int i = 0; i < fragments.length; i++)
        {
            if (i % 2 == 1)
            {
                String pageTitle = fragments[i];
                if (pageTitle.length() < 100)
                {
                    PageData pageData = pageDataBag.get(pageTitle);
                    if (pageData != null)
                    {
                        pageData.incrementReferenceCount();                     
                    }
                }
            }
        }
    }

    // ---------------------------------------- PageDumpReader xxxxx
    
    private static void recordWordReferences(Hashtable<String, WordData> wordDataBag, String text)
    {
        text = text.replaceAll("\\&.*?\\;", "");
        
        List<TextFragment> fragmentList = TextFragment.splitTextIntoFragments(text);
        for (int fragmentIndex = 0; fragmentIndex < fragmentList.size(); fragmentIndex++)
        {
//          if (fragmentIndex % 10000 != 0) continue;   // not enough memory to do every one
            TextFragment fragment = fragmentList.get(fragmentIndex);
            if (fragment.getType() == FragmentType.Word)
            {
                String fragmentText = fragment.getText();
                if (!fragmentText.equals(fragmentText.toLowerCase())) continue;
                String word = fragmentText;

                boolean hasLetter = false;
                for (int i = 0; i < word.length(); i++)
                {
                    char ch = word.charAt(i);
                    if (('A' <= ch && ch <= 'Z') || ('a' <= ch && ch <= 'z'))
                    {
                        hasLetter = true; 
                        break;
                    }
                }
                if (!hasLetter) continue;
                
                if (word.indexOf("''") >= 0) continue;
                
                
                WordData wordData = wordDataBag.get(word);
                if (wordData == null)
                {
                    wordData = new WordData(word);
                    wordDataBag.put(word, wordData);
                }
                wordData.incrementReferenceCount();
            }
        }
    }

    // ---------------------------------------- PageDumpReader dumpPageData
    
    private static void dumpPageData(Hashtable<String, PageData> pageDataBag, String pathToDumpFile, int minThresholdCount) throws IOException
    {
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(pathToDumpFile)));
        
        int definedCount = 0;
        int referencedCount = 0;
        List<PageData> values = new ArrayList<PageData>(pageDataBag.values());
        values = sortAndPare(values, minThresholdCount);
        for (PageData pageData : values)
        {
            writer.println("*** " + pageData.getReferenceCount() + "\t" + pageData.title + " ");
            definedCount++;
            if (pageData.getReferenceCount() > 0)
            {
                referencedCount++;
            }
        }
        writer.println("### defined count    = " + definedCount);
        writer.println("... referenced count = " + referencedCount);
        System.out.println("### defined count    = " + definedCount);
        System.out.println("... referenced count = " + referencedCount);
        
        writer.close();
    }

    // ---------------------------------------- PageDumpReader sortAndPare

    private static List<PageData> sortAndPare(List<PageData> pageDataList, int minThresholdCount)
    {
        pageDataList = removeChaff(pageDataList);
        sort(pageDataList);
        if (pageDataList.size() < minThresholdCount) return pageDataList;
        
        int thresholdRefCount = pageDataList.get(minThresholdCount).getReferenceCount();
        List<PageData> paredPageDataList = new ArrayList<PageData>();
        for (PageData pageData : pageDataList)
        {
            if (pageData.getReferenceCount() < thresholdRefCount) break;
            paredPageDataList.add(pageData);
        }
        
        return paredPageDataList;
    }

    // ---------------------------------------- PageDumpReader xxxxx
    
    private static List<PageData> removeChaff(List<PageData> pageDataList)
    {
        List<PageData> filteredPageDataList = new ArrayList<PageData>();
        Pattern pattern = Pattern.compile("^[0-9]+$");
        for (PageData pageData : pageDataList)
        {
            if (pageData.title.startsWith("Category:")) continue;
            if (pageData.title.startsWith("Wikipedia:")) continue;
            if (pageData.title.startsWith("Template:")) continue;
            if (pageData.title.startsWith("WP:")) continue;
            if (pageData.title.startsWith("Image:")) continue;
            if (pattern.matcher(pageData.title).matches()) continue;
            filteredPageDataList.add(pageData);
        }
        
        return filteredPageDataList;
    }

    // ---------------------------------------- PageDumpReader sort
    
    private static void sort(Collection<PageData> pageDataList)
    {
        Comparator<PageData> refCountComparator = new Comparator<PageData>()
        {
            public int compare(PageData pd1, PageData pd2)
            {
                // Sort first by refcount descending.
                int refCountComp = -(pd1.getReferenceCount() - pd2.getReferenceCount());
                if (refCountComp != 0) return refCountComp;
                int titleComp = pd1.title.compareTo(pd2.title);
                return titleComp;
            }
        };

        Collections.sort((List<PageData>)pageDataList, refCountComparator);
    }

    // ---------------------------------------- PageDumpReader dumpWordData

    private static void dumpWordData(Hashtable<String, WordData> wordDataBag, String pathToDumpFile) throws IOException
    {

        Comparator<WordData> countComparator = new Comparator<WordData>()
        {
            public int compare(WordData wd1, WordData wd2)
            {
                // Sort first by refcount descending.
                int countComp = -(wd1.getCount() - wd2.getCount());
                return countComp;
            }
        };

        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(pathToDumpFile)));
        
        int distinctWordCount = 0;
        List<WordData> values = new ArrayList<WordData>(wordDataBag.values());
        Collections.sort(values, countComparator);
        for (WordData wordData : values)
        {
            writer.println("*** " + wordData.getCount() + "\t" + wordData.getWord() + " ");
            distinctWordCount++;
        }
        writer.println("### disinct count    = " + distinctWordCount);
        System.out.println("### disinct count    = " + distinctWordCount);
        
        writer.close();
    }

    // ---------------------------------------- class PageData
    
    private static class PageData
    {
        public final String title;
        private int myReferenceCount = 0;
        
        public PageData(String _title)
        {
            title = _title;
        }
        
        public int getReferenceCount()
        {
            return myReferenceCount;
        }
        
        public void setReferenceCount(int referenceCount)
        {
            myReferenceCount = referenceCount;
        }
        
        public void incrementReferenceCount()
        {
            myReferenceCount++;
        }
        
    }

    // ---------------------------------------- class WordData
    
    private static class WordData
    {
        private String _word;
        private int _count = 0;
        
        public WordData(String word)
        {
            _word = word;
        }
        
        public String getWord()
        {
            return _word;
        }
        
        public int getCount()
        {
            return _count;
        }
        
        public void incrementReferenceCount()
        {
            _count++;
        }

    
    
    }

}
