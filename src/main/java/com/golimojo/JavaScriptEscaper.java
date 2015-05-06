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

/**
 * This class provides an efficient way to escape text for safe
 * embedding in JavaScript string literals.
 */
public class JavaScriptEscaper
{
    // ---------------------------------------- JavaScriptEscaper instance variables
    
    private static final String hexDigits = "0123456789abcdef";
    private static String[] escapeTable = createEscapeTable();
    
    // ---------------------------------------- JavaScriptEscaper constructor
    
    private JavaScriptEscaper()
    {
        assert false: "This class is just a module!";
    }

    // ---------------------------------------- JavaScriptEscaper escape

    public static String escape(String s)
    {
        StringBuffer sb = new StringBuffer();
        escapeAndAppend(s, sb);
        return sb.toString();
    }
    
    public static void L1TEST_escape()
    {
        assert escape("").equals("");
        assert escape(" ").equals(" ");
        assert escape("0").equals("0");
        assert escape("A").equals("A");
        assert escape("a").equals("a");
        assert escape("+").equals("+");
        assert escape("@").equals("@");
        assert escape("~").equals("~");
        assert escape("\"").equals("\\\"");
        assert escape("'").equals("\\'");
        assert escape("\0").equals(".");
        assert escape("\t").equals("\\u0009");
        assert escape("\n").equals("\\u000a");
        assert escape("\u1234").equals("\\u1234");
        assert escape("\u00ff").equals("\\u00ff");
        assert escape("\u0123").equals("\\u0123");
        assert escape("foo").equals("foo");
        assert escape("foo bar").equals("foo bar");
        assert escape("\"foo bar\"").equals("\\\"foo bar\\\"");
        assert escape("'foo bar'").equals("\\'foo bar\\'");
    }

    // ---------------------------------------- JavaScriptEscaper escapeAndAppend
    
    public static void escapeAndAppend(String s, StringBuffer sb)
    {
        for (int i = 0; i < s.length(); i++)
        {
            escapeAndAppend(s.charAt(i), sb);
        }
    }
    
    // ---------------------------------------- JavaScriptEscaper escapeAndAppend
    
    public static void escapeAndAppend(char ch, StringBuffer sb)
    {
        if (ch < escapeTable.length)
        {
            String chEscaped = escapeTable[ch];
            sb.append(chEscaped);
        }
        else
        {
            sb.append("\\u");
            toFourHexDigits(ch, sb);
        }
    }
    
    // ---------------------------------------- JavaScriptEscaper createEscapeTable

    private static String[] createEscapeTable()
    {
        // Build the basic escape table -- non-printing characters are
        // represented by their backslash-uxxx escape sequences and
        // printing characters are represented simply by themselves.
        String[] escapeTable = new String['~' + 1];
        for (char ch = 0; ch <= '~'; ch++)
        {
            if (' ' <= ch && ch <= '~') 
            {
                escapeTable[ch] = String.valueOf(ch);
            }
            else
            {
                escapeTable[ch] = "\\u" + toFourHexDigits(ch);
            }
        }
        
        // Special cases: Both double and single quotes are escaped with
        // backslashes.  We should not ever see NULs, but if we do, replace
        // them "." since there's some possibility that some JavaScript 
        // routines will treat NULs embedded in strings as terminators.
        escapeTable['"'] = "\\\"";
        escapeTable['\''] = "\\'";
        escapeTable['\0'] = ".";
        
        return escapeTable;
    }
    
    // ---------------------------------------- JavaScriptEscaper toFourHexDigits

    private static String toFourHexDigits(char ch) {
        StringBuffer sb = new StringBuffer();
        toFourHexDigits(ch, sb);
        return sb.toString();
    }
    
    public static void L1TEST_toFourHexDigits() {
        assert toFourHexDigits((char)0).equals("0000");
        assert toFourHexDigits((char)16).equals("0010");
        assert toFourHexDigits((char)256).equals("0100");
        assert toFourHexDigits((char)4096).equals("1000");
        assert toFourHexDigits('A').equals("0041");
        assert toFourHexDigits('\u1234').equals("1234");
        assert toFourHexDigits('\uABCD').equals("abcd");
    }
    
    // ---------------------------------------- JavaScriptEscaper toFourHexDigits

    private static void toFourHexDigits(char ch, StringBuffer sb) {
        sb.append(toHexDigit(ch >> 12 & 0x000F));
        sb.append(toHexDigit(ch >> 8  & 0x000F));
        sb.append(toHexDigit(ch >> 4  & 0x000F));
        sb.append(toHexDigit(ch >> 0  & 0x000F));
    }

    // ---------------------------------------- JavaScriptEscaper toHexDigit
    
    private static char toHexDigit(int n) {
        assert 0 <= n;
        assert n < 16;
        return hexDigits.charAt(n);
    }
    
    public static void L1TEST_toHexDigit() {
        assert hexDigits.length() == 16;
        assert toHexDigit(0) == '0';
        assert toHexDigit(1) == '1';
        assert toHexDigit(2) == '2';
        assert toHexDigit(3) == '3';
        assert toHexDigit(4) == '4';
        assert toHexDigit(5) == '5';
        assert toHexDigit(6) == '6';
        assert toHexDigit(7) == '7';
        assert toHexDigit(8) == '8';
        assert toHexDigit(9) == '9';
        assert toHexDigit(10) == 'a';
        assert toHexDigit(11) == 'b';
        assert toHexDigit(12) == 'c';
        assert toHexDigit(13) == 'd';
        assert toHexDigit(14) == 'e';
        assert toHexDigit(15) == 'f';
    }
}
