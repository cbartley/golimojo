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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

class HtmlFragment
{
    private String myText;
    
    public HtmlFragment(String text)
    {
        myText = text;
    }
    
    public String toString()
    {
        return myText;
    }
}

class Tag extends HtmlFragment
{
    public String myTagName;
    
    public Tag(String tagName, String text)
    {
        super(text);
        myTagName = tagName.toUpperCase();
    }
    
    public String getTagName()
    {
        return myTagName;
    }
}

class StartTag extends Tag
{
    public StartTag(String tagName, String text)
    {
        super(tagName, text);
    }
}

class EndTag extends Tag
{
    public EndTag(String tagName, String text)
    {
        super(tagName, text);
    }
}

class Element extends HtmlFragment
{
    public Element(String text)
    {
        super(text);
    }
}

class TextNode extends HtmlFragment
{
    public TextNode(String text)
    {
        super(text);
    }
}

public class Parser 
{
    
    enum State
    {
        TAGSTART,               // <tag ...>, </tag>
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
        TAGEND                  // ...>text, ...><tag
    };

    private static State nextState(State state, char chNext)
    {
        //  abc<..., abcd
        if (state == null)
        {
            if (chNext == '<') return State.TAGSTART;
            return null;
        }
        
        // <tag, </tag
        else if (state == State.TAGSTART)
        {
            if ('A' <= chNext && chNext <= 'Z') return State.TAGNAMEFIRST;
            if ('a' <= chNext && chNext <= 'z') return State.TAGNAMEFIRST;
            if (chNext == '/') return State.TAGSTARTSLASH;
            return null;
        }
        
        // </tag
        else if (state == State.TAGSTARTSLASH)
        {
            if ('A' <= chNext && chNext <= 'Z') return State.TAGNAMEFIRST;
            if ('a' <= chNext && chNext <= 'z') return State.TAGNAMEFIRST;
            return null;
        }
        
        // <head, <h1, <p>, <p/>, <p style...
        else if (state == State.TAGNAMEFIRST)
        {
            if ('A' <= chNext && chNext <= 'Z') return State.TAGNAMETAIL;
            if ('a' <= chNext && chNext <= 'z') return State.TAGNAMETAIL;
            if ('0' <= chNext && chNext <= '9') return State.TAGNAMETAIL;
            if (chNext == '>') return State.TAGEND;
            if (chNext == '/') return State.TAGENDSLASH;
            return State.TAGATTRIBUTES;
        }
        
        // <tagx, <tag1, <tag/, <tag>, <tag style...
        else if (state == State.TAGNAMETAIL)
        {
            if ('A' <= chNext && chNext <= 'Z') return State.TAGNAMETAIL;
            if ('a' <= chNext && chNext <= 'z') return State.TAGNAMETAIL;
            if ('0' <= chNext && chNext <= '9') return State.TAGNAMETAIL;
            if (chNext == '>') return State.TAGEND;
            if (chNext == '/') return State.TAGENDSLASH;            
            return State.TAGATTRIBUTES;
        }
        
        // <tag attr> <tag attrx, <tag attr/, <tag attr>
        else if (state == State.TAGATTRIBUTES)
        {
            if (chNext == '\'') return State.TAGATTRIBQ1BEGIN;
            if (chNext == '"') return State.TAGATTRIBQ2BEGIN;
            if (chNext == '/') return State.TAGENDSLASH;
            if (chNext == '>') return State.TAGEND;
            return State.TAGATTRIBUTES;
        }
        
        else if (state == State.TAGATTRIBQ1BEGIN)
        {
            if (chNext == '\'') return State.TAGATTRIBQ1END;
            return State.TAGATTRIBQ1TEXT;
        }
        
        else if (state == State.TAGATTRIBQ1TEXT)
        {
            if (chNext == '\'') return State.TAGATTRIBQ1END;
            return State.TAGATTRIBQ1TEXT;
        }
        
        else if (state == State.TAGATTRIBQ1END)
        {
            if (chNext == '/') return State.TAGENDSLASH;
            if (chNext == '>') return State.TAGEND;
            return State.TAGATTRIBUTES;
        }
        
        else if (state == State.TAGATTRIBQ2BEGIN)
        {
            if (chNext == '"') return State.TAGATTRIBQ2END;
            return State.TAGATTRIBQ2TEXT;
        }
        
        else if (state == State.TAGATTRIBQ2TEXT)
        {
            if (chNext == '"') return State.TAGATTRIBQ2END;
            return State.TAGATTRIBQ2TEXT;
        }
        
        else if (state == State.TAGATTRIBQ2END)
        {
            if (chNext == '/') return State.TAGENDSLASH;
            if (chNext == '>') return State.TAGEND;
            return State.TAGATTRIBUTES;
        }
        
        // <tag/>
        else if (state == State.TAGENDSLASH)
        {
            if (chNext == '>') return State.TAGEND;
            return null;
        }
        
        // <tag><, <tag>...
        else if (state == State.TAGEND)
        {
            if (chNext == '<') return State.TAGSTART;
            return null;
        }
        
        return null;
    }

    private static class Tracker
    {
        int myFirstIndex = -1;
        int myLastIndex = -1;
        
        public int getFirstIndex()
        {
            return myFirstIndex;
        }
        
        public int getLastIndex()
        {
            return myLastIndex;
        }
    
        void update(int index)
        {
            if (myFirstIndex == -1)
            {
                myFirstIndex = index;
            }
            
            myLastIndex = index;
        }
        
        void reset()
        {
            myFirstIndex = -1;
            myLastIndex = -1;
        }
    }
    
    private static class StateTracker
    {
        private Tracker[] myTrackers;
        
        public StateTracker()
        {
            int trackerCount = State.values().length;
            myTrackers = new Tracker[trackerCount];
            for (int i = 0; i < trackerCount; i++)
            {
                myTrackers[i] = new Tracker();
            }
        }
        
        private Tracker getTracker(State state)
        {
            Tracker tracker = myTrackers[state.ordinal()];
            return tracker;
        }
        
        public void update(int index, State state)
        {
            if (state != null)
            {
                Tracker tracker = getTracker(state);
                tracker.update(index);
            }
        }
        
        public void reset()
        {
            for (int i = 0; i < myTrackers.length; i++)
            {
                myTrackers[i].reset();
            }
        }
        
        public int getFirstIndex(State state)
        {
            Tracker tracker = getTracker(state);
            return tracker.getFirstIndex();
        }
        
        public int getLastIndex(State state)
        {
            Tracker tracker = getTracker(state);
            return tracker.getLastIndex();
        }
    }

    private static class FragmentBuilder
    {
        private StringBuffer myStringBuffer = new StringBuffer();
        private StateTracker myStateTracker = new StateTracker();
        
        private HtmlFragment createTagFragment(String tagName, String tagText, int tagStartSlashIndex, int tagEndSlashIndex)
        {
            if (tagStartSlashIndex >= 0 && tagEndSlashIndex >= 0)
            {
                // malformed!
                return new Element(tagText);
            }

            else if (tagEndSlashIndex >= 0)
            {
                return new Element(tagText);
            }
            
            else if (tagStartSlashIndex >= 0)
            {
                return new EndTag(tagName, tagText);
            }
            
            else
            {
                return new StartTag(tagName, tagText);
            }
            
        }
        
        public void update(char ch, State state, List<HtmlFragment> fragmentReceiverList)
        {
            if (state == State.TAGSTART)
            {
                myStateTracker.reset();
            }
            
            myStateTracker.update(myStringBuffer.length(), state);
            myStringBuffer.append(ch);
            
            if (state == State.TAGEND)
            {
                int tagIndex = myStateTracker.getFirstIndex(State.TAGSTART);
                String leadingText = myStringBuffer.substring(0, tagIndex);
                String tagText = myStringBuffer.substring(tagIndex);
                
                int tagNameFirstIndex = myStateTracker.getFirstIndex(State.TAGNAMEFIRST);
                int tagNameLastIndex = Math.max(tagNameFirstIndex, myStateTracker.getLastIndex(State.TAGNAMETAIL));
                if (tagNameFirstIndex == -1 || tagNameLastIndex == -1)
                {
                    // Malformed tag!
                    myStateTracker.reset();
                    return;
                }
                
                String tagName = myStringBuffer.substring(tagNameFirstIndex, tagNameLastIndex + 1);
//System.out.println("*** " + tagName);
//System.out.println("... " + tagText);

                myStringBuffer.setLength(0);
                
                if (tagIndex > 0)
                {
                    HtmlFragment textNode = new TextNode(leadingText);
                    fragmentReceiverList.add(textNode);
                }
                
                int tagStartSlashIndex = myStateTracker.getFirstIndex(State.TAGSTARTSLASH);
                int tagEndSlashIndex = myStateTracker.getFirstIndex(State.TAGENDSLASH);             
                HtmlFragment tagFragment = createTagFragment(tagName, tagText, tagStartSlashIndex, tagEndSlashIndex);
                fragmentReceiverList.add(tagFragment);
            }
        }

        public void finish(List<HtmlFragment> fragmentReceiverList)
        {
            String text = myStringBuffer.toString();
            myStringBuffer.setLength(0);
            HtmlFragment fragment = new TextNode(text);
            fragmentReceiverList.add(fragment);
        }
        
    }
    
    public static List<HtmlFragment> parse(CharSequence text)
    {
        State state = null;
        FragmentBuilder fragmentBuilder = new FragmentBuilder();
        List<HtmlFragment> fragmentList = new ArrayList<HtmlFragment>();
        for (int i = 0; i < text.length(); i++)
        {
            char nextCh = text.charAt(i);
            State nextState = nextState(state, nextCh);
            fragmentBuilder.update(nextCh, nextState, fragmentList);
            state = nextState;
        }
        
        return fragmentList;
    }
    
    private static List<HtmlFragment> copyWithoutElements(List<HtmlFragment> fragmentList, String tagName)
    {
        boolean inElement = false;
        List<HtmlFragment> resultFragmentList = new ArrayList<HtmlFragment>();
        for (HtmlFragment fragment : fragmentList)
        {
            Tag tag = null;
            if (fragment instanceof Tag)
            {
                tag = (Tag)fragment;
            }
            
            Tag excludeTag = null;
            if (tag != null && tag.getTagName().equalsIgnoreCase(tagName))
            {
                excludeTag = tag;
            }

            if (excludeTag != null && excludeTag instanceof StartTag) 
            {
                inElement = true;
            }
            
            if (!inElement)
            {
                resultFragmentList.add(fragment);
            }

            if (excludeTag != null && excludeTag instanceof EndTag)
            {
                inElement = false;
            }

        }
        
        return resultFragmentList;
    }

    public static String getContentText(List<HtmlFragment> fragmentList)
    {
        boolean passedBody = false;
        fragmentList = copyWithoutElements(fragmentList, "SCRIPT");
        fragmentList = copyWithoutElements(fragmentList, "STYLE");
        List<TextNode> contentTextNodeList = new ArrayList<TextNode>();
        for (HtmlFragment fragment : fragmentList)
        {
            StartTag startTag = null;
            if (fragment instanceof StartTag)
            {
                startTag = (StartTag)fragment;
            }

            if (startTag != null && startTag.getTagName().equalsIgnoreCase("BODY"))
            {
                passedBody = true;
            }
            
            if (passedBody && fragment instanceof TextNode)
            {
                contentTextNodeList.add((TextNode)fragment);
            }
        }
        
        StringBuffer sbContentText = new StringBuffer();
        for (TextNode textNode : contentTextNodeList)
        {
            sbContentText.append(textNode.toString());
            sbContentText.append(" ");
        }
        
        return sbContentText.toString();
    }

    public static String readHttpFile(String urlString) throws Exception
    {
            URL url = new URL(urlString);
        BufferedReader in = new BufferedReader(new InputStreamReader(new BufferedInputStream(url.openStream())));
        StringBuffer buffer = new StringBuffer();
        while (true)
        {
            int chInt = in.read();
            if (chInt == -1) break;
            buffer.append((char)chInt);
        }
        return buffer.toString();
    }

    public static String join(List<HtmlFragment> fragmentList)
    {
        StringBuffer sbHtmlText = new StringBuffer();
        for (HtmlFragment fragment : fragmentList)
        {
            sbHtmlText.append(fragment.toString());
        }
        return sbHtmlText.toString();
    }

    public static void main(String[] args) throws Exception
    {
        String url = "http://www.almostinfinite.com/memtrack.html";
//      url = "http://slashdot.org";
        String htmlText = readHttpFile(url);
        List<HtmlFragment> fragmentList = parse(htmlText);
        String contentText = getContentText(fragmentList);
        System.out.println(contentText);
    }

}
