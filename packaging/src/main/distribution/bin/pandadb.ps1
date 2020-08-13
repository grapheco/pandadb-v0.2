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

try
{
    Unblock-File -Path '%~dp0Pandadb-Management\*.*' -ErrorAction 'SilentlyContinue'
}
catch
{
};

Import-Module "$PSScriptRoot\Pandadb-Management.psd1"
$Arguments = Get-Args $args
Exit (Invoke-Pandadb -Verbose:$Arguments.Verbose -Command $Arguments.ArgsAsStr)

