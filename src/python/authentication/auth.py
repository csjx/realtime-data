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
__version__ = "$LastChangedRevision$"
# $LastChangedDate$
# $LastChangedBy$

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
logger.addHandler(logHandler)     # send to log file
#consoleHandler = logging.StreamHandler()
#consoleHandler.setFormatter(logFormatter)
#logger.addHandler(consoleHandler) # send to console
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
        self.isConnected = False
        self.isAuthenticated = self.config.get('isAuthenticated')
        self.startNetworkCommand = '/etc/init.d/networking start'
        self.stopNetworkCommand = '/etc/init.d/networking stop'
        self.checkIPCommand = '/sbin/ifconfig'
        self.cookieJar = CookieJar()
    
    
    def authenticate(self, waitInterval=60):
        ''' Establish an authenticated session with the service.'''
        
        try:
            # continuously test the connection
            while True:
                
                # listen for the stop event flag
                # event.wait(waitInterval)
                # if event.is_set():
                #     break
                
                # test the network connection
                if not self.has_connection():
                    self.logger.info('(' + str(self.failureCount) + 
                                      ') There is no network connection.')
                    
                    # test if the network downtime is past the threshhold
                    if self.failureCount > 4:
                        
                        # attempt to restart the network connection
                        self.disconnect()
                        self.connect()
                        self.failureCount = 0
                        
                else:
                    
                    # otherwise, test the authentication status
                    if self.isAuthenticated == False:
                        
                        # logout first
                        isLoggedIn = self.logout(self.config)
                        
                        # then try to login
                        isLoggedIn = self.login(self.config)
                        
                        self.logger.info("Currently logged in: " + str(isLoggedIn))
                        
                    else:
                        self.logger.debug('Already authenticated.')
                        
                # pause the thread for the given wait interval in seconds
                time.sleep(waitInterval)
         
        except KeyboardInterrupt:
            logger.info("Caught keyboard interrupt. Stopping.")
            self.stop()
    
    
    def connect(self):
        '''Connect to the local network.'''
        
        self.logger.info('Starting the network connection.')
        process = Popen(self.startNetworkCommand, 
                        stdout=PIPE, stderr=PIPE, shell=True)
        (resultString, ErrorString) = process.communicate()
        returnCode = process.returncode
        
    
    
    def disconnect(self):
        '''Disconnect from the local network'''
        
        self.logger.info('Stopping the network connection.')
        process = Popen(self.stopNetworkCommand, 
                        stdout=PIPE, stderr=PIPE, shell=True)
        (resultString, ErrorString) = process.communicate()
        returnCode = process.returncode
        
    
    
    def has_connection(self):
        '''Check the network connection at a known URL.'''
        
        # the IP address pattern needed for a match
        pattern = '\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}'
        
        try:
            
            # first check for a local IP address
            if self.isConnected == False:
                
                # send the check ip command
                process = Popen(self.checkIPCommand + ' ' + 
                                self.config.get('interface'),
                                stdout=PIPE, stderr=PIPE, shell=True)
                (resultString, ErrorString) = process.communicate()
                returnCode = process.returncode
                
                lanIP = 'no-ip-yet'
                
                # extract the local IP if there is one
                for line in resultString.split("\n"):
                    self.logger.debug(line)
                    if re.match('^inet ', line.lstrip()):
                        lanIP = line.lstrip().split(" ")[1].split(":")[1]
                
                self.logger.info('LAN IP address: ' + lanIP)
                
                # test for the correct IP address pattern
                self.logger.debug(re.match(pattern , lanIP))
                
                if re.match(pattern , lanIP) == None:
                    self.isConnected = False
                    self.failureCount += 1
                    return self.isConnected
                    
                else:
                    self.isConnected = True
                    self.failureCount = 0
                    return self.isConnected
            
            else:
                
                # we are connected, send the GET request to the test URL
                response = urllib2.urlopen(self.testURL)
                content = response.read()
                wanIP = content.split(":")[1].split("<")[0].lstrip()
                self.logger.info('WAN IP address: ' + wanIP)
                
                # test if the returned WAN IP address is valid
                if re.match(pattern, wanIP) is types.NoneType:
                    self.isConnected = False
                    self.failureCount += 1
                
                else:
                    self.isConnected = True
                    self.failureCount = 0
                
        except urllib2.URLError as (errorNumber, errorString):
            self.logger.debug('There was an error reading the URL:' +
              'Error({0}) message: {1}'.format(errorNumber, errorString))
            self.isConnected = False
            self.failureCount += 1
            
        except:
            self.logger.debug('Unexpected error: ' + str(sys.exc_info()[1]))
            self.isConnected = False
            self.failureCount += 1
            
        else:
            response.close()
            
        return self.isConnected
    
    
    def login(self, config=None):
        '''Log in to the service with the username and password.'''
        
        self.logger.debug('login() called.')
        
        loginURL = self.config.get('login').get('url')
        loginParams = urllib.urlencode(self.config.get('login').get('params'))
        request = urllib2.Request(loginURL, loginParams)
        httpHandler = urllib2.HTTPHandler()
        cookieHandler = urllib2.HTTPCookieProcessor(self.cookieJar)
        urlOpener = urllib2.build_opener(httpHandler, cookieHandler)
        
        try:
            response = urlOpener.open(request)
            content = response.read()
            self.logger.info('Login: response code ' + str(response.getcode()) +
                              ' from ' + response.geturl() + '.')
            self.logger.debug(content)
            self.isAuthenticated = True
            
        except urllib2.URLError as (errorNumber, errorString):
            self.logger.debug('There was an error reading the URL:' +
              'Error({0}) message: {1}'.format(errorNumber, errorString))
            self.isAuthenticated = False
            
        except urllib2.HTTPError as (errorNumber, errorString):
            self.logger.debug('There was an HTTP error:' +
              'Error({0}) message: {1}'.format(errorNumber, errorString))
            self.isAuthenticated = False
            
        except:
            self.logger.debug('Unexpected error: ' + str(sys.exc_info()[1]))
            self.isAuthenticated = False
            
        else:
            response.close()
        
        return self.isAuthenticated
    
    
    def logout(self, config=None):
        '''Log out of the service.'''
        
        self.logger.debug('logout() called.')
        
        logoutURL = self.config.get('logout').get('url')
        logoutParams = urllib.urlencode(self.config.get('logout').get('params'))
        request = urllib2.Request(logoutURL, logoutParams)
        httpHandler = urllib2.HTTPHandler()
        cookieHandler = urllib2.HTTPCookieProcessor(self.cookieJar)
        urlOpener = urllib2.build_opener(httpHandler, cookieHandler)
        try:
            response = urlOpener.open(request)
            content = response.read()
            self.logger.info('Logout: response code ' + str(response.getcode()) +
                              ' from ' + response.geturl() + '.')
            self.logger.debug(content)
            
        except urllib2.URLError as (errorNumber, errorString):
            self.logger.debug('There was an error reading the URL:' +
              'Error({0}) message: {1}'.format(errorNumber, errorString))
            self.isAuthenticated = False
            
        except:
            self.logger.debug('Unexpected error: ' + str(sys.exc_info()[1]))
            self.isAuthenticated = False
            
        else:
            response.close()
        
        return self.isAuthenticated
    
    
    def start(self):
        '''Start the authentication thread.'''
        self.logger.debug('start() called.')
        
        # execute the authenticate() method at the given interval
        self.authenticate(self.config.get('interval'))
    
    
    def stop(self, signal=None, frame=None):
        '''Stop the authentication thread.'''
        self.logger.debug('stop() called.')
        
        isLoggedIn = self.logout(self.config)
        sys.exit()
    
    


def main():
    ''' Creates and runs an instance of the Authentication class'''
    
    logger.debug('main() called.')
    
    # set up the POST parameters per site:
    # interface - the network interface on the device to be used
    # domain - the domain to be used for returning cookies
    # testURL - the URL used for testing IP connectivity
    # interval - the interval between tests in seconds
    # isAuthenticated - defaults to False, set to True to bypass authentication
    # login - the login service URL and parameters to be sent for authentication
    # logout - the logout service URL and parameters to be sent to close the session
    location = {
        'Palau': {
            'interface': 'wlan0',
            'domain': None,
            'testURL': 'http://checkip.dyndns.org',
            'interval': 20.0,
            'isAuthenticated': True,
            'login': {'url': 'http://10.22.0.1:5788/login_form.php',
                      'params': {'username': 'palauUsername',
                                 'password': 'palauPassword',
                                 'hiddenField': 'hiddenValue',
                                }
                     },
            'logout': {'url': 'http://10.22.0.1:5788/login_form.php',
                       'params': {'username': 'palauUsername',
                                  'password': 'palauPassword',
                                  'hiddenField': 'hiddenValue',
                                 }
                      },
            'relogin': {'url': 'http://10.22.0.1:5788/login_form.php',
                        'params': {'username': 'palauUsername',
                                   'password': 'palauPassword',
                                   'hiddenField': 'hiddenValue',
                                  }
                       },
        },
        'Micronesia': {
            'interface': 'wlan0',
            'domain': None,
            'testURL': 'http://checkip.dyndns.org',
            'interval': 20.0,
            'isAuthenticated': False,
            'login': {'url': 'http://10.22.0.1:5788/fs_login.php',
                      'params': {'name': 'loginForm',
                                 'username': 'fsmUsername',
                                 'password': 'fsmPassword',
                                 'ok_url': 'redirect.php',
                                 'fail_url': 'login_form.php',
                                 'zero_url': 'cart.php',
                                 'cart_url': 'cart.php'
                                }
                     },
            'logout': {'url': 'http://10.22.0.1/disconnect.php',
                       'params': {'name': 'logoutForm'
                                 }
                      },
            'relogin': {'url': 'http://10.22.0.1:5788/login_form.php',
                        'params': {'name': 'forcelogout',
                                   'username': 'fsmUsername',
                                   'password': 'fsmPassword',
                                   'sess_patronsoft1': '',
                                   'sess_patronsoft2': '',
                                   'ok_url': 'redirect.php',
                                   'fail_url': 'login_form.php',
                                   'zero_url': 'cart.php',
                                   'cart_url': 'cart.php',
                                   'logout': 'true'
                                  }
                       },
        },
        'AmericanSamoa': {
            'interface': 'wlan0',
            'domain': None,
            'testURL': 'http://checkip.dyndns.org',
            'interval': 20.0,
            'isAuthenticated': True,
            'login': {'url': 'http://10.22.0.1:5788/login_form.php',
                      'params': {'username': 'samoaUsername',
                                 'password': 'samoaPassword',
                                 'hiddenField': 'hiddenValue',
                                }
                     },
            'logout': {'url': 'http://10.22.0.1:5788/login_form.php',
                      'params': {'username': 'samoaUsername',
                                 'password': 'samoaPassword',
                                 'hiddenField': 'hiddenValue',
                                }
                     },
            'relogin': {'url': 'http://10.22.0.1:5788/login_form.php',
                      'params': {'username': 'samoaUsername',
                                 'password': 'samoaPassword',
                                 'hiddenField': 'hiddenValue',
                                }
                     },
        },
        'MarshallIslands': {
            'interface': 'wlan0',
            'domain': '.ntamar.net',
            'testURL': 'http://checkip.dyndns.org',
            'interval': 20.0,
            'isAuthenticated': False,
            'login': {'url': 'http://scc.ntamar.net/minta/eup/login',
                      'params': {'login_id': 'rmiUsername',
                                 'password': 'rmiPassword',
                                 'authsys_id': '1',
                                 'portal_id': '7',
                                }
                     },
            'logout': {'url': 'http://scc.ntamar.net/minta/eup/login',
                      'params': {'login_id': 'rmiUsername',
                                 'password': 'rmiPassword',
                                 'hiddenField': 'hiddenValue'
                                }
                     },
            'relogin': {'url': 'http://scc.ntamar.net/minta/eup/login',
                        'params': {'login_id': 'rmiUsername',
                                   'password': 'rmiPassword',
                                   'hiddenField': 'hiddenValue'
                                  }
                       },
        },
        'Test': {
            'interface': 'wlan0',
            'domain': '.ecoinformatics.org',
            'testURL': 'http://checkip.dyndns.org',
            'interval': 20.0,
            'isAuthenticated': False,
            'login': {'url': 'http://knb.ecoinformatics.org/knb/metacat',
                      'params': {'username': 'uid=kepler,o=unaffiliated,dc=ecoinformatics,dc=org',
                                 'password': 'knbPassword',
                                 'action': 'login',
                                }
                     },
            'logout': {'url': 'http://knb.ecoinformatics.org/knb/metacat',
                       'params': {'action': 'logout',
                                  'sessionId': None,
                                 }
                      },
        },
    }
    
    # create an Authentication instance
    session = Authentication(location.get('Test'))
    
    # handle abrupt signals such as as Control-C, kill, etc.
    signal.signal(signal.SIGTERM, session.stop)
    
    # start the authentication session
    session.start()
    


# run the main method of the module if called from the command line
if __name__ == '__main__':
    main()

