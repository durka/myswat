<?php
/*
 * Copyright 2012 Alex Burka
 * 
 * This file is part of Trashmower.
 * Trashmower is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trashmower is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trashmower.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * There are four operations:
 *
 * 0. Request all locations
 *      provide nothing
 * 1. Update location
 *      provide an ID and a name|location
 * 2. Remove location
 *      provide just an ID
 *      note: stale locations will be pruned after at least 1 day
 *              (pruning is done whenever locations are accessed or retrieved)
 * 3. Request ID and update location
 *      provide just a name|location
 *      this is ONLY ok if the app doesn't have an ID yet
 */

    // parse a CSV file into a people array
    function parse($fp)
    {
        $arr = array();
        while (($line = fgetcsv($fp)) !== FALSE)
        {
            $arr[$line[0]] = array_slice($line, 1);
        }
        return $arr;
    }
    
    // encode a people array into a CSV string
    function encode($arr)
    {
        ob_start();
        foreach ($arr as $id => $loc)
        {
            array_unshift($loc, $id);
            fputcsv(STDOUT, $loc);
        }
        return ob_get_flush();
    }

    // prune old locations out of a people array
    function prune(&$arr)
    {
    }


    if (isset($_GET['i']))
    {
        $id = $_GET['i'];
    }
    if (isset($_GET['l']))
    {
        $loc = $_GET['l'];
    }

    $fp = fopen('people', 'r+');
    if (flock($fp, LOCK_EX))
    {
        $people = parse($fp);
        prune($people);

        if (!isset($id) && !isset($loc))
        {
            // requesting all locations
            $encoded = encode($people);

            echo $encoded;
        }
        else if (isset($id) && isset($loc))
        {
            // update location
            $people[$id] = $loc;

            $encoded = encode($people);
        }
        else if (isset($id) && !isset($loc))
        {
            // remove location
            unset($people[$id]);

            $encoded = encode($people);
        }
        else if (!isset($id) && isset($loc))
        {
            // request ID and update location
            $id = max(array_keys($people)) + 1;
            $people[$id] = $loc;

            $encoded = encode($people);
        }

        ftruncate($fp, 0);
        fwrite($fp, $encoded);
        flock($fp, LOCK_UN);
        fclose($fp);
    }
    else
    {
        die('flock() failed!');
    }
?>
