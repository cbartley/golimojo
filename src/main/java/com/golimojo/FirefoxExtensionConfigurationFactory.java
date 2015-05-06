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
import java.util.HashSet;
import java.util.Set;

// ------------------------------------------------------------
// -------- class FirefoxExtensionConfigurationFactory --------
// ------------------------------------------------------------

class FirefoxExtensionConfigurationFactory
{
    // ---------------------------------------- FirefoxExtensionConfigurationFactory class variables

    private static String our_extensionName = "golimojo";
    private static String our_pathToExtensionFolder = "web-root/extension";
    private static Set<String> our_cacheSet = new HashSet<String>();

    // ---------------------------------------- FirefoxExtensionConfigurationFactory lazyCreateXpi
    /**
     *  This method creates an XPI (Firefox extension package) on the first call for
     *  each unique configuration.  All subsequent calls with that same configuration
     *  simply re-use the same XPI.  We do it this way because there is no way to know
     *  for sure what server name and port are going to be used until we get the first
     *  request.  In fact several different server names and ports might be used by
     *  a single service.  This way we can handle whatever configurations without 
     *  arbitrary limitations.  Also note that this function is synchronized, so if
     *  multiple requests come in close together, then the first request will cause 
     *  the XPI to be built and the subsequent requests will wait until the build is
     *  complete.
     */
    public synchronized static String lazyCreateXpi(String serverName, int serverPort)
        throws IOException
    {
        // Create the unique name for this configuration, then create the full path name.
        String uniqueXpiName = createUniqueName(our_extensionName, serverName, serverPort);
        String uniqueXpiPathName = our_pathToExtensionFolder + "/" + uniqueXpiName + ".xpi";

        // If we haven't built this configuration yet, then build it and update the cache.
        if (!our_cacheSet.contains(uniqueXpiPathName))
        {
            FirefoxExtensionBuilder.createFirefoxExtension( our_pathToExtensionFolder, uniqueXpiName, 
                                                            serverName, serverPort );
            our_cacheSet.add(uniqueXpiPathName);
        }

        // Return the path name to the XPI for this configuration.
        return uniqueXpiPathName;
    }

    // ---------------------------------------- FirefoxExtensionConfigurationFactory lazyCreateXpi

    private static String createUniqueName(String name, String serverName, int serverPort)
    {
        return name + "_" + serverName + "_" + serverPort;
    }

    // ---------------------------------------- FirefoxExtensionConfigurationFactory test code
    
    // These tests are sufficiently slow that we don't want to run them every 
    // time, so we use the L1B prefix to differentiate them from the regular
    // L1 tests.
    
    public static void L1BTEST_FirefoxExtensionConfigurationFactory() throws IOException
    {
        // Verify that basic construction and caching work.
        String pathToXpiFile1 = lazyCreateXpi("test", 999001);
        assert our_cacheSet.contains(pathToXpiFile1);
        assert new File(pathToXpiFile1).exists();
        
        // Verify that a second request for the same configuration
        // utitlizes the cache.  Note that this test isn't 100%
        // deterministic, but it's probably good enough for now.
        long startTime = System.currentTimeMillis();
        lazyCreateXpi("test", 999001);
        long elapsedTime = System.currentTimeMillis() - startTime;
        assert elapsedTime <= 2;

        // Verify that changing the server name or the port also
        // changes the configuration.
        String pathToXpiFile2 = lazyCreateXpi("test", 999002);
        assert new File(pathToXpiFile2).exists();
        assert pathToXpiFile2 != pathToXpiFile1;
        assert pathToXpiFile2 != lazyCreateXpi("testb", 999002);
    }

}

