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
Install a Pandadb Server Windows Service

.DESCRIPTION
Install a Pandadb Server Windows Service

.PARAMETER PandadbServer
An object representing a valid Pandadb Server object

.EXAMPLE
Install-PandadbServer -PandadbServer $ServerObject

Install the Pandadb Windows Windows Service for the Pandadb installation at $ServerObject

.OUTPUTS
System.Int32
0 = Service is installed or already exists
non-zero = an error occured

.NOTES
This function is private to the powershell module

#>
function Install-PandadbServer
{
  [CmdletBinding(SupportsShouldProcess = $false,ConfirmImpact = 'Medium')]
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
    if (-not $Found)
    {
      $prunsrv = Get-PandadbPrunsrv -PandadbServer $PandadbServer -ForServerInstall
      if ($prunsrv -eq $null) { throw "Could not determine the command line for PRUNSRV" }

      Write-Verbose "Installing Pandadb as a service"
      $result = Invoke-ExternalCommand -Command $prunsrv.cmd -CommandArgs $prunsrv.args

      # Process the output
      if ($result.exitCode -eq 0) {
        Write-Host "Pandadb service installed"
      } else {
        Write-Host "Pandadb service did not install"
        # Write out STDERR if it did not install
        Write-Host $result.capturedOutput
      }

      Write-Output $result.exitCode
    } else {
      Write-Verbose "Service already installed"
      Write-Output 0
    }
  }

  end
  {
  }
}
