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
Stop a Pandadb Server Windows Service

.DESCRIPTION
Stop a Pandadb Server Windows Service

.PARAMETER PandadbServer
An object representing a valid Pandadb Server object

.EXAMPLE
Stop-PandadbServer -PandadbServer $ServerObject

Stop the Pandadb Windows Windows Service for the Pandadb installation at $ServerObject

.OUTPUTS
System.Int32
0 = Service was stopped and not running
non-zero = an error occured

.NOTES
This function is private to the powershell module

#>
function Stop-PandadbServer
{
  [CmdletBinding(SupportsShouldProcess = $true,ConfirmImpact = 'Medium')]
  param(
    [Parameter(Mandatory = $true,ValueFromPipeline = $true)]
    [pscustomobject]$PandadbServer

  )

  begin
  {
  }

  process
  {
    $ServiceName = Get-PandadbWindowsServiceName -PandadbServer $PandadbServer -ErrorAction Stop
    $Found = Get-Service -Name $ServiceName -ComputerName '.' -ErrorAction 'SilentlyContinue'
    if ($Found)
    {
      $prunsrv = Get-PandadbPrunsrv -PandadbServer $PandadbServer -ForServerStop
      if ($prunsrv -eq $null) { throw "Could not determine the command line for PRUNSRV" }

      Write-Verbose "Stopping Pandadb service"
      $result = Invoke-ExternalCommand -Command $prunsrv.cmd -CommandArgs $prunsrv.args

      # Process the output
      if ($result.exitCode -eq 0) {
        Write-Host "Pandadb service stopped"
      } else {
        Write-Host "Pandadb service did not stop"
        # Write out STDERR if it did not stop
        Write-Host $result.capturedOutput
      }

      Write-Output $result.exitCode
    }
    else
    {
      Write-Host "Service stop failed - service '$ServiceName' not found"
      Write-Output 1
    }
  }

  end
  {
  }
}