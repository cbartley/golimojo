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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

// ------------------------------------------------------------
// --------------------- class ShellTools ---------------------
// ------------------------------------------------------------

public class ShellTools
{
    // ---------------------------------------- ShellTools system #1

    public static int system(String cmdLineTemplate, Substitution... arguments)
    {
        return system(cmdLineTemplate, null, arguments);
    }

    // ---------------------------------------- ShellTools system #2

    public static int system(String cmdLineTemplate, String[] extraEnvVariables, Substitution... arguments)
    {
        String cmdLine = substitute(cmdLineTemplate, arguments);
        System.out.println("[" + cmdLine + "]");
        try
        {
            Process process = Runtime.getRuntime().exec(cmdLine, createEnviroment(extraEnvVariables));
            InputStream inputStreamFromProcessOutput = process.getInputStream();
            InputStream inputStreamFromProcessErrorOutput = process.getErrorStream();
            
            while (true)
            {
                int chInt = inputStreamFromProcessOutput.read();
                if (chInt == -1) break;
                System.out.print((char)chInt);
            }
            
            while (true)
            {
                int chInt = inputStreamFromProcessErrorOutput.read();
                if (chInt == -1) break;
                System.out.print((char)chInt);
            }
            
            return process.waitFor();
        } 
        catch (InterruptedException e)
        {
            reportErrorAndQuit("Command shell call was interrupted: {exception}", sub("exception", e.toString()));
            return 0;
        }
        catch (IOException e)
        {
            reportErrorAndQuit("Command shell call I/O error: {exception}", sub("exception", e.toString()));
            return 0;
        }
    }
    
    // ---------------------------------------- ShellTools reportErrorAndQuit
    
    public static void reportErrorAndQuit(String errMsgTemplate, Substitution... substitutions)
    {
        String errMsg = substitute(errMsgTemplate, substitutions);
        System.out.println("### Fatal Error ###");
        System.out.println(errMsg);
        System.exit(1);
    }
    
    // ---------------------------------------- ShellTools sub
    
    public static Substitution sub(String name, String value)
    {
        return new Substitution(name, value);
    }
    
    // ---------------------------------------- ShellTools substitute
    
    public static String substitute(String template, Substitution... arguments)
    {
        for (Substitution substitution : arguments)
        {
            template = substitution.substitute(template);
        }
        return template;
    }

    // ---------------------------------------- ShellTools Substitution
    
    public static class Substitution
    {
        private final String myName;
        private final String myValue;
        
        public Substitution(String name, String value)
        {
            myName = name;
            myValue = value;
        }
        
        public String substitute(String template)
        {
            return template.replace("{" + myName + "}", myValue);
        }
    }

    // ---------------------------------------- ShellTools createEnviroment

    private static String[] createEnviroment(String[] extraEnvVariables)
    {
        if (extraEnvVariables == null) return null;
        
        // Use a process builder to get the current environment.
        ProcessBuilder processBuilder = new ProcessBuilder("");
        Map<String, String> environment = processBuilder.environment();
        
        // Add the extra environment variables to the environment.
        for (String envVariable : extraEnvVariables)
        {
            int indexEqualSign = envVariable.indexOf('=');
            assert indexEqualSign > 0;
            String key = envVariable.substring(0, indexEqualSign);
            String value = envVariable.substring(indexEqualSign + 1);
            environment.put(key, value);
        }

        // Translate the environment to array of assignments form.
        List<Entry<String, String>> entryList = new ArrayList<Entry<String, String>>(environment.entrySet());
        String[] envVariables = new String[entryList.size()];
        for (int i = 0; i < entryList.size(); i++)
        {
            Entry<String, String> entry = entryList.get(i);
            envVariables[i] = entry.getKey() + "=" + entry.getValue();
        }
        
        // Return the enviroment in array of assignments form.
        return envVariables;
    }

}
