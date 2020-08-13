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
Starts a Pandadb Server instance

.DESCRIPTION
Starts a Pandadb Server instance either as a java console application or Windows Service

.PARAMETER PandadbServer
An object representing a valid Pandadb Server object

.EXAMPLE
Start-PandadbServer -PandadbServer $ServerObject

Start the Pandadb Windows Windows Service for the Pandadb installation at $ServerObject

.OUTPUTS
System.Int32
0 = Service was started and is running
non-zero = an error occured

.NOTES
This function is private to the powershell module

#>
function Start-PandadbServer
{
  [CmdletBinding(SupportsShouldProcess = $false,ConfirmImpact = 'Low',DefaultParameterSetName = 'WindowsService')]
  param(
    [Parameter(Mandatory = $true,ValueFromPipeline = $false)]
    [pscustomobject]$PandadbServer

    ,[Parameter(Mandatory = $true,ParameterSetName = 'Console')]
    [switch]$Console

    ,[Parameter(Mandatory = $true,ParameterSetName = 'WindowsService')]
    [switch]$Service
  )

  begin
  {
  }

  process
  {
    # Running Pandadb as a console app
    if ($PsCmdlet.ParameterSetName -eq 'Console')
    {
      $JavaCMD = Get-Java -PandadbServer $PandadbServer -ForServer -ErrorAction Stop
      if ($JavaCMD -eq $null)
      {
        Write-Error 'Unable to locate Java'
        return 255
      }

      Write-Verbose "Starting Pandadb as a console with command line $($JavaCMD.java) $($JavaCMD.args)"
      $result = (Start-Process -FilePath $JavaCMD.java -ArgumentList $JavaCMD.args -Wait -NoNewWindow -Passthru -WorkingDirectory $PandadbServer.Home)
      Write-Verbose "Returned exit code $($result.ExitCode)"

      Write-Output $result.exitCode
    }

    # Running Pandadb as a windows service
    if ($PsCmdlet.ParameterSetName -eq 'WindowsService')
    {
      $ServiceName = Get-PandadbWindowsServiceName -PandadbServer $PandadbServer -ErrorAction Stop
      $Found = Get-Service -Name $ServiceName -ComputerName '.' -ErrorAction 'SilentlyContinue'
      if ($Found)
      {
        $prunsrv = Get-PandadbPrunsrv -PandadbServer $PandadbServer -ForServerStart
        if ($prunsrv -eq $null) { throw "Could not determine the command line for PRUNSRV" }

        Write-Verbose "Starting Pandadb as a service"
        $result = Invoke-ExternalCommand -Command $prunsrv.cmd -CommandArgs $prunsrv.args

        # Process the output
        if ($result.exitCode -eq 0) {
          Write-Host "Pandadb service started"
        } else {
          Write-Host "Pandadb service did not start"
          # Write out STDERR if it did not start
          Write-Host $result.capturedOutput
        }

        Write-Output $result.exitCode
      }
      else
      {
        Write-Host "Service start failed - service '$ServiceName' not found"
        Write-Output 1
      }
    }
  }

  end
  {
  }
}
