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
import java.util.List;

/* ------------------------------------------------------------ */
/* ---------------------- class PageData ---------------------- */
/* ------------------------------------------------------------ */

public class PageData
{
    // ---------------------------------------- PageData instance variables

    private String _title;
    private int _wordCount;
    private int _refCount;
    private int _refRank;

    // ---------------------------------------- PageData constructor

    public PageData(String title, int refCount)
    {
        _title = title;
        _wordCount = countWords(title);
        _refCount = refCount;
        _refRank = -1;
    }

    // ---------------------------------------- PageData getTitle

    public String getTitle()
    {
        return _title;
    }

    // ---------------------------------------- PageData getWordCount

    public int getWordCount()
    {
        return _wordCount;
    }

    // ---------------------------------------- PageData getRefCount
    
    public int getRefCount()
    {
        return _refCount;
    }

    // ---------------------------------------- PageData _getRefRank

    public int getRefRank()
    {
        return _refRank;
    }

    // ---------------------------------------- PageData setRefRank

    public void setRefRank(int rank)
    {
        _refRank = rank;
    }

    // ---------------------------------------- PageData readPageTitles

    public static List <PageData> readPageTitles(String articleTitlesFilePath) throws Exception
    {
        // Read the page data records into an array of page data objects.
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
        
        // Compute synthesized page attributes and return the results.
        computeAndApplyRankings(pageDataList);
        return pageDataList;
    
    }

    // ---------------------------------------- PageData computeAndApplyRankings

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

    // ---------------------------------------- PageData countWords

    private static int countWords(String phrase)
    {
        int wordCount = 0;
        List<TextFragment> fragmentList = TextFragment.splitTextIntoFragments(phrase);
        for (TextFragment fragment : fragmentList)
        {
            if (fragment.getType() == TextFragment.FragmentType.Word)
            {
                wordCount++;
            }
        }
        return wordCount;
    }
    
}
