#  Copyright: 2010 Regents of the University of Hawaii and the
#             School of Ocean and Earth Science and Technology
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

'''Instrument package that allows instrument to RBNB DataTurbine communication.'''

__author__  = "Christopher Jones, csjones@hawaii.edu"
__version__ = "$LastChangedRevision: $"
# $LastChangedDate: $
# $LastChangedBy:   $

import tchain
# import adam 
# import adcp
# import ctd
# import dvp2
# import flntu
import sys
import os

#Add the rbnb jar file to the python class path
sys.path.append(os.path.join('..', '..', '..', 'lib', 'rbnb.jar'))
