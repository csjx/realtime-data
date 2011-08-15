#  Copyright: 2011 Regents of the University of Hawaii and the
#       School of Ocean and Earth Science and Technology
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

'''Utility Class for managing file archives for PacIOOS data.'''

__author__  = "Christopher Jones, csjones@hawaii.edu"
__version__ = "$LastChangedRevision$"
# $LastChangedDate$
# $LastChangedBy$

import os
import sys
import socket
import StringIO as sio
import time

class FileArchiver(object):
  '''A class providing file archiving utilities.'''

  def __init__(self, config):
    '''Constructor: create a FileArchiver instance.'''
    
    # set the configuration parameters. Validation occurs on first use or reset
    self.config = config
    self._sourceName = self.config.get('sourceName')
    self._channelName = self.config.get('channelName')
    self._dateFormat = self.config.get('dateFormat')
    self._timeZone = self.config.get('timeZone')    
    self._archiveDirectory = self.config.get('archiveDirectory')
    self._start = self.config.get('start')
    self._stop= self.config.get('stop')
    self._dataFileName = self.config.get('dataFileName')
    self._interval = self.config.get('interval')
    
  @property
  def sourceName(self):
    '''The name of the source instrument.'''
    return self._sourceName
  
  @sourceName.setter
  def sourceName(self, sourceName):
    '''Set the name of the instrument source.'''
    self._sourceName = sourceName
      
  @property 
  def channelName(self):
    '''The name of the data input file.'''
    return self._channelName

  @channelName.setter  
  def channelName(self, channelName):
    self._channelName = channelName
  
  @property
  def start(self):
    '''The timestamp to start archiving data.'''
    return self._start
  
  @start.setter
  def start(self, start):
    '''Set the archiver start timestamp.'''
    self._start = start

  @property
  def stop(self):
    '''The timestamp to stop archiving data.'''
    return self._stop
  
  @stop.setter
  def stop(self, stop):
    '''Set the archiver stop timestamp.'''
    self._stop = stop
  
  @property
  def timeZone(self):
    '''The time zone for the instrument location.'''
    return self._timeZone
  
  @timeZone.setter
  def timeZone(self, timeZone):
    '''Set the time zone for the instrument location.'''
    self._timeZone = timeZone
    
  @property
  def interval(self):
    '''The interval used to create archive files, either hourly or daily.'''
    return self._interval
  
  @interval.setter
  def interval(self, interval):
    '''Set the interval for the archive files, either hourly or daily.'''
    
    # only if it is hourly or daily
    if self.validateInterval(interval):
      self._interval = interval
    
    else:
      raise ValueError('''The interval must be either 'hourly' or 'daily'.''')
  
  @property
  def dateFormat(self):
    '''The format of the date column(s) of the CTD data sample'''
    return self._dateFormat
  
  @dateFormat.setter
  def dateFormat(self, dateFormat):
    self._dateFormat = dateFormat
     
  @property
  def archiveDirectory(self):
    '''The directory used to create archive files.'''
    return self._archiveDirectory
  
  @archiveDirectory.setter
  def archiveDirectory(self, archiveDirectory):
    '''Set the directory path to archive data.'''
    self._archiveDirectory = archiveDirectory
  
  @property
  def dataFileName(self):
    '''The name of the input data file.'''
    return self._dataFileName
  
  @dataFileName.setter
  def dataFileName(self, dataFileName):
    '''Set the name of the input data file name.'''
    if self.validateDataFile(dataFileName):
      self._dataFileName = dataFileName
      
    else:
      raise ValueError('''Couldn't read the data file.''')

  def rebuild(self):
    '''rebuild the archive files using the given parameters.'''

  def validate(self):
    '''Validate the configuration parameters.'''
    isValid = False

    if self.validateInterval(self._interval):
      isValid = True
    else:
      isValid = False
      
    if self.validateDataFile(self._dataFileName):
      isValid = True
    else:
      isValid = False

    if self.validateTimestamps(self._start, self._stop):
      isValid = True
    else:
      isValid = False
    
    if isValid == False:
      raise ValueError('Configuration is not valid. Please check your values.')
    
  def validateInterval(self, interval):
    '''Validate the interval parameter, ensuring it has a proper value.'''
    if interval == 'hourly' or interval == 'daily':
      return True
    
    else:
      return False    

  def validateTimestamps(self, start, stop):
    '''Validate the start and stop timestamp parameters.'''
    
    try:
      startDate = time.strptime(start, '%Y%m%dT%H%M%S')
      stopDate = time.strptime(stop, '%Y%m%dT%H%M%S')
    
      if startDate > stopDate:
        return False
      
      return True
    
    except ValueError:
      return False

  def validateDataFile(self, dataFileName):
    '''Validate that the input data file is readable.'''
    try:
      with open(dataFileName, 'r') as fileObject: pass
      return True
    
    except IOError:
      return False
                      
def main():
  ''' Creates and runs an instance of the FileArchiver class.'''

  # The configuration dictionary that sets the default values for each instrument
  # location.  Don't change these, but rather use the setter methods below.
  location = {
              'NS01': {'sourceName': 'AW01XX_002CTDXXXXR00',
                       'channelName': 'DecimalASCIISampleData',
                       'archiveDirectory': './data/raw/alawai',
                       'dataFileName': 'AW01.backup.txt',
                       'start': '20110101T000000',
                       'stop': '20111231T235959',
                       'timeZone': 'HST',
                       'interval': 'hourly',
                       'dateFormat': 'YYYY-MM-DD HH:MM:SS'
                      },
              'NS02': {'sourceName': 'AW02XX_001CTDXXXXR00',
                       'channelName': 'DecimalASCIISampleData',
                       'archiveDirectory': './data/raw/alawai',
                       'dataFileName': 'AW02.backup.txt',
                       'start': '20110101T000000',
                       'stop': '20111231T235959',
                       'timeZone': 'HST',
                       'interval': 'hourly',
                       'dateFormat': 'YYYY-MM-DD HH:MM:SS'
                      },
              'NS03': {'sourceName': 'WK01XX_001CTDXXXXR00',
                       'channelName': 'DecimalASCIISampleData',
                       'archiveDirectory': './data/raw/alawai',
                       'dataFileName': 'WK01.backup.txt',
                       'start': '20110101T000000',
                       'stop': '20111231T235959',
                       'timeZone': 'HST',
                       'interval': 'hourly',
                       'dateFormat': 'YYYY-MM-DD, HH:MM:SS'
                      },
              'NS04': {'sourceName': 'WK02XX_001CTDXXXXR00',
                       'channelName': 'DecimalASCIISampleData',
                       'archiveDirectory': './data/raw/alawai',
                       'dataFileName': 'WK02.backup.txt',
                       'start': '20110101T000000',
                       'stop': '20111231T235959',
                       'timeZone': 'HST',
                       'interval': 'hourly',
                       'dateFormat': 'YYYY-MM-DD, HH:MM:SS'
                      },
              'NS05': {'sourceName': 'PIAS01_001CTDXXXXR00',
                       'channelName': 'DecimalASCIISampleData',
                       'archiveDirectory': './data/raw/alawai',
                       'dataFileName': 'PIAS01.backup.txt',
                       'start': '20110101T000000',
                       'stop': '20111231T235959',
                       'timeZone': 'SST',
                       'interval': 'hourly',
                       'dateFormat': 'YYYY-MM-DD HH:MM:SS'
                      },
              'NS06': {'sourceName': 'PIFM01_001CTDXXXXR00',
                       'channelName': 'DecimalASCIISampleData',
                       'archiveDirectory': './data/raw/pacioos',
                       'dataFileName': 'PIFM01.backup.txt',
                       'start': '20110101T000000',
                       'stop': '20111231T235959',
                       'timeZone': 'PONT',
                       'interval': 'hourly',
                       'dateFormat': 'YYYY-MM-DD HH:MM:SS'
                      },
              'NS07': {'sourceName': 'PIMI01_001CTDXXXXR00',
                       'channelName': 'DecimalASCIISampleData',
                       'archiveDirectory': './data/raw/pacioos',
                       'dataFileName': 'PIMI01.backup.txt',
                       'start': '20110101T000000',
                       'stop': '20111231T235959',
                       'timeZone': 'MHT',
                       'interval': 'hourly',
                       'dateFormat': 'YYYY-MM-DD HH:MM:SS'
                      },
              'NS08': {'sourceName': 'PIPL01_001CTDXXXXR00',
                       'channelName': 'DecimalASCIISampleData',
                       'archiveDirectory': './data/raw/pacioos',
                       'dataFileName': 'PIPL01.backup.txt',
                       'start': '20110101T000000',
                       'stop': '20111231T235959',
                       'timeZone': 'PWT',
                       'interval': 'hourly',
                       'dateFormat': 'YYYY-MM-DD HH:MM:SS'
                      },
              'NS09': {'sourceName': 'PIGM01_001CTDXXXXR00',
                       'channelName': 'DecimalASCIISampleData',
                       'archiveDirectory': './data/raw/pacioos',
                       'dataFileName': 'PIGM01.backup.txt',
                       'start': '20110101T000000',
                       'stop': '20111231T235959',
                       'timeZone': 'ChST',
                       'interval': 'hourly',
                       'dateFormat': 'YYYY-MM-DD HH:MM:SS'
                      },
              'NS10': {'sourceName': 'MB01XX_001CTDXXXXR00',
                       'channelName': 'DecimalASCIISampleData',
                       'archiveDirectory': './data/raw/alawai',
                       'dataFileName': 'MB01.backup.txt',
                       'start': '20110101T000000',
                       'stop': '20111231T235959',
                       'timeZone': 'HST',
                       'interval': 'hourly',
                       'dateFormat': 'YYYY-MM-DD HH:MM:SS'
                      }
             }
  
  # Set the location to archive data.  If desired, override any default 
  # property values by uncommenting the setter methods and change the values
  try:
    fileArchiver = FileArchiver(location.get('NS10'))
    #fileArchiver.sourceName = 'Change this to your sourceName'
    #fileArchiver.channelName = 'Change this to your channelName'
    #fileArchiver.archiveDirectory = 'Change this to your archive directory'
    fileArchiver.dataFileName = '/Users/cjones/NS10.backup.txt'
    #fileArchiver.start = 'Change this to your start timestamp'
    #fileArchiver.stop = 'Change this to your stop timestamp'
    #fileArchiver.timeZone = 'Change this to your timeZone'
    #fileArchiver.interval = 'daily'
    #fileArchiver.dateFormat = 'YYYY-MMM-DD, HH:MM:SS'
    
    fileArchiver.validate()
    
    print "\
    Configuration Summary:\n\n\
    sourceName      : %s\n\
    channelName     : %s\n\
    archiveDirectory: %s\n\
    dataFileName    : %s\n\
    start           : %s\n\
    stop            : %s\n\
    timeZone        : %s\n\
    interval        : %s\n\
    dateFormat      : %s\n"\
    % \
    (fileArchiver.sourceName, 
     fileArchiver.channelName, 
     fileArchiver.archiveDirectory,
     fileArchiver.dataFileName, 
     fileArchiver.start, 
     fileArchiver.stop, 
     fileArchiver.timeZone,
     fileArchiver.interval,
     fileArchiver.dateFormat)

  except ValueError:
    print 'A configuration error occured. The error was: ', sys.exc_info()[1]
    
# run the main method of the module if called from the command line
if __name__ == '__main__':
  sys.exit(main())
