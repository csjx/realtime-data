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

'''Auth module that allows authentication to password protected WiFi services.'''

__author__  = "Christopher Jones, csjones@hawaii.edu"
__version__ = "$LastChangedRevision: $"
# $LastChangedDate: $
# $LastChangedBy: $

import logging
import logging.handlers
import os
import re
import signal
import sys
import time
import types
import urllib, urllib2
from cookielib import CookieJar
from subprocess import Popen, PIPE

# set up logging
logger = logging.getLogger('authentication')
logFormatter = logging.Formatter('%(asctime)s %(name)s %(levelname)s %(message)s')
logHandler   = logging.handlers.RotatingFileHandler(
                      '/var/log/wifi/Authentication.log',
                      'a',      # append mode
                      2097152, # 2 MB per file
                      5)        # rotate up to 5 files

logHandler.setFormatter(logFormatter)
consoleHandler = logging.StreamHandler()
consoleHandler.setFormatter(logFormatter)
logger.addHandler(logHandler)     # send to log file
logger.addHandler(consoleHandler) # send to console
logger.setLevel(logging.INFO)

class Authentication(object):
    '''Provide authentication functions for web-based WiFi services.
    
    A simple class used to authenticate to password protected internet access
    in the Pacific Islands for PacIOOS remote deployments.  Each island's
    ISP handles authentication differently.  For those that don't allow
    MAC-based authentication, this class can provide login, logout, and other
    methods for maintaining a web session without a graphical browser.
    
    '''
    
    def __init__(self, config):
        
        '''Creates an Authentication instance.'''
        
        # logging: set up the logger to use syslog style output
        self.logger = logging.getLogger('authentication.Authentication')
        
        self.logger.debug('Constructor called.')
        
        self.failureCount = 0
        self.config = config
        self.testURL = self.config.get('testURL')
        self.isAuthenticated = self.config.get('isAuthenticated')
        self.startNetworkCommand = '/etc/init.d/networking start'
        self.stopNetworkCommand = '/etc/init.d/networking stop'
        self.cookieJar = CookieJar()
    
    


# run the main method of the module if called from the command line
if __name__ == '__main__':
    main()

