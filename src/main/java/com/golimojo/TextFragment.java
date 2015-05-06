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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TextFragment
{
    
    // ---------------------------------------- instance variables

    private final FragmentType _type;
    private final String _text;

    // ---------------------------------------- TextFragment constructor

    public TextFragment(FragmentType type, String text)
    {
        _type = type;
        _text = text;
    }

    // ---------------------------------------- TextFragment getType
    
    public FragmentType getType()
    {
        return _type;
    }

    // ---------------------------------------- TextFragment getText
    
    public String getText()
    {
        return _text;
    }

    // ---------------------------------------- TextFragment join
    
    public static String join(Collection<TextFragment> fragmentCollection)
    {
        StringBuffer sbText = new StringBuffer();
        for (TextFragment fragment : fragmentCollection)
        {
            sbText.append(fragment.getText());
        }
        return sbText.toString();
    }
        
        public static void L1TEST_join()
        {
            TextFragment foo = new TextFragment(FragmentType.Other, "foo");
            TextFragment bar = new TextFragment(FragmentType.Other, "bar");
            
            assert TextFragment.join(new ArrayList<TextFragment>()).equals("");
            assert TextFragment.join(Arrays.asList(new TextFragment[] {foo, bar})).equals("foobar");
        }

    // ---------------------------------------- TextFragment  subJoin

    public static String subJoin(List<TextFragment> fragmentList, int startIndex, int fragmentCount)
    {
        StringBuffer sbPhrase = new StringBuffer();
        int endIndex = Math.min(startIndex + fragmentCount, fragmentList.size());
        for (int i = startIndex; i < endIndex; i++)
        {
            TextFragment fragment = fragmentList.get(i);
            if (fragment.getType() == FragmentType.Whitespace)
            {
                sbPhrase.append(" ");
            }
            else
            {
                sbPhrase.append(fragment.getText());
            }
        }
        return sbPhrase.toString();
    }

        public static void L1TEST_assemblePhrase()
        {
            assert subJoin(TextFragment.splitTextIntoFragments(""), 0, 0).equals("");
            assert subJoin(TextFragment.splitTextIntoFragments(""), 0, 1).equals("");
            assert subJoin(TextFragment.splitTextIntoFragments(" "), 0, 1).equals(" ");
            assert subJoin(TextFragment.splitTextIntoFragments(" "), 0, 2).equals(" ");
            assert subJoin(TextFragment.splitTextIntoFragments("  "), 0, 2).equals(" ");
            assert subJoin(TextFragment.splitTextIntoFragments("   "), 0, 3).equals(" ");

            assert subJoin(TextFragment.splitTextIntoFragments(" foo"), 0, 0).equals("");
            assert subJoin(TextFragment.splitTextIntoFragments(" foo"), 0, 1).equals(" ");
            assert subJoin(TextFragment.splitTextIntoFragments(" foo"), 0, 2).equals(" foo");
            assert subJoin(TextFragment.splitTextIntoFragments("foo "), 0, 1).equals("foo");
            assert subJoin(TextFragment.splitTextIntoFragments("foo "), 0, 2).equals("foo ");
            assert subJoin(TextFragment.splitTextIntoFragments("foo  "), 0, 2).equals("foo ");
            assert subJoin(TextFragment.splitTextIntoFragments("foo\t\tbar"), 0, 3).equals("foo bar");

            assert subJoin(TextFragment.splitTextIntoFragments("foo bar baz"), 1, 0).equals("");
            assert subJoin(TextFragment.splitTextIntoFragments("foo bar baz"), 1, 1).equals(" ");
            assert subJoin(TextFragment.splitTextIntoFragments("foo bar baz"), 1, 2).equals(" bar");
            assert subJoin(TextFragment.splitTextIntoFragments("foo bar baz"), 1, 3).equals(" bar ");
            assert subJoin(TextFragment.splitTextIntoFragments("foo bar baz"), 1, 4).equals(" bar baz");

            assert subJoin(TextFragment.splitTextIntoFragments("foo bar baz"), 2, 0).equals("");
            assert subJoin(TextFragment.splitTextIntoFragments("foo bar baz"), 2, 1).equals("bar");
            assert subJoin(TextFragment.splitTextIntoFragments("foo bar baz"), 2, 2).equals("bar ");
            assert subJoin(TextFragment.splitTextIntoFragments("foo bar baz"), 2, 3).equals("bar baz");

            assert subJoin(TextFragment.splitTextIntoFragments("foo/bar"), 0, 3).equals("foo/bar");
        }

    // ---------------------------------------- TextFragment splitTextIntoFragments

    public static List<TextFragment> splitTextIntoFragments(String text)
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
        
        public static void L1TEST_SplitTextIntoFragments()
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

    //   ---------------------------------------- enum FragmentType

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

        public static void L1TEST_FragmentType()
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
        
    };
}



