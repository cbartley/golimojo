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

import java.util.ArrayList;
import java.util.List;

// ------------------------------------------------------------
// ---------------------- class QuickLog ----------------------
// ------------------------------------------------------------

public class QuickLog
{
    private final int maxLogSize = 10 * 1000;
    private RollingList<LogEntry> _rollingLogEntryList = new RollingList<LogEntry>(maxLogSize);
    
    
    public void logRequest(String clientAddress, String url)
    {
        LogEntry logEntry = new LogEntry(clientAddress, url);
        _rollingLogEntryList.add(logEntry);
    }
    
    public List<LogEntry> copyToList()
    {
        return _rollingLogEntryList.copyToList();
    }

    public static class LogEntry
    {
        private final String _clientAddress;
        private final String _url;
        
        public LogEntry(String clientAddress, String url)
        {
            _clientAddress = clientAddress;
            _url = url;
        }

        public String getClientAddress()
        {
            return _clientAddress;
        }

        public String getUrl()
        {
            return _url;
        }
        
    }
}

// ------------------------------------------------------------
// --------------------- class RollingList --------------------
// ------------------------------------------------------------

class RollingList<T>
{
    // ---------------------------------------- RollingList instance variables

    private int _nextIndex = 0;
    private final int _maxCapacity;
    private List<T> _list = new ArrayList<T>();

    // ---------------------------------------- RollingList constructor
    
    public RollingList(int maxCapacity)
    {
        assert(maxCapacity > 0);
        _maxCapacity = maxCapacity;
    }

    // ---------------------------------------- RollingList add
    
    public void add(T item)
    {
        if (_nextIndex >= _list.size())
        {
            _list.add(item);
        }
        else
        {
            _list.set(_nextIndex, item);
        }
        
        _nextIndex = (_nextIndex + 1) % _maxCapacity;
    }

    // ---------------------------------------- RollingList copyToList

    public List<T> copyToList()
    {
        // If the list isn't even full yet, just return a straight copy.
        if (_list.size() < _maxCapacity) return new ArrayList<T>(_list);
        
        // The list is full, so we need to deal with the wrapping behavior.
        List<T> resultList = new ArrayList<T>();
        for (int i = 0; i < _maxCapacity; i++)
        {
            T item = _list.get((_nextIndex + i) % _maxCapacity);
            resultList.add(item);
        }
        return resultList;
    }
    
    // ---------------------------------------- RollingList tests
    
    public static void L1TEST_class()
    {
        RollingList<Integer> rollingList;

        rollingList = new RollingList<Integer>(1);
        rollingList.add(1);
        assert rollingList._list.size() == 1;
        rollingList.add(2);
        assert rollingList._list.size() == 1;
        
        rollingList = new RollingList<Integer>(3);
        rollingList.add(1);
        assert rollingList._list.size() == 1;
        rollingList.add(2);
        assert rollingList._list.size() == 2;
        rollingList.add(3);
        assert rollingList._list.size() == 3;
        rollingList.add(4);
        assert rollingList._list.size() == 3;
        
        List<Integer> list = rollingList.copyToList();
        assert list.get(0) == 2;
        assert list.get(1) == 3;
        assert list.get(2) == 4;            
    }
    
}
