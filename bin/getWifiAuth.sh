#!/bin/bash

# getWifiAuth.sh
# 
# Copyright: 2010 Regents of the University of Hawaii and the
#        School of Ocean and Earth Science and Technology 
# 
#  '$Author: cjones $'
#  '$Date: 2010-04-26 17:14:11 -0600 (Mon, 26 Apr 2010) $'
#  '$Revision: 612 $'

# system variables
wGet="/usr/bin/wget";

# Status variables
loginURLwGetOptions=" -O - -q --save-headers --post-data ";
checkURLwGetOptions=" -O - -q --load-cookies $cookieFile ";
checkURL="http://checkip.dyndns.org";
isAuthorized="false";
sleepInterval=60;
failureCount=0;
failureThreshold=5;

# subroutine to save session cookies to the cookies file. This is only needed
# because wget version 1.9.1 doesn't support the '--keep-session-cookies' option.
saveSessionCookies () 
{
  cookieStrings=$(echo ${result} | tr "\r" "\n" | grep "Set-Cookie");

  # parse the Set-Cookie strings
  IFS=$'\n';

  # put each Set-Cookie string into an array as a member
  cookiesArray=( ${cookieStrings} );

  # Iterate through the cookie strings array
  for setString in ${cookiesArray[@]};
    do
      # Strip the 'Set-Cookie: ' prefix
      pairString=${setString#*Set-Cookie: };

      # set the expires field to the default 0 seconds for session cookies
      expires="0";

      IFS=";";
      # Add each name/value pair to a pair array
      pairArray=(${pairString});

      # Iterate through the pair array
      for pair in ${pairArray[@]};
        do
          # trim the spaces or tabs from the beginning of the name/value line
          trimmedPair=${pair#* };

          # Only process name/value pairs, not single-name members (like 'HttpOnly')
          if [[ "${trimmedPair}" =~ "=" ]]; then
                        
            # for each name/value pair, remove the value to get the name
            name=${trimmedPair/=*/};
            
            # for each name/value pair, remove the name to get the value
            trimmedPairLength=${#trimmedPair}; 
            delimIndex=$(expr index ${trimmedPair} "=");
            value=${trimmedPair:$delimIndex:$trimmedPairLength};

            # assign the variables appropriately
            if [[ $name = "domain" || $name = "Domain" ]]; then
              domain=$value;

            elif [[ $name = "expires" || $name = "Expires" ]]; then
              expires=$(date --date "${value}" +%s);

            elif [[ $name = "path" || $name = "Path" ]]; then
              path=$value;

            else
              cookieName=$name;
              cookieValue=$value;
            fi
          fi
        done
        # build the cookie line for the persistent cookie file
        cookie=${domain}$'\t'"TRUE"$'\t'${path}$'\t'"FALSE"$'\t'"0"$'\t'${cookieName}$'\t'${cookieValue};

        # only save session cookies
        if [[ $expires = "0" ]]; then
          echo $cookie >> /tmp/cookies.txt;

        fi
    done
  
}

#move old session cookies first
if [[ -f /tmp/cookies.txt ]]; then
  mv /tmp/cookies.txt /tmp/cookies.txt".old";
fi

# Location variables
#location="test";
#location="RepublicOfTheMarshallIslands";
location="FederatedStatesOfMicronesia";
#location="AmericanSamoa";
#location="Palau";

# Login form variables
if [ $location = "RepublicOfTheMarshallIslands" ]; then
  
  loginURL="http://scc.ntamar.net/minta/eup/login";
  domain=".ntamar.net";
  usernameField="login_id";
  username="pacioos";
  passwordField="password";
  password="Wifi4PacIOOS";
  #Sputnik specific variables
  authTypeField="as_select";
  authType="2";
  authSystemIdField="authsys_id";
  authSystemId="2";
  portalIdField="portal_id";
  portalId="7";

elif [ $location = "FederatedStatesOfMicronesia" ]; then
  loginURL="http://10.22.0.1:5788/login_form.php";
  reLoginURL="http://10.22.0.1/fs_login.php";
  logoutURL="http://10.22.0.1/fs_logout.php";
  usernameField="username";
  username="pacioos";
  passwordField="password";
  password="sharedPW4PacIOOS";
  hasCurrentSessionString="User has logged in previously";
  
  # First Spot specific variables
  okURLField="ok_url";
  okURL="redirect.php"; 
  failURLField="fail_url";
  failURL="login_form.php";
  zeroURLField="zero_url";
  zeroURL="cart.php"; 
  cartURLField="cart_url";
  cartURL="cart.php"; 
  
  # First Spot specific login variables
  stateField="state";
  state="PNI";
  desturlField="desturl";
  desturl="http://checkip.dyndns.org";
  sessionField1="patronsoft1";
  sessionID1="";
  sessionField2="patronsoft2";
  sessionID2="";
  loginPageField="loginPage";
  loginPage="1";
  
  # First Spot specific re-login variables
  
  sessionReLoginField1="sess_patronsoft1";
  sessionReLoginID1="";
  sessionReLoginField2="sess_patronsoft2";
  sessionReLoginID2="";  
        
elif [ ${location} = "AmericanSamoa" ]; then
  echo "This location is not implemented yet."
  exit 0;

elif [ ${location} = "Palau" ]; then
  echo "This location is not implemented yet."
  exit 0;

elif [ ${location} = "test" ]; then
  loginURL="http://data.piscoweb.org/catalog/metacat";
  domain=".piscoweb.org";
  usernameField="username";
  username="uid=kepler,o=unaffiliated,dc=ecoinformatics,dc=org";
  passwordField="password";
  password="kepler";
  actionField="action";
  action="login";
  
else
  echo "The location variable is not set correctly."
  exit 0;
  
fi

# monitor the prepaid authorization at the set interval
while [ 1 = 1 ]; 
  do
    # try to authorize if needed
    if [ ${isAuthorized} == "false" ]; then
      
      # prompt for session id parameters
      parameters="";
      
      if [[ ${usernameField-} && ${username-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$usernameField"="$username;
        
      fi
      
      if [[ ${passwordField-} && ${password-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$passwordField"="$password;
        
      fi
      
      if [[ ${okURLField-} && ${okURL-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$okURLField"="$okURL;
        
      fi
      
      # logout first
      logoutResult=$(${wGet} -O - --save-headers --post-data $parameters $logoutURL 2>&1); 
      
      result=$(${wGet} -O - --save-headers --post-data $parameters $loginURL 2>&1);
      
      # set the session ids
      sessionID1=$(echo $result | tr " " "\n" | tr "&" "\n" | grep "$sessionField1=" | uniq | cut -d"=" -f2);
      sessionID2=$(echo $result | tr " " "\n" | tr "&" "\n" | grep "$sessionField2=" | uniq | cut -d"=" -f2);
      
      echo "Session IDs: "${sessionID1}" and "${sessionID2};
          
      # first set the URL parameters. Only add the parameter if it exists
      # and is not empty.  Only add "&" if $parameters is not empty
      parameters="";
      
      if [[ ${authTypeField-} && ${authType-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$authTypeField"="$authType;
        
      fi
    
      if [[ ${authSystemIdField-} && ${authSystemId-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$authSystemIdField"="$authSystemId;
        
      fi
      
      if [[ ${stateField-} && ${state-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$stateField"="$state;
        
      fi

      if [[ ${desturlField-} && ${state-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$desturlField"="$desturl;
        
      fi

      if [[ ${okURLField-} && ${okURL-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$okURLField"="$okURL;
        
      fi

      if [[ ${failURLField-} && ${failURL-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$failURLField"="$failURL;
        
      fi

      if [[ ${zeroURLField-} && ${zeroURL-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$zeroURLField"="$zeroURL;
        
      fi

      if [[ ${cartURLField-} && ${cartURL-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$cartURLField"="$cartURL;
        
      fi

      if [[ ${sessionReLoginField1-} && ${sessionID1-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$sessionReLoginField1"="$sessionID1;
        
      fi

      if [[ ${sessionReLoginField2-} && ${sessionID2-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$sessionReLoginField2"="$sessionID2;
        
      fi

#      if [[ ${logoutField-} && ${logout-} ]]; then
#        if [[ $parameters ]]; then parameters=$parameters"&"; fi
#        parameters=$parameters$logoutField"="$logout;
#        
#      fi
#
#      if [[ ${loginPageField-} && ${loginPage-} ]]; then
#        if [[ $parameters ]]; then parameters=$parameters"&"; fi
#        parameters=$parameters$loginPageField"="$loginPage;
#        
#      fi
#      
      if [[ ${usernameField-} && ${username-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$usernameField"="$username;
        
      fi
      
      if [[ ${passwordField-} && ${password-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$passwordField"="$password;
        
      fi
      
      if [[ ${actionField-} && ${action-} ]]; then
        if [[ $parameters ]]; then parameters=$parameters"&"; fi
        parameters=$parameters$actionField"="$action;
        
      fi
      
      # try to log in at the login URL      
      result=$(${wGet} -O - --save-headers --post-data $parameters $reLoginURL);
      echo ${result};
            
      # save session cookies too
      saveSessionCookies;
      
      # test a known URL for the correct response.
      checkResult=$(${wGet} -O - -q --load-cookies /tmp/cookies.txt $checkURL);
      
      # Did we get an IP address back in the response?
      if [[ ${checkResult} =~ "Current IP Address:" ]]; then
        checkResultSuffix=${checkResult#*<body>};
        checkResultString=${checkResultSuffix%</body>*};
        echo $(date) "Wifi connection is authorized." ${checkResultString};
        failureCount=0; # reset the number of failures
        isAuthorized="true";
        
        
      
      else
        echo $(date) "Wifi connection is not authorized.";
        #echo ${checkResult};
        let "failureCount+=1";        
        isAuthorized="false";
        
        # for persistent failures, restart the networking service
        if [[ "$failureCount" -gt "$failureThreshold" ]]; then
          /etc/init.d/networking stop;
          sleep 10;
          /etc/init.d/networking start;
          failureCount=0;
        fi
        
      fi
      
    else
      # test a known URL for the correct response.
      checkResult=$($wGet -O - -q --load-cookies /tmp/cookies.txt $checkURL);
      
      if [[ $checkResult =~ "Current IP Address:" ]]; then
        checkResultSuffix=${checkResult#*<body>};
        checkResultString=${checkResultSuffix%</body>*};
        echo $(date) "Wifi connection is authorized." ${checkResultString};
        failureCount=0; # reset the number of failures
        isAuthorized="true";

      else
        echo $(date) "Wifi connection is not authorized.";
        echo ${checkResult};
        let "failureCount+=1";
        isAuthorized="false";
        
      fi
        
    fi
    
    sleep $sleepInterval;
    
  done
