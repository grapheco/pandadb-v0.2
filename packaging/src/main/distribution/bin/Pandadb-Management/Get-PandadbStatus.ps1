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
Retrieves the status for the Pandadb Windows Service

.DESCRIPTION
Retrieves the status for the Pandadb Windows Service

.PARAMETER PandadbServer
An object representing a valid Pandadb Server object

.EXAMPLE
Get-PandadbStatus -PandadbServer $ServerObject

Retrieves the status of the Windows Service for the Pandadb database at $ServerObject

.OUTPUTS
System.Int32
0 = Service is running
3 = Service is not installed or is not running

.NOTES
This function is private to the powershell module

#>
function Get-PandadbStatus
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
    $ServiceName = Get-PandadbWindowsServiceName -PandadbServer $PandadbServer -ErrorAction Stop
    $neoService = $null
    try {
      $neoService = Get-Service -Name $ServiceName -ErrorAction Stop
    }
    catch {
      Write-Host "The Pandadb Windows Service '$ServiceName' is not installed"
      return 3
    }

    if ($neoService.Status -eq 'Running') {
      Write-Host "Pandadb is running"
      return 0
    }
    else {
      Write-Host "Pandadb is not running.  Current status is $($neoService.Status)"
      return 3
    }
  }

  end
  {
  }
}
