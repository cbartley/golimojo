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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class Template
{
    
    // ---------------------------------------- Template preprocessFileTree

    public static void preprocessFileTree(File folder, Dictionary<String, String> subDict)
        throws IOException
    {
        System.out.printf("### %s\n", folder.toString());
        for (File f : folder.listFiles())
        {
            if (f.isDirectory())
            {
                preprocessFileTree(f, subDict);
            }
            else
            {
                preprocessFile(f, subDict);
            }
        }
    }

    // ---------------------------------------- Template preprocessFile
    
    public static void preprocessFile(File inputFile, Dictionary<String, String> subDict) 
        throws IOException
    {
        String inputFileName = inputFile.getName();
        String outputFileName = inputFileName.replace(".template.", ".");
        if (outputFileName != inputFileName)
        {
            File outputFile = new File(inputFile.getParentFile(), outputFileName);
            preprocessFile(inputFile, outputFile, subDict);
        }
    }

    // ---------------------------------------- Template preprocessFile
    
    public static void preprocessFile(File inputFile, File outputFile,  Dictionary<String, String> subDict) 
        throws IOException
    {
        String fileText = readFile(inputFile.getAbsolutePath());
        String preprocessedFileText = applySubstitutions(fileText, subDict);
        writeFile(outputFile.getAbsolutePath(), preprocessedFileText);
    }
    
    // ---------------------------------------- Template readFile
    
    public static String readFile(String pathToFile) throws IOException
    {
        Reader reader = new BufferedReader(new FileReader(pathToFile));
        try
        {
            StringBuffer sbText = new StringBuffer();
            while (true)
            {
                int chInt = reader.read();
                if (chInt == -1) break;
                char ch = (char)chInt;
                sbText.append(ch);
            }   
            return sbText.toString();
        }
        finally
        {
            reader.close();
        }
    }
    
    // ---------------------------------------- Template writeFile
    
    public static void writeFile(String pathToFile, String fileText) throws IOException
    {
        Writer writer = new BufferedWriter(new FileWriter(pathToFile));
        try
        {
            for (int i = 0; i < fileText.length(); i++)
            {
                char ch = fileText.charAt(i);
                writer.write(ch);
            }
        }
        finally
        {
            writer.close();
        }
    }

    // ---------------------------------------- Template applySubstitutions

    public static String applySubstitutions(String templateText, Dictionary<String, String> subDict)
    {
        // Substitutions are of the form "{{expression}}", where the simplest
        // expression is simply a named variable.  We split at "{{" or "}}"
        // which gives us an array where odd locations (1, 3, 5, ...) are variable
        // names to be substituted, and even locations are literal text runs which
        // may be empty strings but usually aren't.
        List<String> fragmentList = splitAtTopLevelBracePairs(templateText);

        // Replace each odd location in the array with its substitution.
        for (int i = 1; i < fragmentList.size(); i += 2)
        {
            String key = fragmentList.get(i);
            String value = computeSubstitution(key, subDict);
            fragmentList.set(i, value);
        }
    
        // Re-assemble the fragments into the final output text.
        StringBuffer sbFinalText = new StringBuffer();
        for (String fragment : fragmentList)
        {
            sbFinalText.append(fragment);
        }
        
        // Return the result.
        return sbFinalText.toString();
    }

    public static void L1TEST_applySubstitutions()
    {       
        Dictionary<String, String> subDict = new Hashtable<String, String>();
        subDict.put("foo", "FOO");
        subDict.put("bar", "BAR");
        assert applySubstitutions("{{foo}}{{bar}}", subDict).equals("FOOBAR");
        assert applySubstitutions("alpha{{foo}}{{bar}}", subDict).equals("alphaFOOBAR");
        assert applySubstitutions("alpha{{foo}}bravo{{bar}}", subDict).equals("alphaFOObravoBAR");
        assert applySubstitutions("alpha{{foo}}bravo{{bar}}charlie", subDict).equals("alphaFOObravoBARcharlie");
        assert applySubstitutions("alpha{{foo}}{{bar}}charlie", subDict).equals("alphaFOOBARcharlie");
        
    }

    // ---------------------------------------- Template splitAtTopLevelBracePairs
    
    private static List<String> splitAtTopLevelBracePairs(String text)
    {
        int level = 0;
        List<String> fragmentList = new ArrayList<String>();
        if (text.length() == 0) return fragmentList;
        StringBuffer fragmentBuffer = new StringBuffer();
        fragmentBuffer.append(text.charAt(0));
        for (int i = 1; i < text.length(); i++)
        {
            char ch1 = text.charAt(i - 1);
            char ch2 = text.charAt(i);
            fragmentBuffer.append(ch2);
            if (  (ch1 == '{' && ch2 == '{')  ||  (ch1 == '}' && ch2 == '}')  )
            {
                // We need to "pop" *before* we check for the end of a fragment.
                if (ch1 == '}')
                {
                    level--;
                }

                // Check for the end of a fragment.
                if (level == 0)
                {
                    String fragment = fragmentBuffer.substring(0, fragmentBuffer.length() - 2);
                    fragmentList.add(fragment);
                    fragmentBuffer.setLength(0);
                }
                
                // We need to "push" *after* we check for the end of a fragment.
                if (ch1 == '{')
                {
                    level++;
                }

                // In cases where we have cccc, we do not want to match c(cc)c!
                i++;
                if (i < text.length())
                {
                    fragmentBuffer.append(text.charAt(i));
                }           
            }           
        }
        
        String finalFragment = fragmentBuffer.toString();
        fragmentList.add(finalFragment);
        
        return fragmentList;
    }

    public static void L1TEST_split()
    {
        assert splitAtTopLevelBracePairs("{{foo}}").get(1).equals("foo");
        assert splitAtTopLevelBracePairs("alpha{{foo}}").get(1).equals("foo");
        assert splitAtTopLevelBracePairs("alpha{{foo}}bravo").get(1).equals("foo");
        assert splitAtTopLevelBracePairs("{{foo}}bravo").get(1).equals("foo");
    
        assert splitAtTopLevelBracePairs("{{foo}}{{bar}}").get(1).equals("foo");
        assert splitAtTopLevelBracePairs("alpha{{foo}}{{bar}}").get(1).equals("foo");
        assert splitAtTopLevelBracePairs("alpha{{foo}}bravo{{bar}}").get(1).equals("foo");
        assert splitAtTopLevelBracePairs("alpha{{foo}}bravo{{bar}}charlie").get(1).equals("foo");
        assert splitAtTopLevelBracePairs("alpha{{foo}}{{bar}}charlie").get(1).equals("foo");
    
        assert splitAtTopLevelBracePairs("{{foo}}{{bar}}").get(3).equals("bar");
        assert splitAtTopLevelBracePairs("alpha{{foo}}{{bar}}").get(3).equals("bar");
        assert splitAtTopLevelBracePairs("alpha{{foo}}bravo{{bar}}").get(3).equals("bar");
        assert splitAtTopLevelBracePairs("alpha{{foo}}bravo{{bar}}charlie").get(3).equals("bar");
        assert splitAtTopLevelBracePairs("alpha{{foo}}{{bar}}charlie").get(3).equals("bar");
        
        assert splitAtTopLevelBracePairs("{{{{foo}}}}").get(1).equals("{{foo}}");
        assert splitAtTopLevelBracePairs("{{{{foo}}{{bar}}}}").get(1).equals("{{foo}}{{bar}}");
        assert splitAtTopLevelBracePairs("{{{{{{foo}}{{bar}}}}}}").get(1).equals("{{{{foo}}{{bar}}}}");
        assert splitAtTopLevelBracePairs("{{alpha{{foo}}}}").get(1).equals("alpha{{foo}}");
        assert splitAtTopLevelBracePairs("{{alpha{{foo}}bravo}}").get(1).equals("alpha{{foo}}bravo");
        assert splitAtTopLevelBracePairs("{{{{foo}}bravo}}").get(1).equals("{{foo}}bravo");
    }

    // ---------------------------------------- Template computeSubstitution
    
    private static String computeSubstitution(String expression, Dictionary<String, String> subDict)
    {
        // We support a simple conditional construct on the queryString:
        // {{queryStringValue??text-to-use-on-match::text-to-use-on-mismatch}}.
        // When the <text-to-use-on-mismatch> is the empty string, the :: may be omitted.
        int qqIndex = expression.indexOf("??");
        if (qqIndex >= 0)
        {
            String conditionalKey = expression.substring(0, qqIndex);
            String valueExpression = expression.substring(qqIndex + 2);
            String thenValue = valueExpression;
            String elseValue = "";
            int ccIndex = valueExpression.indexOf("::");
            if (ccIndex >= 0)
            {
                thenValue = valueExpression.substring(0, ccIndex);
                elseValue = valueExpression.substring(ccIndex + 2);
            }
            
            String resultValue = thenValue;
            if (!subDict.get("queryString").equals(conditionalKey))
            {
                resultValue = elseValue;
            }

            // Recursively apply substitutions to the sub-text.
            resultValue = Template.applySubstitutions(resultValue, subDict);
            return resultValue;
        }

        // It was a simple substitution, or possibly a syntax error.
        String value = subDict.get(expression);
        if (value == null)
        {
            // Unknown variable, or possibly a syntax error.
            value = "{{" + expression + "}}";
        }

        return value;
    }

    public static void L1TEST_computeSubstitution()
    {
        Dictionary<String, String> subDict = new Hashtable<String, String>();
        subDict.put("foo", "FOO");
        subDict.put("bar", "BAR");
        subDict.put("queryString", "test");

        assert computeSubstitution("foo", subDict).equals("FOO");
        assert computeSubstitution("bar", subDict).equals("BAR");
        assert computeSubstitution("baz", subDict).equals("{{baz}}");
        assert computeSubstitution("test??yes", subDict).equals("yes");
        assert computeSubstitution("testx??yes", subDict).equals("");
        assert computeSubstitution("testx??yes::", subDict).equals("");
        assert computeSubstitution("testx??yes::no", subDict).equals("no");
    }
    
}
