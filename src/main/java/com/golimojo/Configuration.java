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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

// ------------------------------------------------------------
// ------------------- class Configuration --------------------
// ------------------------------------------------------------

/**
 * Singleton class for accessing the global configuration.  The configuration is stored
 * in a Java properties file in the current working directory (at startup).  Overrides
 * may be stored in a Java properties file in the user's home directory.
 */
public class Configuration {

    // ---------------------------------------- Configuration constants

    private static final String CONFIG_FILENAME = "golimojo.properties";
    private static final String PROPERTIES_FOLDER = System.getProperty("user.dir");
    private static final String OVERRIDE_PROPERTIES_FOLDER = System.getProperty("user.home");

    // ---------------------------------------- Configuration class variables
    
    private static final Configuration our_Configuration = new Configuration(readCombinedProperties());

    // ---------------------------------------- Configuration instance variables

    private final String _siteName;

    // ---------------------------------------- Configuration get

    public static Configuration get()
    {
        return our_Configuration;
    }

    // ---------------------------------------- Configuration constructor

    private Configuration(Properties properties)
    {
        _siteName = properties.getProperty("siteName", "Golimojo");
    }

    // ---------------------------------------- Configuration getSiteName

    public String getSiteName()
    {
        return _siteName;
    }

    // ---------------------------------------- Configuration writeConfiguration
    
    public static void writeConfiguration(String pathToFile) throws FileNotFoundException, IOException
    {
        Properties p = readCombinedProperties();
        p.store(new FileOutputStream(pathToFile), "");
    }

    // ---------------------------------------- Configuration readCombinedProperties

    private static Properties readCombinedProperties()
    {
        Properties properties = readProperties(PROPERTIES_FOLDER, CONFIG_FILENAME);
        Properties overrides = readProperties(OVERRIDE_PROPERTIES_FOLDER, CONFIG_FILENAME);
        properties.putAll(overrides);
        return properties;
    }

    // ---------------------------------------- Configuration readCombinedProperties

    private static Properties readProperties(String pathToFolder, String propertiesFileName)
    {
        Properties properties = new Properties();
        File propertiesFile = new File(new File(pathToFolder), propertiesFileName);
        if (!propertiesFile.exists()) return properties;
        try
        {
            properties.load(new FileInputStream(propertiesFile));
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
        }
        return properties;      
    }
}
