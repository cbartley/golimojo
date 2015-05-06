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
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;

import com.golimojo.QdmlParser.QdmlEndTagFragment;
import com.golimojo.QdmlParser.QdmlFragment;
import com.golimojo.QdmlParser.QdmlStartTagFragment;
import com.golimojo.QdmlParser.QdmlElementFragment;
import com.golimojo.QdmlParser.QdmlTextNodeFragment;

public class Linker 
{
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Test
    {
        
    }
    
    static class Tester 
    {
        public Tester(Class metaClass)
        {
            List<Method> testMethodList = getTestMethods(metaClass);
            callMethods(testMethodList);
        }

        private static List<Method> getTestMethods(Class metaClass)
        {
            List<Method> methodList = new ArrayList();
            Method[] methods = metaClass.getDeclaredMethods();
            for (Method method : methods)
            {
                if (method.isAnnotationPresent(Test.class))
                {
                    methodList.add(method);
                }
            }
            return methodList;
        }
        
        private static void callMethods(List<Method> methodList)
        {
            Object[] args = new Object[] {};
            for (Method method : methodList)
            {
                try
                {
                    method.invoke(null, args);
                }
                catch (Throwable t)
                {
                    t.printStackTrace();
                    break;
                }
            }
        }
    }

    Tester t = new Tester(Linker.class);
    
    // ---------------------------------------- Linker xxxx

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
    
    // ---------------------------------------- Linker xxxx

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
    
    // ---------------------------------------- Linker xxxx

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
    
    // ---------------------------------------- Linker xxxx
    
    private void addLinksToTextNode(QdmlTextNodeFragment textNode, List<QdmlFragment> fragmentOutList)
    {
        String text = textNode.toString();
        String linkedText = addLinks((Hashtable<String, String>)myArticleTitleBag, text);
        fragmentOutList.add(new QdmlTextNodeFragment(linkedText));
    }

    // ---------------------------------------- Linker xxxx
    
    private static String addLinks(Hashtable<String, String> pageBag, String text)
    {
        List<TextFragment> fragmentList = splitTextIntoFragments(text);
        List<TextFragment> receiverList = new ArrayList<TextFragment>();
        addLinks(pageBag, fragmentList, receiverList);
        return TextFragment.join(receiverList);
    }

    // ---------------------------------------- Linker xxxx
    
    private static void addLinks(Hashtable<String, String> pageBag, List<TextFragment> fragmentList, List<TextFragment> receiverList)
    {
        int index = 0;
        while (index < fragmentList.size())
        {
            int phraseFragmentCount = findLongestPhraseAtPosition(pageBag, fragmentList, index);

            // If we matched a phrase, copy and link it.
            if (phraseFragmentCount > 0)
            {
                copyLinkedPhrase(fragmentList, index, phraseFragmentCount, receiverList);
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

    // ---------------------------------------- Linker xxxxx

    private static void copyLinkedPhrase(List<TextFragment> fragmentList, int index, int fragmentCount, List<TextFragment> receiverList)
    {
        String phraseText = assemblePhrase(fragmentList, index, fragmentCount);
        String url = buildPhraseUrl(phraseText);
        String startTagText = "<a style='font-weight:bold; color:red' href='" + url + "'>";
        String endTagText = "</a>";
        TextFragment anchorStartTagFragment = new TextFragment(FragmentType.Other, startTagText);
        TextFragment anchorEndTagFragment = new TextFragment(FragmentType.Other, endTagText);
        
        receiverList.add(anchorStartTagFragment);
        receiverList.addAll(fragmentList.subList(index, index + fragmentCount));
        receiverList.add(anchorEndTagFragment);
    }

    // ---------------------------------------- Linker buildPhraseUrl
    
    private static String buildPhraseUrl(String phraseText)
    {
        try
        {
            String urlPrefix = "http://en.wikipedia.org/wiki/";
            String encodedPageName = URLEncoder.encode(phraseText.replace(" ", "_"), "UTF-8");
            return urlPrefix + encodedPageName;
        } 
        catch (UnsupportedEncodingException e)
        {
            System.err.println("Could not encode '" + phraseText + "'!");
            return "about:blank";
        }
    }

    // ---------------------------------------- Linker xxxx

    public static @Test void testFindLongestPhraseAtPosition()
    {
        Hashtable<String, String> pageBag = new Hashtable<String, String>();

        List<TextFragment> fragmentList = splitTextIntoFragments("one two three four five");
        assert findLongestPhraseAtPosition(pageBag, fragmentList, 0) == 0;

        pageBag.put("one", "one");
        assert findLongestPhraseAtPosition(pageBag, fragmentList, 0) == 1;
        assert findLongestPhraseAtPosition(pageBag, fragmentList, 1) == 0;
        
        pageBag.put("two", "two");
        assert findLongestPhraseAtPosition(pageBag, fragmentList, 0) == 1;
        assert findLongestPhraseAtPosition(pageBag, fragmentList, 1) == 0;
        assert findLongestPhraseAtPosition(pageBag, fragmentList, 2) == 1;
        assert findLongestPhraseAtPosition(pageBag, fragmentList, 3) == 0;
        
        pageBag.put("three four", "three four");
        assert findLongestPhraseAtPosition(pageBag, fragmentList, 4) == 3;
        
        pageBag.put("three", "three");
        assert findLongestPhraseAtPosition(pageBag, fragmentList, 4) == 3;
        
        pageBag.put("three four five", "three four five");
        assert findLongestPhraseAtPosition(pageBag, fragmentList, 4) == 5;
    }

    private static int findLongestPhraseAtPosition(Hashtable<String, String> pageBag, List<TextFragment> fragmentList, int index)
    {
        final int maxWordCount = 4;
        int maxPhraseLength = findMaxPhraseLengthAtPosition(fragmentList, index, maxWordCount);

        for (int i = maxPhraseLength; i > 0; i -= 2)
        {
            String phrase = assemblePhrase(fragmentList, index, i);
            if (pageBag.get(phrase) != null) return i;
        }

        return 0;
    }

    // ---------------------------------------- Linker xxxx

        public static @Test void testFindMaxPhraseLengthAtPosition()
        {
            // Some of these results are surprising if you forget to account  for whitespace 
            // fragments.  Also remember that a two word phrase will have *3* fragments.
            assert findMaxPhraseLengthAtPosition(splitTextIntoFragments("foo"), 0, 0) == 0;
            assert findMaxPhraseLengthAtPosition(splitTextIntoFragments("foo"), 0, 0) == 0;
            assert findMaxPhraseLengthAtPosition(splitTextIntoFragments("foo"), 0, 1) == 1;
            assert findMaxPhraseLengthAtPosition(splitTextIntoFragments("foo"), 0, 2) == 1;
            assert findMaxPhraseLengthAtPosition(splitTextIntoFragments(" foo"), 0, 1) == 0;
            assert findMaxPhraseLengthAtPosition(splitTextIntoFragments("foo bar"), 0, 1) == 1;
            assert findMaxPhraseLengthAtPosition(splitTextIntoFragments("foo bar"), 0, 2) == 3;
            assert findMaxPhraseLengthAtPosition(splitTextIntoFragments("foo bar baz"), 1, 1) == 0;
            assert findMaxPhraseLengthAtPosition(splitTextIntoFragments("foo bar baz"), 1, 2) == 0;
            assert findMaxPhraseLengthAtPosition(splitTextIntoFragments("foo bar baz"), 2, 1) == 1;
            assert findMaxPhraseLengthAtPosition(splitTextIntoFragments("foo bar baz"), 2, 2) == 3;

            assert findMaxPhraseLengthAtPosition(splitTextIntoFragments("foo."), 0, 2) == 1;
            assert findMaxPhraseLengthAtPosition(splitTextIntoFragments("foo bar."), 0, 2) == 3;
        }

    private static int findMaxPhraseLengthAtPosition(List<TextFragment> fragmentList, int index, int maxWordCount)
    {
        // Figure out how many fragments might constitute our longest possible phrase.  Note
        // that "adjacent" word fragments will actually be separated by whitespace fragments.
        int remainingFragmentCount = fragmentList.size() - index;
        int maxLookAheadCount = 2 * maxWordCount - 1;
        int lookAheadCount = Math.min(remainingFragmentCount, maxLookAheadCount);
        
        // We might be done already.
        if (lookAheadCount < 1) return 0;
        
        // Is the first fragment a word?
        if (fragmentList.get(index).getType() != FragmentType.Word) return 0;
        
        // Scan the remaining fragments to determine the longest actual phrase.
        for (int i = 2; i < lookAheadCount; i += 2)
        {
            if (fragmentList.get(i - 1).getType() != FragmentType.Whitespace) return i - 1;
            if (fragmentList.get(i).getType() != FragmentType.Word) return i - 1;
        }
        
        // We matched the longest allowable phrase.
        return lookAheadCount - ((lookAheadCount + 1) % 2); // yuck
    }

    // ---------------------------------------- Linker xxxx
    
    public static @Test void testAssemblePhrase()
    {
        assert assemblePhrase(splitTextIntoFragments(""), 0, 0).equals("");
        assert assemblePhrase(splitTextIntoFragments(" foo"), 0, 0).equals("");
        assert assemblePhrase(splitTextIntoFragments(" foo"), 0, 1).equals("");
        assert assemblePhrase(splitTextIntoFragments(" foo"), 0, 2).equals("foo");
        assert assemblePhrase(splitTextIntoFragments("foo "), 0, 1).equals("foo");
        assert assemblePhrase(splitTextIntoFragments("foo "), 0, 2).equals("foo");
        assert assemblePhrase(splitTextIntoFragments("foo\t\tbar"), 0, 3).equals("foo bar");

        assert assemblePhrase(splitTextIntoFragments("foo bar baz"), 1, 0).equals("");
        assert assemblePhrase(splitTextIntoFragments("foo bar baz"), 1, 1).equals("");
        assert assemblePhrase(splitTextIntoFragments("foo bar baz"), 1, 2).equals("bar");
        assert assemblePhrase(splitTextIntoFragments("foo bar baz"), 1, 3).equals("bar");
        assert assemblePhrase(splitTextIntoFragments("foo bar baz"), 1, 4).equals("bar baz");

        assert assemblePhrase(splitTextIntoFragments("foo bar baz"), 2, 0).equals("");
        assert assemblePhrase(splitTextIntoFragments("foo bar baz"), 2, 1).equals("bar");
        assert assemblePhrase(splitTextIntoFragments("foo bar baz"), 2, 2).equals("bar");
        assert assemblePhrase(splitTextIntoFragments("foo bar baz"), 2, 3).equals("bar baz");
    }

    private static String assemblePhrase(List<TextFragment> fragmentList, int index, int fragmentCount)
    {
        StringBuffer sbPhrase = new StringBuffer();
        for (int i = 0; i < fragmentCount; i++)
        {
            TextFragment fragment = fragmentList.get(index + i);
            if (fragment.getType() == FragmentType.Whitespace)
            {
                sbPhrase.append(" ");
            }
            else
            {
                sbPhrase.append(fragment.getText());
            }
        }
        return sbPhrase.toString().trim();
    }

    // ---------------------------------------- Linker splitTextIntoFragments
    
        public static @Test void testSplitTextIntoFragments()
        {
            assert splitTextIntoFragments("").size() == 0;
            assert splitTextIntoFragments("foo").size() == 1;
            assert splitTextIntoFragments("foo bar").size() == 3;
            assert splitTextIntoFragments("foo;bar").size() == 3;
            assert splitTextIntoFragments("foo;;;bar").size() == 3;

            assert splitTextIntoFragments("foo bar").get(0).getText().equals("foo");
            assert splitTextIntoFragments("foo bar").get(1).getText().equals(" ");
            assert splitTextIntoFragments("foo bar").get(2).getText().equals("bar");
        }

    private static List<TextFragment> splitTextIntoFragments(String text)
    {   
        int fragmentStartIndex = 0;
        List<TextFragment> fragmentList = new ArrayList<TextFragment>();
        for (int i = 0; i < text.length(); i++)
        {
            FragmentType prevType = FragmentType.typeOfCharAtAnyIndex(text, i);
            FragmentType currType = FragmentType.typeOfCharAtAnyIndex(text, i + 1); // out-of-range ==> null

            if (prevType != currType)
            {
                String fragmentText = text.substring(fragmentStartIndex, i + 1);
                TextFragment textFragment = new TextFragment(prevType, fragmentText);
                fragmentList.add(textFragment);
                fragmentStartIndex = i + 1;
            }
        }
        return fragmentList;
    }

    // ---------------------------------------- class TextFragment
    
        public static @Test void testTextFragment()
        {
            TextFragment foo = new TextFragment(FragmentType.Other, "foo");
            TextFragment bar = new TextFragment(FragmentType.Other, "bar");
            
            assert TextFragment.join(new ArrayList<TextFragment>()).equals("");
            assert TextFragment.join(Arrays.asList(new TextFragment[] {foo, bar})).equals("foobar");
        }
    
    private static class TextFragment
    {
        private final FragmentType _type;
        private final String _text;

        public TextFragment(FragmentType type, String text)
        {
            _type = type;
            _text = text;
        }
        
        public FragmentType getType()
        {
            return _type;
        }
        
        public String getText()
        {
            return _text;
        }
        
        public static String join(Collection<TextFragment> fragmentCollection)
        {
            StringBuffer sbText = new StringBuffer();
            for (TextFragment fragment : fragmentCollection)
            {
                sbText.append(fragment.getText());
            }
            return sbText.toString();
        }
    }

    // ---------------------------------------- enum FragmentType
    
        public static @Test void testFragmentType()
        {
            assert FragmentType.typeOf(' ') == FragmentType.Whitespace;
            assert FragmentType.typeOf('\t') == FragmentType.Whitespace;
            assert FragmentType.typeOf('\r') == FragmentType.Whitespace;
            assert FragmentType.typeOf('\n') == FragmentType.Whitespace;

            assert FragmentType.typeOf('-') == FragmentType.Word;
            assert FragmentType.typeOf('\'') == FragmentType.Word;
            assert FragmentType.typeOf('0') == FragmentType.Word;
            assert FragmentType.typeOf('9') == FragmentType.Word;
            assert FragmentType.typeOf('A') == FragmentType.Word;
            assert FragmentType.typeOf('Z') == FragmentType.Word;
            assert FragmentType.typeOf('a') == FragmentType.Word;
            assert FragmentType.typeOf('z') == FragmentType.Word;

            assert FragmentType.typeOf(',') == FragmentType.Other;
            assert FragmentType.typeOf(';') == FragmentType.Other;
            assert FragmentType.typeOf(':') == FragmentType.Other;
            assert FragmentType.typeOf('?') == FragmentType.Other;
            assert FragmentType.typeOf('.') == FragmentType.Other;
            
            assert FragmentType.typeOfCharAtAnyIndex("", -2) == null;
            assert FragmentType.typeOfCharAtAnyIndex("", -1) == null;
            assert FragmentType.typeOfCharAtAnyIndex("", 0) == null;
            assert FragmentType.typeOfCharAtAnyIndex("", 1) == null;
            assert FragmentType.typeOfCharAtAnyIndex(" ", 0) == FragmentType.Whitespace;
        }

    enum FragmentType 
    { 
        Other,
        Word,
        Whitespace;

        public static FragmentType typeOf(char ch)
        {
            if (isWhitespaceChar((char)ch)) return Whitespace;
            if (isWordChar((char)ch)) return Word;
            return Other;
        }
        
        public static FragmentType typeOfCharAtAnyIndex(String text, int index)
        {
            if (index < 0) return null;
            if (index >= text.length()) return null;
            return typeOf(text.charAt(index));
        }
        
        private static boolean isWhitespaceChar(char ch)
        {
            if (ch == ' ') return true;
            if (ch == '\t') return true;
            if (ch == '\r') return true;
            if (ch == '\n') return true;
            return false;
        }
        
        private static boolean isWordChar(char ch)
        {
            if (ch == '-') return true;
            if (ch == '\'') return true;
            if ('0' <= ch && ch <= '9') return true;
            if ('A' <= ch && ch <= 'Z') return true;
            if ('a' <= ch && ch <= 'z') return true;
            return false;
        }
        
    };
    
    
    
    
    
    
    private Dictionary<String, String> myArticleTitleBag;
    
    public Linker(Ranker ranker, String articleTitlesFilePath) throws Exception
    {
        myArticleTitleBag = readArticleTitles(ranker, articleTitlesFilePath);
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
                String articleTitleLine = in.readLine();
                if (articleTitleLine == null) break;
                String[] articleTitleFields = articleTitleLine.split("\t");
                String articleTitle = articleTitleFields[articleTitleFields.length - 1];
                articleTitle = articleTitle.trim();
                articleTitle = articleTitle.replace('_', ' ');
                //if (countWords(articleTitle) < 2) continue;
                //if (!justLetters(articleTitle)) continue;
                double rank = ranker.rankPhrase(articleTitle);
                if (rank < 0.0001)
                {
                    articleTitleCount++;
                    articleTitleBag.put(articleTitle, articleTitle);
                }
                else
                {
                    System.out.println("--- " + articleTitle);
                }
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

}
