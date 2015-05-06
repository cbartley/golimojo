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
    private Hashtable<String, WeightedWord> myWeightedWordBag;
    
    public Ranker(String pathToWordFrequencyFile) throws Exception
    {
        List<CountedWord> countedWordList = readWordFrequencyFile(pathToWordFrequencyFile);
        List<WeightedWord> weightedWordList = createWeightedWordList(countedWordList);
        myWeightedWordBag = createWeightedWordBag(weightedWordList);
    }
    
    public double rankPhrase(String phrase)
    {
        double phraseRank = 1.0;
        String[] wordList = phrase.split("[ \t\r\n]+");
        for (String word : wordList)
        {
            double weight = rankWord(word);
            phraseRank *= weight;
        }
        return phraseRank;
    }
    
    private double rankWord(String word)
    {
        WeightedWord weightedWord = myWeightedWordBag.get(word.toLowerCase());
        if (weightedWord == null) return 0.0;
        return weightedWord.weight;
    }
    
    private static List<CountedWord> readWordFrequencyFile(String pathToWordFrequencyFile) throws Exception
    {
        List <CountedWord> countedWordList = new ArrayList<CountedWord>();
        BufferedReader in = new BufferedReader(new FileReader(pathToWordFrequencyFile));
        try
        {
            in.readLine();      // first line is column headings, not data
            while (true)
            {
                String line = in.readLine();
                if (line == null) break;
                String[] fields = line.split(" *\t *");
                if (fields.length == 4)
                {
                    String word = fields[1];
                    String countAsString = fields[3];
                    int count = Integer.parseInt(countAsString);
                    CountedWord countedWord = new CountedWord(word, count);
                    countedWordList.add(countedWord);
                }
                else
                {
                    System.out.println("*** Line skipped, bad format!");
                    System.out.println("... " + line);
                }               
            }
        }
        finally
        {
            in.close();
        }
        return countedWordList;
    }
    
    private static List<WeightedWord> createWeightedWordList(List<CountedWord> countedWordList)
    {
        long totalWordCount = totalWordCount(countedWordList);
        List<WeightedWord> weightedWordList = new ArrayList<WeightedWord>();
        for (CountedWord countedWord : countedWordList)
        {
            double wordWeight = ((double)countedWord.count) / ((double)totalWordCount);
            weightedWordList.add(new WeightedWord(countedWord.word, wordWeight));
        }
        return weightedWordList;
    }

    private static long totalWordCount(List<CountedWord> countedWordList)
    {
        long totalWordCount = 0;
        for (CountedWord countedWord : countedWordList)
        {
            totalWordCount += countedWord.count;
        }
        return totalWordCount;
    }
    
    private static Hashtable<String, WeightedWord> createWeightedWordBag(List<WeightedWord> weightedWordList)
    {
        Hashtable<String, WeightedWord> weightedWordBag = new Hashtable<String, WeightedWord>();
        for (WeightedWord weightedWord : weightedWordList)
        {
            weightedWordBag.put(weightedWord.word.toLowerCase(), weightedWord);
        }
        return weightedWordBag;
    }
    
    private static class CountedWord
    {
        public final String word;
        public final int count;
        
        public CountedWord(String _word, int _count)
        {
            word = _word;
            count = _count;
        }
    }
    
    private static class WeightedWord
    {
        public final String word;
        public final double weight;
        
        public WeightedWord(String _word, double _weight)
        {
            word = _word;
            weight = _weight;
        }
    }
    
    

    
}
