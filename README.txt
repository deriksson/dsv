DSV
===
Create and parse CSV in Clojure.

Installation
------------
git clone git://github.com/deriksson/dsv.git
cd dsv
ant

Creating CSV
------------
The function for creating CSV is in the namespace "se.abc.dsv.write".
Make it available to your program like this:

(use 'se.abc.dsv.write)

At its simplest you create CSV by passing a dataset in the form of a
vector of vectors of strings to the function "write-csv":

(write-csv [["apple" "fruit"]["monkey" "animal"]])


Parsing CSV
-----------
(use 'se.abc.dsv.read)
(read-csv "apple,fruit\nmonkey,animal")
