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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.*;

// ------------------------------------------------------------
// ----------------- class GolimojoXpiServlet -----------------
// ------------------------------------------------------------
/**
 *  This servlet builds a configuration-specific Golimojo XPI file 
 *  and serves it to the client.  The XPI file is the installation
 *  package for the Golimojo Firefox extension.  Configuration
 *  parameters are simply drawn from the HTTP request.
 */

@SuppressWarnings("serial")
public class GolimojoXpiServlet extends HttpServlet
{
    // ---------------------------------------- GolimojoXpiServlet doGet
    
    public final void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        // Set the appropriate content type for an XPI file.
        response.setContentType("application/x-xpinstall");
        
        // Turn of caching.
        response.setDateHeader("Expires", 0);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");

        // Create the Firefox extension (if necessary) and get the path to it.
        // Note that the extension is customized to the particular server name
        // and port that the download request came in on.
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String pathToXpiFile = FirefoxExtensionConfigurationFactory.lazyCreateXpi(serverName, serverPort);

        // Copy the firefox extension file to the output.
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(pathToXpiFile));
        try
        {
            BufferedOutputStream os = new BufferedOutputStream(response.getOutputStream());
            try
            {
                while (true)
                {
                    int octetInt = is.read();
                    if (octetInt == -1) break;
                    os.write(octetInt);
                }
            }
            finally
            {
                os.close();
            }       
        }
        finally
        {
            is.close();
        }
    }
}
