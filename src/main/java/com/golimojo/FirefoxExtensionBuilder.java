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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

// ------------------------------------------------------------
// --------------- class FirefoxExtensionBuilder --------------
// ------------------------------------------------------------
/**
 *  This class automates the packaging of component files into a an "XPI" 
 *  file, which is an installation package for a Firefox extension.
 */
public class FirefoxExtensionBuilder extends ShellTools
{
    // ---------------------------------------- FirefoxExtensionBuilder createFirefoxExtension

    public static void createFirefoxExtension(  String pathToExtensionFolder, String xpiName, String extensionName, 
                                                String serverName, int serverPort )
        throws IOException
    {
        Dictionary<String, String> subDict = new Hashtable<String, String>();
        subDict.put("serverName", serverName);
        subDict.put("serverPort", Integer.toString(serverPort));
        subDict.put("buildId", Long.toString(System.currentTimeMillis()));
        Template.preprocessFileTree(new File(pathToExtensionFolder), subDict);
        createXpiJarFile(pathToExtensionFolder + "/chrome", extensionName + ".jar");
        createXpi(pathToExtensionFolder, xpiName, extensionName);
    }

    // ---------------------------------------- FirefoxExtensionBuilder createXpi
    
    private static void createXpi(String pathToExtensionFolder, String xpiName, String extensionName)
    {
        String cmdLineCreateXpi = 
            "jar cvfM {extFolder}/{xpiName}.xpi -C {extFolder} chrome/{extName}.jar -C {extFolder} install.rdf -C {extFolder} chrome.manifest";
        system(cmdLineCreateXpi, sub("extFolder", pathToExtensionFolder), sub("xpiName", xpiName), sub("extName", extensionName));      
    }

    // ---------------------------------------- FirefoxExtensionBuilder createXpiJarFile
    /** 
     *  Create a jar file inside the directory "pathToFolder" which contains everything
     *  inside the directory except for the new jar file itself.  The relative paths
     *  inside the jar file will be relative to "pathToFolder", e.g. 
     *  {pathToFolder}/content/example.txt will just be stored as "content/example.text".
     */
    private static void createXpiJarFile(String pathToFolder, String jarFileName)
    {
        List<String> filesAndFoldersList = listFilesAndFolders(pathToFolder);
        filesAndFoldersList = excludeJarFiles(filesAndFoldersList);
        String includeArgs = prefixJoin(" -C " + pathToFolder + " ", filesAndFoldersList);
        String cmdLineCreateJar = "jar cvfM {pathToFolder}/{jarFileName} {includeArgs}";
        system( cmdLineCreateJar, sub("pathToFolder", pathToFolder), 
                sub("jarFileName", jarFileName), sub("includeArgs", includeArgs));
    }
    
    // ---------------------------------------- FirefoxExtensionBuilder prefixJoin
    
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

    // ---------------------------------------- FirefoxExtensionBuilder listFilesAndFolders
    
    private static List<String> listFilesAndFolders(String pathToFolder)
    {
        List<String> ffNameList = new ArrayList<String>();
        File folder = new File(pathToFolder);
        for (String ffName : folder.list())
        {
            ffNameList.add(ffName);
        }
        
        return ffNameList;      
    }

    // ---------------------------------------- FirefoxExtensionBuilder excludeJarFiles
    
    private static List<String> excludeJarFiles(List<String> ffNameList)
    {
        List<String> ffNameListOut = new ArrayList<String>();
        for (String ffName : ffNameList)
        {
            if (!ffName.toLowerCase().endsWith(".jar"))
            {
                ffNameListOut.add(ffName);
            }
        }
        return ffNameListOut;
    }
    
}
