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
(use 'se.abc.dsv.write)
(write-csv [["apple" "fruit"]["monkey" "animal"]])

Parsing CSV
-----------
(use 'se.abc.dsv.read)
(read-csv "apple,fruit\nmonkey,animal")
