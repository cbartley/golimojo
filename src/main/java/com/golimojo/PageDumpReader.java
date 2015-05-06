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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.golimojo.QdmlParser.QdmlEndTagFragment;
import com.golimojo.QdmlParser.QdmlFragment;
import com.golimojo.QdmlParser.QdmlStartTagFragment;
import com.golimojo.QdmlParser.QdmlTagFragment;
import com.golimojo.QdmlParser.QdmlTextNodeFragment;

public class PageDumpReader
{

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
//      String pathToPageDumpFile = "temp/truncated-enwiki-pages-articles.xml";
//      String pathToPageDumpFile = "temp/enwiki-pages-articles.xml";
//      new PageDumpReader(pathToPageDumpFile);
        
        PageDumpReaderMkII("temp/truncated-enwiki-pages-articles.xml");
    }
    
    public static void PageDumpReaderMkII(String pathToPageDumpFile) throws Exception
    {
        int pageCount = 0;
        Hashtable<String, PageData> pageDataBag = new Hashtable<String, PageData>();
        boolean inTitle = false;
        boolean inText = false;
        StringBuffer sbTitle = new StringBuffer();
        StringBuffer sbText = new StringBuffer();
        
        QdmlParser qdmlParser = new QdmlParser();
        BufferedReader in = new BufferedReader(new FileReader(pathToPageDumpFile));
        while (true)
        {
            int chInt = in.read();
            QdmlFragment fragment = qdmlParser.parseCharacter((char)chInt);
            if (fragment != null) 
            {
                if (fragment instanceof QdmlStartTagFragment)
                {
                    String tagName = ((QdmlTagFragment)fragment).getTagName();
                    if (tagName.equals("TITLE")) 
                    {
                        inTitle = true;
                    }
                    if (tagName.equals("TEXT")) 
                    {
                        inText = true;
                    }
                }
                if (fragment instanceof QdmlEndTagFragment)
                {
                    String tagName = ((QdmlTagFragment)fragment).getTagName();
                    if (tagName.equals("TITLE")) 
                    {
                        inTitle = false;
                    }
                    if (tagName.equals("TEXT")) 
                    {
                        inText = false;
                    }
                    if (tagName.equals("PAGE"))
                    {
                        String title = sbTitle.toString();
                        String text = sbText.toString();
                        sbTitle.setLength(0);
                        sbText.setLength(0);
                        PageData pageData = pageDataBag.get(title);
                        if (pageData == null)
                        {
                            pageData = new PageData(title);
                            pageDataBag.put(title, pageData);
                        }
                        pageData.setIsDefined(true);
                        recordPageReferences(pageDataBag, text);
                        
                        pageCount++;
                        if (pageCount % 1000 == 0)
                        {
                            System.out.println("::: " + pageCount);
                        }
                    }
                }
                
                if (fragment instanceof QdmlTextNodeFragment)
                {
                    String text = ((QdmlTextNodeFragment)fragment).toString();
                    if (inTitle)
                    {
                        sbTitle.append(text);
                    }
                    if (inText)
                    {
                        sbText.append(text);
                    }
                }
            }
            
            if (chInt == -1) break;
        }
        
        dumpPageData(pageDataBag);
        System.out.println("Done.");
    }

    private static void dumpPageData(Hashtable<String, PageData> pageDataBag) throws IOException
    {
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("temp/page-data-dump.txt")));
        
        int definedCount = 0;
        int referencedCount = 0;
        for (PageData pageData : pageDataBag.values())
        {
            if (pageData.getIsDefined())
            {
                writer.println("*** " + pageData.getReferenceCount() + "\t" + pageData.title + " ");
                definedCount++;
            }
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

    // ---------------------------------------- xxxx 
    
    private static void recordPageReferences(Hashtable<String, PageData> pageDataBag, String text)
    {
        List<String> pageRefList = new ArrayList<String>();
        String[] fragments = text.split("(\\[\\[)|(\\]\\])");
        for (int i = 0; i < fragments.length; i++)
        {
            if (i % 2 == 1)
            {
                String pageTitle = fragments[i];
                if (pageTitle.length() < 100)
                {
                    PageData pageData = pageDataBag.get(pageTitle);
                    if (pageData == null)
                    {
//                      pageData = new PageData(pageTitle);
//                      pageDataBag.put(pageTitle, pageData);
                    }
                    if (pageData != null)
                    {
                        pageData.incrementReferenceCount();                     
                    }
                }
            }
        }
    }

    // ---------------------------------------- class PageData
    
    private static class PageData
    {
        public final String title;
        private boolean myIsDefined = false;
        private int myReferenceCount = 0;
        
        public PageData(String _title)
        {
            title = _title;
        }
        
        public boolean getIsDefined()
        {
            return myIsDefined;
        }
        
        public void setIsDefined(boolean isDefined)
        {
            myIsDefined = isDefined;
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

}
