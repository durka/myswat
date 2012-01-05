#!/usr/bin/env python2

# Copyright 2012 Alex Burka
# 
# This file is part of Trashmower.
# Trashmower is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# 
# Trashmower is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with Trashmower.  If not, see <http://www.gnu.org/licenses/>.

from xml.dom.minidom import Document

def make_string_array(doc, parent, name):
    arr = doc.createElement('string-array')
    arr.setAttribute('name', name)
    parent.appendChild(arr)
    return arr

def append_text_node(doc, arr, text):
    item = doc.createElement('item')
    item.appendChild(doc.createTextNode(text))
    arr.appendChild(item)

if __name__ == '__main__':
    import urllib
    import sys

    if len(sys.argv) != 2:
        print 'Usage: %s output.xml' % sys.argv[0]
        sys.exit(1)

    sock = urllib.urlopen('http://www.swarthmore.edu/campusmap/')
    html = sock.read()
    sock.close()

    lines = html.split('\n')
    markers = {}
    for i in range(len(lines)):
        if 'createMarker(' in lines[i] and 'function createMarker' not in lines[i]:
            params = lines[i].split(',')
            short_name = params[0].split('"')[-2]
            lat = lines[i-2][16:33]
            lng = lines[i-1][16:34]
            name = params[4][1:-1]
            desc = ','.join(params[5:])[1:-3]
            markers[short_name] = {'latitude': lat,
                                   'longitude': lng,
                                   'desc': desc,
                                   'names': [name]}
        if 'relateCommonDestination(' in lines[i] and 'function relateCommonDestination' not in lines[i]:
            name = lines[i-2].split('"')[-2]
            short = lines[i-1].split('"')[-2]
            markers[short]['names'].append(name)

    doc = Document()
    resources = doc.createElement('resources')
    doc.appendChild(resources)
    arrs = [make_string_array(doc, resources, n) for n in  ['shorts',
                                                            'names',
                                                            'latitudes',
                                                            'longitudes',
                                                            'descriptions']]
    [resources.appendChild(a) for a in arrs]

    for short, info in markers.iteritems():
        append_text_node(doc, arrs[0], short)
        append_text_node(doc, arrs[1], '|'.join(info['names']))
        append_text_node(doc, arrs[2], info['latitude'])
        append_text_node(doc, arrs[3], info['longitude'])
        append_text_node(doc, arrs[4], info['desc'])
    
    f = open(sys.argv[1], 'w')
    f.write(doc.toxml().replace("'", "\\'"))
    f.close()

