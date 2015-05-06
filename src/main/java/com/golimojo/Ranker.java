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
import java.util.Hashtable;
import java.util.List;

public class Ranker
{
    private Hashtable<String, WeightedWord> _weightedWordBag;
    private int _defaultWeight;

    public Ranker(String pathToWordFrequencyFile) throws Exception
    {
        List<WeightedWord> weightedWordList = readWordFrequencyFile(pathToWordFrequencyFile);
        _weightedWordBag = createWeightedWordBag(weightedWordList);
        _defaultWeight = weightedWordList.size() * 3;
    }
    
    public static class PageScore
    {
        private String _phrase;
        private int _weight;
        private int _wordCount;
        private int _wordCharCount;
        
        public PageScore(String phrase, int weight, int wordCount, int wordCharCount)
        {
            _phrase = phrase;
            _weight = weight;
            _wordCount = wordCount;
            _wordCharCount = wordCharCount;
        }
        
        public String getPhrase() {return _phrase;}
        public int getWeight() {return _weight;}
        public int getWordCount() {return _wordCount;}
        public int getWordCharCount() {return _wordCharCount;}
    }
    
    public PageScore phraseWeightAndWordCount(String phrase)
    {
        List<TextFragment> fragmentList = TextFragment.splitTextIntoFragments(phrase);
    
        int weight = 0;
        int wordCount = 0;
        int wordsCharCount = 0;
        for (TextFragment fragment : fragmentList)
        {
            if (fragment.getType() == TextFragment.FragmentType.Word)
            {
//              System.out.printf("... %-64s %7d\n", fragment.getText().toLowerCase(), wordWeight(fragment.getText().toLowerCase()));
                weight += wordWeight(fragment.getText().toLowerCase());
                wordCount++;
                wordsCharCount += fragment.getText().length();
            }
        }
        return new PageScore(phrase, weight, wordCount, wordsCharCount);
    }

    private int wordWeight(String word)
    {
        boolean isNumber = true;
        for (int i = 0; i < word.length(); i++)
        {
            if (!Character.isDigit(word.charAt(i)))
            {
                isNumber = false;
            }
        }

        if (isNumber) return 0;
        
        WeightedWord weightedWord = _weightedWordBag.get(word.toLowerCase());
        if (weightedWord == null) return _defaultWeight;
        return weightedWord.weight;
    }

    private static List<WeightedWord> readWordFrequencyFile(String pathToWordFrequencyFile) throws Exception
    {
        List <WeightedWord> weightedWordList = new ArrayList<WeightedWord>();
        BufferedReader in = new BufferedReader(new FileReader(pathToWordFrequencyFile));
        try
        {
            int count = 1;
            while (true)
            {
                String line = in.readLine();
                if (line == null) break;
                String[] fields = line.split("\t");
                String word = fields[fields.length - 1].trim();
                WeightedWord weightedWord = new WeightedWord(word, count);
                weightedWordList.add(weightedWord);
                count++;
            }
        }
        finally
        {
            in.close();
        }
        return weightedWordList;
    }

    private static Hashtable<String, WeightedWord> createWeightedWordBag(List<WeightedWord> weightedWordList)
    {
        Hashtable<String, WeightedWord> weightedWordBag = new Hashtable<String, WeightedWord>();
        java.util.Collections.reverse(weightedWordList);
        for (WeightedWord weightedWord : weightedWordList)
        {
            weightedWordBag.put(weightedWord.word.toLowerCase(), weightedWord);
        }
        return weightedWordBag;
    }

    private static class WeightedWord
    {
        public final String word;
        public final int weight;
        
        public WeightedWord(String _word, int _weight)
        {
            word = _word;
            weight = _weight;
        }
    }
    
    

    
}
