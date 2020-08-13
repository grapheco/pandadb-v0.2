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
Invokes a command which manipulates a Pandadb Server e.g Start, Stop, Install and Uninstall

.DESCRIPTION
Invokes a command which manipulates a Pandadb Server e.g Start, Stop, Install and Uninstall.  

Invoke this function with a blank or missing command to list available commands

.PARAMETER Command
A string of the command to run.  Pass a blank string for the help text

.EXAMPLE
Invoke-Pandadb

Outputs the available commands

.EXAMPLE
Invoke-Pandadb status -Verbose

Gets the status of the Pandadb Windows Service and outputs verbose information to the console.

.OUTPUTS
System.Int32
0 = Success
non-zero = an error occured

.NOTES
Only supported on version 3.x Pandadb Community and Enterprise Edition databases

#>
function Invoke-Pandadb
{
  [CmdletBinding(SupportsShouldProcess = $false,ConfirmImpact = 'Low')]
  param(
    [Parameter(Mandatory = $false,ValueFromPipeline = $false,Position = 0)]
    [string]$Command = ''
  )

  begin
  {
  }

  process
  {
    try
    {
      $HelpText = "Usage: pandadb { console | start | stop | restart | status | install-service | uninstall-service | update-service } < -Verbose >"

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

      switch ($Command.Trim().ToLower())
      {
        "help" {
          Write-Host $HelpText
          return 0
        }
        "console" {
          Write-Verbose "Console command specified"
          return [int](Start-PandadbServer -Console -PandadbServer $thisServer -ErrorAction Stop)
        }
        "start" {
          Write-Verbose "Start command specified"
          return [int](Start-PandadbServer -Service -PandadbServer $thisServer -ErrorAction Stop)
        }
        "stop" {
          Write-Verbose "Stop command specified"
          return [int](Stop-PandadbServer -PandadbServer $thisServer -ErrorAction Stop)
        }
        "restart" {
          Write-Verbose "Restart command specified"

          $result = (Stop-PandadbServer -PandadbServer $thisServer -ErrorAction Stop)
          if ($result -ne 0) { return $result }
          return (Start-PandadbServer -Service -PandadbServer $thisServer -ErrorAction Stop)
        }
        "status" {
          Write-Verbose "Status command specified"
          return [int](Get-PandadbStatus -PandadbServer $thisServer -ErrorAction Stop)
        }
        "install-service" {
          Write-Verbose "Install command specified"
          return [int](Install-PandadbServer -PandadbServer $thisServer -ErrorAction Stop)
        }
        "uninstall-service" {
          Write-Verbose "Uninstall command specified"
          return [int](Uninstall-PandadbServer -PandadbServer $thisServer -ErrorAction Stop)
        }
        "update-service" {
          Write-Verbose "Update command specified"
          return [int](Update-PandadbServer -PandadbServer $thisServer -ErrorAction Stop)
        }
        default {
          if ($Command -ne '') { Write-Host "Unknown command $Command" }
          Write-Host $HelpText
          return 1
        }
      }
      # Should not get here!
      return 2
    }
    catch {
      Write-Error $_
      return 1
    }
  }

  end
  {
  }
}
