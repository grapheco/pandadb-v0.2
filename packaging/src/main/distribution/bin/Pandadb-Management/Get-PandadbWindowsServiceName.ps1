# Copyright (c) 2002-2018 "Pandadb,"
# Pandadb Sweden AB [http://pandadb.com]
#
# This file is part of Pandadb.
#
# Pandadb is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.


<#
.SYNOPSIS
Retrieves the name of the Windows Service from the configuration information

.DESCRIPTION
Retrieves the name of the Windows Service from the configuration information

.PARAMETER PandadbServer
An object representing a valid Pandadb Server object

.EXAMPLE
Get-PandadbWindowsServiceName -PandadbServer $ServerObject

Retrieves the name of the Windows Service for the Pandadb Database at $ServerObject

.OUTPUTS
System.String
The name of the Windows Service or $null if it could not be determined

.NOTES
This function is private to the powershell module

#>
function Get-PandadbWindowsServiceName
{
  [CmdletBinding(SupportsShouldProcess = $false,ConfirmImpact = 'Low')]
  param(
    [Parameter(Mandatory = $true,ValueFromPipeline = $true)]
    [pscustomobject]$PandadbServer
  )

  begin
  {
  }

  process {
    $ServiceName = ''
    # Try pandadb.conf first, but then fallback to pandadb-wrapper.conf for backwards compatibility reasons
    $setting = (Get-PandadbSetting -ConfigurationFile 'pandadb.conf' -Name 'dbms.windows_service_name' -PandadbServer $PandadbServer)
    if ($setting -ne $null) {
      $ServiceName = $setting.value
    } else {
      $setting = (Get-PandadbSetting -ConfigurationFile 'pandadb-wrapper.conf' -Name 'dbms.windows_service_name' -PandadbServer $PandadbServer)
      if ($setting -ne $null) { $ServiceName = $setting.value }
    }

    if ($ServiceName -eq '')
    {
      throw 'Could not find the Windows Service Name for Pandadb (dbms.windows_service_name in pandadb.conf)'
      return $null
    }
    else
    {
      Write-Verbose "Pandadb Windows Service Name is $ServiceName"
      Write-Output $ServiceName.Trim()
    }
  }

  end
  {
  }
}
