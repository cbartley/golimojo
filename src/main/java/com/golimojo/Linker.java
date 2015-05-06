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
import java.util.*;
import java.util.regex.*;

import com.golimojo.QdmlParser.QdmlEndTagFragment;
import com.golimojo.QdmlParser.QdmlFragment;
import com.golimojo.QdmlParser.QdmlStartTagFragment;
import com.golimojo.QdmlParser.QdmlElementFragment;
import com.golimojo.QdmlParser.QdmlTextNodeFragment;

public class Linker 
{
    private Dictionary<String, String> myArticleTitleBag;
    
    public Linker(Ranker ranker, String articleTitlesFilePath) throws Exception
    {
        myArticleTitleBag = readArticleTitles(ranker, articleTitlesFilePath);
    }
    
    public static String createLinkUrl(String linkMatch)
    {
        String articleTitle = linkMatch.replaceAll("[ \t\r\n]+", "_");
        String url = "http://en.wikipedia.org/wiki/" + articleTitle;
        return url;
    }

    public List<QdmlFragment> findLinks(List<QdmlFragment> fragmentList, String url)
    {
        // Insert BASE element.
        for (int i = 0; i < fragmentList.size(); i++)
        {
            QdmlFragment fragment = fragmentList.get(i);
            if (fragment instanceof QdmlStartTagFragment)
            {
                QdmlStartTagFragment startTag = (QdmlStartTagFragment)fragment;
                if (startTag.getTagName().equals("HEAD"))
                {
                    QdmlElementFragment elem = new QdmlElementFragment("<base href='" + url + "'>");
                    fragmentList.add(i + 1, elem);
                    break;
                }
            }
        }
        
        return findLinks(fragmentList);
    }

    public List<QdmlFragment> findLinks(List<QdmlFragment> fragmentList)
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
            
            inBody = insideElement(fragment, "BODY", inBody);
            inStyle = insideElement(fragment, "STYLE", inStyle);
            inScript = insideElement(fragment, "SCRIPT", inScript);
            inAnchor = insideElement(fragment, "A", inAnchor);
                
            if (!isTextNode || !inBody || inStyle || inScript || inAnchor)
            {
                fragmentOutList.add(fragment);
            }
            else
            {
                QdmlTextNodeFragment textNode = (QdmlTextNodeFragment)fragment;
                findLinks(textNode, fragmentOutList);
            }
        }

        return fragmentOutList;
    }

    private boolean insideElement(QdmlFragment fragment, String tagName, boolean insideElementNow)
    {
        if (fragment instanceof QdmlStartTagFragment)
        {
            QdmlStartTagFragment startTag = (QdmlStartTagFragment)fragment;
            if (startTag.getTagName().equals(tagName)) return true;
        }
        if (fragment instanceof QdmlEndTagFragment)
        {
            QdmlEndTagFragment endTag = (QdmlEndTagFragment)fragment;
            if (endTag.getTagName().equals(tagName)) return false;
        }
        return insideElementNow;
    }
    
    private void findLinks(QdmlTextNodeFragment textNode, List<QdmlFragment> fragmentOutList)
    {
        String text = textNode.toString();
        String[] words = extractWords(text);
        List<String> phrases = phrases(words, 2, 4);
        List<String> links = links(myArticleTitleBag, phrases);
        String patternText = createPatternText(links);
        if (patternText == null)
        {
            fragmentOutList.add(textNode);
            return;
        }
        
        Pattern p = Pattern.compile(patternText);
        Matcher m = p.matcher(text);
        
        if (!m.find())
        {
            fragmentOutList.add(textNode);
            return;
        }

        do  
        {
            String url = createLinkUrl(m.group());
            String replacement = "<a style='color:red;font-weight:bold' href='" + url + "'>" + m.group() + "</a>";
            StringBuffer sb = new StringBuffer();
            m.appendReplacement(sb, replacement);
            QdmlTextNodeFragment subNode = new QdmlTextNodeFragment(sb.toString());
            fragmentOutList.add(subNode);
        } while (m.find());
        
        StringBuffer sb = new StringBuffer();
        m.appendTail(sb);
        QdmlTextNodeFragment subNode = new QdmlTextNodeFragment(sb.toString());
        fragmentOutList.add(subNode);
    }
    
    public String findLinks(String text, String htmlText)
    {
        String[] words = extractWords(text);
        List<String> phrases = phrases(words, 2, 4);
        List<String> links = links(myArticleTitleBag, phrases);
        String patternText = createPatternText(links);
//      htmlText = htmlText.replaceAll(patternText, "<a href='$1'>$1</a>");
        
        Pattern p = Pattern.compile(patternText);
        Matcher m = p.matcher(htmlText);
        StringBuffer sbHtmlText = new StringBuffer();
        while (m.find()) 
        {
            String url = createLinkUrl(m.group());
            String replacement = "<a style='color:red;font-weight:bold' href='" + url + "'>" + m.group() + "</a>";
            m.appendReplacement(sbHtmlText, replacement);
        }
        m.appendTail(sbHtmlText);
        return sbHtmlText.toString();
    }

    private static Dictionary<String, String> readArticleTitles(Ranker ranker, String articleTitlesFilePath) throws Exception
    {
        BufferedReader in = new BufferedReader(new FileReader(articleTitlesFilePath));

        Hashtable<String, String> articleTitleBag = new Hashtable<String, String>();

        try
        {
            long startTimeMs = new Date().getTime();

            int articleTitleCount = 0;
            while (true)
            {
                    String articleTitle = in.readLine();
                    if (articleTitle == null) break;
                    //if (countWords(articleTitle) < 2) continue;
                    if (!justLetters(articleTitle)) continue;
                    articleTitle = translateArticleTitle(articleTitle);
                    double rank = ranker.rankPhrase(articleTitle);
                    if (rank > 0.0)
                    {
//                      System.out.println("*** " + articleTitle);
//                      System.out.println("... " + rank);
                    }
                    if (rank > 0.0) continue;
                    articleTitleCount++;
                    articleTitleBag.put(articleTitle, articleTitle);
            }
            long endTimeMs = new Date().getTime();
            long elapsedTimeMs = endTimeMs - startTimeMs;

            System.out.println(articleTitleCount);
            System.out.printf("%.2f", new Object[] {elapsedTimeMs / 1000.0});
        }
        finally
        {
            in.close();
        }
        
        return articleTitleBag;

    }

    private static int countWords(String articleTitle)
    {
        int wordCount = 0;
        for (int i = 0; i < articleTitle.length(); i++)
        {
            char ch = articleTitle.charAt(i);
            if (ch == '_')
            {
                wordCount++;
            }
        }
        return wordCount + 1;
    }
    
    private static boolean justLetters(String articleTitle)
    {
        for (int i = 0; i < articleTitle.length(); i++)
        {
            char ch = articleTitle.charAt(i);
            if (ch != '_' && !Character.isLetter(ch)) return false; 
        }
        return true;
    }
    
    private static String translateArticleTitle(String articleTitle)
    {
//      articleTitle = articleTitle.toLowerCase();
        articleTitle = articleTitle.replace('_', ' ');
        return articleTitle;
    }
    
    private static String[] extractWords(String text)
    {
        Pattern splitPat = Pattern.compile("[^A-Za-z0-9]+");
        String[] words = splitPat.split(text);
//      for (int i = 0; i < words.length; i++)
//      {
//          words[i] = words[i].toLowerCase();
//      }
        return words;
    }
    
    private static boolean isStrongWord(String word)
    {
        if (word.length() == 0) return false;
        
        char ch = word.charAt(0);
        if (Character.isLetter(ch) && Character.isUpperCase(ch)) return true;
        if (Character.isDigit(ch)) return true;
        
        return false;       
    }
    
    private static String phraseFromPosition(String[] words, int index, int wordCount)
    {
        if (index + wordCount > words.length) return null;
        if (!isStrongWord(words[index])) return null;
        if (!isStrongWord(words[index + wordCount - 1])) return null;

        StringBuffer sb = new StringBuffer();
        for (int i = index; i < index + wordCount; i++)
        {
            if (i >= words.length) return null;
            if (i > index) sb.append(" ");
            sb.append(words[i]);
        }
        return sb.toString();
    }
    
    private static List<String> phrasesFromPosition(String[] words, int index, int minWords, int maxWords)
    {
        List<String> phrases = new ArrayList<String>();
        for (int i = maxWords; i >= minWords; i--)
        {
            String phrase = phraseFromPosition(words, index, i);
            if (phrase != null)
            {
                phrases.add(phrase);
            }
        }
        return phrases;
    }
    
    private static List<String> phrases(String[] words, int minWords, int maxWords)
    {
        List<String> phrases = new ArrayList<String>();
        for (int i = 0; i < words.length; i++)
        {
            List<String> phrasesFromPosition = phrasesFromPosition(words, i, minWords, maxWords);
            phrases.addAll(phrasesFromPosition);
        }
        return phrases;
    }
    
    private static List<String> links(Dictionary<String, String> articleTitleBag, List<String> phrases)
    {
        List<String> links = new ArrayList<String>();
        for (String phrase : phrases)
        {
            if (articleTitleBag.get(phrase/*.toLowerCase()*/) != null)
            {
                links.add(phrase);
            }
        }
        return links;
    }

    private static String join(String[] strings, String separator)
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < strings.length; i++)
        {
            sb.append(strings[i]);
            if (i < strings.length - 1)
            {
                sb.append(separator);
            }
        }
        return sb.toString();
    }
    
    private static String createLinkPhrasePatternText(String linkPhrase)
    {
        return linkPhrase.replace(" ", "[ \t\r\n]+");
    }

    private static String createPatternText(List<String> linkPhrases)
    {
        if (linkPhrases.size() == 0) return null;
        String[] patterns = (String[])linkPhrases.toArray(new String[] {});
        for (int i = 0; i < patterns.length; i++)
        {
            patterns[i] = createLinkPhrasePatternText(patterns[i]);
        }
        String patternText = "((" + join(patterns, ")|(") + "))";
        return patternText;
    }

    
    
/*  
    private static void readLinksFromHttpFile(Dictionary<String, String> articleTitleBag, String urlString) throws Exception
    {
        String text = readAndParseHttpFile(urlString);
        String[] words = extractWords(text);
        List<String> phrases = phrases(words, 2, 4);
        List<String> links = links(articleTitleBag, phrases);
        System.out.println("-----");
        for (String link : links)
        {
            System.out.println(link);
        }
    }
*/

}
