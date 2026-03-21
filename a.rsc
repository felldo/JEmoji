# -------------------------------------------------------------------------------
# DDNS update script for Hetzner's DNS API
#
# by Philip 'ShokiNN' Henning <mail@philip-henning.com>
# Version 1.0
# last update: 10.11.2023
# License: MIT
#
# Compatibility: RouterOS 7
# -------------------------------------------------------------------------------







# --- Define variables -----------------------------------------------------------------------------------------
# Enter all required variables and secrets here. -- All secrets are stored unencrypted!
## API Key to authenticate to Hetzners API
### Data Type: String
:local apiKey "L9CNH7G766T30BEZ1iqRTKpwXONE9JQ8"; # Example: "3su1OLc0gUhUdwxn1bmKFss5V19mBhBx"; -- This one is invalid, you don't need to try ;)

## --- Domain config --------------------------------------------------------------------------------
# Interface
# The interface name where the IP should be fetched from
# Data Type: String
# Example: "pppoe-out1";
#
# Pool
# The prefix delegation pool which is used to automatically setup the IPv6 interface IP
# Use "" when you don't use a pool to set your interface ip or for a type A record
# Data Type: String
# Example: "pool-ipv6";
#
# Zone
# Zone which should be used to set a record to
# Data Type: String
# Example: "domain.com";
#
# Record type
# The type of record which will be set
# Data Type: String
# Valid values: "A", "AAAA"
# Example: "A";
#
# Record name
# Record name to be used to set a DNS entry
# Data Type: String
# Example: "@"; -- use @ to setup an entry at the root of your domain, e.g. "domain.com"
#
# Record TTL
# TTL value of the record in seconds, for a dynamic entry a short lifetime like 300 is recommended
# Data Type: String
# Example: "300";
#
# Array structure
# {
#     "pppoe-out1"; # Interface
#     ""; # Pool
#     "domain.com"; # Zone
#     "A"; # Record type
#     "@"; # Record name
#     300; # Record TTL
# };
## --------------------------------------------------------------------------------------------------
:local domainEntryConfig {
    {
        "wan_bridge";
        "";
        "fellbaum.net";
        "A";
        "hass.home";
        "600";
    };
};
# ---------------------------------------------------------------------------------------------------------------



:local logPrefix "[Hetzner DDNS]";
:local apiUrl "https://api.hetzner.cloud/v1";



:local getLocalIpv4 do={
    :local ip [/ip address get [:pick [find interface="$configInterface"] 0] address];
    :return [:pick $ip 0 [:find $ip /]];
};

:local getLocalIpv6 do={
    :local ip [/ipv6 address get [:pick [find interface="$configInterface" from-pool="$configInterfacePool" !link-local] 0] address];
    :return [:pick $ip 0 [:find $ip /]];
};

:local getRemoteIpv4 do={
    :do {
        :local ip [:resolve "$configDomain"];
        :return "$ip";
    } on-error={
        return "";
    };
};

:local getRemoveIpv6 do={
    :local result [:toarray ""]
    :local maxwait 5
    :local cnt 0
    :local listname "tmp-resolve$cnt"
    /ipv6 firewall address-list {
        :do {
            :while ([:len [find list=$listname]] > 0) do={
                :set cnt ($cnt + 1);
                :set listname "tmp-resolve$cnt";
            };
            :set cnt 0;
            add list=$listname address=$1;
            :while ([find list=$listname && dynamic] = "" && $cnt < $maxwait) do={
                :delay 1;:set cnt ($cnt +1)
            };
            :foreach i in=[find list=$listname && dynamic] do={
                 :local rawip [get $i address];
                 :set result ($result, [:pick $rawip 0 [:find $rawip "/"]]);
            };
            remove [find list=$listname && !dynamic];
        };
    };
    :return $result;
};

:local apiGetZones do={
    [/system script run "JParseFunctions"; global JSONLoad; global JSONLoads; global JSONUnload];

    :local apiPage -0;
    :local apiNextPage 1;
    :local apiLastPage 0;
    :local apiResponse "";
    :local returnArr [:toarray ""];

    :do {
        :set apiResponse ([/tool/fetch "$apiUrl/zones?page=$apiNextPage&name=$configZone" http-method=get http-header-field="Auth-API-Token:$apiKey" output=user as-value]->"data");

        :set apiPage ([$JSONLoads $apiResponse]->"meta"->"pagination"->"page");
        :set apiNextPage ([$JSONLoads $apiResponse]->"meta"->"pagination"->"next_page");
        :set apiLastPage ([$JSONLoads $apiResponse]->"meta"->"pagination"->"last_page");

        :set returnArr ($returnArr , ([:toarray ([$JSONLoads $apiResponse]->"zones")]));
    } while=($apiPage != $apiLastPage);
    $JSONUnload;

    :return $returnArr;
};

:local apiGetZoneId do={
    :foreach responseZone in=$responseZones do={
        :if (($responseZone->"name") = $configZone) do={
            :return ($responseZone->"id");
        };
    };
};

:local apiSetRecord do={
    #apiUrl=$apiUrl apiKey=$apiKey zoneId=$zoneId configType=$configType configRecord=$configRecord configTtl=$configTtl interfaceIp=$interfaceIp
    [/system script run "JParseFunctions"; global JSONLoad; global JSONLoads; global JSONUnload];

    :local recordId "";
    :local apiResponse "";
    :local payload "{\"zone_id\": \"$zoneId\",\"type\": \"$configType\",\"name\": \"$configRecord\",\"value\": \"$interfaceIp\",\"ttl\": $([:tonum $configTtl])}";
    :local records ([$JSONLoads ([/tool/fetch "$apiUrl/records?zone_id=$zoneId" http-method=get http-header-field="Auth-API-Token:$apiKey" output=user as-value]->"data")]->"records");

    :foreach record in=$records do={
        :if ((($record->"name") = $configRecord) && (($record->"type") = $configType)) do={
            :set recordId ($record->"id");
        }
    };

    :if ($recordId != "") do={
        :set apiResponse ([/tool/fetch "$apiUrl/records/$recordId" http-method=put http-header-field="Content-Type:application/json,Auth-API-Token:$apiKey" http-data=$payload output=user as-value]->"status");
    } else={
        :set apiResponse ([/tool/fetch "$apiUrl/records" http-method=post http-header-field="Content-Type:application/json,Auth-API-Token:$apiKey" http-data=$payload output=user as-value]->"status");
    };





    :local payload "{\"records\":[{\"value\":\"198.51.100.1\",\"comment\":\"My web server at Hetzner Cloud.\"}]}";
    :local records ([$JSONLoads ([/tool/fetch "$apiUrl/zones/$zoneId/rrsets" http-method=get http-header-field="Auth-API-Token:$apiKey" output=user as-value]->"data")]->"rrsets");

    :foreach record in=$records do={
        :if ((($record->"name") = $configRecord) && (($record->"type") = $configType)) do={
            :set recordId ($record->"id");
        }
    };

    :if ($recordId != "") do={
        #######:set apiResponse ([/tool/fetch "$apiUrl/records/$recordId" http-method=put http-header-field="Content-Type:application/json,Auth-API-Token:$apiKey" http-data=$payload output=user as-value]->"status");
        :set apiResponse ([/tool/fetch "$apiUrl/zones/$configZone/rrsets/$configRecord/$configType/actions/update_records" http-method=put http-header-field="Content-Type:application/json,Auth-API-Token:$apiKey" http-data=$payload output=user as-value]->"status");
    } else={
    ########todo
        :set apiResponse ([/tool/fetch "$apiUrl/records" http-method=post http-header-field="Content-Type:application/json,Auth-API-Token:$apiKey" http-data=$payload output=user as-value]->"status");
    };



    $JSONUnload;
    return $apiResponse;
};


# Log "run of script"
:log info "$logPrefix running";

:local index 0;
:foreach i in=$domainEntryConfig do={
    :local configInterface ("$($i->0)");
    :local configIpv6Pool ("$($i->1)");
    :local configZone ("$($i->2)");
    :local configType ("$($i->3)");
    :local configRecord ("$($i->4)");
    :local configTtl ("$($i->5)");
    :local configDomain "";
    :local interfaceIp "";
    :local dnsIp "";
    :local startLogMsg "$logPrefix Start configuring domain:";
    :local endLogMsg "$logPrefix Finished configuring domain:";


    :if ($configRecord = "@") do={
        :set configDomain ("$($i->2)");
    } else={
        :set configDomain ("$($i->4).$($i->2)");
    };


    :if ($configType = "A") do={
        :log info "$startLogMsg $configDomain - Type A record";

        :set interfaceIp [$getLocalIpv4 configInterface=$configInterface];
        :set dnsIp [$getRemoteIpv4 configDomain=$configDomain];

        :if ($interfaceIp != $dnsIp) do={
            :log info "$logPrefix $configDomain: local IP ($interfaceIp) differs from DNS IP ($dnsIp) - Updating entry";

            :local responseZones [$apiGetZones apiUrl=$apiUrl apiKey=$apiKey configZone=$configZone];
            :local zoneId [$apiGetZoneId responseZones=$responseZones configZone=$configZone];
            :local responseSetRecord [$apiSetRecord apiUrl=$apiUrl apiKey=$apiKey zoneId=$zoneId configType=$configType configRecord=$configRecord configTtl=$configTtl interfaceIp=$interfaceIp];
            :if ($responseSetRecord = "finished") do={
                :log info "$logPrefix $configDomain: update successful"
            };
        } else={
            :log info "$logPrefix $configDomain: local IP and DNS IP are equal - Nothing to do";
        }

        :log info "$endLogMsg $configDomain - Type A record";
    };


    :if ($configType = "AAAA") do={
        :log info "$startLogMsg $configDomain - Type AAAA record";

        :set interfaceIp [$getLocalIpv6 configInterface=$configInterface configInterfacePool=$configIpv6Pool];
        :set dnsIp [$getRemoveIpv6 $configDomain];

        :if ($interfaceIp != $dnsIp) do={
            :log info "$logPrefix $configDomain: local IP ($interfaceIp) differs from DNS IP ($dnsIp) - Updating entry";

            :local responseZones [$apiGetZones apiUrl=$apiUrl apiKey=$apiKey configZone=$configZone];
            :local zoneId [$apiGetZoneId responseZones=$responseZones configZone=$configZone];
            :local responseSetRecord [$apiSetRecord apiUrl=$apiUrl apiKey=$apiKey zoneId=$zoneId configType=$configType configRecord=$configRecord configTtl=$configTtl interfaceIp=$interfaceIp];
            :if ($responseSetRecord = "finished") do={
                :log info "$logPrefix $configDomain: update successful"
            };
        } else={
            :log info "$logPrefix $configDomain: local IP and DNS IP are equal - Nothing to do";
        }

        :log info "$endLogMsg $configDomain - Type AAAA record";
    };


    :if (($configType != "A") && ($configType != "AAAA")) do={
        :log error ("$logPrefix Wrong record type for array index number " . $index . " (Value: $configType)");
    };

    :set index ($index+1);
};
:set index;

:log info "$logPrefix finished";