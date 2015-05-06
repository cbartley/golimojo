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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* ------------------------------------------------------------ */
/* ----------------------- class Tester ----------------------- */
/* ------------------------------------------------------------ */

public class Tester 
{
    public Tester(String testPrefix)
    {       
        try
        {
            List<String> classNameList = listAllClasses();
            List<Class> classList = loadClasses(classNameList);
            for (Class metaClass : classList)
            {
                new TestSuite(metaClass, testPrefix);
            }
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static List<String> listAllClasses() throws IOException
    {
        String classPath = System.getProperty("java.class.path");
        List<String> classNameList = new ArrayList<String>();
        for (String classPathComponent : classPath.split(File.pathSeparator))
        {
            if (!classPathComponent.toLowerCase().endsWith(".jar"))
            {
                listClasses(classPathComponent, classNameList);
            }
        }

        return classNameList;
    }

    private static void listClasses(String pathToClassFolder, List<String> classNameReceiverList)
        throws IOException
    {
        File fdClassFolder = new File(pathToClassFolder);
        pathToClassFolder = fdClassFolder.getCanonicalPath();
        if (!pathToClassFolder.endsWith(File.separator))
        {
            pathToClassFolder = pathToClassFolder + File.separator;
        }
        
        List<File> fileList = new ArrayList<File>();
        recursiveListFiles(fdClassFolder, fileList);
        
        for (File file : fileList)
        {
            String pathToFile = file.getCanonicalPath();
            
            if (pathToFile.toLowerCase().endsWith(".class"))
            {
                String relativePathToFile = pathToFile.replace(pathToClassFolder, "");
                String className = relativePathToFile
                    .replace(".class", "")
                    .replace(File.separator, ".")
                    .replace("$", ".")
                ;
    
                classNameReceiverList.add(className);
            }
        }
    }

    private static void recursiveListFiles(File folder, List<File> fileReceiverList)
    {
        assert folder.isDirectory();
        File[] fdNodes = folder.listFiles();
        for (File fdNode : fdNodes)
        {
            if (fdNode.isDirectory())
            {
                recursiveListFiles(fdNode, fileReceiverList);
            }
            else
            {
                fileReceiverList.add(fdNode);
            }
        }
    }
    
    private static List<Class> loadClasses(List<String> classNameList) 
    {
        List<Class> classList = new ArrayList<Class>();
        for (String className : classNameList)
        {
            try
            {
                Class metaClass = Class.forName(className);
                classList.add(metaClass);
            } catch (ClassNotFoundException e)
            {
                // We just ignore them if we can't load them.
            }
        }
        return classList;
    }
}

/* ------------------------------------------------------------ */
/* ---------------------- class TestSuite --------------------- */
/* ------------------------------------------------------------ */

class TestSuite
{
    public TestSuite(Class metaClass, String testPrefix)
    {
        List<Method> testMethodList = getTestMethods(metaClass, testPrefix);
        if (testMethodList.size() > 0)
        {
            System.out.println("Testing: " + metaClass);
            callMethods(testMethodList);
            System.out.println();
        }
    }

    private static List<Method> getTestMethods(Class metaClass, String testPrefix)
    {
        List<Method> methodList = new ArrayList<Method>();
        String fullTestPrefix = testPrefix + "_";
        Method[] methods = metaClass.getDeclaredMethods();
        for (Method method : methods)
        {
            if (method.getName().startsWith(fullTestPrefix))
            {
                methodList.add(method);
            }
        }
        Collections.reverse(methodList);
        return methodList;
    }

    private static void callMethods(List<Method> methodList)
    {
        Object[] args = new Object[] {};
        for (Method method : methodList)
        {
            try
            {
                System.out.println("... " + method);
                method.invoke(null, args);
            }
            catch (Throwable t)
            {
                t.printStackTrace();
                break;
            }
        }
    }
}
