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

import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// ------------------------------------------------------------
// --------------------- class QdmlParser ---------------------
// ------------------------------------------------------------

/**
 * A quick-and-dirty markup language parser.  It will parse either HTML or XML
 * regardless of whether it is well-formed or not.  The input is parsed into
 * a flat list of "fragments" which can be rejoined into exactly the same input
 * text, even if the parser makes errors.  Tags are separated into "start tags",
 * and "end tags" (and "elements" in the case of closed XML tags).  Start tags,
 * end tags, elements, and text nodes are the primary types of fragments, although
 * more could be introduced as needed.  The tree structure can be recovered by a 
 * separate process which matches start and end tags, although that process should
 * allow for errors either in the input or due to deficiencies in the parser 
 * itself.
 */

public class QdmlParser
{

    // ---------------------------------------- instance variables
    
    private ParserState _state = null;
    private FragmentBuilder _fragmentBuilder = new FragmentBuilder();

    // ---------------------------------------- QdmlParser readAndParseHttpFile

    public static List<QdmlFragment> readAndParseHttpFile(String urlString) throws Exception
    {
        URL url = new URL(urlString);
        InputStreamReader in = new InputStreamReader(new BufferedInputStream(url.openStream()), "UTF-8");
        QdmlParser parser = new QdmlParser();
        List<QdmlFragment> fragmentList = new ArrayList<QdmlFragment>();
        while (true)
        {
            int chInt = in.read();
            QdmlFragment fragment = parser.parseCharacter(chInt);
            if (fragment != null)
            {
                fragmentList.add(fragment);
            }
            if (chInt == -1) break;
        }
        return fragmentList;
    }

    // ---------------------------------------- QdmlParser join

    public static String join(List<QdmlFragment> fragmentList)
    {
        StringBuffer sbHtmlText = new StringBuffer();
        for (QdmlFragment fragment : fragmentList)
        {
            sbHtmlText.append(fragment.toString());
        }
        return sbHtmlText.toString();
    }

    // ---------------------------------------- QdmlParser parse
    
    /**
     * Parse markup language source into a list of "fragments".
     * Note that the fragments can be "joined" to fully
     * reconstitute the original source text.
     */
    
    public List<QdmlFragment> parse(CharSequence text)
    {
        List<QdmlFragment> fragmentList = new ArrayList<QdmlFragment>();

        for (int i = 0; i < text.length(); i++)
        {
            QdmlFragment fragment = parseCharacter(text.charAt(i));
            if (fragment != null)
            {
                fragmentList.add(fragment);
            }
        }
        
        QdmlFragment lastFragment = parseCharacter(-1);
        if (lastFragment != null) 
        {
            fragmentList.add(lastFragment);
        }

        return fragmentList;
    }

    // ---------------------------------------- QdmlParser parseCharacter
    /**
     * Parse markup language source one character at a time.  If the character
     * completes a "fragment" then return the new fragment object, otherwise
     * return null.  This method should always be called with "-1" as the 
     * last character since sometimes a final fragment will need to be
     * returned.
     */
    public QdmlFragment parseCharacter(int chInt)
    {
        if (chInt == -1) return _fragmentBuilder.finish();
        _state = ParserState.nextState(_state, (char)chInt);
        return _fragmentBuilder.update((char)chInt, _state);
    }

    // ---------------------------------------- class QdmlFragment

    public static class QdmlFragment
    {
        private String myText;
        
        public QdmlFragment(String text)
        {
            myText = text;
        }
        
        public String toString()
        {
            return myText;
        }
        
        public boolean isStartTagCs(String tagName)
        {
            return false;
        }
        
        public boolean isStartTagCi(String tagName)
        {
            return false;
        }
        
        public boolean isEndTagCs(String tagName)
        {
            return false;
        }
        
        public boolean isEndTagCi(String tagName)
        {
            return false;
        }
        
        public boolean isTextNode()
        {
            return false;
        }
    }

    // ---------------------------------------- class QdmlTagFragment

    public static class QdmlTagFragment extends QdmlFragment
    {
        public String myTagName;
        
        public QdmlTagFragment(String tagName, String text)
        {
            super(text);
            myTagName = tagName;
        }
        
        public String getTagName()
        {
            return myTagName;
        }
    }

    // ---------------------------------------- class QdmlStartTagFragment

    public static class QdmlStartTagFragment extends QdmlTagFragment
    {
        public QdmlStartTagFragment(String tagName, String text)
        {
            super(tagName, text);
        }
        
        public boolean isStartTagCs(String tagName)
        {
            return getTagName().equals(tagName);
        }
        
        public boolean isStartTagCi(String tagName)
        {
            return getTagName().equalsIgnoreCase(tagName);
        }
    }

    // ---------------------------------------- class QdmlEndTagFragment

    public static class QdmlEndTagFragment extends QdmlTagFragment
    {
        public QdmlEndTagFragment(String tagName, String text)
        {
            super(tagName, text);
        }
        
        public boolean isEndTagCs(String tagName)
        {
            return getTagName().equals(tagName);
        }
        
        public boolean isEndTagCi(String tagName)
        {
            return getTagName().equalsIgnoreCase(tagName);
        }
    }

    // ---------------------------------------- class QdmlElementFragment

    public static class QdmlElementFragment extends QdmlFragment
    {
        public QdmlElementFragment(String text)
        {
            super(text);
        }
    }

    // ---------------------------------------- class QdmlCommentFragment

    public static class QdmlCommentFragment extends QdmlFragment
    {
        public QdmlCommentFragment(String text)
        {
            super(text);
        }
    }

    // ---------------------------------------- class QdmlTextNodeFragment

    public static class QdmlTextNodeFragment extends QdmlFragment
    {
        public QdmlTextNodeFragment(String text)
        {
            super(text);
        }
        
        public boolean isTextNode()
        {
            return true;
        }
    }

    // ---------------------------------------- class FragmentBuilder

    /*
     * A fragment builder is fed a sequence of character/parser-state pairs.
     * As complete fragments are recognized (possibly several at one time)
     * fragment objects representing them are allocated and copied into the
     * designated receiver-list.  The caller must call a "finish" method 
     * after the last character to guarantee pick-up of the last fragment.
     */

    private static class FragmentBuilder
    {
        private StringBuffer _stringBuffer = new StringBuffer();
        private ParserStateRangeTracker _stateTracker = new ParserStateRangeTracker();

        public QdmlFragment update(char ch, ParserState state)
        {
            // If we have a start tag then we need to reset, and we may
            // have some leading text that we'll want to return as well.
            QdmlFragment leadingTextFragmentIfAny = null;
            if (state == ParserState.TAGSTART)
            {
                if (_stringBuffer.length() > 0)
                {
                    String leadingText = _stringBuffer.toString();
                    leadingTextFragmentIfAny = new QdmlTextNodeFragment(leadingText);
                    _stringBuffer.setLength(0);
                }
                _stateTracker.reset();
            }
            
            // Update our state stracker and the string-buffer accumulator.
            _stateTracker.update(_stringBuffer.length(), state);
            _stringBuffer.append(ch);
            
            // If this was a start tag, we can return now.
            if (state == ParserState.TAGSTART)
            {
                return leadingTextFragmentIfAny;
            }
            
            // If we've hit the end of a comment then we need to return a comment fragment.
            if (state == ParserState.TAGENDCOMMENT)
            {
                QdmlFragment commentFragment = new QdmlCommentFragment(_stringBuffer.toString());
                _stateTracker.reset();
                _stringBuffer.setLength(0);
                return commentFragment;             
            }

            // If we've hit the end of the tag then we need to return a tag fragment.
            if (state == ParserState.TAGEND)
            {
                // Figure out where the tag name is at in the accumulated text.
                int tagNameFirstIndex = _stateTracker.getFirstIndex(ParserState.TAGNAMEFIRST);
                int tagNameLastIndex = Math.max(tagNameFirstIndex, _stateTracker.getLastIndex(ParserState.TAGNAMETAIL));
                if (tagNameFirstIndex == -1)
                {                   
                    // Not a tag at all, or a malformed tag.
                    _stateTracker.reset();
                    return null;
                }
    
                // Extract the tag name and tag text, then reset the string-buffer accumulator.
                String tagName = _stringBuffer.substring(tagNameFirstIndex, tagNameLastIndex + 1);              
                String tagText = _stringBuffer.toString();
                _stringBuffer.setLength(0);

                // Create and return the appropriate kind of tag/element fragment.
                int tagStartSlashIndex = _stateTracker.getFirstIndex(ParserState.TAGSTARTSLASH);
                int tagEndSlashIndex = _stateTracker.getFirstIndex(ParserState.TAGENDSLASH);                
                QdmlFragment tagFragment = createTagFragment(tagName, tagText, tagStartSlashIndex, tagEndSlashIndex);
                return tagFragment;
            }
            
            // No fragment to return this time.
            return null;    
        }

        public QdmlFragment finish()
        {
            if (_stringBuffer.length() == 0) return null;
            QdmlFragment fragment = new QdmlTextNodeFragment(_stringBuffer.toString());
            _stringBuffer.setLength(0);
            return fragment;
        }
        
        private QdmlFragment createTagFragment(String tagName, String tagText, int tagStartSlashIndex, int tagEndSlashIndex)
        {
            if (tagStartSlashIndex >= 0 && tagEndSlashIndex >= 0)
            {
                // malformed!
                return new QdmlElementFragment(tagText);
            }

            else if (tagEndSlashIndex >= 0)
            {
                return new QdmlElementFragment(tagText);
            }
            
            else if (tagStartSlashIndex >= 0)
            {
                return new QdmlEndTagFragment(tagName, tagText);
            }
            
            else
            {
                return new QdmlStartTagFragment(tagName, tagText);
            }
            
        }

    }

    // ---------------------------------------- class ParserStateRangeTracker
    
    private static class ParserStateRangeTracker
    {
        private RangeTracker[] _trackers = new RangeTracker[ParserState.values().length];

        public ParserStateRangeTracker()
        {
            for (int i = 0; i < _trackers.length; i++)
            {
                _trackers[i] = new RangeTracker();
            }
        }
        
        private RangeTracker getTracker(ParserState state)
        {
            RangeTracker tracker = _trackers[state.ordinal()];
            return tracker;
        }
        
        public void update(int index, ParserState state)
        {
            if (state != null)
            {
                RangeTracker tracker = getTracker(state);
                tracker.update(index);
            }
        }
        
        public void reset()
        {
            for (int i = 0; i < _trackers.length; i++)
            {
                _trackers[i].reset();
            }
        }
        
        public int getFirstIndex(ParserState state)
        {
            RangeTracker tracker = getTracker(state);
            return tracker.getFirstIndex();
        }
        
        public int getLastIndex(ParserState state)
        {
            RangeTracker tracker = getTracker(state);
            return tracker.getLastIndex();
        }
    }
    
    // ---------------------------------------- class RangeTracker
    
    /**
     * A range tracker is fed a list of indexes, in increasing
     * order.  It remembers both the first/lowest index seen
     * and the last/highest index seen.  If no index has been
     * seen, it will return -1 for both values.
     */

    private static class RangeTracker
    {
        int _firstIndex = -1;
        int _lastIndex = -1;
        
        public int getFirstIndex()
        {
            return _firstIndex;
        }
        
        public int getLastIndex()
        {
            return _lastIndex;
        }
    
        void update(int index)
        {
            if (_firstIndex == -1)
            {
                _firstIndex = index;
            }
            
            _lastIndex = index;
        }
        
        void reset()
        {
            _firstIndex = -1;
            _lastIndex = -1;
        }
    }
    
    // ---------------------------------------- enum ParserParserState

    private static enum ParserState
    {
        TAGSTART,               // <tag ...>, </tag>
        
        TAGCOMMENT1,            // <(!)--...-->
        TAGCOMMENT2,            // <!(-)-...-->
        TAGCOMMENT3,            // <!-(-)...-->
        TAGCOMMENTTEXT,         // <!--(.)...-->
        TAGENDCOMMENT1,         // <!--...(-)->
        TAGENDCOMMENT2,         // <!--...-(-)>
        TAGENDCOMMENT,          // <!--...--(>)

        TAGSTARTSLASH,          // </tag>, <tag/>
        TAGNAMEFIRST,           // <head>, <h1>
        TAGNAMETAIL,                // <tag>, <tag...>, <tag/>
        TAGATTRIBUTES,          // <tag...>
        
        TAGATTRIBQ1BEGIN,
        TAGATTRIBQ1END,
        TAGATTRIBQ1TEXT,
        
        TAGATTRIBQ2BEGIN,
        TAGATTRIBQ2END,
        TAGATTRIBQ2TEXT,
        
        TAGENDSLASH,
        TAGEND;                 // ...>text, ...><tag
        
        public static ParserState nextState(ParserState state, char chNext)
        {
            //  abc<..., abcd
            if (state == null)
            {
                if (chNext == '<') return TAGSTART;
                return null;
            }
            
            // <tag, </tag
            else if (state == TAGSTART)
            {
                if ('A' <= chNext && chNext <= 'Z') return TAGNAMEFIRST;
                if ('a' <= chNext && chNext <= 'z') return TAGNAMEFIRST;
                if (chNext == '/') return TAGSTARTSLASH;
                if (chNext == '!') return TAGCOMMENT1;
                return null;
            }
            
            // <(!)--...-->
            else if (state == TAGCOMMENT1)
            {
                if (chNext == '-') return TAGCOMMENT2;
                return null;
            }
            
            // <!(-)-...-->
            else if (state == TAGCOMMENT2)
            {
                if (chNext == '-') return TAGCOMMENT3;
                return null;
            }
            
            // <!-(-)...-->
            else if (state == TAGCOMMENT3)
            {
                if (chNext == '-') return TAGENDCOMMENT1;
                return TAGCOMMENTTEXT;
            }
            
            // <!--(.)...-->
            else if (state == TAGCOMMENTTEXT)
            {
                if (chNext == '-') return TAGENDCOMMENT1;
                return TAGCOMMENTTEXT;
            }

            // <!--...(-)->
            else if (state == TAGENDCOMMENT1)
            {
                if (chNext == '-') return TAGENDCOMMENT2;
                return TAGCOMMENTTEXT;
            }

            // <!--...-(-)>
            else if (state == TAGENDCOMMENT2)
            {
                if (chNext == '>') return TAGENDCOMMENT;
                return TAGCOMMENTTEXT;
            }

            // <!--...--(>)
            else if (state == TAGENDCOMMENT)
            {
                if (chNext == '<') return TAGSTART;
                return null;
            }

            // </tag
            else if (state == TAGSTARTSLASH)
            {
                if ('A' <= chNext && chNext <= 'Z') return TAGNAMEFIRST;
                if ('a' <= chNext && chNext <= 'z') return TAGNAMEFIRST;
                return null;
            }
            
            // <head, <h1, <p>, <p/>, <p style...
            else if (state == TAGNAMEFIRST)
            {
                if ('A' <= chNext && chNext <= 'Z') return TAGNAMETAIL;
                if ('a' <= chNext && chNext <= 'z') return TAGNAMETAIL;
                if ('0' <= chNext && chNext <= '9') return TAGNAMETAIL;
                if (chNext == '>') return TAGEND;
                if (chNext == '/') return TAGENDSLASH;
                return TAGATTRIBUTES;
            }
            
            // <tagx, <tag1, <tag/, <tag>, <tag style...
            else if (state == TAGNAMETAIL)
            {
                if ('A' <= chNext && chNext <= 'Z') return TAGNAMETAIL;
                if ('a' <= chNext && chNext <= 'z') return TAGNAMETAIL;
                if ('0' <= chNext && chNext <= '9') return TAGNAMETAIL;
                if (chNext == '>') return TAGEND;
                if (chNext == '/') return TAGENDSLASH;          
                return TAGATTRIBUTES;
            }
            
            // <tag attr> <tag attrx, <tag attr/, <tag attr>
            else if (state == TAGATTRIBUTES)
            {
                if (chNext == '\'') return TAGATTRIBQ1BEGIN;
                if (chNext == '"') return TAGATTRIBQ2BEGIN;
                if (chNext == '/') return TAGENDSLASH;
                if (chNext == '>') return TAGEND;
                return TAGATTRIBUTES;
            }
            
            else if (state == TAGATTRIBQ1BEGIN)
            {
                if (chNext == '\'') return TAGATTRIBQ1END;
                return TAGATTRIBQ1TEXT;
            }
            
            else if (state == TAGATTRIBQ1TEXT)
            {
                if (chNext == '\r') return TAGEND;          // ??? tentative ???
                if (chNext == '\n') return TAGEND;          // ??? tentative ???
                if (chNext == '\'') return TAGATTRIBQ1END;
                return TAGATTRIBQ1TEXT;
            }
            
            else if (state == TAGATTRIBQ1END)
            {
                if (chNext == '/') return TAGENDSLASH;
                if (chNext == '>') return TAGEND;
                return TAGATTRIBUTES;
            }
            
            else if (state == TAGATTRIBQ2BEGIN)
            {
                if (chNext == '"') return TAGATTRIBQ2END;
                return TAGATTRIBQ2TEXT;
            }
            
            else if (state == TAGATTRIBQ2TEXT)
            {
                if (chNext == '"') return TAGATTRIBQ2END;
                return TAGATTRIBQ2TEXT;
            }
            
            else if (state == TAGATTRIBQ2END)
            {
                if (chNext == '/') return TAGENDSLASH;
                if (chNext == '>') return TAGEND;
                return TAGATTRIBUTES;
            }
            
            // <tag/>
            else if (state == TAGENDSLASH)
            {
                if (chNext == '>') return TAGEND;
                return null;
            }
            
            // <tag><, <tag>...
            else if (state == TAGEND)
            {
                if (chNext == '<') return TAGSTART;
                return null;
            }
            
            return null;
        }
    };

}
