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
TODO UPDATE HELPTEXT
Retrieves properties about a Pandadb installation

.DESCRIPTION
Retrieves properties about a Pandadb installation

.PARAMETER PandadbServer
An object representing a valid Pandadb Server object

.PARAMETER ConfigurationFile
The name of the configuration file or files to parse.  If not specified the default set of all configuration files are used.  Do not use the full path, just the filename, the path is relative to '[PandadbHome]\conf'

.PARAMETER Name
The name of the property to retrieve.  If not specified, all properties are returned.

.EXAMPLE
Get-PandadbSetting -PandadbServer $ServerObject | Format-Table

Retrieves all settings for the Pandadb installation at $ServerObject

.EXAMPLE
Get-PandadbSetting -PandadbServer $ServerObject -Name 'dbms.active_database'

Retrieves all settings with the name 'dbms.active_database' from the Pandadb installation at $ServerObject

.EXAMPLE
Get-PandadbSetting -PandadbServer $ServerObject -Name 'dbms.active_database' -ConfigurationFile 'pandadb.conf'

Retrieves all settings with the name 'dbms.active_database' from the Pandadb installation at $ServerObject in 'pandadb.conf'

.OUTPUTS
System.Management.Automation.PSCustomObject
This is a Pandadb Setting Object
Properties;
'Name' : Name of the property
'Value' : Value of the property.  Multivalue properties are string arrays (string[])
'ConfigurationFile' : Name of the configuration file where the setting is defined
'IsDefault' : Whether this setting is a default value (Reserved for future use)
'PandadbHome' : Path to the Pandadb installation

.LINK
Get-PandadbServer 

.NOTES
This function is private to the powershell module

#>
function Get-PandadbSetting
{
  [CmdletBinding(SupportsShouldProcess = $false,ConfirmImpact = 'Low')]
  param(
    [Parameter(Mandatory = $true,ValueFromPipeline = $true)]
    [pscustomobject]$PandadbServer

    ,[Parameter(Mandatory = $false)]
    [string[]]$ConfigurationFile = $null

    ,[Parameter(Mandatory = $false)]
    [string]$Name = ''
  )

  begin
  {
  }

  process
  {
    # Get the Pandadb Server information
    if ($PandadbServer -eq $null) { return }

    # Set the default list of configuration files    
    if ($ConfigurationFile -eq $null)
    {
      $ConfigurationFile = ('pandadb.conf','pandadb-wrapper.conf')
    }

    $ConfigurationFile | ForEach-Object -Process `
       {
      $filename = $_
      $filePath = Join-Path -Path $PandadbServer.ConfDir -ChildPath $filename
      if (Test-Path -Path $filePath)
      {
        $keyPairsFromFile = Get-KeyValuePairsFromConfFile -FileName $filePath
      }
      else
      {
        $keyPairsFromFile = $null
      }

      if ($keyPairsFromFile -ne $null)
      {
        $keyPairsFromFile.GetEnumerator() | Where-Object { ($Name -eq '') -or ($_.Name -eq $Name) } | ForEach-Object -Process `
           {
          $properties = @{
            'Name' = $_.Name;
            'Value' = $_.value;
            'ConfigurationFile' = $filename;
            'IsDefault' = $false;
            'PandadbHome' = $PandadbServer.Home;
          }

          Write-Output (New-Object -TypeName PSCustomObject -Property $properties)
        }
      }
    }
  }

  end
  {
  }
}
