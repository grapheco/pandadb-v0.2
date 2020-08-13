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
Invokes various Pandadb Utilities

.DESCRIPTION
Invokes various Pandadb Utilities.  This is a generic utility function called by the external functions e.g. Shell, Import

.PARAMETER Command
A string of the command to run.

.PARAMETER CommandArgs
Command line arguments to pass to the utility

.OUTPUTS
System.Int32
0 = Success
non-zero = an error occured

.NOTES
Only supported on version 3.x Pandadb Community and Enterprise Edition databases

.NOTES
This function is private to the powershell module

#>
function Invoke-PandadbUtility
{
  [CmdletBinding(SupportsShouldProcess = $false,ConfirmImpact = 'Low')]
  param(
    [Parameter(Mandatory = $false,ValueFromPipeline = $false,Position = 0)]
    [string]$Command = ''

    ,[Parameter(Mandatory = $false,ValueFromRemainingArguments = $true)]
    [object[]]$CommandArgs = @()
  )

  begin
  {
  }

  process
  {
    # Determine the Pandadb Home Directory.  Uses the PANDADB_HOME environment variable or a parent directory of this script
    $PandadbHome = Get-PandadbEnv 'PANDADB_HOME'
    if (($PandadbHome -eq $null) -or (-not (Test-Path -Path $PandadbHome))) {
      $PandadbHome = Split-Path -Path (Split-Path -Path $PSScriptRoot -Parent) -Parent
    }
    if ($PandadbHome -eq $null) { throw "Could not determine the Pandadb home Directory.  Set the PANDADB_HOME environment variable and retry" }
    Write-Verbose "Pandadb Root is '$PandadbHome'"

    $thisServer = Get-PandadbServer -PandadbHome $PandadbHome -ErrorAction Stop
    if ($thisServer -eq $null) { throw "Unable to determine the Pandadb Server installation information" }
    Write-Verbose "Pandadb Server Type is '$($thisServer.ServerType)'"
    Write-Verbose "Pandadb Version is '$($thisServer.ServerVersion)'"
    Write-Verbose "Pandadb Database Mode is '$($thisServer.DatabaseMode)'"

    $GetJavaParams = @{}
    switch ($Command.Trim().ToLower())
    {
      "admintool" {
        Write-Verbose "Admintool command specified"
        $GetJavaParams = @{
          StartingClass = 'org.neo4j.commandline.admin.AdminTool';
        }
        break
      }
      "import" {
        Write-Verbose "Import command specified"
        $GetJavaParams = @{
          StartingClass = 'org.neo4j.tooling.ImportTool';
        }
        break
      }
      "backup" {
        Write-Verbose "Backup command specified"
        if ($thisServer.ServerType -ne 'Enterprise')
        {
          throw "Pandadb Server type $($thisServer.ServerType) does not support online backup"
        }
        $GetJavaParams = @{
          StartingClass = 'org.neo4j.backup.BackupTool';
        }
        break
      }
      default {
        Write-Host "Unknown utility $Command"
        return 255
      }
    }

    # Generate the required Java invocation
    $JavaCMD = Get-Java -PandadbServer $thisServer -ForUtility @GetJavaParams
    if ($JavaCMD -eq $null) { throw 'Unable to locate Java' }

    $ShellArgs = $JavaCMD.args
    if ($ShellArgs -eq $null) { $ShellArgs = @() }
    # Add unbounded command line arguments
    $ShellArgs += $CommandArgs

    Write-Verbose "Starting pandadb utility using command line $($JavaCMD.java) $ShellArgs"
    $result = (Start-Process -FilePath $JavaCMD.java -ArgumentList $ShellArgs -Wait -NoNewWindow -Passthru)
    return $result.exitCode
  }

  end
  {
  }
}
