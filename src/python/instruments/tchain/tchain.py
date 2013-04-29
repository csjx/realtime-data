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

'''Classes for TChain communication, data parsing, and DataTurbine insertion.'''

__author__  = "Christopher Jones, csjones@hawaii.edu"
__version__ = "$LastChangedRevision$"
# $LastChangedDate$
# $LastChangedBy$

import os
import sys
import socket
import time

# also import Java classes for the DataTurbine
sys.path.append(os.path.join('..', '..', '..', '..', 'lib', 'rbnb.jar'))
from com.rbnb.sapi import ChannelMap
from com.rbnb.sapi import Source

class TChainSource(object):
    '''A class used to harvest ASCII data from a TChain temperature logger.
    
    A simple class used to harvest a decimal ASCII data stream from a TChain
    temperature logger over a TCP socket connection to a serial2ip converter
    host. The data stream is then converted into RBNB frames and pushed into the
    RBNB DataTurbine real time server.
    
    '''
    
    def __init__(self, rbnbServer=None, rbnbPort=None, rbnbCacheSize=None, 
                 rbnbArchiveSize=None, rbnbArchiveMode=None, rbnbSourceName=None, 
                 rbnbChannelName=None, rbnbChannelType=None, sourceHostName=None, 
                 sourceHostPort=None):
        '''Creates an rbnb connection and registers channels'''
        
        # each need value testing prior to setting attributes
        self.rbnbServer      = rbnbServer
        self.rbnbPort        = rbnbPort
        self.rbnbAddress     = rbnbServer + ':' + str(rbnbPort)
        self.rbnbCacheSize   = rbnbCacheSize
        self.rbnbArchiveSize = rbnbArchiveSize
        self.rbnbArchiveMode = rbnbArchiveMode
        self.rbnbSourceName  = rbnbSourceName
        self.rbnbChannelName = rbnbChannelName
        self.rbnbChannelType = rbnbChannelType
        self.sourceHostName  = sourceHostName
        self.sourceHostPort  = sourceHostPort
        self.channelIndex    = 0
        
        # create a source and connect it to the rbnb server
        self.rbnbSource = Source(self.rbnbCacheSize, 
                                 self.rbnbArchiveMode, self.rbnbArchiveSize)
        self.rbnbSource.OpenRBNBConnection(self.rbnbAddress, self.rbnbSourceName)
        
        # create a channel map used to register and send data to the rbnb server
        self.channelMap = ChannelMap()
        self.channelIndex = self.channelMap.Add(self.rbnbChannelName)
        self.channelMap.PutUserInfo(self.channelIndex, 'units=none')
        
        # then register the channel map with the rbnb server
        self.rbnbSource.Register(self.channelMap)
        self.channelMap.Clear()
    
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
        # add the timestamp and data to the channel map
        sample = sample + '     ' + timestamp
        self.channelIndex = self.channelMap.Add(self.rbnbChannelName)
        self.channelMap.PutTime(secondsSinceEpochInUTC, 0.0)
        self.channelMap.PutMime(self.channelIndex, self.rbnbChannelType)
        self.channelMap.PutDataAsString(self.channelIndex, sample + '\n\r')
        
        # Send the channel map to the server
        self.rbnbSource.Flush(self.channelMap)
        self.channelMap.Clear()
        print 'Sent sample to the dataTurbine: ' + sample
    
    
    def start(self):
        ''' Start streaming data to the rbnb server'''
        self.connect()
        
        # first create a timestamp in seconds since the unix epoch
        
        # replace this with socket streaming code (vs file reading)
        for sample in self.dataFile:
            sample = sample.rstrip()
            time.sleep(5) # don't use this with socket code
            self.insertData(sample)
        
        # once all data are inserted, close the connections
        self.stop()
    
    
    def stop(self):
        ''' Stops the data streaming.'''
        
        self.rbnbSource.Detach()
        self.dataFile.close()
    
    
    def setSourceFile(self, filePath):
        '''Sets the path to the tchain data sourcefile.'''
        
        # demo purposes only.  data should come from TCP socket
        self.sourceFile = filePath
    
    
def main():
    ''' Creates and runs an instance of the TChainSource class'''
    
    # define rbnb server, source, and channel details
    rbnbServer = 'localhost'
    rbnbPort = 3333
    rbnbCacheSize = 25000
    rbnbArchiveSize = 3000000
    rbnbArchiveMode = 'append'
    rbnbSourceName = 'KN0101_010TCHNXXXR00'
    rbnbChannelName = 'DecimalASCIISampleData'
    rbnbChannelType = 'text/plain'
    # define instrument TCP details
    sourceHostName = '128.171.104.52'
    sourceHostPort = 2103
    
    # create a tchain source instance
    tChainSource = TChainSource(rbnbServer, rbnbPort, rbnbCacheSize, 
                                rbnbArchiveSize, rbnbArchiveMode, rbnbSourceName, 
                                rbnbChannelName, rbnbChannelType, sourceHostName, 
                                sourceHostPort)
    
    # set the source file name (demo only, not needed with TCP sockets)
    sourceFile = os.path.join('..', 'resources', 'tchain.short.log')
    #sourceFile = os.path.join('..', 'resources', 'tchain.long.log')
    
    tChainSource.setSourceFile(sourceFile)
    
    # start the streaming
    tChainSource.start()
    


# run the main method of the module if called from the command line
if __name__ == '__main__':
    sys.exit(main())
