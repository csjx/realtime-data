#  Copyright: 2010 Regents of the University of Hawaii and the
#             School of Ocean and Earth Science and Technology
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

'''Classes for WebDAV communication, data parsing, and DataTurbine insertion.'''

__author__  = "Christopher Jones, csjones@hawaii.edu"
__version__ = "$LastChangedRevision: 723 $"
# $LastChangedDate: 2010-08-24 21:57:03 -0600 (Tue, 24 Aug 2010) $
# $LastChangedBy: cjones $

import os
import pycurl
import sys
import socket
import StringIO as sio
import time

class WebDAVSource(object):
    '''Harvest ASCII data from an instrument and communicate via WebDAV.
    
    A simple class used to harvest a decimal ASCII data stream from an
    instrument over a TCP socket connection to a serial2ip converter host. The
    data stream is then converted into RBNB frames and pushed into the RBNB
    DataTurbine real time server via the WebDAV protocol.
    
    '''
    
    def __init__(self, rbnbServer=None, rbnbPort=None, rbnbService=None, 
                 rbnbCacheSize=None, rbnbArchiveSize=None, rbnbArchiveMode=None, 
                 rbnbSourceName=None, sourceChannelName=None, 
                 sourceChannelType=None, sourceDataType=None, 
                 sourceHostName=None, sourceHostPort=None):
        '''Creates an rbnb connection and registers channels'''
        
        # each need value testing prior to setting attributes
        self.protocol          = 'http'
        self.rbnbServer        = rbnbServer
        self.rbnbPort          = rbnbPort
        self.rbnbService       = rbnbService
        self.rbnbAddress       = rbnbServer + ':' + str(rbnbPort)
        self.rbnbCacheSize     = rbnbCacheSize
        self.rbnbArchiveSize   = rbnbArchiveSize
        self.rbnbArchiveMode   = rbnbArchiveMode
        self.rbnbSourceName    = rbnbSourceName
        self.sourceChannelName = sourceChannelName
        self.sourceChannelType = sourceChannelType
        self.sourceDataType    = sourceDataType
        self.sourceHostName    = sourceHostName
        self.sourceHostPort    = sourceHostPort
        
        # create a source using the WebDAV interface
        
        mkcolURL = self.protocol + '://' + \
                   self.rbnbAddress + '/' + \
                   self.rbnbService + '/' + \
                   self.rbnbSourceName + '?' \
                   'mode=' + self.rbnbArchiveMode + '&' + \
                   'archive=' + str(self.rbnbArchiveSize) + '&' + \
                   'cache=' + str(self.rbnbCacheSize)
        
        curlObject = pycurl.Curl()
        curlObject.setopt(pycurl.CUSTOMREQUEST, 'MKCOL')
        curlObject.setopt(pycurl.URL, mkcolURL)
        curlObject.perform()
        curlObject.close()
    
    
    def connect(self):
        '''Creates a TCP socket connection to the instrument'''
        
        # for demonstration purposes, get data from a file, not the socket
        # this code should be replaced with socket connection code
        self.dataFile = open(self.sourceFile, 'r', 0)
    
    
    def insertData(self, sample):
        ''' Inserts an ASCII data sample into the rbnb server'''
        
        # create a timestamp for the current time
        secondsSinceEpochInUTC = time.time()
        localTime = time.localtime(secondsSinceEpochInUTC)
        timestamp = time.strftime('%d %b %Y %H:%M:%S', localTime) 
        # add the timestamp to the sample
        sample = sample.rstrip() + '     ' + timestamp + '\n\r'
        sampleSize = len(sample)
        stringIO = sio.StringIO(sample)
        
        putURL = self.protocol + '://' + \
                 self.rbnbAddress + '/' + \
                 self.rbnbService + '/' + \
                 self.rbnbSourceName + '/' + \
                 self.sourceChannelName + '?' + \
                 'datatype=' + self.sourceDataType + '&' + \
                 'mime=' + self.sourceChannelType + '&' + \
                 'time=' + str(secondsSinceEpochInUTC)
        
        curlObject = pycurl.Curl()
        curlObject.setopt(pycurl.UPLOAD, 1)
        curlObject.setopt(pycurl.URL, putURL)
        curlObject.setopt(pycurl.READFUNCTION, stringIO.read)
        curlObject.setopt(pycurl.INFILESIZE, sampleSize)
        curlObject.perform()
        
        print 'Sent sample to the dataTurbine: ' + sample
        curlObject.close()
        stringIO.close()
    
    
    def start(self):
        ''' Start streaming data to the rbnb server'''
        self.connect()
        
        # replace this with socket streaming code (vs file reading)
        for sample in self.dataFile:
            sample = sample.rstrip()
            time.sleep(1) # don't use this with socket code
            self.insertData(sample)
        
        # once all data are inserted, close the connections
        self.stop()
    
    
    def stop(self):
        ''' Stops the data streaming.'''
        
        self.dataFile.close()
    
    
    def setSourceFile(self, filePath):
        '''Sets the path to the data sourcefile.'''
        
        # demo purposes only.  data should come from TCP socket
        self.sourceFile = filePath
    
    
def main():
    ''' Creates and runs an instance of the WebDAVSource class'''
    
    # define rbnb server, source, and channel details
    rbnbServer = '10.0.0.100'
    rbnbPort = 8080
    rbnbService = 'RBNB'
    rbnbCacheSize = 25000
    rbnbArchiveSize = 3000000
    rbnbArchiveMode = 'append'
    rbnbSourceName = 'KN0101_010TCHNXXXR00'
    sourceChannelName = 'DecimalASCIISampleData'
    sourceChannelType = 'text/plain'
    sourceDataType = 'string'
    # define instrument TCP details
    sourceHostName = '128.171.104.52'
    sourceHostPort = 2103
    
    # create a tchain source instance
    webDAVSource = WebDAVSource(rbnbServer, rbnbPort, rbnbService,
                                rbnbCacheSize, rbnbArchiveSize, rbnbArchiveMode, 
                                rbnbSourceName, sourceChannelName, 
                                sourceChannelType, sourceDataType, 
                                sourceHostName, sourceHostPort)
    
    # set the source file name (demo only, not needed with TCP sockets)
    sourceFile = os.path.join('..', '..', '..', '..', 'test', 'tchain.short.log')
    #sourceFile = os.path.join('..', '..', '..', '..', 'test', 'tchain.long.log')
    
    webDAVSource.setSourceFile(sourceFile)
    
    # start the streaming
    webDAVSource.start()
    


# run the main method of the module if called from the command line
if __name__ == '__main__':
    sys.exit(main())
