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
Retrieves properties about a Pandadb installation

.DESCRIPTION
Retrieves properties about a Pandadb installation and outputs a Pandadb Server object.

.PARAMETER PandadbHome
The full path to the Pandadb installation.

.EXAMPLE
Get-PandadbServer -PandadbHome 'C:\Pandadb'

Retrieves information about the Pandadb installation at C:\Pandadb

.EXAMPLE
'C:\Pandadb' | Get-PandadbServer

Retrieves information about the Pandadb installation at C:\Pandadb

.EXAMPLE
Get-PandadbServer

Retrieves information about the Pandadb installation as determined by Get-PandadbHome

.OUTPUTS
System.Management.Automation.PSCustomObject
This is a Pandadb Server Object

.LINK
Get-PandadbHome

.NOTES
This function is private to the powershell module

#>
function Get-PandadbServer
{
  [CmdletBinding(SupportsShouldProcess = $false,ConfirmImpact = 'Low')]
  param(
    [Parameter(Mandatory = $false,ValueFromPipeline = $true)]
    [Alias('Home')]
    [AllowEmptyString()]
    [string]$PandadbHome = ''
  )

  begin
  {
  }

  process
  {
    # Get and check the Pandadb Home directory
    if (($PandadbHome -eq '') -or ($PandadbHome -eq $null))
    {
      Write-Error "Could not detect the Pandadb Home directory"
      return
    }

    if (-not (Test-Path -Path $PandadbHome))
    {
      Write-Error "$PandadbHome does not exist"
      return
    }

    # Convert the path specified into an absolute path
    $PandadbDir = Get-Item $PandadbHome
    $PandadbHome = $PandadbDir.FullName.TrimEnd('\')

    $ConfDir = Get-PandadbEnv 'PANDADB_CONF'
    if ($ConfDir -eq $null)
    {
      $ConfDir = (Join-Path -Path $PandadbHome -ChildPath 'conf')
    }

    # Get the information about the server
    $serverProperties = @{
      'Home' = $PandadbHome;
      'ConfDir' = $ConfDir;
      'LogDir' = (Join-Path -Path $PandadbHome -ChildPath 'logs');
      'ServerVersion' = '';
      'ServerType' = 'Community';
      'DatabaseMode' = '';
    }

    # Check if the lib dir exists
    $libPath = (Join-Path -Path $PandadbHome -ChildPath 'lib')
    if (-not (Test-Path -Path $libPath))
    {
      Write-Error "$PandadbHome is not a valid Pandadb installation.  Missing $libPath"
      return
    }

    # Scan the lib dir...
    Get-ChildItem (Join-Path -Path $PandadbHome -ChildPath 'lib') | Where-Object { $_.Name -like 'pandadb-server-*.jar' } | ForEach-Object -Process `
       {
      # if pandadb-server-enterprise-<version>.jar exists then this is the enterprise version
      if ($_.Name -like 'pandadb-server-enterprise-*.jar') { $serverProperties.ServerType = 'Enterprise' }

      # Get the server version from the name of the pandadb-server-<version>.jar file
      if ($matches -ne $null) { $matches.Clear() }
      if ($_.Name -match '^pandadb-server-(\d.+)\.jar$') { $serverProperties.ServerVersion = $matches[1] }
    }
    $serverObject = New-Object -TypeName PSCustomObject -Property $serverProperties

    # Validate the object
    if ([string]$serverObject.ServerVersion -eq '') {
      Write-Error "Unable to determine the version of the installation at $PandadbHome"
      return
    }

    # Get additional settings...
    $setting = (Get-PandadbSetting -ConfigurationFile 'pandadb.conf' -Name 'dbms.mode' -PandadbServer $serverObject)
    if ($setting -ne $null) { $serverObject.DatabaseMode = $setting.value }

    # Set process level environment variables
    #  These should mirror the same paths in pandadb-shared.sh
    $dirSettings = @{ 'PANDADB_DATA' = @{ 'config_var' = 'dbms.directories.data'; 'default' = (Join-Path $PandadbHome 'data') }
      'PANDADB_LIB' = @{ 'config_var' = 'dbms.directories.lib'; 'default' = (Join-Path $PandadbHome 'lib') }
      'PANDADB_LOGS' = @{ 'config_var' = 'dbms.directories.logs'; 'default' = (Join-Path $PandadbHome 'logs') }
      'PANDADB_PLUGINS' = @{ 'config_var' = 'dbms.directories.plugins'; 'default' = (Join-Path $PandadbHome 'plugins') }
      'PANDADB_RUN' = @{ 'config_var' = 'dbms.directories.run'; 'default' = (Join-Path $PandadbHome 'run') }
    }
    foreach ($name in $dirSettings.Keys)
    {
      $definition = $dirSettings[$name]
      $configured = (Get-PandadbSetting -ConfigurationFile 'pandadb.conf' -Name $definition['config_var'] -PandadbServer $serverObject)
      $value = $definition['default']
      if ($configured -ne $null) { $value = $configured.value }

      if ($value -ne $null) {
        if (-not (Test-Path $value -IsValid)) {
          throw "'$value' is not a valid path entry on this system."
        }

        $absolutePathRegex = '(^\\|^/|^[A-Za-z]:)'
        if (-not ($value -match $absolutePathRegex)) {
          $value = (Join-Path -Path $PandadbHome -ChildPath $value)
        }
      }
      Set-PandadbEnv $name $value
    }

    # Set log dir on server object and attempt to create it if it doesn't exist
    $serverObject.LogDir = (Get-PandadbEnv 'PANDADB_LOGS')
    if ($serverObject.LogDir -ne $null) {
      if (-not (Test-Path -PathType Container -Path $serverObject.LogDir)) {
        New-Item -ItemType Directory -Force -ErrorAction SilentlyContinue -Path $serverObject.LogDir | Out-Null
      }
    }

    #  PANDADB_CONF and PANDADB_HOME are used by the Pandadb Admin Tool
    if ((Get-PandadbEnv 'PANDADB_CONF') -eq $null) { Set-PandadbEnv "PANDADB_CONF" $ConfDir }
    if ((Get-PandadbEnv 'PANDADB_HOME') -eq $null) { Set-PandadbEnv "PANDADB_HOME" $PandadbHome }

    # Any deprecation warnings
    $WrapperPath = Join-Path -Path $ConfDir -ChildPath 'pandadb-wrapper.conf'
    if (Test-Path -Path $WrapperPath) { Write-Warning "$WrapperPath is deprecated and support for it will be removed in a future version of Pandadb; please move all your settings to pandadb.conf" }

    Write-Output $serverObject
  }

  end
  {
  }
}
