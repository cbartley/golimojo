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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.golimojo.QdmlParser.QdmlFragment;

public class HttpReader
{

    // ---------------------------------------- HttpReader readAndParseHttpFile

    public static List<QdmlParser.QdmlFragment> readAndParseHttpFile(String urlString) throws IOException
    {
        URL url = new URL(urlString);
        
        String[] encodingInOut = new String[1];
        encodingInOut[0] = null;

        long startTimeMs = System.currentTimeMillis();
        List<QdmlParser.QdmlFragment> fragmentList = readAndParse(url, encodingInOut);
        if (fragmentList == null)
        {
            // There was an encoding mismatch.  The previous call stored the new
            // encoding in "encodingInOut[0]", so all we have to do is try again.
            fragmentList = readAndParse(url, encodingInOut);
        }
        long elapsedTimeMs = System.currentTimeMillis() - startTimeMs;
        System.out.printf("### %s: elapsed time: %1.2f\n", HttpReader.class.getName(), (0.001 * elapsedTimeMs));

        if (fragmentList != null) return fragmentList;
    
        throw new IOException("Could not figure out encoding: " + urlString);
    }

    // ---------------------------------------- HttpReader readAndParse
    
    // If "encodingInOut[0]" is null, this method will try to determine the encoding.  If 
    // it guesses the wrong encoding and later detects the mistake, it will store the
    // new encoding in "encodingInOut[0]" and will return null.

    public static List<QdmlFragment> readAndParse(URL url, String[] encodingInOut) throws IOException
    {
        // Open the connection and get the encoding if one hasn't already been specified.
        URLConnection connection = url.openConnection();
        String encoding = encodingInOut[0];
        if (encoding == null)
        {
            encoding = getEncoding(connection);
            if (!Charset.availableCharsets().containsKey(encoding))
            {
                encoding = "ISO-8859-1";  // Usable for most Western European languages.
            }
        }

        InputStream inputStream = new BufferedInputStream(connection.getInputStream());
        InputStreamReader reader = new InputStreamReader(inputStream, encoding);
        try
        {
            List<QdmlFragment> fragmentList = readAndParseFromReader(reader, encodingInOut);
            return fragmentList;
        }
        finally
        {
            reader.close();
        }       
    }

    // ---------------------------------------- HttpReader readAndParseFromReader
    
    // This method assumes that the reader has been created using the encoding indicated
    // by "encodingInOut[0]".  If an explicit encoding is found on a META tag, and that
    // encoding is different, this method will copy the new encoding into "encodingInOut[0]"
    // and will then immediately return null.
    
    public static List<QdmlFragment> readAndParseFromReader(Reader reader, String[] encodingInOut) 
        throws IOException
    {
        QdmlParser parser = new QdmlParser();
        List<QdmlFragment> fragmentList = new ArrayList<QdmlFragment>();
        while (true)
        {
            int chInt = reader.read();
            QdmlFragment fragment = parser.parseCharacter(chInt);
            if (fragment != null)
            {
                // A meta-tag could tell us we're using the wrong encoder -- bail if that's the case.
                if (checkForExplicitEncoding(fragment, encodingInOut)) return null;
                fragmentList.add(fragment);
            }
            if (chInt == -1) break;
        }
        
        return fragmentList;
    }

    // ---------------------------------------- HttpReader getEncoding

    private static String getEncoding(URLConnection openConnection)
    {
        // Try to figure out the character encoding.  There must be some better
        // way to do this, but if so I haven't figured it out yet.
        String encoding = "UTF-8";  // assume this as the default.
        String contentType = openConnection.getHeaderField("Content-Type");
        if (contentType != null)
        {
            String marker = "charset=";
            int index = contentType.indexOf(marker);
            if (index != -1)
            {
                encoding = contentType.substring(index + marker.length());
            }
        }
        return encoding;
    }

    // ---------------------------------------- HttpReader checkForExplicitEncoding
        
    private static boolean checkForExplicitEncoding(QdmlFragment fragment, String[] encodingInOut)
    {
        if (fragment.isStartTagCi("META"))
        {
            String tagText = fragment.getText().toLowerCase();
            if (tagText.indexOf("http-equiv") == -1) return false;
            if (tagText.indexOf("content-type") == -1) return false;

            Pattern pattern = Pattern.compile(".*charset=([a-z0-9-]*).*");
            Matcher matcher = pattern.matcher(tagText);
            if (matcher.matches())
            {
                String explicitEncoding = matcher.group(1);
                
                // Don't report a change in encoding if we don't know the explicit encoding anyway.
                if (!Charset.availableCharsets().containsKey(explicitEncoding)) return false;
                
                // If we're already using the explicit encoding, report no change.
                if (explicitEncoding.equalsIgnoreCase(encodingInOut[0])) return false;
                
                // It's a new encoding and it's one we know, so report it.
                encodingInOut[0] = explicitEncoding;
                return true;
            }
        }
        return false;
    }

}
