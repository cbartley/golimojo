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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Deployer extends ShellTools
{
    // ---------------------------------------- Deployer main

    public static void main(String[] args) throws IOException
    {
        System.out.println("Begin deployment? (y/n)");
        
        StringBuffer sbReadLine = new StringBuffer();
        while (true)
        {
            int chInt = System.in.read();
            if (chInt == -1) break;
            char ch = (char)chInt;
            if (ch == '\r' || ch == '\n') break;
            sbReadLine.append(ch);
        }
        
        String readLine = sbReadLine.toString().trim();
        if (!readLine.equalsIgnoreCase("y")) 
        {
            System.out.println("### Deployment Aborted!!!");
            return;
        }

        final String currDir = System.getProperty("user.dir");
    
        final String SERVER_TEMPLATE_FOLDER_PATH = "service-template";
        final String DEPLOYMENT_PARENT_FOLDER_PATH;
        if (onWindows())
        {
            DEPLOYMENT_PARENT_FOLDER_PATH = "c:/deploy";
        }
        else if (onOSX())
        {
            DEPLOYMENT_PARENT_FOLDER_PATH = currDir + "/testing";
        }
        else if (onLinux())
        {
            DEPLOYMENT_PARENT_FOLDER_PATH = "/home/curtisb";
        }
        else
        {
            DEPLOYMENT_PARENT_FOLDER_PATH = "/home/curtisb";
            reportErrorAndQuit("Unsupported OS!");
        }
    
        final String DEPLOYMENT_FOLDER_PATH = DEPLOYMENT_PARENT_FOLDER_PATH + "/service";
        final String DEPLOYMENT_JAR_FOLDER_PATH = DEPLOYMENT_FOLDER_PATH + "/lib";

        String pathToPlistFile = DEPLOYMENT_FOLDER_PATH + "/golimojo.plist";

        // Get our full classpath.
        String classPath = System.getProperty("java.class.path");
        
        // If the service exists, make sure it's stopped and remove it.
        if (onOSX())
        {
            system("launchctl unload {pathToPlistFile}", sub("pathToPlistFile", pathToPlistFile));
        }
        else
        {
            System.err.printf("*** Don't know how to start and stop the service on this system! ***\n");
        }

        // Build the deployment folder structure.
        deleteFolder(DEPLOYMENT_FOLDER_PATH);
        copyFolder(SERVER_TEMPLATE_FOLDER_PATH, DEPLOYMENT_FOLDER_PATH);
        createFolder(DEPLOYMENT_JAR_FOLDER_PATH);
        copyFolder("../golimojo/web-root", DEPLOYMENT_FOLDER_PATH + "/web-root");
        copyFolder("../golimojo/resource-root", DEPLOYMENT_FOLDER_PATH + "/resource-root");

        // Deploy the jar files and configure the config file.
        List<String> pathToDeployedJarFileList = deployJarFiles(classPath, DEPLOYMENT_JAR_FOLDER_PATH);

        editPlistFile(pathToPlistFile, DEPLOYMENT_FOLDER_PATH, pathToDeployedJarFileList);
        
        // Write out the properties file.
        com.golimojo.Configuration.writeConfiguration(DEPLOYMENT_FOLDER_PATH + "/golimojo.properties");

        // Install and start the new service.
        if (onOSX()) 
        {
            system("launchctl load {pathToPlistFile}", sub("pathToPlistFile", pathToPlistFile));
        }
        else
        {
            System.err.printf("*** Don't know how to start and stop the service on this system! ***\n");
        }
    }

    // ---------------------------------------- Deployer deployJarFiles
    
    private static List<String> deployJarFiles(String classPath, String pathToJarDeploymentFolder)
    {
        // Break the classpath up into its individual components.
        List<String> classPathComponentList = new ArrayList<String>(Arrays.asList(classPath.split(File.pathSeparator)));

        // We will build a list of paths to the deployed jar files.
        List<String> pathToDeployedJarFileList = new ArrayList<String>();
        
        // Jar up un-jarred class trees.
        for (String classPathComponent : classPathComponentList)
        {
            if (!classPathComponent.toLowerCase().endsWith(".jar"))
            {
                String jarFileName = guessProjectName(classPathComponent) + ".jar";
                String pathToDeployedJarFile = pathToJarDeploymentFolder + "/" + jarFileName;
                createJarFile(classPathComponent, pathToDeployedJarFile);
                pathToDeployedJarFileList.add(pathToDeployedJarFile);
            }
        }
        
        // Just copy the already jarred files.
        for (String classPathComponent : classPathComponentList)
        {
            if (classPathComponent.toLowerCase().endsWith(".jar"))
            {
                List<String> pathAsList = splitPathAtSeparators(classPathComponent);
                String jarFileName = pathAsList.get(pathAsList.size() - 1);
                String pathToDeployedJarFile = pathToJarDeploymentFolder + "/" + jarFileName;
                copyFile(classPathComponent, pathToDeployedJarFile);
                pathToDeployedJarFileList.add(pathToDeployedJarFile);
            }
        }

        return pathToDeployedJarFileList;
    }
    
    // ---------------------------------------- Deployer createJarFile

    private static void createJarFile(String pathToClassFolder, String pathToJarFile)
    {
        String cmdLineTemplate = "jar cvf {pathToJarFile} -C {pathToClassFolder}/ .";
        system(cmdLineTemplate, sub("pathToClassFolder", pathToClassFolder), sub("pathToJarFile", pathToJarFile));
    }
    
    // ---------------------------------------- Deployer guessProjectName
    
    private static String guessProjectName(String pathToFolderInProject)
    {
        // If we can guess an eclipse project name, then just use that.
        String projectName = guessEclipseProjectName(pathToFolderInProject);
        if (projectName != null) return projectName;
        
        List<String> pathAsList = splitPathAtSeparators(pathToFolderInProject);
        String projectNameMajor = pathAsList.get(pathAsList.size() - 2);
        String projectNameMinor = pathAsList.get(pathAsList.size() - 1);
        return projectNameMajor + "-" + projectNameMinor;
    }
    
    // ---------------------------------------- Deployer guessEclipseProjectName
    
    private static String guessEclipseProjectName(String pathToFolderInProject)
    {
        List<String> pathAsList = splitPathAtSeparators(pathToFolderInProject);
        for (int i = pathAsList.size() - 2; i >= 0; i--)
        {
            String folderName = pathAsList.get(i);
            if (folderName.toLowerCase().endsWith("workspace"))
            {
                return pathAsList.get(i + 1);
            }
        }
        return null;
    }
    
    // ---------------------------------------- Deployer editPlistFile

    private static void editPlistFile(String pathToPlistFile, String workingDir, List<String> pathToDeployedJarFileList)
    {
        List<String> lineList = readLines(pathToPlistFile);
        String classPath = createClassPathString(pathToDeployedJarFileList);
        for (int i = 0; i < lineList.size(); i++)
        {
            String line = lineList.get(i);
            line = line.replace("<!--working-directory-goes-here-->", workingDir);
            line = line.replace("<!--classpath-goes-here-->", classPath);
            lineList.set(i, line);
        }
        writeLines(pathToPlistFile, lineList);
    }

    private static String createClassPathString(List<String> pathToDeployedJarFileList)
    {
        StringBuffer sbClassPath = new StringBuffer();
        for (int i = 0; i < pathToDeployedJarFileList.size(); i++)
        {
            if (i > 0) sbClassPath.append(':');
            sbClassPath.append(pathToDeployedJarFileList.get(i));
        }
        return sbClassPath.toString();
    }

    // ---------------------------------------- Deployer readLines

    private static List<String> readLines(String pathToFile)
    {
        List<String> lineList = new ArrayList<String>();
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(pathToFile));
            while (true)
            {
                String line = reader.readLine();
                if (line == null) break;
                lineList.add(line);
            }
            reader.close();
        } 
        catch (FileNotFoundException e)
        {
            reportErrorAndQuit("File not found: '{pathToFile}'!", sub("pathToFile", pathToFile));
        } 
        catch (IOException e)
        {
            reportErrorAndQuit("IOException while reading '{pathToFile}'!", sub("pathToFile", pathToFile));
        }
        
        return lineList;
    }

    // ---------------------------------------- Deployer writeLines

    private static void writeLines(String pathToFile, List<String> lineList)
    {
        try
        {
            PrintWriter writer = new PrintWriter(new FileWriter(pathToFile));
            for (String line : lineList)
            {
                writer.println(line);
            }
            writer.close();
        } catch (IOException e)
        {
            reportErrorAndQuit("Could not write file: '{pathToFile}'!", sub("pathToFile", pathToFile));
        }
    }

    // *** ---------------------------------------- Deployer splitPathAtSeparators

    private static List<String> splitPathAtSeparators(String path)
    {
        String[] pathAsArray = path.split("\\" + File.separator);
        return new ArrayList<String>(Arrays.asList(pathAsArray));
    }
    
    // ---------------------------------------- Deployer copyFile
    
    private static void copyFile(String sourcePath, String destPath)
    {
        if (onWindows())
        {
            String cmdLineTemplate = "cmd /c copy {sourcePath} {destPath}";
            system(cmdLineTemplate, sub("sourcePath", sourcePath), sub("destPath", destPath));
        }
        else
        {
            String cmdLineTemplate = "cp {sourcePath} {destPath}";
            system(cmdLineTemplate, sub("sourcePath", sourcePath), sub("destPath", destPath));
        }
    }
    
    // ---------------------------------------- Deployer createFolder

    private static void createFolder(String folderPath)
    {
        if (onWindows())
            system("cmd /c mkdir {folderPath}", sub("folderPath", folderPath));
        else 
            system("mkdir {folderPath}", sub("folderPath", folderPath));
    }
    
    // ---------------------------------------- Deployer deleteFolder

    private static void deleteFolder(String folderPath)
    {
        if (onWindows())
            system("cmd /c rmdir /S/Q {folderPath}", sub("folderPath", folderPath));
        else
            system("rm -rf {folderPath}", sub("folderPath", folderPath));
    }

    // ---------------------------------------- Deployer copyFolder
    
    private static void copyFolder(String sourceFolderPath, String destParentFolderPath)
    {
        if (onWindows())
        {
            String cmdLineTemplate = "XCOPY /S/E/Y {sourceFolderPath} {destParentFolderPath}/";
            system(cmdLineTemplate, sub("sourceFolderPath", sourceFolderPath), sub("destParentFolderPath", destParentFolderPath));
        }
        else
        {
            String cmdLineTemplate = "cp -r {sourceFolderPath} {destParentFolderPath}/";
            int result = system(cmdLineTemplate, sub("sourceFolderPath", sourceFolderPath), 
                                                    sub("destParentFolderPath", destParentFolderPath));
            if (result != 0)
            {
                reportErrorAndQuit("Failed file copy!");
            }
        }
    }

}
