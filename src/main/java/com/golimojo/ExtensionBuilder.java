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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

// ------------------------------------------------------------
// ------------------ class ExtensionBuilder ------------------
// ------------------------------------------------------------

public class ExtensionBuilder extends ShellTools
{

    // ---------------------------------------- ExtensionBuilder main
    
    public static void main(String[] args) throws IOException
    {
        createFirefoxExtension("web-root/extension", "golimojo", "localhost", 8085);
    }

    // ---------------------------------------- ExtensionBuilder createFirefoxExtension

    public static void createFirefoxExtension(  String pathToExtensionFolder, String extensionName, 
                                                String serverName, int serverPort )
        throws IOException
    {
        Dictionary<String, String> subDict = new Hashtable<String, String>();
        subDict.put("serverName", serverName);
        subDict.put("serverPort", Integer.toString(serverPort));
        Template.preprocessFileTree(new File(pathToExtensionFolder), subDict);
        createXpiJarFile(pathToExtensionFolder + "/chrome", extensionName + ".jar");
        createXpi(pathToExtensionFolder, extensionName);
    }

    // ---------------------------------------- ExtensionBuilder createXpi
    
    private static void createXpi(String pathToExtensionFolder, String extensionName)
    {
        String cmdLineCreateXpi = 
            "jar cvfM {extFolder}/{extName}.xpi -C {extFolder} chrome/{extName}.jar -C {extFolder} install.rdf";
        system(cmdLineCreateXpi, sub("extFolder", pathToExtensionFolder), sub("extName", extensionName));       
    }

    // ---------------------------------------- ExtensionBuilder createXpiJarFile
    /** Create a jar file inside the directory "pathToFolder" which contains everything
     *  inside the directory except for the new jar file itself.  The relative paths
     *  inside the jar file will be relative to "pathToFolder", e.g. 
     *  {pathToFolder}/content/example.txt will just be stored as "content/example.text".
     */
    private static void createXpiJarFile(String pathToFolder, String jarFileName)
    {
        List<String> filesAndFoldersList = listFilesAndFolders(pathToFolder, jarFileName);
        String includeArgs = prefixJoin(" -C " + pathToFolder + " ", filesAndFoldersList);
        String cmdLineCreateJar = "jar cvfM {pathToFolder}/{jarFileName} {includeArgs}";
        system( cmdLineCreateJar, sub("pathToFolder", pathToFolder), 
                sub("jarFileName", jarFileName), sub("includeArgs", includeArgs));
    }
    
    // ---------------------------------------- ExtensionBuilder prefixJoin
    
    private static String prefixJoin(String prefix, List<String> stringList)
    {
        StringBuffer sbJoin = new StringBuffer();
        for (String string : stringList)
        {
            sbJoin.append(prefix);
            sbJoin.append(string);
        }
        return sbJoin.toString();
    }

    // ---------------------------------------- ExtensionBuilder listFilesAndFolders
    
    private static List<String> listFilesAndFolders(String pathToFolder, String... excludes)
    {
        List<String> excludeList = Arrays.asList(excludes);
        HashSet<String> excludeSet = new HashSet<String>(excludeList);
        
        List<String> ffNameList = new ArrayList<String>();
        File folder = new File(pathToFolder);
        for (String ffName : folder.list())
        {
            if (!excludeSet.contains(ffName))
            {
                ffNameList.add(ffName);
            }
        }
        
        return ffNameList;      
    }

}
