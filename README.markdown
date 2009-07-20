# DSV

Create and parse CSV in Clojure.

## Installation

- `git clone git://github.com/deriksson/dsv.git`   
- `cd dsv`  
- `ant`

## Creating CSV

The function for creating CSV is in the namespace "se.abc.dsv.write".
Make it available to your program like this:

    (use 'se.abc.dsv.write)

At its simplest, you create CSV by passing a dataset in the form of a
vector of vectors of strings to the function "write-csv":

    (write-csv [["apple" "fruit"]["monkey" "animal"]])

Customise the CSV by specifying field delimiter and quote character.
In this example semicolons are used instead of commas as
field delimiters:

    (write-csv [["apple" "fruit"]["monkey" "animal"]] \;)

In the following example single quotes are used instead of the default
double quotes for avoiding delimiter collisions:

    (write-csv [["apple, red" "fruit"]["apple, green" "fruit"]] \, \')

## Parsing CSV

The namespace for the CSV parser is "se.abc.dsv.read":

    (use 'se.abc.dsv.read)

The following simple example uses the default settings, and will create
a vector containing two vectors, each containing two strings.

    (read-csv "apple,fruit\nmonkey,animal")

Use duck-streams to read a CSV file like this:

    (use 'clojure.contrib.duck-streams)
    (read-csv (slurp* "/tmp/stats.csv"))

The parser will raise an error if the CSV is not well-formed. Use 
error-kit from clojure contrib to handle errors:

    (use 'clojure.contrib.error-kit)  
    (with-handler  
     (read-csv (slurp* "/tmp/stats.csv"))  
     (handle malformed-csv [msg] (println "Malformed CSV.")))

The CSV function can be customised to handle other field delimiters
and quote characters than the defaults (',' and '"'):

    (read-csv "apple;fruit\nmonkey;animal" \;)

    (read-csv "'apple, red',fruit" \, \')
