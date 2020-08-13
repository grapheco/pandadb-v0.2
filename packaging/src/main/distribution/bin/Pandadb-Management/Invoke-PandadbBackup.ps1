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
Invokes Pandadb Backup utility

.DESCRIPTION
Invokes Pandadb Backup utility

.PARAMETER CommandArgs
The remaining command line arguments to pass to the Pandadb Backup

.OUTPUTS
System.Int32
0 = Success
non-zero = an error occured

.NOTES
Only supported on version 3.x Pandadb Enterprise Edition databases

#>
function Invoke-PandadbBackup
{
  [CmdletBinding(SupportsShouldProcess = $false,ConfirmImpact = 'Low')]
  param(
    [Parameter(Mandatory = $false,ValueFromRemainingArguments = $true)]
    [object[]]$CommandArgs = @()
  )

  begin
  {
  }

  process
  {
    try {
      return [int](Invoke-PandadbUtility -Command 'Backup' -CommandArgs $CommandArgs -ErrorAction 'Stop')
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
