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

# Module manifest for module 'Pandadb-Management'
#


@{
ModuleVersion = '3.0.0'

GUID = '2a3e34b4-5564-488e-aaf6-f2cba3f7f05d'

Author = 'Network Engine for Objects'

CompanyName = 'Network Engine for Objects'

Copyright = 'https://pandadb.com/licensing/'

Description = 'Powershell module to manage a Pandadb instance on Windows'

PowerShellVersion = '2.0'

NestedModules = @('Pandadb-Management\Pandadb-Management.psm1')

FunctionsToExport = @(
'Invoke-Pandadb',
'Invoke-PandadbAdmin',
'Invoke-PandadbShell',
'Invoke-PandadbBackup',
'Invoke-PandadbImport',
'Get-Args'
)

CmdletsToExport = ''

VariablesToExport = ''

AliasesToExport = ''
}
